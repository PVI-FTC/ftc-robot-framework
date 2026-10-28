using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using Unity.Collections;
using UnityEngine;

namespace PviHarness.Replay
{
    /// <summary>
    /// Shows harness runs in 3D, in a window laid out like the Swing viewer: header with the
    /// mission clock, a chooser, the field, the "mission control" cards and gamepads on the right,
    /// and the playback bar at the bottom (<see cref="ReplayHud"/>). Put it on one empty GameObject
    /// (the Replay scene already has it) and press Play.
    ///
    /// <para><b>Replay</b> (Stage U1): opens the newest recording in Harness/build/recordings (or
    /// <see cref="recordingPath"/>) and plays it back. Keys: Space play/pause, Left/Right one loop,
    /// Shift+Left/Right one second, Home/End, [ and ] slower/faster, N/P newer/older recording.</para>
    ///
    /// <para><b>Live</b> (Stage U2): press <b>Live mode</b> (or L), which starts the harness side
    /// (<see cref="HarnessLauncher"/>), then pick "Live: ..." in the chooser. The harness runs the real TeamCode and reads the game controllers; Unity asks for one
    /// OpMode call every 20 ms (<see cref="LivePacer"/>) and draws each one. Enter = PLAY, Esc = STOP,
    /// then Save keeps the run as a recording.</para>
    ///
    /// <para><b>Unity physics</b> (Stage U3): with the Physics button (or U) set to "Unity", the next
    /// live run is moved by Unity's physics engine (<see cref="UnityRobotPhysics"/>): walls and the
    /// season field's solid parts really stop the robot.</para>
    ///
    /// <para>Always: T top-down / 3D view, F follow the robot, V the Visuals screen (arena and
    /// setting), G next arena; in 3D view right-drag turns the camera and scroll zooms.</para>
    /// </summary>
    public sealed class ReplayController : MonoBehaviour
    {
        [Tooltip("A recording to open. Leave empty to open the newest file in Harness/build/recordings.")]
        [SerializeField] private string recordingPath = "";

        [Tooltip("The port gradlew unityBridge listens on (7465 unless started with -Pport=...).")]
        [SerializeField] private int bridgePort = TcpBridgeTransport.DefaultPort;

        private FieldBuilder.Built scene;
        private Camera replayCamera;
        private OrbitCamera orbit;
        private readonly ReplayHud hud = new ReplayHud();
        private RunRecordingData recording;
        private ReplayTimeline timeline;
        private GamePieceTrack pieceTrack;
        private GamePieceView pieceView;
        private AprilTagSightView tagSights;
        // The A key / Visuals switch: draw the AprilTag sight lines and the "Tags in view" card (remembered, on by default).
        private bool showAprilTags = true;
        private const string AprilTagsPreferenceKey = "HarnessReplay.aprilTags";
        private string shownFile;
        private List<string> recordingsNewestFirst = new List<string>();

        // Season fields (Harness/fields/*.json) and the one drawn now.
        private const string FieldPreferenceKey = "HarnessReplay.field";
        private List<FieldLayoutData> fields = new List<FieldLayoutData>();
        private string fieldId = FieldLayouts.PlainFieldId;
        private Transform seasonRoot;
        // The O key: the ball obstacles drawn over the field (null when hidden).
        private Transform obstacleRoot;
        private bool showObstacles;

        // The setting around the arena (SettingBuilder) and the one drawn now.
        private const string SettingPreferenceKey = "HarnessReplay.setting";
        private string settingId = SettingBuilder.MissionControlId;
        private Transform settingRoot;

        // Live play.
        private HarnessLauncher launcher;
        private HarnessLauncher.LaunchState lastLaunchState = HarnessLauncher.LaunchState.Off;
        private const string PhysicsPreferenceKey = "HarnessReplay.physics";
        private bool unityPhysicsChosen;
        private UnityRobotPhysics robotPhysics;
        private LiveSession live;
        private readonly LivePacer pacer = new LivePacer();
        private string liveTitle;

        // Trail: every recorded pose as a Unity position, and how many belong to loops up to each index.
        private NativeArray<Vector3> trailPoints;
        private int trailPointCount;
        private readonly List<int> trailCountUpTo = new List<int>();
        private int trailShownIndex = -1;

        /// <summary>The camera, for the View buttons (preset viewpoints).</summary>
        public OrbitCamera Orbit => orbit;
        /// <summary>The recording being shown (a live run grows step by step), or null if none.</summary>
        public RunRecordingData Recording => recording;
        /// <summary>The playback state (null until a recording is open).</summary>
        public ReplayTimeline Timeline => timeline;
        /// <summary>The game pieces drawn for the recording, or null if it has none (older than version 4).</summary>
        public GamePieceView Pieces => pieceView;
        /// <summary>What is shown: a file name like "viewer-teamA-autonomous.json", "Live: Team A TeleOp", or null.</summary>
        public string ShownFileName => live != null ? "Live: " + liveTitle : shownFile == null ? null : Path.GetFileName(shownFile);
        /// <summary>Why the last action failed (open, connect, step), or null.</summary>
        public string ErrorMessage { get; private set; }
        /// <summary>A short note for the user, e.g. where a live run was saved, or null.</summary>
        public string Notice { get; private set; }
        /// <summary>The recordings in Harness/build/recordings, newest first (for the chooser).</summary>
        public List<string> RecordingsNewestFirst => recordingsNewestFirst;

        /// <summary>True while a live run is shown (running, or stopped and not yet replaced).</summary>
        public bool IsLive => live != null;
        /// <summary>The live session (null when not live), for the panels.</summary>
        public LiveSession Live => live;
        /// <summary>The OpModes the harness offered the last time the chooser asked, or null if it wasn't running.</summary>
        public List<LiveSession.OpModeChoice> LiveChoices { get; private set; }
        /// <summary>Why the live choices couldn't be listed (usually: the harness side isn't running), or null.</summary>
        public string LiveUnavailable { get; private set; }
        /// <summary>True once the live run has been saved.</summary>
        public bool LiveSaved { get; private set; }
        /// <summary>Where the Live mode button is: off, starting the harness side, running, or failed.</summary>
        public HarnessLauncher.LaunchState LiveModeState => launcher?.State ?? HarnessLauncher.LaunchState.Off;
        /// <summary>True if live mode is using a harness side someone started by hand.</summary>
        public bool LiveModeStartedOutside => launcher != null && launcher.StartedOutside;
        /// <summary>True if the next live run should be moved by Unity's physics (the Physics button, or U).</summary>
        public bool UnityPhysicsChosen => unityPhysicsChosen;
        /// <summary>Whether the AprilTag sight lines and the "Tags in view" card are shown (A key, Visuals).</summary>
        public bool ShowAprilTags => showAprilTags;

        /// <summary>Shows or hides what the webcams saw (A key, Visuals screen), and remembers it.</summary>
        public void SetShowAprilTags(bool show)
        {
            showAprilTags = show;
            PlayerPrefs.SetInt(AprilTagsPreferenceKey, show ? 1 : 0);
            if (timeline != null)
            {
                ShowPlayhead();
            }
        }
        /// <summary>The season field files that can be picked (Harness/fields), by id.</summary>
        public List<FieldLayoutData> Fields => fields;
        /// <summary>The arena drawn now: a field file's id, or <see cref="FieldLayouts.PlainFieldId"/>.</summary>
        public string FieldId => fieldId;
        /// <summary>The setting drawn now (see <see cref="SettingBuilder.All"/>).</summary>
        public string SettingId => settingId;
        /// <summary>The setting drawn now, by name ("Competition hall").</summary>
        public string SettingName => SettingBuilder.Find(settingId).name;
        /// <summary>The season field drawn now: its name ("BIOBUZZ 2026-2027"), or "Plain field".</summary>
        public string FieldName
        {
            get
            {
                FieldLayoutData field = fields.Find(f => f.id == fieldId);
                return field == null ? "Plain field" : field.name + " " + field.season;
            }
        }

        private void Start()
        {
            replayCamera = new GameObject("Replay camera").AddComponent<Camera>();
            replayCamera.clearFlags = CameraClearFlags.SolidColor;
            replayCamera.backgroundColor = HudTheme.Background;
            replayCamera.tag = "MainCamera";
            orbit = replayCamera.gameObject.AddComponent<OrbitCamera>();

            // Robot size is set again by each recording's drive model; 18 in until then.
            scene = FieldBuilder.Build(transform, 18f, 18f);
            orbit.followTarget = scene.robot;

            var fieldProblems = new List<string>();
            fields = FieldLayouts.LoadAll(FieldLayouts.DefaultFolder, fieldProblems);
            fieldProblems.ForEach(p => Debug.LogWarning("[HarnessReplay] " + p));
            // "-obstacles" on the command line starts with the ball obstacles drawn (the O key).
            showObstacles = Array.Exists(Environment.GetCommandLineArgs(),
                arg => string.Equals(arg, "-obstacles", StringComparison.OrdinalIgnoreCase));
            // "-field <id>" on the command line, else the field picked last time.
            string startField = FieldLayouts.FieldFromCommandLine(Environment.GetCommandLineArgs())
                ?? PlayerPrefs.GetString(FieldPreferenceKey, "");
            ShowField(FieldLayouts.Pick(null, startField, fields.ConvertAll(f => f.id)));
            // "-setting <id>" on the command line, else the setting picked last time.
            ShowSetting(SettingBuilder.SettingFromCommandLine(Environment.GetCommandLineArgs())
                ?? PlayerPrefs.GetString(SettingPreferenceKey, SettingBuilder.MissionControlId));

            unityPhysicsChosen = PlayerPrefs.GetInt(PhysicsPreferenceKey, 0) == 1;
            showAprilTags = PlayerPrefs.GetInt(AprilTagsPreferenceKey, 1) == 1;
            tagSights = new AprilTagSightView(transform);
            launcher = new HarnessLauncher(
                HarnessLauncher.ResolveHarnessFolder(Environment.GetCommandLineArgs(), Application.dataPath, Application.isEditor), bridgePort);

            RefreshRecordingList();
            // A recording named in the Inspector, else one given on the command line (dropped on
            // unity-view.bat), else the newest one.
            string fromCommandLine = RecordingLoader.FileFromCommandLine(Environment.GetCommandLineArgs());
            string first = !string.IsNullOrWhiteSpace(recordingPath) ? recordingPath
                : fromCommandLine ?? (recordingsNewestFirst.Count > 0 ? recordingsNewestFirst[0] : null);
            if (first == null)
            {
                ErrorMessage = "No recordings in " + RecordingLoader.DefaultFolder + ".\nRun the harness tests "
                    + "(.\\gradlew.bat test), save a run from the Swing viewer, or pick \"Live: ...\" above "
                    + "while .\\gradlew.bat unityBridge runs.";
            }
            else
            {
                Open(first);
                // "-at <seconds>": open the replay at that moment (for pictures and demos).
                double? at = SecondsFromCommandLine(Environment.GetCommandLineArgs(), "-at");
                if (at.HasValue && timeline != null)
                {
                    timeline.SeekSeconds(at.Value);
                }
            }
            // "-fpslog <file>": measure the frame rate, write it there and close (for checking heavy views).
            FrameRateLog.AttachIfAsked(gameObject, Environment.GetCommandLineArgs());
        }

        /// <summary>The number after <paramref name="option"/> on the command line (e.g. "-at 5.3"), or null.</summary>
        public static double? SecondsFromCommandLine(string[] args, string option)
        {
            for (int i = 0; i < args.Length - 1; i++)
            {
                if (string.Equals(args[i], option, StringComparison.OrdinalIgnoreCase)
                    && double.TryParse(args[i + 1], NumberStyles.Float, CultureInfo.InvariantCulture, out double seconds))
                {
                    return seconds;
                }
            }
            return null;
        }

        private void OnDestroy()
        {
            EndLive(null);
            launcher?.Dispose(); // ends the harness side if Live mode started it
            if (trailPoints.IsCreated)
            {
                trailPoints.Dispose();
            }
        }

        // ---- Replay -------------------------------------------------------------------------------

        /// <summary>Opens a recording file and shows its first moment. On failure, keeps the old one and shows why.</summary>
        public void Open(string path)
        {
            RunRecordingData loaded;
            try
            {
                loaded = RecordingLoader.Load(path);
            }
            catch (RecordingFormatException e)
            {
                ErrorMessage = e.Message;
                Debug.LogWarning("[HarnessReplay] " + e.Message);
                return;
            }
            EndLive(null);
            shownFile = path;
            Show(loaded);
        }

        /// <summary>Reads the recordings folder again (the chooser does this each time it opens).</summary>
        public void RefreshRecordingList()
        {
            recordingsNewestFirst = RecordingLoader.RecordingsNewestFirst(RecordingLoader.DefaultFolder);
        }

        /// <summary>Picks any recording file with a file dialog (in the editor; does nothing in a built player).</summary>
        public void OpenFilePicker()
        {
#if UNITY_EDITOR
            string picked = UnityEditor.EditorUtility.OpenFilePanel("Open run recording", RecordingLoader.DefaultFolder, "json");
            if (!string.IsNullOrEmpty(picked))
            {
                Open(picked);
            }
#endif
        }

        /// <summary>Shows a recording (a file, or a live run that will keep growing) from its first moment.</summary>
        private void Show(RunRecordingData shown)
        {
            int speed = timeline?.SpeedIndex ?? -1;
            recording = shown;
            timeline = new ReplayTimeline(recording);
            if (speed >= 0)
            {
                timeline.SetSpeedIndex(speed);
            }
            ErrorMessage = null;
            DriveModelData model = recording.header.driveModel;
            float length = model != null && model.length != null ? (float)model.length.value : 18f;
            float width = model != null && model.width != null ? (float)model.width.value : 18f;
            FieldBuilder.RebuildRobot(transform, scene, length, width);
            orbit.followTarget = scene.robot;
            if (recording.header.field != null)
            {
                ShowField(FieldLayouts.Pick(recording.header.field, fieldId, fields.ConvertAll(f => f.id)));
            }
            // The recording's game pieces (POLLEN, NECTAR), if it has any.
            pieceView?.Destroy();
            pieceView = null;
            pieceTrack = new GamePieceTrack(recording);
            if (pieceTrack.Start.Count > 0)
            {
                pieceView = new GamePieceView(transform, pieceTrack.Start);
            }
            ResetTrail();
            AddTrailPoints();
            ShowPlayhead();
        }

        // ---- Season field -------------------------------------------------------------------------

        /// <summary>Switches to the next season field (plain, then each file by id) and remembers it. Key G.</summary>
        public void NextField()
        {
            var ids = new List<string> { FieldLayouts.PlainFieldId };
            ids.AddRange(fields.ConvertAll(f => f.id));
            int next = (ids.IndexOf(fieldId) + 1) % ids.Count;
            ShowField(ids[next]);
            PlayerPrefs.SetString(FieldPreferenceKey, fieldId);
        }

        /// <summary>Picks the arena on the Visuals screen: a field file's id, or the plain field. Remembered.</summary>
        public void SelectField(string id)
        {
            ShowField(id);
            PlayerPrefs.SetString(FieldPreferenceKey, fieldId);
        }

        /// <summary>Picks the setting around the arena on the Visuals screen. Remembered.</summary>
        public void SelectSetting(string id)
        {
            ShowSetting(id);
            PlayerPrefs.SetString(SettingPreferenceKey, settingId);
        }

        /// <summary>Draws a setting (or none), replacing the one shown, and sets its sky and light.</summary>
        private void ShowSetting(string id)
        {
            if (settingRoot != null)
            {
                Destroy(settingRoot.gameObject);
                settingRoot = null;
            }
            SettingInfo setting = SettingBuilder.Find(id);
            settingId = setting.id;
            settingRoot = SettingBuilder.Build(transform, setting);
            SettingBuilder.ApplyLighting(setting, replayCamera, scene.sun);
        }

        /// <summary>Draws a season field by id (or only the plain field), replacing the one shown.</summary>
        private void ShowField(string id)
        {
            if (seasonRoot != null)
            {
                Destroy(seasonRoot.gameObject);
                seasonRoot = null;
            }
            FieldLayoutData field = fields.Find(f => f.id == id);
            fieldId = field == null ? FieldLayouts.PlainFieldId : field.id;
            if (field != null)
            {
                seasonRoot = FieldBuilder.BuildSeason(transform, field);
            }
            // A CAD field brings its own tiles and walls, so the plain ones step aside.
            FieldBuilder.ShowPlainField(scene, field == null || FieldBuilder.CadModels(field.id).Length == 0);
            ShowObstacles(showObstacles);
        }

        /// <summary>Whether the ball obstacles (O key) are drawn over the field.</summary>
        public bool ObstaclesShown => obstacleRoot != null;

        /// <summary>Shows or hides the shown field's ball obstacles (what the harness's balls bounce off).</summary>
        public void ShowObstacles(bool show)
        {
            showObstacles = show;
            if (obstacleRoot != null)
            {
                Destroy(obstacleRoot.gameObject);
                obstacleRoot = null;
            }
            FieldLayoutData field = fields.Find(f => f.id == fieldId);
            if (show && field != null && field.pieceObstacles.Count > 0)
            {
                obstacleRoot = FieldBuilder.BuildObstacles(transform, field);
            }
        }

        // ---- Live ---------------------------------------------------------------------------------

        /// <summary>
        /// Asks the harness which OpModes it can run (the chooser calls this when it opens). If the
        /// harness side isn't running, <see cref="LiveUnavailable"/> says how to start it.
        /// </summary>
        public void RefreshLiveChoices()
        {
            if (live != null && live.OpModes.Count > 0)
            {
                LiveChoices = live.OpModes;
                LiveUnavailable = null;
                return;
            }
            try
            {
                using (var probe = new LiveSession(new TcpBridgeTransport(bridgePort, 300)))
                {
                    probe.Hello();
                    LiveChoices = probe.OpModes;
                    LiveUnavailable = null;
                }
            }
            catch (Exception e) when (e is IOException || e is System.Net.Sockets.SocketException || e is BridgeErrorException)
            {
                LiveChoices = null;
                LiveUnavailable = LiveModeState == HarnessLauncher.LaunchState.Starting
                    ? "Live mode is starting the harness side (the first time takes up to a minute)..."
                    : "Live play: press Live mode first (or run .\\gradlew.bat unityBridge in Harness\\)";
            }
        }

        /// <summary>Connects to the harness, loads the OpMode (the harness reads the controllers) and presses INIT.</summary>
        public void StartLive(LiveSession.OpModeChoice choice)
        {
            EndLive(null);
            LiveSession session = null;
            try
            {
                session = new LiveSession(new TcpBridgeTransport(bridgePort));
                session.Hello();
                session.Load(choice.id, unityPhysicsChosen);
                session.Init();
            }
            catch (Exception e) when (e is IOException || e is System.Net.Sockets.SocketException || e is BridgeErrorException)
            {
                session?.Dispose();
                ErrorMessage = "Could not start live play: " + e.Message;
                return;
            }
            live = session;
            if (live.UnityPhysics && live.RobotPhysics != null)
            {
                // Unity's physics, with the walls and the solid parts of the field shown now.
                robotPhysics = new UnityRobotPhysics(live.RobotPhysics, fields.Find(f => f.id == fieldId));
            }
            liveTitle = choice.title + (robotPhysics != null ? ", Unity physics" : "");
            LiveSaved = false;
            Notice = null;
            shownFile = null;
            pacer.Reset();
            Show(live.Recording);
        }

        /// <summary>Presses PLAY in a live run (Enter).</summary>
        public void LivePlay()
        {
            LiveCall(() => live.Play(NextPhysicsState()));
        }

        /// <summary>Presses STOP in a live run (Esc).</summary>
        public void LiveStop()
        {
            LiveCall(() => live.Stop());
        }

        /// <summary>
        /// The Live mode button (L): switches on (starts the harness side in the background) or off
        /// (ends any live run and the harness side it started).
        /// </summary>
        public void ToggleLiveMode()
        {
            if (LiveModeState == HarnessLauncher.LaunchState.Starting || LiveModeState == HarnessLauncher.LaunchState.Running)
            {
                EndLive(null);
                launcher.Stop();
                Notice = "Live mode off";
                return;
            }
            ErrorMessage = null;
            launcher.Start(Time.realtimeSinceStartup);
            Notice = launcher.State == HarnessLauncher.LaunchState.Starting
                ? "Starting the harness side for live play (the first time takes up to a minute)..." : null;
        }

        /// <summary>Follows the Live mode launcher: says when it is ready, or why it failed.</summary>
        private void WatchLiveMode()
        {
            launcher.Poll(Time.realtimeSinceStartup);
            HarnessLauncher.LaunchState now = launcher.State;
            if (now == lastLaunchState)
            {
                return;
            }
            lastLaunchState = now;
            if (now == HarnessLauncher.LaunchState.Running)
            {
                RefreshLiveChoices();
                Notice = "Live mode on: open the Replay chooser and pick \"Live: ...\", then press Enter to PLAY";
            }
            else if (now == HarnessLauncher.LaunchState.Failed)
            {
                EndLive(null);
                Notice = null;
                ErrorMessage = "Live mode: " + launcher.Problem;
            }
        }

        /// <summary>
        /// Switches between the harness's physics and Unity's for the next live run (U). Not while a
        /// live run is going: its physics was chosen when it started.
        /// </summary>
        public void TogglePhysics()
        {
            if (live != null && (live.Initialized || live.Playing))
            {
                Notice = "Physics can change between live runs, not during one";
                return;
            }
            unityPhysicsChosen = !unityPhysicsChosen;
            PlayerPrefs.SetInt(PhysicsPreferenceKey, unityPhysicsChosen ? 1 : 0);
        }

        /// <summary>
        /// With Unity's physics: runs it one loop with the newest frame's drive command and returns
        /// where the robot ended up, for the next step or play. Null with the harness's own physics.
        /// </summary>
        private PhysicsStateData NextPhysicsState()
        {
            return robotPhysics?.Step(live.LastDriveCommand, live.LoopPeriodSeconds);
        }

        /// <summary>Saves the live run as a recording in Harness/build/recordings.</summary>
        public void SaveLive()
        {
            if (live == null)
            {
                return;
            }
            string name = "unity-live-" + live.OpModeId + "-" + DateTime.Now.ToString("yyyyMMdd-HHmmss", CultureInfo.InvariantCulture);
            try
            {
                string path = live.Save(name);
                LiveSaved = true;
                Notice = "Saved " + Path.GetFileName(path) + " (in build\\recordings; replay it from the chooser)";
            }
            catch (Exception e) when (e is IOException || e is BridgeErrorException)
            {
                ErrorMessage = "Could not save: " + e.Message;
            }
        }

        /// <summary>Runs one live request; on a lost connection or an OpMode crash, ends live play and keeps the run on screen.</summary>
        private void LiveCall(Action call)
        {
            if (live == null)
            {
                return;
            }
            try
            {
                call();
            }
            catch (BridgeErrorException e)
            {
                // A refused request, or the OpMode crashed (then the run is over but stays on screen).
                ErrorMessage = e.Message;
            }
            catch (Exception e) when (e is IOException || e is System.Net.Sockets.SocketException)
            {
                EndLive("Lost the harness connection (is .\\gradlew.bat unityBridge still running?): " + e.Message);
            }
            SyncLive();
        }

        /// <summary>Picks up new live loops: the timeline and trail grow, and the newest moment is shown.</summary>
        private void SyncLive()
        {
            timeline.Sync();
            timeline.Seek(timeline.Count - 1);
            AddTrailPoints();
        }

        /// <summary>Leaves live mode (closing the connection). The run stays on screen as a recording.</summary>
        private void EndLive(string why)
        {
            if (live == null)
            {
                return;
            }
            live.Dispose();
            live = null;
            robotPhysics?.Dispose();
            robotPhysics = null;
            if (why != null)
            {
                ErrorMessage = why;
            }
        }

        // ---- Every frame ------------------------------------------------------------------------

        private void Update()
        {
            // The 3D view only fills the field area of the window; the panels cover the rest.
            ReplayHud.Layout layout = ReplayHud.Layout.For(Screen.width, Screen.height);
            replayCamera.rect = new Rect(layout.field.x / Screen.width, (Screen.height - layout.field.yMax) / Screen.height,
                layout.field.width / Screen.width, layout.field.height / Screen.height);

            WatchLiveMode();
            HideCeilingFromAbove();
            // The setting's 360° sky shows in the 3D view; the top-down view keeps its plain background.
            replayCamera.clearFlags = !orbit.TopDown && RenderSettings.skybox != null ? CameraClearFlags.Skybox : CameraClearFlags.SolidColor;
            if (timeline == null)
            {
                return;
            }
            if (live != null && (live.Initialized || live.Playing))
            {
                // Live: one OpMode call per 20 ms of real time. Simulated time stays exact.
                int steps = pacer.StepsDue(Time.unscaledDeltaTime);
                for (int i = 0; i < steps && live != null && !live.Finished; i++)
                {
                    LiveCall(() => live.Step(NextPhysicsState()));
                }
            }
            else
            {
                timeline.Advance(Time.unscaledDeltaTime);
            }
            ShowPlayhead();
        }

        /// <summary>Hides a setting's ceiling while the camera is above it (zoomed out), so it never covers the field.</summary>
        private void HideCeilingFromAbove()
        {
            Transform ceiling = settingRoot == null ? null : settingRoot.Find(SettingBuilder.CeilingName);
            if (ceiling != null)
            {
                ceiling.gameObject.SetActive(replayCamera.transform.position.y < SettingBuilder.CeilingHideAboveMeters);
            }
        }

        /// <summary>Moves the robot to the (blended) pose at the playhead, colors its wheels, updates the trail.</summary>
        private void ShowPlayhead()
        {
            PoseData pose = timeline.PoseAtPlayhead();
            scene.robot.gameObject.SetActive(pose != null);
            if (pose != null)
            {
                FtcToUnity.Pose(pose, 0f, out Vector3 position, out Quaternion rotation);
                scene.robot.SetPositionAndRotation(position, rotation);
            }
            FieldBuilder.ShowPowers(scene, timeline.Current);
            pieceView?.Show(pieceTrack.At(timeline.Index));
            tagSights?.Show(timeline.Current, pose, showAprilTags);
            if (timeline.Index != trailShownIndex || timeline.Index >= trailCountUpTo.Count - 1)
            {
                int count = trailCountUpTo[Math.Min(timeline.Index, trailCountUpTo.Count - 1)];
                scene.trail.positionCount = count;
                if (count > 0)
                {
                    scene.trail.SetPositions(trailPoints.GetSubArray(0, count));
                }
                trailShownIndex = timeline.Index;
            }
        }

        // ---- Trail --------------------------------------------------------------------------------

        private void ResetTrail()
        {
            trailPointCount = 0;
            trailCountUpTo.Clear();
            trailShownIndex = -1;
        }

        /// <summary>Adds trail points for loops not yet added (all of a file at once; a live run step by step).</summary>
        private void AddTrailPoints()
        {
            for (int i = trailCountUpTo.Count; i < recording.loops.Count; i++)
            {
                PoseData pose = recording.loops[i].pose;
                if (pose != null)
                {
                    EnsureTrailCapacity(trailPointCount + 1);
                    trailPoints[trailPointCount++] = FtcToUnity.Position(pose.xInches, pose.yInches, 0.004f);
                }
                trailCountUpTo.Add(trailPointCount);
            }
        }

        /// <summary>Grows the trail's point store (doubling), keeping the points so far.</summary>
        private void EnsureTrailCapacity(int needed)
        {
            if (trailPoints.IsCreated && trailPoints.Length >= needed)
            {
                return;
            }
            int capacity = Math.Max(needed, trailPoints.IsCreated ? trailPoints.Length * 2 : 8192);
            var bigger = new NativeArray<Vector3>(capacity, Allocator.Persistent);
            if (trailPoints.IsCreated)
            {
                NativeArray<Vector3>.Copy(trailPoints, bigger, trailPointCount);
                trailPoints.Dispose();
            }
            trailPoints = bigger;
        }

        // ---- Keys and panels ----------------------------------------------------------------------

        /// <summary>Opens the next newer (step -1) or older (step +1) recording in the folder.</summary>
        private void OpenNeighbour(int step)
        {
            RefreshRecordingList();
            if (recordingsNewestFirst.Count == 0)
            {
                return;
            }
            int current = shownFile == null ? -1 : recordingsNewestFirst.FindIndex(f =>
                string.Equals(Path.GetFullPath(f), Path.GetFullPath(shownFile), StringComparison.OrdinalIgnoreCase));
            int next = current < 0 ? 0 : (current + step + recordingsNewestFirst.Count) % recordingsNewestFirst.Count;
            Open(recordingsNewestFirst[next]);
        }

        private void OnGUI()
        {
            HandleKeys(Event.current);
            hud.Draw(this, ReplayHud.Layout.For(Screen.width, Screen.height));
        }

        private void HandleKeys(Event e)
        {
            if (e.type != EventType.KeyDown)
            {
                return;
            }
            bool liveRunning = live != null && (live.Initialized || live.Playing);
            bool used = true;
            switch (e.keyCode)
            {
                case KeyCode.Return:
                case KeyCode.KeypadEnter:
                    if (live != null && live.Initialized) LivePlay(); else used = false;
                    break;
                case KeyCode.Escape:
                    // Esc closes the Visuals screen first; otherwise it is STOP in a live run.
                    if (hud.VisualsOpen) hud.VisualsOpen = false; else if (liveRunning) LiveStop(); else used = false;
                    break;
                case KeyCode.V:
                    hud.VisualsOpen = !hud.VisualsOpen;
                    break;
                case KeyCode.N:
                    if (!liveRunning) OpenNeighbour(-1);
                    break;
                case KeyCode.P:
                    if (!liveRunning) OpenNeighbour(+1);
                    break;
                case KeyCode.G:
                    NextField();
                    break;
                case KeyCode.U:
                    TogglePhysics();
                    break;
                case KeyCode.L:
                    ToggleLiveMode();
                    break;
                case KeyCode.O:
                    ShowObstacles(!showObstacles);
                    break;
                case KeyCode.A:
                    SetShowAprilTags(!showAprilTags);
                    break;
                default:
                    // While a live run is going, the timeline always follows the newest moment.
                    used = !liveRunning && timeline != null && HandlePlaybackKey(e);
                    break;
            }
            if (used)
            {
                e.Use();
            }
        }

        private bool HandlePlaybackKey(Event e)
        {
            switch (e.keyCode)
            {
                case KeyCode.Space: timeline.TogglePlay(); return true;
                case KeyCode.LeftArrow: if (e.shift) timeline.JumpSeconds(-1); else timeline.Step(-1); return true;
                case KeyCode.RightArrow: if (e.shift) timeline.JumpSeconds(1); else timeline.Step(1); return true;
                case KeyCode.Home: timeline.Seek(0); return true;
                case KeyCode.End: timeline.Seek(timeline.Count - 1); return true;
                case KeyCode.LeftBracket: case KeyCode.Minus: timeline.Slower(); return true;
                case KeyCode.RightBracket: case KeyCode.Equals: timeline.Faster(); return true;
                default: return false;
            }
        }
    }
}

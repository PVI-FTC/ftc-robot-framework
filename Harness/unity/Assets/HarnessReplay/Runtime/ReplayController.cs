using System;
using System.Collections.Generic;
using System.IO;
using Unity.Collections;
using UnityEngine;

namespace PviHarness.Replay
{
    /// <summary>
    /// Replays a harness run recording in 3D (Stage U1), in a window laid out like the Swing
    /// viewer: header with the mission clock, a recording chooser, the field, the "mission
    /// control" cards and gamepads on the right, and the playback bar at the bottom
    /// (<see cref="ReplayHud"/>). Put it on one empty GameObject (the Replay scene already has it)
    /// and press Play: it builds the field, opens the newest recording in Harness/build/recordings
    /// (or <see cref="recordingPath"/>), and plays it back.
    ///
    /// <para>Keys: Space play/pause, Left/Right one loop, Shift+Left/Right one second, Home/End,
    /// [ and ] slower/faster, N/P newer/older recording, T top-down / 3D view, F follow the robot.
    /// In 3D view, right-drag turns the camera and scroll zooms.</para>
    ///
    /// <para>It only reads the saved file. Nothing here talks to the harness (that is Stage U2).</para>
    /// </summary>
    public sealed class ReplayController : MonoBehaviour
    {
        [Tooltip("A recording to open. Leave empty to open the newest file in Harness/build/recordings.")]
        [SerializeField] private string recordingPath = "";

        private FieldBuilder.Built scene;
        private Camera replayCamera;
        private OrbitCamera orbit;
        private readonly ReplayHud hud = new ReplayHud();
        private RunRecordingData recording;
        private ReplayTimeline timeline;
        private string shownFile;
        private List<string> recordingsNewestFirst = new List<string>();

        // Trail: every recorded pose as a Unity position, and how many belong to loops up to each index.
        private NativeArray<Vector3> trailPoints;
        private int[] trailCountUpTo;
        private int trailShownIndex = -1;

        /// <summary>The recording being shown, or null if none could be opened.</summary>
        public RunRecordingData Recording => recording;
        /// <summary>The playback state (null until a recording is open).</summary>
        public ReplayTimeline Timeline => timeline;
        /// <summary>The shown file's name, e.g. "viewer-teamA-autonomous.json", or null.</summary>
        public string ShownFileName => shownFile == null ? null : Path.GetFileName(shownFile);
        /// <summary>Why the last recording couldn't be opened, or null.</summary>
        public string ErrorMessage { get; private set; }
        /// <summary>The recordings in Harness/build/recordings, newest first (for the chooser).</summary>
        public List<string> RecordingsNewestFirst => recordingsNewestFirst;

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

            RefreshRecordingList();
            string first = !string.IsNullOrWhiteSpace(recordingPath) ? recordingPath
                : recordingsNewestFirst.Count > 0 ? recordingsNewestFirst[0] : null;
            if (first == null)
            {
                ErrorMessage = "No recordings in " + RecordingLoader.DefaultFolder + ".\nRun the harness tests "
                    + "(.\\gradlew.bat test) or save a run from the Swing viewer, then press N.";
            }
            else
            {
                Open(first);
            }
        }

        private void OnDestroy()
        {
            if (trailPoints.IsCreated)
            {
                trailPoints.Dispose();
            }
        }

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
            int speed = timeline?.SpeedIndex ?? -1;
            recording = loaded;
            timeline = new ReplayTimeline(recording);
            if (speed >= 0)
            {
                timeline.SetSpeedIndex(speed);
            }
            shownFile = path;
            ErrorMessage = null;

            DriveModelData model = recording.header.driveModel;
            float length = model != null && model.length != null ? (float)model.length.value : 18f;
            float width = model != null && model.width != null ? (float)model.width.value : 18f;
            FieldBuilder.RebuildRobot(transform, scene, length, width);
            orbit.followTarget = scene.robot;
            PrepareTrail();
            ShowPlayhead();
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

        private void PrepareTrail()
        {
            if (trailPoints.IsCreated)
            {
                trailPoints.Dispose();
            }
            var points = new List<Vector3>();
            trailCountUpTo = new int[recording.loops.Count];
            for (int i = 0; i < recording.loops.Count; i++)
            {
                PoseData pose = recording.loops[i].pose;
                if (pose != null)
                {
                    points.Add(FtcToUnity.Position(pose.xInches, pose.yInches, 0.004f));
                }
                trailCountUpTo[i] = points.Count;
            }
            trailPoints = new NativeArray<Vector3>(points.ToArray(), Allocator.Persistent);
            trailShownIndex = -1;
        }

        private void Update()
        {
            // The 3D view only fills the field area of the window; the panels cover the rest.
            ReplayHud.Layout layout = ReplayHud.Layout.For(Screen.width, Screen.height);
            replayCamera.rect = new Rect(layout.field.x / Screen.width, (Screen.height - layout.field.yMax) / Screen.height,
                layout.field.width / Screen.width, layout.field.height / Screen.height);

            if (timeline == null)
            {
                return;
            }
            timeline.Advance(Time.unscaledDeltaTime);
            ShowPlayhead();
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
            if (timeline.Index != trailShownIndex)
            {
                int count = trailCountUpTo[timeline.Index];
                scene.trail.positionCount = count;
                if (count > 0)
                {
                    scene.trail.SetPositions(trailPoints.GetSubArray(0, count));
                }
                trailShownIndex = timeline.Index;
            }
        }

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
            bool used = true;
            switch (e.keyCode)
            {
                case KeyCode.N: OpenNeighbour(-1); break;
                case KeyCode.P: OpenNeighbour(+1); break;
                default: used = timeline != null && HandlePlaybackKey(e); break;
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

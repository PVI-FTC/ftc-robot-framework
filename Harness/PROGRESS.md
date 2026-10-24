# Harness progress

Where the FTC test harness stands, what is being worked on and what comes next. Update this file in
the same change as the work (see [ai-instructions/03-workflow.md](ai-instructions/03-workflow.md)).

**Last updated:** 2026-10-24
**Test status:** 219 tests in 35 classes, all passing (`.\gradlew.bat test`, 2026-10-24, with the bots' uncommitted work)
**Git:** committed on branch `Team2-Testing` (local only, not pushed)
**Next up:** position estimator Step 15 (opt-in switches; decide their behavior with the user)

Status key: ✅ done · 🟡 partly done · ⏳ next · ⬜ not started · 💤 later / optional

---

## Harness phases

| Phase | What | Status |
|---|---|---|
| 0 | Scope and audit of the code (devices, hardware access points) | ✅ Done |
| 1 | Feasibility check: standalone build in `Harness/`, compile TeamCode, fake motors / hardware map / telemetry, first TeleOp test | ✅ Done |
| 2 | Scripted gamepads, full telemetry, full-match runs, crash capture, rumble recording, Pedro menu support, robot configs | ✅ Done |
| 3 | Java agent + simulated clock, so timed autonomous and match-timer rumbles are testable | ✅ Done |
| 4 | Vision: scripted AprilTag detections to test the `VisionSubsystem` state machine and `TeamAAprilTagVisionTestOpMode` | ✅ Done 2026-10-24 (`FakeWebcam`, `AprilTagScript`, `SeenAprilTag`, `FakeHardwareMap.addWebcam`; harness copies of SDK `VisionPortal` and `AprilTagProcessorImpl`; runner steps webcams each loop; `AprilTagVisionTest`, 12 tests) |
| 5 | Physics and Pinpoint: acceleration, fake encoders, fake Pinpoint so Pedro paths run, `PhysicsBackend` interface | ✅ Done 2026-10-24: momentum physics behind `PhysicsBackend`, encoders and Pinpoint read the physics; Team A's three routes finish within 0.25 in (`PedroRouteTest`). Response time and traction limit are placeholders (Step 10) |
| 6 | Logging, README updates, optional GitHub Actions CI | 🟡 README done; logging and CI not started |
| 7 | Unity bridge: Unity replaces the physics backend over a socket (planned in three stages, U1–U3, [below](#unity-phase-7-in-three-stages)) | ⬜ Not started |

---

## Position estimator + viewer steps

| Step | What | Status |
|---|---|---|
| 0 | Decisions: speeds (62.61 / 49.76 in/s), turn-rate placeholder, FTC center coordinates, 18 × 18 in size, feet | ✅ Done |
| 1 | `Pose` and `DisplayUnit`: positions, feet/inches, heading wrap | ✅ Done |
| 2 | `DriveModel`: robot speeds with sources, read live from the Pedro config | ✅ Done |
| 3 | `WheelReader`: which way each wheel really pushes, plus a miswiring option | ✅ Done |
| 4 | `MecanumKinematics`: four wheels into whole-robot motion, checked against the real controller | ✅ Done |
| 5 | `PoseEstimator`: adds up movement every loop | ✅ Done |
| 6 | Report printing | ✅ Done |
| (extra) | Automatic final-position report after **every** test; tests always rerun; `deg` instead of `°` | ✅ Done |
| 7 | More real-code motion tests: turning, a miswired motor causing drift, STOP freezing position, on-field check (`RealCodeMotionTest`) | ✅ Done |
| 8 | Save the track as a CSV for graphing (`TrackCsv`, `saveTrackCsv`; format v1 in README) | ✅ Done |
| 9 | README section for the estimator ("How motion is simulated, start to finish": loop order diagram, momentum, walls, encoders, Pinpoint, placeholders) | ✅ Done 2026-10-24 |
| 10 | Calibrate at the robot: measure turn rate and size, compare with a real autonomous run (team task). Checklist ready: [CALIBRATION.md](CALIBRATION.md) (size, turn rate, response time, traction limit; which `ModelValue` each replaces; how to compare a real route run) | ⬜ Ready for the team |
| 11 | Full run recording (pose, motor powers, step names, telemetry per loop) saved as JSON (`RunRecorder`, format v1 in README) | ✅ Done |
| 12 | Field drawing: 12 × 12 ft grid, robot box, heading arrow, trail (`viewer/FieldPainter`, PNG via `FieldImage`) | ✅ Done |
| 13 | Viewer window (Swing) with a side panel (`ViewerWindow`, `SidePanelText`, `gradlew viewRecording`) | ✅ Done |
| 14 | Playback controls: play/pause, speed, timeline slider, keyboard (`Playback`) | ✅ Done |
| 15 | Opt-in switches: `-Pview`, `simView`, `viewRecording`; no windows during normal runs (`viewRecording` exists from Step 13; behavior still to confirm) | ⏳ Next |
| 16 | Extras: HTML replay, Pedro path overlay, Panels feasibility check | 💤 Optional |
| 17 | Phase 5 upgrades: acceleration, encoders, fake Pinpoint, `PhysicsBackend` (encoders and fake Pinpoint 2026-10-23; momentum `Dynamics`, `PhysicsBackend` + `MomentumPhysics`, physics-driven drive encoders, `Pose.fromPedro`, route tests 2026-10-24) | ✅ Done |

---

## Drive and record a test

Drive the robot live in the viewer window (keyboard first, then a USB or wireless controller). The
recorded inputs become a new test. Agreed with the user on 2026-10-18.

| Step | What | Status |
|---|---|---|
| D1 | One gamepad input entry point in the core: scripts, live input (and later Unity) all feed `gamepad1`/`gamepad2` through it ([02](ai-instructions/02-unity-readiness.md) §5) (`ds/GamepadSource`, `runner.gamepad1(source)`) | ✅ Done |
| D2 | Live driving in the viewer with the **keyboard** (sticks on WASD / arrows, buttons on letter keys). Real-time pacing lives in the viewer; simulated time is unchanged (`LiveDrive`, `KeyboardGamepads`, "Drive: ..." in the dropdown; layout approved 2026-10-23) | ✅ Done |
| D3 | Converter: recorded gamepad timeline into `GamepadScript` steps (only the moments something changed, snapped to the 20 ms loop) (`recorded/RecordedInputs`: press + release = `hold`, sticks rounded to 2 decimals, 0.05 step and dead zone, notes for marked moments) | ✅ Done |
| D4 | Save as a new **generated `.java` test file** that students can open and edit: the `GamepadScript` from D3, automatic safe checks (no crash, powers within -1..1, robot on the field), a marked place for your own checks, optional snapshot checks (`recorded/RecordedTestWriter`, `DriveChecks`, `gradlew makeTest`; checks the drive already fails are written turned off with the reason; snapshots come from replaying the test's own inputs). Viewer **SAVE AS TEST** button (next to REPLAYS...; asks for the class name, works for any run on screen, shows the run command; `viewer/SaveAsTest`, UI chosen by the user 2026-10-24) | ✅ Done |
| D5 | **Jamepad** controllers: Xbox, PlayStation (DualShock 4 / DualSense) and Logitech F310/F710 (X mode), USB or wireless; mapped to the FTC `Gamepad` (cross = A, circle = B, square = X, triangle = Y; stick up is negative); keyboard fallback when none is connected (`JamepadControllers` with Jamepad 2.30.0.0, `ControllerReading`, `LiveGamepads`) | ✅ Done (tried with a PS5 controller, 2026-10-23) |
| D6 | Wireless disconnects: release every input, mark the moment in the recording and tell the user (recording format v2 `events`, beep + message in the viewer) | ✅ Done |

Decisions:
- **Jamepad approved** as a dependency (2026-10-18), for USB and wireless controllers. Pin an
  exact version when D5 starts. It belongs to the `viewer` source set only, never the core or
  TeamCode build.
- **Keyboard first** (D2), Jamepad after (D5). The keyboard stays as the fallback.
- **Generated `.java` tests** (D4, decided 2026-10-18): each recorded drive becomes its own
  readable test class in `src/test/java/pvi/harness/`, in the same style as hand-written tests
  (script, set up, run, check). Students edit it like any other test. It is not a JSON scenario
  file.

## Unity (Phase 7) in three stages

Planned with the user on 2026-10-23. Each stage builds on the last; nothing here is built yet.

| Stage | What | Status |
|---|---|---|
| U1 | **Unity replays saved run recordings.** Unity reads the same JSON files as the Swing viewer (format v2) and animates a robot on a 3D field. No harness Java changes needed. | ✅ Done (2026-10-23) |
| U2 | **Unity live.** The harness runs TeamCode and opens a local socket (`pvi.harness.bridge`, `gradlew unityBridge`); each step it sends the loop snapshot. **The harness reads the game controllers and Unity only displays** (user's decision, 2026-10-24), through the D1 gamepad entry point. Unity asks for each step, so simulated time stays exact. | ✅ Done 2026-10-24 (bridge message format v2; Unity "Live: ..." entries, Enter = PLAY, Esc = STOP, Save) |
| U3 | **Unity becomes the physics.** Behind the `PhysicsBackend` interface (Step 17): wheel commands to Unity, pose and speed back. Opt-in per live run (`"physics": "unity"`, Unity's **Physics:** button / U); the harness's own physics stays the default and is what every test uses. Walls and the season field's solid parts block the robot (PhysX); game pieces and pushing robots are later work. | 🟡 Done for driving and blocking 2026-10-24 (bridge message format v3; `RemotePhysics`, `UnityRobotPhysics`, `DriveLaw`) |

Stage U1 in steps (Unity side unless marked):

| Step | What | Status |
|---|---|---|
| U1.0 | Decisions: where the Unity project lives (**decided: `Harness/unity/`**, 2026-10-23); Unity version (**6000.3.14f1**, the one installed); JSON library (**Newtonsoft**, chosen at setup, can still change); FTC→Unity mapping in [02](ai-instructions/02-unity-readiness.md) §6 (**confirmed** by the user, 2026-10-23) | ✅ Done |
| U1.1 | Unity project set up in `Harness/unity/`, JSON library installed; Unity's generated folders (`Library/`, `Temp/`, `Logs/`, `obj/`, `UserSettings/`, `Build/`) added to `Harness/.gitignore` (opened cleanly in Unity batch mode; packages: Newtonsoft JSON 3.2.2, Test Framework 1.6.0, AI Assistant 2.17.0-pre.1) | ✅ Done |
| U1.2 | C# data classes matching run recording format v2 (v1 files: no `events`) (`RecordingData.cs`) | ✅ Done |
| U1.3 | Loader: open a file, check `header.version` is 1 or 2, clear errors (`RecordingLoader.cs`) | ✅ Done |
| U1.4 | `FtcToUnity`: the one place that converts inches/radians to Unity metres/degrees, with tests (`FtcToUnity.cs`, mapping as proposed in 02 §6) | ✅ Done |
| U1.5 | Scene: 3.66 m field with 2 ft tiles and walls; robot box sized from `header.driveModel` (placeholder sizes marked) (`FieldBuilder.cs`, scene `Replay.unity`) | ✅ Done |
| U1.6 | Playback: timeline on `sinceInitSeconds`, play/pause/speed/slider, smooth movement between 20 ms loops (`ReplayTimeline.cs`, `ReplayController.cs`, `OrbitCamera.cs`) | ✅ Done |
| U1.7 | Overlays: trail, telemetry screen for the moment, gamepads, rumbles and events (`ReplayPanelText.cs`; gamepads shown as text for now, not pictures) | ✅ Done |
| U1.8 | Tests (Unity Test Framework): parse a sample recording, conversion numbers, playback timing (18 EditMode tests, **Harness → Run Replay Tests**, each break checked) | ✅ Done |
| U1.9 | (Harness) README section "Watching a recording in Unity"; a small sample recording the Unity tests can use (README sections; sample `Tests/Samples/teamA-teleop-turn.json`) | ✅ Done |
| U1.10 | **Standalone Unity replay app**: open the Unity replay by double-clicking, without the Unity Editor, like `view.bat` (`unity-view.bat`, `buildUnity.bat`, **Harness → Build Windows App**, `RecordingLoader.ResolveFolder`; 3 `RecordingFolderTests`). Sub-plan below | ✅ Done 2026-10-24 |
| U1.11 | **Season field files** (C9): `fields/biobuzz-2026.json` and `fields/decode-2025.json` (format `pvi-harness-field` v1, README "Season field files"), drawn by Unity with a **Field:** button / G / `-field <id>` (`FieldLayout.cs`, `FieldBuilder.BuildSeason`; 10 `FieldLayoutTests`). DECODE parts other than its goal tags still to add from the DECODE manual; BIOBUZZ `approximate` numbers to check against official CAD; recording header `field` not written by the harness yet | 🟡 Done for BIOBUZZ 2026-10-24 |

**U1.10 sub-plan: standalone replay app** (requested by the user 2026-10-23; done 2026-10-24):

1. **Find recordings outside the Editor.** `RecordingLoader.DefaultFolder` is
   `Application.dataPath/../../build/recordings`, right in the Editor (`Harness/unity/Assets`) but
   wrong in a built player (`Build/HarnessReplay_Data`). New rule, in order: a command-line
   argument (`-recordings <folder>` or a recording file path, via
   `Environment.GetCommandLineArgs`), else relative to the .exe (`Build/` → `../../build/recordings`),
   else the Editor path. Add a Unity test that the rule gives the right folder for both layouts.
2. **Pick files without the Editor.** The "Replays..." button uses `EditorUtility.OpenFilePanel`
   (Editor only). The built app keeps the in-app **Replay** chooser (it already lists
   `build/recordings`, and N/P step through them) and opens a `.json` passed on the command line;
   hide or replace "Replays..." in the player.
3. **Build a Windows player:** `HarnessReplay.exe` + `HarnessReplay_Data` in `Harness/unity/Build/`
   (already ignored by git; usually 100+ MB, never commit it). Either from the Editor (Build
   Profiles → Windows) or headless with `buildUnity.bat` calling
   `Unity.exe -batchmode -quit -projectPath unity -executeMethod <small Editor build method>`.
   Building needs Unity installed and signed in (Personal licence); running the .exe needs
   nothing. The .exe must be rebuilt after Unity script changes.
4. **Launcher `Harness/unity-view.bat`:** runs the built .exe, passes a dropped `.json` as the
   argument, and offers to build first if the .exe is missing.
5. **Docs:** README "Watching a recording in Unity" and `unity/README.md`.

Done when: replaying `viewer-teamA-autonomous.json` in Unity shows the robot about 9.4 in
(0.24 m) forward at the end, matching the Swing viewer at the same moment.

Decided: the Unity project lives in **`Harness/unity/`**, next to the recordings it reads
(user, 2026-10-23). It is part of the harness, so the Harness rules cover it, and Gradle ignores
it (Gradle only builds `src/`).

Open decisions (ask the user): message format for U2 (likely one JSON message per line over TCP,
with a `version`), and whether Unity drives the clock in U2 (recommended). The unit mapping was
confirmed on 2026-10-23.

The Unity project also includes Unity's **AI Assistant** package (`com.unity.ai.assistant`), whose
MCP bridge is what Claude Code's `unity-mcp` tool connects to while the editor is open (the same
setup as the user's other Unity project). It is optional; see `unity/README.md`.

How the steps fit the rules in `ai-instructions/`:
- **Step 11's recording** is the shared plain-data snapshot that both the desktop viewer and the
  Unity bridge read ([02](ai-instructions/02-unity-readiness.md) §4,
  [05](ai-instructions/05-desktop-viewer.md) §1).
- **Steps 12–15** are the desktop viewer ([05-desktop-viewer.md](ai-instructions/05-desktop-viewer.md)).
- **Step 17's `PhysicsBackend`** is the seam Unity plugs into in Phase 7.

---

## Known issues and housekeeping

- `SimTime` allows one active clock per process, so only one runner can loop at a time.
- `Thread.sleep()` and code outside the rewritten packages (including `java.*`) still use the real
  clock.
- Motor encoder numbers (537.7 counts per turn, 312 RPM) are placeholders until the team gives real motor types.
- Pedro route accuracy depends on placeholder physics (response time, traction limit, turn rate, robot size):
  routes end within 0.25 in today, but match the real robot only after calibration ([CALIBRATION.md](CALIBRATION.md)).
  Route tests must start the robot at the route's start (`Pose.fromPedro`), or the walls are in the wrong place.
- `Harness/build.gradle` library versions are copied by hand from `../build.dependencies.gradle`.
- `RobotConfigs.teamB()` and `teamC()` are copies of Team A for now. Checked 2026-10-24 (A3): `TeamBRobot` and
  `TeamCRobot` still use the shared `RobotHardware` (drive motors `frontLeft`/`frontRight`/`rearLeft`/`rearRight`
  and `intake`; `VisionHardware` looks up no device) and carry "replace after Team B/C hardware is known" TODOs,
  so the configs stay as they are. (They also include Team A's `pinpoint`, which B and C never look up.) Revisit
  when either team gets its own hardware wrappers.

---

## Log

Newest first. One line per change: date, what changed, test result.

| Date | Change | Tests |
|---|---|---|
| 2026-10-24 | **W2**: momentum in the Swing viewer: `RecordingView.motion(index)` (speed, speed change, turn rate from the recorded poses; no format change needed) and `wallHitWithin(index, s)` (from the wall-hit events); a **Motion** card in `TelemetryPanel` (speed, turning, speeding-up/slowing-down bar) after TestBot's cards; the wall just hit **flashes red** for 0.4 s (`FieldPainter`, `FieldPanel`); motion line in `SidePanelText`. `RecordingMotionTest` (3) + 1 `RobotPartsDrawingTest`, each checked to fail when broken | viewer tests pass |
| 2026-10-24 | **C10 / U3: Unity physics** (opt-in). Bridge **message format v3**: `load` takes `"physics": "unity"`; `loaded` adds `physics` and `robotPhysics` (speeds, size, momentum numbers, start); each `frame` adds `driveCommand` (wheel pushes + braking); `step`/`play` must then carry `physicsState` (pose, robot-frame speed, wall contact). `RemotePhysics` (bridge) implements `PhysicsBackend` and is plugged in with the public `PoseEstimator(model, wheels, start, backend)` + `usePositionEstimator`, so **no core file changed**. Unity: `DriveLaw` (MomentumPhysics' rules in C#), `UnityRobotPhysics` (PhysX Rigidbody robot, invisible walls and field-part colliders from `fields/*.json`, stepped by script 20 ms at a time), `LiveSession` v3, **Physics:** button / U key. Unity replay now also reads run recording format v3 (TestBot's T3 adds `aprilTags` per loop; not drawn yet). `BridgeUnityPhysicsTest` (5), `UnityRobotPhysicsTests` (6), a LiveSession test and a v3 loader test; each break (supply ignored, no drive command, state not required, harness physics kept; no traction limit, speed ignores PhysX, field parts ignored, state not sent) failed exactly its test | Bridge 22/22; Unity 52/52 |
| 2026-10-24 | **T1** `GETTING_STARTED.md` (five steps for students, linked from the README top). **T2** `PedroRouteTest` prints each route's time from PLAY to "Sequence Complete" (LP-11 0.96 s, DECODE 12.86 s, Curving 19.56 s; printed, not checked); route-time table added to CALIBRATION.md (manager and WorkerBot OK). **T3** run recording **format v3**: each loop's `aprilTags` (camera, id, ftcPose-frame inches/degrees; older files load with none), `RunRecording.AprilTagState`, side panel **Tags in view** card (only for runs that saw a tag); README format section updated; UnityBot told (its loader accepts v1-2 only, and `BridgeSessionTest` expects the old loop keys). 6 new tests (`AprilTagVisionTest` +3, `TagsInViewCardTest` 3); breaking the recorder, the JSON writer or the card each failed tests | 235/236 (39 classes): only UnityBot's `BridgeSessionTest.framesUseTheRecordingLoopLayout`, which needs `aprilTags` in its expected keys |
| 2026-10-24 | **W3**: battery voltage in the physics: wheel pushes scale with battery volts / the 12.5 V the speeds were measured at (`DriveModel.withSpeedVoltage`, placeholder); `FakeVoltageSensor.setSag(voltsPerMinute)` drops with simulated time (floor 9 V); `PhysicsBackend` unchanged. `BatteryTest` (4): 11 V drives 88% as far, sag over 60 s, Pedro's voltage compensation reads the battery, DECODE still finishes at 11 V; checked to fail when broken | physics tests pass |
| 2026-10-24 | **W1 / Step 9**: README section "How motion is simulated, start to finish" for students (one loop step by step with a text diagram, MomentumPhysics, walls, sensors, placeholders table, what isn't simulated); replaces the shorter physics paragraph | docs only |
| 2026-10-24 | **C9 season fields**: `fields/biobuzz-2026.json` from the BIOBUZZ Competition Manual TU06 (Section 9, Figure 10-2) and the SDK's `getBioBuzzTagLibrary()` (16 tags in 4 clusters, cluster offsets copied exactly; the SDK has no field pose for the clusters because the HIVES tip, so starting poses are worked out from the manual and marked approximate); `fields/decode-2025.json` with the SDK's DECODE tags (20/24 exact, 21-23 no position). Frame: inverted square (+x audience, +y red to blue) per ftc-docs. Unity: `FieldLayout.cs` (data, loader, folder rule, pick order recording > `-field` / last choice > plain), `FieldBuilder.BuildSeason` (boxes, cylinders, tags), `FtcToUnity.Rotation(heading, tilt)` / `Direction`, **Field:** button and G key, optional header `field`. Checked in the built app: BIOBUZZ drawn where the manual shows it. 10 `FieldLayoutTests`; 5 breaks (tilt sign, pick order, a tag's SDK offset, a tag's position, player folder) each failed exactly its test | Unity 44/44; Java not rerun (no Java change) |
| 2026-10-24 | **Phase 4 done (B4)**: runner steps `FakeWebcam`s each loop (line added by WorkerBot), `FakeHardwareMap.addWebcam/webcam`; temporary test hook removed; README §6 vision ✅. **B5**: took over WorkerBot's `PedroRouteTest` (LP-11 0.24 in, DECODE 0.10 in, Curving 0.25 in / 1.6°, all within 1 in / 2°, plus a miswired robot that misses); starts and ends now read from TeamCode's `TeamAPedroPathRoute` instead of copied numbers, routes picked by enum; README §6 route accuracy ✅. Break-check: starting every route at the field center failed DECODE (17 in off) and Curving (unfinished) | 219/219 pass (35 classes) |
| 2026-10-24 | **B6 / D4 viewer button**: SAVE AS TEST next to REPLAYS... (class-name dialog, any run on screen, off while driving, saves off the Swing thread, shows the run command with Copy command); `viewer/SaveAsTest`. `SaveAsTestTest` (4), break-checked. **B4 / Phase 4 scripted AprilTags**: `hardware/FakeWebcam` (WebcamName, 30 pictures/s of sim time, timestamps on robot code's nanoTime clock), `AprilTagScript` (see/lose/loseAll/duringInit), `SeenAprilTag` (camera-relative inches, ftcPose frame); harness copies of SDK `VisionPortal` and `AprilTagProcessorImpl` (real SDK builders, tag library and pose math). `AprilTagVisionTest` (12, real TeamAAprilTagVisionTestOpMode: state machine Searching→TargetAcquired→Tracking→LostTarget→Searching, tag 22 robot pose through Team A's mount, goal-tag field candidate accepted at the right distance); 4 breaks each failed their tests. Runner line still to add (WorkerBot), so the test uses a marked temporary hook | 219/219 pass (35 classes, with the other bots' uncommitted work) |
| 2026-10-24 | **U1.10 done (C8)**: stand-alone Windows replay app. `RecordingLoader.ResolveFolder` (`-recordings <folder>`, else a dropped `.json`'s folder, else `../../build/recordings` in the Editor or `../../../build/recordings` next to the .exe) and `FileFromCommandLine`; the app opens a `.json` given on the command line; **Replays...** is Editor-only. `BuildWindowsApp` (**Harness → Build Windows App** / `BuildFromCommandLine`: windowed, resizable 1600×1000, runs in background, adds the four code-used shaders to Always Included Shaders), `buildUnity.bat`, `unity-view.bat`. Built (`unity/Build/`, 87 MB, ignored) and opened through `unity-view.bat`: it found `build/recordings` and replayed the newest recording. First build showed a magenta field (shaders stripped), fixed. 3 `RecordingFolderTests`; breaking the player rule failed exactly `BuiltAppLayoutFindsHarnessBuildRecordings` | Unity 34/34; Java not rerun (no Java change) |
| 2026-10-24 | **U2 done (C7)**: live play in Unity. User decided the harness reads the controllers. Bridge **message format v2**: `load` takes `"input": "controllers"` (viewer's `LiveGamepads` + Jamepad, no keyboard; a disconnect releases every input and is marked as an event) or `"messages"`; frames add `controllers` / `controllerProblem`; bridge source set now uses the viewer's controller classes and Jamepad (rule in `05-desktop-viewer.md` §9 updated). Unity: `LiveSession` (TCP client, growing recording), `LivePacer` (one step per 20 ms, max 5 per frame), `ReplayTimeline.Sync`, "Live: ..." chooser entries, LIVE header, PLAY/STOP/Save, **Harness → Live Smoke Test** menu. `BridgeControllersTest` (6, fake controller) each break-checked; 8 new Unity tests. Real end-to-end check: Unity batch smoke test against `gradlew unityBridge` passed (5 OpModes, INIT, PLAY, 50 steps = 1.00 s, STOP) | Bridge 17/17; Unity 31/31; full Java suite 212/219 (the 7 failures are all in `AprilTagVisionTest`, WorkerBot's uncommitted vision work, not the bridge) |
| 2026-10-24 | **A3**: checked Team B and C in TeamCode: both still use Team A's shared hardware, so `RobotConfigs.teamB()`/`teamC()` stay as they are (noted under Known issues) | no change |
| 2026-10-24 | **A2**: `CALIBRATION.md`, the at-the-robot checklist for Step 10, with a suggested helper OpMode for TeamCode (not added: outside Harness) | docs only |
| 2026-10-24 | **A1 / Step 17**: `PhysicsBackend` interface (drive command in, state out, one step at a time) with `MomentumPhysics` (0.1 s response, 200 in/s² traction limit: placeholders; FLOAT coasting at Pedro's measured zero-power accelerations; BRAKE; static-friction stop; walls cancel speed). `PoseEstimator` drives the backend; the runner advances physics before sensors read (no one-loop lag); drive encoders follow real wheel motion. `Pose.fromPedro`. Routes were off for three reasons: one-loop sensor lag (LP-11 bang-bang), the robot started at the field center instead of the route start (walls in the wrong place: DECODE/Curving), no momentum. Now LP-11 0.24 in, DECODE 0.10 in, Curving 0.25 in/1.6° (`PedroRouteTest`, 1 in / 2° tolerance), `MomentumPhysicsTest` (5); Team A timing tests allow 10% for momentum | mine pass; other sessions' in-progress vision and Unity bridge tests fail (not caused by this) |
| 2026-10-23 | Planned U1.10, a standalone Unity replay app (double-click, no Editor): recordings-folder rule for the player, in-app file picking, Windows build into `unity/Build/`, `unity-view.bat` launcher (docs only, not started) | Not rerun (no code change) |
| 2026-10-23 | Stage U2 pieces 1 + 2 (harness side of the live Unity connection): new `bridge` source set (`src/bridge/java`, the core cannot see it) with `BridgeServer` (127.0.0.1:7465, one client, `gradlew unityBridge`, Java agent loaded), `BridgeSession` (hello/load/init/step/play/stop/save/bye; Unity drives the clock; frames in the recording's loop layout, rumbles and events sent once; auto-stop at full time; clear errors), `BridgeOpModes`, `BridgeGamepads`; message format v1 documented in README. Small core additions: `RunRecorder.lastLoop()/loopCount()/events()/header()`, `RunRecordingJson.loopJson/headerJson/rumblesJson/eventsJson/gamepadFrom`, `number/string` and `JsonReader` made public. `BridgeSessionTest` (10) + `BridgeServerTest` (1, real local socket); each of 9 breaks applied on its own failed exactly its test; smoke-tested `gradlew unityBridge` with a separate client (50 steps full forward, 61.4 in) | 188/188 pass |
| 2026-10-23 | Unity replay GUI made to match the Swing viewer: `HudTheme` (Swing colors and fonts), `HudText` (same words and numbers), `ReplayHud` (header with mission clock, Replay chooser and Replays..., field caption and legend, Vehicle/Position/Auto step/Motors/Driver Station/Events cards, gamepad pictures, playback bar with speed box and slider); dark field and robot like `FieldPainter` (wheels lit by power); top-down view by default, T for 3D. FTC→Unity mapping marked confirmed. Compared side by side with a screenshot of the Swing window. 5 new `HudTextTests`, each checked to fail when broken | Unity 23/23 pass; harness tests not rerun (no Java change) |
| 2026-10-23 | **Field walls**: `PoseEstimator` stops the robot box at ±72 in at any heading and lets it slide along a wall; `wallHits()`, report line, `withoutWalls()` for off-field tests; each hit saved as a recording event ("Robot hit the +x wall"). Generated tests now check `DriveChecks.assertRobotDoesNotHitAWall` instead of the on-field check (which walls make always pass). 4 `PoseEstimatorTest` + 1 `RealCodeMotionTest` tests | 177/177 pass |
| 2026-10-23 | Phase 5 step 1: fake motor encoders (`FakeDcMotorEx.step`: counts with power, RUN_TO_POSITION, setVelocity, reset; placeholder 312 RPM motor), fake goBILDA Pinpoint (`FakePinpoint`, driven by the position estimate; added to `RobotConfigs.teamA()` as "pinpoint"), Control Hub battery voltage (`FakeVoltageSensor`), desktop-safe motor type. Runner steps sensors before each call. Real Pedro routes now run; Pedro's pose matches the harness's to 0.01 in. `SensorsTest` (11), each checked to fail when broken | 177/177 pass (with the other sessions' uncommitted walls and viewer work) |
| 2026-10-23 | Stage U1 replay in Unity (U1.2–U1.9): `Assets/HarnessReplay/` with recording classes, loader (v1/v2, Newtonsoft), `FtcToUnity` (only conversion), `ReplayTimeline` (Swing `Playback` rules + blending), field/robot/trail built from code, orbit/top-down camera, side panel, scene `Replay.unity`, **Harness → Run Replay Tests** menu. Checked in the editor through the `unity-mcp` bridge: replays the newest drive correctly (pictures in `build/unity-replay-*.png`); Team A autonomous ends 0.24 m forward. 7 deliberate breaks each failed a test | Unity 18/18 pass; harness tests not rerun (no Java change) |
| 2026-10-23 | Viewer: replays separated from the dropdown (now Drive / Run only). New **Replays** window (`ReplayPicker`, `ReplayLibrary`; REPLAYS... button or Ctrl+O): saved recordings newest first with type (Drive / Scenario / Test), saved time and size, text filter, ALL/DRIVES/SCENARIOS/TESTS buttons, Open file... `ReplayLibraryTest` (3) | 161/161 pass |
| 2026-10-23 | Viewer opens straight into **Drive: Team A TeleOp** when no file is given (`view.bat`, Android Studio button, `gradlew viewRecording`); robot drawing shows four mecanum wheels (power fill: cyan forward, amber backward) and the intake (cyan in, amber out); dark disabled dropdowns. `RobotPartsDrawingTest` (3) | 158/158 pass |
| 2026-10-23 | U1.1: Unity project in `Harness/unity/` (Unity 6000.3.14f1, built-in renderer; Newtonsoft JSON, Test Framework, AI Assistant for the `unity-mcp` bridge), `unity/README.md`, Unity folders ignored in `Harness/.gitignore`. Opened once in batch mode: packages resolved, no errors, return code 0. Then opened in the editor by the user: the `unity-mcp` bridge connects and sees the project; console has no errors (warnings: old Input Manager deprecated, so use the Input System package in U1.6; bridge signature check; relay startup). No scene or replay code yet | Harness tests not rerun (no Java change) |
| 2026-10-23 | Viewer: smaller, quieter T+ clock (header 58 px); one controller runs both gamepads: its D-pad works the operator's A/B/X/Y (up = Y, down = A, left = X, right = B) and isn't sent to the driver, until a second controller connects. 2 new `ControllerTest` tests, checked to fail when broken | all pass |
| 2026-10-23 | D3 + D4: `recorded/RecordedInputs` (recording into `GamepadScript` lines), `RecordedTestWriter` + `MakeRecordedTest` (`gradlew makeTest`, never overwrites), `DriveChecks` (powers in range, robot on the field, final pose). `RecordedDriveToTestTest` (11 tests; generated tests compiled and run inside it), each checked to fail when broken; tried on a real 33 s PS5 drive (about 700 lines, passes) | 152/152 pass |
| 2026-10-23 | Viewer redesign, SpaceX-style "mission control" look: `Theme` (near-black, hairlines, Bahnschrift uppercase labels, cyan/amber/red accents), `MissionHeader` (T+ clock, stage light, REC), `TelemetryPanel` (cards, big position readouts, motor/servo gauges, Driver Station, events, live cards), restyled field (wireframe robot, thin cyan path), gamepads and controls. `TelemetryPanelTest` (2) | all pass |
| 2026-10-23 | Planned the Unity connection in three stages (U1 replay, U2 live socket, U3 physics) with Stage U1 split into steps U1.0–U1.9; summary added to `ai-instructions/02-unity-readiness.md` (docs only) | Not rerun (no code change) |
| 2026-10-23 | D5 + D6: Jamepad 2.30.0.0 added to the viewer (and test classpath, never opened in tests); controller per gamepad slot with keyboard fallback, 5% stick dead zone; disconnect releases every input in the same loop, beeps, shows a message and is saved as an event. Run recording **format v2** adds `events` (`RunRecorder.note`); v1 files still load. `ControllerTest` (3) + 1 `RunRecordingTest`, checked to fail when broken | 139/139 pass |
| 2026-10-23 | Viewer **Record** toggle: off by default (practice, shown but not saved); on before Enter saves the drive. Decided at PLAY and locked (`LiveDrive.requestPlay(keepRecording)`, `keepRecording()`). 2 new `LiveDriveTest` tests, each checked to fail when broken | 135/135 pass |
| 2026-10-23 | D1 + D2: `ds/GamepadSource` (live input through `GamepadDriver`, after the script, via `Gamepad.copy()`), `OpModeRunner.gamepad1/2(GamepadSource)`, `Button.set`/`Axis.set` now public; viewer `HeldKeys`, `KeyboardGamepads` (approved layout), `LiveDrive` (own thread, real-time pacing, Enter = PLAY, Esc = STOP, auto-stop at 2:00, saves `drive-<teleop>-<date>-<time>.json`), "Drive: Team A/B/C TeleOp (keyboard)" in the dropdown. `GamepadSourceTest` (3) and `LiveDriveTest` (6), each checked to fail when broken | 133/133 pass |
| 2026-10-23 | Shared Android Studio run configuration `.run/View recording.run.xml` (runs `viewRecording`); README updated | 124/124 pass (no Java code change) |
| 2026-10-23 | `view.bat`: double-click (or drop a `.json` on it) to open the viewer with Android Studio's Java; README updated | 124/124 pass (no Java code change) |
| 2026-10-18 | Decided D4: recorded drives are saved as generated `.java` test files students can edit (docs only) | 124/124 pass (no code change) |
| 2026-10-18 | Planned "Drive and record a test" (D1–D6); Jamepad approved for USB and wireless controllers; committed Steps 11–14 and viewer work locally | 124/124 pass |
| 2026-10-18 | Viewer dropdown ("Run or replay"): ready-made `Scenarios` (real Team A OpModes, run in the window and saved as `viewer-<id>.json`) and saved recordings (newest first, rescanned on open); window can switch recordings; `viewRecording` now loads the Java agent; with no recordings it runs the first scenario. `ViewerDropdownTest` (4 tests), each checked to fail when broken | 124/124 pass |
| 2026-10-18 | Viewer: gamepad pictures in the bottom-right of the window (`GamepadIndicator`, `GamepadPanel`): held buttons lit, sticks and triggers shown. Recording loops gained `gamepad1`/`gamepad2` (format v1, still uncommitted, so kept at v1); `Button.isPressed` and `Axis.read` added. `GamepadIndicatorTest` (6 tests), each checked to fail when broken | 120/120 pass |
| 2026-10-18 | Steps 13 + 14: viewer window (`ViewerWindow`: field, side panel, controls, keyboard), `Playback` (speed, seek, step, stop at end), `SidePanelText` (screen for the selected moment, rumbles so far), `RecordingViewer` + `gradlew viewRecording`; `RunRecordingJson.load/parse` with the JSON reader moved from tests to `runtime/JsonReader`; recording header gained `period` (v1 not yet committed, so kept at v1). Window opened by hand on the demo recording; `ViewerReplayTest` (12 tests) + 3 reading tests, each checked to fail when broken (except the headless-window test) | 114/114 pass |
| 2026-10-18 | Step 12: new `viewer` source set (`src/viewer/java`, core cannot see it) with `FieldCoordinates` (only y flip), `FieldPainter` (grid, trail, robot box, front edge, heading arrow, placeholder legend), `FieldImage` (off-screen render, PNG to `build/field-images/`); tests forced headless in `build.gradle`; `FieldDrawingTest` (9 tests), each checked to fail when broken | 99/99 pass |
| 2026-10-18 | Step 11 (done before Step 9, to unblock the viewer): `runtime/RunRecording` records, `RunRecorder` (via `everyLoop`), hand-written `RunRecordingJson` saving `build/recordings/<name>.json`, format v1 documented in README. Small API additions: `FakeHardwareMap.configName()`/`named()` (set by `RobotConfigs`), `OpModeRunner.stage()` and `loopPeriodSeconds()`. `RunRecordingTest` (11 tests, test-only `MiniJson` reader), each checked to fail when broken | 90/90 pass |
| 2026-10-18 | Step 8: `physics/TrackCsv` + `PoseEstimator.saveTrackCsv(name)` write `build/tracks/<name>.csv` (format v1 documented in README); `TrackPoint` gained `sinceInitSeconds` so INIT rows do not pile up at time 0; `Pose.tidy` now package-private for reuse; `TrackCsvTest` (5 tests), each checked to fail when broken | 79/79 pass |
| 2026-10-18 | Step 7: `physics/RealCodeMotionTest` (real Team A OpModes): right-stick turn in place, miswired front-left curving left, STOP freezing the pose, whole robot box on the field during autonomous plus a clear report when it leaves; each checked to fail when broken. Test-only, no main code changed | 74/74 pass |
| 2026-10-18 | First commit of `Harness/` (local, not pushed); removed stray `__pycache__/pygame` file | 69/69 pass |
| 2026-10-18 | Replaced the guessed plan in this file with the team's phase and step tables | 69/69 pass (no code change) |
| 2026-10-18 | Added `AGENTS.md`, `CLAUDE.md`, `ai-instructions/` and this file (docs only) | 69/69 pass |

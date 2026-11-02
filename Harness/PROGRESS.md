# Harness progress

Where the FTC test harness stands, what is being worked on and what comes next. Update this file in
the same change as the work (see [ai-instructions/03-workflow.md](ai-instructions/03-workflow.md)).

**Last updated:** 2026-11-01
**Test status:** 390 tests in 65 classes (385 pass, 5 skipped), no failures (`.\gradlew.bat test`, 2026-11-01, on Pedro 3, with the bots' uncommitted work); Unity 116/116 (2026-11-01)
**Git:** branch `Team2-Testing-Pedro3` (local only, not pushed; manager decision D1)
**Next up:** see the manager's lists (Step 15 done 2026-10-27)

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
| 15 | Opt-in switches. **Option A** (user, 2026-10-27): `viewer.open=true` in `harness.properties` opens the viewer after `gradlew test` finishes, on the run's newest recording or the failed test's (`viewer/AfterTestsViewer`, `viewAfterTests` task); no screen = skipped; `viewRecording` stays | ✅ Done 2026-10-27 (W9.3; `AfterTestsViewerTest`, 4) |
| 16 | Extras: HTML replay, Pedro path overlay (the Panels check is dropped: Panels left with Pedro 3, 2026-10-27) | 💤 Optional |
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

## Robot profiles and modularity

**Goal (from the user, 2026-10-24):** make the harness as modular as possible, so any FTC team's
code can hook in with little work. Today it is built around PVI's Team A. Planned only; no code
yet. WorkerBot owns the RP rows and TestBot adds its own rows below.

**History:** what the harness assumed about the robot *before* RP1–RP5, RP8 and D11 (checked in the code
2026-10-24). Every point below has since changed: robot facts, device names and library versions now come
from `harness.properties` and the host's `build.dependencies.gradle`, OpModes from `OpModeCatalog`, and
Team A's Pedro values only from the optional PVI extras (see the RP rows and the Log). Kept so the plan
below still makes sense:

- `OpModeRunner` always tracks position with `WheelReader.teamA()` and `DriveModels.teamA()`
  (Team A's mecanum motor names, directions and speeds from `TeamAPedroConfiguration`), whatever
  OpMode or `FakeHardwareMap` it is given. `BridgeSession` uses `WheelReader.teamA()` too.
- The physics only knows mecanum: `WheelPushes` has four named wheels, `PhysicsBackend.DriveCommand`
  carries a `WheelPushes`, and `MomentumPhysics` / `PoseEstimator` use `MecanumKinematics`.
- `RobotConfigs` is built from PVI's `DriveHardware` / `IntakeHardware` constants, with the
  Pinpoint name from `TeamAPedroHarnessAccess` (a harness file in Team A's TeamCode package, because
  the Pedro constants are package-private). `teamB()` / `teamC()` are copies of `teamA()`.
- `Dynamics.teamA()` reads Pedro's zero-power accelerations through the same access file.
- The bridge's `driveCommand` JSON writes `frontLeft`/`frontRight`/`rearLeft`/`rearRight`, and the
  viewer's robot drawing (`FieldPainter`) finds the wheels by those names.
- `RecordedTestWriter` only knows the `teamA`/`teamB`/`teamC`/`driveOnly` configs.
- `RunRecorder` reads autonomous step names from the `"Auto Step"` telemetry caption only.
- `build.gradle` compiles a fixed `../TeamCode/src/main/java` against a fixed `robotLibs` list
  (SDK 12.0.0, Pedro 2.1.2 + telemetry 1.0.0, Panels 1.0.12).
- `HarnessAgent` only moves `org.firstinspires.ftc.`, `com.qualcomm.`, `com.pedropathing.` and
  `com.bylazar.` onto simulated time.
- The viewer's scenarios and the bridge's OpMode list name Team A's OpModes (TestBot's and
  UnityBot's files; see their rows).

| Step | What | Owner | Status |
|---|---|---|---|
| RP1 | **The robot profile is one config file** (user, 2026-10-25): `Harness/harness.properties` read by `runtime/HarnessConfig` (`java.util.Properties`): motor and Pinpoint names, robot size and calibration values (each `measured` or `placeholder`), field, viewer switch. Fails early on missing, unknown or bad keys. Keys in README "The config file". | WorkerBot | ✅ Done 2026-10-25 (W4; `HarnessConfigTest`, 8). Not read by the runner yet (RP2) |
| RP2 | **The runner, physics and sensors read the config** instead of Team A statics. The core stops using `TeamAPedroHarnessAccess`, which becomes an optional PVI extra (built only when the config says so) that checks the config's Pedro numbers against Team A's real constants. In other repos the Pedro numbers go into the config. | WorkerBot | ✅ Done 2026-10-27 (W6; config v2; `TeamAConfigMatchesPedroTest` 4 + `HarnessConfigTest` +2; full suite 279/279) |
| RP3 | **Other drivetrains**: tank/differential. **Parked**: the user chose mecanum + Pinpoint only for now (2026-10-25). | WorkerBot | 💤 parked |
| RP4 | **Build settings in one place**: `build.gradle` reads the TeamCode path from `harness.properties` and the library list and versions from the host's `../build.dependencies.gradle` (one library set, the host's), minus Android-only `host.skipLibraries`. Passes the list to tests as `harness.robotLibraries`. Agent package list: not moved yet. | WorkerBot | ✅ Done 2026-10-25 (W4; full suite 264/264; checked by `SdkCompatibilityTest`) |
| RP5 | **The bridge and recorded tests use the config** (wheel names, robot config) instead of names written into the code. | WorkerBot | ✅ Done 2026-10-27/28: `RobotConfigs.forOpMode` (D7), `RecordedTestWriter.findOpModeClass` via `OpModeCatalog` (W9.1), `LiveDrive.options()` from the catalog with `viewer.defaultTeleOp` (W10.1, D8); the bridge part by UnityBot (C15: `BridgeOpModes` from the catalog, `WheelReader.fromConfig`) |
| RP6 | **`LinearOpMode` support**: `runOpMode()` runs in step with simulated time, with `waitForStart`, `sleep`, `opModeIsActive` and `isStopRequested` working as on the robot. | TestBot | ✅ Done 2026-10-27 (`runtime/LinearOpModeDriver`, `LinearOpModeTest` 11 tests; README "LinearOpModes") |
| RP7 | **More hardware fakes**, most common first: IMU, CR servo, distance, touch, color, dead wheels, OTOS, Limelight, analog. Motion-driven ones read the physics; world-driven ones are scriptable. | TestBot | ✅ Done 2026-10-27 (all nine: `FakeImu`, `FakeCRServo`, `FakeDistanceSensor`, `FakeTouchSensor`, `FakeColorSensor`, `FakeAnalogInput`, `FakeOtos`, `FakeDeadWheel`, `FakeLimelight`; `SensorScript`; `SensorFakesTest` 8 + `MoreSensorFakesTest` 4) |
| RP8 | **OpMode discovery**: find TeamCode's `@TeleOp` / `@Autonomous` classes automatically for the viewer, bridge, Save as test and the profile's OpMode list. | TestBot | ✅ Done 2026-10-27 (`runtime/OpModeCatalog`, `OpModeCatalogTest` 10 tests; now used by `RecordedTestWriter`, the viewer's `Scenarios`, the bridge tests and `EveryOpModeSmokeTest`) |
| RP9 | **SDK-version safety test**: fails with a plain message when the harness's SDK copies, its reflection into SDK internals, or its library versions no longer match the SDK. | TestBot | ✅ Done 2026-10-25 (`SdkCompatibilityTest`, 3 tests; names of the reflected fields kept in the test) |
| RP10 | **"Bring your own robot" guide**, a starter test template, and a smoke test that runs every discovered OpMode and names missing devices or crashes. | TestBot | ✅ Done 2026-10-27 (`BRING_YOUR_OWN_ROBOT.md`, `EveryOpModeSmokeTest`, `MyFirstRobotTest`, `GuideFileListTest`; D10, D11) |

**RP1: `RobotProfile`.**

> **Changed by the user's answers (2026-10-25):** the profile is `harness.properties` plus
> `HarnessConfig`, not a Java class per team. Mecanum + Pinpoint only, and one library set
> (the host's). The plan below is the earlier version, kept for the ideas RP2 and RP5 still use.

- **What changes:** a new `pvi.harness.profile` package. A `RobotProfile` holds:
  - its name (`"teamA"`), which also goes in the recording header;
  - its OpModes (TeleOp and autonomous classes, for the viewer and bridge lists);
  - its hardware config, as a method that builds the `FakeHardwareMap` (device names and types);
  - its drivetrain: type (mecanum, tank, none), motor names, which direction drives forward, and
    full-power speeds as `ModelValue`s;
  - its localizer: Pinpoint (name and offsets), none, or drive encoders only;
  - its dynamics (`Dynamics`, or none for the instant model) and robot size;
  - its conventions, such as the telemetry caption for autonomous step names (default `"Auto Step"`).
  - `RobotProfiles.teamA()/teamB()/teamC()` wrap today's `RobotConfigs`, `WheelReader.teamA()`,
    `DriveModels.teamA()` and `Dynamics.teamA()`, which stay as thin shortcuts so no test breaks.
- **Format, recommended: a Java class per robot.** It can read the robot's own constants live
  (`DriveHardware.FRONT_LEFT_MOTOR_NAME`, Pedro constants through a `HarnessAccess` file), so it
  never drifts from TeamCode and a renamed constant is a compile error. Students already read Java,
  it needs no JSON parser in the core, and placeholders stay marked with `ModelValue`. A data file
  would copy numbers by hand that already exist in TeamCode, so it would go stale. A data file can
  be added later for teams with no Java constants to read, with a small adapter that builds the
  same `RobotProfile`.
- **Files:** new `profile/RobotProfile.java`, `profile/RobotProfiles.java`, `profile/Drivetrain.java`
  (type, names, directions), `profile/Localizer.java`; `RobotConfigs`, `DriveModels`, `WheelReader`
  delegate to them. README: a "Add your robot" section.
- **Depends on:** nothing.
- **Size:** medium (about 4 new small classes, a README section).
- **Tested by:** a `RobotProfileTest`: Team A's profile gives exactly today's motor names,
  directions, speeds and Pinpoint name; the whole existing suite passes unchanged; a made-up
  profile with other motor names builds a matching `FakeHardwareMap`.

**RP2: the runner, physics and sensors read the profile.**

- **What changes:** `new OpModeRunner(opMode, profile)` (the `FakeHardwareMap` constructor stays,
  using Team A's drivetrain as today). The runner takes its `WheelReader`, `DriveModel` and
  `Dynamics` from the profile. The Pinpoint is fed only when the profile has one. When the drivetrain
  is `none` or a motor is missing, the run reports one clear line ("position not tracked: robot
  'x' has no drivetrain in its profile") instead of guessing, as `untrackedReason` does now.
  `RunRecorder` uses the profile's step caption.
- **Files:** `runtime/OpModeRunner`, `runtime/RunRecorder`, `physics/PoseEstimator`,
  `physics/WheelReader`, `hardware/FakeHardwareMap` (Pinpoint stepping), README.
- **Depends on:** RP1.
- **Size:** medium.
- **Tested by:** Team A runs give the same poses as before (existing route and drive tests);
  a profile with renamed motors is tracked; a profile with drivetrain `none` runs and reports
  why it isn't tracked; a custom step caption shows up in the recording.

**RP3: other drivetrains (tank/differential first).**

- **What changes:** a `TankKinematics` next to `MecanumKinematics` (left/right sides, any number
  of motors per side; forward speed and turn rate, no strafing). `PhysicsBackend.DriveCommand`
  carries a drivetrain-neutral command (forward, strafe, turn powers, plus braking) instead of
  four mecanum wheels, so `MomentumPhysics` and Unity's physics work for both. The mecanum wheel
  values stay available for the bridge.
- **Files:** new `physics/TankKinematics.java`, `physics/PhysicsBackend`, `physics/MomentumPhysics`,
  `physics/PoseEstimator`, `physics/WheelReader` (per drivetrain), README physics section.
- **Depends on:** RP2. Changing `DriveCommand` touches UnityBot's `RemotePhysics` and the bridge's
  `driveCommand` message, so agree the new message (and its version bump) with UnityBot first.
- **Size:** medium to large.
- **Tested by:** `TankKinematicsTest` (both sides forward drives straight, opposite sides spins in
  place, strafing input does nothing); a tank profile driving a scripted TeleOp ends where the
  math says; all mecanum tests unchanged.

**RP4: build settings in one place.**

> **Changed (2026-10-25):** the settings are in `harness.properties`, not `gradle.properties`. The
> library list is read from the host's `build.dependencies.gradle` instead of being typed into
> the file. The agent's package list hasn't moved yet.

- **What changes:** `Harness/gradle.properties` (new, inside `Harness/`) holds `teamCodeDir`
  (default `../TeamCode/src/main/java`), the robot library list, and the agent's package list
  (default today's four, e.g. add `com.acmerobotics.` for RoadRunner). `build.gradle` reads them;
  the agent gets its list as an agent argument. When TeamCode fails to compile because a package
  is missing (`package com.acmerobotics... does not exist`), the build prints which setting to add
  the library to.
- **Files:** new `Harness/gradle.properties`, `build.gradle`, `agent/HarnessAgent.java`, README
  "Using your own TeamCode" section, `ai-instructions/03-workflow.md` (the `robotLibs` line).
- **Depends on:** nothing (can go before RP1).
- **Size:** small to medium.
- **Tested by:** an agent test that a class in an added package follows simulated time; the
  normal suite passes with the defaults; a manual check that a wrong `teamCodeDir` gives the clear
  message (Gradle settings are hard to unit-test).

**RP5: the bridge and recorded tests use the profile.**

- **What changes:** recordings already carry the robot config name; `RecordedTestWriter` looks it
  up in `RobotProfiles` instead of a fixed `switch`, so a new profile works with **Save as test**
  and replays without editing the writer. The bridge's `driveCommand` and the viewer's wheel
  drawing use the profile's wheel names.
- **Files:** `recorded/RecordedTestWriter`, `bridge/BridgeSession` (UnityBot's file: agree first),
  `viewer/FieldPainter`, README.
- **Depends on:** RP1 (writer), RP2 (bridge).
- **Size:** small.
- **Tested by:** a recorded drive from a made-up profile generates a test that compiles and
  passes; an unknown config name still gives today's clear message; bridge tests unchanged for
  Team A.

**RP6: `LinearOpMode` support.**

- **Why:** the SDK's `LinearOpMode` makes `init()`, `loop()` and the rest empty `final` methods and
  runs `runOpMode()` on its own thread. The runner calls `init()`/`loop()` directly, so a
  LinearOpMode does nothing and no error is shown. Team A has none (all 10 of its OpModes are
  iterative), but most teams and the SDK samples use them.
- **What changes:** the runner runs `runOpMode()` on one extra thread, in **lock-step** with
  `SimClock`: each cycle it hands control to the OpMode thread and waits until that thread
  reaches a sync point, then moves time one loop period. Sync points are the SDK's own:
  `waitForStart()`, `idle()` (which `opModeIsActive()` calls), `sleep(ms)`, `isStarted()` and
  `isStopRequested()`. The Java agent rewrites `Thread.sleep` / `Thread.yield` in robot classes to
  `SimTime` hooks, so `sleep(500)` lasts 500 ms of simulated time (today `Thread.sleep` is real
  time). PLAY and STOP go through the SDK's package-private `internalOnStart` /
  `internalOnStopRequested`, through a `HarnessAccess` file in the SDK's package. Telemetry and
  gamepads arrive between steps, as now.
- **Risks:** a loop with no sync point (`while (!gamepad1.a) { }`) never hands control back, so a
  watchdog fails the test after a few real seconds with "the OpMode didn't call idle(),
  opModeIsActive() or sleep() for 3 s". An exception on the OpMode thread goes into `RunResult`
  like any crash. Only one OpMode thread runs at a time, so `SimTime`'s one-clock rule still
  holds. It is the harness's first thread besides the viewer's, so it needs the user's OK
  (04-code-style.md asks before adding concurrency).
- **Files:** `runtime/OpModeRunner` (WorkerBot; or a new `runtime/LinearOpModeDriver` it calls),
  `agent/HarnessAgent` and `SimTime`, a new
  `com/qualcomm/robotcore/eventloop/opmode/LinearOpModeHarnessAccess.java`, README.
- **Depends on:** nothing.
- **Size:** large.
- **Tested by:** small sample-style LinearOpModes in `src/test`: drive for `sleep(1000)` (the robot
  moves for exactly 50 loops), wait for start, loop on `opModeIsActive()` until STOP, stop
  requested during init, a crash in `runOpMode()`, and a busy loop the watchdog catches. Each is
  checked to fail when broken, and gives the same result every run.

**RP7: more hardware fakes.**

- **What changes:** new fakes, in order of how often FTC teams use them:
  1. **IMU** (the SDK `IMU` interface, BHI260AP / BNO055): yaw from the physics heading, plus an
     optional scripted drift or offset. Most common: field-centric drive and turns use it.
  2. **CR servo**: power in, like a motor without an encoder.
  3. **Distance sensor** (REV 2 m): scripted readings; later, distance to the field walls.
  4. **Touch sensor / digital channel**: scripted pressed and released.
  5. **Color sensor** (REV v3, `NormalizedColorSensor`): scripted color, e.g. "red sample from 2.1 s".
  6. **Dead wheels** (two- or three-wheel odometry on motor encoder ports): counts from the
     robot's real motion, not motor power.
  7. **SparkFun OTOS**: pose from the physics, like `FakePinpoint`.
  8. **Limelight 3A**: scripted results, reusing `SeenAprilTag` for its AprilTag pipeline.
  9. **Analog input**: scripted volts.

  Rule: what the robot's motion decides (IMU, dead wheels, OTOS) comes from the physics; what the
  world decides (game pieces, buttons, distances) comes from one `SensorScript` timeline in the
  `GamepadScript` / `AprilTagScript` style.
- **Files:** new `hardware/Fake*` classes and `hardware/SensorScript`; `FakeHardwareMap.add*` and
  `stepSensors` (WorkerBot: agree first); README sensor section.
- **Depends on:** RP1 (which devices a profile's robot has); the physics-driven ones on RP2.
- **Size:** small each for IMU, CR servo, touch and analog; medium for color, dead wheels, OTOS
  and Limelight.
- **Tested by:** each fake read through the real SDK calls (e.g.
  `imu.getRobotYawPitchRollAngles()` after a scripted turn), plus a small TeamCode-style OpMode in
  `src/test` that uses it. Each is checked to fail when broken.

**RP8: OpMode discovery.**

- **What changes:** a core `runtime/OpModeCatalog` finds every `@TeleOp` / `@Autonomous` class
  under the TeamCode package roots (skipping `@Disabled`) and returns plain records: class name,
  display `name`, `group`, period and `preselectTeleOp`. The viewer's hand-written lists
  (`Scenarios`, `LiveDrive.options()`), the bridge's `BridgeOpModes`, and the file search in
  `RecordedTestWriter.findOpModeClass` (used by `makeTest` and Save as test) all read it. RP1's
  profile OpMode list defaults to the catalog, so a team doesn't have to list OpModes by hand.
  Reading the annotations is a small, plain use of reflection, so this needs the user's OK too
  (the alternative is reading the `.java` files as text, as `findOpModeClass` already does).
- **Files:** new `runtime/OpModeCatalog`; then small edits in `viewer/Scenarios` and
  `viewer/LiveDrive` (WorkerBot), `bridge/BridgeOpModes` (UnityBot), `recorded/RecordedTestWriter`
  (WorkerBot) and `viewer/SaveAsTest`.
- **Depends on:** RP1 (package roots, and which profile's robot an OpMode runs on).
- **Size:** medium.
- **Tested by:** finds each of Team A's annotated OpModes with the right name and period;
  `@Disabled` ones are skipped; a test-only annotated OpMode shows up without editing any list.

**RP9: SDK-version safety test.**

- **What changes:** one `SdkCompatibilityTest` that fails with a plain message when the SDK
  changes under the harness:
  - the harness copies of `VisionPortal` and `AprilTagProcessorImpl` still have every public
    constructor and method of the SDK classes in the `Vision` jar (the SDK's versions are loaded
    from the jar in a separate class loader and compared by signature);
  - every non-public SDK field or method the harness reaches into (today `OpModeRunner`'s stop
    flag and OpMode-manager link; RP6's internal calls later) still exists with the expected type;
  - `build.gradle`'s `robotLibs` versions match `../build.dependencies.gradle` (read only), so a
    team's SDK upgrade can't be missed.
- **Files:** a new test only (it could share a list of the reflected names with `OpModeRunner`,
  if WorkerBot agrees).
- **Depends on:** nothing; RP4 later moves the version list.
- **Size:** small.
- **Tested by:** it is the test. Checked to fail by renaming a method in a harness copy and by
  changing a version string.

**RP10: "Bring your own robot" guide and starter tests.**

- **What changes:**
  - `BRING_YOUR_OWN_ROBOT.md`, linked from `GETTING_STARTED.md`: point the harness at your
    TeamCode (RP4), write a profile with your device names and drivetrain (RP1), run the smoke
    test, read its report, copy the starter test.
  - `EveryOpModeSmokeTest`: a JUnit `@TestFactory` with one test per discovered OpMode (RP8). Each
    runs INIT, a few init loops, START, 2 s of loops and STOP on the profile's robot, and fails
    with a plain line such as `missing device "armMotor" (DcMotorEx)` or the OpMode's crash.
  - `MyFirstRobotTest.java`, a starter template with the script → set up → run → check comments.
- **Files:** the new guide and tests; a link in `GETTING_STARTED.md` and the README top.
- **Depends on:** RP1 and RP8 (and RP6 for teams whose OpModes are linear).
- **Size:** medium.
- **Tested by:** the smoke test passes on Team A, and fails naming the device on a profile with a
  device removed.

**Suggested order:** RP4 and RP1 (independent), then RP2, then RP5 and RP3. TestBot's RP9 and
RP6 need nothing first and can run alongside; then RP8 (after RP1), RP7 (after RP1 and RP2) and
RP10 (after RP8).

---

## Game pieces (POLLEN and NECTAR, level 3)

User request 2026-10-25: the balls sit at their starting spots, the robot pushes the loose ones,
and every replay shows exactly the same pushes. Leave room for an intake later. Starting spots are
written into Java, and the Swing viewer draws the pieces too (user decisions). The plan and shared
format are in UnityBot's notes (`game-pieces-plan.md`).

| Step | What | Owner | Status |
|---|---|---|---|
| GP1 | Piece simulation (`physics/GamePiece`, `PieceModel`, `GamePieces`, `BioBuzzPieces`), hooked into `PoseEstimator`: the robot box pushes loose balls; they roll, slow down and bounce off walls and each other; placeholders for friction, bounce and mass | WorkerBot | ✅ Done (tests pass; full suite waits on the bridge and vision test updates below) |
| GP2 | Run recording v4: `header.pieces` (all at INIT) and `loops[i].pieces` (only changed ones), JSON read/write, README format docs | WorkerBot | ✅ Done |
| GP3 | Swing viewer draws the pieces (loose balls filled under the robot, FLOWER/CELL balls as rings over it) | WorkerBot | ✅ Done |
| GP4 | Bridge protocol v4 and Unity loading, live view and drawing (`GamePieceTrack`, `GamePieceView`: CAD balls that roll as they move) | UnityBot | ✅ Done |
| GP5 | Intake: a `"held"` state is reserved in the format | — | Later |

Not simulated yet: the robot being slowed by balls. (Balls bouncing off the HIVE frame and the
FLOWERs: done in W9.2, 2026-10-27, from UnityBot's CAD-measured `pieceObstacles`.)

**Ball facts (W7, 2026-10-27):** the official manual (TU06, 2026-10-22, the latest on
ftc-resources.firstinspires.org; copy in `fields/sources/`) section 9.8 gives POLLEN about 2.8 in
and NECTAR about 3.6 in, Gopher ResisDent polyethylene (AndyMark am-5851 / am-5852), and nothing
on mass, bounce or friction. So the sizes are official values with that source, and the six
`PieceModel` values stay placeholders, now saying the manual has no number for them. Next step for
real numbers: weigh and roll real balls (or the AndyMark product pages, which aren't FIRST sources).

**Ball masses (W16, 2026-10-28, decision D6):** the user read AndyMark's store pages: POLLEN
(am-5851) 0.055 lb = 24.9 g, NECTAR (am-5852) 0.091 lb = 41.3 g. They are now measured values in
`PieceModel.bioBuzz()`; rolling and the three bounces stay placeholders.

## Field walls from the official CAD

The official BIOBUZZ field CAD (processed 2026-10-25, see `art-inbox/processed/field/manifest.json`)
puts the inside of the walls at **±70.585 in**. That is where the tile edge and the rail's inner
face are. The harness's physics walls are at **±72 in** today (`Pose.FIELD_HALF_WIDTH_INCHES`), so
the simulated robot can drive 1.4 in further than the real one at every wall.

| Step | What | Owner | Status |
|---|---|---|---|
| FW1 | Physics walls at the CAD's ±70.585 in | WorkerBot | planned, waiting: the user said on 2026-10-25 to leave it until Team A's robot length is measured ([CALIBRATION.md](CALIBRATION.md) step 1) |

- **What changes:**
  - Add a new `Pose.WALL_HALF_WIDTH_INCHES` = 70.585, stored as a `ModelValue` marked measured
    from the official CAD.
  - `MomentumPhysics` wall limits, the `DriveChecks` "never hits a wall" check, the viewer's
    wall drawing and its red flash, and the wall tests use it.
  - `FIELD_HALF_WIDTH_INCHES` stays 72, because Pedro's frame (`Pose.fromPedro`, 0–144 in) and
    the viewer's field scale are built on it. The 1.4 in strip between them is drawn as the wall.
- **Files:** `physics/Pose`, `physics/MomentumPhysics`, `recorded/DriveChecks`,
  `viewer/FieldPainter` (and maybe `FieldCoordinates`), tests `RealCodeMotionTest`,
  `BatteryTest`, `PedroRouteTest`, `RobotPartsDrawingTest`, plus README "How motion is simulated"
  and CALIBRATION.md.
- **Depends on:** nothing. Overlaps TestBot's `PedroRouteTest`, so agree first.
- **Size:** small.
- **Tested by:**
  - A robot driving into the +x wall stops with its bumper at 70.585 in.
  - The viewer flashes the new wall line.
  - The Pedro routes still finish within their tolerances.
- **Question for the user first:** Pedro routes start the robot 8 in from Pedro's 144 in edge, for
  example `(56, 8)`. With walls at 70.585, that leaves 6.6 in from the real wall to the robot's
  centre, so the robot fits only if it is **at most 13.2 in** in that direction. With 72 in
  walls the limit is 16 in. Either Team A's real robot is small enough, or Pedro's coordinates
  in TeamCode assume a 144 in field and the start poses need checking on the real field.
- **Unity:** `FtcToUnity.FieldHalfWidthInches` = 72 is UnityBot's. Tell UnityBot when FW1 lands
  (it already has the CAD numbers).

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
  so the configs stay as they are. (They also include Team A's `pinpoint`, which B and C never look up.)
  **Decided by the user 2026-10-24:** "Dont mess with them for now just keep them up to date". So: no own
  hardware for Team B/C yet; they keep copying `teamA()` (any device added to Team A's fake robot comes along);
  if TeamCode's Team B/C code changes (new devices or wrappers), update their configs to match and tell the user.

---

## Log

Newest first. One line per change: date, what changed, test result.

| Date | Change | Tests |
|---|---|---|
| 2026-11-01 | **C24 camera tilted up 18° to see the HIVE's raised tags** (UnityBot chose, WorkerBot set; user: "see the AprilTags on the raised section of the HIVE"). Simulated with the harness's CameraView rules, robot facing the HIVE from 24-72 in (24 spots): pitch 0 → raised row (34-37, 42-45) never, lower row 56 tags from 36 in; +10 → raised 28 from 60 in; **+18 → raised 38 from 48 in, lower 76 from 24 in** (no lower tag lost); +32 → raised from 36 in. +18 is the smallest tilt that sees the raised row from a 4 ft scoring distance; the floor near the robot leaves the picture. `harness.properties` pitchDegrees=18 (placeholder; TeamCode still says level, so Team 2's robot-code pose will be off until TeamCode matches), TeamCode check is a note (WorkerBot); vision-field-tags.json remade from (55, -12) facing -x (all of 30-37 at the start). The webcam picture now leaves out the sight lines and their markers (layer 31; `TheWebcamPictureLeavesOutTheSightLines`, break-checked), so the real C25 patterns aren't hidden. `BuildWindowsApp` takes `-buildPath` to build a check copy (build/unity-check) while the user's app is open. Checked in that copy: `build/c24-c25-camera.png`, webcam close-up `build/c24-c25-webcam-zoom.png`. unity\Build not rebuilt yet (user's app open) | Unity 125/125 |
| 2026-11-01 | **T14 follow-up for C24** (TestBot): Team 2's settings webcam is now tilted up 18 degrees, so `AutomaticAprilTagsTest.theSettingsFileWebcamSeesWhatTeamTwosMountSees` now expects TeamCode's level view turned by the settings' tilt about the lens's right axis (worked out with plain rotation, not `CameraView.from`'s own formula) and checks the mount position matches TeamCode; it skips itself if the settings camera also has yaw or roll. Break-checked: a flipped pitch sign in `CameraView.from` fails it | AutomaticAprilTagsTest 10/10 |
| 2026-11-01 | **Camera tilted up 18 degrees** (WorkerBot, UnityBot's C24 pick; user: tilt to see the HIVE's raised tags): `webcam.logitechVisionWebcam.pitchDegrees=18 | placeholder | ...TeamCode still says level`; `TeamACameraMatchesTeamCodeTest` prints it as a NOTE. `VisionFieldTagsRecordingTest` now starts at (55, -12) facing -x: all eight red tags (lower 30-33 and raised 34-37) are in view at the start, leave one by one and come back as it turns; new check that 34-37 are seen (break-checked: pitch 0 sees only 30-33). Recording `build/recordings/vision-field-tags.json` remade. TestBot adapted `AutomaticAprilTagsTest.theSettingsFileWebcamSeesWhatTeamTwosMountSees` (it assumed the level TeamCode mount) | 390 run, 0 failed, 5 skipped |
| 2026-11-01 | **C25 real AprilTag patterns on the BIOBUZZ field** (UnityBot; user: "add the april tags to biobuzz field"). Before: the CAD's `apriltags.obj` drew all 16 tags as plain black squares (one material, no pattern), and the simple field drew a black square on a white plate. Now every tag in the field file is drawn with its real **36h11 pattern** (white edge, black border, 6x6 data cells, 10x10 px point-filtered texture) at its file position, facing, and size (the black square = `sizeInches`, the whole picture 10/8 of it), 1 mm in front of the CAD square; the HIVE tags (`moves`) where the file puts them at the start. The 587 codes come from the FTC SDK's own AprilTag library on this PC (`org.openftc:apriltag:2.1.0` libapriltag.so, AprilTag 3, BSD 2-clause), no download; checked by drawing tags 0 and 1 from them and from the older AprilTag 2 codes laid out row by row (identical). New `AprilTag36h11.cs`, `FieldBuilder.AddTag` public; new `AprilTagPatternTests` (4, break-checked). Not rebuilt yet: the user's app was open | Unity 124/124 |
| 2026-11-01 | **The config owns the camera mount** (WorkerBot; user: tilt the camera to see the bottom of the HIVE, the location belongs in the config; UnityBot C24 picks the pitch): `TeamACameraMatchesTeamCodeTest.theMountFollowsTeamCodeOrSaysWhereItCameFrom` prints a NOTE for each mount number that differs from TeamCode instead of failing, but refuses one whose source still starts with "TeamCode" (a wrong copy); the picture size stays strict (robot code asks the camera for it). New `aChosenMountNumberIsReportedButAWrongCopyIsRefused`. Break-checked both: without the source check, and a -15 pitch still sourced "TeamCode" | 390 run, 0 failed, 5 skipped |
| 2026-11-01 | **T16 Live mode without a reason** (UnityBot; TestBot found the cause: a mixed USB copy's harness.properties made `HarnessConfig.load()` throw when the `BridgeSession` was created, so the bridge never sent `welcome` and Unity showed no OpModes). The session now reads harness.properties only when needed (wheel reader on `load`), and `hello` answers `error` "The harness can't list its OpModes: ..." with the reason, keeping the connection (protocol unchanged, v5; README updated). Unity shows that reason in amber in the Live row and in its error box (`ReplayController.LiveProblem`, `HudText.LiveProblemText`). The Unity picker never filtered: TeleOp and Autonomous OpModes are listed together in Driver Station order. New `BridgeSessionTest.aBrokenConfigIsReportedOnHello`, `CameraPictureTests.LiveProblemsAreShownOnOneLine` (break-checked) | Java 389 (0 failed, 5 skipped); Unity 120/120 |
| 2026-11-01 | **W20 pushed POLLEN stopped dead with Physics: Unity** (UnityBot; WorkerBot found the cause): `RemotePhysics.supply` put Unity's pose in place as soon as it arrived, so the runner's pose before and after each step were the same and a pushed ball got no speed. Unity's state is now kept pending and taken in `step()`, like the harness's own physics. New `BridgeUnityPhysicsTest.aPushedBallKeepsRollingAfterTheRobotStops` (Unity's part played: the robot backs into the preloaded POLLEN at 50 in/s and stops); break-checked: the old supply makes it "roll on 0.0 in", the user's symptom | Java 388 (0 failed, 5 skipped) |
| 2026-11-01 | **W21: PVI's viewer dropdown lists every autonomous** (WorkerBot, from TestBot's T16): with `pvi.extras=true` the dropdown had only PVI's five hand-made scenarios, so "Team A Pedro Paths" (`TeamAPedroPilotAutoOpMode`) was never offered. `PviScenarios.all()` now adds, after the hand-made five, the generic 30 s scenario of every catalog autonomous they don't cover (by class; new `Scenarios.autonomousFromCatalog`, generic scenario code shared). New `ScenariosTest.pviAlsoOffersEveryOtherAutonomous` (break-checked: without the addition it fails); `pviKeepsItsHandMadeScenarios` now checks the first five; `ViewerDropdownTest` runs the new entry to STOP. README | 388 run, 0 failed, 5 skipped |
| 2026-11-01 | **T16.1 "auto modes were not listed"** (TestBot; user, Unity Live on another laptop with the USB copy): reproduced with a copy of `E:tc-robot-framework` at a path with spaces (`Harness/build/t16 usb copy/`, deleted afterwards). **Cause: the USB snapshot was taken mid-change (W19):** its Java (`HarnessConfig`, `CameraSetup`, 17:49) needs `config.version` 4 and the `webcam.*` lines, but its `harness.properties` (2026-10-30) and `build.gradle` are version 3, so the build passes and every `HarnessConfig.load()` fails at run time; the bridge session can't start, so Unity Live gets no OpMode list. Not the cause: the path with spaces (with consistent files the copy's `BridgeOpModes.all()` lists both autos, Team A Autonomous Demo and Team A Pedro Paths) or Unity (its picker shows every entry, autonomous included). Also found: with `pvi.extras=true` the Swing viewer's dropdown has only one auto (PviScenarios' Team A autonomous), never Team A Pedro Paths (WorkerBot's, reported). Fixed (TestBot's): `OpModeCatalog` no longer drops an OpMode that can't load without a word: new `scan(...)` gives `Scan(opModes, leftOut)` and `find`/`fromConfig` print a warning. New `OpModeCatalogTest.anOpModeThatCantLoadIsReportedNotDropped` (a class loader hides a helper class; break-checked: without the report, it fails) and `withEveryClassThereNothingIsLeftOut`, samples in `runtime/catalogbroken` | OpModeCatalogTest all pass; full run 387 run, 1 failed (UnityBot's `BridgeUnityPhysicsTest.aPushedBallKeepsRollingAfterTheRobotStops`, its RemotePhysics work in progress), 5 skipped |
| 2026-11-01 | **W20: pushed balls keep their speed** (WorkerBot, user: "the POLLEN stops moving the moment the robot stops pushing it", seen in Live mode with Unity physics): cause found in the bridge's `RemotePhysics` (UnityBot's): `supply()` sets Unity's new pose before the runner's `advanceTo()`, so `GamePieces.step` gets the same robot pose before and after the loop; a ball is only pushed out of the box with robot speed 0, so it stops dead. Harness physics was right (the ball keeps the push speed and slows at the 15 in/s² placeholder, unchanged). Fix proposed to UnityBot (apply Unity's state in `step()`); waiting. New PVI-extras `PushedBallRollsOnTest`: Team A's TeleOp backs into its pre-loads for 0.5 s and lets go; the POLLEN rolls on to the far wall, bounces back a little and stops; recording `build/recordings/pushed-ball-rolls-on.json`. Break-checked: giving the ball the robot speed 0 (the Unity bug) makes it fail, "rolled on 0.0 in" | 384 run, 0 failed, 5 skipped |
| 2026-11-01 | **Recording header names its field** (WorkerBot, manager after UnityBot's C23): v5 header `"field"` is always `field=` from `harness.properties` (e.g. `"biobuzz-2026"`), so Unity shows that field's tags and camera lines; just a label, no field switching. Older files read it as null. `RunRecordingTest` checks it (break-checked: null fails it); README | (in the run above) |
| 2026-11-01 | **T15.1 vision recordings show BIOBUZZ tags** (TestBot; user found DECODE ids 20/22/24 in Unity): `TagsInViewCardTest.aTurningVisionRunIsSavedForUnity` (vision-tag-22.json, scripted DECODE tags) replaced by `aFieldTagsVisionRunIsSavedForUnity`, which saves **`build/recordings/vision-field-demo.json`**: Team 2's vision OpMode, webcam on the settings file's `aprilTags=field`, robot turning left from 50 in in front of the HIVE's blue AUDIENCE side; tags 38, 39, 40, 41 come into view in that order and leave, no red tags, header says `field` (break-checked: a scripted webcam gives no tags). The scripted SDK-logic save is now `scripted-apriltag-vision.json` (was apriltag-vision.json). Header mode fix (a) by WorkerBot (`CameraSetup.withAprilTags`) | TagsInViewCardTest 4/4, AprilTagVisionTest 16/16; WorkerBot's full run 383 run, 0 failed, 5 skipped |
| 2026-11-01 | **Recording header gives each webcam's real tag mode** (WorkerBot, T15.1(a) from TestBot; UnityBot found it in C23): `cameras[].aprilTags` was the settings file's value even when a test scripted the webcam. `RunRecorder` now writes each webcam's mode at the end of the run (`FakeWebcam.seesFieldTags()`), via new `CameraSetup.withAprilTags(mode)`. New `CameraSetupTest.theRecordingSaysWhetherTheWebcamWasScripted` (break-checked: the settings value fails it); README note | 383 run, 0 failed, 5 skipped |
| 2026-11-01 | **C23 vision lines and missing webcam window** (UnityBot; user: "vision lines that don't appear to point at anything and there is no camera view window"). Cause: the user's app (started 18:06, has C22) had the webcam picture **off by default**, and the AprilTag recordings in build/recordings (`vision-tag-22.json`, `apriltag-vision.json`) hold **scripted** detections (DECODE ids 20/22/24 a fixed 30 in ahead), not field tags. Their header says `aprilTags: field` because it copies harness.properties, not the webcam's real mode; reported to WorkerBot. Fixes: the webcam picture is **on by default** (C toggles, Visuals switch; same in Live mode, which shows its run through the same code with the bridge's header cameras); sight lines and picture boxes: solid cyan on a field tag (within 4 in), solid amber to the real tag when measured elsewhere, **dashed amber labelled "22 · not on this field"** for an id the shown field doesn't have (manager's rule), listed under the picture too (`AprilTagSightView.Target`/`LabelText`, `CameraMount.SeenFrom`); the no-camera note says why (`HudText.NoCameraText`: before format 5, or no webcam); "MOUNT NOT MEASURED" only for guessed mount fields (it also fired for maxViewAngleDegrees). Checked WorkerBot's new `vision-field-tags.json` and `vision-field-demo.json` (Team 2's vision OpMode, automatic tags): all 732 detections land within 0.92 in of the BIOBUZZ tags with Unity's math. Asked WorkerBot to write `header.field` for field-tag runs. New tests `LinesGoToFieldTagsOrAreMarkedNotOnTheField`, `AFieldPointSeenFromTheCameraRoundTrips`, `ThePictureSaysWhyNothingIsShown`, an off-field (dashed) tag in `OneLinePerTagAndNoneWhenOff`, mount-guess checks in `TheFieldOfViewSetsTheLensAndGuessesAreMarked` (all break-checked). Rebuilt after the user closed the app; checked in my copy on `vision-field-tags.json` (`build/c23-field-tags.png`) and TestBot's `scripted-apriltag-vision.json` at 1280×720 (`build/c23-scripted-tags.png`) | Unity 119/119 |
| 2026-11-01 | **Vision recording for UnityBot's C23 check** (WorkerBot): new PVI-extras `VisionFieldTagsRecordingTest`: Team 2's "Team A AprilTag Vision Test" OpMode on the settings robot (webcam `aprilTags=field`), turned slowly through about 410 degrees at (45, 0) by setting the drive powers from the test (the OpMode doesn't drive). The red SCORING tags 30-33 come into view, leave one by one and come back; saved to `build/recordings/vision-field-tags.json` (recording v5 with `cameras`). Break-checked: no turn fails it | 382 run, 0 failed, 5 skipped |
| 2026-11-01 | **W19.3: PVI's webcam sees field tags by default** (WorkerBot, manager after the user asked for tags to appear automatically): `webcam.logitechVisionWebcam.aprilTags=field`, so live drive, the viewer and Unity see the field's AprilTags in view (TestBot's T14); tests that need a script still choose it. New `TeamACameraMatchesTeamCodeTest.theWebcamSeesTheFieldsTagsByDefault` (break-checked: `script` fails it) | 380 run, 0 failed, 5 skipped (full suite with `field`), +1 PVI test |
| 2026-11-01 | **W19: camera setups as data** (WorkerBot; user: tags seen automatically + a camera POV window; with TestBot T14 and UnityBot C22): **config v4**: each webcam in `devices.webcams` needs `webcam.<name>.<fact>` lines: mount `forwardInches` / `leftInches` / `upInches` and `yawDegrees` / `pitchDegrees` / `rollDegrees` (robot frame, 0,0,0 = level looking forward), `horizontalFovDegrees`, `verticalFovDegrees`, `imageWidthPixels`, `imageHeightPixels`, `maxRangeInches`, `maxViewAngleDegrees` (facts), and `aprilTags` (`field` or `script`). New `hardware/CameraSetup` (checks the numbers), `HarnessConfig.cameras()` (signed facts allowed for the mount; keys for an unlisted webcam refused), `FakeHardwareMap.addWebcam(CameraSetup)` / `cameraSetups()` (gives the webcam its setup; `RobotConfigs` turns on field tags for `aprilTags=field`); `build.gradle` checks version 4. PVI: mount and 640 x 480 measured from `TeamAAprilTagVisionRobot` (4.875 forward, -6.875 left, 19 up, 0/0/0), field of view 65.9 x 51.8 deg, range 120 in and view angle 60 deg placeholders, `aprilTags=script`; new PVI-extras `TeamACameraMatchesTeamCodeTest` reads the TeamCode constants from the source (they're private). **Run recording v5**: header `cameras` (plain numbers, `aprilTags`, `placeholders`); v1-v4 still load; Unity reads it (UnityBot), bridge stays v5 (`loaded` carries the header). Removed `OpModeRunner`'s old `FakeWebcam.stepAll` call (webcams are stepped with the pose now, T14). New `CameraSetupTest` (6) and `TeamACameraMatchesTeamCodeTest` (3), all break-checked; `RobotConfigsTest`, `RunRecordingTest` updated. README: config v4 keys, recording v5 `cameras` | 380 run, 0 failed, 5 skipped |
| 2026-11-01 | **T14.1 automatic AprilTags** (TestBot; user: tags appear in the fake webcam automatically): `FakeWebcam.seeFieldTags(tags, CameraView)` / `seeFieldTags(fieldId)` (uses the `webcam.<name>.*` setup) works out each picture from the robot's true pose, the mount and the field file's tags; `script(...)` switches back. New `hardware/FieldAprilTags` (reads `fields/<id>.json`, then `fields/disabled/`) and `hardware/CameraView` (range, view angle 60 degrees placeholder, all four corners inside the horizontal and vertical FOV; `from(CameraSetup)` is the one place W19's forward/left/up convention becomes `setCameraPose` numbers). FakeWebcam is now a `SimulatedSensor` (gets the pose); `FakeWebcam.stepAll` removed. No occlusion; moving HIVE tags stay where the file puts them; Team 2's DECODE tag library gives BIOBUZZ tags id only. New `AutomaticAprilTagsTest` (10, each break-checked: no range, no view angle, no horizontal FOV, no FOV + behind lens, bearing sign, pitch and left conversion, automatic mode off); one runs Team 2's AprilTag Vision OpMode and gets the robot's true field position and yaw. `AprilTagVisionTest.versionTwoRecordingsStillLoadWithNoTags` now ignores v5's `cameras[].aprilTags` text. `webcam.<name>.aprilTags=field` in harness.properties turns it on for live and viewer runs (WorkerBot wired it in RobotConfigs; Team 2's webcam now defaults to `field`). Guide's PVI test list updated | 380 run, 0 failed, 5 skipped (full `.\gradlew.bat test`, 2026-11-01) |
| 2026-11-01 | **C22.1 webcam picture** (UnityBot; user: "a smaller window to see the camera's POV"): C key / Visuals switch (remembered, off by default; `-webcam` for one run) shows a small picture-in-picture in the field's bottom-right corner rendered by a second Unity camera at the webcam's mount (harness frame: 0/0/0 = level, forward; yaw + left, pitch + up, roll + clockwise from behind) with its picture size and field of view (FOV NOT MEASURED / MOUNT NOT MEASURED from the setup's `placeholders`; C920 lens if no FOV), into a 320-px-wide RenderTexture. Each detection of that webcam in the loop gets a box and id sized from the field file's tag size; C cycles through several webcams. AprilTag sight lines now start at the lens. No camera setup: "No camera on this robot". Reads recording v5 `header.cameras` in WorkerBot's W19 format (agreed: name, forward/left/upInches, yaw/pitch/rollDegrees, horizontal/verticalFovDegrees, imageWidth/HeightPixels, maxRangeInches, placeholders); `RecordingLoader.SupportedVersions` now 1-5. Live mode gets it through the bridge's `loaded` header, so the bridge protocol stays v5. New `CameraMount.cs`, `CameraPictureView.cs`, `CameraData`; new `CameraPictureTests` (10, each break-checked, plus `UnknownVersionIsRefusedClearly` now uses version 6; one checks the Unity camera draws a tag exactly where its box goes). Checked in the built app with a v5 copy of vision-tag-22.json given PVI's camera setup (`build/c22-webcam.png`) | Unity 116/116; Java not rerun (no Java change) |
| 2026-11-01 | **C21 step 1: goBILDA Starter Bot as the robot's 3D model** (UnityBot; user: model only for now, true size, also in the top-down view). New `Harness/robots/` (D7's first piece): one folder per robot with `robot.properties` (name, source, Resources model path, units, forward/up axes, center, wheel parts) and a README; `robots/gobilda-starter-bot/` with `slim_model.py` (plain Python) that turns the user's OBJ export of goBILDA's STEP (3.4 M triangles, one object per color) into a 141,592-triangle, 4.3 MB OBJ: parts split by connection, screws and nuts dropped, the six wheels gathered into named groups, vertex clustering. Unity: `RobotProfile.cs` reads the file; `RobotModelView.cs` places the model at true size (turned by its axes, centered, lowest point on the floor) and spins wheel parts from motor power (not when paused or jumping); `FieldBuilder.RebuildRobot` draws it instead of the box (heading arrow kept above it); Visuals screen gets a **Robot** row (Box / goBILDA Starter Bot, remembered) and `-robot <id|box>`; `FieldBuilder.MotorPower` now internal. Front assumed at -y in the model (where the mechanism sticks out); one line in robot.properties flips it. Credit in Art/CREDITS.md. New `RobotModelTests` (9, each break-checked). Built the app and checked it in 3D (`build/c21-gobilda.png`). Swing viewer unchanged (still a box) | Unity 106/106; Java not rerun (no Java change) |
| 2026-10-30 | **W18: RobotConfigsTest works on a CRLF checkout** (WorkerBot): git (core.autocrlf) re-created `harness.properties` with CRLF, and `theDeviceListsAddTheirDevices` filled in the device lists by matching `"...=
"` before normalizing line endings, so nothing matched ("imu should be on the robot"); it would fail on any fresh Windows checkout. The fill-in is now a helper that turns CRLF into `
` first; new `fillingInTheDeviceListsIgnoresLineEndings` checks an LF and a CRLF copy give the same text (break-checked: without the normalizing both tests fail). Other tests reading repo files checked: none matches on `
` (regexes and the JSON reader don't care) | 361 run, 0 failed, 5 skipped |
| 2026-10-28 | **W16: ball masses measured** (WorkerBot, D6): `PieceModel.bioBuzz()` POLLEN 24.9 g (0.055 lb, AndyMark am-5851) and NECTAR 41.3 g (0.091 lb, am-5852), measured, source the AndyMark store pages; rolling and bounces stay placeholders. Masses only set the ball-to-ball split (ratio 0.5 to 0.6); the robot's push doesn't use them, and no existing result moved. `GamePiecesTest.ballSizesComeFromTheManualAndMassesFromAndyMark` (renamed; break-checked: a 30 g NECTAR made it fail) | (in the full run, see Test status) |
| 2026-10-28 | **W15: `view.bat` checks its drive** (WorkerBot): before building, PowerShell `Get-Volume` reads the drive's type, file system and free space; on a removable, FAT32/FAT/exFAT or under-1 GB drive it explains why that fails and asks "Build here anyway?" (Y goes on). Unknown free space counts as plenty. `HARNESS_TEST_DRIVE_INFO` fakes a drive for testing | batch file only |
| 2026-10-28 | **C19.1 Live mode refuses a USB stick** (UnityBot; precaution after view.bat and Live mode failed from the user's FAT32 stick on another PC): before starting `gradlew unityBridge`, `HarnessLauncher` reads the Harness folder's drive (.NET `DriveInfo`) and, like WorkerBot's view.bat (W15, same rules and words), fails at once on a removable drive, a FAT32 / FAT / exFAT one, or under 1024 MB free, saying to copy the whole folder to the PC's own drive; unknown drives don't block. `DriveProblem`, `ReadDrive`, injectable `DriveReader`; 2 new `HarnessLauncherTests` (break-checked). Real drives read: C: Fixed NTFS (passes), the USBRANLY stick E: Removable FAT32 286 MB (would be refused). App rebuilt | Unity 97/97 |
| 2026-10-28 | **W14: doc and leftover clean-up** (WorkerBot, after TestBot's docs pass): `CALIBRATION.md` points at the `harness.properties` lines (not `ModelValue` code, not "read live"); the robot-profile "what the harness assumes" list is marked as history; RP5 ✅; Step 16 drops the Panels check (Panels left with Pedro 3), also in `05-desktop-viewer.md`; the unused `com.bylazar.` (Panels) prefix removed from `HarnessAgent.robotCode()` and its comments (TestBot's OK) and from README's agent note. Also W13.2: the drop-in rehearsal following `BRING_YOUR_OWN_ROBOT.md` exactly gives 169 run / 0 failed / 15 skipped in another team's repo; `DriveModelTest`'s PVI-only checks now skip without `pvi.extras` | 360 run, 0 failed, 5 skipped (60 classes) |
| 2026-10-27 | **C18 long OpMode names in the Unity Vehicle card** (UnityBot): each Vehicle value is cut with "..." to fit its column (`ReplayHud.VehicleText`), so a long name like TeamAAprilTagVisionTestOpMode no longer runs into PERIOD; the full name stays in the header line. `HudTextTests.LongOpModeNamesFitTheVehicleCard` (break-checked). App rebuilt; checked with TestBot's turning-robot `vision-tag-22.json` (sight line turns with the robot) | Unity 95/95 |
| 2026-10-27 | **C17.1 AprilTags in Unity** (UnityBot): the replay (and Live mode, whose frames use the same loop layout) reads each loop's `aprilTags` (recording v3+) and draws a thin cyan sight line from the robot to where each webcam measured each tag (its ftcPose turned into the field with the robot's pose), with the tag id, plus a **Tags in view** card worded exactly like the Swing viewer's (TestBot's TagsInViewCardTest numbers). The webcam's mount isn't in the recording or harness.properties, so lines start at the middle of the robot's top, facing its front (unity/README says so). **A** key and a Visuals switch, on by default and remembered. New `AprilTagSightView`, `HudText.AprilTagLines`, `AprilTagSeenData`; 4 `AprilTagSightTests` (bearing sign and right/left flip failed exactly 3). Checked in my own app copy with TestBot's new `apriltag-vision.json` (tags 20 and 24) | Unity 94/94 |
| 2026-10-27 | **D13 bridge tests work in any repo** (UnityBot, manager decision D13 after WorkerBot's W13 drop-in rehearsal): the 5 bridge test classes no longer load PVI OpModes by name. New test helper `BridgeTestOpModes` picks `viewer.defaultTeleOp` (class or Driver Station name), else the first TeleOp, the first autonomous and the config's front-left motor; PVI-only checks (Team A title, Team B/C robots, rumble demo, stop() zeroing the drive, the 30 s autonomous) sit behind `assumeTrue(pvi.extras)`. Drop-in copy (build/dropin/team-repo): 168 run, 0 failed, 13 skipped, with all bridge tests present. Helper break-checked (8 failed) | Bridge 25/25 (none skipped in PVI's repo) |
| 2026-10-27 | **T9** (TestBot): BRING_YOUR_OWN_ROBOT.md fixed from WorkerBot's W13 drop-in rehearsal (viewer.defaultTeleOp and smoke rows, rename the template, no-Pinpoint note, measured to placeholder, clear PVI values, copy from a clone). Smoke test: a second @TestFactory pushes the left stick forward 0.2-1.2 s after PLAY in every TeleOp on a robot with the drive motors and fails if no drive motor got power; `smokeTest.notDrivingTeleOps` (D14, PVI's three tool TeleOps) skips only that check; break-checked (stick not pushed: the 4 driving TeleOps fail). Recordings for UnityBot's C17: `apriltag-vision.json` (AprilTagVisionTest) and `vision-tag-22.json` (TagsInViewCardTest, robot turning, two tags) | 355 pass, 6 skipped |
| 2026-10-27 | **RP10 done, and D12 route tests** (TestBot): `EveryOpModeSmokeTest` unparked: one INIT/0.5 s/PLAY/2 s/STOP run per catalog OpMode on `RobotConfigs.forOpMode`, plain `missing device "x" (Type)` lines, OpModes in `smokeTest.skipOpModes` shown as SKIPPED (D10; PVI skips KimCode TeleopV1); 9/10 pass, break-checked (no frontLeft: 7 fail naming it). `GuideFileListTest` keeps the guide's delete list equal to the tests that use PVI TeamCode or `src/pvi*` classes (D11; 29 files now), break-checked. Three `OpModeCatalogTest` checks of PVI's OpModes skip themselves with pvi.extras=false. `PedroRouteTest` (D12): DECODE, Curving and Illegal Path run without robot obstacles; new `decodeSeasonRoutesHitTheBiobuzzHive` pins their HIVE-frame hits; break-checked. Guide: skip key, field-elements note, robot-recorder link; the "not finished" note is gone after WorkerBot's W12.2 | 348 pass, 2 skipped |
| 2026-10-27 | **C16 Unity physics stops at the field elements; bounce recordings reproducible** (UnityBot, agreed with WorkerBot). C16.1: bridge format **version 5**: `loaded` robotPhysics gains `robotObstacles` (harness.properties `field.robotObstacles`) and each `physicsState` may carry `obstacleContact` ("HIVE frame", "far FLOWER"), passed into `PhysicsBackend.State` so the same "Robot hit the ..." events appear in both physics modes. `UnityRobotPhysics` builds its colliders from the field file's 18 `pieceObstacles` (bottom to top) when `robotObstacles` is on (the older `elements` only for fields without them), and names contacts with WorkerBot's rule. Bridge tests use `BridgeProtocol.VERSION`. C16.2: `FieldObstacleRecordingsTest` (2) remakes `c15-flower-bracket-bounce.json` and `c15-foot-bounce.json` on every run from a set start pose (no bridge): balls roll under the blue-wall FLOWER head to its brackets, and to the blue far foot and lip; checks use the true ball-to-box distance. New tests: 4 in `UnityRobotPhysicsTests`, 1 in `BridgeUnityPhysicsTest`, 2 recording tests; all break-checked. App rebuilt; READMEs updated | Unity 90/90; Java 349/349 (clean full run, 23:20) |
| 2026-10-28 | **W12** (WorkerBot). W12.1 (D10): config key `smokeTest.skipOpModes` (PVI: `KimCode TeleopV1`; `HarnessConfig.smokeTestSkipOpModes()`) for TestBot's smoke test; `devices.webcams` and RP7's other device keys were already in (W10.2, W11; TestBot set PVI's webcam). W12.2 (D11): `viewer/Scenarios.java` no longer imports any TeamCode class. PVI's five hand-made scenarios moved unchanged to `src/pviViewer/java/pvi/harness/viewer/PviScenarios.java`, added to the viewer source set only with `pvi.extras=true`; `Scenarios.all()` returns them when built (PVI's dropdown is unchanged), otherwise one generic scenario per `OpModeCatalog` OpMode (autonomous 30 s, TeleOp standing still 3 s, minus `smokeTest.skipOpModes`). Checked: `pvi.extras=false` compiles main, viewer and bridge with no PVI class. New `ScenariosTest` (3), break-checked; PVI viewer tests call `PviScenarios`. Also D12: `BatteryTest`'s DECODE check runs `withoutRobotObstacles()` (DECODE-season path) | viewer, config and battery classes pass; full run after TestBot's D12 `PedroRouteTest` change |
| 2026-10-28 | **W11: the robot is blocked by the HIVE frame and FLOWERs** (WorkerBot, manager decision D9): `MomentumPhysics.setObstacles` (both its paths, with and without momentum) pushes the robot box out of any of the 18 `BioBuzzObstacles` it overlaps, cancels its speed into it and lets it slide along, like a wall; the robot is a box from the tiles up, so the FLOWER heads count. Motion is checked every 0.1 in, so a fast robot can't jump the 0.2 in lip. `PhysicsBackend.State` gains `obstacleContact` (the old 3-argument form stays, so Unity's bridge is unchanged); `PoseEstimator.obstacleHits()`, `withoutRobotObstacles()`, report line, recording events "Robot hit the HIVE frame" / "… FLOWER". New config key `field.robotObstacles=true`. Balls squeezed between robot and frame are no longer carried through. Also RP7 batch 2 keys (`devices.analogInputs`, `otos`, `limelights`, `webcams`, `deadWheels`) and `hardware.servos`; `addWebcam` returns an existing camera of that name. New `RobotObstaclesTest` (7, each break-checked, incl. a 100 ft/s robot that would otherwise jump the lip); `PoseEstimatorTest` (motion math from the field center) runs without field elements. **Finding:** Team A's DECODE Route, Curving Test and Illegal Path (DECODE-season paths) drive into the BIOBUZZ HIVE frame and never finish (DECODE's end point lies on the blue lip); LP-11 is fine. Route-test handling waits on the manager | 331 run, all pass except the 4 route tests (finding above) |
| 2026-10-27 | **RP7 more hardware fakes** (TestBot; add* methods, the runner's `SimulatedSensors.stepAll` line and the `devices.*` config keys agreed with WorkerBot): IMU (yaw/rate from the physics, resetYaw, drift/offset, any axes order), CR servo, distance (infinity until scripted), touch (TouchSensor + DigitalChannel, REV inverted state), color (normalized + raw with gain, plus distance), analog (0-3.3 V), OTOS (own frame, units, scalars), dead wheels (parallel/perpendicular pods driving a port's encoder from real motion, honor STOP_AND_RESET) and Limelight 3A (JSON from a `LimelightView` through the SDK's own `LLResult` parser, no network). One `SensorScript` timeline on `FakeHardwareMap.sensorScript(...)`, per-kind values. Motion-driven ones are readings only (mecanum + Pinpoint stays the position source). `SensorFakesTest` (8) and `MoreSensorFakesTest` (4) with TeamCode-style sample OpModes that read the config's drive names and directions (`ConfigDrive`). Break-checked (11 breaks, all caught), restored | 12/12 pass |
| 2026-10-27 | **T8.2 Illegal Path** (TestBot): the 4th route is followed like the others (10.22 s, 0.286 in, 0.78 deg, no walls); new `PedroRouteTest.illegalPathIsFollowedLikeAnyOtherRoute`, break-checked. (Before W11's HIVE/FLOWER obstacles; see WorkerBot's W11 note) | 5/5 pass (pre-W11) |
| 2026-10-27 | **RP10 started** (TestBot): `BRING_YOUR_OWN_ROBOT.md` (copy, pvi.extras=false + the 26 PVI test files to delete, settings table, smoke test, first test; linked from GETTING_STARTED, README top, robot-recorder), the `MyFirstRobotTest` template (finds the TeleOp by Driver Station name, so it compiles in any repo; break-checked), `EveryOpModeSmokeTest` (@TestFactory over `OpModeCatalog.fromConfig`, plain `missing device "x" (Type)` lines; parked until the manager decides about KimCode TeleopV1). `devices.webcams=logitechVisionWebcam` for PVI; `AprilTagVisionTest`'s no-camera case uses `without(WEBCAM)` | changed classes pass |
| 2026-10-27 | **C15.2 ball bounces checked in Unity** (UnityBot): with WorkerBot's W9.2 collisions, three recordings show it: `obstacle-bounce.json` (WorkerBot's test: pre-loads bounce off the blue frame lip at y 22.76), and two driven through the bridge with scripted sticks, `c15-flower-bracket-bounce.json` (a POLLEN rolls under the blue-wall FLOWER head and bounces off its bracket at y 67.82; another glances off the bracket corner) and `c15-foot-bounce.json` (a column of POLLEN stops against the blue far foot at y 21.4). Pictures in `build/unity-c15-*.png`. New in the replay: **O** draws the 18 `pieceObstacles` in magenta (`FieldBuilder.BuildObstacles`), start-up options `-obstacles` and `-at <seconds>`. 2 tests (`ObstacleDrawingMatchesTheFile`, `AtOptionGivesTheStartSeconds`), each break-checked. App rebuilt. Seen: a robot pushing a ball column into the frame squeezes the front ball in (the robot isn't blocked by the frame, a known W9.2 limit). A break-check run leaves a bad `obstacle-bounce.json` until the next clean run | Unity 87/87; Java GamePiecesRecordingTest 5/5 |
| 2026-10-27 | **W10.2: RP7 device keys** (WorkerBot): config v3 adds `devices.imu`, `devices.crServos`, `devices.distanceSensors`, `devices.touchSensors`, `devices.colorSensors` (name lists, may be empty; PVI's are empty because Team A's TeamCode uses none of them yet); `HarnessConfig` getters; `RobotConfigs.robot(config)` adds each named device through TestBot's `FakeHardwareMap.add*`. New `RobotConfigsTest.theDeviceListsAddTheirDevices` (break-checked). Also, for UnityBot's C15.2: `GamePiecesRecordingTest.aPushedPreloadBouncesOffTheHiveFrameLip` saves `build/recordings/obstacle-bounce.json`, a real run where a pre-load bounces off the blue HIVE frame lip (break-checked). It showed a limit: the simulated robot isn't blocked by the HIVE frame (only the walls), so a ball squeezed between the robot and the frame is carried through; the test stops the robot short of the lip | 318/318 pass |
| 2026-10-27 | **W10.1: live-drive TeleOps from the catalog** (WorkerBot, manager decision D8): `LiveDrive.options()` lists every `@TeleOp` from `OpModeCatalog` (each on `RobotConfigs.forOpMode`), the one in the new config key `viewer.defaultTeleOp` first (PVI: `TeamATeleOp`, so the viewer opens on Team A's TeleOp as before); `LiveDrive.defaultOption()`; empty = first by Driver Station name; unknown = plain error listing the TeleOps. Config **v3**. Dropdown labels are now Driver Station names ("Drive: Team A Mecanum Drive (keyboard)") and saved drives use the class name (`drive-TeamATeleOp-…`). Tests use `defaultOption()`; 2 new `LiveDriveTest` tests. Break-check: ignoring the configured default failed 6 `LiveDriveTest` tests | 318/318 pass (53 classes, with W10.2 and TestBot's RP7) |
| 2026-10-27 | **W9.3: Step 15, option A** (WorkerBot): new `viewer/AfterTestsViewer` and a `viewAfterTests` Gradle task that `test` is finalized by. With `viewer.open=true` it opens the viewer after the tests (pass or fail) on the run's newest recording, or the failed test's newest `<TestClass>-*.json` (failed classes read from this run's JUnit XML); no recordings or no screen = one line, no window. Default `false`: the task is skipped. Checked: with `viewer.open=true` and no screen, the task runs and stays closed; with a real screen not tried here (it would open a window on this PC). New `AfterTestsViewerTest` (4), each break-checked (ignore failures, oldest first, old result files). README, `harness.properties` comment, `05-desktop-viewer.md` §3 | `AfterTestsViewerTest` 4/4; full suite below |
| 2026-10-27 | **W9.2: balls bounce off the HIVE frame and FLOWERs** (WorkerBot): new `physics/PieceObstacle` (an upright box or cylinder with a bottom height; a ball reaches it only if `bottom < 2r`) and `BioBuzzObstacles` (the 18 shapes from UnityBot's `fields/biobuzz-2026.json` "pieceObstacles", copied as constants with the file as the source). `GamePieces` takes an obstacle list (the old constructor means none) and, whenever the walls are on, pushes a loose ball out of any obstacle it reaches and bounces it with the wall bounce; `PoseEstimator` uses the BIOBUZZ list. New `BioBuzzObstaclesTest` (5): the constants equal the file, a POLLEN bounces off a FLOWER bracket and off the HIVE frame lip and is never inside an obstacle, a NECTAR rolls straight under a FLOWER head, the same push gives the same path. Break-checks: bounce off, "always reached" and one number changed each failed the right tests. Existing game-piece and recording results unchanged | `BioBuzzObstaclesTest` 5/5, `GamePiecesTest`, `GamePiecesRecordingTest`, `GamePiecesDrawingTest` pass; full suite at the end of W9 |
| 2026-10-27 | **W9.1: RP5, part 1** (WorkerBot): `RobotConfigs.forOpMode(OpModeCatalog.Entry, HarnessConfig)` (manager decision D7: the config's robot; with `pvi.extras=true` the "Team B" / "Team C" Driver Station groups keep teamB / teamC). `RecordedTestWriter.findOpModeClass` uses `OpModeCatalog` instead of searching `.java` text (the 2-argument form still works); `MakeRecordedTest` uses it. New `RobotConfigsTest` (2). The bridge part (BridgeOpModes from the catalog, ids become class names) is proposed to UnityBot, whose code it is. `LiveDrive.options()` kept as a hand list: from the catalog its first TeleOp would be "KimCode TeleopV1", which can't run on Team A's robot (decision asked of the manager) | `RobotConfigsTest` 2/2, `RecordedDriveToTestTest` pass |
| 2026-10-27 | **C15.1 Live mode lists every TeamCode OpMode** (UnityBot, manager decision D7): `BridgeOpModes` no longer has a hand list of 5; it offers every `OpModeCatalog.fromConfig` OpMode (10 today) in Driver Station name order, id = class simple name (`TeamATeleOp`, `TeamAAutoOpMode`, `RumbleDemoTeleOp`, ...; full class name if two share one), title = Driver Station name, robot = WorkerBot's `RobotConfigs.forOpMode` (config robot; Team B / Team C groups keep their robots with `pvi.extras=true`). `BridgeSession` reads `WheelReader.fromConfig`. Welcome layout unchanged, so bridge protocol stays v4; the 4 bridge tests and Unity's `LiveSmokeMenu` use the new ids. New `BridgeOpModesTest` (2, break-checked); README updated. Unity Live Mode smoke test in batch: 10 OpModes offered, TeamATeleOp ran with harness and Unity physics, bridge stopped | Java 296/296 |
| 2026-10-27 | **RP6 LinearOpMode support** (TestBot; OpModeRunner/SimTime/HarnessAgent edits agreed with WorkerBot): `runtime/LinearOpModeDriver` runs `runOpMode()` on one daemon thread in lock-step (two semaphores, only one thread runs at a time) with a 3 s real-time watchdog. Sync points: `waitForStart` (one loop per check), `idle`/`opModeIsActive` (one loop), `sleep`/`Thread.sleep` (exact simulated ms), `isStarted`/`isStopRequested` (one loop per 2 checks). The agent sends robot code's `Thread.sleep`/`yield`, and LinearOpMode's `isInterrupted`/`wait`, to new `SimTime` hooks, which pass through on every other thread. New `LinearOpModeHarnessAccess` (SDK package) sets `isStarted`/`stopRequested` itself instead of calling `internalOnStart`/`internalOnStopRequested` (they would deadlock on `runningNotifier` / assert on the SDK executor) and skips `internalRunOpMode`'s Android log. `LinearOpModeTest` (11): sleep(1000) = 50 loops, Thread.sleep, same result every run, waitForStart, opModeIsActive and opModeInInit per loop, stop in INIT, crash into RunResult, early finish, watchdog, a catalog-found LinearOpMode. Break-checked (6 breaks, two runs), restored. Also for Pedro 3: `PedroRouteTest` reads "Selected Route" and `route.getEnd*()` (LP-11 0.155 in, DECODE 0.192 in, Curving 0.157 in); `OpModeCatalogTest`'s list (TeamAPedroTuning gone, KimCode TeleopV1 added) | 292/292 pass |
| 2026-10-27 | **C14.1 ball obstacles from the CAD** (UnityBot): `fields/biobuzz-2026.json` is field format **version 2** with a new `pieceObstacles` list (README "Season field files"): 18 shapes a ball rolling on the tiles can hit, measured from the official CAD at ball-center height (1.4 to 1.8 in), each with straight sides from `bottomInches` to `topInches`. HIVE frame: per side, the foot bar's 2.15 in lip and a foot at each end. FLOWERs: two wall brackets each, and the head as a cylinder starting 3.75 in up (balls roll under it). Left out: HIVE baskets (above 30 in) and parts under 0.6 in. Format agreed with WorkerBot for the Java collisions (W8 follow-up). Unity reads versions 1 and 2 (`PieceObstacleData`, checks); new `PieceObstacleTests` (3) cast level rays at the CAD meshes and find every reachable side within 0.3 in; fail-checked (a foot side, the lip top and a FLOWER head bottom each failed its own test). App rebuilt; BIOBUZZ loads in it | Unity 85/85; Java not rerun (no Java change) |
| 2026-10-27 | **W8: harness on Pedro 3** (WorkerBot), after the manager merged `origin/Pedro-3-Implementation` (`0f9c602`; TeamCode and `build.dependencies.gradle` only). `build.gradle` now also reads the robot build's Maven repositories from `host.dependenciesFile` (Pedro 3's `repo.dairy.foundation`; the Panels repo is gone), so no library URL is typed in the harness; the libraries already came from that file (now `pedropathing:revhub:3.0.2`, `tuning:1.0.1`). All of TeamCode (new `pedro/` tuners, `KimCode/TeleopV1`, rewritten Team A Pedro files) compiles unchanged. PVI extras: `TeamAPedroHarnessAccess` reads Pedro 3's `ForesightConfig` / `MecanumConfig` / `PinpointConfig`; `TeamAConfigMatchesPedroTest` compares against them (Team A's Pedro 3 values equal `harness.properties` exactly, so the config didn't change); `PedroBatteryTest` removed because Pedro 3 has no voltage compensation, replaced by `PedroMecanumTest` (Pedro 3's mecanum drives every simulated wheel forward). `RunnerAndMenuTest`: Pedro 2's `SelectableOpMode` is gone, so its menu test uses a test-only D-pad menu OpMode, and the Team A menu test checks D-pad down steps "Selected Route" through all four routes. `BatteryTest` / `SensorsTest` route checks read "Selected Route" and work out the end error from "Observed End" and the route's end point (Pedro 3's OpMode shows no "Path" / "Expected End" / "End Position Error"). `FakePinpoint` and the agent's package list needed no change (Pedro 3 calls only Pinpoint methods the fake already has; still `com.pedropathing.*`). Break-checks: a flipped wheel direction failed `PedroMecanumTest` and the match test; wrong strafe speed, coasting and Pinpoint name failed the match test; wrong expected menu values failed both menu tests. `PedroRouteTest` (TestBot's) updated by TestBot for the new captions; on Pedro 3 the routes finish LP-11 1.06 s / 0.16 in, DECODE 13.54 s / 0.19 in, Curving 17.78 s / 0.16 in (Pedro 2: 0.96 / 0.24, 12.86 / 0.10, 19.56 / 0.25), all within the existing tolerances, so no expected value changed; `CALIBRATION.md` route table updated | route classes pass (PedroRouteTest, BatteryTest, SensorsTest); full run by TestBot next |
| 2026-10-27 | **W7: official ball facts** (WorkerBot): checked FIRST's game materials page; the manual copy in `fields/sources/` is the latest (TU06, 2026-10-22), so nothing was downloaded. Section 9.8 gives POLLEN ≈ 2.8 in and NECTAR ≈ 3.6 in Gopher ResisDent polyethylene balls and no mass, bounce or friction. New `GamePiece.POLLEN_DIAMETER` / `NECTAR_DIAMETER` (official, with that source; `radiusInches` now derives from them, same 1.4 / 1.8 in); the wrong "section 10.3.1" size citation fixed; the six `PieceModel` placeholders keep their numbers and say the manual gives none. No number changed. New `GamePiecesTest.ballSizesAreTheOfficialManualsAndTheRestStayPlaceholders` (break-checked: a 2.79 in POLLEN made it fail) | 281/281 pass |
| 2026-10-27 | **C13.1 crowd frame rate** (UnityBot; target 30 fps, user's decision): new opt-in `-fpslog <file>` / `-fpsseconds` start-up option (`FrameRateLog.cs`: 5 s warm-up, then average, slowest-frame and 1%-low fps written to the file, app closes). Measured the built app with the BIOBUZZ CAD field and the full competition-hall crowd, 10 s per view: presets 1, 2, 3, 5, 7 and a zoomed-out 3D overview all averaged 60.0 fps with a slowest frame of 59.9 (the vsync limit) on an RTX 2060 at 2560×1369, Ultra quality. Above 30 everywhere, so the crowd is unchanged. Pictures and numbers in `build/fps-hall-*.png/.txt`. 2 `FrameRateLogTests` (summary break-checked) | Unity 82/82; Java not rerun (no Java change) |
| 2026-10-27 | **RP8** (TestBot): `runtime/OpModeCatalog` finds every `@TeleOp` / `@Autonomous` OpMode by reflection in the packages named by `teamCode.opModePackages` (key added by WorkerBot), leaving out `@Disabled`, abstract and non-OpMode classes; plain `Entry` records (class, name, group, period, preselectTeleOp), sorted by name; classes loaded without static init. `OpModeCatalogTest` (10) with test-only samples in `runtime/catalogsample`: TeamCode's 10 OpModes found with the right names, groups and periods; a new OpMode shows up with no list edited. Break-checked (no `@Disabled`/abstract skip, no name fallback, wrong TeleOp period: 6 tests fail), restored. README "Finding the robot's OpModes" | 280/280 pass |
| 2026-10-27 | **W6: RP2** (WorkerBot): the runner, physics and fake robot read `harness.properties` instead of Team A's Pedro constants. `WheelReader`, `DriveModels`, `Dynamics` and `RobotConfigs` get `fromConfig(HarnessConfig)` (and `RobotConfigs.robot(config)`); their `teamA()` keep their names and results and read the file. None imports Pedro or `TeamAPedroHarnessAccess` any more. Config **v2** adds `robot.name`, `hardware.otherMotors`, the four `drive.*.direction`, forward/strafe speeds, forward/lateral coasting (Team A's Pedro values copied exactly, marked measured, same sources), `pvi.extras` and TestBot's `teamCode.opModePackages`. `TeamAPedroHarnessAccess` moved to `src/pvi/java` and is built only with `pvi.extras=true` (with `src/pviTest/java`). New PVI extra `TeamAConfigMatchesPedroTest` (4) checks the copies against Team A's Pedro configuration and TeamCode's hardware names. Pedro's voltage-compensation test moved unchanged to `pvi/PedroBatteryTest`. `HarnessConfigTest` +2. Break-checks: 4 wrong config values made all 4 PVI checks fail; broken direction and list parsing made the 2 new reader tests fail; `pvi.extras=false` compiles everything with no access class. Not added: Pinpoint pod offsets (the fake Pinpoint takes them from the robot code, which sets them through Pedro) | 279/279 pass (incl. TestBot's 9 `OpModeCatalogTest`) |
| 2026-10-27 | **C12.1 Live mode says when Java is missing** (UnityBot): before starting `gradlew unityBridge`, `HarnessLauncher.JavaProblem` looks for Java the way gradlew.bat does (JAVA_HOME, else Android Studio's jbr, else java.exe on the PATH) and reads its `release` file. With no Java, a JAVA_HOME without java.exe, or Java older than 17, Live mode fails at once with "Live mode: Needs Java 17 or newer: install Android Studio or a Java 17+ JDK (adoptium.net) and set JAVA_HOME (… is Java 11)" instead of "The harness side didn't start."; other failures are unchanged. 2 new `HarnessLauncherTests` (break-checked: accepting Java 11 failed exactly those 2); unity/README updated. App rebuilt; checked in my own copy with a fake Java 11 (message shown, no bridge started) | Unity 80/80; Java not rerun (no Java change) |
| 2026-10-27 | **W5: Java 17+** (WorkerBot): `build.gradle` no longer pins a Java 25 toolchain. It builds and runs on the Java that runs Gradle (stops with a plain message below 17), and `--sun-misc-unsafe-memory-access=allow` is added to test / viewRecording / makeTest / unityBridge only on Java 23+. `view.bat` finds Java (Android Studio's jbr, then `JAVA_HOME`, then PATH), reads its version and explains what to install if it's missing or older than 17 (detection dry-run checked for jbr, JAVA_HOME, bad JAVA_HOME and no Java; version parsing for 8/11/17/21/25). README "What you need" + "Running on another PC"; one-line pointer in GETTING_STARTED (TestBot's OK). unityBridge's name, folder and port argument unchanged. **Not yet run on a real Java 17**: the download was approved and verified (SHA-256), but running it was blocked by a permission check, so it waits on the user | Java 25: 264/264 pass |
| 2026-10-25 | **W4: RP4 + RP1** (WorkerBot): new `harness.properties` (config v1: TeamCode path, host dependency file, Android-only skip list, drive motor and Pinpoint names, robot facts as `number \| measured or placeholder \| source`, field, `viewer.open`). `build.gradle` reads it: TeamCode path from the file, robot libraries from `../build.dependencies.gradle` minus `host.skipLibraries` (same 7 as before), passed to tests as `harness.robotLibraries`; clear errors for a missing key, bad path or wrong version. New `runtime/HarnessConfig` (reads the file, lists every problem at once) and `HarnessConfigTest` (8, each failed in one of two break rounds). README "The config file", maintainer note and `03-workflow.md` updated. Not connected to the runner yet (RP2); agent package list not moved | 264/264 pass (TestBot's combined run) |
| 2026-10-25 | **T5** (TestBot): the harness `AprilTagProcessorImpl` copy gets the SDK's `TAG` ("AprilTagProcessorImpl"); `SdkCompatibilityTest` now also compares public fields (name, type, static, final). Its library checks follow RP4: the Vision version and the harness's library list come from `harness.robotLibraries`, and the version test checks they equal `../build.dependencies.gradle` minus `host.skipLibraries`, and that every skipped name is in that file. Break-checked: TAG removed, a stale skip name added; both restored. Full run includes WorkerBot's RP4 (`HarnessConfigTest`) | 264/264 pass |
| 2026-10-25 | **C11.1 HIVE NECTAR positions** (UnityBot, agreed with WorkerBot): the 6 NECTAR in the upward CELLs were a level row (z 51.8) on a CELL floor tipped 30°, so they floated up to 2.8 in or sank 0.8 in. Measured the CELLs in the official CAD (floor, back wall and alliance-side wall, tipped about the pivot at z 43.95) and placed them per manual 10.3.1: `BioBuzzPieces` CELL_X {9.57, 12.69, 15.80}, CELL_Y 21.03, per-ball CELL_Z {49.86, 51.66, 53.46} (were {7.37, 10.97, 14.57}, 20.95, 51.8); blue mirrored. Unity drawing was already right. New `GamePiecesTest.cellNectarSitOnTheTiltedCellFloorAgainstTheBackWall` and Unity `CellNectarTests` (rays against the CAD HIVE meshes), both break-checked against the old numbers. Checked in the app (pictures `build/unity-c11-cell-nectar-preset5/6.png`) | Java 256/256, Unity 78/78 |
| 2026-10-25 | **RP9** (TestBot): `SdkCompatibilityTest` checks that the harness copies of `VisionPortal` / `AprilTagProcessorImpl` have every public constructor and method of the Vision jar's classes (nested classes too; loaded in a separate class loader), that `OpModeRunner`'s reflected `OpMode` fields `stopRequested` and `internalOpModeServices` still exist with usable types, and that `build.gradle`'s robotLibs versions match `../build.dependencies.gradle`. Each checked to fail (renamed `VisionPortal.makeMultiPortalView`, Pedro 2.1.1, wrong field name). Found, not fixed: the SDK's `AprilTagProcessorImpl` has a public `TAG` field the copy lacks (fields aren't checked yet) | 255/255 pass |
| 2026-10-25 | **Game pieces GP4** (UnityBot): bridge protocol v4 (`pieces` in `loaded` and each frame), Unity reads run recording v4 and draws POLLEN/NECTAR with the CAD balls, rolling the ones that move, in replays and live mode; `GamePieceTests` (4, break-checked); bridge tests updated to v4. Checked with a real recording (the teleop turn knocks the 4 pre-loads about 25 in) and in the rebuilt app | Java 252/252, Unity 77/77 pass |
| 2026-10-25 | Game pieces GP1–GP3 (WorkerBot): BIOBUZZ's 44 balls in every tracked run; the robot pushes loose ones; run recording v4 (`pieces` in header and loops); Swing viewer draws them; README. New `GamePiecesTest` (8), `GamePiecesRecordingTest` (4), `GamePiecesDrawingTest` (2); `RunRecordingTest` updated for v4. | New tests pass; full suite 252 run, 5 failed on the v4 change: 2 fixed (mine), 3 in the bridge and vision tests sent to UnityBot and TestBot |
| 2026-10-25 | **See-through walls and HIVEs** (user request): the BIOBUZZ CAD's clear panels (perimeter, red and blue HIVE) are drawn as see-through plastic with `Legacy Shaders/Transparent/Specular`, which the build always includes (the Standard shader's transparent modes were clear in the Editor but solid in the app, because the build left out that shader version); trim, rails and the HIVE frame stay solid. `FieldLayoutTests.WallAndHivePanelsAreSeeThrough` (break-checked). Field boxes made in the Editor tests now remove their colliders without an edit-mode error. Panels made clearer at the user's request (opacity 0.22 to 0.11) | Unity 73/73 pass |
| 2026-10-25 | **Real people** (user provided Mixamo characters): `People.cs` puts seated fans (frozen sit/clap poses, shared per character, no shadows) in the hall bleachers and the lower 5 rows of the stadium stands, and animated drive teams in alliance shirts at the hall's tables; random character, outfit colors, height and turn per person. `Editor/PeopleBuilder.cs` (**Harness → Build People**) and `PeopleImport.cs` turn the Mixamo downloads into `Resources/People` (59 MB); `PeopleTests` (4, break-checked). Not yet measured: frame rate of the full hall crowd in the app | Unity 72/72 pass |
| 2026-10-25 | **Camera preset viewpoints** (user request via WorkerBot): keys 1–7 and View buttons above the field: Audience, Red drive team, Blue drive team, Top-down, Corner, HIVE close-up, Robot chase (follows and turns with the robot). Eased moves; free orbit/zoom still work afterwards; camera can look up (pitch down to -30°) but never goes below the floor; `-preset N` start-up option. Mixamo character Ch08 imported as a humanoid (`Editor/PeopleImport`), clothes recolourable per person (not yet placed). 3 `OrbitCameraTests` (pitch sign break-checked) | Unity 68/68 |
| 2026-10-25 | **Settings art fixes** (user feedback): NASA models converted again keeping each part's own material (the astronaut was pink/green and the lunar module's 12 materials were merged into one); NASA's Space Exploration Vehicle (pressurized lunar rover, 413 k triangles) replaces the box rover (box stays the fallback); stadium benches removed (they read as troughs). People stay capsules until the user provides models (Mixamo; art-inbox README section 5). Checked in the built app | Unity 65/65 |
| 2026-10-25 | **Settings art** (user approved the list; Unity AI unavailable): downloaded 25 free assets (Poly Haven and ambientCG, CC0; NASA) into art-inbox/downloads, converted NASA's Draco .glb models to OBJ, and imported 219 MB into `Resources/Settings/<setting>`. `SettingAssets` (skybox per setting, textured surfaces with normal/AO maps, models sized to real size and stood on the ground, plain fallback), `Editor/SettingArtImport` (sky as cubemap, normal maps), camera shows the sky in 3D only, build keeps the Skybox shader and a normal-mapped Standard variant. Hall: Gym 01 sky, wood floor, block walls, laptops/gamepads/tool cart/chest. Stadium: night sky (exposure 0.12), grass, red rubber track, concrete, benches. Moon: NASA star map and Blue Marble Earth, moon dust, metal pad, NASA lunar module/astronaut/habitat, moon rocks. Credits in `Art/CREDITS.md`. 3 `SettingAssetsTests` (sizing break-checked; the field-clear test also caught the unscaled astronaut). Checked in the built app with pictures | Unity 65/65 |
| 2026-10-25 | **Official BIOBUZZ field CAD in Unity**: the user's CAD export (15.2 M triangles) was split, slimmed to 1.2 M and turned into the FTC frame by WorkerBot (art-inbox/processed). Imported into `Resources/Field/biobuzz-2026` (12 parts, 39 MB) and `Resources/GamePieces/biobuzz-2026` (balls); `Editor/FieldModelImport` (calculated normals 40°, no compression); `FtcToUnity.FtcModelRotation`; `FieldBuilder.BuildSeason` draws the CAD when present (and the file's parts outside the field), hiding the plain floor and walls; boxes stay the fallback. `fields/biobuzz-2026.json` updated with WorkerBot's CAD measurements (loading zones, gardens, frame base and top bar, flowers, all 16 tags; accuracy `cad`). Checked in the built app: top-down and 3D match the manual's layout. 2 new `FieldLayoutTests` (orientation, all parts present), the orientation test break-checked. Unity AI generation still blocked: no models offered (Unity account has no Unity AI entitlement) | Unity 62/62 |
| 2026-10-25 | Processed the official BIOBUZZ field CAD for UnityBot (art-inbox/processed/field, 15.2 M → 1.2 M triangles, FTC frame) and planned FW1: physics walls at the CAD's ±70.585 in. Docs only. | not run (no code change) |
| 2026-10-24 | Planned "Robot profiles and modularity" (RP1–RP5, WorkerBot): what the harness assumes about Team A today, and the profile, runner, drivetrain, build and bridge steps. Docs only. | not run (no code change) |
| 2026-10-24 | **Visuals screen and settings** (user request): the Field button is now **Visuals** (V), a screen to pick the arena (plain field or a season field) and the setting around it. `SettingBuilder`: Mission control (the old dark look), Competition hall (gym, bleachers with a seeded crowd, banners, drive-team stations, scoreboard, ceiling lights), Night stadium (grass, running track, stands, floodlight towers with spot lights, goal posts, stars, Moon), Moon base (regolith, craters, rocks, habitat domes, antenna, rover, stars, Earth); each sets the sky, fill light and sun. Scenery is decoration only: nothing above the floor inside the field square, no colliders, fixed seed. Choices remembered; `-setting`, `-view 3d`, `-zoom`, `-yaw` start-up options; zoom reaches 8x. 4 `SettingBuilderTests` (one caught a ceiling truss over the field, fixed); 3 breaks each failed exactly its test | Unity 60/60 |
| 2026-10-24 | **DECODE 2025 field switched off** (user request): `fields/decode-2025.json` moved to `fields/disabled/`, which Unity doesn't read, so the field picker offers only the plain field and BIOBUZZ 2026 (move the file back to switch it on). `FieldLayoutTests` updated (active fields = BIOBUZZ only; the DECODE file is still checked where it is). Windows app rebuilt with Live mode, Unity physics and this change | Unity 56/56 |
| 2026-10-24 | **Live mode button** (user request): Unity's **Live mode** button / L key starts the harness side itself (`HarnessLauncher`: hidden `cmd /c gradlew.bat unityBridge` from the Harness folder found next to the Editor or the .exe, JAVA_HOME or Android Studio's Java, output in `build/unity-bridge.log`; watches the port each frame without waiting), then lists the live OpModes. Off (or closing Unity) ends the run, Gradle and the bridge's own Java process (found with netstat); a hand-started bridge is used and left alone. The chooser's live row is a Start button too. 4 `HarnessLauncherTests`, each break-checked; **Harness -> Live Mode Smoke Test** in batch: launcher started the bridge in 2 s, harness and Unity physics passes ran (with a PS5 controller connected), and the port was free after Live mode off | Unity 56/56 |
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

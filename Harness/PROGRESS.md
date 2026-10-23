# Harness progress

Where the FTC test harness stands, what is being worked on and what comes next. Update this file in
the same change as the work (see [ai-instructions/03-workflow.md](ai-instructions/03-workflow.md)).

**Last updated:** 2026-10-23
**Test status:** 139 tests in 22 classes, all passing (`.\gradlew.bat test`, 2026-10-23)
**Git:** committed on branch `Team2-Testing` (local only, not pushed)
**Next up:** D4's viewer "Save as test" button (after the viewer redesign lands); position estimator Step 15 (opt-in switches; decide their behavior with the user); Step 9 (estimator README section) is still open

Status key: ✅ done · 🟡 partly done · ⏳ next · ⬜ not started · 💤 later / optional

---

## Harness phases

| Phase | What | Status |
|---|---|---|
| 0 | Scope and audit of the code (devices, hardware access points) | ✅ Done |
| 1 | Feasibility check: standalone build in `Harness/`, compile TeamCode, fake motors / hardware map / telemetry, first TeleOp test | ✅ Done |
| 2 | Scripted gamepads, full telemetry, full-match runs, crash capture, rumble recording, Pedro menu support, robot configs | ✅ Done |
| 3 | Java agent + simulated clock, so timed autonomous and match-timer rumbles are testable | ✅ Done |
| 4 | Vision: scripted AprilTag detections to test the `VisionSubsystem` state machine and `TeamAAprilTagVisionTestOpMode` | ⬜ Not started |
| 5 | Physics and Pinpoint: acceleration, fake encoders, fake Pinpoint so Pedro paths run, `PhysicsBackend` interface | 🟡 Partly done (the position estimator below) |
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
| 9 | README section for the estimator | ⏳ Next |
| 10 | Calibrate at the robot: measure turn rate and size, compare with a real autonomous run (team task) | ⬜ |
| 11 | Full run recording (pose, motor powers, step names, telemetry per loop) saved as JSON (`RunRecorder`, format v1 in README) | ✅ Done |
| 12 | Field drawing: 12 × 12 ft grid, robot box, heading arrow, trail (`viewer/FieldPainter`, PNG via `FieldImage`) | ✅ Done |
| 13 | Viewer window (Swing) with a side panel (`ViewerWindow`, `SidePanelText`, `gradlew viewRecording`) | ✅ Done |
| 14 | Playback controls: play/pause, speed, timeline slider, keyboard (`Playback`) | ✅ Done |
| 15 | Opt-in switches: `-Pview`, `simView`, `viewRecording`; no windows during normal runs (`viewRecording` exists from Step 13; behavior still to confirm) | ⏳ Next |
| 16 | Extras: HTML replay, Pedro path overlay, Panels feasibility check | 💤 Optional |
| 17 | Phase 5 upgrades: acceleration, encoders, fake Pinpoint, `PhysicsBackend` | 💤 Later |

---

## Drive and record a test

Drive the robot live in the viewer window (keyboard first, then a USB or wireless controller). The
recorded inputs become a new test. Agreed with the user on 2026-10-18.

| Step | What | Status |
|---|---|---|
| D1 | One gamepad input entry point in the core: scripts, live input (and later Unity) all feed `gamepad1`/`gamepad2` through it ([02](ai-instructions/02-unity-readiness.md) §5) (`ds/GamepadSource`, `runner.gamepad1(source)`) | ✅ Done |
| D2 | Live driving in the viewer with the **keyboard** (sticks on WASD / arrows, buttons on letter keys). Real-time pacing lives in the viewer; simulated time is unchanged (`LiveDrive`, `KeyboardGamepads`, "Drive: ..." in the dropdown; layout approved 2026-10-23) | ✅ Done |
| D3 | Converter: recorded gamepad timeline into `GamepadScript` steps (only the moments something changed, snapped to the 20 ms loop) (`recorded/RecordedInputs`: press + release = `hold`, sticks rounded to 2 decimals, 0.05 step and dead zone, notes for marked moments) | ✅ Done |
| D4 | Save as a new **generated `.java` test file** that students can open and edit: the `GamepadScript` from D3, automatic safe checks (no crash, powers within -1..1, robot on the field), a marked place for your own checks, optional snapshot checks (`recorded/RecordedTestWriter`, `DriveChecks`, `gradlew makeTest`; checks the drive already fails are written turned off with the reason; snapshots come from replaying the test's own inputs). Viewer "Save as test" button still to add | 🟡 Done except the viewer button |
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
| U1 | **Unity replays saved run recordings.** Unity reads the same JSON files as the Swing viewer (format v2) and animates a robot on a 3D field. No harness Java changes needed. | ⬜ Next (planning) |
| U2 | **Unity live, as display and controller.** The harness runs TeamCode and opens a local socket (`pvi.harness.bridge`); each step it sends the loop snapshot, Unity sends gamepad input back through the D1 gamepad entry point. Unity asks for each step, so runs stay repeatable. | ⬜ |
| U3 | **Unity becomes the physics.** Behind the `PhysicsBackend` interface (Step 17): wheel powers to Unity, pose (later encoders, Pinpoint) back. Walls, game pieces and pushing become real. | ⬜ |

Stage U1 in steps (Unity side unless marked):

| Step | What | Status |
|---|---|---|
| U1.0 | Decisions: where the Unity project lives (**decided: `Harness/unity/`**, 2026-10-23); Unity version (**6000.3.14f1**, the one installed); JSON library (**Newtonsoft**, chosen at setup, can still change); confirm the FTC→Unity mapping in [02](ai-instructions/02-unity-readiness.md) §6 (still open) | 🟡 |
| U1.1 | Unity project set up in `Harness/unity/`, JSON library installed; Unity's generated folders (`Library/`, `Temp/`, `Logs/`, `obj/`, `UserSettings/`, `Build/`) added to `Harness/.gitignore` (opened cleanly in Unity batch mode; packages: Newtonsoft JSON 3.2.2, Test Framework 1.6.0, AI Assistant 2.17.0-pre.1) | ✅ Done |
| U1.2 | C# data classes matching run recording format v2 (v1 files: no `events`) | ⬜ |
| U1.3 | Loader: open a file, check `header.version` is 1 or 2, clear errors | ⬜ |
| U1.4 | `FtcToUnity`: the one place that converts inches/radians to Unity metres/degrees, with tests | ⬜ |
| U1.5 | Scene: 3.66 m field with 2 ft tiles and walls; robot box sized from `header.driveModel` (placeholder sizes marked) | ⬜ |
| U1.6 | Playback: timeline on `sinceInitSeconds`, play/pause/speed/slider, smooth movement between 20 ms loops | ⬜ |
| U1.7 | Overlays: trail, telemetry screen for the moment, gamepads, rumbles and events | ⬜ |
| U1.8 | Tests (Unity Test Framework): parse a sample recording, conversion numbers, playback timing | ⬜ |
| U1.9 | (Harness) README section "Watching a recording in Unity"; a small sample recording the Unity tests can use | ⬜ |

Done when: replaying `viewer-teamA-autonomous.json` in Unity shows the robot about 9.4 in
(0.24 m) forward at the end, matching the Swing viewer at the same moment.

Decided: the Unity project lives in **`Harness/unity/`**, next to the recordings it reads
(user, 2026-10-23). It is part of the harness, so the Harness rules cover it, and Gradle ignores
it (Gradle only builds `src/`).

Open decisions (ask the user): message format for U2 (likely one JSON message per line over TCP,
with a `version`), whether Unity drives the clock in U2 (recommended), and the unit mapping.

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
- Fake motor encoders stay at zero until Phase 5 physics moves them (`FakeDcMotorEx`).
- `Harness/build.gradle` library versions are copied by hand from `../build.dependencies.gradle`.
- `RobotConfigs.teamB()` and `teamC()` are copies of Team A for now.
- `Harness/README.md` §6 lists vision as "Phase 4" and field position as "Phase 5". Update it as
  those phases finish.

---

## Log

Newest first. One line per change: date, what changed, test result.

| Date | Change | Tests |
|---|---|---|
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

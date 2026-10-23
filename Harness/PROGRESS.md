# Harness progress

Where the FTC test harness stands, what is being worked on and what comes next. Update this file in
the same change as the work (see [ai-instructions/03-workflow.md](ai-instructions/03-workflow.md)).

**Last updated:** 2026-10-23
**Test status:** 139 tests in 22 classes, all passing (`.\gradlew.bat test`, 2026-10-23)
**Git:** committed on branch `Team2-Testing` (local only, not pushed)
**Next up:** position estimator Step 15 (opt-in switches; decide their behavior with the user); Step 9 (estimator README section) is still open

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
| 7 | Unity bridge: Unity replaces the physics backend over a socket | ⬜ Not started |

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
| D3 | Converter: recorded gamepad timeline into `GamepadScript` steps (only the moments something changed, snapped to the 20 ms loop) | ⬜ |
| D4 | Save as a new **generated `.java` test file** that students can open and edit: the `GamepadScript` from D3, automatic safe checks (no crash, powers within -1..1, robot on the field), a marked place for your own checks, optional snapshot checks | ⬜ |
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

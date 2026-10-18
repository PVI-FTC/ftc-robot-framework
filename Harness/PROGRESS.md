# Harness progress

Where the FTC test harness stands, what is being worked on and what comes next. Update this file in
the same change as the work (see [ai-instructions/03-workflow.md](ai-instructions/03-workflow.md)).

**Last updated:** 2026-10-18
**Test status:** 69 tests in 12 classes, all passing (`.\gradlew.bat test`, 2026-10-18)
**Git:** committed on branch `Team2-Testing` (local only, not pushed)
**Next up:** position estimator Step 7 (more real-code motion tests)

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
| 7 | More real-code motion tests: turning, a miswired motor causing drift, STOP freezing position, on-field check | ⏳ Next |
| 8 | Save the track as a CSV for graphing | ⬜ |
| 9 | README section for the estimator | ⬜ |
| 10 | Calibrate at the robot: measure turn rate and size, compare with a real autonomous run (team task) | ⬜ |
| 11 | Full run recording (pose, motor powers, step names, telemetry per loop) saved as JSON | ⬜ |
| 12 | Field drawing: 12 × 12 ft grid, robot box, heading arrow, trail | ⬜ |
| 13 | Viewer window (Swing) with a side panel | ⬜ |
| 14 | Playback controls: play/pause, speed, timeline slider, keyboard | ⬜ |
| 15 | Opt-in switches: `-Pview`, `simView`, `viewRecording`; no windows during normal runs | ⬜ |
| 16 | Extras: HTML replay, Pedro path overlay, Panels feasibility check | 💤 Optional |
| 17 | Phase 5 upgrades: acceleration, encoders, fake Pinpoint, `PhysicsBackend` | 💤 Later |

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
| 2026-10-18 | First commit of `Harness/` (local, not pushed); removed stray `__pycache__/pygame` file | 69/69 pass |
| 2026-10-18 | Replaced the guessed plan in this file with the team's phase and step tables | 69/69 pass (no code change) |
| 2026-10-18 | Added `AGENTS.md`, `CLAUDE.md`, `ai-instructions/` and this file (docs only) | 69/69 pass |

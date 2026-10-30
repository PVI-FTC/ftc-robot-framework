# PVI-FTC Implementation Status

PVI-FTC | Editable master guide

[Click here to read the ARCHITECTURE guide](../TeamCode/project-docs/ARCHITECTURE.md)

[Click here to read the STUDENT CODING WORKFLOW guide](../TeamCode/project-docs/STUDENT_WORKFLOW.md)

## Repository baseline
- Source repository: PVI-FTC fork of FtcRobotController
- Current sequential prompt: LP-11 four-path Pedro selector ready for deployment review
- Last completed prompt: LP-10 Pedro tuning and path-readiness acceptance
- Last verified commit: d7b9014 (LP-09 reviewed implementation)
## Team 3 Development Session Notes (October 30, 2026)
### Problems Addressed & Solutions Implemented
1. **Pedro 3 Autonomous Drift Fix**:
   - *Problem*: After following autonomous paths (e.g., 24-inch pilot), the robot drifted slowly backward and to the right without stopping.
   - *Root Cause*: `follower.holdEnd` was actively attempting to hold target position using continuous motor power after completion.
   - *Solution*: Set `follower.holdEnd.set(false)` in `Team3PedroFollowerFactory.java`, updated `Team3PedroDriveController.java` (`isPathFollowingActive()` checks `follower.following()`, `stop()` cuts drive motor power).
2. **Temporary Autonomous (2" Forward + 8" Right with Servo Deploy)**:
   - Created `Team3TempStrafeRight8AutoOpMode.java` (`@Autonomous(name = "Team 3 Temp Strafe Right 8 in", group = "Team 3 Testing")`) and `Team3PedroStrafeRight8Path.java` (`(0,0) -> (2,0) -> (2,-8)`).
   - *Requirement*: Servo must NOT move during `init()`, but at `start()` must deploy (matching 'A' in TeleOp). Added `robot.deployIntake()` inside `start()`.
3. **Intake-Deploy Servo Startup Behavior**:
   - *Requirement*: Robot starts with intake physically deployed on field; servo must NOT move or twitch at all during `init()` or TeleOp start, and the first press of 'A' must stow it.
   - *Solution*: Created `Team3IntakeNotMovedState.java` as initial FSM state in `Team3IntakeDeploySubsystem.java` (sends no commands until requested). Initialized `deployRequested = true` so the first press of 'A' transitions to `stowedState` (`STOWED_POSITION = 0.0`).
4. **Single-Gamepad Mechanism Controls & Power Tuning**:
   - Setup: Only Gamepad 1 exists (no Gamepad 2).
   - *Intake*: Added `setIntakePower(double)` and `setEjectPower(double)` to `IntakeSubsystem.java` (defaults remain 1.0 for other teams); `Team3Robot.java` exposes them. In `Team3TeleOp.java`: Left Trigger (hold) = 75% IN, Left Bumper (hold) = 15% OUT.
   - *Midtake*: Set `MIDTAKE_POWER = 0.5` (50% power in forward and reverse) in `Team3MidtakeSubsystem.java`. In `Team3TeleOp.java`: Right Trigger (hold) = 50% Forward, Right Bumper (hold) = 50% Reverse.
   - *Outtake*: Disregarded for now; commanded to `0.0`.
   - *Brake*: Mapped to Back button (`gamepad1.back` hold). Cleared old idle auto-run toggle from Back button.
   - *Heading Snaps*: Removed all snap logic on X, B, D-pad Left, D-pad Right to clear collisions.
   - *Pose Lock*: Kept Y button toggle.
5. **Controller Hardware Diagnostics for Future AI**:
   - Driver uses a gamepad with rear ring-finger paddles/buttons.
   - Hardware Diagnostic: Left rear paddle is factory hardware-mapped to duplicate `gamepad.a`, while right rear paddle is unprogrammed.
   - *Note for Future AI*: Android and FTC SDK cannot distinguish physical rear paddles from the standard buttons they duplicate because the controller chip transmits standard HID codes. Ring-finger paddles can only be used by hardware-programming them on the controller to mirror unused front buttons (e.g. stick clicks or D-pad).

## Completed work
- Merged `origin/Pedro-3-Implementation` into `feature/team-3-setup`:
  - Upgraded Pedro Pathing from v2 to v3 (`com.pedropathing:revhub:3.0.2` and `com.pedropathing:tuning:1.0.1`).
  - Added Pedro 3 package `pedro` with `Constants.java`, `Tuning.java`, and AutoTune procedures (`ForesightTuner`, `MecanumTuner`, `PinpointTuner`, `Tests`, etc.).
  - AutoTune now runs directly through the robot-hosted web page at `http://192.168.43.1:10158`.
  - Completed Team 3 Pedro 3 Foresight migration:
    - Updated `Team3PedroConfiguration.java` and `Constants.java` with AutoTune Foresight constants: max velocities (`70.02 in/s` forward, `52.01 in/s` strafe), natural deceleration (`32.55 in/s²` forward, `62.90 in/s²` strafe), heading feedback (`5.60`), linear and quadratic braking matrices, while preserving measured physical Pinpoint pod offsets (`-8.0`, `-5.75` inches).
    - Resolved autonomous path endpoint drift: disabled `follower.holdEnd.set(false)` in `Team3PedroFollowerFactory.java` and updated `Team3PedroDriveController.java` (`isPathFollowingActive()` checks `follower.following()`, `stop()` explicitly commands `follower.drivetrain.stop()`) so paths complete cleanly and cut drive motor power when reaching the target instead of creeping in continuous hold.
    - Updated Team 3 TeleOp control mappings and mechanism subsystems:
      - `IntakeSubsystem.java`: Added `setIntakePower(double)` and `setEjectPower(double)` narrow shared APIs (defaults remain `1.0` so other teams are unaffected).
      - `Team3Robot.java`: Exposed `setIntakePower(double)` and `setIntakeEjectPower(double)`.
      - `Team3MidtakeSubsystem.java`: Set `MIDTAKE_POWER = 0.5` (50% power in forward and reverse).
      - `Team3IntakeDeploySubsystem.java` & `Team3IntakeNotMovedState.java`: Added initial `Not Moved` state ensuring the intake servo is never commanded during `init()` or `start()` until 'A' is pressed; configured to start physically deployed so the first 'A' press stows it.
      - `Team3TeleOp.java`: Configured single-gamepad mappings: Left Trigger = intake IN at 75%, Left Bumper = intake OUT at 15%, Right Trigger = midtake forward at 50%, Right Bumper = midtake reverse at 50%, Back button (hold) = brake, A = toggle intake deploy servo, Y = toggle XY Pose Lock. Disregarded outtake, removed heading snaps, and cleared idle intake auto-run toggle.
    - Added and updated temporary forward-then-strafe autonomous: created `Team3PedroStrafeRight8Path.java` configured to move 2 inches forward (`(0,0) -> (2,0)`) then 8 inches right (`(2,0) -> (2,-8)`), updated `STRAFE_RIGHT_8` in `Team3PedroPathRoute.java`, and configured `Team3TempStrafeRight8AutoOpMode.java` to deploy the intake servo at start (`start()`, matching pressing 'A' in TeleOp, kept stowed during `init()`) before following the path. Easily deletable later.
    - Updated `Team3PedroFollowerFactory.java` to construct `Follower` with `PinpointLocalizer`, `Mecanum`, and `Foresight`.
    - Updated `Team3PedroDriveController.java` to use Pedro 3 `follower.manual(forward, strafe, rotate)`, `follower.follow(Path)`, and `follower.pose()`.
    - Adapted all Team 3 path definitions to Pedro 3 `Paths.line(...)` and `Paths.curve(...)` building `Path` objects: `Team3PedroPilotPath.java`, `Team3PedroForward36Path.java`, `Team3PedroStrafeRightPath.java`, `Team3PedroCurvingTestPath.java`, `Team3PedroDecodePath.java`, and `Team3PedroIllegalPath.java`.
    - Updated `Team3PedroPathRoute.java` to return `Path` and use `com.pedropathing.math.Pose`.
    - Replaced the removed Pedro 2 `SelectableOpMode` in `Team3PedroPilotAutoOpMode.java` with a clean FTC `OpMode` offering interactive D-pad route selection in `init_loop()`.
    - Removed obsolete Pedro 2 monolithic tuner `Team3PedroTuning.java`.
    - Verified complete build with `TeamCode:assembleDebug` (BUILD SUCCESSFUL, 0 errors).
  integration, and OpModes for Team 3 under `robots.team3` and `opmodes.*`:
  - `robots.team3`: `Team3Robot`, `Team3PedroRobot`, `Team3PedroConfiguration`, `Team3PedroFollowerFactory`,
    `Team3PedroDriveController`, `Team3PedroPathRoute`, `Team3PedroPathStep`, `Team3PedroPilotPath`,
    `Team3PedroCurvingTestPath`, `Team3PedroDecodePath`, `Team3PedroIllegalPath`, and `package-info.java`.
  - `opmodes`: `Team3TeleOp` (TeleOp mecanum drive with InputManager), `Team3AutoOpMode` (timed/FSM AutoSequence demo),
    `Team3PedroPilotAutoOpMode` (Pedro autonomous path selector), `Team3PedroDiagnostic` (raised-wheel/push localization diagnostics),
    and `Team3PedroTuning` (Pedro interactive follower tuner with Panels dashboard integration).
  - All architecture boundaries and dependency directions preserved without modifying shared baseline classes or Team A files.
- (branch: feature/team-3-setup) Physical localization verification and automatic velocity tuning completed for Team 3:
  - Motor direction configured: `leftFrontMotorDirection` set to `REVERSE` in `Team3PedroConfiguration` matching physical wiring and mecanum drive.
  - Pinpoint odometry verified: `strafeEncoderDirection` set to `FORWARD` (ensuring positive Y on left translation), and `strafePodX` set to `-5.75` inches after mounting the strafe pod 5.75" behind the center of rotation. Spin test verified in `Team3PedroDiagnostic` (X and Y remained within ±0.5" over 3 full rotations in place).
  - Forward velocity tuned: 5-run average measured at `57.666 in/s` recorded in `Team3PedroConfiguration.xVelocity`.
  - Lateral velocity tuned: 5-run average measured at `58.178 in/s` (runs: 56.12, 55.90, 59.67, 59.20, 60.00) recorded in `Team3PedroConfiguration.yVelocity`.
  - Robot mass configured at `8.62 kg` (19 lbs nominal, representing the measured 17–21 lb complete mechanism range: 7.71 to 9.53 kg) in `Team3PedroConfiguration`. Deceleration and predictive braking remain at baseline values until physical powered path trials.
  - All architecture boundaries and dependency directions preserved without modifying shared baseline classes or Team A files.
- (branch: feature/team-3-setup) Added Team 3 mechanisms under `robots.team3`: `Team3MechanismHardware`
  (Expansion Hub motors `outtake` port 1 and `midtake` port 2, servo `intake-deploy` port 0, all optional),
  `Team3OuttakeSubsystem` (Idle/Spinning), `Team3MidtakeSubsystem` (Idle/Feeding), and
  `Team3IntakeDeploySubsystem` (Stowed/Deployed), each with its own FSM states. `Team3Robot` registers them
  and exposes `setOuttakePower(double)`, `getOuttakePower()`, `setMidtakeFeeding`, `toggleIntakeDeploy`,
  and `deployIntake()`. `Team3TeleOp` was remapped (gamepad 1): left trigger = intake, left bumper = eject,
  right trigger = outtake with variable power equal to trigger pull (0.05 deadband), right bumper = midtake feed,
  A = toggle intake deploy, X/Y = drive modes. Driver Station telemetry now reports `Outtake Power (%)` and
  `Right Trigger (%)`. Motor directions are SDK defaults (forward); servo `STOWED_POSITION = 0.0` and
  `DEPLOYED_POSITION = 0.5` are placeholders to tune on the robot.
- (branch: feature/team-3-setup) Narrow shared API addition: `IntakeSubsystem.setIdlePower(double)` (default
  0.0, clamped 0 to 1) makes `IdleIntakeState` run the intake forward at that power instead of zero. Teams that
  never call it are unchanged, and `stop()` still commands zero. `Team3Robot.setIntakeIdlePower` exposes it, and
  `Team3TeleOp` configures `IDLE_INTAKE_POWER = 0.30` controlled by a D-pad Down toggle on Gamepad 1 (starts OFF; driver can toggle ON/OFF during match). Also expanded `InputManager` to support D-pad queries (`wasDpadDownJustPressed`, etc.).
- (branch: feature/team-3-setup) Added Pinpoint-backed toggle heading hold for Team 3:
  - Created `Team3PinpointHardware`: hardware wrapper safely looking up goBILDA Pinpoint Odometry Computer, configuring pod offsets (-8.0, -5.75 inches), encoder directions, and IMU tracking.
  - Created `Team3HeadingHoldSubsystem`: Subsystem managing target heading lock and negative-feedback proportional heading correction (`KP = 1.0`, max power clamp `MAX_CORRECTION_POWER = 0.45`, deadband 0.5 deg). Corrected coordinate sign convention (CCW Pinpoint angle vs CW mecanum motor rotate command) to prevent positive feedback runaway spin. Automatically yields to manual driver steering on Right Stick ($> 0.05$) and re-locks target upon stick release.
  - `Team3Robot`: composed `pinpointHardware` and `headingHoldSubsystem`, injecting heading correction into `drive()`, and exposing heading queries (`getHeadingDegrees()`, `getTargetHeadingDegrees()`, `isHeadingHoldActive()`, `isPinpointAvailable()`).
  - `Team3TeleOp`: mapped Gamepad 1 button `Y` as a toggle (`wasYJustPressed()`) to switch heading hold ON/OFF (starts OFF), reporting live status, target heading, and Pinpoint availability on Driver Station telemetry.
- (branch: feature/team-3-setup) Upgraded to Full 3-DOF Pose Lock with Anti-Rubber-Banding & Deceleration Settling for Team 3:
  - Created `Team3DriveAssistLevel` (`OFF`, `HEADING`, `FULL`) and `Team3StabilizedDriveController` implementing `DriveController`.
  - Replaced `Team3HeadingHoldSubsystem` with `Team3PoseLock`: pure mathematical controller providing closed-loop 3-DOF pose stabilization (X, Y, Heading) using Pinpoint odometry and IMU.
  - Direction Holding Always On: Direction holding (heading lock) is active continuously by default on boot (`HEADING` mode). Driver steering takes immediate priority, settling smoothly (150ms) upon stick release.
  - Heading Priority Desaturation: When translating (forward/strafe) and correcting heading simultaneously, `Team3StabilizedDriveController` dynamically scales translation power down to preserve full rotation correction authority. This prevents mecanum motor saturation from inverting or choking heading correction during rapid back-and-forth strafing.
  - Toggleable Physical X/Y Position Hold (Y Button): Pressing `Y` on Gamepad 1 toggles the additional physical X/Y position hold (`FULL` mode vs `HEADING` mode). In `FULL` mode, the robot holds both (X, Y) and heading; if shoved or dragged 4+ feet, it returns to the exact locked spot. When toggled off, translation is 100% manual while direction holding remains on. Gamepad rumbles 2 blips for FULL, 1 blip for HEADING.
  - Anti-Rubber-Banding & Fast Stabilization:
    - Implemented `TranslationState` (`MANUAL_DRIVING`, `DECELERATING`, `LOCKED`).
    - On manual stick release in `FULL` mode, entered a brief deceleration settling phase (until chassis speed < 1.5 in/s or 180ms timeout) where active damping brings the chassis to a halt before freezing the anchor at the exact stopped coordinates. This eliminates freezing the anchor behind a moving robot and completely cures overshoot and rubber-banding.
    - True velocity damping: computed robot physical velocity $(\Delta \text{pose} / \Delta t)$ with low-pass filtering and damped directly against measured velocity, preventing derivative kick.
    - Continuous static friction ramp ($K_S$): replaced discontinuous sign steps with linear blending across 0.5 inches (and 2.0 degrees), eliminating limit-cycle chattering.
    - Zero-crossing integral reset: restricted integral accumulation to $\pm 2.0$ inches and immediately reset accumulators to 0 upon zero crossing to prevent pull-through overrun.
  - `Team3TeleOp`: Starts with direction holding ALWAYS ON (`HEADING` mode). `Y` toggles additional X/Y position lock. Cardinal heading snaps (X=0°, B=180°, D-pad Left=90°, D-pad Right=-90°) and right-bumper hold brake supported. Telemetry displays live state (`HEADING` target, `XY Pose Lock` status, speed in/s, anchor origin, and disturbance error).
- (branch: feature/team-3-setup) Added `Team3AutonomousOpMode` ("Team 3 Autonomous"), the primary
  autonomous OpMode for Team 3. On START it strafes 6 inches to the right using `Team3PedroRobot`,
  `Team3PedroPathStep`, `Team3PedroPathRoute.STRAFE_RIGHT_6`, and `AutoSequence`. At 2.0 seconds into the run,
  it deploys the intake servo to `DEPLOYED_POSITION = 0.5` (the same default position mapped to button A in
  TeleOp) via `robot.deployIntake()`. `Team3PedroRobot` was updated to compose `Team3MechanismHardware` and
  expose `deployIntake()`, `stowIntake()`, `toggleIntakeDeploy()`, and mechanism controls. Zero-power
  deceleration, Predictive Braking, and mass still hold earlier baseline values until physical assembly is finalized.
- Fixed the standalone `KimCode/TeleopV1` mecanum rotation and strafe mappings by negating the
  right-stick X and left-stick X inputs before passing them to Pedro's manual drive API. Forward
  and reverse mappings are unchanged; this corrects left/right rotation and strafe direction at
  the TeleOp input boundary.
- Pedro 3 migration completed: `revhub:3.0.2` and `tuning:1.0.1`
  replace the Pedro 2 artifacts. The Pedro 3 `pedro/procedures` sources are present, with
  `Constants.java` and `Tuning.java` registered for the AutoTune web server on port 10158.
  Existing Pedro 2-era Team A paths and the autonomous route selector were adapted to the
  Pedro 3 APIs. The obsolete Pedro 2 monolithic tuner was removed.
- Added independent `KimCode/TeleopV1.java`, adapted from the reference `pedro3` project. It does
  not depend on `TeamAPedroRobot` or `TeamAPedroPathStep`, uses `InputManager`, and owns follower
  and Limelight access in a private controller within the one-file KimCode entry point.

### Historical records below

Entries below this point preserve earlier prompt evidence and may describe the former Pedro 2,
Panels, or Predictive Braking workflow. They are historical records, not instructions for the
current Pedro 3 implementation.
- SDK 12 AprilTag compatibility fix (2026-10-07, `codex/fix-sdk12-apriltag`): the upstream
  SDK upgrade from 11.2.1 to 12.0.0 caused 11 compilation errors in
  `VisionPortalAprilTagSource` because individual-tag `id` and `metadata` fields are no longer
  exposed by the base `AprilTagDetection` type. The source now checks for
  `AprilTagSingleDetection` before conversion and uses that type in its private helpers.
  Cluster detections are skipped because this source verifies individual tags only; a fresh
  frame containing only clusters produces an empty fresh snapshot, clearing previous observations.
  Existing frame timestamps, retained-snapshot handling, metadata verification, and pose gates
  are preserved. No public API, FTC SDK source, dependency, or drivetrain behavior changed.
  JDK 17 `.\gradlew.bat --no-daemon --console=plain TeamCode:assembleDebug` passed
  (52 tasks, 0 errors), and `git diff --check` passed. Hardware vision validation and rollout
  through PVI master to active team branches remain pending review; no deployment occurred.
- (branch: feature/gamepad-rumble) Extended `core.util.RumbleManager` with two new public methods:
  `addTimerAlert(double secondsElapsed, int blips)` lets any team register additional match-timer
  rumble events at custom elapsed-time thresholds without touching the built-in 90 s / 105 s /
  115 s defaults. `rumbleNow(int durationMs)` fires an immediate rumble on the gamepad at the
  moment it is called, usable anywhere in a TeleOp `loop()`. Both methods are documented with
  Javadoc and usage examples. Updated `ARCHITECTURE.md` to document `core.util.RumbleManager` and
  clarify that direct gamepad access is permitted for feedback/haptic outputs while control input
  must strictly flow through `InputManager`.
  `TeamCode:assembleDebug` passed with `BUILD SUCCESSFUL` (52 tasks, 0 errors). Architecture
  boundaries are preserved: `RumbleManager` still holds only a `Gamepad` reference and contains
  no hardware map, FSM, subsystem, or autonomous logic.
- Added `PEDRO_ROBOT_SETUP_REFERENCE.md`, a reusable Pedro 3 setup and tuning guide grounded in the
  current Team A code and official AutoTune workflow. It inventories the pinned dependencies,
  hardware and Pinpoint facts, current config locations, procedure registration, build steps,
  localization/direction evidence, future-robot replacement map, and sign-off checklist. Older
  Pedro 2/Panels records are explicitly marked historical.
- LP-11 preserves the student-created Pedro Visualizer `1.2.1` export as
  `TeamCode/project-docs/TeamA_LP11_Pilot.pp`. The reviewed chain contains one straight line from
  `(0, 0)` to `(24, 0)` inches, no control points or waits, and an effective `0` degree heading.
  The code adaptation uses explicit constant heading rather than copying unrelated Visualizer
  simulation settings or generated framework code.
- Added `TeamAPedroPilotPath`, a Team A-owned Pedro `PathChain` definition, and the reusable
  `TeamAPedroPathStep`, a non-blocking `AutoStep` that requests the selected route once, checks
  completion on later loops, and cancels through the Robot API. `TeamAPedroDriveController` remains
  the follower owner and starts a selected route behind `TeamAPedroRobot.startPath(...)`.
- `TeamAPedroPilotAutoOpMode` now provides a four-route autonomous menu through Pedro telemetry
  `1.0.0`'s `SelectableOpMode`, exactly matching the tuning selector controls: D-pad moves the
  cursor, right bumper selects, and left bumper returns. The choices are the existing 24-inch LP-11
  pilot, the imported DECODE route, the imported Curving Test, and the imported Illegal Path. Only
  the selected inner OpMode initializes `TeamAPedroRobot`, advances one `AutoSequence`, calls
  `robot.update()` once per loop, and routes completion and FTC STOP through the existing
  cancel/stop paths.
- The imported DECODE route contains the Visualizer export's 11 straight segments and linear
  heading interpolations in sequence, starts at `(56, 8, 90 deg)`, and ends at
  `(48, 81, 135 deg)`. Route selection prepares the declared starting pose before START. Visualizer
  simulation settings were not imported, and the application power remains the single recorded
  `TeamAPedroConfiguration.APPLICATION_MAX_POWER = 0.20` value.
- The imported Curving Test contains the Visualizer export's one 10-segment chain: six quadratic
  curves and four straight lines. Its first segment uses linear heading interpolation from
  `90 deg` to `180 deg`; the remaining nine use tangential heading. It starts at
  `(56, 8, 90 deg)`, ends near `(24.307, 93.826, 179.848 deg)`, and contains no waits. Visualizer
  field shapes and simulation settings were not imported. This route has not been physically run.
- The imported Illegal Path contains the Visualizer export's one three-segment chain: one cubic
  curve, one quadratic curve, and one straight line. Its linear heading interpolations are
  `90 deg` to `180 deg`, constant `180 deg`, and `180 deg` to `90 deg`. It starts at
  `(56, 8, 90 deg)`, ends near `(103.425, 32.682, 90 deg)`, and contains no waits. Visualizer field
  shapes and simulation settings were not imported. This route has not been physically run.
- The director reported that the original LP-11 pilot path worked. Exact observed pose/error and
  stop telemetry were not supplied, so no values are inferred. The three imported routes have not
  been physically run. The four-route implementation passes
  `TeamCode:assembleDebug`; static review found no direct OpMode ownership of the follower, motors,
  or Pinpoint, no autonomous drive routed through gamepad input, and no blocking wait, loop, or
  extra scheduler.
- The latest completed automatic tuner outputs supersede the earlier velocity values:
  forward velocity is `62.61374213751846 in/s`, lateral velocity is `49.757958599901585 in/s`,
  forward zero-power acceleration is `-42.4832745291992 in/s^2`, and lateral zero-power
  acceleration is `-50.60694780564594 in/s^2`. `TeamAPedroConfiguration` stores the velocities in
  `MecanumConstants` and the accelerations in `FollowerConstants`; Predictive Braking values remain
  unchanged.
- LP-10 approach A was authorized by the director. The testing-only `TeamAPedroTuning` selector is
  adapted from official Pedro Pathing Quickstart commit
  `d3aea9ca3c5b4c09eded8580229b86996480ee89`, whose dependency file pins Pedro FTC `2.1.2`, Pedro
  telemetry `1.0.0`, and FullPanels `1.0.12`. The installed Pedro FTC AAR itself contains no tuning
  OpModes.
- `TeamAPedroTuning` constructs exactly one follower through `TeamAPedroFollowerFactory` and the
  recorded Team A configuration; it does not construct `TeamAPedroRobot` or call
  `HardwareMap.get(...)`. Irrelevant swerve-only tuners and their direct analog-sensor access were
  omitted from the mecanum integration. After the director confirmed the first ground-test safety
  gate, the selector exposes only a 24-inch forward shakedown at the `0.20` ceiling.
- The adaptation preserves the director-approved `0.20` tuning-power ceiling, caps the official
  predictive-braking power sweep to that ceiling, returns immediately from automatic tuner STOP
  requests, and gives every selected tuner a shared final STOP callback that cancels following and
  commands zero output. No path execution has occurred, no tuning values were accepted, and the
  path-following gate remains closed.
- On the first 24-inch forward shakedown, releasing right bumper after the movement produced a null
  `OpModeServices` exception from the selected inner tuner's `requestOpModeStop()` call. Pedro's
  `SelectableOpMode` creates the selected tuner internally rather than registering it as a standalone
  FTC OpMode, so that inner object has no service through which it can request its own stop. The
  zero-output call occurred before the exception, but no tuning result was accepted. The reachable
  shakedown now latches an aborted state, holds zero output, and instructs the operator to use Driver
  Station STOP instead of calling the unsupported service.
- The rebuilt 24-inch forward shakedown completed without an exception. The student measured
  `24.2 in` of travel, reported a raw `8.62 in/s` velocity observation, and described the final
  heading as slightly left by approximately 2%. Automatic stopping, bumper release, and Driver
  Station STOP all worked, and no unusual sound was observed. Start and end poses were not
  reported, so this trial does not establish pose drift or repeatability. The `8.62 in/s` result is
  retained as raw shakedown evidence only and is not an accepted forward-velocity constant.
- A second 24-inch shakedown measured `24.0 in`, reported `10.107 in/s`, and had no observed drift
  or turning. Driver Station telemetry again did not visibly report the captured start/end poses or
  pose delta. The two velocity observations differ by `1.487 in/s` (the second is about 17.3% above
  the first), so neither is accepted. The single next hypothesis is that a 24-inch run at the `0.20`
  ceiling did not yield a repeatable steady-velocity sample. The result display now removes the ten
  raw sample rows and uses only standard Driver Station telemetry for velocity and pose evidence.
- A third shakedown reported `9.997 in/s`, start pose `(72.000, 72.00) in`, end pose
  `(96.06, 71.83) in`, and delta `(24.06, -0.169, -1.28 deg)`. The second and third velocity
  observations differ by `0.110 in/s`, about 1.1% of their `10.052 in/s` average, supporting the
  hypothesis that the first `8.62 in/s` observation was an outlier. Manual measurement was exactly
  `24.000 in`; automatic stop, bumper release, and Driver Station STOP all worked; and no abnormal
  sound was reported. `10.052 in/s` is the candidate forward-velocity value pending explicit
  director acceptance.
- The director accepted `10.052 in/s` as Team A's forward velocity at the restricted `0.20` power
  ceiling. Pedro 2.1.2 stores this drivetrain value in `MecanumConstants`, so the recorded Team A
  configuration now applies `.xVelocity(10.052)`. The tuning selector is disabled again before the
  next category; lateral velocity and all later values remain unset, and path following stays
  locked.
- The director confirmed the lateral-motion safety gate: adult supervision, six feet clear to the
  robot's left, front/back clearance, secure hardware, charged battery, and a designated Driver
  Station STOP operator. The next build exposes only a 24-inch robot-left shakedown at the `0.20`
  ceiling. It uses the LP-09-verified negative Pedro strafe command, requires right bumper plus A to
  arm and continued bumper hold, and stops on 24-inch Y delta, bumper release, B, or Driver Station
  STOP. No lateral result has been accepted.
- The first lateral shakedown was rejected. It reported `6.62 in/s`, start pose
  `(72, 72, -3.39 deg)`, end pose `(69.61, 47.95, -6.74 deg)`, and pose delta
  `(-2.38, -24.04, -3.35 deg)` as calculated from the two headings. Manual travel was `24.3 in`;
  the robot moved right instead of left and turned slightly right. No abnormal sound, vibration, or
  wheel slip was reported. Only Driver Station STOP is confirmed; automatic distance stop, bumper
  release, and B were not accepted as verified stop evidence.
- The direct tuning composition therefore uses the opposite strafe sign from the earlier diagnostic
  route. The tuner command is changed from `-1` to `+1` and the OpMode is disabled again. The single
  next hypothesis is that positive direct Pedro strafe moves this robot left. That hypothesis must
  pass a short, lower-power direction/stop check before lateral velocity testing resumes.
- The director confirmed the follow-up direction-check gate with clearance on both sides, adult
  supervision, secure hardware, charged battery, and a designated STOP operator. Only a 6-inch
  positive-strafe check is exposed. The initial `0.10` attempt did not move the robot, so the
  director authorized raising the follower ceiling to `0.20`. It reports PASS only when Y increases
  and does not display or save a lateral-velocity candidate.
- The 6-inch positive-strafe direction check passed at the `0.20` ceiling; the director stated that
  no additional data from that direction-only check is required. The selector now advances to one
  24-inch robot-left lateral-velocity shakedown at the same `0.20` ceiling. Its velocity display is
  raw evidence only and does not save a Y-velocity value.
- The first corrected-direction lateral-velocity shakedown reported `7.394 in/s`. Manual travel was
  `24.5 in` against the 24-inch target, and the robot moved approximately `0.5 in` forward during
  the strafe. No turning, slipping, or other abnormal behavior was reported. This is retained as raw
  trial-one evidence pending one same-configuration repeatability run; no lateral-velocity value is
  accepted yet.
- The second corrected-direction lateral shakedown reported `7.324 in/s`, `24.25 in` of manual
  travel, and approximately `0.5 in` of forward drift. The two corrected-direction velocities differ
  by `0.070 in/s`, about 0.95% of their `7.359 in/s` average. Their repeatability supports
  `7.359 in/s` as the lateral-velocity candidate.
- The director accepted `7.359 in/s` as Team A's lateral velocity at the restricted `0.20` power
  ceiling. Pedro 2.1.2 stores it in `MecanumConstants`, so the recorded Team A configuration now
  applies `.yVelocity(7.359)`. The tuning selector is disabled before heading tuning; no later
  category is exposed and path following remains locked.
- The director instructed LP-10 to proceed to heading tuning. The stock Quickstart `HeadingTuner`
  is not exposed unchanged because it continuously alternates along 40-inch paths and instructs an
  operator to push the powered robot. The reviewed increment instead exposes only a one-way
  24-inch heading-hold baseline at the existing `0.20` ceiling. It requires right bumper plus A and
  continued bumper hold, forbids human contact during motion, and stops on path completion,
  24-inch X travel, bumper release, B, or Driver Station STOP. It reports start/end pose, heading
  delta, and maximum heading error without saving a PIDF value. This narrow testing-only path does
  not open the application path-following gate.
- The director confirmed the heading-baseline safety gate: the robot and wiring are secure, the
  battery is charged, at least three feet forward is clear, adult supervision and a designated
  Driver Station STOP operator are present, and nobody will touch or push the robot during powered
  motion. The authorized limits remain one 24-inch forward pass at the `0.20` ceiling.
- The director then superseded the per-tuner unlocking approach and requested the complete Pedro
  Quickstart tuning workflow. `TeamAPedroTuning` now exposes every applicable menu from pinned
  Quickstart commit `d3aea9ca3c5b4c09eded8580229b86996480ee89`: Localization, Automatic,
  Manual, and Tests. The Swerve folder is intentionally omitted because Team A is mecanum and the
  swerve-only tuners require hardware that this robot does not have. Official 48-inch/full-command
  velocity behavior, the continuous 40-inch manual tuners, and the full predictive-braking power
  sweep are restored. The testing OpMode overrides its follower to `1.0` maximum power without
  changing the recorded robot configuration's normal `0.20` limit.
- The earlier `10.052 in/s` forward and `7.359 in/s` lateral values were measured at a restricted
  `0.20` power ceiling, so they are retained only as low-power observations and removed from
  `MecanumConstants`; Pedro's intended maximum-velocity constants require the restored full-power
  tuners. Inner-tuner STOP requests are routed through the registered outer selector to preserve
  Pedro behavior without the previously observed null `OpModeServices` exception. The application
  path-following gate remains closed while LP-10 tuning continues.
- The first restored full-power `Forward Velocity Tuner` run completed automatically and reported
  `52.741 in/s`. It is retained as trial-one evidence pending a same-program repeat before the
  maximum forward velocity is written to `MecanumConstants`.
- The second restored full-power forward run completed automatically and reported `52.008 in/s`.
  The two results differ by `0.733 in/s`, about 1.4% of their `52.3745 in/s` average. The recorded
  Team A configuration now applies the rounded `.xVelocity(52.375)`, and the workflow advances to
  the official full-power lateral velocity tuner.
- The first restored full-power `Lateral Velocity Tuner` run completed automatically and reported
  `41.477 in/s`. It is retained as lateral trial-one evidence pending one same-program repeat before
  writing `yVelocity`.
- The second restored full-power lateral run completed automatically and reported `42.560 in/s`.
  The two results differ by `1.083 in/s`, about 2.6% of their `42.0185 in/s` average. The recorded
  Team A configuration now applies the rounded `.yVelocity(42.019)`, and the official workflow
  advances to heading tuning.
- Panels is already installed and integrated for the upcoming PID stages through FullPanels
  `1.0.12`, Pedro telemetry `1.0.0`, `@Configurable`, `PanelsConfigurables.refreshClass(...)`, and
  the Quickstart field/telemetry support. Live PID edits will be made under
  `Tuning -> Follower -> Constants`; the operator must press Enter to apply each edit, report the
  final values, and then those values must be copied into `TeamAPedroConfiguration` because Panels
  edits do not persist in source code.
- Heading PIDF tuning in Panels finished with `P=2.2`, `I=0.2`, `D=0.189`, and `F=0.02`; the
  director reported that the robot returned to its original heading. The recorded Team A follower
  configuration now applies
  `.headingPIDFCoefficients(new PIDFCoefficients(2.2, 0.2, 0.189, 0.02))`.
- The director selected Pedro's Predictive Braking drive algorithm instead of the longer manual
  translational/drive PIDF route. The next program is the restored automatic
  `Predictive Braking Tuner`, which performs 12 alternating one-second drive/brake samples from
  full power down to `0.20` and reports `kQuadraticFriction` and `kLinearBraking`.
- All 12 Predictive Braking samples completed. The tuner reported
  `kQuadratic=0.002367856854157051` and `kLinear=0.051792761842529726`. Team A's follower now uses
  `.predictiveBrakingCoefficients(new PredictiveBrakingCoefficients(0.1, kLinear, kQuadratic))`
  with Pedro's recommended initial `kP=0.1`, plus `.centripetalScaling(0.0)`. The next step is the
  continuous Line test to adjust only Predictive Braking `kP`.
- The Line test selected Predictive Braking `P=0.4`. The robot showed slight sliding overshoot,
  corrected it, and held without jitter. The configured Predictive Braking coefficients now use
  `P=0.4`. Team A configuration also gains the narrow `PathConstraints` value required by the
  follower factory and starts Predictive Braking validation at Pedro's conservative recommended
  parametric-end constraint `0.97`, preserving the default `100 ms` timeout and other defaults.
- The rebuilt Line validation with `P=0.4` and parametric-end `0.97` settled and reversed correctly
  at both endpoints with no overshoot, undershoot, lateral drift, or heading drift, but it produced
  heavy holding jitter. That combination is rejected. The next single-variable trial lowers only
  Predictive Braking `P` to `0.30` through Panels; the measured braking coefficients and `0.97`
  constraint remain unchanged.
- Panels did not apply the attempted live Predictive Braking P change. The code is therefore updated
  directly from `P=0.4` to `P=0.3` for a rebuild and re-upload. No other Predictive Braking value or
  path constraint changed.
- The director requested the next code-based Predictive Braking trial at `P=0.2`. Only P changed
  from `0.3` to `0.2`; the measured linear/quadratic braking coefficients and parametric-end `0.97`
  remain unchanged.
- The next director-requested code trial places Predictive Braking P midway between `0.10` and
  `0.20`, at `P=0.15`. The measured braking coefficients and parametric-end `0.97` remain unchanged.
- The rebuilt Line validation at Predictive Braking `P=0.15` had only very slight, nearly absent
  jitter. It had no overshoot or undershoot, settled and reversed correctly at both endpoints, and
  showed no lateral or heading drift. This accepts `P=0.15` and the `0.97` parametric-end constraint
  as the recorded Team A Predictive Braking values. In accordance with Pedro's Predictive Braking
  guidance, centripetal scaling remains disabled at `0.0`.
- The director accepted the complete LP-10 tuning evidence and authorized path following at the
  existing `0.20` application power limit. `recordedTeamAConfiguration()` now opens the path gate;
  the LP-09-only diagnostic configuration deliberately keeps that gate closed. No pilot path is
  implemented or started by this permission change.
- The LP-10 tuning-support increment passes `TeamCode:assembleDebug` with the version-matched
  telemetry dependencies.
- LP-09 added `TeamAPedroDiagnostic`, a narrow testing OpMode that initializes
  `TeamAPedroRobot`, starts disabled, uses `InputManager`, calls `robot.update()` once per loop,
  reports the library-neutral pose and readiness gates, and calls `robot.stop()` from FTC stop. It
  never accesses the follower, motors, Pinpoint, FSM, or `HardwareMap.get(...)` directly.
- The LP-09 director supervised with a stable inspected robot, secure wiring, clear test area,
  approved raised-wheel support, charged battery, and Driver Station STOP responsibility. No path
  was requested or enabled.
- LP-09 localization observations with the drivetrain disabled:

  | Check | Expected | Observed | Result |
  | --- | --- | --- | --- |
  | Initial connection | Available pose near `(0, 0, 0)` | `(-0.0, -0.0019, 0.0002°)` | Passed |
  | First 24 in forward | X increases | `(-23.8, 0.1891, -0.2°)` | X sign failed; changed only forward encoder to `REVERSED` |
  | Forward retest | X near `+24` | `(23.9, 0.1, 0.5°)` | Passed |
  | First 24 in left | Y increases | Delta `(0.3, -24.2, -1.5°)` | Y sign failed; changed only strafe encoder to `REVERSED` |
  | Left retest | Y near `+24` | `(0.6906, 23.9803, 0.5484°)` | Passed |
  | 90° counterclockwise rotation | Heading near `+90°` | `(0.1209, 0.2142, 91.38°)` | Passed; angle measured with protractor |
  | 24 in forward and return | Return near zero | Forward `(23.9, 0.6, -0.2°)`; return `(-0.3, 0.1, -0.5°)` | Passed; about `0.32 in` final position error |
- The first restricted forward request stopped with a Pedro 2.1.2 null-pose exception before motor
  output. Version-matched bytecode showed that field-oriented `setTeleOpDrive(...)` reads Pedro's
  internal pose, which is initialized by `startTeleOpDrive()`. `TeamAPedroDriveController` now
  starts Pedro at zero output before storing the first requested command, adding a safe one-loop
  delay; the rebuilt retest had no exception.
- Raised-wheel tests at the director-approved `0.20` ceiling verified forward, left-strafe, and
  counterclockwise wheel patterns. Releasing either deadman control, holding X, Driver Station STOP,
  and the OpMode `robot.stop()` path all removed output. The director reported no unexpected sound,
  vibration, or movement. Diagnostic strafe and rotation signs were corrected one at a time from
  observed wheel patterns.
- Safe initialization, restricted manual driving, and path following are open for the recorded
  Team A configuration after the director accepted LP-10 tuning evidence and the `0.20` limit.
- Added `LOCALIZATION_PATHING_TEACHER_VERIFICATION_PROMPT.md`, a reusable read-only teacher review
  prompt that generates a checklist matched to the branch's recorded LP status. It audits
  repository evidence, staged readiness gates, physical facts, software boundaries, build results,
  and student understanding without advancing prompts, changing progress, authorizing motion, or
  modifying the branch.
- LP-08 staged Team A Pedro readiness: the default `TeamAPedroRobot` now uses configuration facts
  inspected or measured on the real robot and may initialize its follower/localizer in the disabled
  drive state. At the LP-08 checkpoint, restricted manual drive and path following were separate
  closed permissions.
- Recorded LP-08 configuration evidence:

  | Value | Recorded source | Unit | Status after LP-08 |
  | --- | --- | --- | --- |
  | Motors `frontLeft`, `rearLeft`, `frontRight`, `rearRight` | Director checked Driver Station configuration | names | Physically verified |
  | Control Hub motor ports 0 FL, 1 FR, 2 RL, 3 RR | Director checked arrangement | ports | Physically verified |
  | Pinpoint name `pinpoint`, Control Hub I2C port 3 | Director checked configuration and wiring | name/port | Physically verified |
  | Forward pod on Pinpoint X; strafe pod on Pinpoint Y | Director traced connections | connections | Physically verified |
  | goBILDA 4-Bar 32 mm pods | Director inspected pod model | model | Physically verified |
  | `forwardPodY = -6.25`, `strafePodX = -10.0` | Director manually measured from center of rotation | inches | Accepted through LP-09 localization checks |
  | Robot mass `4.85` | Director measured | kilograms | Recorded physical measurement |
  | Starting pose `(0, 0, 0)` | Director approved | inches/radians | Used and verified in LP-09 diagnostics |
  | Left motors reverse; right motors forward | Existing `DriveHardware` setting, recorded by director | directions | Physically verified in LP-09 raised-wheel checks |
  | Forward and strafe encoders reversed | LP-09 unpowered 24 in direction tests | directions | Physically verified |
  | Restricted manual maximum power `0.20` | Director-approved safety ceiling | fraction | Raised-wheel checks passed; manual-drive gate open |
  | Pedro gains, velocities, constraints, braking, and path-end values | LP-10 powered tuning | varies | Recorded and Line-validated; path-following gate open at `0.20` application power |
- `TeamAPedroFollowerFactory` applies the approved `(0, 0, 0)` starting pose and cancels following
  before the drive subsystem enters its continuously disabled state. No path or OpMode was added,
  and no deployment or powered robot test occurred.
- Pedro `com.pedropathing:ftc:2.1.2` resolved in Gradle dependency insight and the existing Pedro
  imports compiled successfully. Red imports remaining in Android Studio indicate Sync/index state,
  not a missing command-line dependency.
- LP-06 Team A pilot: added `TeamAPedroRobot`, `TeamAPedroDriveController`, `TeamAPedroFollowerFactory`, and `TeamAPedroConfiguration` under `robots.teamA`. `TeamARobot` remains unchanged as the simple option.
- The Pedro robot owns only its follower-backed DriveSubsystem and does not create `RobotHardware` or `DriveHardware`, preventing duplicate pilot-motor ownership. The follower factory uses Pedro 2.1.2 `FollowerBuilder` with Pinpoint and mecanum builders.
- The earlier all-or-nothing unconfigured default was replaced in LP-08. The default now holds
  inspected initialization facts, while manual-drive and path-following permissions remain closed
  until their separate physical evidence exists.
- No Team A path or pathing OpMode exists. LP-10 opened the path-readiness gate, but no path can run
  until a later approved prompt supplies one. LP-09 verified only localization
  and restricted raised-wheel manual behavior.
- `TeamCode:assembleDebug` succeeded with Microsoft OpenJDK 17.0.20; no supported local unit-test source set exists.
- LP-07 Session 2 validation: static inspection confirms Pedro imports stay inside Team A, `TeamAPedroRobot` has no direct hardware lookup or `RobotHardware`/`DriveHardware` ownership, and the simple Team A/B/C robots retain their default mecanum controller.
- Corrected one Session 2 lifecycle defect: on the first manual loop, `TeamAPedroDriveController` lets Pedro's `startTeleOpDrive()` perform its built-in update and returns. Later manual loops call `follower.update()` once. This avoids a double follower update on startup.
- Software evidence shows `breakFollowing()` is the configured cancellation path, but physical motor stop behavior, Pinpoint pose accuracy, tuning, and safe path execution still require real-hardware bring-up after actual configuration facts are supplied.

- LP-05 localization/pathing seams: added the vendor-neutral `PoseEstimate` value and `DriveController` interface. `MecanumDriveController` preserves the simple drive behavior and reports unavailable pose/path-following capability.
- `DriveSubsystem` now owns mutually exclusive disabled, manual, heading-hold, and path-following FSM requests. Its existing `DriveHardware` constructor remains the default simple mecanum composition; a controller constructor supports a future team-specific implementation.
- `PathFollowingDriveState` updates a controller once per FTC loop. The baseline mecanum controller safely stops when path following is requested, so Teams A, B, and C retain their existing simple-drive behavior.
- No supported TeamCode unit-test source set exists, so the new seams were compile-checked with `TeamCode:assembleDebug`. Common source imports contain no Pedro classes.

- LP-04 localization/pathing setup: added the `https://mymaven.bylazar.com/releases` repository and pinned `com.pedropathing:ftc:2.1.2` in `build.dependencies.gradle`.
- Raised the shared TeamCode and FtcRobotController compile SDK settings from 30 to 34. FTC SDK 11.2.1, Gradle 9.1.0, Android Gradle Plugin 8.13.2, and Java 8 source/target compatibility remain unchanged.
- Verified `TeamCode:assembleDebug` with Microsoft OpenJDK 17.0.20; build succeeded. Android SDK Platform 34 installed in the existing per-user SDK location after its license was accepted.
- No Pedro drivetrain, Pinpoint localization, dashboard, path, or hardware configuration code exists yet.

- Added repository instructions and architecture documentation.
- Added sequential student workflow documentation.
- Added AprilTag vision-pilot planning artifacts: a student guide, teacher guide, branch-local
  progress record, and separate architecture decision record. These documents define an
  observation-only, Logitech-first and Limelight-ready learning path; no AprilTag, VisionPortal,
  Limelight, localization-fusion, or drivetrain-control implementation has been added.
- Completed Prompt 1: confirmed the TeamCode Java source root is
  `TeamCode/src/main/java` and its root package is `org.firstinspires.ftc.teamcode`.
- Added documented package-level structure under that root: `core.fsm`, `core.robot`,
  `core.input`, `common.hardware`, `common.subsystems.drive`,
  `common.subsystems.intake`, `common.subsystems.vision`, `common.autonomous`,
  `robots.teamA`, `robots.teamB`, `robots.teamC`, `opmodes.teleop`,
  `opmodes.autonomous`, and `opmodes.testing`.
- Confirmed `TeamCode/project-docs` exists as the TeamCode documentation directory.
- Completed Prompt 2: added the `Subsystem` lifecycle contract and the `Robot` base class in
  `core.robot`.
- `Robot` owns registered subsystems in deterministic registration order. It initializes each
  subsystem once, updates each subsystem once per FTC loop, and stops each subsystem before
  calling its protected `onStop()` safety hook.
- Duplicate subsystem instances and registration after initialization are rejected with clear
  exceptions.
- Completed Prompt 3: added the reusable `State`, `Transition`, and `FSM` types in `core.fsm`.
- `FSM` is explicitly activated by `initialize()`, which enters its configured initial state once.
  Each `update()` evaluates transitions from the current state in registration order and permits
  only the first satisfied transition to fire. A firing transition exits the old state, enters the
  target state, then updates the target state in that same cycle.
- `Transition` compares source states by instance identity and uses `BooleanSupplier` for its
  condition. The FSM reports update-before-initialization with a clear exception.
- Completed Prompt 4: added `DriveHardware`, `IntakeHardware`, `VisionHardware`, and
  `RobotHardware` in `common.hardware`.
- `DriveHardware` requires the configured `frontLeft`, `frontRight`, `rearLeft`, and `rearRight`
  `DcMotorEx` motors. It configures left motors reverse, right motors forward, BRAKE zero-power
  behavior, and `RUN_WITHOUT_ENCODER` mode without resetting encoders. It clamps requested motor
  powers and retains their last commanded values for telemetry.
- `IntakeHardware` treats the configured `intake` `DcMotorEx` as optional. A missing intake leaves
  it unavailable and makes intake commands safe no-ops, without preventing drivetrain startup.
- `VisionHardware` provides only the safe optional lifecycle. Camera, AprilTag, OpenCV, and
  VisionPortal setup remain deferred, so it currently reports unavailable.
- `RobotHardware` owns all three wrappers, initializes them in drivetrain, intake, vision order,
  and provides `stopAll()`.
- Required versus optional policy: drive motors are required and fail clearly when absent; intake
  and vision are optional during early testing and must not disable the drivetrain.
- Completed Prompt 5: added `DriveSubsystem` and its `DisabledDriveState`,
  `ManualDriveState`, and `HeadingHoldState` in `common.subsystems.drive`.
- `DriveSubsystem` owns the drive FSM. Its existing `DriveHardware` constructor uses the default
  simple controller, while its optional controller constructor supports future team-specific pathing.
  It stores requested forward, strafe, and rotate values; its public requests select disabled,
  manual, heading-hold, or path-following mode without exposing FSM state manipulation.
- `DriveSubsystem` owns drive-mode selection. Its default `MecanumDriveController` applies the standard
  four-wheel equations, normalizes power when needed, and sends the results through `DriveHardware`.
- Disabled drive continuously stops the motors. Heading hold is an explicit safe manual-drive
  fallback with no IMU target or correction; IMU heading correction remains deferred.
- Drive requests made before initialization are stored safely. The FSM initializes in disabled
  mode, and transitions are registered in deterministic order.
- Completed Prompt 6: added `TeamARobot` in `robots.teamA` as the Team A composition root.
- `TeamARobot` owns a `RobotHardware` composition and one registered `DriveSubsystem`. Its
  `initialize(HardwareMap)` method initializes hardware first and then invokes the inherited
  subsystem lifecycle once, preventing duplicate initialization on repeated calls.
- OpModes use Team A's public drive request methods rather than accessing mechanisms directly.
  Telemetry can read the drivetrain state, requested inputs, and last commanded motor powers
  without access to the drive FSM.
- During stop, the inherited Robot lifecycle stops the registered subsystem before Team A's
  `onStop()` calls `RobotHardware.stopAll()` once for robot-wide hardware safety.
- Completed Prompt 7: added the robot-agnostic `InputManager` in `core.input` for one FTC
  `Gamepad`.
- Construct `InputManager` with a non-null gamepad and call `update()` exactly once at the start
  of every TeleOp loop before reading its queries. The first update records initial button state,
  so already-held buttons produce no just-pressed or just-released event.
- The manager exposes held, just-pressed, and just-released queries for A, B, X, Y, and both
  bumpers, plus all requested stick and trigger values. It has no robot, hardware, subsystem,
  FSM, telemetry, or autonomous dependency.
- Completed Prompt 8: added the iterative `TeamATeleOp` FTC entry point in `opmodes.teleop`.
- `TeamATeleOp` initializes Team A through `TeamARobot.initialize(hardwareMap)`, initializes the
  gamepad edge snapshot in `start()`, and updates input once per loop before mapping to the robot
  public API. It inverts left-stick Y for forward, uses left-stick X for strafe, and right-stick X
  for rotation.
- A Y just-press requests the current heading-hold placeholder; an X just-press returns to manual
  drive. The OpMode publishes drive state, requests, and all four commanded wheel powers through
  TeamARobot's read-only diagnostics, then calls `robot.stop()` during FTC stop.
- Completed Prompt 9: added the shared `IntakeSubsystem` and `IdleIntakeState`, `IntakingState`,
  `HoldingState`, and `EjectingState` in `common.subsystems.intake`.
- `IntakeSubsystem` owns the reusable FSM and accepts only public mode requests: `startIntake()`,
  `stopIntake()`, `hold()`, and `eject()`. It exposes current state and availability for
  telemetry, but does not expose concrete state selection.
- Idle and holding stop the intake output. Intaking and ejecting use the named configurable
  `INTAKE_POWER` and `EJECT_POWER` constants. Holding deliberately uses zero power until a future
  mechanism prompt defines a physical low-power holding requirement.
- The optional intake remains safe when unavailable: all state output calls reach IntakeHardware's
  safe no-op behavior and do not affect drivetrain operation.
- Completed Prompt 10: Team A now composes and registers one `IntakeSubsystem` after its
  `DriveSubsystem`, using `RobotHardware`'s optional `IntakeHardware` wrapper.
- `TeamARobot` exposes the narrow intake requests `startIntake()`, `stopIntake()`,
  `holdIntake()`, and `ejectIntake()`, plus read-only intake state and availability diagnostics.
  It does not expose intake FSM or state instances.
- `TeamATeleOp` owns a second InputManager for `gamepad2`: A starts intake, B stops it, X ejects,
  and Y requests holding. Drive controls remain on gamepad1 without changes. Intake state and
  availability are included in telemetry.
- Because `IntakeHardware` is optional, its unavailable state initializes safely and leaves Team A
  drive behavior unaffected.
- Completed Prompt 11: added the shared `VisionSubsystem` and its `VisionDisabledState`,
  `SearchingState`, `TargetAcquiredState`, `TrackingState`, and `LostTargetState` in
  `common.subsystems.vision`.
- `VisionSubsystem` owns the reusable FSM and exposes `enableVision()`, `disableVision()`, and
  `reportTargetDetected(boolean)` without exposing states. It reports current state and hardware
  availability for telemetry.
- This is a lifecycle-only skeleton: it creates no camera or processor and never invents target
  data. When unavailable, enable requests remain safely disabled and reported availability is
  false. A future processor supplies observations through `reportTargetDetected`.
- Policy: TargetAcquired lasts one update cycle before Tracking; LostTarget lasts one update cycle
  before Searching resumes. Searching, TargetAcquired, Tracking, and LostTarget update the
  VisionHardware lifecycle while active.
- Completed Prompt 12: Team A now composes and registers one `VisionSubsystem` after its drive
  and intake subsystems, using `RobotHardware`'s optional `VisionHardware` wrapper.
- `TeamARobot` exposes narrow `enableVision()` and `disableVision()` requests plus read-only
  vision state and availability diagnostics. It does not expose vision FSM or state instances.
- `TeamATeleOp` maps gamepad1 right-bumper press to enable vision and left-bumper press to disable
  it. No TeleOp target reports are generated. Vision state and availability are now telemetry
  items; drive and intake controls remain unchanged.
- Missing vision hardware remains safe and does not prevent Team A drive or intake initialization.
- AprilTag vision prompt AV-04 added the library-neutral immutable `AprilTagObservation` model
  and the internal `AprilTagVisionSource` seam. Observations document tag ID, timestamp,
  robot-relative right/forward/up position, pitch/roll/yaw, range/bearing/elevation,
  reference-frame name, and quality status without exposing FTC or Limelight types.
- `VisionHardware` now updates one internal source once per active vision lifecycle update and
  returns an immutable observation snapshot. Its default source remains unavailable and empty, so
  existing simple robots retain safe no-camera behavior. `VisionSubsystem` remains the sole FSM
  owner and derives detection from that snapshot only after its active states update hardware.
  No camera, VisionPortal, Limelight, calibration, mount value, field pose, localization, Pedro,
  drivetrain, or autonomous behavior was added.
- AprilTag vision prompt AV-05 added the separate `TeamAAprilTagVisionRobot` and the narrow
  `TeamAAprilTagVisionTestOpMode`. Only this robot composes the Logitech/UVC
  `VisionPortalAprilTagSource`, using the student-selected configured name
  `logitechVisionWebcam`. Missing or unavailable camera hardware is caught as an unavailable
  result. The source reports IDs only; metric robot-relative pose remains unavailable until the
  later calibration and mount evidence gate.
- AprilTag vision prompt AV-06 corrected the pilot lifecycle so entering the disabled FSM state
  clears stale observations without closing the initialized optional camera. Active vision states
  remain the only states that update the source, and `robot.stop()` still closes VisionPortal and
  clears observations. This allows a later OpMode `start()` enable request to succeed.
- AprilTag vision prompt AV-07 recorded the supervised stationary-test equipment and safety facts
  in `APRILTAG_VISION_PHYSICAL_TEST_EVIDENCE.md`. The Logitech C920, configured name, official
  DECODE tag 22, physical mount, robot frame, measurement setup, supervision, and STOP operator
  open the later ID-only stationary gate. Metric robot-relative validation remains closed because
  the actual VisionPortal resolution and matching calibration are unknown.
- AprilTag vision prompt AV-08 added processing timestamp/age telemetry to the narrow diagnostic
  and completed supervised ID-only stationary validation with drive motors disconnected. The
  Logitech C920 reported a 640-by-480 stream, detected ID 22, cleared observations on loss,
  reacquired the tag, remained stable, closed its preview on stop, and reopened without a
  camera-in-use error. The one-loop `LostTarget` state was not visible at Driver Station refresh
  speed. A later architecture audit found that this timestamp/age telemetry does not establish
  camera-frame freshness because the source timestamps each copied detection with `System.nanoTime()`
  instead of preserving the FTC SDK acquisition timestamp. Metric validation remains closed until
  matching calibration and observation freshness are verified.
- Calibration follow-up evidence on 2026-08-20: the Logitech C920 continued detecting tag IDs with
  a reported 640-by-480 VisionPortal stream, and no camera-calibration warning was observed in the
  Driver Station preview or filtered Android Studio Logcat. Windows reported hardware identity
  `USB\VID_046D&PID_082D&MI_00`; VID `046D` and PID `082D` match the Logitech C920 identity used by
  the SDK's published built-in 640-by-480 calibration. The calibration identity/resolution check
  is therefore supported by recorded evidence. Metric observations remain closed until the
  existing frame-freshness prerequisite is corrected and metric output is separately validated.
- Added the Stage 4 planning artifacts `APRILTAG_METRIC_OBSERVATION_STUDENT_GUIDE.md` and
  `APRILTAG_METRIC_OBSERVATION_PROMPT_PROGRESS.md`. The sequential MV-01 through MV-06 workflow
  covers SDK discovery, freshness semantics, explicit 640-by-480 configuration, tag 22 metadata,
  experimental neutral metrics, software audit, and supervised stationary range/bearing checks.
  This is planning only; no metric, localization, drivetrain, autonomous, or Limelight behavior
  was added.
- AprilTag metric-observation prompt MV-03 replaced loop-time detection timestamps with the FTC
  SDK's `AprilTagDetection.frameAcquisitionNanoTime` and uses `getFreshDetections()`. Neutral
  immutable snapshots distinguish `FRESH`, `RETAINED`, and `UNAVAILABLE`; retained observations
  keep their original acquisition timestamps, while a fresh empty frame clears observations.
  The Logitech VisionPortal now explicitly requests the recorded calibrated 640-by-480 stream.
  The stationary diagnostic reports neutral fresh/retained/unavailable frame status, acquisition
  timestamp, increasing observation age, and Robot-loop retained-timestamp check/failure counts.
  Observations remain ID-only; no metric pose, localization, movement, camera-control tuning, or
  Limelight behavior was added.
- MV-03 supervised Control Hub validation confirmed that the updated diagnostic deployed and the
  preview streamed normally with no camera-calibration warning. With tag 22 visible, retained
  timestamp checks ran, displayed `PASS`, and reported zero failures. After removing the tag,
  frame status continued changing between fresh and retained empty results, detection count stayed
  zero, and the vision FSM stayed in `Searching`.
- AprilTag metric-observation prompt MV-04 explicitly selects the SDK 11.2.1 DECODE 36h11 library,
  whose tag 22 entry is `Obelisk_PGP` with a 6.5-inch black-square size, and explicitly requests
  inch/degree output. Fresh tag-22 detections must pass timestamp, metadata, C920 640-by-480
  calibration, FTC pose, finite-value, and verified zero-rotation mount gates before metric pose is
  available. One neutral observation can contain both the FTC camera-relative pose and an
  experimental Team A robot-relative pose; retained or failed-gate observations remain ID-only.
  The aligned camera-to-robot transform adds the measured +6.875-inch right, +4.875-inch forward,
  and +19-inch up mount translation, then recomputes robot-frame range, bearing, and elevation.
  No field pose, localization, movement, pathing, camera-control, or Limelight behavior was added.
- MV-04 supervised display-only Control Hub validation confirmed that the code deployed without
  error and fresh tag-22 results briefly reported pose available with visible camera-frame and
  robot-frame names, XYZ, and range/bearing/elevation values that were all finite. No mechanism
  moved. On retained results, tag 22 remained identified while pose availability was false and
  both camera-relative and robot-relative pose displays were unavailable, as required. This was a
  smoke test only; it did not establish physical range or bearing accuracy.
- MV-05 software-only audit confirmed that the selected source is updated once per active vision
  FSM loop, preserves acquisition timestamps across retained snapshots, clears fresh-empty frames,
  closes and clears on stop, and keeps all FTC types below the neutral public API. No localization,
  movement, autonomous decision, Pedro, Limelight implementation, blocking wait, or direct OpMode
  camera access was found. The physical evidence record now distinguishes its original 2026-08-19
  calibration-unknown entry from the later recorded C920 640-by-480 calibration evidence; MV-06
  must re-confirm all physical prerequisites and validate stationary metric accuracy.
- MV-06 supervised stationary validation on 2026-09-02 recorded three fresh measurements at each
  usable centered and left/right placement. All robot-relative results were finite, repeatable,
  sign-correct, and within the team's preselected range, bearing, and spread tolerances. A centered
  24-inch placement could not keep the entire offset-camera tag in frame and was recorded as a
  visibility limit. At 36, 48, and 60 inches the robot range was consistently about 2.9 to 3.4
  inches low; a direct 55.33-inch lens-plane check showed that the physical reference and
  camera-to-robot translation do not explain the low estimate. Camera-left bearing also showed
  more error than camera-right bearing. Fresh-empty loss cleared stale data, reacquisition used a
  newer timestamp, STOP closed the preview, and a second INIT/STOP cycle reopened and released the
  camera cleanly. The narrow diagnostic now places camera- and robot-frame pose telemetry at the
  top of the Driver Station list. The observations remain experimental; localization and
  alignment remain unauthorized while the range bias and position sensitivity are unresolved or
  unaccepted.
- C920 calibration investigation on 2026-09-18 added a separate
  `Team A Camera Calibration Capture` testing OpMode, `TeamACameraCalibrationRobot`, and
  `CameraFrameCaptureHardware`.
  The capture-only hardware wrapper requests the configured `logitechVisionWebcam` at 640 by 480,
  accepts one raw-frame save request on each gamepad X press only while streaming, and closes the
  VisionPortal on STOP. INIT telemetry refreshes camera state as it opens; telemetry labels
  requests rather than claiming that files were saved.
  The TeamCode Java 17 build passed. No AprilTag processor, camera calibration, mount value,
  localization, alignment, or motor behavior changed. Ten 640-by-480 images were subsequently
  captured and processed with 3DF Zephyr. Its 0.207046-pixel mean square reprojection error was
  internally encouraging, but the exported camera metadata was incomplete and its focal length
  differed materially from the FTC SDK's built-in C920 calibration. The Zephyr calibration was
  therefore not adopted.
- The stationary AprilTag diagnostic now latches the first fresh camera and robot range/bearing
  after each START and displays that one sample until STOP. It is labeled `Latched First Fresh`,
  `display only`, and shows increasing age so it cannot be mistaken for live data. This OpMode-only
  display latch does not change neutral snapshot freshness, restore retained metric pose, correct
  measurements, or feed localization or movement. Each new measurement requires a new OpMode run.
- Corrected stationary validation on 2026-09-22 and 2026-09-23 found that the laser distance meter
  used for the earlier MV-06 measurements reported 36.95 inches over a tape-measured 33.00-inch
  span. The earlier approximately 3-inch low-bias conclusion is retained above as historical MV-06
  evidence but is not a valid camera-calibration conclusion. New camera-centered tests measured
  lens-plane-to-tag-plane distance with a tape measure and recorded three independent latched runs
  at 33, 48, and 66 inches. Mean camera ranges were 33.46, 48.41, and 66.19 inches, for errors of
  +0.46, +0.41, and +0.19 inches. Mean transformed robot ranges were 38.86, 53.84, and 71.57 inches,
  compared with expected values of 38.49, 53.32, and 71.21 inches from the recorded camera mount.
  The results were repeatable and do not justify the Zephyr calibration, a constant range offset,
  or another software correction. Bearing was repeatable but reflected small physical centering
  offsets, so these measurements remain experimental. Localization, automatic alignment, and
  motor behavior remain unauthorized and unimplemented.
- Stage 5 planning on 2026-09-24 added
  `APRILTAG_LOCALIZATION_ALIGNMENT_STUDENT_GUIDE.md` and its branch-local prompt progress record.
  LA-01 through LA-10 separate field-pose candidates from target-relative alignment, require pure
  transform and rejection checks before physical validation, and place distinct STOP gates before
  stationary localization, raised-wheel alignment, and low-speed floor alignment. This is planning
  only; no field-pose, localization, alignment, drivetrain, autonomous, Limelight, or motor behavior
  was added.
- LA-03 verified the checked-out SDK 11.2.1 DECODE library directly: fixed `BlueTarget` tag 20 and
  fixed `RedTarget` tag 24 are 6.5-inch tags with field position and orientation metadata, while
  OBELISK tags 21 through 23 have no field position or orientation. Added neutral immutable
  `FieldPose`, `AprilTagFieldPoseCandidate`, and `AprilTagLocalizationConfiguration` value types.
  The reviewed DECODE configuration allows only tags 20 and 24 in the named official FTC field
  frame and defensively stores its tag IDs with a 250-millisecond maximum candidate age. Constructor
  validation rejects missing names/poses, non-finite pose values, invalid or duplicate IDs, invalid
  timestamps, and non-positive age limits. These types do not yet connect to VisionPortal or create
  live candidates; no localization estimate, fusion, alignment, autonomous, or motor behavior was
  added.
- LA-04 connects the reviewed DECODE configuration to the separate Team A stationary diagnostic.
  The hardware source supplies the verified +6.875-inch right, +4.875-inch forward, +19-inch up
  camera position and the SDK's forward-camera yaw 0, pitch -90, roll 0 orientation through
  `AprilTagProcessor.Builder.setCameraPose(...)`. Fresh tag 20/24 detections can expose one
  immutable neutral field-pose candidate copied from SDK `robotPose` only when the acquisition age
  is at most 250 milliseconds and fixed-tag metadata, C920 640-by-480 calibration, mount, SDK pose,
  and finite-value gates pass. Every failure has an explicit status; retained snapshots strip the
  candidate and report that the frame is retained. The stationary diagnostic displays candidate
  availability, status, field frame, XYZ, pitch/roll/yaw, and age. Existing tag-22 experimental
  camera/robot-relative behavior remains separate and unchanged. No estimator, stored robot pose,
  odometry/Pedro write, fusion, alignment, autonomous, drivetrain, Limelight, or motor behavior was
  added.
- LA-05 completed the software-only field-pose candidate audit without changing production code.
  The checked-out FTC SDK localization sample confirms the current robot axes (+X right, +Y
  forward, +Z up), measured camera-position signs, forward horizontal camera orientation (yaw 0,
  pitch -90, roll 0), and use of SDK `robotPose` as the robot pose relative to the official field
  origin. Focused checks passed for fresh-frame input, original acquisition timestamps, the
  250-millisecond age gate, configured fixed IDs, metadata and finite-value gates, retained-frame
  candidate removal, configuration immutability, and absence of FTC types in neutral localization
  classes. Lifecycle and complete-diff review found no stale candidate acceptance, localization
  provider write, drive/FSM request, autonomous decision, Limelight behavior, blocking wait, or
  motor command. The JDK 17 TeamCode build and `git diff --check` passed.
- LA-06 remains a physical STOP gate. Before it can begin, the team must currently reconfirm the
  reviewed C920 and secure measured mount; configured `logitechVisionWebcam`; explicit 640-by-480
  stream and matching calibration evidence; an official flat 6.5-inch fixed DECODE tag 20 or 24
  with verified field identity/pose; powered Control Hub and Driver Station; deployment access; a
  flat measured test area with marked official field axes; tape measure and angle tool; secure
  stationary robot and tag; good lighting and complete tag visibility; adult supervision; a named
  Driver Station STOP operator; and physically disconnected drive motors or an equivalently
  reviewed movement-prevention method. Expected stationary field poses and acceptance tolerances
  must be selected before results are viewed. This audit does not authorize deployment, physical
  testing, localization-provider writes, alignment, or movement.
- LA-06 preparation added `APRILTAG_LOCALIZATION_FIELD_POSE_TEST_WORKSHEET.md`. Checked-out SDK
  11.2.1 bytecode supplies tag 20 at (-58.3727, -55.6425, 29.5) inches with its official
  orientation. The worksheet translates that entry into a marked-axis setup, the three preselected
  expected robot poses, blank three-reading tables, error/spread calculations, freshness and
  loss/reacquisition checks, and the complete prerequisite and STOP checklists. No physical result
  is recorded: the team does not currently have access to a space that can reproduce the official
  tag pose and field axes. Production code and prompt status remain unchanged.
- Completed Prompt 13: added non-blocking autonomous sequencing in `common.autonomous`:
  `AutoStep`, `AutoSequence`, `WaitStep`, `TimedDriveStep`, and `TimedIntakeStep`.
- `AutoSequence` runs one step at a time. Empty sequences finish immediately; repeated starts do
  nothing; updates before start or after completion do nothing; and stopping stops the active step
  and finishes the sequence.
- Timed steps use FTC `ElapsedTime`, never blocking waits. Drive and intake steps stop their
  requested mechanism on completion or cancellation.
- `AutonomousRobotControl` is the narrow shared API used by timed steps. TeamARobot implements it,
  keeping shared autonomous code independent of Team A while avoiding direct hardware or FSM use.
- Completed Prompt 14: added the iterative `TeamAAutoOpMode` in `opmodes.autonomous`.
- The cautious demonstration sequence drives forward at low power, stops drive through its timed
  step, runs intake, stops intake through its timed step, waits briefly, and finishes safely.
- The OpMode starts the sequence after requesting safe drive and intake modes. Every loop updates
  the sequence and `robot.update()` without blocking; after completion it continues telemetry while
  repeatedly requesting safe drive and intake behavior. Stop cancels the sequence and stops robot
  hardware.
- Completed Prompt 15: added `TeamBRobot` and `TeamCRobot` as minimal team-specific composition
  roots. Each currently reuses the shared required drive, optional intake, and optional vision
  hardware and subsystem policy while leaving team-specific hardware and mechanisms as TODO items.
- Completed Prompt 16: added iterative `TeamBTeleOp` and `TeamCTeleOp` skeletons. Each owns its
  corresponding robot and one driver `InputManager`, initializes through the robot boundary,
  snapshots input in `start()`, applies the cautious common mecanum mapping in `loop()`, calls
  `robot.update()` once per loop, publishes drive/intake/vision state and optional-hardware
  availability, and calls `robot.stop()` during FTC stop.
- Team B and Team C mechanism and mode-selection mappings remain clearly marked TODO items; no
  intake, vision, heading-hold, or team-specific mechanism mapping was invented.
- Prompt 16 architecture audit found no clear local violations requiring code changes. FTC SDK
  modules have no PVI changes in the audited history; hardware access remains in wrappers;
  gamepad access remains in InputManager and TeleOps; FSMs are not exposed by robot APIs or
  manipulated by OpModes; autonomous remains non-blocking and does not use InputManager; mecanum
  calculation and subsystem lifecycle ownership are not duplicated; shared packages contain no
  Team A class dependency; and optional intake/vision failures do not disable required drive.
- Fixed Team A autonomous drivetrain activation: `TimedDriveStep` now requests the robot's
  requested-drive mode before applying its timed drive request. Previously, `TeamAAutoOpMode`
  started by disabling drive and the timed step updated only the requested inputs, so
  `DisabledDriveState` continuously commanded zero motor power.
- `AutonomousRobotControl` now calls the autonomous-neutral `enableRequestedDrive()` API.
  `enableManualDrive()` remains available on Team A's robot and drive subsystem as the
  TeleOp-compatible convenience name.
## Current public APIs
- `org.firstinspires.ftc.teamcode.core.robot.Subsystem`
  - `initialize()`, `update()`, `stop()`, and `getName()`
- `org.firstinspires.ftc.teamcode.core.robot.Robot`
  - `protected final registerSubsystem(Subsystem)`
  - `public final initialize()`, `update()`, and `stop()`
  - `protected onStop()` for robot-level safety work after subsystem shutdown
- `org.firstinspires.ftc.teamcode.core.fsm.State`
  - `enter()`, `update()`, `exit()`, and `getName()`
- `org.firstinspires.ftc.teamcode.core.fsm.Transition`
  - `Transition(State, State, BooleanSupplier)`
  - `appliesTo(State)`, `isConditionSatisfied()`, `getSourceState()`, and `getTargetState()`
- `org.firstinspires.ftc.teamcode.core.fsm.FSM`
  - `FSM()`, `FSM(State)`, `setInitialState(State)`, `addTransition(Transition)`
  - `initialize()`, `update()`, `getCurrentState()`, and `getCurrentStateName()`
- `org.firstinspires.ftc.teamcode.common.hardware.DriveHardware`
  - `initialize(HardwareMap)`, `setMotorPowers(double, double, double, double)`, `stop()`,
    `setBrakeMode()`, `setFloatMode()`, and individual last-commanded-power getters
- `org.firstinspires.ftc.teamcode.common.hardware.IntakeHardware`
  - `initialize(HardwareMap)`, `forward(double)`, `reverse(double)`, `stop()`, and `isAvailable()`
- `org.firstinspires.ftc.teamcode.common.hardware.VisionHardware`
  - `VisionHardware()`, `VisionHardware(String)`,
    `VisionHardware(AprilTagCameraConfiguration)`,
    `VisionHardware(AprilTagCameraConfiguration, AprilTagLocalizationConfiguration)`,
    `initialize()`, `initialize(HardwareMap)`,
    `update()`, `stop()`, `isAvailable()`, and
    `getObservations()`; `getSnapshot()` exposes the neutral frame status with the immutable list
- `org.firstinspires.ftc.teamcode.common.hardware.AprilTagCameraConfiguration`
  - immutable neutral stream, calibration, robot-frame, and measured camera-mount facts; contains
    no FTC camera or processor type
- `org.firstinspires.ftc.teamcode.common.vision.AprilTagObservation`
  - immutable neutral tag ID, timestamp, pose-availability, reference-frame, quality, position,
    orientation, range, bearing, and elevation getters; optional `getCameraRelativePose()` and
    `getRobotRelativePose()` views; optional `getFieldPoseCandidate()`,
    `isFieldPoseCandidateAvailable()`, and explicit `getFieldPoseStatus()`; unavailable legacy
    robot metric values are `NaN`
- `org.firstinspires.ftc.teamcode.common.vision.AprilTagPose`
  - immutable finite position, orientation, range, bearing, and elevation values in one named
    neutral reference frame
- `org.firstinspires.ftc.teamcode.common.localization.FieldPose`
  - immutable finite X/Y/Z and pitch/roll/yaw values in one named field reference frame
- `org.firstinspires.ftc.teamcode.common.localization.AprilTagFieldPoseCandidate`
  - one immutable accepted tag ID, positive acquisition timestamp, and neutral `FieldPose`
- `org.firstinspires.ftc.teamcode.common.localization.AprilTagLocalizationConfiguration`
  - immutable configuration name, defensive fixed-tag ID list, field-frame name, maximum candidate
    age, `allowsTagId(int)`, and the reviewed `decodeGoalTags()` factory
- `org.firstinspires.ftc.teamcode.common.vision.AprilTagFrameStatus` and
  `AprilTagObservationSnapshot`
  - neutral `FRESH`, `RETAINED`, and `UNAVAILABLE` frame status; immutable observation list;
    `getFrameStatus()`, `isFreshFrame()`, `isRetained()`, and `getObservations()`
- `org.firstinspires.ftc.teamcode.common.hardware.RobotHardware`
  - `initialize(HardwareMap)`, hardware-wrapper getters, and `stopAll()`
- `org.firstinspires.ftc.teamcode.common.subsystems.drive.DriveSubsystem`
  - `DriveSubsystem(DriveHardware)` and `DriveSubsystem(DriveController)`
  - `initialize()`, `update()`, `stop()`, and `getName()`
  - `drive(double, double, double)`, `enableRequestedDrive()`, `enableManualDrive()`,
    `disableDrive()`, `enableHeadingHold()`, `enablePathFollowing()`, and `cancelPathFollowing()`
  - request diagnostics plus `getPoseEstimate()` and `isPathFollowingActive()`
- `org.firstinspires.ftc.teamcode.common.subsystems.drive.DriveController` and
  `MecanumDriveController`
  - library-neutral optional pathing/localization seam and the default simple-mecanum implementation
- `org.firstinspires.ftc.teamcode.common.localization.PoseEstimate`
  - immutable available/unavailable pose boundary using inches and radians
- `org.firstinspires.ftc.teamcode.common.subsystems.drive.DisabledDriveState`,
  `ManualDriveState`, `HeadingHoldState`, and `PathFollowingDriveState`
  - public `State` implementations with `DriveSubsystem` constructors
- `org.firstinspires.ftc.teamcode.robots.teamA.TeamARobot`
  - `TeamARobot()`, `TeamARobot(RobotHardware)`, and `initialize(HardwareMap)`
  - `drive(double, double, double)`, `enableRequestedDrive()`, `enableManualDrive()`,
    `disableDrive()`, and
    `enableHeadingHold()`
  - drivetrain state, requested-input, and last-commanded-power telemetry getters
  - `startIntake()`, `stopIntake()`, `holdIntake()`, `ejectIntake()`, `getIntakeStateName()`,
    and `isIntakeAvailable()`
  - `enableVision()`, `disableVision()`, `getVisionStateName()`, and `isVisionAvailable()`
- `org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroConfiguration`
  - `unconfigured()`, `configured(...)`, `recordedTeamAConfiguration()`, and
    `restrictedManualTestConfiguration()`
  - read-only readiness queries for safe initialization, restricted manual drive, and path following
- `org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroRobot`
  - default recorded Team A configuration plus the existing injectable constructor
  - `initialize(HardwareMap)`, drive mode requests, cancellation, pose/state diagnostics, and
    read-only readiness queries; manual drive and path following fail closed while their gates are
    unavailable
- `org.firstinspires.ftc.teamcode.core.input.InputManager`
  - `InputManager(Gamepad)` and `update()` once per TeleOp loop
  - held, just-pressed, and just-released button queries for A, B, X, Y, and both bumpers
  - left/right stick-axis and trigger getters
- `org.firstinspires.ftc.teamcode.opmodes.teleop.TeamATeleOp`
  - iterative TeleOp lifecycle mapping `gamepad1` to TeamARobot's public drivetrain API
- `org.firstinspires.ftc.teamcode.common.subsystems.intake.IntakeSubsystem`
  - `IntakeSubsystem(IntakeHardware)`, lifecycle methods, `startIntake()`, `stopIntake()`,
    `hold()`, `eject()`, `getCurrentStateName()`, and `isAvailable()`
- `org.firstinspires.ftc.teamcode.common.subsystems.intake.IdleIntakeState`, `IntakingState`,
  `HoldingState`, and `EjectingState`
  - public `State` implementations with `IntakeSubsystem` constructors
- `org.firstinspires.ftc.teamcode.common.subsystems.vision.VisionSubsystem`
  - `VisionSubsystem(VisionHardware)`, lifecycle methods, `enableVision()`, `disableVision()`,
    `reportTargetDetected(boolean)`, `getCurrentStateName()`, `isAvailable()`, and
    `getLatestObservations()`; `getLatestSnapshot()` adds neutral frame status
- `org.firstinspires.ftc.teamcode.robots.teamA.TeamAAprilTagVisionRobot`
  - `TeamAAprilTagVisionRobot(String)`, `initialize(HardwareMap)`, public vision enable/disable,
    availability/state diagnostics, `getAprilTagObservations()`, and
    `getAprilTagObservationSnapshot()`
- `org.firstinspires.ftc.teamcode.opmodes.testing.TeamAAprilTagVisionTestOpMode`
  - stationary diagnostic that uses only TeamAAprilTagVisionRobot public APIs
- `org.firstinspires.ftc.teamcode.common.subsystems.vision.VisionDisabledState`,
  `SearchingState`, `TargetAcquiredState`, `TrackingState`, and `LostTargetState`
  - public `State` implementations with `VisionSubsystem` constructors
- `org.firstinspires.ftc.teamcode.common.autonomous.AutoStep`
  - `start()`, `update()`, `isFinished()`, `stop()`, and `getName()`
- `org.firstinspires.ftc.teamcode.common.autonomous.AutoSequence`
  - empty and varargs constructors, `addStep(AutoStep)`, `start()`, `update()`, `stop()`,
    `isFinished()`, and `getCurrentStepName()`
- `org.firstinspires.ftc.teamcode.common.autonomous.WaitStep`, `TimedDriveStep`, and
  `TimedIntakeStep`
  - non-blocking baseline timed steps
- `org.firstinspires.ftc.teamcode.common.autonomous.AutonomousRobotControl`
  - narrow `enableRequestedDrive()`, drive, and intake requests implemented by TeamARobot
- `org.firstinspires.ftc.teamcode.opmodes.autonomous.TeamAAutoOpMode`
  - iterative non-blocking Team A demonstration autonomous sequence
- `org.firstinspires.ftc.teamcode.robots.teamB.TeamBRobot` and
  `org.firstinspires.ftc.teamcode.robots.teamC.TeamCRobot`
  - constructors, `initialize(HardwareMap)`, public drive/intake/vision requests, state and
    availability diagnostics, inherited `update()` and `stop()`
- `org.firstinspires.ftc.teamcode.opmodes.teleop.TeamBTeleOp` and
  `org.firstinspires.ftc.teamcode.opmodes.teleop.TeamCTeleOp`
  - iterative drive-only TeleOp lifecycles using the corresponding robot public API
- `org.firstinspires.ftc.teamcode.opmodes.testing.TeamAPedroDiagnostic`
  - disabled-by-default LP-09 pose telemetry and deadman-controlled raised-wheel requests at the
    configured restricted ceiling
## Build status
- Build runtime observed in the latest verification: Java 24; source/target 8 deprecation warnings are emitted.
- Android Studio version: Quail 1
- FTC SDK version or tag: Release 12.0.0
- Pedro dependencies: `revhub:3.0.2`, `tuning:1.0.1`
- TeamCode build command (Windows): `.\gradlew.bat TeamCode:assembleDebug`
- Latest result: `BUILD SUCCESSFUL`; `git diff --check` passed.
## Known limitations and TODO items
- `pedro/Constants.java` is copied from the local `pedro3` project and `create()` intentionally
  returns `null`. `KimCode/TeleopV1` therefore compiles but cannot drive until the team completes
  constants and constructs the follower.
- Mecanum, Pinpoint, and Foresight procedures are registered. The local reference has not completed
  Foresight tuning; generated values still need review and must be copied into the Pedro 3
  `ForesightConfig`. Pinpoint configuration also remains unfinished in the reference Constants file.
- Earlier LP-10 velocities, heading PIDF, Predictive Braking coefficients, `P=0.15`, and the `0.97`
  parametric-end constraint are historical Pedro 2 evidence. They are not verified Pedro 3 tuning
  results and must not be used to open the current path-following gate.
- No successful physical hardware validation has been recorded for this Pedro 3 integration. The
  build proves compilation and packaging only.
- Older LP-09/LP-10 records describe hardware and tuning checks made with the former Pedro 2
  implementation. They do not establish Pedro 3 localization, tuning, or path readiness.
- Configure branch protection and pull-request review.
- Consider adding compile-only GitHub Actions validation.
- MV-03 software checks verify that `VisionPortalAprilTagSource` preserves
  `AprilTagDetection.frameAcquisitionNanoTime`, uses `getFreshDetections()`, and retains immutable
  observations without assigning a new Robot-loop timestamp. Supervised Control Hub evidence also
  confirms retained timestamp checks pass with zero failures and fresh-empty results clear the
  detection count. Metric observations remain closed until MV-04, and vision observations must not
  drive localization or robot motion.
- MV-04 metric values are experimental software output. Stationary measured range/bearing accuracy
  and camera-to-robot transform behavior remain unverified on hardware until MV-06; these values
  must not drive localization or movement.
- Only the separate Team A diagnostic robot composes the Logitech VisionPortal camera source. The
  default `VisionHardware` used by simple Team A, Team B, and Team C robots remains safely
  unavailable and does not create a camera.
- IMU heading correction remains intentionally deferred. `HeadingHoldState` currently provides a
  safe manual-drive fallback.
- Intake holding power remains zero until a future mechanism prompt defines the physical holding
  requirement.
- Team B and Team C currently assume the shared hardware policy: required `frontLeft`,
  `frontRight`, `rearLeft`, and `rearRight` drive motors, optional `intake`, and unavailable
  lifecycle-only vision. Confirm each team's wiring, motor directions, geometry, and controls
  before field use.
- Team B and Team C currently expose only the common drive mapping in TeleOp. Their mechanism and
  mode-selection mappings remain TODO items.
- Deferred architecture review: no larger or ambiguous Prompt 16 findings require a speculative
  refactor at this time.
- Team 3 Pedro tuning: Forward and lateral maximum velocities (57.666 in/s and 58.178 in/s), motor directions, and Pinpoint odometry geometry are calibrated and verified. Weight-dependent parameters (Forward/Lateral Zero-Power Deceleration, Predictive Braking, and robot mass) are intentionally deferred until physical mechanism assembly is completed.
- Team 3 Autonomous: `Team3AutonomousOpMode` updated to drive 36 inches (3 feet) straight forward (`Team3PedroPathRoute.FORWARD_36`, via `Team3PedroForward36Path`) while deploying the intake servo at 2.0 seconds into the run. A full autonomous suite with multi-waypoint Pedro pathing and AprilTag vision integration will be constructed once physical mechanism assembly is finalized and camera hardware is mounted.
- Team 3 TeleOp & hardware optimizations: Added write-caching to `Team3MechanismHardware` to eliminate redundant Expansion Hub bus traffic for idle motors and servos. Streamlined match-loop telemetry in `Team3TeleOp` from 22 lines down to 6 core fields (moving static hardware health checks to `init()`), eliminating periodic Android GC stop-the-world pauses and Wi-Fi latency. Removed dead `maxPower` overload from `Team3PedroFollowerFactory`.
- Team 3 TeleOp brake: Gamepad 1 Right Bumper (hold) calls `Team3Robot.brake()`, which commands zero power to all drive motors and re-anchors the pose lock to current robot physical coordinates; sticks are ignored while held. Midtake feed moved from Right Bumper to D-pad Up (hold).
- Team 3 Cardinal Heading Snap: In `Team3TeleOp`, mapped X to 0° (forward), B to 180° (backward), D-pad Left to +90° (left), and D-pad Right to -90° (right). Calls `Team3Robot.snapToHeading(double)`, which aligns the virtual anchor and engages assist while leaving D-pad Up (midtake feed) and D-pad Down (idle auto-run) completely intact.
- Team 3 "Pose Lock" 3-DOF Closed-Loop Drive Assist:
  - Created `Team3DriveAssistLevel`: enum defining `OFF` and `FULL` (resists both uncommanded translation and rotation).
  - Created `Team3PoseLock`: pure mathematical controller maintaining a moving virtual anchor (X, Y, Heading) advanced exclusively by driver commanded velocity (`57.666 in/s` fwd, `58.178 in/s` lateral, `360 deg/s` turn). Removed disturbance leashes so external pushes (even dragged 4+ feet or spun 180°) never drag the anchor; the robot always returns to its exact locked origin.
  - Added anti-windup Integral terms (`KI_TRANSLATIONAL = 0.05`, `KI_HEADING = 0.08`) and boosted static friction feedforward (`KS_TRANSLATIONAL = 0.08`, `KS_HEADING = 0.06`) to eliminate steady-state friction stall against carpet, driving error to absolute zero.
  - Increased return authority: `KP_TRANSLATIONAL = 0.14`, `KP_HEADING = 2.2`, `MAX_TRANSLATIONAL_CORRECTION = 0.65`, `MAX_HEADING_CORRECTION = 0.60`.
  - Created `Team3StabilizedDriveController`: implements `DriveController`, reading Pinpoint odometry once per loop, applying `Team3PoseLock` stabilization, and delegating normalized motor power outputs through `MecanumDriveController`.
  - Updated `Team3PinpointHardware`: exposed `getXInches()` and `getYInches()`.
  - Updated `Team3Robot`: composed `DriveSubsystem` with `Team3StabilizedDriveController`, enabled Lynx module bulk caching mode `AUTO`, added `toggleDriveAssist()` and target getters (`getTargetXInches()`, `getTargetYInches()`, `getXInches()`, `getYInches()`). Removed redundant `Team3HeadingHoldSubsystem`.
  - Updated `Team3TeleOp`: starts with assist `OFF` by default; button `Y` toggles `OFF (MANUAL)` and `ON (FULL POSE LOCK)` (locking current pose upon activation). Telemetry reports live Pinpoint coordinates (fusing odometry + IMU), locked origin coordinates, displacement error, and loop time in ms.
  - Build verified: `TeamCode:assembleDebug` passed (`BUILD SUCCESSFUL`, 52 tasks, 0 errors). `git diff --check` passed clean.

## Update instructions
After every completed update, replace or extend the sections above with:
- completed classes and behavior;
- actual package names and public APIs;
- important implementation decisions;
- build command and result;
- known limitations;


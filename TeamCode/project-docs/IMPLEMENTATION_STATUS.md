# PVI-FTC Implementation Status

PVI-FTC | Editable master guide

[Click here to read the ARCHITECTURE guide](../TeamCode/project-docs/ARCHITECTURE.md)

[Click here to read the STUDENT CODING WORKFLOW guide](../TeamCode/project-docs/STUDENT_WORKFLOW.md)

## Repository baseline
- Source repository: PVI-FTC fork of FtcRobotController
- Current sequential prompt: LP-11 four-path Pedro selector ready for deployment review
- Last completed prompt: LP-10 Pedro tuning and path-readiness acceptance
- Last verified commit: d7b9014 (LP-09 reviewed implementation)
## Completed work
- Added `PEDRO_ROBOT_SETUP_REFERENCE.md`, a reusable setup and tuning guide grounded in the current
  Team A code and recorded physical evidence. It inventories the pinned dependencies, hardware and
  Pinpoint facts, exact active constants, completed and uncompleted tuners, Panels workflow,
  localization/direction evidence, future-robot replacement map, and sign-off checklist. Unverified
  physical dimensions are explicitly separated from inconsistent Visualizer-only footprints.
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
  - `initialize()`, `update()`, `stop()`, and `isAvailable()`
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
    `reportTargetDetected(boolean)`, `getCurrentStateName()`, and `isAvailable()`
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
- Approved JDK: Record the team-approved version here. Microsoft OpenJDK Java 17.0.x
- Android Studio version: Quail 1
- FTC SDK version or tag: Release 11.2.1
- TeamCode build command (Windows): `.\gradlew.bat TeamCode:assembleDebug`
## Known limitations and TODO items
- `TeamAPedroTuning` exposes the full Pedro Quickstart menu applicable to Team A's mecanum robot.
  The Swerve-only folder is omitted. The operator must follow the documented Pedro order rather
  than menu order because every applicable option is now visible.
- Full-power velocities, heading PIDF, Predictive Braking coefficients, `P=0.15`, and the `0.97`
  parametric-end constraint are recorded and validated by the Line test. The director accepted the
  evidence and the `0.20` application limit, so path readiness is open. No pilot path exists yet.
- Before LP-09, resync Android Studio if Pedro imports remain red; the command-line build already
  resolves and compiles Pedro 2.1.2.
- LP-09 verified Pinpoint pose signs, approximate distances, return-to-start error, both encoder
  directions, recorded motor directions, restricted raised-wheel movement, cancellation, Driver
  Station STOP, and `robot.stop()`. Ground driving outside a later supervised tuning/test plan has
  not been authorized.
- Path readiness is open after version-matched tuning recorded accepted velocities, heading PIDF,
  Predictive Braking values, constraints, repeatability, and safe stop behavior. LP-11 still must
  implement and cautiously validate the first pilot path.
- Configure branch protection and pull-request review.
- Consider adding compile-only GitHub Actions validation.
- Vision hardware integration is intentionally deferred until a future prompt defines camera and
  processor requirements.
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

## Update instructions
After every completed update, replace or extend the sections above with:
- completed classes and behavior;
- actual package names and public APIs;
- important implementation decisions;
- build command and result;
- known limitations;


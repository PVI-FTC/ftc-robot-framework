# Pedro Pathing Robot Setup Reference

This guide records the Pedro Pathing configuration and tuning evidence for the current Team A
robot, then turns that evidence into a repeatable setup process for a future robot. It is written
for the versions currently pinned in this repository: Pedro RevHub `3.0.2` and Pedro tuning `1.0.1`.

Do not copy Team A's physical or tuned values into another robot without measuring and testing that
robot. Motor directions, pod offsets, encoder directions, mass, velocities, gains, braking values,
and path constraints are robot-specific.

## Sources of truth

Use these sources in this order when this document and the code disagree:

1. The current checked-out code, especially
   [`TeamAPedroConfiguration.java`](../src/main/java/org/firstinspires/ftc/teamcode/robots/teamA/TeamAPedroConfiguration.java).
2. Real measurements and repeatable tests on the robot being configured.
3. The version-matched official Pedro and goBILDA documentation linked at the end of this guide.
4. This reference document and `IMPLEMENTATION_STATUS.md` as historical evidence.

The current follower is created by
[`TeamAPedroFollowerFactory.java`](../src/main/java/org/firstinspires/ftc/teamcode/robots/teamA/TeamAPedroFollowerFactory.java).
It combines Pedro 3 `ForesightConfig`, `MecanumConfig`, and `PinpointConfig`.

## Current Pedro 3 implementation workflow

The active Pedro 3 files are:

- `pedro/Constants.java` and `pedro/Tuning.java`, copied from the local `pedro3` reference.
- `pedro/procedures/`, containing the version-matched AutoTune procedures.
- `robots/teamA/TeamAPedroConfiguration.java`, which adapts the recorded Team A configuration to
  the Pedro 3 follower.
- `robots/teamA/TeamAPedroFollowerFactory.java`, which creates the RevHub Mecanum + Pinpoint +
  Foresight follower.
- `KimCode/TeleopV1.java`, an independent Pedro 3 TeleOp entry point.

Build verification does not require a phone, Control Hub, emulator, or other target device. From
the repository root, run:

```powershell
.\gradlew.bat --no-daemon --console=plain TeamCode:assembleDebug
```

Android Studio can perform the same device-free check through **Build > Make Project**. The green
Run button is different: it installs and launches the APK and therefore requires a target device.

The current `Constants.create()` is intentionally still the reference placeholder and returns
`null`. Complete the Foresight output and follower construction there before attempting to drive
with `KimCode/TeleopV1`.

## Recorded Team A hardware facts

| Category | Current value | Unit or meaning | Replace for a future robot? |
| --- | --- | --- | --- |
| Drivetrain | Four-wheel mecanum | Hardware type | Confirm on the robot |
| Motor names | `frontLeft`, `frontRight`, `rearLeft`, `rearRight` | Driver Station configuration | Match the robot configuration |
| Left motor directions | `REVERSE` | Recorded Team A setting | Recheck with wheels raised |
| Right motor directions | `FORWARD` | Recorded Team A setting | Recheck with wheels raised |
| Pinpoint name | `pinpoint` | Hardware map name | Match the robot configuration |
| Pinpoint pod | goBILDA 4-Bar Odometry Pod | Recorded Team A hardware | Inspect the installed pod |
| Pinpoint offsets | X `-10.0 in`, Y `-6.25 in` | Recorded Team A values | Re-measure after hardware changes |
| Pinpoint encoder directions | Both `REVERSED` | Recorded Team A setting | Recheck with measured motion |
| Distance unit | `INCH` | Pinpoint and path coordinate unit | Keep consistent |

Earlier velocity, braking, and heading results are retained below as Pedro 2/Panels evidence.
They are not accepted Pedro 3 tuning results. Run Pedro 3 AutoTune on the current robot before
using those values for path following.

## Software and build configuration

| Item | Current repository value |
| --- | --- |
| FTC SDK | `12.0.0` |
| Pedro RevHub | `com.pedropathing:revhub:3.0.2` |
| Pedro tuning | `com.pedropathing:tuning:1.0.1` |
| Maven repository added for Pedro | `https://repo.dairy.foundation/releases/` |
| Compile SDK | `34` |
| Minimum SDK | `24` |
| Target SDK | `28` |
| Java | Microsoft OpenJDK 17.0.x |
| Build command | `.\gradlew.bat TeamCode:assembleDebug` |

The dependencies and repository are in `build.dependencies.gradle`. After changing dependencies,
run Android Studio Gradle Sync and the command-line TeamCode build. A successful command-line build
with red editor imports normally means Android Studio still needs to sync or re-index.

## Coordinate and sign convention

Pedro's robot coordinate convention used by this project is:

- Positive X: forward.
- Negative X: backward.
- Positive Y: robot-left.
- Negative Y: robot-right.
- Positive heading: counterclockwise.

The forward pod measures X motion but its mounting location is a Y offset, so Pedro names its
location `forwardPodY`. The strafe pod measures Y motion but its mounting location is an X offset,
so Pedro names its location `strafePodX`.

For Team A:

- `forwardPodY = -6.25` means the forward pod is 6.25 inches to the robot-right of the center of
  rotation.
- `strafePodX = -10.0` means the strafe pod is 10 inches behind the center of rotation.

Always measure from the robot's center of rotation, not from a frame edge, wheel, or Control Hub.

## Physical robot and drivetrain facts

### Verified Team A facts

| Item | Team A value | Evidence |
| --- | --- | --- |
| Drivetrain | Four-wheel mecanum | Physical configuration and raised-wheel tests |
| Front-left motor name | `frontLeft` | Driver Station configuration |
| Front-right motor name | `frontRight` | Driver Station configuration |
| Rear-left motor name | `rearLeft` | Driver Station configuration |
| Rear-right motor name | `rearRight` | Driver Station configuration |
| Control Hub motor ports | `0=frontLeft`, `1=frontRight`, `2=rearLeft`, `3=rearRight` | Director-verified wiring |
| Left motor directions | `REVERSE` | Raised-wheel direction checks |
| Right motor directions | `FORWARD` | Raised-wheel direction checks |
| Robot mass | `4.85 kg` | Current recorded physical measurement |
| Normal path power ceiling | `0.20` | Director-approved application limit |

### Dimensions and visual footprint

The physical chassis width, chassis length, wheelbase, track width, wheel diameter, and gear ratio
are not recorded as verified Team A runtime constants. The Pinpoint localizer used here does not
require wheelbase dimensions in `PinpointConstants`, and empirically measured velocities avoid a
wheel-diameter calculation.

The Visualizer files contain display/collision footprints, not verified on-robot dimensions:

| Visualizer project | Width | Height | Status |
| --- | ---: | ---: | --- |
| LP-11 Pilot | `16 in` | `16 in` | Visualizer-only |
| DECODE Route | `16 in` | `16 in` | Visualizer-only |
| Curving Test | `16 in` | `16 in` | Visualizer-only |
| Illegal Path | `19.5 in` | `15 in` | Visualizer-only |

Because those values disagree, no physical size should be inferred from them. For a future robot,
measure the full bumper-to-bumper length and width and update every Visualizer project consistently.
If another localizer such as drive encoders requires wheelbase dimensions, also measure the distance
between front/rear wheel centers and left/right wheel centers.

The current Pedro 3 AutoTune page does not use the removed Panels drawing workflow. Any visualizer
or robot-footprint values below are historical display evidence, not runtime geometry.

## Pinpoint and odometry configuration

| Item | Team A value | Replace or verify on a future robot |
| --- | --- | --- |
| Hardware-map name | `pinpoint` | Match the Driver Station configuration exactly |
| Hub connection | Control Hub I2C port 3 | Verify the physical cable; avoid the built-in IMU port 0 |
| Mounting orientation | No explicit software override recorded | Verify sticker/ports face up and record the actual mount |
| Forward pod connection | Pinpoint X port | Trace the cable |
| Strafe pod connection | Pinpoint Y port | Trace the cable |
| Pod model | goBILDA 4-Bar Odometry Pod, 32 mm wheel | Select the actual pod or custom resolution |
| Encoder resolution | `goBILDA_4_BAR_POD` | Replace for another pod type |
| Forward pod offset | `forwardPodY(-6.25)` | Measure or run Offsets Tuner |
| Strafe pod offset | `strafePodX(-10.0)` | Measure or run Offsets Tuner |
| Forward encoder direction | `REVERSED` | Verify by pushing forward; X must increase |
| Strafe encoder direction | `REVERSED` | Verify by pushing left; Y must increase |
| Distance unit | `INCH` | Keep units consistent with offsets and paths |
| Yaw scalar | No override | Leave Pinpoint calibration unless testing proves a need |

Team A used manual offset measurements. The Offsets Tuner was not used to produce the accepted
`-6.25` and `-10.0` values. If a future robot uses Offsets Tuner, temporarily set both offsets to
zero only for that test, then store the reported final offsets in source code.

## Historical Pedro 2 runtime constants

The following block documents the former Pedro 2 representation for historical comparison only.
It is not valid Pedro 3 source. The active Pedro 3 representation is in
`TeamAPedroConfiguration.java` and uses `ForesightConfig`, `MecanumConfig`, and `PinpointConfig`.

The active values in `recordedTeamAConfiguration()` are equivalent to:

```java
FollowerConstants follower = new FollowerConstants()
        .mass(4.85)
        .headingPIDFCoefficients(new PIDFCoefficients(2.2, 0.2, 0.189, 0.02))
        .forwardZeroPowerAcceleration(-42.4832745291992)
        .lateralZeroPowerAcceleration(-50.60694780564594)
        .predictiveBrakingCoefficients(new PredictiveBrakingCoefficients(
                0.15, 0.051792761842529726, 0.002367856854157051))
        .centripetalScaling(0.0);

MecanumConstants mecanum = new MecanumConstants()
        .xVelocity(62.61374213751846)
        .yVelocity(49.757958599901585)
        .maxPower(0.20)
        .leftFrontMotorName("frontLeft")
        .leftRearMotorName("rearLeft")
        .rightFrontMotorName("frontRight")
        .rightRearMotorName("rearRight")
        .leftFrontMotorDirection(DcMotorSimple.Direction.REVERSE)
        .leftRearMotorDirection(DcMotorSimple.Direction.REVERSE)
        .rightFrontMotorDirection(DcMotorSimple.Direction.FORWARD)
        .rightRearMotorDirection(DcMotorSimple.Direction.FORWARD);

PinpointConstants pinpoint = new PinpointConstants()
        .forwardPodY(-6.25)
        .strafePodX(-10.0)
        .distanceUnit(DistanceUnit.INCH)
        .hardwareMapName("pinpoint")
        .encoderResolution(GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_4_BAR_POD)
        .forwardEncoderDirection(GoBildaPinpointDriver.EncoderDirection.REVERSED)
        .strafeEncoderDirection(GoBildaPinpointDriver.EncoderDirection.REVERSED);

PathConstraints constraints = new PathConstraints(0.97, 100.0);
Pose startingPose = new Pose(0.0, 0.0, 0.0);
```

Only the selected path may replace the default starting pose at initialization. Route start poses
belong in `TeamAPedroPathRoute`, not in the robot-wide constants.

## Values not explicitly tuned or overridden

The following are not custom Team A runtime values and should not be presented as measured facts:

- Physical chassis length and width.
- Wheelbase and track width.
- Drive wheel diameter and drivetrain gear ratio.
- A Pinpoint yaw scalar.
- A custom odometry encoder resolution.
- Translational PIDF coefficients.
- Drive PIDF coefficients.
- Secondary translational, heading, or drive PIDFs and switch thresholds.
- Global deceleration/braking strength for every route.
- Kalman filter covariance overrides.

The following data describes the former Pedro 2 implementation. It is historical evidence only;
the current Pedro 3 configuration and tuner outputs use different config classes and must be
validated with the pinned Pedro 3 version.

## Completed physical localization and direction tests

These earlier physical observations used `Team A Pedro Diagnostic` with the former Pedro 2
integration, not the path menu. The unpowered portion kept the drive state disabled; the restricted
raised-wheel checks used right bumper
plus exactly one of A (forward), B (left), or Y (counterclockwise); releasing right bumper or
holding X disables drive.

| Test | Final observation | Result or change |
| --- | --- | --- |
| Initial Pinpoint connection | `(-0.0, -0.0019, 0.0002 deg)` | Pose available and near zero |
| First 24-inch forward push | `(-23.8, 0.1891, -0.2 deg)` | Wrong X sign; reverse forward encoder |
| Forward retest | `(23.9, 0.1, 0.5 deg)` | Passed |
| First 24-inch left push | delta `(0.3, -24.2, -1.5 deg)` | Wrong Y sign; reverse strafe encoder |
| Left retest | `(0.6906, 23.9803, 0.5484 deg)` | Passed |
| 90-degree counterclockwise rotation | `(0.1209, 0.2142, 91.38 deg)` | Passed; protractor measurement |
| 24 inches forward and return | forward `(23.9, 0.6, -0.2 deg)`; return `(-0.3, 0.1, -0.5 deg)` | Passed; about 0.32-inch final position error |
| Raised-wheel forward pattern | Left and right wheels produced forward robot motion | Passed |
| Raised-wheel left-strafe pattern | FL/RR backward and FR/RL forward after sign correction | Passed |
| Raised-wheel counterclockwise pattern | Left wheels backward and right wheels forward after sign correction | Passed |
| Stop behavior | Control release, X, Driver Station STOP, and `robot.stop()` removed output | Passed |

For a future robot, change one sign or direction at a time, rebuild, and repeat the same observation.
Do not correct several unknown directions simultaneously.

## Historical pre-migration tuning records

The following recorded tables describe the earlier Pedro 2/Panels workflow. They are retained as
historical evidence and must not be used as the current Pedro 3 tuner menu or API instructions.
The current Pedro 3 tuning entry point is the robot-hosted AutoTune page at
`http://192.168.43.1:10158`, with Mecanum, Pinpoint, and Foresight registered in `Tuning.java`.

## Tuning programs and recorded results

Pedro 3 tuners are selected from the robot-hosted AutoTune page, not a Driver Station
`Team A Pedro Tuning` OpMode.

### Historical Pedro 2 tuning-program parameters

These values control the tests themselves. They are not all robot constants, but they may need to
change when another robot has different speed, available floor space, or stopping distance.

| Program | Former source parameter | Purpose |
| --- | --- | --- |
| All selected tuners | `OFFICIAL_TUNING_MAX_POWER = 1.0` | Overrides the normal `0.20` application ceiling during official tuning |
| Forward Tuner | `DISTANCE = 48 in` | Manual localization distance |
| Lateral Tuner | `DISTANCE = 48 in` | Manual localization distance |
| Turn Tuner | `ANGLE = 2π rad` | Manual localization rotation target |
| Forward Velocity Tuner | `DISTANCE = 48 in`, `RECORD_NUMBER = 10` | Full-power travel and number of final velocity samples averaged |
| Lateral Velocity Tuner | `DISTANCE = 48 in`, `RECORD_NUMBER = 10` | Full-power travel and number of final velocity samples averaged |
| Forward Zero Power Acceleration Tuner | `VELOCITY = 30 in/s` | Speed reached before cutting power |
| Lateral Zero Power Acceleration Tuner | `VELOCITY = 30 in/s` | Speed reached before cutting power |
| Predictive Braking Tuner | 12 test powers, `BRAKING_POWER = -0.2`, `DRIVE_TIME_MS = 1000` | Generates the measured braking curve |
| Translational Tuner | `DISTANCE = 40 in` | Continuous alternative-PIDF test path |
| Heading Tuner | `DISTANCE = 40 in` | Continuous heading test path |
| Drive Tuner | `DISTANCE = 40 in` | Continuous alternative drive-PIDF test path |
| Line | `DISTANCE = 40 in` | Combined validation path |
| Centripetal Tuner | `DISTANCE = 20 in` | Curved alternative-PIDF test path |
| Circle | `RADIUS = 10 in` | Test-only circle radius |
| Panels Drawing | `ROBOT_RADIUS = 9 in` | Display circle only; not robot geometry |

Most tuners use a field pose around `(72, 72)` so their test geometry is visible in Panels. That is
a tuner display/test origin, not the autonomous starting pose.

### Completed and used for the former Pedro 2 configuration

| Menu path | Program or test | How it was used | Recorded result |
| --- | --- | --- | --- |
| Automatic | Forward Velocity Tuner | Full-power 48-inch run; averages the latest velocity samples | Recorded `xVelocity=62.61374213751846 in/s` |
| Automatic | Lateral Velocity Tuner | Full-power 48-inch left run | Recorded `yVelocity=49.757958599901585 in/s` |
| Automatic | Forward Zero Power Acceleration Tuner | Accelerate to 30 in/s, cut power, average deceleration | `-42.4832745291992 in/s^2` |
| Automatic | Lateral Zero Power Acceleration Tuner | Accelerate left to 30 in/s, cut power, average deceleration | `-50.60694780564594 in/s^2` |
| Manual | Heading Tuner | Alternating 40-inch line; heading PIDF adjusted through Panels | `P=2.2`, `I=0.2`, `D=0.189`, `F=0.02`; returned to original heading |
| Automatic | Predictive Braking Tuner | Twelve alternating one-second drive/brake samples | `kLinear=0.051792761842529726`, `kQuadratic=0.002367856854157051` |
| Tests | Line | Continuous 40-inch forward/back validation with all corrections active | Accepted Predictive Braking `P=0.15`, parametric end `0.97`; almost no jitter, no overshoot, endpoint reversal passed, no lateral/heading drift |
| Autonomous path menu | LP-11 Pilot | 24-inch restricted-power path | Director reported the pilot worked; detailed final telemetry was not recorded |

The Predictive Braking Tuner's 12 power samples are
`1.0, 1.0, 1.0, 0.9, 0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3, 0.2`. Each drive sample lasts
`1000 ms`; braking uses `-0.2` times the current travel direction.

The former Pedro 2 Line test tried Predictive Braking P values `0.4`, `0.3`, `0.2`, and `0.15`. At `0.4`, one
validation produced heavy holding jitter. Panels did not apply the attempted change to `0.3`, so
the remaining values were changed in code and re-uploaded. The accepted `0.15` test had only very
slight jitter and no overshoot or drift.

### Earlier evidence retained for comparison

| Test | Observations | Why it is not the current constant |
| --- | --- | --- |
| Restricted forward shakedowns | `8.62`, `10.107`, `9.997 in/s`; accepted low-power average `10.052 in/s` | Measured at application power `0.20`, not full tuning power |
| Restricted lateral shakedowns | `7.394` and `7.324 in/s`; average `7.359 in/s` | Measured at application power `0.20`, not full tuning power |
| First full-power forward pair | `52.741` and `52.008 in/s`; average `52.3745` | Superseded by current recorded tuner output |
| First full-power lateral pair | `41.477` and `42.560 in/s`; average `42.0185` | Superseded by current recorded tuner output |

### Available but not completed or not adopted

| Menu path | Program | Team A status |
| --- | --- | --- |
| Localization | Offsets Tuner | Not used; accepted offsets were manually measured |
| Localization | Forward Tuner | Not used to change Pinpoint resolution; 24-inch diagnostic checks were used instead |
| Localization | Lateral Tuner | Not used to change Pinpoint resolution; 24-inch diagnostic checks were used instead |
| Localization | Turn Tuner | Not used to change a multiplier; 90-degree diagnostic check passed |
| Manual | Translational Tuner | Not adopted because Team A selected Predictive Braking |
| Manual | Drive Tuner | Not adopted because Team A selected Predictive Braking |
| Manual | Centripetal Tuner | Not adopted; centripetal scaling is `0.0` for Predictive Braking |
| Tests | Triangle | No accepted run recorded |
| Tests | Circle | No accepted run recorded |
| Path menu | DECODE Route | Added to code; no accepted physical run recorded |
| Path menu | Curving Test | Added to code; no accepted physical run recorded |
| Path menu | Illegal Path | Added to code; no accepted physical run recorded |

`Localization Test` behavior was covered by the narrower LP-09 diagnostic and its recorded tests.
Do not infer that every item visible in the tuning menu was completed.

## Pedro 3 AutoTune usage

Pedro 3 tuning runs through the robot-hosted AutoTune page at
`http://192.168.43.1:10158`, not through the removed Panels menu or a Driver Station selector.
The registered procedures are Mecanum, Pinpoint, and Foresight. The Foresight procedure generates
Java source; it does not silently edit `Constants.java`. Copy its generated `ForesightConfig` into
that file, rebuild, and then run the Tests procedure before trusting a path.

The old Panels workflow described below is retained only as historical evidence from the former
Pedro 2 implementation.

### Tests that used Panels

- Automatic velocity and zero-power-acceleration tuners displayed their results through Panels
  telemetry and drew the robot/pose history.
- Heading Tuner used Panels live configuration under `Tuning -> Follower -> Constants` to reach
  `2.2, 0.2, 0.189, 0.02`.
- Predictive Braking Tuner displayed all sample and fitted coefficient results through Panels.
- Line displayed the field and pose history while validating Predictive Braking and path end
  behavior. A live Predictive Braking P edit was attempted, but did not apply; later trials used
  source-code changes and re-uploading.

After editing a value in Panels, press Enter to apply it to the running Robot Controller. Panels
changes are temporary and are not saved into Java source. Record the accepted result, stop the
OpMode, copy it into `TeamAPedroConfiguration`, rebuild, upload, and repeat the validation test.

## Recommended setup process for another robot

### 1. Create a fresh hardware record

Record, photograph, or diagram all of the following before editing constants:

- Robot name and build date.
- Complete competition mass in kilograms, including battery and bumpers.
- Bumper-to-bumper width and length.
- Wheelbase length and track width if the chosen localizer requires them.
- Drive wheel type/diameter, motor/gearbox type, and external gear ratio.
- Four motor hardware names, hub, and port numbers.
- Required motor directions determined from wheel observations.
- Localizer type and hardware-map name.
- Pinpoint hub and I2C port, mounting orientation, pod model, pod connections, and cable routing.
- Pod offsets measured from the center of rotation using Pedro's coordinate diagram.
- Initial intended application power limit and field starting pose.

### 2. Install and verify the pinned software

1. Add the Dairy Maven repository and the pinned `revhub` and `tuning` dependencies.
2. Set compile SDK 34 and use Java 17 for this repository version.
3. Gradle Sync in Android Studio.
4. Run `.\gradlew.bat TeamCode:assembleDebug`.
5. Inspect the actual Pedro API for the pinned version before copying code from a different
   Quickstart or documentation version.

### 3. Enter only measured initialization constants

Create new team-specific `ForesightConfig`, `MecanumConfig`, and `PinpointConfig` objects in
`Constants.java`. Enter
mass, names, measured offsets, pod model/resolution, and an intentionally conservative maximum
power. Keep manual and path-following permissions closed until their tests pass.

### 4. Verify localization with motors disabled

1. Confirm the pose is available and stable near the approved start pose.
2. Push the unpowered robot forward a measured distance; X must increase by approximately that
   distance.
3. Push it left; Y must increase.
4. Rotate counterclockwise by a measured angle; heading must increase.
5. Push forward and return to the start to check accumulated error.
6. If one sign is wrong, change only that encoder direction and repeat.

### 5. Verify motor directions and stopping

Use an approved raised-wheel test with a low command limit. Verify forward, strafe, and rotation
wheel patterns one at a time. Confirm control release, explicit cancel, Driver Station STOP, and
the OpMode `stop()` callback all remove output before ground tuning.

### 6. Run the version-matched tuning workflow

For the current Pedro 3 Foresight route:

1. Run the Mecanum tuner and verify motor names/directions.
2. Run the Pinpoint tuner and verify pod directions/offsets.
3. Run Foresight Velocity and Deceleration procedures.
4. Run Heading Braking and Heading Tuner procedures.
5. Run Forward/Strafe Braking procedures.
6. Run Forward/Strafe Translational procedures.
7. Copy the generated `ForesightConfig` Java output into `Constants.java`.
8. `Tests.java` is present but not registered in `Tuning.java`; register it when ready, then rebuild
   and validate a short path.

If the team selects the traditional PIDF route instead, follow the version-matched Pedro sequence
for zero-power acceleration, translational PIDF, drive PIDF, and centripetal tuning. Do not combine
parts of both algorithms without understanding the version's controller behavior.

### 7. Validate a simple path before competition routes

Start with one short straight path at the approved application power. Record expected and observed
start/end pose, position and heading error, completion behavior, and every stop path. Only then add
longer curves, sharp heading changes, and competition paths.

## Future-robot replacement checklist

| What changes | Where to update in this repository | Required validation |
| --- | --- | --- |
| Dependency versions | `build.dependencies.gradle` and matching tuning adaptation | Sync, API inspection, full rebuild, repeat all tuning |
| Robot mass | `ForesightConfig`/team configuration | Reweigh complete robot |
| Motor names | `MecanumConfig.*Name` | Driver Station configuration and initialization |
| Motor directions | `MecanumConfig.*Direction` | Raised-wheel forward/strafe/turn checks |
| Application power | `APPLICATION_MAX_POWER` and follower controls | Controlled path and stop tests |
| Maximum velocities | `ForesightConfig.maxAchievable*Velocity` | Repeat Foresight velocity tuners |
| Natural deceleration | `ForesightConfig.natural*Deceleration` | Repeat Foresight deceleration tuners |
| Heading/translational gains | `ForesightConfig` controller fields | Foresight tuning and Tests |
| Braking coefficients | `ForesightConfig.*BrakeCoefficients` | Forward/Strafe Braking tuners |
| Pinpoint name | `PinpointConfig.name` | Hardware-map lookup and stable pose |
| Pod model/resolution | `PinpointConfig.podType` and resolution fields | Physical pod inspection and distance checks |
| Pod offsets | `PinpointConfig.xPodOffset/yPodOffset` | Pinpoint tuner, then pose tests |
| Encoder directions | `PinpointConfig.xPodDirection/yPodDirection` | Unpowered forward and left checks |
| Starting pose | `TeamAPedroConfiguration` and each `TeamAPedroPathRoute` | Confirm field placement and telemetry before START |
| Path end values | `PathConstraints` | Line and real path completion tests |
| Robot dimensions | Every `.pp` Visualizer project and optional Panels `ROBOT_RADIUS` | Physical measurement and collision review |
| Routes | Team-specific path classes and `TeamAPedroPathRoute` | Visual review, build, then physical validation |

## Sign-off record for a new robot

Copy this table into the new robot's implementation record and fill it with evidence rather than
sample values.

| Gate | Evidence required | Accepted by/date |
| --- | --- | --- |
| Hardware facts recorded | Names, ports, directions, mass, dimensions, localizer, pods, offsets | |
| Safe initialization | Successful build/upload; stable pose; drive disabled | |
| Localization | Forward/left/rotation signs, distances, and return error | |
| Raised-wheel drive | Forward/strafe/rotation patterns and all stop paths | |
| Velocity tuning | Repeated forward/lateral full-power results | |
| Heading tuning | Accepted PIDF and settling behavior | |
| Drive algorithm | Complete Predictive Braking or PIDF evidence | |
| Line validation | Endpoint behavior, jitter, overshoot, and drift | |
| Application limit | Approved normal maximum power | |
| Pilot path | Start/end pose, error, completion, cancel, and STOP evidence | |

## Official references

- [Pedro Pathing installation](https://pedropathing.com/docs/pathing/installation)
- [Pedro constants](https://pedropathing.com/docs/pathing/constants)
- [Pedro mecanum setup and mass](https://pedropathing.com/docs/pathing/tuning/setup)
- [Pedro Pinpoint setup](https://pedropathing.com/docs/pathing/tuning/localization/pinpoint)
- [Pedro localization testing](https://pedropathing.com/docs/pathing/tuning/localization)
- [Pedro automatic tuners](https://pedropathing.com/docs/pathing/tuning/automatic)
- [Pedro Predictive Braking configuration](https://pedropathing.com/docs/pathing/tuning/drive-algorithm/predictive/configuration)
- [Pedro Predictive Braking reference](https://pedropathing.com/docs/pathing/reference/predictive)
- [goBILDA Pinpoint Odometry Computer user guide](https://www.gobilda.com/content/user_manuals/3110-0002-0001_user-guide.pdf)

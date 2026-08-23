# Team A Pedro Pathing Reference

This document describes the implementation currently in the repository. It is limited to the Team
A Pedro composition and `TeamAPedroTeleOp`; it does not describe `TeamATeleOp`.

When this document disagrees with the code, the code and measured hardware are authoritative. The
main implementation files are:

- [`TeamAPedroTeleOp.java`](../src/main/java/org/firstinspires/ftc/teamcode/opmodes/teleop/TeamAPedroTeleOp.java)
- [`TeamAPedroRobot.java`](../src/main/java/org/firstinspires/ftc/teamcode/robots/teamA/TeamAPedroRobot.java)
- [`TeamAPedroDriveController.java`](../src/main/java/org/firstinspires/ftc/teamcode/robots/teamA/TeamAPedroDriveController.java)
- [`TeamAPedroConfiguration.java`](../src/main/java/org/firstinspires/ftc/teamcode/robots/teamA/TeamAPedroConfiguration.java)
- [`DriveSubsystem.java`](../src/main/java/org/firstinspires/ftc/teamcode/common/subsystems/drive/DriveSubsystem.java)

## Current software and hardware scope

| Item | Current value |
| --- | --- |
| FTC SDK | `11.2.1` |
| Pedro FTC | `com.pedropathing:ftc:2.1.2` |
| Pedro telemetry | `1.0.0` |
| FullPanels | `1.0.12` |
| Compile SDK | `34` |
| Minimum SDK | `24` |
| Java | Microsoft OpenJDK 17.0.x is the documented build environment |
| Robot | Four-wheel mecanum drivetrain with goBILDA Pinpoint localization |
| Pedro hardware-map name | `pinpoint` |
| Application power ceiling | `APPLICATION_MAX_POWER = 0.20` |
| Starting pose | `(0, 0, 0)` unless an autonomous route supplies another pose |

The Team A Pedro robot is a separate composition. It is the sole owner of the Pedro follower,
Pinpoint, and Pedro drivetrain motors for its OpModes. The ordinary Team A robot remains separate.

## TeleOp controls

`TeamAPedroTeleOp` uses gamepad 1 and calls the Robot API once per loop:

| Input | Manual-drive behavior |
| --- | --- |
| Left stick Y | Forward/backward translation |
| Left stick X | Strafe translation |
| Right stick X | Rotation |
| Y | Toggle heading hold on or off |
| Right bumper + left stick direction | Select an eight-way preset heading |

Y is the only button that toggles the heading-hold FSM. Pressing Y once enables heading hold and
captures the current Pinpoint heading. Pressing Y again disables heading hold, clears the selected
preset, and returns to manual drive. There is no X-based heading-control action in this TeleOp.

While heading hold is active, left-stick translation remains available, but right-stick rotation is
not used as the rotation command. The controller replaces that rotation command with its heading
correction. In manual mode, right-stick X directly controls rotation.

## Preset headings

Hold the right bumper and aim the left stick. The stick is quantized to the nearest 45-degree
direction after its magnitude passes `PRESET_DIRECTION_DEADZONE` (`0.5`). The field-heading
convention is:

| Stick direction | Target heading |
| --- | ---: |
| Right / E | `0°` |
| Up / N | `90°` |
| Left / W | `180°` (equivalent to `-180°`) |
| Down / S | `-90°` |
| Up-right / NE | `45°` |
| Up-left / NW | `135°` |
| Down-right / SE | `-45°` |
| Down-left / SW | `-135°` |

Selecting a preset automatically enables heading hold. Releasing the right bumper does not cancel
the selected target; the robot continues holding it. Moving to another sector replaces the target
immediately. Targets are not queued, so rapid movement through several directions does not make the
robot attempt each direction in sequence.

`PRESET_DIRECTION_HYSTERESIS_RADIANS` is currently 5 degrees. It is added to the normal 22.5-degree
sector boundary so the selector does not chatter near a boundary. The selected sector must be left
by more than 27.5 degrees before a new sector is accepted.

## Heading-hold implementation

Heading hold is not a Pedro path. It is a TeleOp drive mode that uses the current Pinpoint/Pedro
pose and sends a normal TeleOp drive command every FTC loop:

1. `TeamAPedroTeleOp` requests manual drive, heading hold, or a new heading target.
2. `DriveSubsystem` selects the requested drive mode and dispatches it through the drive FSM.
3. `TeamAPedroDriveController` starts Pedro TeleOp drive with `startTeleOpDrive()`.
4. The controller computes the signed shortest-angle error: target heading minus current heading.
5. The correction is `active Pedro heading P × heading error`, clamped to
   `MAX_HEADING_HOLD_ROTATION` (`1.0`).
6. The correction is sent as the rotation component of Pedro's TeleOp command, then
   `follower.update()` applies the command.

The actual P value is read from the active follower through
`follower.getConstants().getCoefficientsHeadingPIDF().P`. Therefore, changing the follower's
heading P value changes the custom TeleOp correction when that follower is used. The controller does
not contain a second hard-coded heading P value.

The custom TeleOp correction is P-only. Pedro's configured I, D, and F values remain part of the
Pedro follower configuration for Pedro-controlled path behavior; they are not used by this custom
TeleOp correction.

## Tolerance and safety limits

| Setting | Current value | Purpose |
| --- | ---: | --- |
| `HEADING_HOLD_TOLERANCE_RADIANS` | `2°` | Stops correction inside the target deadband |
| `HEADING_HOLD_TOLERANCE_HYSTERESIS_RADIANS` | `0.5°` | Requires more than `2.5°` error before correction resumes |
| `MAX_HEADING_HOLD_ROTATION` | `1.0` | Limits custom rotation correction before Pedro's power limit |
| `APPLICATION_MAX_POWER` | `0.20` | Limits configured drivetrain output |

The tolerance is a deadband with hysteresis. It prevents continuous micro-adjustments near the
target and prevents rapid toggling at the boundary. These values are independent of the P gain and
must be retuned separately if the robot's behavior changes.

## Drive-mode ownership and conflicts

The TeleOp loop issues one high-level drive request and then calls `robot.update()`. The drive
subsystem owns the active mode; the OpMode does not write motors or call Pedro directly.

- Manual drive uses Pedro `startTeleOpDrive()`, `setTeleOpDrive(...)`, and `update()`.
- Heading hold uses the same Pedro TeleOp interface with a computed rotation correction.
- Autonomous path following uses Pedro's path follower through a separate requested mode.
- Starting TeleOp drive breaks any active Pedro path before manual or heading commands are sent.
- Stopping breaks following, clears heading state, and leaves the drivetrain disabled.

Heading hold therefore does not run a path at the same time as manual translation, and preset
selection does not create a path. The left stick can continue translating while the heading target
is being changed.

One edge case is that a preset selected in the same loop as a Y press is processed first and the Y
toggle is processed second. The final request in that loop is determined by the Y toggle. This is
deterministic and does not create two simultaneous drive modes.

## Recorded Team A Pedro configuration

The following values are currently constructed in `recordedTeamAConfiguration()`:

```text
mass = 4.85 kg
heading PIDF = P 2.2, I 0.2, D 0.189, F 0.02
forward zero-power acceleration = -42.4832745291992 in/s^2
lateral zero-power acceleration = -50.60694780564594 in/s^2
Predictive Braking = P 0.15, kLinear 0.051792761842529726,
                    kQuadratic 0.002367856854157051
centripetal scaling = 0.0
forward velocity = 62.61374213751846 in/s
lateral velocity = 49.757958599901585 in/s
Pinpoint forward pod offset = -6.25 in
Pinpoint strafe pod offset = -10.0 in
distance unit = INCH
forward encoder direction = REVERSED
strafe encoder direction = REVERSED
path constraints = parametric end 0.97, timeout 100 ms
```

These are Team A measurements and configuration records, not universal Pedro defaults. Physical
dimensions, pod offsets, encoder directions, gains, velocities, and path constraints must be
measured again for another robot.

## Retuning procedure

1. Verify the installed Pedro version and inspect the actual API for that version.
2. Change the relevant values in `TeamAPedroConfiguration` or use the approved Pedro tuning
   workflow. The heading PIDF and TeleOp behavior limits are named constants; several measured
   velocity, acceleration, braking, and localization values are written directly in
   `recordedTeamAConfiguration()` and must be updated there.
3. If Panels or another live tool changes follower constants, copy accepted values back into the
   recorded configuration when final code is prepared.
4. Search the repository for the old value and the constant name. Confirm that no stale duplicate
   literal remains in an OpMode, controller, diagnostic, tuner, or document.
5. Rebuild with `./gradlew.bat TeamCode:assembleDebug` on Unix-like shells or
   `.\gradlew.bat TeamCode:assembleDebug` in PowerShell.
6. Repeat supervised localization, heading, manual-drive, and path validation as applicable.

The P term used by custom TeleOp heading correction is linked to the active follower value. The
custom tolerance, hysteresis, correction cap, preset deadzone, and preset hysteresis are separate
named settings. The application power ceiling is also separate and must not change implicitly when
tuning heading behavior.

## Alignment with Pedro's official tuning workflow

The repository configuration follows Pedro's documented high-level order:

1. Configure follower, drivetrain, localizer, and path-constraint constants.
2. Verify localization and coordinate/sign behavior.
3. Run the forward and lateral velocity tuners.
4. Run the heading tuner and record the heading PIDF values.
5. Select one drive algorithm:
   - Predictive Braking: tune Predictive Braking, choose its `P`, disable centripetal scaling when
     appropriate, and validate with LineTest.
   - Traditional PIDF: tune zero-power acceleration, translational PIDF, drive PIDF, and
     centripetal behavior.
6. Copy accepted live Panels values into the source configuration and validate the resulting paths.

The current Team A configuration selected Predictive Braking. Its recorded values correspond to
the official Predictive Braking setup: tuned `kLinear` and `kQuadratic`, a selected braking `P`,
`centripetalScaling(0.0)`, and a `0.97` parametric-end constraint.

There is one deliberate implementation distinction: Pedro's online Heading Tuner tunes the
follower's heading PIDF for Pedro path following. `TeamAPedroTeleOp` does not call that PIDF
controller directly. Its custom heading hold uses the active follower P value in a separate P-only
correction, with its own tolerance, hysteresis, and correction cap. Therefore, after changing
heading values, both path behavior and supervised TeleOp heading behavior must be checked.

## Predictive Braking tuning guide

Use this procedure when the selected Pedro drive algorithm is Predictive Braking. Predictive
Braking and the traditional translational/drive/centripetal PIDF route are alternatives. Do not run
the traditional PIDF tuners just because their values appear in an older configuration or test
record.

### Step 1: Confirm the prerequisites

Before applying powered tuning:

1. Confirm the Pedro FTC version and the actual APIs available in that version.
2. Configure the follower, mecanum drivetrain, Pinpoint localizer, and path constraints.
3. Verify motor names, motor directions, pod offsets, encoder directions, distance units, and the
   starting pose.
4. Verify localization with the robot unpowered and confirm the X, Y, and heading signs.
5. Run the restricted manual-drive safety checks before using full tuning power.

Predictive Braking depends on reliable localization and velocity measurements. Do not tune braking
to compensate for an incorrect pod direction, pod offset, motor direction, or field coordinate
convention.

### Step 2: Tune forward and lateral velocity

Run Pedro's velocity tuners and record separate forward and lateral maximum velocities. These values
belong in the drivetrain constants, such as `xVelocity(...)` and `yVelocity(...)`.

The current Team A recorded values are:

```text
xVelocity = 62.61374213751846 in/s
yVelocity = 49.757958599901585 in/s
```

These are not Predictive Braking coefficients. They describe the drivetrain's measured ability to
move forward and laterally and are required before path-following results can be interpreted.

### Step 3: Tune heading PIDF

Run Pedro's `HeadingTuner` and tune `coefficientsHeadingPIDF` in Panels. The robot should return to
its starting heading with minimal oscillation. Adjust P, I, D, and F as appropriate for the robot,
then press Enter in Panels so the live value is applied.

When the result is accepted, copy the values into the follower construction in
`TeamAPedroConfiguration` in the order `P, I, D, F`:

```java
.headingPIDFCoefficients(new PIDFCoefficients(P, I, D, F))
```

For the current Team A record:

```text
P = 2.2
I = 0.2
D = 0.189
F = 0.02
```

Heading tuning remains required when using Predictive Braking. Predictive Braking replaces the
translational/drive braking algorithm; it does not replace heading control.

### Step 4: Choose Predictive Braking instead of traditional PIDFs

At this point choose one drive algorithm. For Predictive Braking, do not continue into the
traditional branch. The following are not required Predictive Braking tuners:

- Forward or lateral zero-power acceleration tuners for the traditional braking model
- Translational PIDF tuner
- Drive PIDF tuner
- Centripetal Force tuner

The repository may retain zero-power acceleration or older PIDF values as historical records, but
they are not evidence that the Predictive Braking branch must be tuned as well.

### Step 5: Run PredictiveBrakingTuner

From the Pedro tuning OpMode, select:

```text
Tuning -> Automatic -> PredictiveBrakingTuner
```

Run the tuner with the robot secured and the approved test area, power, and Driver Station STOP
procedure. The tuner determines the braking-distance coefficients:

- `kLinear`: the velocity-proportional part of braking distance.
- `kQuadratic`: the velocity-squared part of braking distance.

Use a starting braking `P` near `0.1` if the tuner or the current Pedro workflow requests one. The
tuner may show heading rotation or slight lift during braking; these observations do not by
themselves invalidate the braking-distance result, provided localization remains accurate and the
test is safe.

Copy the accepted tuner results into:

```java
.predictiveBrakingCoefficients(
        new PredictiveBrakingCoefficients(kP, kLinear, kQuadratic))
```

The parameter order is `kP, kLinear, kQuadratic`, not the order in which the tuner may display the
two distance coefficients.

### Step 6: Tune Predictive Braking P with LineTest

Run Pedro's `LineTest` after inserting `kLinear` and `kQuadratic`. Adjust the Predictive Braking
`kP` to maximize holding strength and endpoint accuracy without introducing jitter. Pedro's guide
identifies a typical range around `0.05` to `0.3`, but the robot's measured behavior is the deciding
criterion.

- If the robot jitters or holds too aggressively, reduce `kP`.
- If it stops too softly or leaves excessive endpoint error, increase `kP` carefully.
- If deceleration needs to be smoother or begin earlier, evaluate the braking coefficients as well;
  do not use an excessive `kP` to hide an incorrect braking curve.

The current Team A recorded Predictive Braking values are:

```text
kP = 0.15
kLinear = 0.051792761842529726
kQuadratic = 0.002367856854157051
```

### Step 7: Set Predictive Braking-specific constants

Predictive Braking naturally accounts for centripetal effects, so the official workflow recommends
turning centripetal scaling off for this branch:

```java
.centripetalScaling(0.0)
```

Set the parametric-end constraint to a value such as `0.97` or `0.95` after validation. Do not set
it below `0.90`; ending the path too early can prevent Predictive Braking from completing its
braking behavior. The current Team A path constraint uses `0.97`.

### Step 8: Copy values from live tuning into source code

Panels changes are live tuning changes and are not automatically saved to the project. After every
accepted tuning session:

1. Copy heading PIDF values into the heading PIDF construction.
2. Copy `kP`, `kLinear`, and `kQuadratic` into `predictiveBrakingCoefficients(...)` in the correct
   order.
3. Confirm `.centripetalScaling(0.0)` and the selected parametric-end constraint.
4. Search the repository for the old numeric values and duplicate literals.
5. Rebuild and upload the source-controlled configuration.

For this project, the custom TeleOp heading controller reads the active follower P value at runtime,
but its tolerance, hysteresis, correction cap, preset thresholds, and application power ceiling are
separate settings. Recheck TeleOp heading hold after changing heading P.

### Step 9: Validate the complete autonomous behavior

Run a short path and then the intended path chain under supervised conditions. Record:

- Start and end pose.
- Position and heading error.
- Overshoot, undershoot, jitter, or drift.
- Whether the robot stops when an individual path ends.
- Whether path-chain transitions preserve the desired momentum.
- Cancellation, Driver Station STOP, and `robot.stop()` behavior.

Predictive Braking may fully stop at individual path boundaries. Use path chains when continuous
motion is desired and the route does not require a full stop. Do not accept a tuning result from a
software build alone; physical validation is required.

### Predictive Braking decision checklist

- [ ] Localization and signs were verified before tuning.
- [ ] Forward and lateral velocities were measured.
- [ ] Heading PIDF was tuned and copied into source.
- [ ] `PredictiveBrakingTuner` produced `kLinear` and `kQuadratic`.
- [ ] `LineTest` was used to select Predictive Braking `kP`.
- [ ] Centripetal scaling is disabled for the Predictive Braking branch unless a documented test
      justifies otherwise.
- [ ] Parametric end is validated and remains at or above `0.90`.
- [ ] Traditional translational, drive, and centripetal PIDF tuners were not mixed into this branch
      without a deliberate algorithm change.
- [ ] Panels values were copied into source code.
- [ ] The source was rebuilt and the physical path, stop, cancel, and TeleOp behavior were tested.

## Validation status and limitations

Recorded checks include Pinpoint pose availability, forward and lateral sign checks, a measured
counterclockwise rotation, return-to-start behavior, and restricted raised-wheel drivetrain checks.
The TeamCode build has been verified with `TeamCode:assembleDebug`.

Physical TeleOp heading-hold and preset-heading behavior still require supervised on-robot testing
after hardware or tuning changes. A software build does not prove heading accuracy, settling time,
overshoot, or field safety.

## Official references

- [Pedro installation](https://pedropathing.com/docs/pathing/installation)
- [Pedro constants](https://pedropathing.com/docs/pathing/constants)
- [Pedro TeleOp example](https://pedropathing.com/docs/pathing/examples/teleop)
- [Pedro heading tuning](https://pedropathing.com/docs/pathing/tuning/heading)
- [Pedro Pinpoint setup](https://pedropathing.com/docs/pathing/tuning/localization/pinpoint)
- [Pedro localization testing](https://pedropathing.com/docs/pathing/tuning/localization)
- [Pedro Predictive Braking configuration](https://pedropathing.com/docs/pathing/tuning/drive-algorithm/predictive/configuration)
- [goBILDA Pinpoint user guide](https://www.gobilda.com/content/user_manuals/3110-0002-0001_user-guide.pdf)

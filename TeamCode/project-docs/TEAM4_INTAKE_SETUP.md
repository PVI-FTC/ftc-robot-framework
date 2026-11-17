# Team 4 continuous-rotation intake

Use **Team 4 TeleOp**. Configure the servo's connected Hub servo port as
**Continuous Rotation Servo**, with exact name **intake4Servo**. The name is a
new default chosen for this implementation; rename the configuration if needed.
The student reports the intake is on **Expansion Hub SERVO port 0**. The code finds
it by name. Do not replace the positional `conveyor4Lift` entry on its separate
signal port; the working linear actuator connects through a power module.

After START, release G1 A and Y once. Then:

| G1 control | Action |
| --- | --- |
| Hold Y | Intake, initial +0.50 servo power |
| Hold A | Reverse/eject, initial -0.50 servo power |
| Release both or hold both | Stop, zero power |

INIT and Driver Station STOP command zero power. A missing/wrongly typed servo
appears as `Intake Available = false`; driving remains usable. G1 drive sticks and
all G2 conveyor controls retain their mappings. This is hold-to-run, not toggle.

The wrapper is `robots.team4.Team4IntakeHardware`, which implements the existing
IntakeHardware API for a CRServo. Team4Robot reuses IntakeSubsystem and its existing
FSM/states. Shared robot constructors retain their original DC motor intake unless
the new `(DriveHardware, IntakeHardware)` overload is used, as in Team 4 composition.

The reported model is goBILDA 2000-0025-0002, a dual-mode servo. It must be programmed
to continuous rotation using its servo programmer; the robot configuration cannot
switch its internal mode. Physical intake rotation direction is unverified. Check that Y
pulls material in; if reversed, change `Team4IntakeHardware.DIRECTION` to REVERSE.
`MAX_POWER = 0.50` is an initial test setting, not a measured speed. Confirm release,
simultaneous A/Y, and STOP stop the servo. Confirm the servo's rated supply voltage
from its own model specifications before choosing its power source.

Latest physical report: telemetry changes to Intaking/Ejecting and Intake Available
is true, but the intake does not move. Availability means a configured SDK device
was found, not that a connected servo is detected. Wiring, internal servo mode,
installed code and actual output still require verification; this is unresolved.

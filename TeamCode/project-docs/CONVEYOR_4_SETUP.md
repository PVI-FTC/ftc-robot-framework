# Team 4 conveyor-4

Use **Team 4 TeleOp**. Gamepad 1 retains the restored drive controls. Gamepad 2
controls the goBILDA 5202 Series 1,150 RPM belt motor and Actuonix
L16-140-35-6-R positional lift servo.

## Configure hardware

| Device | Configuration name | Connection |
| --- | --- | --- |
| Belt motor | `conveyor4Motor` | Free DC motor port on a Control/Expansion Hub |
| Lift servo | `conveyor4Lift` | Servo port configured as a positional Servo |

The student's final wiring clarification supersedes the earlier lift relocation:
**Expansion Hub SERVO port 0 belongs to the intake (`intake4Servo`), not the lift.**
The linear lift remains connected through its power module; configure `conveyor4Lift`
on the separate Hub signal port feeding that module channel. That signal port has
not been physically confirmed. Do not move or reconfigure the working lift to match
the earlier, mistaken port assignment. See TEAM4_INTAKE_SETUP.md for the intake.

All four drive motor names remain `frontLeft`, `frontRight`, `rearLeft`, `rearRight`.
If they occupy all four Control Hub motor ports, the conveyor needs a motor port
on an Expansion Hub. Do not share or rename a drive motor port for the conveyor.
Select the matching goBILDA motor entry if available. The belt runs open loop in
RUN_WITHOUT_ENCODER; no encoder cable or RPM regulation is required by this code.

The servo wrapper sets 1000-2000 microsecond pulses (retracted-extended), with
FORWARD direction. Configure the name on the Hub servo port that supplies its
signal, including when routing it through a servo power module. The actuator's
rated supply is 6 V. External servo power remains present when PWM is disabled;
loss of PWM is not a verified mechanical stop. The program cannot prevent
startup movement before INIT. Make wiring changes with power off.

Devices are optional individually to preserve drive-only use. Telemetry reports
motor/lift availability; a missing or misspelled name means that device cannot run.

## Controls

After START, center the G2 left stick and release A/Y once to enable controls.

| G2 control | Action |
| --- | --- |
| Hold A | Run belt forward at initial power +0.30 |
| Hold Y | Run belt reverse at initial power -0.30 |
| Release A/Y, or hold both | Stop belt |
| B / X | Unassigned; no reference or confirmation needed |
| Left stick up | Select FULL extension (target 1.0 / 2000 us) |
| Left stick down | Select FULL retraction (target 0.0 / 1000 us) |
| Center stick | Keep the last endpoint; actuator may continue moving to it |

As requested on 2026-11-13, endpoint control replaces gradual jogging and B/X setup.
No stored confirmation or saved target is read or written. INIT/START send no lift
target; the first deliberate stick command after neutral enables PWM at an endpoint.
Stick values within +/-0.10 keep the last selected endpoint. Deflection magnitude
does not set speed. Centering the stick is not an intermediate-position stop.
Telemetry command names describe selected endpoints, not measured actuator position.
`WAITING_FOR_STICK` means no target has been selected during this run.
STOP zeroes the belt and disables lift PWM, but does not remove external servo power.
Use robot power removal when motion must be cut off.

## Initial verification and tuning

- Verify the mounted mechanism can safely reach the chosen retracted/extended
  positions before using full-stroke targets. Mechanical clearance and travel limits
  have not been measured for this conveyor.
- Verify that positive motor power moves the belt in the intended forward direction.
  Change `Conveyor4Hardware.MOTOR_DIRECTION` if needed; physical direction is unverified.
- `Conveyor4Subsystem.BELT_POWER = 0.30` is an initial limited-power trial, not a
  request to regulate the motor at 1,150 RPM. Tune after checking belt behavior.
- Verify stick endpoint selection, endpoint retention after release, A/Y release,
  simultaneous buttons, and Driver Station STOP on hardware. Respect the Actuonix
  duty cycle and avoid stalling or forcing its rod by hand.

## Implementation

`Team4TeleOp -> InputManager / Team4Robot public API -> Conveyor4Subsystem ->
FSM -> ConveyorState -> Conveyor4Hardware -> FTC SDK`.
The state updates independent belt and lift outputs each loop without blocking.
The display name is `conveyor-4`; Java uses `Conveyor4Subsystem` because hyphens
are not valid Java identifiers. Team-specific code lives under
`robots.team4.conveyor`. No FTC SDK files are modified.

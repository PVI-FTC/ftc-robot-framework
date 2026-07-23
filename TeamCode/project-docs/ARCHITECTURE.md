# PVI-FTC Baseline Software Architecture
## Purpose

PVI-FTC Codex Sequential Repository Build — Master Instructions

PVI-FTC | Editable master guide

This document defines the intended architecture for the shared PVI-FTC baseline. It records design
intent. TeamCode/project-docs/IMPLEMENTATION_STATUS.md records what has actually been implemented.
## Design goals
- Support Teams A, B, and C in one FTC SDK repository.
- Share framework and mechanism behavior when it is genuinely common.
- Allow each robot team to extend or replace hardware and mechanisms without copying the whole
  framework.
- Use finite-state machines for drive, intake, and vision subsystems.
- Support both TeleOp and non-blocking autonomous control.
- Keep the first implementation understandable to beginning Java students.
## Intended package map
core.fsm
- State
- Transition
- FSM
  core.robot
- Subsystem
- Robot
  core.input
- InputManager
  common.hardware
- DriveHardware
- IntakeHardware
- VisionHardware
- RobotHardware
  common.subsystems.drive
- DriveSubsystem and drive states
  common.subsystems.intake
- IntakeSubsystem and intake states
  common.subsystems.vision
- VisionSubsystem and vision states
  common.autonomous
- AutoStep
- AutoSequence
- beginner-readable timed steps
  robots.teamA
  robots.teamB
  robots.teamC
- robot composition and team-specific extensions

PVI-FTC Codex Sequential Repository Build — Master Instructions

PVI-FTC | Editable master guide

opmodes.teleop
opmodes.autonomous
opmodes.testing
- FTC SDK entry points only
## Dependency direction
OpMode
-&gt; Robot public API
-&gt; Subsystem
-&gt; FSM
-&gt; State
-&gt; Hardware wrapper
-&gt; FTC SDK hardware
Dependencies should point downward. Lower layers must not depend on higher layers.
## TeleOp control path
Gamepad
-&gt; InputManager
-&gt; OpMode maps controls to public Robot methods
-&gt; Subsystem request methods
-&gt; FSM transition
-&gt; active State behavior
-&gt; Hardware wrapper
-&gt; FTC SDK hardware
InputManager detects held, pressed, and released controls and exposes axis values. It does not know
TeamARobot and does not manipulate FSMs.
## Autonomous control path
AutoSequence
-&gt; active AutoStep
-&gt; public Robot methods
-&gt; Subsystem request methods
-&gt; FSM transition
-&gt; active State behavior
-&gt; Hardware wrapper
Autonomous does not use InputManager. Autonomous must be non-blocking so robot.update() continues
every loop.
## Lifecycle
FTC OpMode init
-&gt; robot and hardware initialization
FTC OpMode start
-&gt; select safe starting behavior

PVI-FTC Codex Sequential Repository Build — Master Instructions

PVI-FTC | Editable master guide

Each FTC loop
-&gt; update InputManager when in TeleOp
-&gt; map controls or update AutoSequence
-&gt; robot.update()
-&gt; subsystem FSM updates
-&gt; telemetry
FTC stop
-&gt; robot.stop()
-&gt; hardware outputs safe
## FSM behavior
- A State has enter(), update(), exit(), and getName().
- A Transition identifies source state, target state, and a Boolean condition.
- FSM evaluates transitions for the current state in deterministic insertion order.
- At most one transition fires in one update cycle.
- A state does not read gamepads or HardwareMap.
## Hardware policy
- Drive motors are required for Team A baseline drive and fail with a clear initialization error
  when missing.
- Intake and vision are optional during early testing and must fail safely without disabling
  drivetrain operation.
- Hardware names are centralized in wrappers.
- Hardware wrappers do not contain gamepad, FSM, or autonomous logic.
## Shared versus team-specific code
Shared packages contain behavior that can be used without assuming one team’s mechanism geometry or
wiring. Team-specific composition, mappings, constants, wrappers, or replacement subsystem
implementations belong under robots.teamA, robots.teamB, or robots.teamC.
## Deferred features
The baseline intentionally defers:
- a general command scheduler;
- parallel autonomous actions;
- subsystem resource locking;
- IMU heading correction;
- encoder or trajectory driving;
- AprilTag or OpenCV processing;
- dependency injection;
- event buses;
- simulation frameworks.
  A Scheduler should be introduced only when commands, cancellation, parallel execution, subsystem
  requirements, and default behaviors create a demonstrated need.
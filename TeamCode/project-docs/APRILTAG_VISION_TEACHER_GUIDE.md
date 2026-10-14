# AprilTag Vision Teacher Guide

## Purpose

This guide supports the student guide's three-session AprilTag observation pilot. It deliberately
does not integrate vision with localization, Pedro Pathing, autonomous selection, or drivetrain
control. Use the branch-local progress record, not chat memory, to determine progress.

## Facilitation expectations

- Students work on one existing branch. Start Here confirms and records it; it never creates or
  switches branches.
- Each result receives five new one-question-at-a-time learning checks. They are engagement, not
  grading: do not retain answers or scores, and an explained incorrect answer does not block work.
- A prompt becomes `Reviewed` only after student acceptance and required next decisions. A blocked
  result with precise evidence is successful engineering practice.
- Keep the architecture decision separate from `ARCHITECTURE.md` during parallel localization work.
  Do not let either pilot silently overwrite the other's shared documentation edits.

## Session plan

| Session | Prompts | Expected evidence |
| --- | --- | --- |
| 1, 60 min | AV-01, AV-02 | Current FSM flow, official-source research, approved observation-only decision. |
| 2, 60 min | AV-03 through AV-06 | Compiling optional Logitech pilot; no vendor types above hardware and no driving/localization behavior. |
| 3, 60 min | AV-07, AV-08 | Measured camera/tag facts and supervised stationary observations, or a well-recorded blocker. |

## Architecture checks

Require this direction:

```
OpMode -> Robot public API -> VisionSubsystem FSM -> VisionHardware -> hardware source -> FTC API
```

The existing `VisionSubsystem` FSM remains behavior owner. Reject a design where an OpMode chooses
vision states, a hardware source manipulates the FSM, or a source reads gamepad input. A source
may report hardware status but must not become a competing behavior controller.

Require Logitech as the first source through VisionPortal. The later Limelight source is a separate
hardware adapter because the Limelight 3A is not a VisionPortal device. Both produce the same
neutral observation model. Keep one active source in this pilot; do not add camera switching or
multiple-camera behavior without a new reviewed prompt.

## Hardware and safety gate

Before AV-07, students must explicitly confirm the complete STOP warning in the student guide.
Require a real configured camera, physical known-size tag, measured camera mount if testing metric
robot-relative observations, calibration evidence, clear area, adult supervision, and a named
Driver Station STOP operator.

This pilot never commands motors. Stop immediately if a student proposes motor control, field-pose
calculation, pose correction, path choice, guessed configuration values, or a Limelight API/pipeline
assumption. ID-only testing may continue without calibration only when results are plainly labeled
ID-only; metric pose remains blocked.

## Expected evidence by stage

- AV-01: source citations, actual class/API map, two design options, and known unknowns.
- AV-02: reviewed decision document and dependency check.
- AV-03: JDK 17 baseline build, exact local SDK sample/API evidence, and planned files.
- AV-04/05: complete diffs, build results, neutral API proof, unchanged `TeamARobot`, and no direct
  camera/FSM access from the OpMode.
- AV-06: static lifecycle and boundary audit plus implementation-status reconciliation.
- AV-07: table of real facts with sources, units, and state: recorded, verified, or unknown.
- AV-08: expected-versus-observed ID and relative-observation results, stop/shutdown evidence, and
  one next hypothesis if blocked.

## Merge-conflict guidance

Prefer new vision-specific files, the separate Team A pilot composition, and a testing OpMode.
Avoid changing `TeamARobot`, TeleOps, autonomous code, localization/pathing packages, or shared
architecture/status files except when a prompt explicitly and accurately reconciles implementation.
When a later merge needs a shared-document edit, re-read both branches and make one small,
reviewed reconciliation rather than copying either document wholesale.

## Completion review

Accept the pilot only when AV-01 through AV-08 are `Reviewed`, or a final actionable `Blocked`
record exists; implemented reality matches `IMPLEMENTATION_STATUS.md`; the simple Robot remains
camera-free capable; no FTC vision type leaks above the vision boundary; the FSM remains the sole
behavior owner; and no localization or movement claim exceeds the observed evidence.

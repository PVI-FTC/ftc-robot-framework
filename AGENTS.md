# PVI-FTC Repository Instructions
## Project
This repository is based on the official FIRST Tech Challenge FtcRobotController repository. All
PVI-developed Java code belongs under the TeamCode module unless an approved task explicitly
requires another location.
## Source of truth
- Do not rely on previous Codex conversations.
- Treat the checked-out repository, Git history, this file, TeamCode/project-docs/ARCHITECTURE.md,
  and TeamCode/project-docs/IMPLEMENTATION_STATUS.md as the source of truth.
- Inspect actual existing classes and public APIs before editing.
- Do not assume an earlier prompt produced an exact constructor, getter, package, class hierarchy,
  or method signature.
## Architectural dependency direction
OpMode
-&gt; Robot public API
-&gt; Subsystem
-&gt; FSM
-&gt; State
-&gt; Hardware wrapper
-&gt; FTC SDK hardware
TeleOp input:
Gamepad
-&gt; InputManager
-&gt; OpMode mapping
-&gt; Robot public API
Autonomous control:
AutoSequence
-&gt; AutoStep
-&gt; Robot public API
## Required boundaries
- Do not modify official FTC SDK source code unless the assigned task explicitly requires a specific
  change.
- Do not access motors, servos, cameras, or sensors directly from an OpMode.
- Do not call hardwareMap.get(...) from a subsystem, FSM, or state.
- Do not read gamepads from a robot, subsystem, FSM, state, or hardware wrapper.
- Do not manipulate an FSM directly from an OpMode.

PVI-FTC Codex Sequential Repository Build — Master Instructions

PVI-FTC | Editable master guide
- Do not route autonomous behavior through InputManager.
- Do not use Thread.sleep(), blocking loops, or long-running work inside an FTC loop.
- Hardware wrappers contain hardware access but not gamepad, FSM, or autonomous logic.
- Shared packages contain genuinely reusable behavior; team-specific behavior belongs under
  robots.teamA, robots.teamB, or robots.teamC.
## Simplicity and readability
- Keep code understandable to beginning Java students.
- Avoid reflection, dependency injection frameworks, event buses, command queues, concurrency, and
  deep inheritance unless a later approved task demonstrates a need.
- Prefer composition and narrow public APIs.
- Avoid duplicate classes that perform an existing responsibility.
- Do not create Scheduler.java during the baseline prompts; Robot.update() manages subsystem
  lifecycle and AutoSequence manages autonomous steps.
## Before editing
1. Run git status and confirm the working tree is clean.
2. Read this file.
3. Read TeamCode/project-docs/ARCHITECTURE.md.
4. Read TeamCode/project-docs/IMPLEMENTATION_STATUS.md.
5. Inspect relevant source files and recent Git history.
6. Verify prompt prerequisites.
7. Run the documented baseline TeamCode build when feasible.
8. If prerequisites or the baseline build fail, stop and report the issue instead of mixing
   unrelated repair work into the assigned prompt.
## Scope control
- Make only changes required by the assigned prompt.
- Do not perform unrelated cleanup, broad reformatting, package renaming, dependency upgrades, or
  speculative refactoring.
- Preserve existing public APIs unless the task explicitly requires a change.
- If a small API addition is necessary, make the narrowest clear change and document it.
## After editing
1. Build the TeamCode module using the documented command.
2. Run git diff --check.
3. Review the complete diff for unrelated changes.
4. Verify architecture boundaries.
5. Update TeamCode/project-docs/IMPLEMENTATION_STATUS.md.
6. Report files created and modified, APIs added or changed, validation results, assumptions,
   deviations, and remaining TODO items.
7. Do not commit, push, merge, or open a pull request unless the student explicitly requests that
   action after reviewing the branch.
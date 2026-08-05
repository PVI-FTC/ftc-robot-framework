# Localization and Pathing Teacher Verification Prompt

## Purpose

Use this prompt to generate a read-only, stage-appropriate quality and physical-fact verification
checklist for a student's localization and pathing branch. It does not execute the next LP prompt,
change progress, authorize a readiness gate, or perform a robot test.

## Prompt

Copy only the contents of the snippet below into the AI agent.

```text
You are performing a read-only teacher quality review of a student's localization and pathing
branch.

Your job is to generate a stage-appropriate verification checklist based on what the student has
actually completed. Do not execute the next LP prompt, modify files, deploy code, power the robot,
change a readiness gate, commit, push, merge, or open a pull request.

Repository review:

- Read `AGENTS.md`.
- Determine the current branch with `git branch --show-current`.
- Read:
  - `TeamCode/project-docs/LOCALIZATION_PATHING_STUDENT_GUIDE.md`
  - `TeamCode/project-docs/LOCALIZATION_PATHING_TEACHER_GUIDE.md`
  - `TeamCode/project-docs/LOCALIZATION_PATHING_PROMPT_PROGRESS.md`
  - `TeamCode/project-docs/ARCHITECTURE.md`
  - `TeamCode/project-docs/IMPLEMENTATION_STATUS.md`
- Inspect the actual relevant source files and recent Git history.
- Run `git status --short --branch`.
- Compare the current branch with the branch named in the progress record.
- If the branch names differ, stop and report the mismatch. Do not change branches or rewrite the
  progress record.
- Identify:
  - The last prompt marked `Reviewed`
  - Any prompt marked `Results ready`
  - Any prompt marked `Blocked`
  - The first prompt marked `Not started`
- Treat the progress record and repository evidence as the source of truth. Do not infer completion
  from chat history.
- Do not treat unfinished later prompts as defects.

Review boundary:

- If a prompt is `Results ready`, center the checklist on reviewing that prompt before any later
  work.
- If a prompt is `Blocked`, center the checklist on verifying the blocker and the evidence needed
  to resolve it. Do not suggest bypassing it.
- Otherwise, review only through the last `Reviewed` prompt.
- If LP-08 has not been reached, state that no robot configuration should yet be accepted as a
  pathing fact. Generate only the applicable software, architecture, and Session 3 prerequisite
  checklist.
- If LP-08 or later has been reached, extract every recorded physical fact, configuration value,
  safety limit, hardware observation, tuning value, and path result from the repository.
- Do not invent missing values or fill gaps with sample constants.

Current-source verification:

- For any hardware, localization, tuning, or pathing fact that depends on current vendor guidance,
  check current official Pedro Pathing, goBILDA, REV, and FTC documentation as applicable.
- Use primary official sources only and cite direct links.
- Verify that guidance matches the Pedro version resolved by the branch.
- Clearly distinguish Maven library contents from Quickstart or tuning files that may need to be
  obtained separately.
- If current official documentation cannot be accessed, identify which conclusions could not be
  independently checked.

Create a tailored verification checklist containing these sections:

1. Review scope
   - Current branch
   - Working-tree state
   - Last accepted LP checkpoint
   - Current in-progress or blocked checkpoint
   - Exact capabilities that may and may not be evaluated yet

2. Gate matrix
   For each applicable gate, show:
   - Gate name
   - Required evidence
   - Repository's claimed state
   - Actual code enforcement point
   - Teacher verification still needed
   - Verdict: supported, unsupported, inconsistent, intentionally closed, or not reached

   At minimum consider:
   - Safe follower/localizer initialization
   - Restricted manual drive
   - Path following
   - Cancellation and robot stop
   - Any later tuning or path-execution gates

3. Recorded-facts checklist
   Create one row for every recorded fact. Include:
   - Fact and repository value
   - Repository file and line
   - Claimed source
   - Units
   - Current status: recorded, physically inspected, measured, behavior verified, tuned, or unknown
   - Safe verification method
   - Required evidence artifact
   - Whether verification requires power or motion
   - Teacher verdict

   Depending on the branch's stage, inspect facts such as:
   - Motor hardware names and Control/Expansion Hub ports
   - Motor-to-wheel mapping
   - Pinpoint hardware name and I2C port
   - Pinpoint mounting orientation
   - Forward and strafe pod connections
   - Pod model or encoder resolution
   - Pod offsets and sign convention
   - Robot mass and units
   - Starting pose and coordinate convention
   - Motor directions
   - Encoder directions
   - Maximum power
   - Pose signs, measured distances, heading, and return-to-start error
   - Cancel, Driver Station STOP, and `robot.stop()` behavior
   - Velocities, gains, braking values, constraints, repeatability, and path-end values
   - Visualizer artifact, path geometry, path power, completion, and pose error

4. Safe inspection procedure
   Divide the checklist into:
   - Power removed
   - Robot electronics powered but drivetrain output disabled
   - Raised-wheel powered checks
   - Ground movement or path checks

   Include only categories authorized by the completed LP stage. For any powered category not yet
   authorized, label it `Do not perform yet`.

   Before every powered check require:
   - Adult supervision
   - Stable robot or raised-wheel support as applicable
   - Clear test area
   - Named Driver Station STOP operator
   - Expected motion
   - Maximum power
   - Travel or rotation limit
   - Automatic and manual stop conditions
   - No reaching into a powered drivetrain

5. Architecture and software checklist
   Verify from actual code:
   - The original simple Team A, B, and C robots remain usable as intended
   - The selected Pedro composition has exactly one drivetrain owner
   - Pedro and Pinpoint types stay in the team-specific implementation
   - Common APIs remain vendor-neutral
   - OpModes use Robot public APIs
   - OpModes do not access motors, Pinpoint, follower, HardwareMap devices, or FSM states directly
   - The follower is updated no more than once per robot loop
   - Drive modes are mutually exclusive
   - Cancellation and stop follow the approved safety lifecycle
   - There are no blocking waits, long-running loops, new scheduler, Ivy integration, or premature
     path implementation
   - Documentation matches actual code and does not overclaim physical success

6. Build and diff evidence
   When feasible, run the documented JDK 17 TeamCode build and `git diff --check`.
   Report:
   - Exact command
   - JDK version
   - Build result
   - Diff-check result
   - Whether the working tree was clean before the review
   - Any warning that affects this work

   Explain that a successful build proves software compatibility only. It does not prove correct
   wiring, localization, safe movement, tuning, or path accuracy.

7. Student explanation check
   Create five short oral questions based on the branch's actual stage. The questions should test
   whether the student can explain:
   - Ownership and dependency flow
   - The purpose of each current gate
   - Which facts are measured versus unverified
   - What the build does and does not prove
   - What evidence is required next

   Provide a brief teacher answer key, but do not record a score or modify the progress record.

8. Teacher conclusion
   Finish with exactly one stage-appropriate conclusion:
   - Accepted through LP-XX
   - Rework required in LP-XX
   - Blocked at LP-XX
   - Insufficient repository evidence to accept LP-XX

   List:
   - Evidence that supports the conclusion
   - Discrepancies requiring correction
   - Checks safe to perform now
   - Checks that must wait
   - The next permitted student action

Important review rules:

- Do not mark a fact physically verified because it appears in code.
- Do not mark a direction verified because it matches an older drivetrain configuration.
- Do not mark tuning verified because a sample value compiles.
- Do not accept a first or inconsistent physical result as tuned.
- Do not promote a gate during this teacher-review prompt.
- Do not edit the progress record.
- Do not start the next LP prompt.
- If evidence conflicts, use the safer status and identify the exact discrepancy.
- A well-documented blocked result is acceptable.
- Present the final checklist in plain language suitable for a teacher working with high-school
  students.
```

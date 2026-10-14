# AprilTag Localization and Alignment Student Guide

## Start Here: copy this prompt into your AI Agent

Copy only the contents of the snippet below. Stop at the closing snippet marker; do not copy the
`Purpose` section or anything after it.

```text
- Read `TeamCode/project-docs/APRILTAG_LOCALIZATION_ALIGNMENT_STUDENT_GUIDE.md` completely.
- Do not create or switch branches. Run `git branch --show-current`, show the exact result, and ask
  whether this is the intended branch for all AprilTag localization and alignment work. Wait.
- After confirmation, read `APRILTAG_LOCALIZATION_ALIGNMENT_PROMPT_PROGRESS.md`. If its working
  branch is `UNCONFIRMED`, update only that field. If it differs, stop for student/teacher
  resolution.
- Use the progress record, repository, Git history, and cited evidence as the source of truth. Do
  not infer completion from chat memory. Resume `Results ready` at review and do not advance a
  `Blocked` prompt.
- Before each prompt, explain what will be inspected, researched, decided, changed, and produced.
  Say whether changes are allowed. Ask whether the student understands and wants to proceed; wait.
- Execute only the first prompt not marked `Reviewed`. Never run the next prompt automatically.
- Present beginner-friendly results. Separate repository facts, official guidance,
  recommendations, software-only checks, physical checks, open decisions, and future integration
  concerns.
- After results, ask five new true/false or multiple-choice learning questions based on that work.
  Ask one question at a time and wait. Explain an incorrect answer plainly. Do not score, save, or
  reuse questions. All five attempts are required before the prompt can become `Reviewed`.
- Then separate questions into `Required before the next prompt` and `Can wait until later`. Ask
  one required engineering question at a time. A quiz answer is not an engineering decision.
- Immediately before results, update only the prompt row to `Results ready`, or `Blocked` if work
  cannot continue. Mark it `Reviewed` only in a later turn after explicit student acceptance.
- When LA-06, LA-09, or LA-10 is next, show the complete matching STOP warning in this guide and
  obtain the student's current confirmation before previewing that prompt.
```

## Purpose

Stage 5 advances the reviewed Logitech C920 AprilTag pilot from validated, robot-relative
observations toward two distinct capabilities:

1. **Localization:** estimate the robot's position and heading on the field from a known tag pose.
2. **Alignment:** cautiously command the drivetrain toward a reviewed target-relative offset.

These are separate responsibilities. A tag-relative observation is not automatically a field pose,
and a field-pose estimate does not authorize movement. Stage 5 adds each boundary in small steps,
validates it independently, and keeps drivetrain output disabled until the later supervised prompts.

Read these records with this guide:

- `APRILTAG_METRIC_OBSERVATION_PROMPT_PROGRESS.md`
- `APRILTAG_VISION_ARCHITECTURE_DECISION.md`
- `APRILTAG_VISION_PHYSICAL_TEST_EVIDENCE.md`
- `ARCHITECTURE.md`
- `IMPLEMENTATION_STATUS.md`

Stage 4 is complete. Corrected tape-measure tests at 33, 48, and 66 inches support the FTC SDK's
built-in C920 640-by-480 calibration; neither the experimental Zephyr calibration nor a constant
range correction was adopted. Stage 5 must still treat pose values as observations with explicit
freshness and quality gates.

This guide does not authorize Limelight support, odometry fusion, Pedro integration, autonomous
path selection, or general camera-profile infrastructure. Those require separate reviewed work.

## Coordinate and ownership rules to preserve

- VisionPortal and FTC AprilTag types stay below `VisionHardware`.
- `VisionSubsystem` remains the owner of vision lifecycle and observation state.
- Robot public APIs expose neutral immutable values, never FTC detection objects.
- The reviewed robot frame is +X right, +Y forward, +Z up.
- Field axes, origin, heading convention, and official tag field poses must be verified before any
  field transform is implemented; do not guess them.
- Localization consumes observations and produces estimates. It does not command motors.
- Alignment consumes a reviewed target-relative error and requests bounded drive behavior through
  the Robot public API. It does not manipulate a drive FSM or hardware directly.
- Retained, stale, unavailable, wrong-ID, wrong-metadata, or non-finite observations cannot produce
  a new localization correction or alignment command.

## Stage 5 sessions

### Session 1 — Evidence and architecture

- LA-01: inspect actual field-pose, transform, localization, and drive-control seams.
- LA-02: approve coordinate conventions, ownership, quality gates, and the smallest file plan.

Checkpoint: students can distinguish camera-relative pose, robot-relative pose, field pose,
localization estimate, target error, and motor command.

### Session 2 — Pure localization candidate

- LA-03: add verified field-tag metadata and pure transform math.
- LA-04: add a neutral candidate/estimate boundary with freshness and quality rejection.
- LA-05: perform the software-only localization audit.

Checkpoint: software can calculate and explain a candidate robot field pose without writing to a
drivetrain, autonomous path, or external localization provider.

### Session 3 — Stationary localization validation

> **STOP — Minimum physical equipment required for LA-06**
>
> Do not begin LA-06 without the reviewed C920 and mount, explicit 640-by-480 stream, matching FTC
> calibration evidence, verified official tag identity/size/field pose, a powered Control Hub and
> Driver Station, deployment access, a flat measured test area, tape measure, marked field axes,
> angle-measurement tool, secure robot and tag, good lighting, adult supervision, and a named
> Driver Station STOP operator. Drive motors must be physically disconnected or equivalently
> prevented from producing movement by a reviewed method.
>
> Stop for any calibration warning, wrong tag or field metadata, stale-as-fresh data, non-finite
> result, wrong axis/sign, unexplained jump, camera error, movement, loss of supervision, or loss of
> STOP control. Do not tune multiple causes at once and do not enable alignment.

- LA-06: validate field-pose candidates at measured stationary placements.

Checkpoint: the team has accepted stationary position/heading evidence and limitations, or has one
documented blocker and testable hypothesis. This still does not authorize movement.

### Session 4 — Alignment design and software-only implementation

- LA-07: approve a target-relative alignment controller and safety envelope without coding it.
- LA-08: implement bounded alignment requests with output disabled by default and test the math.

Checkpoint: students can explain deadbands, clamps, timeouts, cancellation, loss behavior, and why
software-only controller output is not proof of safe robot movement.

### Session 5 — Supervised alignment validation

> **STOP — Minimum physical equipment required for LA-09**
>
> Do not begin LA-09 without all LA-06 evidence reviewed, LA-08 software checks passing, the robot
> raised securely so every drive wheel is clear of people and objects, a low approved power clamp,
> adult supervision, and a named STOP operator with immediate Driver Station control. Confirm wheel
> clearance, robot stability, motor directions, cancellation, timeout, and tag-loss behavior before
> enabling output. Stop for any unexpected wheel direction, acceleration, oscillation, stale data,
> failure to cancel, resource error, or loss of supervision.

- LA-09: validate signs, clamps, cancellation, timeout, and loss behavior with wheels raised.

> **STOP — Minimum physical equipment required for LA-10**
>
> Do not begin LA-10 until LA-09 is reviewed. Use a clear bounded floor area, the lowest reviewed
> useful speed, secure camera and tag, measured starting offset, adult supervision, a spotter, and a
> named STOP operator. Test one axis or correction at a time before combined alignment. Stop for
> wrong direction, overshoot beyond the agreed limit, oscillation, failure to stop in the deadband,
> tag loss without immediate safe cancellation, obstacle entry, or any unexpected behavior.

- LA-10: perform cautious floor alignment validation; no autonomous path integration.

Checkpoint: the team has accepted a narrow alignment capability and its limits, or recorded a
blocked result. Localization fusion and autonomous path selection remain separate future work.

## Prompt register

### LA-01 v1.0 — Verify actual localization and alignment seams

> - Read-only except for the required progress-row update.
> - Inspect `AGENTS.md`, Stage 4 records, current vision model/source/subsystem/Robot/diagnostic,
>   drive subsystem/FSM/Robot APIs, autonomous boundaries, localization/pathing documents, actual
>   SDK 11.2.1 artifacts, and recent Git history.
> - Run the JDK 17 TeamCode baseline build; stop if it fails.
> - Research current official FTC documentation for AprilTag reference frames, tag metadata and
>   field locations, `ftcPose`, freshness, camera calibration, and coordinate conventions.
> - Inventory every existing localization provider or field-pose API. Identify ownership conflicts
>   and prove whether none exists.
> - Trace the safe control path from an alignment decision through Robot public API to the drive
>   subsystem, FSM, state, hardware wrapper, and motors. Do not add code.
> - Produce terminology, transform inputs, trusted/untrusted evidence, exact planned files, and
>   open decisions. Stop for review.

Evidence to save:

- Actual APIs and official citations.
- Observation/localization/alignment responsibility table.
- Existing ownership and prohibited-coupling audit.
- Exact file plan and unresolved decisions.

### LA-02 v1.0 — Approve the Stage 5 design

> - Do not implement production code.
> - Define and diagram the complete transform chain from field tag pose and fresh camera detection
>   through camera mount to robot field-pose candidate. Specify units, axes, handedness, heading
>   sign, transform direction, and composition order.
> - Define neutral immutable field-pose candidate and quality/rejection semantics.
> - Decide whether Stage 5 exposes independent candidates or a minimal estimator. Do not add
>   odometry fusion merely because it may be useful later.
> - Define acceptance gates for freshness, age, tag identity, metadata, calibration, finiteness,
>   ambiguity/quality evidence actually available in this SDK, and physical validation.
> - Define the alignment responsibility boundary without controller constants or motor output.
> - Preserve one owner for vision lifecycle and one owner for drive behavior. Approve the smallest
>   beginner-readable file/API plan and update only the architecture decision if necessary.

Evidence to save:

- Approved transform and coordinate table.
- Candidate acceptance/rejection policy.
- Ownership diagram and file/API plan.
- Explicit deferred features.

### LA-03 v1.0 — Add verified field-tag metadata and pure transforms

> - Run the baseline build and inspect actual APIs before editing.
> - Add only the reviewed neutral field-tag pose representation and pure transform code.
> - Enter no field coordinate from memory. Cite the current official season source and preserve the
>   source's tag identity, units, origin, axes, and orientation.
> - Keep transform functions deterministic and independent of FTC hardware, Robot lifecycle,
>   localization providers, drivetrain, and gamepads.
> - Add focused checks for identity, zero/known transforms, inverse direction, signs, units,
>   rotation composition, and non-finite rejection.
> - Do not write a localization estimate or command movement. Build, run `git diff --check`, inspect
>   the complete diff, and update `IMPLEMENTATION_STATUS.md`; stop for review.

### LA-04 v1.0 — Add neutral field-pose candidates

> - Implement the LA-02 candidate boundary using only fresh, accepted neutral observations.
> - Preserve the original acquisition timestamp and expose age/quality/rejection reason.
> - Reject retained, stale, unavailable, wrong-ID, missing-metadata, uncalibrated, and non-finite
>   observations. Do not silently reuse the previous candidate as a new estimate.
> - Keep candidate calculation separate from VisionSubsystem state transitions and drive behavior.
> - Extend only the stationary diagnostic needed to display candidate pose and rejection reason.
> - Do not write Pedro, Pinpoint, odometry, autonomous, or drivetrain state. Build and audit.

### LA-05 v1.0 — Software-only localization audit

> - Change production behavior only for a focused defect introduced by LA-03 or LA-04.
> - Run the Java 17 build, focused checks, `git diff --check`, and complete-diff review.
> - Audit transform direction, axes, units, signs, rotation order, timestamps, rejection gates,
>   immutability, lifecycle, and public boundaries.
> - Search for motor requests, drive-FSM manipulation, localization-provider writes, autonomous
>   decisions, stale-data acceptance, Limelight code, blocking waits, or FTC-type leakage.
> - Reconcile documentation and list every physical prerequisite for LA-06; stop for review.

### LA-06 v1.0 — Supervised stationary field-pose validation

Use only after LA-05 is reviewed and the complete LA-06 STOP warning is confirmed:

> - Reconfirm every physical prerequisite one at a time; mark `Blocked` if any is unavailable.
> - Choose measured stationary field poses and acceptance tolerances before viewing results.
> - Deploy only the narrow diagnostic with drivetrain output physically prevented.
> - Record expected and observed X, Y, heading, tag ID, timestamp/age, quality, rejection reason,
>   repeatability, distance, angle, lighting, and tag visibility at each placement.
> - Include centered and off-axis observations and tag loss/reacquisition. Do not average away a
>   failed placement or change several variables at once.
> - Stop for any STOP condition. Do not enable alignment or pathing.
> - Record evidence, run the final build and diff check, and stop for review.

### LA-07 v1.0 — Approve the target-relative alignment controller

> - Do not implement production code.
> - Select the exact alignment goal: tag ID, desired forward/lateral offset, and heading. A quiz
>   response is not this engineering decision.
> - Define independent forward, strafe, and turn errors, signs, deadbands, low output clamps,
>   minimum useful output if needed, settle time, overall timeout, cancellation, and tag-loss rule.
> - Require fresh accepted observations and explicit operator authorization. Default output is off.
> - Define how alignment requests use Robot public APIs without bypassing the drive FSM or creating
>   two motor owners.
> - Define software tests, raised-wheel checks, and floor-test acceptance criteria. Stop for review.

### LA-08 v1.0 — Implement bounded alignment requests, disabled by default

> - Run the baseline build and inspect actual drive/Robot APIs before editing.
> - Implement the reviewed pure alignment-error/controller math and narrow Robot/subsystem request
>   boundary. Keep it disabled unless the explicit test OpMode authorizes it.
> - Clamp every output, require fresh accepted data, cancel on loss/staleness/timeout/STOP, and
>   request zero output before leaving alignment behavior.
> - Do not add autonomous path selection, field-pose fusion, or direct hardware access.
> - Add software checks for signs, deadbands, clamps, settle time, timeout, loss, cancellation, and
>   unavailable observations. Build, diff-check, audit ownership, and stop for review.

### LA-09 v1.0 — Supervised raised-wheel alignment validation

Use only after LA-08 is reviewed and the complete LA-09 STOP warning is confirmed:

> - Reconfirm every STOP prerequisite and the preselected output limits.
> - With wheels securely raised, validate one axis at a time, then the approved combination.
> - Verify motor directions, clamps, deadbands, settle behavior, timeout, manual cancellation,
>   tag-loss cancellation, STOP, and restart/resource lifecycle.
> - Stop immediately for any wrong direction, unstable behavior, or failed safety action.
> - Record raw evidence and limitations. Passing raised-wheel checks does not authorize floor use
>   until the result is reviewed and LA-10 is separately confirmed.

### LA-10 v1.0 — Supervised low-speed floor alignment validation

Use only after LA-09 is reviewed and the complete LA-10 STOP warning is confirmed:

> - Reconfirm every STOP prerequisite, starting pose, goal, bounds, and acceptance tolerance.
> - Begin with one correction axis and the lowest reviewed useful speed. Expand only after each
>   prior behavior stops correctly and is accepted.
> - Record start/end range, bearing, heading error, elapsed time, overshoot, settling, repeatability,
>   loss/cancellation, STOP, lighting, and tag visibility.
> - Do not select autonomous paths or claim general localization accuracy from this narrow test.
> - Stop for any STOP condition. Record results, build, diff-check, and stop for review.

## Working rules

- Use the approved JDK 17 and never weaken workstation security.
- Build before and after every code-changing prompt and run `git diff --check` after edits.
- Inspect actual checked-out APIs before editing; do not trust planned signatures.
- Keep changes small, neutral, immutable, and understandable to beginning Java students.
- Never invent tag field poses, axes, units, transforms, tolerances, controller constants, hardware
  facts, measurements, or physical results.
- Do not commit, push, merge, or open a pull request until the student reviews the complete diff.
- A future Limelight source must repeat identity, configuration, coordinate, freshness, mounting,
  quality, and physical-validation work. It is not authorized by this guide.

## Official starting references

At execution time, re-check current official FTC documentation instead of treating these links as
frozen API evidence:

- AprilTag reference frame:
  `https://ftc-docs.firstinspires.org/en/latest/apriltag/vision_portal/apriltag_reference_frame/apriltag-reference-frame.html`
- AprilTag metadata:
  `https://ftc-docs.firstinspires.org/en/latest/apriltag/vision_portal/apriltag_metadata/apriltag-metadata.html`
- Detection values:
  `https://ftc-docs.firstinspires.org/en/latest/apriltag/understanding_apriltag_detection_values/understanding-apriltag-detection-values.html`
- Camera calibration:
  `https://ftc-docs.firstinspires.org/en/latest/apriltag/vision_portal/apriltag_camera_calibration/apriltag-camera-calibration.html`

## Prompt change log

| Date | Prompt | Version | Change |
| --- | --- | --- | --- |
| 2026-09-24 | Guide | 1.0 | Added the Stage 5 localization and alignment workflow. |
| 2026-09-24 | LA-01 through LA-10 | 1.0 | Added research, design, pure transforms, candidate localization, software audit, stationary validation, alignment design/implementation, and supervised movement gates. |

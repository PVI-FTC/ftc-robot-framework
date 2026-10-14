# AprilTag Vision Student Guide

## Start Here: copy this prompt into your AI Agent

```text
- Read `TeamCode/project-docs/APRILTAG_VISION_STUDENT_GUIDE.md`.
- Do not create or switch branches. Run `git branch --show-current`, show the exact result, and
  ask whether this is the intended branch for all AprilTag vision work. Wait for the answer.
- After confirmation, read `APRILTAG_VISION_PROMPT_PROGRESS.md`. If its working branch is
  `UNCONFIRMED`, update only that field. If it differs, stop for student/teacher resolution.
- Use the progress record and cited repository evidence, not chat memory. Resume `Results ready`
  at review; do not advance a `Blocked` prompt.
- Before each prompt, explain what will be inspected, researched, decided, changed, and produced.
  Say whether changes are allowed. Ask whether the student understands and wants to proceed; wait.
- Execute only the first prompt not marked `Reviewed`. Never run the next prompt automatically.
- Present beginner-friendly results. Separate repository facts, official guidance, recommendations,
  software-only checks, physical checks, open decisions, and future integration concerns.
- After results, ask five new true/false or multiple-choice learning questions based on that work.
  Ask one question at a time and wait. Explain an incorrect answer plainly. Do not score, save, or
  reuse questions. All five attempts are required before the prompt can become `Reviewed`.
- Then separate questions into `Required before the next prompt` and `Can wait until later`.
  Ask one required engineering question at a time. A quiz answer is not an engineering decision.
- Immediately before results, update only the prompt row to `Results ready`, or `Blocked` if work
  cannot continue. Mark it `Reviewed` only in a later turn after explicit student acceptance.
- When AV-07 is next, show the complete Session 3 STOP warning in this guide and obtain the
  student's current confirmation before previewing AV-07.
```

## Purpose

Students add an optional AprilTag detection and observation pilot without receiving a packaged
solution. The pilot reports tag identity and robot-relative observations only. It does not update
localization, calculate a field pose, select a path, or command drivetrain movement.

Read `APRILTAG_VISION_ARCHITECTURE_DECISION.md` with this guide. It is the approved design intent;
`IMPLEMENTATION_STATUS.md` remains the record of actual implemented behavior.

## Three sessions

### Session 1 — Understand and decide (60 minutes)

- AV-01, about 25 minutes: inspect the existing vision FSM and research official FTC guidance.
- AV-02, about 25 minutes: review and record the approved pilot architecture.
- Keep about 10 minutes for student review and decisions.

Checkpoint: students can explain why vision is optional, where the FSM lives, why observations do
not change robot pose, and why Logitech/VisionPortal and Limelight need separate hardware sources.

### Session 2 — Implement and inspect without hardware (60 minutes)

- AV-03, about 10 minutes: inspect current SDK samples/APIs and run the baseline build.
- AV-04, about 25 minutes: add the optional neutral observation boundary.
- AV-05, about 18 minutes: add the Logitech pilot Robot composition and testing OpMode.
- AV-06, about 7 minutes: build, inspect boundaries, and reconcile implementation status.

Checkpoint: a compiling Logitech-ready pilot with no motor, localization, Pedro, or field-pose
behavior; missing camera hardware remains safe.

### Session 3 — Stationary supervised validation (60 minutes)

> **STOP — Minimum physical equipment required for Session 3**
>
> Do not begin AV-07 unless the team has a powered Control Hub or supported RC phone, compatible
> Driver Station and deployment access, one securely mounted camera, its real configured hardware
> name, a known flat 36h11 AprilTag with known size/library provenance, a clear well-lit test area,
> a way to measure approximate camera-to-tag distance and left/right position, and adult
> supervision. For metric robot-relative observations, the team must also measure the lens center
> and orientation relative to its chosen robot origin and have calibration evidence for the chosen
> camera/resolution. A named Driver Station STOP operator is required.
>
> A Logitech/UVC camera uses VisionPortal. A Limelight 3A is not a VisionPortal camera and may not
> substitute for the Logitech pilot without its separately reviewed source implementation. Do not
> invent hardware names, camera mount values, tag sizes, calibration values, or Limelight pipeline
> settings. The pilot must not command drivetrain motors.

- AV-07, about 15 minutes: record real camera, tag, calibration, mount, and safety facts.
- AV-08, about 35 minutes: validate stationary detection, observation signs, loss/reacquisition,
  and safe portal shutdown.
- Keep about 10 minutes for evidence and status reconciliation.

## Prompt register

### AV-01 — Architecture and observation discovery

> - Read-only except for the required progress-row status update.
> - Inspect `AGENTS.md`, `ARCHITECTURE.md`, `IMPLEMENTATION_STATUS.md`, the current vision states,
>   `VisionSubsystem`, `VisionHardware`, `RobotHardware`, Team A/B/C Robots, relevant OpModes,
>   public APIs, current SDK samples, and recent Git history.
> - Research current official FTC guidance for VisionPortal, AprilTagProcessor, supported UVC
>   cameras, calibration, resource lifecycle, and Limelight interoperability.
> - Reconstruct the current FSM and dependency flow. Compare extending `TeamARobot` with a separate
>   vision-only Robot composition. Explain why the latter minimizes risk and merge conflicts.
> - Identify a Logitech-first, Limelight-ready hardware-source seam that does not create another
>   FSM and does not expose FTC types above the hardware boundary.
> - State the observation-only goal, prohibited behavior, physical unknowns, and recommendation.
> - Cite repository files and official sources; stop for review.

Evidence: current flow, two design options, source boundary, official citations, and open questions.

### AV-02 — Review the approved vision architecture

> - Do not implement AprilTag code.
> - Reinspect AV-01 evidence and `APRILTAG_VISION_ARCHITECTURE_DECISION.md`.
> - Update only that decision document if student/teacher review identifies a necessary correction.
> - Preserve the existing VisionSubsystem FSM as behavior owner, the optional camera policy, the
>   separate Team A pilot composition, neutral observations, and the Logitech-first/Limelight-later
>   rule. Do not modify shared localization/pathing documents.
> - Show the complete diff and run `git diff --check`; stop for review.

### AV-03 — Verify SDK APIs and baseline

> - Read-only except for the required progress-row update.
> - Inspect the checked-out FTC AprilTag/VisionPortal and Limelight samples plus actual APIs. Do not
>   rely on another SDK version or prior chat.
> - Run `./gradlew.bat TeamCode:assembleDebug` in Windows PowerShell with the team-approved JDK 17.
> - Confirm that VisionPortal/AprilTag support is already supplied by the FTC SDK and no dependency
>   addition is required. List exact planned files and the physical facts AV-07 must obtain.
> - Stop if the baseline build fails; do not mix repairs into vision work.

### AV-04 — Add optional neutral observation boundary

> - Run the baseline build before editing.
> - Make the smallest beginner-readable changes to let `VisionHardware` obtain neutral AprilTag
>   observations and let `VisionSubsystem` own their FSM-driven lifecycle.
> - Keep FTC VisionPortal, AprilTag, webcam, and future Limelight types below `VisionHardware`.
> - Preserve the current FSM states and public enable/disable behavior. Hardware sources must not
>   choose states. Missing camera must be safe and unavailable.
> - Define a documented robot frame and neutral observation model, but do not invent mount or
>   calibration values. Do not add field pose, localization, Pedro, drivetrain, or auto logic.
> - Build, run `git diff --check`, inspect the complete diff, and update `IMPLEMENTATION_STATUS.md`.

### AV-05 — Add the Logitech pilot composition and testing OpMode

> - Inspect actual APIs and run the baseline build before editing.
> - Add a separate Team A AprilTag-vision Robot composition and a narrow testing OpMode. Leave
>   `TeamARobot`, TeleOps, autonomous OpModes, drive code, and localization code unchanged.
> - Implement only the Logitech/UVC VisionPortal source. Keep the internal source seam ready for a
>   later Limelight adapter, but do not implement, configure, or guess Limelight behavior now.
> - The OpMode uses only Robot public APIs, calls `robot.update()` once per loop, displays neutral
>   observations, and calls `robot.stop()` on stop. It must not access camera hardware or an FSM.
> - Build, run `git diff --check`, inspect the full diff, and update `IMPLEMENTATION_STATUS.md`.

### AV-06 — Session 2 software-only review

> - Do not change production behavior unless a focused review finds a defect caused directly by
>   AV-04 or AV-05.
> - Verify the vision FSM remains sole behavior owner; one source is active; the source is updated
>   once per Robot loop; stop releases camera resources; simple Robots work without cameras; and no
>   FTC vision types leak through public Robot APIs.
> - Verify no field pose, localization correction, Pedro dependency, motor request, or autonomous
>   path behavior exists. Run the build and `git diff --check` and reconcile status documentation.

### AV-07 — Record real equipment and open stationary test gate

> - Reconfirm every Session 3 STOP requirement one at a time. Mark Blocked if any is unavailable.
> - Record actual camera model, configured name, connection, resolution, calibration source/status,
>   tag family/library/size, robot-origin convention, measured lens position/orientation, lighting,
>   supervision, and STOP operator. Record the source of every fact.
> - Do not guess. A missing calibration or camera-mount fact may permit ID-only validation but keeps
>   metric robot-relative observation validation closed.
> - Do not power or command the drivetrain. Build and update only the configuration/evidence needed
>   for safe stationary observation testing.

### AV-08 — Supervised stationary detection validation

> - Begin with drivetrain disabled and reconfirm the STOP plan.
> - Deploy the narrow diagnostic. Validate camera state, visible tag ID, detection count,
>   loss/reacquisition, timestamp/age, expected range and bearing signs, and approximate measured
>   positions. Check safe Robot stop and camera-resource shutdown.
> - Change one cause at a time. Stop for unavailable camera, incorrect identity, unstable results,
>   unknown calibration/mount facts, or any unexpected behavior. A documented Blocked result is
>   valid.
> - Do not calculate or write a robot field pose and do not command movement. Record observed facts,
>   build results, and remaining work in `IMPLEMENTATION_STATUS.md`.

## Working rules

- Use the student's normal account; do not run Android Studio, PowerShell, or Gradle as
  Administrator or weaken system security.
- Use the approved JDK 17. If build setup fails, record the complete error and ask teacher/IT.
- Build before and after each code-changing prompt and run `git diff --check` after edits.
- Do not commit, push, merge, or open a pull request until the team reviews the full diff.
- A later Limelight prompt must re-check the exact Limelight model and current official FTC API;
  it is not authorized by this Logitech pilot.

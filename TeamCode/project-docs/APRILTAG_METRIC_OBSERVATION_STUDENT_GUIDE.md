# AprilTag Metric Observation Student Guide

## Start Here: copy this prompt into your AI Agent

Copy only the contents of the snippet below. Stop at the closing snippet marker; do not copy the
`Purpose` section or anything after it.

```text
- Read `TeamCode/project-docs/APRILTAG_METRIC_OBSERVATION_STUDENT_GUIDE.md` completely.
- Do not create or switch branches. Run `git branch --show-current`, show the exact result, and ask
  whether this is the intended branch for all AprilTag metric-observation work. Wait for the answer.
- After confirmation, read `APRILTAG_METRIC_OBSERVATION_PROMPT_PROGRESS.md`. If its working branch
  is `UNCONFIRMED`, update only that field. If it differs, stop for student/teacher resolution.
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
- When MV-06 is next, show the complete Stage 4 physical STOP warning in this guide and obtain the
  student's current confirmation before previewing MV-06.
```

## Purpose

Stage 4 advances the reviewed Logitech C920 pilot from ID-only detections to experimental,
stationary metric observations. Students first correct observation freshness, lock the verified
640-by-480 stream, verify DECODE tag 22 metadata, and preserve the neutral observation boundary.
Only then may they compare reported range and bearing with physical measurements.

This guide does not authorize localization, robot field pose, pose fusion, path selection,
drivetrain commands, autonomous behavior, or Limelight support. Metric output remains experimental
until the complete Stage 4 evidence is reviewed.

Read these records with this guide:

- `APRILTAG_VISION_ARCHITECTURE_DECISION.md`
- `APRILTAG_VISION_PHYSICAL_TEST_EVIDENCE.md`
- `APRILTAG_VISION_PROMPT_PROGRESS.md`
- `IMPLEMENTATION_STATUS.md`

The completed AV-01 through AV-08 sequence remains unchanged. This guide has its own progress
record so another student can continue Stage 4 from repository evidence rather than chat history.

## Current entry evidence

- Camera: Logitech C920.
- Hardware identity: `USB\VID_046D&PID_082D&MI_00`.
- Configured FTC name: `logitechVisionWebcam`.
- Observed VisionPortal stream: 640 by 480 pixels.
- FTC calibration warning: none observed during the recorded run.
- Calibration evidence: the VID/PID and resolution match the SDK's published built-in C920
  calibration.
- Tag: official DECODE 36h11 tag 22 with a measured 6.5-inch black square.
- Robot frame: origin at drivetrain center of rotation projected onto the floor; +X right,
  +Y forward, +Z up.
- Measured lens center: X +6.875 inches, Y +4.875 inches, Z +19 inches.
- Measured nominal camera orientation: yaw 0 degrees, pitch 0 degrees, roll 0 degrees.
- Known defect: the current source assigns `System.nanoTime()` while copying detections, so stale
  results can appear newly acquired. Metric observations must remain closed until corrected.

## Stage 4 sessions

### Session 1 — Verify APIs and approve the metric boundary

- MV-01: inspect actual SDK 11.2.1 freshness, timestamp, calibration, metadata, and pose APIs.
- MV-02: approve the smallest experimental metric-observation design.

Checkpoint: students can explain frame acquisition time, stale versus fresh detections, why
resolution and calibration must match, how tag size affects pose, and why an observation is not a
robot field pose.

### Session 2 — Implement and audit without moving the robot

- MV-03: correct freshness and explicitly configure 640 by 480.
- MV-04: verify tag 22 metadata and add experimental neutral metric observations.
- MV-05: perform the software-only Stage 4 audit.

Checkpoint: compiling experimental metric observations preserve FTC timestamps, cannot make stale
frames look fresh, keep FTC types below `VisionHardware`, and have no localization or movement.

### Session 3 — Supervised stationary range/bearing validation

> **STOP — Minimum physical equipment required for Stage 4 validation**
>
> Do not begin MV-06 unless the team has the reviewed Logitech C920 with hardware identity
> `USB\VID_046D&PID_082D&MI_00`, active FTC name `logitechVisionWebcam`, an explicitly configured
> 640-by-480 stream, recorded matching calibration evidence, the official flat DECODE 36h11 tag 22
> with a verified 6.5-inch black square, the reviewed camera-mount measurements, a powered Control
> Hub, compatible Driver Station, deployment access, a clear well-lit stationary test area, a tape
> measure, marked centerline, angle-measurement tool, adult supervision, and a named Driver Station
> STOP operator. Drive motors must be physically disconnected or equivalently prevented from
> producing movement by a reviewed method.
>
> Stop immediately for a calibration warning, camera error, incorrect tag identity, stale data
> reported as fresh, non-finite metric values, wrong range/bearing signs, unstable results,
> unexpected motion, or any loss of supervision or STOP control. Do not tune several causes at
> once. Do not calculate a robot field pose and do not command drivetrain motors.

- MV-06: validate experimental range, bearing, freshness, loss/reacquisition, and shutdown while
  stationary.

Checkpoint: the team has either reviewed repeatable stationary metric evidence or a documented
blocked result with one next hypothesis. Neither outcome authorizes localization.

## Prompt register

### MV-01 v1.0 — Verify actual SDK metric and freshness APIs

> - Read-only except for the required progress-row update.
> - Confirm the branch and progress record, then inspect `AGENTS.md`, the Stage 4 entry records,
>   actual AprilTag source/model/subsystem/Robot/OpMode code, SDK samples, supplied SDK artifacts,
>   and recent Git history.
> - Run the documented TeamCode baseline with the approved JDK 17. Stop if it fails.
> - Research current official FTC documentation for `AprilTagDetection.frameAcquisitionNanoTime`,
>   `AprilTagProcessor.getDetections()`, `getFreshDetections()`, camera calibration, VisionPortal
>   resolution selection, AprilTag libraries/metadata, `ftcPose`, and FTC reference frames.
> - Inspect the actual checked-out SDK 11.2.1 APIs; do not rely on signatures from another release.
> - Explain whether `getFreshDetections()` returns null or an empty list when no new frame exists,
>   whether detection lists are snapshots, and what timestamp survives each API. Cite evidence.
> - Verify the repository evidence for C920 VID/PID `046D:082D`, 640-by-480 built-in calibration,
>   no observed warning, tag 22 family/size, and measured mount. Mark any mismatch `Blocked`.
> - Reconstruct the current stale-data failure and list focused tests that prove old detections do
>   not receive a new acquisition time.
> - Produce exact planned files and open decisions; do not implement code. Stop for review.

Evidence to save:

- Actual SDK method/field signatures and official citations.
- Fresh, empty, retained-snapshot, and loss behavior table.
- Verified calibration, metadata, frame, and mount facts.
- Exact file plan and unresolved questions.

### MV-02 v1.0 — Approve the experimental metric-observation design

> - Do not implement production code.
> - Reinspect MV-01 evidence and `APRILTAG_VISION_ARCHITECTURE_DECISION.md`.
> - Define beginner-readable semantics for fresh detections and an optional retained latest
>   snapshot. A retained observation must keep its original acquisition timestamp and age.
> - Decide how an active loop with no newly processed frame differs from a newly processed frame
>   with zero detections. Do not silently treat both as the same event if the SDK distinguishes them.
> - Preserve one `VisionSubsystem` FSM, one selected source, and one source update per Robot loop.
> - Keep VisionPortal, FTC detections, FTC pose, tag-library, calibration, and camera types below
>   `VisionHardware`. Public Robot APIs continue returning neutral immutable observations.
> - Define the transform flow only as `FTC camera-relative pose -> documented neutral camera frame
>   -> measured robot frame`. Do not introduce field-tag locations or calculate a robot field pose.
> - Define when `poseAvailable` must be false: missing/wrong metadata, missing calibration evidence,
>   stale or invalid source data under the approved policy, non-finite values, or unverified frame
>   conversion.
> - Update only `APRILTAG_VISION_ARCHITECTURE_DECISION.md` if review requires a real correction or
>   clarification. Show the full diff and run `git diff --check`; stop for review.

Evidence to save:

- Approved freshness/snapshot semantics.
- Approved frame-transform flow and pose-availability gates.
- Proof that localization and movement remain prohibited.

### MV-03 v1.0 — Correct freshness and lock the C920 stream

> - Inspect actual APIs and run the baseline build before editing.
> - Make the smallest change to preserve `frameAcquisitionNanoTime` in every neutral observation.
>   Never replace a camera acquisition timestamp with the Robot-loop time.
> - Implement the reviewed `getFreshDetections()`/latest-snapshot policy from MV-02. Do not let a
>   stale detection appear newly acquired on every loop. Keep collection snapshots immutable.
> - Replace the easy VisionPortal construction with a simple builder that explicitly selects the
>   Logitech C920 and 640-by-480 resolution. Preserve the configured name and safe missing-camera
>   behavior. Do not add camera-control tuning or zoom.
> - Update diagnostic telemetry so students can distinguish acquisition timestamp, observation
>   age, fresh-frame status, and retained-snapshot status without exposing FTC types.
> - Add focused software checks if the repository supports them. At minimum, inspect each data path
>   and demonstrate that repeated Robot loops cannot rewrite a retained timestamp.
> - Keep all observations ID-only in this prompt. Build, run `git diff --check`, inspect the full
>   diff, and update `IMPLEMENTATION_STATUS.md`; stop for review.

Evidence to save:

- Before/after freshness behavior.
- Proof of explicit 640-by-480 configuration.
- Proof that timestamps survive unchanged and age can increase.
- Build and complete-diff results.

### MV-04 v1.0 — Verify tag 22 metadata and add experimental metrics

> - Inspect actual APIs and run the baseline build before editing.
> - Verify the processor's selected tag library contains DECODE 36h11 tag 22 with the correct
>   6.5-inch black-square size. Do not infer metadata merely because ID 22 is detected.
> - If defaults do not provide the verified entry, use the smallest official SDK-supported library
>   selection or builder below the hardware boundary. Do not add field-tag coordinates.
> - Inspect the exact FTC camera/reference-frame definitions and `ftcPose` units/signs. Document the
>   conversion; do not guess axes, rotation order, or angle signs.
> - Populate experimental neutral camera-relative metrics only from a fresh, finite, calibrated
>   detection with verified metadata. Apply the reviewed measured camera-to-robot transform to
>   produce robot-relative metrics using +X right, +Y forward, +Z up.
> - Keep an ID-only observation with `poseAvailable == false` whenever any gate is unmet. Include a
>   clear quality status explaining which gate failed.
> - Do not calculate field pose, read a localization provider, command motors, choose a path, or
>   modify TeamARobot, TeleOps, autonomous OpModes, Pedro code, or Limelight code.
> - Extend only the narrow stationary diagnostic telemetry needed to display neutral range,
>   bearing, pose availability, frame name, quality, acquisition timestamp, and age.
> - Build, run `git diff --check`, inspect the complete diff, and update
>   `IMPLEMENTATION_STATUS.md`; stop for review.

Evidence to save:

- Exact tag-library entry and physical-size source.
- FTC-to-neutral and camera-to-robot conversion explanation.
- Pose-availability failure table.
- Public-boundary and prohibited-behavior audit.
- Build and complete-diff results.

### MV-05 v1.0 — Stage 4 software-only audit

> - Do not change production behavior unless a focused review finds a defect caused directly by
>   MV-03 or MV-04.
> - Run the Java 17 TeamCode build and `git diff --check`.
> - Verify one source is active, one source update occurs per Robot loop, and the FSM remains sole
>   behavior owner.
> - Verify source acquisition timestamps survive unchanged; stale snapshots cannot appear fresh;
>   ages are monotonic for retained observations; loss/reacquisition semantics match MV-02; and
>   stop clears observations and closes camera resources.
> - Verify the stream is explicitly 640 by 480, tag 22 metadata is correct, calibration evidence is
>   cited, metric values require every approved gate, and invalid/unavailable values stay neutral.
> - Verify FTC vision types do not leak through Robot public APIs and the diagnostic accesses only
>   the vision-only Robot API.
> - Search for and report any field pose, localization correction, Pedro dependency, motor request,
>   autonomous decision, Limelight implementation, blocking wait, or direct OpMode camera access.
> - Reconcile `IMPLEMENTATION_STATUS.md` with actual behavior and list every physical check required
>   by MV-06. Stop for review.

Evidence to save:

- Lifecycle and freshness audit table.
- Metric gate and boundary audit.
- Prohibited-behavior search results.
- Build, diff-check, and remaining physical prerequisites.

### MV-06 v1.0 — Supervised stationary range/bearing validation

Use only after MV-05 is reviewed and the complete Stage 4 STOP warning has been confirmed:

> - Reconfirm every STOP prerequisite one at a time. Mark `Blocked` if any is unavailable.
> - Begin with drive motors physically disconnected, the camera and tag secure, and the named STOP
>   operator controlling the Driver Station.
> - Reconfirm the C920 VID/PID, active name, explicit 640-by-480 stream, absence of calibration
>   warnings, tag 22 metadata/size, mount measurements, reference frame, measurement tools, clear
>   area, lighting, and supervision. Do not guess or reuse an unverified physical fact.
> - Before deployment, define a small set of safe stationary placements with measured tape-distance
>   and expected bearing sign: centered, left, and right. Have the student/teacher choose acceptance
>   tolerances before viewing software results; do not invent a universal tolerance.
> - Deploy only the narrow vision diagnostic. Validate camera state, fresh-frame indicator,
>   acquisition timestamp, increasing retained age, pose availability, quality status, tag ID,
>   detection count, range, bearing sign, loss, reacquisition, stability, STOP, preview closure,
>   and clean resource reacquisition.
> - Compare repeated measurements at each placement. Record raw expected and observed values,
>   units, absolute errors, sign correctness, repeatability, lighting, and distance. Do not average
>   away failures or change several causes at once.
> - Stop immediately for stale-as-fresh data, wrong identity, wrong signs, non-finite metrics,
>   unavailable pose, unstable results, calibration warnings, movement, or resource errors. Record
>   one testable next hypothesis. A documented `Blocked` result is valid.
> - Do not calculate robot field pose, write localization, command movement, select a path, add
>   autonomous behavior, or generalize to another camera model.
> - Run the final Java 17 build and `git diff --check`. Record the validation evidence in
>   `APRILTAG_VISION_PHYSICAL_TEST_EVIDENCE.md` and actual status in `IMPLEMENTATION_STATUS.md`;
>   stop for review.

Evidence to save:

- Current safety setup and STOP responsibility.
- Expected-versus-observed table for centered, left, and right placements.
- Freshness, loss/reacquisition, shutdown, and resource-reopen results.
- Accepted limitations or one next hypothesis if blocked.
- Final builds and complete documentation diff.

## Working rules

- Use the student's normal account. Do not run Android Studio, PowerShell, or Gradle as
  Administrator or weaken system security.
- Use the approved JDK 17. If build setup fails, record the complete error and ask teacher/IT.
- Build before and after each code-changing prompt and run `git diff --check` after edits.
- Inspect actual existing classes and SDK APIs before every edit; do not trust planned signatures.
- Keep changes small and understandable to beginning Java students.
- Do not invent calibration, metadata, tag size, camera mount, coordinate signs, timestamps,
  tolerances, measurements, or physical results.
- Do not add a generic camera-profile framework during Stage 4. Prove one C920 metric source first.
- Do not commit, push, merge, or open a pull request until the team reviews the complete diff.
- A later Logitech-model or Limelight prompt must repeat its own identity, calibration,
  configuration, mount, freshness, and physical validation. It is not authorized by this guide.

## Official starting references

At execution time, re-check current official FTC documentation rather than treating these links as
frozen API evidence:

- Camera calibration:
  `https://ftc-docs.firstinspires.org/en/latest/apriltag/vision_portal/apriltag_camera_calibration/apriltag-camera-calibration.html`
- AprilTag metadata:
  `https://ftc-docs.firstinspires.org/en/latest/apriltag/vision_portal/apriltag_metadata/apriltag-metadata.html`
- AprilTag reference frame:
  `https://ftc-docs.firstinspires.org/en/latest/apriltag/vision_portal/apriltag_reference_frame/apriltag-reference-frame.html`
- Detection values:
  `https://ftc-docs.firstinspires.org/en/latest/apriltag/understanding_apriltag_detection_values/understanding-apriltag-detection-values.html`

## Prompt change log

| Date | Prompt | Version | Change |
| --- | --- | --- | --- |
| 2026-08-20 | Guide | 1.0 | Added the Stage 4 metric-observation student workflow. |
| 2026-08-20 | MV-01 through MV-06 | 1.0 | Added discovery, design, freshness/resolution, metadata/metrics, software audit, and stationary validation prompts. |

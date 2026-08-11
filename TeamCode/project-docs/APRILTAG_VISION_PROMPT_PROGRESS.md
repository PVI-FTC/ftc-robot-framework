# AprilTag Vision Prompt Progress

This branch-local record is the source of truth for student handoff. Do not use chat history to
infer completion.

Working branch: `codex/Vision`

## Status meanings

- `Not started`: no work has run on this branch.
- `Results ready`: evidence exists but the student has not finished review.
- `Reviewed`: the student accepted the evidence and resolved every decision required next.
- `Blocked`: a prerequisite, safety condition, build, permission, or required decision is open.

## Rules

- Start Here confirms the current checked-out branch; it never creates or switches branches.
- Immediately before presenting results, change only that prompt row to `Results ready`, or to
  `Blocked` with the exact reason.
- Mark `Reviewed` only in a later turn after student review and required decisions.
- Cite repository files, command results, measurement records, or the architecture decision. Do
  not record quiz answers, scores, credentials, or personal information.
- A new student or AI chat reads this file and its evidence before continuing.

| Prompt | Status | Review date | Durable evidence or decision |
| --- | --- | --- | --- |
| AV-01 | Reviewed | 2026-08-04 | Student approved the observation-only, Logitech/UVC-first separate Team A vision-only pilot; current FSM remains behavior owner. Evidence: current FSM and SDK samples inspected; official FTC VisionPortal/AprilTag, calibration, UVC, lifecycle, and Limelight guidance reviewed. |
| AV-02 | Reviewed | 2026-08-11 | Student accepted the unchanged approved decision: separate Team A vision-only composition, VisionSubsystem FSM ownership, neutral observations, and Logitech-first/Limelight-later rule. AV-02 baseline build passed. |
| AV-03 | Reviewed | 2026-08-11 | Student approved AV-04's neutral observation/source-boundary file plan. FTC SDK 11.2.1 samples/APIs verified for VisionPortal, AprilTagProcessor, and separate Limelight3A; Vision/Hardware artifacts already supplied and baseline build passed. |
| AV-04 | Reviewed | 2026-08-11 | Student accepted the neutral observation boundary: immutable ID-only-safe observations, one internal source, and VisionSubsystem FSM ownership. Default source remains safely unavailable; pre/post builds passed. |
| AV-05 | Reviewed | 2026-08-11 | Student accepted the separate Team A Logitech/UVC vision-only pilot and testing OpMode using confirmed configured name `logitechVisionWebcam`. TeamARobot, drive, localization, TeleOps, and autonomous remain unchanged. Android Studio TeamCode build passed after Gradle sync. |
| AV-06 | Reviewed | 2026-08-11 | Student accepted the corrected camera/FSM lifecycle. Java 17 baseline and post-fix builds passed; active states alone update the source, robot stop releases it, and `git diff --check` passed. |
| AV-07 | Not started | — | Physical camera/tag configuration and safety gate not reviewed. |
| AV-08 | Not started | — | Supervised stationary AprilTag validation not completed. |

# AprilTag Localization and Alignment Prompt Progress

This branch-local record is the source of truth for Stage 5 student handoff. Do not use chat
history to infer completion.

Working branch: `codex/Vision`

## Status meanings

- `Not started`: no work has run on this branch.
- `Results ready`: evidence exists but the student has not finished review.
- `Reviewed`: the student accepted the evidence and resolved every decision required next.
- `Blocked`: a prerequisite, safety condition, build, permission, or required decision is open.

## Rules

- Start Here confirms the current checked-out branch; it never creates or switches branches.
- Execute only the first prompt not marked `Reviewed`.
- Immediately before presenting results, change only that prompt row to `Results ready`, or to
  `Blocked` with the exact reason.
- Mark `Reviewed` only in a later turn after the student attempts five new learning questions,
  resolves required engineering decisions, and explicitly accepts the result.
- Cite repository files, command results, measurement records, or current official sources. Do not
  record quiz answers, scores, credentials, serial numbers, or personal information.

| Prompt | Status | Review date | Durable evidence or decision |
| --- | --- | --- | --- |
| LA-01 | Reviewed | 2026-09-24 | Student accepted the repository/SDK 11.2.1 and official-guidance evidence. Stage 5 will use fixed DECODE GOAL tags 20 and 24 through a replaceable season configuration, the official FTC field coordinate system, and independent fresh field-pose candidates rather than a maintained estimate. Candidates require a `FRESH` snapshot, age at most 250 ms, verified fixed-tag metadata, matching calibration, verified mount, and finite pose; failures expose a rejection reason. Pedro/odometry fusion, BIOBUZZ SDK 12 migration, alignment control, and autonomous integration remain deferred. DECODE tags 21-23 and moving BIOBUZZ tags are not absolute-localization references. No production code changed; JDK 17 baseline passed. |
| LA-02 | Reviewed | 2026-09-24 | Student accepted the Stage 5 design recorded in `APRILTAG_VISION_ARCHITECTURE_DECISION.md`: official FTC field coordinates; replaceable DECODE fixed-GOAL tag 20/24 configuration; SDK 11.2.1 `setCameraPose`/`robotPose` below hardware; optional candidate plus separate status on each immutable observation; and neutral `FieldPose`, `AprilTagFieldPoseCandidate`, and `AprilTagLocalizationConfiguration` classes. The unchanged forward camera remains neutral mount rotation 0/0/0 and maps to SDK camera yaw 0, pitch -90, roll 0. Candidate gates produce an accepted value or explicit rejection. Pedro, BIOBUZZ SDK 12, fusion, alignment, autonomous, Limelight, and motors remain deferred. Builds and diff check passed. |
| LA-03 | Reviewed | 2026-09-24 | Student accepted the neutral immutable `FieldPose`, `AprilTagFieldPoseCandidate`, and defensive `AprilTagLocalizationConfiguration` types. Checked-out SDK 11.2.1 bytecode verified fixed 6.5-inch `BlueTarget` tag 20 at (-58.3727, -55.6425, 29.5) inches and `RedTarget` tag 24 at (-58.3727, 55.6425, 29.5), each with field orientation; OBELISK tags 21-23 have no field pose. SDK metadata remains authoritative without duplicated TeamCode coordinates/quaternions; only tags 20/24 may produce candidates. Constructor/immutability checks, JDK 17 build, prohibited-dependency search, and diff check passed. Live candidate wiring remains for LA-04. |
| LA-04 | Reviewed | 2026-09-24 | Student accepted the SDK camera-pose wiring and optional neutral field-pose candidate boundary after completing all five new learning questions. Candidate gates cover positive acquisition time, 250 ms age, allowed fixed tag 20/24 ID, SDK metadata/field pose, C920 640-by-480 calibration, verified mount, SDK robot pose, and finite values. Retained frames strip candidates with an explicit reason. The stationary diagnostic displays candidate pose, age, and status. Existing tag-22 relative metrics remain separate; no localization estimate, Pedro/odometry write, alignment, autonomous, Limelight, drivetrain, or motor behavior was added. JDK 17 TeamCode build, prohibited-behavior search, complete-diff audit, and diff check passed. |
| LA-05 | Reviewed | 2026-09-24 | Student accepted the software-only audit after completing all five new learning questions. No focused LA-03/LA-04 defect was found, so production code was unchanged. SDK evidence and complete-diff review confirmed transforms, axes/signs, camera orientation, timestamps, rejection gates, immutability, lifecycle, and neutral boundaries; prohibited-behavior searches, JDK 17 build, focused checks, and diff check passed. For later LA-06, the student selected fixed tag 20; three fresh readings at a centered 48-inch robot-origin placement and 12-inch left/right placements while facing the tag; maximum X/Y errors of 3 inches, heading error of 5 degrees, and three-reading spreads of 2 inches per position axis and 3 degrees heading. No physical test or movement was authorized. |
| LA-06 | Blocked | 2026-09-24 | Physical validation cannot continue because the team does not currently have access to a measured space that can reproduce tag 20's official field position/orientation and the official field axes. Prerequisites already reconfirmed: secure recorded C920 mount, configured name/direct Control Hub USB connection, explicit calibrated 640-by-480 stream with no warning, and official flat 6.5-inch tag 20. Preparation added `APRILTAG_LOCALIZATION_FIELD_POSE_TEST_WORKSHEET.md` with SDK tag metadata, exact marked-axis setup, preselected expected robot poses, three-reading tables, tolerances, calculations, loss/reacquisition checks, and STOP checklist. No deployment, physical result, alignment, localization-provider write, or movement occurred. JDK 17 TeamCode build and diff check passed. Resume LA-06 at the unavailable-space prerequisite; do not advance to LA-07. |
| LA-07 | Not started | — | — |
| LA-08 | Not started | — | — |
| LA-09 | Not started | — | — |
| LA-10 | Not started | — | — |

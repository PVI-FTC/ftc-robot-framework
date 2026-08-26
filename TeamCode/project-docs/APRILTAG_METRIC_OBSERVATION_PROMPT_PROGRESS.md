# AprilTag Metric Observation Prompt Progress

This branch-local record is the source of truth for Stage 4 student handoff. Do not use chat
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
- Cite repository files, command results, measurement records, or official sources. Do not record
  quiz answers, scores, credentials, serial numbers, or personal information.

| Prompt | Status | Review date | Durable evidence or decision |
| --- | --- | --- | --- |
| MV-01 | Reviewed | 2026-08-20 | Accepted SDK 11.2.1 evidence. MV-02 must distinguish no-new-frame from fresh-empty, retain labeled stale snapshots with original timestamps, request calibrated 640-by-480, suppress invalid tag-22 metric pose, and expose camera-relative plus experimental robot-relative measurements without localization. |
| MV-02 | Reviewed | 2026-08-21 | Accepted `FRESH`/`RETAINED`/`UNAVAILABLE` snapshots; retained IDs may remain in stationary tracking but have no metric pose; one observation contains optional neutral camera-relative and robot-relative pose objects while remaining one detection. Localization and movement remain prohibited. |
| MV-03 | Reviewed | 2026-08-21 | Accepted neutral fresh/retained/unavailable snapshots, preserved SDK acquisition timestamps, explicit calibrated 640-by-480 configuration, and freshness telemetry. Software checks/build passed; supervised Control Hub checks showed retained timestamp `PASS` with zero failures, fresh-empty loss cleared detections, preview streamed, and no calibration warning appeared. |
| MV-04 | Reviewed | 2026-08-21 | Accepted explicit DECODE 36h11 tag-22 metadata, dual neutral camera/robot pose views, strict fresh/calibration/metadata/finite/zero-rotation gates, and ID-only retained behavior. Builds/checks passed; supervised smoke test showed both frames and finite values without movement, while retained pose was unavailable. Physical accuracy remains for MV-06. |
| MV-05 | Reviewed | 2026-08-26 | Accepted Java 17 TeamCode debug-APK build and clean diff check. The source/FSM/freshness/metric-gate/public-boundary/prohibited-behavior audit found no MV-03/MV-04 production defect. The physical-evidence record now preserves its original calibration-unknown entry and adds the later C920 640-by-480 evidence; MV-06 must re-confirm every physical prerequisite and validate accuracy. |
| MV-06 | Blocked | 2026-08-26 | Supervised stationary session ended before fresh metric readings were collected. Confirmed before stopping: drive motors physically disconnected; reviewed C920/configured name; 640-by-480 preview with no calibration warning; official flat DECODE 36h11 tag 22 with 6.5-inch black square; recorded secure mount; clear/lighted area, measurement tools, adult supervision, and STOP operator. Accepted tolerances: range error <= 2 inches or 10% of measured distance (whichever is larger), bearing error <= 5 degrees, and three-reading spreads <= 1 inch range and <= 2 degrees bearing. The centered placement was set to 36 inches forward and 0 lateral. Fresh/retained frame-status changes were observed, but no range/bearing values were recorded. Next action after re-confirming all STOP prerequisites: capture three FRESH, pose-available Robot range/bearing readings at that centered placement; do not record retained rows as measurements. |

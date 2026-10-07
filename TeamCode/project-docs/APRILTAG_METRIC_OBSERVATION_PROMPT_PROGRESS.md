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
| MV-06 | Reviewed | 2026-09-02 | Accepted the supervised stationary evidence and its limitations. Every usable centered and left/right placement passed the team's preselected robot range, bearing, and repeatability tolerances; the centered 24-inch placement remained outside the offset camera's full-tag view. The repeatable approximately 2.9-to-3.4-inch low range bias and camera-left position sensitivity are accepted only for experimental observation work. The direct lens-plane measurement supports a camera-pose/calibration hypothesis rather than a transform error. Fresh-empty loss, reacquisition, STOP, preview closure, and resource reopen passed. Localization and automatic alignment remain unauthorized pending separate accuracy work and approval. |

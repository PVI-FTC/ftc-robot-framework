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
| MV-03 | Blocked | — | Production work did not start because reviewed MV-02 changes in the progress record and architecture decision are uncommitted; repository instructions require a clean worktree before editing. |
| MV-04 | Not started | — | Tag 22 metadata and experimental neutral metric observations not implemented. |
| MV-05 | Not started | — | Stage 4 software-only audit not completed. |
| MV-06 | Not started | — | Supervised stationary range/bearing validation not completed. |

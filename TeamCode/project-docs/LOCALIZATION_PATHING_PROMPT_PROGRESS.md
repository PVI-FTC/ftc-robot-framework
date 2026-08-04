# Localization and Pathing Prompt Progress

This is the durable, branch-local progress record used by the student guide. It lets a new chat or a
different student determine what has actually been reviewed without relying on an earlier
conversation.

Working branch: `PedroPathingSetup2.0`

## Status meanings

- `Not started`: the prompt has not been executed on this branch.
- `Results ready`: the assistant produced results, but the student has not finished reviewing them.
- `Reviewed`: the student reviewed the evidence, resolved every question required for the next
  prompt, and explicitly accepted the result.
- `Blocked`: work stopped because a prerequisite, build, permission, or required decision is
  unresolved. Record the blocker instead of advancing.

## Progress rules

- `Start Here` advances past a prompt only when its status is `Reviewed`.
- Running a prompt does not automatically make it `Reviewed`.
- Immediately before presenting results, the assistant updates only that prompt's row to `Results
  ready`, or to `Blocked` when work cannot continue. This status-only edit is administrative
  recordkeeping and is allowed even when the prompt itself is read-only.
- After the student explicitly accepts the result and resolves required questions, update only that
  row in a separate turn to mark `Reviewed`.
- Save a short repository-based evidence reference or decision. Do not use chat history as the only
  evidence.
- A different student continuing the same branch reads this record and the cited repository evidence
  before proceeding.
- A fresh student-branch template uses `Working branch: UNCONFIRMED` and sets every prompt to `Not
  started`. `Start Here` replaces `UNCONFIRMED` only after the student confirms the checked-out
  branch. Students must not reset an existing branch's record merely to skip a blocker.

## Prompt record

| Prompt | Status | Review date | Durable evidence or decision |
| --- | --- | --- | --- |
| LP-01 | Reviewed | 2026-08-02 | Student accepted the separate, team-specific Team A Pedro Robot design. Baseline drivetrain ownership is in `DriveSubsystem` and `DriveHardware`. |
| LP-02 | Reviewed | 2026-08-02 | Student accepted `ARCHITECTURE.md` documentation of the approved separate Team A Pedro Robot design as future intent only. |
| LP-03 | Reviewed | 2026-08-02 | Student accepted pinned Pedro v2.1.2 core-only pilot plan, compile SDK 34 requirement, and Microsoft OpenJDK 17.0.2 Gradle readiness. |
| LP-04 | Reviewed | 2026-08-02 | Student accepted pinned Pedro `2.1.2`, compile SDK 34, successful Android Studio Sync, and successful JDK 17 TeamCode build; no Pedro behavior exists yet. |
| LP-05 | Reviewed | 2026-08-03 | Student accepted vendor-neutral pose/drive seams, safe path-following FSM state, preserved simple mecanum composition, and successful TeamCode build. |
| LP-06 | Reviewed | 2026-08-04 | Student accepted separate Team A Pedro robot/controller/factory/configuration gate, preserved TeamARobot, and successful JDK 17 TeamCode build. |
| LP-07 | Reviewed | 2026-08-04 | Student accepted Session 2 static/lifecycle validation, successful Android Studio Sync/TeamCode assembly, and the real-hardware limitations. |
| LP-08 | Reviewed | 2026-08-04 | Student accepted recorded Team A hardware facts, separate initialization/manual/path gates, and successful TeamCode build; LP-09 hardware checks remain required. |
| LP-09 | Not started | — | Pinpoint pose and restricted manual-drive hardware checks not started. |
| LP-10 | Not started | — | Version-matched Pedro tuning and evidence collection not started. |
| LP-11 | Not started | — | Cautious visualized pilot path and final hardware reconciliation not started. |

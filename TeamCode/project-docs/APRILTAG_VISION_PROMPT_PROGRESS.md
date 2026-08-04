# AprilTag Vision Prompt Progress

This branch-local record is the source of truth for student handoff. Do not use chat history to
infer completion.

Working branch: `UNCONFIRMED`

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
| AV-01 | Not started | — | Architecture and official-guidance discovery not started. |
| AV-02 | Not started | — | Vision architecture decision not reviewed. |
| AV-03 | Not started | — | FTC API and baseline-build compatibility check not started. |
| AV-04 | Not started | — | Optional neutral observation boundary not implemented. |
| AV-05 | Not started | — | Logitech pilot composition and testing OpMode not implemented. |
| AV-06 | Not started | — | Session 2 software-only validation not completed. |
| AV-07 | Not started | — | Physical camera/tag configuration and safety gate not reviewed. |
| AV-08 | Not started | — | Supervised stationary AprilTag validation not completed. |

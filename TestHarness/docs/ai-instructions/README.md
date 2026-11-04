# AI Instructions for the Harness

These are the rules for any AI assistant (Claude, Codex, Copilot, Cursor or anything else) working
on the FTC test harness in `TestHarness/`.

## When these instructions apply

**These instructions apply only when the user specifically says the task is Harness work**, for
example by naming "the harness", "the test harness" or the `TestHarness/` folder in their request.

- **The user names the Harness:** these files are your only agent instructions for that task.
  Do not follow `AGENTS.md`, `CLAUDE.md`, `TeamCode/project-docs/` or any other instruction file
  outside `TestHarness/`. Those are written for robot code. If they disagree with these files, these
  files win.
- **The task isn't Harness work:** ignore these files completely, even if you opened, read or
  searched files in `TestHarness/`, these files were loaded automatically, or an earlier task in
  this session was Harness work. Follow the repository's normal instructions (the root `AGENTS.md`
  and the files it points to).
- **You are not sure:** ask the user whether the task is Harness work before you change
  anything. Don't guess.

Reading a file in `TestHarness/` while doing other work does not make it a Harness task.

## These rules cover the task, not the session

Naming the Harness makes that task Harness work. It doesn't restrict the rest of the session:

- **During a Harness task,** add, edit or delete nothing outside `TestHarness/`. If the task needs a
  file outside it changed, stop and tell the user which file and what change.
- **A later request that isn't Harness work** follows the repository's normal instructions and may
  change files outside `TestHarness/`, in the same session. No new session is needed.
- You may always **read** anything (see [01-scope-and-boundaries.md](01-scope-and-boundaries.md)).

## Read in this order

| File | What it covers |
|---|---|
| [01-scope-and-boundaries.md](01-scope-and-boundaries.md) | What you may and may not touch. **Read this first, every time.** |
| [02-unity-readiness.md](02-unity-readiness.md) | How to write code so a future Unity simulator can plug into it. |
| [03-workflow.md](03-workflow.md) | What to do before, during and after a change, and how to report it. |
| [04-code-style.md](04-code-style.md) | How harness code should look and read. |
| [05-desktop-viewer.md](05-desktop-viewer.md) | The fallback window (field view and telemetry) for when Unity can't be used. |
| [06-bot-team.md](06-bot-team.md) | The manager and worker bots: who owns what, how work is handed out, and how a new session takes over as manager. |

The current state of the work is in [`TestHarness/docs/PROGRESS.md`](../PROGRESS.md). Read it
before you start and update it when you finish.

## The two rules that matter most

1. **For a Harness task, only add, edit or delete files inside `TestHarness/`.** Nothing outside it.
2. **Keep the simulation core free of any engine, window or network code**, so Unity, or the
   fallback desktop window, can be connected later without rewriting it.

## Keeping these files up to date

If the user changes a rule or decides something new (for example how Unity will connect), update
the matching file here in the same change, so the next assistant sees it.

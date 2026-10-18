# AI Instructions for the Harness

These are the rules for any AI assistant (Claude, Codex, Copilot, Cursor or anything else) working
on the FTC test harness in `Harness/`.

## When these instructions apply

**These instructions apply only when the user specifically says the task is Harness work**, for
example by naming "the harness", "the test harness" or the `Harness/` folder in their request.

- **The user names the Harness:** these files are your only agent instructions for that task.
  Do not follow `AGENTS.md`, `CLAUDE.md`, `TeamCode/project-docs/` or any other instruction file
  outside `Harness/`. Those are written for robot code. If they disagree with these files, these
  files win.
- **The user has not named the Harness at any point in this session:** ignore these files
  completely, even if you opened,
  read or searched files in `Harness/`, or these files were loaded automatically. Follow the
  repository's normal instructions (the root `AGENTS.md` and the files it points to).
- **You are not sure:** ask the user whether the task is Harness work before you change
  anything. Don't guess.

Reading a file in `Harness/` while doing other work does not make it a Harness task.

## Once it's a Harness session, it stays one

As soon as the user has named the Harness as the task, **the whole session (conversation) is a
Harness session until it ends.** From that point:

- **Add, edit or delete nothing outside `Harness/` for the rest of the session.** This still
  holds if a later message doesn't mention the Harness, is about other work, or asks for a change
  outside `Harness/`. It also holds after the conversation is summarized or compacted.
- If a later request needs a file outside `Harness/` changed, **don't do it.** Tell the user which
  file and what change, and suggest they make it themselves or start a **new session** for that
  work. Only a new session can lift this rule. Saying "it's fine" in the same session does not.
- You may still **read** anything (see [01-scope-and-boundaries.md](01-scope-and-boundaries.md)).
- If your context is summarized, keep the fact "this is a Harness session" in the summary so the
  rule survives.

## Read in this order

| File | What it covers |
|---|---|
| [01-scope-and-boundaries.md](01-scope-and-boundaries.md) | What you may and may not touch. **Read this first, every time.** |
| [02-unity-readiness.md](02-unity-readiness.md) | How to write code so a future Unity simulator can plug into it. |
| [03-workflow.md](03-workflow.md) | What to do before, during and after a change, and how to report it. |
| [04-code-style.md](04-code-style.md) | How harness code should look and read. |
| [05-desktop-viewer.md](05-desktop-viewer.md) | The fallback window (field view and telemetry) for when Unity can't be used. |

The current state of the work is in [`Harness/PROGRESS.md`](../PROGRESS.md). Read it before you
start and update it when you finish.

## The two rules that matter most

1. **Only add, edit or delete files inside `Harness/`.** Nothing outside it, for the rest of the
   session.
2. **Keep the simulation core free of any engine, window or network code**, so Unity, or the
   fallback desktop window, can be connected later without rewriting it.

## Keeping these files up to date

If the user changes a rule or decides something new (for example how Unity will connect), update
the matching file here in the same change, so the next assistant sees it.

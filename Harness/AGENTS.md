# Harness agent instructions

**These instructions apply only when the user specifically says the task is Harness work**, for
example by naming "the harness", "the test harness" or the `Harness/` folder in their request.

- **The user names the Harness:** follow these instructions instead of the repository-root
  `AGENTS.md` and every other agent file outside `Harness/`.
- **The user has not named the Harness at any point in this session:** ignore this file and
  `ai-instructions/` completely, even if you are reading or searching files in `Harness/`. Follow
  the repository's normal instructions.
- **You are not sure:** ask the user before changing anything.

**Once the Harness has been named, the rest of the session is a Harness session.** Until the
session ends, add, edit or delete nothing outside `Harness/`, even if a later request doesn't
mention the Harness or asks for an outside change. Tell the user what would need changing and
suggest a new session for it. See
[ai-instructions/README.md](ai-instructions/README.md#once-its-a-harness-session-it-stays-one).

For a Harness task, read every file in [`ai-instructions/`](ai-instructions/README.md) before making
changes. In short:

1. **Only add, edit or delete files inside `Harness/`, for the rest of the session.** Read
   anything, change nothing outside. If a task needs a change elsewhere, stop and tell the user.
2. **Keep the simulation core engine-free, UI-free and step-driven**, sharing state as plain data
   in FTC units, so a future Unity simulator, or the fallback desktop window (field view and
   telemetry), can connect without a rewrite
   ([02-unity-readiness.md](ai-instructions/02-unity-readiness.md),
   [05-desktop-viewer.md](ai-instructions/05-desktop-viewer.md)).
3. Run the harness tests from `Harness/` with `.\gradlew.bat test` before and after your change.
   Then update [PROGRESS.md](PROGRESS.md) with the new status and a log line.
4. Do not commit, push or open a pull request unless the user asks.

# Harness agent instructions

**These instructions apply only when the user specifically says the task is Harness work**, for
example by naming "the harness", "the test harness" or the `TestHarness/` folder in their request.

- **The user names the Harness:** follow these instructions instead of the repository-root
  `AGENTS.md` and every other agent file outside `TestHarness/`.
- **The task isn't Harness work:** ignore this file and `docs/ai-instructions/` completely, even if
  you are reading or searching files in `TestHarness/`, or an earlier task in this session was
  Harness work. Follow the repository's normal instructions.
- **You are not sure:** ask the user before changing anything.

**These rules last only as long as the Harness task.** Naming the Harness doesn't restrict the rest
of the session. See
[docs/ai-instructions/README.md](docs/ai-instructions/README.md#these-rules-cover-the-task-not-the-session).

For a Harness task, read every file in [`docs/ai-instructions/`](docs/ai-instructions/README.md) before making
changes. In short:

1. **For a Harness task, only add, edit or delete files inside `TestHarness/`.** Read anything,
   change nothing outside. If the task needs a change elsewhere, stop and tell the user.
2. **Keep the simulation core engine-free, UI-free and step-driven**, sharing state as plain data
   in FTC units, so a future Unity simulator, or the fallback desktop window (field view and
   telemetry), can connect without a rewrite
   ([02-unity-readiness.md](docs/ai-instructions/02-unity-readiness.md),
   [05-desktop-viewer.md](docs/ai-instructions/05-desktop-viewer.md)).
3. Run the harness tests from `TestHarness/th_src/` with `.\gradlew.bat test` before and after your change.
   Then update [PROGRESS.md](docs/PROGRESS.md) with the new status and a log line.
4. Do not commit, push or open a pull request unless the user asks.
5. Several bots may be working here at once (a manager and worker bots). Read
   [06-bot-team.md](docs/ai-instructions/06-bot-team.md) for who owns which files, how to coordinate,
   and how to take over as manager. Never run `gradlew test` while another bot is running it.

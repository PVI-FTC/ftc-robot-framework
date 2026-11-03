# 03 · Workflow

## Before you change anything

1. Read `TestHarness/docs/ai-instructions/` (all of it), `TestHarness/docs/README.md` and
   `TestHarness/docs/PROGRESS.md`.
2. From the repository root, run `git status --short` and note anything that is **already**
   changed. That isn't your work, so leave it alone.
3. Read the harness classes you will touch and any TeamCode classes your work depends on. Check
   the real constructors, method names and packages. Never assume them.
4. Run the tests once so you know they passed before you started (see below). If they already
   fail, tell the user before you continue.

## Running the tests

From PowerShell, in `TestHarness/th_src/`:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat test
```

One class:

```powershell
.\gradlew.bat test --tests "pvi.harness.TeamATeleOpScriptTest"
```

The HTML report is at `TestHarness/th_src/build/reports/tests/test/index.html`.

Always run tests through Gradle. Without it the Java agent isn't loaded and timed code reads the
real clock.

## While you work

- Make only the change the user asked for. Don't clean up, rename or reformat code you weren't
  asked to touch.
- Every new behavior gets a test in `th_src/src/test/java/pvi/harness/` (or a matching sub-package),
  following **script → set up → run → check** from the README.
- Check that a new test can fail: break the expected value once, see it fail, then put it back.
- If the harness's public API changes (anything a test or a future Unity bridge would call),
  update `TestHarness/docs/README.md` in the same change.
- `TestHarness/th_src/build.gradle` reads the library versions from the repository's
  `build.dependencies.gradle` by itself (settings in `TestHarness/th_src/harness.properties`).
  **Never** edit the root file. If a new robot library only works on Android, add it to
  `host.skipLibraries` instead.

## After you change something

1. Run all the harness tests. They must pass.
2. Run `git status --short` from the repository root. Every path you changed must start with
   `TestHarness/` (see [01-scope-and-boundaries.md](01-scope-and-boundaries.md)).
3. Run `git diff --check -- TestHarness` to catch whitespace problems.
4. Read your whole diff once and remove anything that is not part of the task.
5. Update [`TestHarness/docs/PROGRESS.md`](../PROGRESS.md):
   - change the status of what you worked on
   - update "Last updated" and "Test status"
   - add one line to the Log (date, change, test result)

   Never mark something ✅ unless its tests pass.
6. Check your work against [02-unity-readiness.md](02-unity-readiness.md) and
   [05-desktop-viewer.md](05-desktop-viewer.md). In particular, tests must still pass with no
   screen and must never open a window by themselves.

## Reporting back

Tell the user, in plain words:

- which files you created, changed or deleted (all under `TestHarness/`)
- any public API you added or changed
- the test results (how many passed and failed, with the failure output if any failed)
- anything you assumed, skipped or could not check
- anything you think needs changing **outside** `TestHarness/`, as a suggestion for the user to
  make. Never make that change yourself.

Do not commit, push or open a pull request unless the user asks.

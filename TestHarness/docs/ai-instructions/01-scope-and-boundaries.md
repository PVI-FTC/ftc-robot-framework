# 01 · Scope and boundaries

> Everything in this file applies **while the task is one the user has named as Harness work**. For
> any other task, even in the same session, ignore it and follow the repository's normal
> instructions. See [README.md](README.md#when-these-instructions-apply).

## The rule

**You may only add, edit or delete files inside `TestHarness/`.**

This includes every file outside it:

- `TeamCode/`, `FtcRobotController/`, `doc/`, `libs/`, `.github/`
- the root `AGENTS.md`, `README.md`, `build.gradle`, `build.common.gradle`,
  `build.dependencies.gradle`, `settings.gradle`, `gradle.properties`, `.gitignore`
- anything else in the repository, and anything on the computer outside the repository

No exception is made for "tiny" changes, typo fixes, formatting or a missing import. If the task
seems to need a change outside `TestHarness/`, **stop and tell the user** what you would change and
why. Let them decide. If they then ask you to make it, treat that as its own request that isn't
Harness work.

**The rule ends with the Harness task.** A later request in the same session that isn't Harness work
may change files outside `TestHarness/` (see
[README.md](README.md#these-rules-cover-the-task-not-the-session)).

## Reading is fine

You may **read** anything in the repository. The harness depends on it:

- `TeamCode/src/main/java` (`../../TeamCode/src/main/java` from `th_src/`) is compiled into the
  harness, so you will often read TeamCode to see what a test should check.
- the repository's `build.dependencies.gradle` holds the library versions that
  `TestHarness/th_src/build.gradle` has to match.

Treat these as read-only input. If TeamCode has a bug that a harness test finds, report the bug.
Don't fix it.

## Other agent files during Harness work

Instruction files outside `TestHarness/` (the root `AGENTS.md`, `TeamCode/project-docs/`, and any
`CLAUDE.md` or `.cursor` rules elsewhere) are for robot code. **During a task the user has named
as Harness work, do not follow them**, and do not update them, even if they ask you to (for
example "update IMPLEMENTATION_STATUS.md after editing"). For that task, the files in
`TestHarness/docs/ai-instructions/` are the only rules.

For a task that isn't Harness work, the opposite is true: those files apply, and this folder does
not.

## Robot packages inside the harness

Some harness files have to use a TeamCode package name to read package-private values, such as
`th_src/src/pvi/java/org/firstinspires/ftc/teamcode/robots/teamA/TeamAPedroHarnessAccess.java`
(a PVI extra, built only with `pvi.extras=true` in `harness.properties`; the harness core never uses
it). That's allowed because the file lives in `TestHarness/`. Rules for these bridge files:

- They only **read** robot values. They never change robot behavior.
- Their class comment starts with `HARNESS FILE, not robot code.`
- Their names end in `HarnessAccess`, so nobody mistakes them for robot code.

## Commands that can touch files outside `TestHarness/`

| Allowed | Not allowed |
|---|---|
| `.\gradlew.bat test` (and other tasks) run **from `TestHarness/th_src/`** | `gradlew` from the repository root (it builds the robot and writes to the root `build/`) |
| `git status`, `git diff`, `git log`, `git show` | `git checkout`, `git restore`, `git reset`, `git stash`, `git clean`, `git rebase`, `git merge`, `git pull` |
| Creating temporary files inside `TestHarness/th_src/build/` | Writing temporary files anywhere else in the repository |

Only commit, push or create branches when the user asks you to. When you do commit, stage only
paths under `TestHarness/` (`git add TestHarness/...`), never `git add -A` or `git add .`.

## Before you finish

Run `git status --short` from the repository root. Every changed or new path must start with
`TestHarness/`. If anything else shows up and you changed it, undo **your** change to that file and
tell the user. If it was already changed before you started, leave it alone.

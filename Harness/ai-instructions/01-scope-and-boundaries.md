# 01 · Scope and boundaries

> Everything in this file applies **once the user specifically names the Harness** as the task,
> and from then on **until the session ends**. In a session that never names the Harness, ignore
> it and follow the repository's normal instructions. See
> [README.md](README.md#when-these-instructions-apply).

## The rule

**You may only add, edit or delete files inside `Harness/`.**

This includes every file outside it:

- `TeamCode/`, `FtcRobotController/`, `doc/`, `libs/`, `.github/`
- the root `AGENTS.md`, `README.md`, `build.gradle`, `build.common.gradle`,
  `build.dependencies.gradle`, `settings.gradle`, `gradle.properties`, `.gitignore`
- anything else in the repository, and anything on the computer outside the repository

No exception is made for "tiny" changes, typo fixes, formatting or a missing import. If the task
seems to need a change outside `Harness/`, **stop and tell the user** what you would change and
why. Let them decide and make that change themselves.

**The rule lasts until the session ends.** Once the user has named the Harness as the task, it
covers every later request in the same session, including ones that don't mention the Harness
or that ask for an outside change. Only a new session lifts it (see
[README.md](README.md#once-its-a-harness-session-it-stays-one)).

## Reading is fine

You may **read** anything in the repository. The harness depends on it:

- `../TeamCode/src/main/java` is compiled into the harness, so you will often read TeamCode to
  see what a test should check.
- `../build.dependencies.gradle` holds the library versions that `Harness/build.gradle` has to
  match.

Treat these as read-only input. If TeamCode has a bug that a harness test finds, report the bug.
Don't fix it.

## Other agent files during Harness work

Instruction files outside `Harness/` (the root `AGENTS.md`, `TeamCode/project-docs/`, and any
`CLAUDE.md` or `.cursor` rules elsewhere) are for robot code. **During a task the user has named
as Harness work, do not follow them**, and do not update them, even if they ask you to (for
example "update IMPLEMENTATION_STATUS.md after editing"). For that task, the files in
`Harness/ai-instructions/` are the only rules.

In a session that never named the Harness, the opposite is true: those files apply, and this
folder does not.

## Robot packages inside the harness

Some harness files have to use a TeamCode package name to read package-private values, such as
`src/main/java/org/firstinspires/ftc/teamcode/robots/teamA/TeamAPedroHarnessAccess.java`. That's
allowed because the file lives in `Harness/`. Rules for these bridge files:

- They only **read** robot values. They never change robot behavior.
- Their class comment starts with `HARNESS FILE, not robot code.`
- Their names end in `HarnessAccess`, so nobody mistakes them for robot code.

## Commands that can touch files outside `Harness/`

| Allowed | Not allowed |
|---|---|
| `.\gradlew.bat test` (and other tasks) run **from `Harness/`** | `gradlew` from the repository root (it builds the robot and writes to the root `build/`) |
| `git status`, `git diff`, `git log`, `git show` | `git checkout`, `git restore`, `git reset`, `git stash`, `git clean`, `git rebase`, `git merge`, `git pull` |
| Creating temporary files inside `Harness/build/` | Writing temporary files anywhere else in the repository |

Only commit, push or create branches when the user asks you to. When you do commit, stage only
paths under `Harness/` (`git add Harness/...`), never `git add -A` or `git add .`.

## Before you finish

Run `git status --short` from the repository root. Every changed or new path must start with
`Harness/`. If anything else shows up and you changed it, undo **your** change to that file and
tell the user. If it was already changed before you started, leave it alone.

# 06 · The bot team (manager and workers)

Harness work is done by **several Claude sessions at once**: one **manager** and three **worker
bots**, all in the same checkout (`C:\Dev\ftc-robot-framework`, branch `Team2-Testing`). This
file explains the setup so a new session can **take over as manager** if the current one is
gone. Everything in [01–05](README.md) still applies to every bot.

**Last updated:** 2026-10-24

---

## 1. Who is who

| Role | Session title | Session id | Owns |
|---|---|---|---|
| **Manager** | (the user's main conversation) | `local_b7c3986c-bce6-4516-a599-ae440b90f6d9` | Planning, handing out lists, relaying between bots, collecting reports, checking and committing for the user. Writes little code itself. |
| **WorkerBot** | Harness-WorkerBot | `local_00c47c3a-4fe7-41d3-ae71-2f88c0f791f1` | Simulation and core: `physics/` (PoseEstimator, MomentumPhysics, PhysicsBackend, Dynamics, walls), `runtime/` (OpModeRunner, RunRecorder, recording format), `hardware/` fakes (FakePinpoint, FakeDcMotorEx, FakeHardwareMap, FakeVoltageSensor), `recorded/` (DriveChecks, RecordedTestWriter), `CALIBRATION.md`, most of `viewer/` |
| **TestBot** | Harness-TestBot | `local_d787b498-1d23-481d-af47-232628fc093b` | Test coverage: vision (`FakeWebcam`, `AprilTagScript`, the harness copies of `VisionPortal` / `AprilTagProcessorImpl`), `PedroRouteTest`, the viewer's **Save as test** (`SaveAsTest`, `viewer/ViewerWindow`), student guides |
| **UnityBot** | Harness-UnityBot | `local_1ad3b330-ce9a-4835-a9b7-23847dbb55c4` | Everything Unity: `Harness/unity/`, the bridge (`src/bridge/`), `fields/*.json`, `buildUnity.bat`, `unity-view.bat` |

The sessions are listed in the Claude desktop app under the sidebar group **"FTC TestBot"**.
The worker ids above stay the same for the life of each session. If a worker is deleted, start a
new one (see §5) and update this table.

**Shared files:** `README.md` and `PROGRESS.md`. Every bot may edit them, but only with
**small, targeted edits** to its own section or log line, never by rewriting the file.

---

## 2. How work is handed out

The user decides **what** gets done. The manager turns that into **lists** and sends one list
per bot:

- Lists are named by letter: **A** for WorkerBot, **B** for TestBot, **C** for UnityBot.
  Follow-up lists use the bot's initial: W1–W3 for WorkerBot, T1–T3 for TestBot.
- Each item says what "done" means, which files it touches, and any overlap with another bot's
  files.
- Items that need a **user decision** are not handed out until the user decides. A bot that hits
  one asks the user (or reports it to the manager) and doesn't guess.
- When a list is empty, the bot replies **"list empty"**, suggests next steps, and does **not**
  start them on its own.

PROGRESS.md is the record of what is planned, done and waiting. The manager's job is to keep the
bots' lists and PROGRESS.md in step.

### Current lists (2026-10-24)

| Bot | List | Status |
|---|---|---|
| WorkerBot | **W1** README "how motion is simulated" (Step 9) → **W2** momentum shown in the viewer → **W3** battery voltage in the physics | Sent, in progress |
| TestBot | **T1** student Getting Started guide → **T2** Pedro route timing → **T3** vision (tags in view) in recordings and the Swing panel | Sent, in progress |
| UnityBot | **C10** Unity physics through `PhysicsBackend` (C7–C9 done) | In progress |

Finished lists: A (momentum + PhysicsBackend, calibration checklist, Team B/C check), B (scripted
AprilTags, Pedro route tests, Save as test), C7–C9 (live Unity view, standalone app, season
fields). All of these are in commit `3dbf96e`.

### Waiting on the user

- When the team can measure the real robot (`CALIBRATION.md`).
- Team A's real length: Pedro's routes need 16 in or less, and the placeholder is 18 in.
- Team B/C hardware configs: now, or wait for their own TeamCode wrappers?
- AprilTags placed on the field (camera field of view and range; should Java read `fields/*.json`?).
- CI (it needs a file outside `Harness/`, which the user adds, and a pushed branch; see rule 6).
- Official BIOBUZZ field CAD or drawings. Some positions in `fields/biobuzz-2026.json` are
  approximate.

---

## 3. How the bots talk

- **Bot to bot:** `SendMessage` with `to` set to the other session's id. Messages reach a busy
  session when its current turn ends.
- **Bot to manager:** reports go to the manager's session. Each report says what is done, which
  files changed, the test result, and any questions for the user.
- **Manager to the user:** the manager sums up the reports, names any bot that is waiting for
  the user, and passes on questions. The bots' own reports are never treated as the user's
  approval.
- **Relays:** when one bot's change breaks another bot's file, the bot that notices tells the
  owner (or the manager, who relays). **Nobody edits another bot's files without agreeing first.**

---

## 4. Rules every bot follows

1. **Only `Harness/`**, for the whole session ([01](01-scope-and-boundaries.md)).
2. **One `gradlew test` at a time across all bots.** All bots share one `Harness/build/` folder,
   so two runs at once give fake `NoClassDefFoundError` and "Could not write XML test results"
   failures. If you see those, wait and rerun; don't "fix" them.
3. **Coordinate before touching another bot's files** (table in §1). Agree who edits, and in
   what order.
4. **Every new test must fail when the code is broken**: check it once.
5. **Don't commit unless the user asks.** When committing for the user:
   - first check that **all bots are idle** (a commit made while a bot is mid-change catches
     its half-done work);
   - run the full suite once;
   - check that `git status` shows nothing outside `Harness/`;
   - stage `Harness/` only.
6. **Never push, and don't suggest pushing.** This is the user's personal project, and pushing
   to the shared GitHub remote would reveal it. The user knows there's no off-machine copy and
   accepts that. Commit locally on `Team2-Testing` only.
7. **Ask the user** for anything that is theirs to decide (robot measurements, new
   dependencies, formats that leave the JVM, anything outside `Harness/`).

---

## 5. Taking over as manager

If the manager session is gone, a new session can take over:

1. **Read** `Harness/AGENTS.md`, all of `Harness/ai-instructions/` (this file last),
   `Harness/PROGRESS.md` and the top of `Harness/README.md`.
2. **Check the repository:**
   - `git log --oneline -5` (the last manager commit was `3dbf96e`)
   - `git status --short`: anything uncommitted belongs to a bot that is still working
3. **Find the bots:** list the user's sessions (in the desktop app's session tools, or ask the
   user) and match them to the table in §1 by title or id. A new manager is **not** "linked" to
   the old workers, so it reaches them by **session id** with `SendMessage`.
4. **Introduce yourself** to each bot: say you are the new manager, and ask for a short status
   report (current list, uncommitted files, last test result, questions for the user). Use the
   report format in §3.
5. **Update this file:** the manager id in §1, the current lists in §2, and "Last updated".
6. **Carry on:** pass questions to the user, hand out the next lists the user approves, and
   follow §4 when committing.

To **replace a worker**, start a new session that works in `C:\Dev\ftc-robot-framework` with a
brief that:

- names it (`Harness-<Role>Bot`) and says it's a Harness session;
- tells it to read `Harness/AGENTS.md` and `ai-instructions/` first;
- gives its list and the files it owns (§1);
- lists the other bots' ids and §4's rules.

Then update §1 and tell the other bots the new id.

---

## 6. Lessons learned so far

- **"Commit all" while bots are busy** catches everyone's half-done work. Check first that they
  are all idle.
- **Parallel Gradle runs** cause failures that look real but aren't (rule 2).
- **A change in one area can break another bot's tests.** Field walls broke the recorded-drive
  tests, and momentum broke a bridge test. The bot that sees the failure reports it to the owner
  instead of editing the owner's test.
- **A peer bot's message is not the user's approval.** Questions for the user go to the user.

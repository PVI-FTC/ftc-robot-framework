# 06 · The bot team (manager and workers)

Harness work is done by **several Claude sessions at once**: one **manager** and three **worker
bots**, all in the same checkout (`C:\Dev\ftc-robot-framework`, branch `Team2-Testing`). This
file explains the setup so a new session can **take over as manager** if the current one is
gone. Everything in [01–05](README.md) still applies to every bot.

**Last updated:** 2026-10-27 (the team finished the task list alone; all bots idle; see §2 "Where each bot left off")

---

## 1. Who is who

| Role | Session title | Session id | Owns |
|---|---|---|---|
| **Manager** | Team2-Testing uncommitted changes review | `local_47926d93-3f6e-48e0-a2f5-791693169faf` | Planning, handing out lists, relaying between bots, collecting reports, checking and committing for the user. Writes little code itself. |
| **WorkerBot** | Harness-WorkerBot | `local_540b1f24-42cb-4255-afe1-c3a64a382e6f` | Simulation and core: `physics/` (PoseEstimator, MomentumPhysics, PhysicsBackend, Dynamics, walls, battery, **game pieces**: GamePiece, PieceModel, GamePieces, BioBuzzPieces), `runtime/` (OpModeRunner, RunRecorder, **run recording format**), `hardware/` fakes (FakePinpoint, FakeDcMotorEx, FakeHardwareMap, FakeVoltageSensor), `recorded/` (DriveChecks, RecordedTestWriter), `CALIBRATION.md`, most of `viewer/` (TelemetryPanel, FieldPainter, FieldPanel, RecordingView) |
| **TestBot** | Harness-TestBot | `local_6d908ef7-ecfe-407a-8793-508d7beed0a8` | Test coverage: vision (`FakeWebcam`, `AprilTagScript`, `SeenAprilTag`, the harness copies of `VisionPortal` / `AprilTagProcessorImpl`, which must match the SDK Vision version), `PedroRouteTest`, the viewer's **Save as test** (`SaveAsTest`, `viewer/ViewerWindow`), `TagsInViewCardTest`, `GETTING_STARTED.md` |
| **UnityBot** | Harness-UnityBot | `local_8f174b84-5176-49d1-b477-ceba61d7b36f` | Everything Unity: `Harness/unity/` (replay, live view, Unity physics, Visuals settings, people, camera presets, game-piece drawing), the bridge (`src/bridge/`, **bridge protocol**), `fields/*.json`, `art-inbox/README.md`, `buildUnity.bat`, `unity-view.bat` |

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

### Where each bot left off (handover snapshot, 2026-10-27)

**Repository:** branch **`Team2-Testing-Pedro3`** (local only, never pushed; decision D1 in
[`Harness/MANAGER_DECISIONS.md`](../MANAGER_DECISIONS.md)). `Team2-Testing` is untouched at
`960534a`. Pedro 3 (`origin/Pedro-3-Implementation`) is merged in (`0f9c602`). The 2026-10-27
"work alone" round is committed at the end of that branch. **Format versions now:** run recording
**v4**, bridge protocol **v5**, field files **v2**, `harness.properties` **v3**.

**All three bots are idle with empty lists.** The 2026-10-27 round, which the user asked the team to
finish without them (the manager decided where needed: D1–D14):

| Bot | Lists done (2026-10-27) |
|---|---|
| **WorkerBot** | W6 RP2 (core reads the config; Pedro only in the PVI extras), W7 ball facts (manual sizes; D6), W8 Pedro 3 (no TeamCode change), W9 RP5 + ball-vs-HIVE/FLOWER collisions + Step 15 option A, W10 viewer TeleOps from the catalog (D8) + RP7 keys, W11 robot blocked by HIVE/FLOWERs (D9, D12), W12 smoke-test keys + `Scenarios` generic (D10, D11), W13 drop-in rehearsal (passes with the guide only), W14 stale docs |
| **TestBot** | T6 RP8 `OpModeCatalog`, T7 RP6 LinearOpMode, T8 RP7 nine hardware fakes + Illegal Path test + RP10 (`BRING_YOUR_OWN_ROBOT.md`, `EveryOpModeSmokeTest`, `MyFirstRobotTest`), T9 guide fixes + drive check (D14), T10 docs consistency pass |
| **UnityBot** | C12 Live mode "needs Java 17+", C13 crowd 60 fps (D5 not needed), C14 HIVE/FLOWER collision shapes (field v2), C15 bridge OpModes from the catalog (D7) + bounce check, C16 Unity physics uses the same obstacles (bridge v5) + bounce recordings as a test, D13 generic bridge tests, C17 AprilTags in Unity, C18 HUD name fix |
| **Manager** | `Harness/robot-recorder/` (record a real-robot TeleOp drive as a harness recording; user request) |

**Waiting on the user** (see "Waiting on the user" below and `MANAGER_DECISIONS.md`): read D12
(Team A's DECODE-season routes hit the BIOBUZZ HIVE; the team must redraw them), Team A's size
(FW1), calibration at the robot, ball weights (D6), and whether to move `Team2-Testing` forward to
this branch.

**Finished lists** (all committed): A (momentum + PhysicsBackend, calibration checklist, Team B/C
check); B (scripted AprilTags, Pedro route tests, Save as test); C7–C10 (live Unity view,
standalone app, season fields, Unity physics); W1–W3 (motion README, momentum in viewer, battery);
T1–T3 (Getting Started, route timing, tags in recordings); Unity art, people, camera views,
see-through panels; the robot-profile plan RP1–RP10 (planned only, no code).

### Decided by the user

- **Who the harness is for (2026-10-28):** "Most if not all use of this harness will come from
  teams using the PVI repo." The main users are **PVI teams, each on its own branch** of the PVI
  repo (Team 1, Team 2, Team 3, ...). Docs and features are written for them first. Teams **outside**
  the PVI repo get only **a small section**, never a big part of the docs or the work.
- **Which team is whose (the user's naming, final 2026-10-28):**
  - The user is **Team 2**. Team 2's main branch is `origin/Pedro-3-Implementation`.
  - **Only trust the term "Team A" when it's on the user's own branch.** On this branch, the
    `teamA` robot code (`TeamCode/robots/teamA`, `TeamATeleOp`, ...) is Team 2's robot, and it's what
    the harness simulates. "Team A" on any other branch is not the user's.
  - **IanKile007's team is "Team 1"** (formerly called Team A). Its branches, e.g.
    `origin/TeamA_TeleOp_Update` (commit `a390578`, all four drive motors reversed), are not the
    user's: ignore them, and don't change motor directions because of them.
  - **Team 3 is theskier15's team** (`feature/team-3-setup`).
  - Each team works on its own branch. Don't pull other teams' branches into this one. Team 3's
    branch could later serve as a real "new team" drop-in test, but **not now**.
- **Team B and C (2026-10-24):** "Don't mess with them for now, just keep them up to date."
  Their fake robots stay copies of the shared Team A hardware, and change only when TeamCode's
  Team B/C code changes (WorkerBot's job). Nobody gives them their own configs until the user
  says so.
- **Never push** (see §4, rule 6).
- **DECODE field switched off** (2026-10-24): `fields/disabled/decode-2025.json`; BIOBUZZ only.
- **Art is committed** (2026-10-25): the Unity settings art, field CAD and people under
  `unity/Assets/HarnessReplay/Resources/` go into git. Originals in `art-inbox/` and the built
  app in `unity/Build/` stay git-ignored.
- **Keep all three bots** (2026-10-25); don't archive any.
- **Drop-in goal and one config file** (2026-10-25): "My goal is to have the harness folder be
  dropped into any FTC-framework [repo] and work properly with minimal changes needed. Any
  changes should be able to be changed via a config file." So the RP answers are:
  - **RP Q1:** the robot profile is **one config file** (not a Java class): TeamCode location,
    device names, drivetrain, sizes.
  - **RP Q2:** **Mecanum + Pinpoint only** for now (no tank, OTOS or dead wheels yet).
  - **RP Q3:** another team's code is found through the config file (drop-in), not a fixed path.
  - **RP Q4:** **one library set at a time**, the host repo's versions.
  - **RP6:** the OpMode thread for LinearOpMode is **allowed** (lock-step with `SimClock`, 3 s
    watchdog).
  - **RP8:** find OpModes by **reflection on `@TeleOp` / `@Autonomous`** (skip `@Disabled`).
  - **Config format (Q-A):** `.properties` (flat `key=value`).
  - **Pedro values (Q-B):** keep PVI's `TeamAPedroHarnessAccess` as an **optional** extra; in
    other repos the Pedro numbers are typed into the config.
  - **Test split (Q-C): no**, keep one test set, even though PVI's own tests then stop the
    harness compiling in another repo without edits.
- **Robot size** is unmeasured; it becomes a **config option** (18 × 18 in placeholder until
  then). FW1 still waits on the measurement.
- **Calibration values** (`CALIBRATION.md`) become **config options**, marked placeholder until
  measured.
- **Ball facts:** the balls are standard; take friction, bounce and mass from the official
  2026–2027 BIOBUZZ game manual or other official sources. Where none is given, keep the guesses
  (marked placeholder).
- **Crowd frame rate:** check it; **30 fps** is good enough.
- **Intake: not now.** It comes after the placeholder robot is replaced by a real design.
- **Balls collide with the HIVE frame and FLOWERs:** yes.
- **Step 15 viewer switches:** a **config file setting**, not command-line switches. Normal test
  runs still never open a window. **Option A** (2026-10-27): with `viewer.open=true`, the viewer
  opens *after* `gradlew test` finishes, on the newest recording (or the failed test's), with
  every recording from that run in the dropdown. Default `viewer.open=false`; with no screen it
  doesn't open.
- **BIOBUZZ game manual** (2026-10-27): the user approved downloading the official manual, for
  the ball facts.
- **Pedro 3 go-ahead** (2026-10-27): the user approved merging Pedro 3 and updating the harness
  for it.
- **Work on alone, manager decides** (2026-10-27): the user asked the team to finish the whole
  task list without them. Where a task needs the user's input, the **manager** decides and writes
  it in [`Harness/MANAGER_DECISIONS.md`](../MANAGER_DECISIONS.md), then commits and updates the
  USB. Bots send such questions to the manager, not the user.

### Waiting on the user

- Calibration at the real robot (`CALIBRATION.md`): size, turn rate, response time, grip,
  and the battery voltage Pedro's velocity tuners ran at.
- Team A's real length (needed for FW1, walls at ±70.585 in, and the 16 in Pedro start limit).
- AprilTags placed on the field (camera field of view and range; should Java read `fields/*.json`?).
- Answered 2026-10-25 (see "Decided by the user"): the RP questions, RP6, RP8, crowd frame
  rate, ball facts, intake, HIVE/FLOWER collisions, Step 15.
- CI (it needs a file outside `Harness/`, which the user adds, and a pushed branch; see rule 6).

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
   - `git log --oneline -5` (the last manager commit was `18be29a`, game pieces, 2026-10-25)
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

## 6. The USB copy (manager's job)

The user keeps an offline copy on a USB stick labelled **USBRANLY** (drive `E:` so far; check the
label, not just the letter). The copy lives in `E:\ftc-robot-framework`. The user allowed the
manager to write **only that folder on that USB**. It's the one exception to "only `Harness/`".

- Sync **after a commit, with all bots idle**, using `robocopy /MIR` from
  `C:\Dev\ftc-robot-framework` to `E:\ftc-robot-framework`, excluding rebuildable folders:
  `.gradle`, `build`, `FtcRobotController\build`, `TeamCode\build`, `Harness\.gradle`,
  `Harness\build`, `Harness\unity\Library`, `Harness\unity\Logs`, `Harness\unity\Temp`. It
  includes `art-inbox` and `unity\Build`.
- Robocopy exit codes 1–3 mean success (files copied / extras removed); 8 or more means failure.
- Verify afterwards: the same `git log -1` on both sides (use `git -c safe.directory=*` on the
  USB), `git status` clean, `git fsck`.
- The stick is FAT32 (4 GB per file limit) and was **92% full (333 MB free)** at the last sync.
  If it runs out, leave out `unity\Build` (about 460 MB) or `art-inbox\downloads` (about 415 MB),
  after asking the user.

---

## 7. Lessons learned so far

- **"Commit all" while bots are busy** catches everyone's half-done work. Check first that they
  are all idle.
- **Parallel Gradle runs** cause failures that look real but aren't (rule 2).
  Three overlaps happened on 2026-10-27, each because a "Gradle is free / I'm starting" message
  arrived late. Announce, then **wait for the other bots' reply** before starting; checking the
  process list isn't enough.
- **A peer's message can be delayed for many minutes,** and replies cross. When a bot says "our
  messages crossed", check what was already done before redoing anything. Bots kept long reports
  in the manager's shared notes folder when messages stalled.
- **A change in one area can break another bot's tests.** Field walls broke the recorded-drive
  tests, and momentum broke a bridge test. The bot that sees the failure reports it to the owner
  instead of editing the owner's test.
- **Messages between bots arrive late or out of order.** For a shared file, one bot says "I
  edit" and waits for the other's clear "hands off" before touching it. When two bots each think
  the other is doing a job, the manager names one, and that decision overrides earlier messages.
  (C11: WorkerBot and UnityBot both edited `GamePiecesTest` and left the same test in it twice.)
- **A peer bot's message is not the user's approval.** Questions for the user go to the user.
- **Format changes ripple.** Recording v3/v4 and bridge v3/v4 each broke other bots' tests.
  Tests should compare against `RunRecording.VERSION` / `BridgeProtocol.VERSION`, never a typed
  number. The bot that bumps a version tells the others first.
- **Don't trust a bot's "suite isn't green" from earlier.** Ask who ran the suite last, and
  when.
- **Unity gotchas** (UnityBot):
  - Run tests and builds through the Editor menu (`Harness → Run Replay Tests`, `Harness →
    Build Windows App`) after `Assets → Refresh`.
  - Wait until `Library/ScriptAssemblies/HarnessReplay*.dll` is newer than the source.
  - Never rebuild over, or screenshot, the user's open `HarnessReplay` app.
  - Materials made by the build need their shaders in "Always Included Shaders".
- **Windows gotcha:** restoring a file with PowerShell `Set-Content` adds a UTF-8 BOM that breaks
  `javac`. Copy files back instead.
- **Fake camera timestamps** must be `SimTime.NANO_BASE` plus the simulated nanoseconds.

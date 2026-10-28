# Decisions the manager made for you

On 2026-10-27 you asked the bot team to finish the task list on its own, and wherever a task
needed your input, the manager was to pick what it thought best and write it down here for you to
check later. Each entry says what was decided, why, and how to change it if you disagree.

Nothing here is final. If you disagree with one, tell the manager (or any bot) and it gets
changed.

**Branch for this work:** `Team2-Testing-Pedro3` (local only, never pushed). `Team2-Testing`
stays exactly as it was at commit `960534a`, so you can always go back to it.

---

## D1 · Work on a new local branch (2026-10-27)

- **Decided:** all of this round's work goes on a new local branch, `Team2-Testing-Pedro3`, made
  from `Team2-Testing` at `960534a`.
- **Why:** the Pedro 3 merge brings in a large change from your teammates (28 files, Pedro 2 →
  Pedro 3). Keeping it on its own branch means `Team2-Testing` stays a known-good copy.
- **To change it:** if you're happy with the result, `Team2-Testing` can be moved forward to this
  branch later (ask the manager). To throw it away, switch back to `Team2-Testing`.

## D2 · Which Pedro 3 branch to merge (2026-10-27)

- **Decided:** merge `origin/Pedro-3-Implementation` (Nyxiney, 2026-10-27, "Tuning finished;
  mechanum drive fixed").
- **Why:** it's the newest, and it changes only 28 files. The other Pedro 3 branch,
  `origin/team-2-pedro-3-implementation`, changes 215 files, deletes about 25,000 lines and was
  uploaded through the GitHub website, so it's much riskier.
- **To change it:** say which branch to use instead.

## D3 · Which robot the harness simulates (2026-10-27)

- **Decided:** keep simulating **Team A**, using the Pedro 3 values in
  `robots/teamA/TeamAPedroConfiguration.java` (motors `frontLeft`, `frontRight`, `rearLeft`,
  `rearRight`; top speeds 62.61 / 49.76 in/s; 4-bar Pinpoint pods at x −10.0, y −6.25 in).
- **Why:** it's the robot every existing harness test, route and recording is built around, and
  its numbers are the same ones the harness uses today, so nothing changes by surprise. The newly
  tuned `pedro/Constants.java` robot (motors `fl`, `fr`, `bl`, `br`; 51.46 / 43.81 in/s;
  swing-arm pods) is used only by `KimCode/TeleopV1`, and it isn't clear which physical robot it
  is.
- **To change it:** once the config work (RP2) is done, switching robots only means changing the
  values in `Harness/harness.properties`. Tell the manager which robot, and a bot can do it.

## D4 · Order: config first, then Pedro 3 (2026-10-27)

- **Decided:** WorkerBot finishes RP2 (the runner and physics read `harness.properties`) on the
  Pedro 2 code first, with all tests passing. Only then is Pedro 3 merged and the harness fixed
  for it.
- **Why:** RP2 removes most of the harness's direct use of Pedro, so after it the Pedro 3 fix is
  much smaller, and each step can be tested on its own.

## D5 · How to fix the crowd if it runs below 30 fps (2026-10-27)

- **Decided:** if the Unity app with the full hall crowd averages under 30 fps, UnityBot uses the
  fix that changes the look least, in this order: GPU instancing / shared materials first, then a
  lower-detail model for distant people, then fewer people. The full crowd stays available in
  the Visuals settings; the default is whatever reaches 30 fps.
- **Why:** you said 30 fps is good enough, and you've put work into the people and the hall. This
  keeps them looking the same where possible and never removes the full crowd.
- **To change it:** pick a different default in the app's Visuals settings, or tell the manager.
  (If the crowd already runs at 30 fps or more, nothing is changed, and this entry says so later.)
- **Outcome:** not needed. UnityBot measured the full hall crowd at a steady **60 fps** (the
  vsync limit) in every heavy view on this PC (RTX 2060, Ultra quality), so nothing was changed.
  The app gained a measuring option, `-fpslog <file>`, documented in `unity/README.md`.

## D6 · Ball mass, bounce and friction (2026-10-27)

- **Decided:** keep today's guesses (marked placeholder) for ball mass, bounce and rolling
  friction. The official manual's sizes are now used (POLLEN 2.8 in, NECTAR 3.6 in, manual TU06
  §9.8, marked measured).
- **Why:** you said to use the manual or online sources and otherwise keep the guesses. The
  BIOBUZZ Competition Manual (TU06, already in `Harness/fields/sources/`) gives only the sizes,
  material and the AndyMark part numbers (am-5851 / am-5852). The manager counted AndyMark's own
  product pages for those parts as an acceptable online source, but they refused automated access
  (HTTP 429), and a web search found no published weight.
- **To change it:** weigh one POLLEN and one NECTAR (and, if you like, roll them on a tile), or
  paste the AndyMark weight; a bot then puts the numbers in, marked measured.

## D7 · Which robot each Live-mode OpMode uses (2026-10-27)

- **Decided:** the Unity bridge stops using its hand-written list of 5 OpModes. It lists every
  OpMode the harness finds by annotation (RP8), and each runs on the robot in
  `harness.properties`. With `pvi.extras=true`, OpModes in a "Team B" / "Team C" group or package
  keep using PVI's Team B / Team C robots.
- **Why:** this fits the drop-in goal (a new repo's OpModes appear in Unity with no list to edit)
  and keeps PVI's three-robot setup working.
- **To change it:** say which OpModes should use which robot; it's one rule in the bridge.
- **Refined (2026-10-27):** the rule looks at the OpMode's Driver Station **group** only ("Team B"
  / "Team C"), not its package, because no PVI OpMode lives in a `robots.teamB` / `robots.teamC`
  package today. The bridge, the viewer and recorded tests all share this one rule
  (`RobotConfigs.forOpMode`). OpModes for robots the harness doesn't have (e.g. `KimCode/TeleopV1`,
  motors `fl`/`fr`/`bl`/`br`) are listed too, and stop at INIT with the SDK's "missing device"
  message, as on a real robot.

## D8 · The viewer's "Drive: ..." list (2026-10-27)

- **Decided:** the viewer's live-drive dropdown lists the TeleOps the harness finds by annotation
  (like Unity's Live mode, D7), and a new config key, `viewer.defaultTeleOp`, names the one it
  starts in. PVI's file sets it to Team A's TeleOp, so the viewer opens exactly as it does today.
  If the key is empty, the viewer starts on the first TeleOp.
- **Why:** WorkerBot suggested keeping the hand-written list of Team A OpModes, which is simplest.
  But that list would break in another team's repo, against your drop-in goal. The key keeps
  today's behavior here and works anywhere.
- **To change it:** set `viewer.defaultTeleOp` in `harness.properties` to any TeleOp's name.

## Pedro 3 merge (2026-10-27)

- Merged `origin/Pedro-3-Implementation` into `Team2-Testing-Pedro3` as commit `0f9c602` (D1, D2).
  It changed only TeamCode and `build.dependencies.gradle`; no harness file. WorkerBot is fixing the
  harness for Pedro 3 (list W8).

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

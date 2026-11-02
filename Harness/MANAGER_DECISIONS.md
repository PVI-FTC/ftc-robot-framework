# Decisions the manager made for you

On 2026-10-27 you asked the bot team to finish the task list on its own, and wherever a task
needed your input, the manager was to pick what it thought best and write it down here for you to
check later. Each entry says what was decided, why, and how to change it if you disagree.

Nothing here is final. If you disagree with one, tell the manager (or any bot) and it gets
changed.

**Branch for this work:** `Team2-Testing-Pedro3` (local only, never pushed). `Team2-Testing`
stays exactly as it was at commit `960534a`, so you can always go back to it.

---

## Summary of the 2026-10-27 round

**Every task on the list is done**, committed as `3dfe4df` on `Team2-Testing-Pedro3` and copied
to the USB (USBRANLY).

**Final checks**

- Java: the full suite, run by the manager with all bots idle: **360 tests in 60 classes,
  0 failures**. 5 are skipped on purpose: KimCode's TeleOp (D10), and the three tool TeleOps in
  the drive check only (D14).
- Unity: **95/95**, from UnityBot's run (the manager didn't rerun Unity).
- Only `Harness/` changed, except the Pedro 3 merge you approved, which brought in your
  teammates' TeamCode changes.

**Read first: D12.** Three of Team A's Pedro routes (DECODE, Curving, Illegal Path) are last
season's paths. On the BIOBUZZ field they drive into the HIVE and can't finish, on the real field
too. The team needs to redraw them.

**What got done**

- **Your drop-in goal works.** Everything about a robot now lives in `harness.properties`, and
  Pedro is optional (only PVI's extras use it). WorkerBot proved it in a pretend "other team" repo
  (different motor names, no Pedro, a LinearOpMode). Following only
  [`BRING_YOUR_OWN_ROBOT.md`](BRING_YOUR_OWN_ROBOT.md), its suite passed: 169 tests, 0 failed.
- **Pedro 3** is merged, and the harness works with it, with no TeamCode edits.
- **New harness features:**
  - LinearOpMode support;
  - nine new sensor fakes: IMU, CR servo, distance, touch, color, analog, OTOS, dead wheels,
    Limelight;
  - finding OpModes by their annotations;
  - a smoke test that runs every OpMode and checks that TeleOps drive;
  - the viewer opening after tests (Step 15, option A).
- **Field realism:** the official ball sizes; balls bounce off the HIVE and FLOWERs; the robot is
  blocked by them, in both the harness physics and Unity's physics.
- **Unity:** a "needs Java 17+" message in Live mode; the crowd measured at 60 fps (nothing to
  fix); AprilTag sight lines; a HUD fix for long OpMode names.
- **Your extra request:** [`robot-recorder/`](robot-recorder/README.md) records a real-robot TeleOp
  drive as a harness recording, for replay or `makeTest`.

**Not checked yet (needs you or the robot)**

- Nothing has run on a real Java 17 or on the other PC yet.
- The viewer window and Unity Live mode weren't tried in the drop-in copy (they need a screen).
- The robot recorder hasn't run on a Control Hub.
- **Measurements still needed:** Team A's size (for FW1, the walls), calibration at the robot
  (`CALIBRATION.md`), and the ball weights (D6).
- Whether to move `Team2-Testing` forward to this branch is your call.

All three bots are idle with empty lists. The details of each decision follow.

---

## D1 · Work on a new local branch (2026-10-27)

- **Decided:** all of this round's work goes on a new local branch, `Team2-Testing-Pedro3`, made
  from `Team2-Testing` at `960534a`.
- **Why:** the Pedro 3 merge brings in a large change from your teammates (28 files, Pedro 2 →
  Pedro 3). Keeping it on its own branch means `Team2-Testing` stays a known-good copy.
- **To change it:** if you're happy with the result, `Team2-Testing` can be moved forward to this
  branch later (ask the manager). To throw it away, switch back to `Team2-Testing`.
- **User Reply:** `Team2-Testing-Pedro3` is fine for now. Keep it seperate though until further instructions.  `Team2-Testing-Pedro3` should have all the same abilities as `Team2-Testing` plus the compatablility with both Pedro 3 and Pedro 2.

## D2 · Which Pedro 3 branch to merge (2026-10-27)

- **Decided:** merge `origin/Pedro-3-Implementation` (Nyxiney, 2026-10-27, "Tuning finished;
  mechanum drive fixed").
- **Why:** it's the newest, and it changes only 28 files. The other Pedro 3 branch,
  `origin/team-2-pedro-3-implementation`, changes 215 files, deletes about 25,000 lines and was
  uploaded through the GitHub website, so it's much riskier.
- **To change it:** say which branch to use instead.
- **User Reply:** Manager made Correct Decision. To my knowledge `origin/Pedro-3-Implementation` is Team 2's main branch now.

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
- **Confirmed by you (2026-10-28):** only trust the term "Team A" when it's on your own branch.
  On this branch, the `teamA` robot code is your Team 2 robot, so D3 stands. IanKile007's team is
  now called **Team 1**; its branch `TeamA_TeleOp_Update` (all four drive motors reversed) is not
  yours, so the harness ignores it. Team 3 is theskier15's team.
- **User Reply:** Assume anything labeled Team A is not ours and should not be used for now. If the Term Team A is in a Team 2 Branch inform me first. The response claiming Team A is ours came from a mix up between Robot A and Team A.
- **Your later rule (2026-10-28) replaces that reply:** only trust "Team A" when it's on your own
  branch; IanKile007's team is "Team 1" (see the confirmation above).

## D4 · Order: config first, then Pedro 3 (2026-10-27)

- **Decided:** WorkerBot finishes RP2 (the runner and physics read `harness.properties`) on the
  Pedro 2 code first, with all tests passing. Only then is Pedro 3 merged and the harness fixed
  for it.
- **Why:** RP2 removes most of the harness's direct use of Pedro, so after it the Pedro 3 fix is
  much smaller, and each step can be tested on its own.
- **User Reply:**  Good

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
- **User Reply:** Note: When the avatars were downloaded the website said they were at 30fps

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
- **User Reply:** AndyMark store page has Pollen listed as 0.055 lbs and Nectar as 0.091 lbs

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
- **User Reply:** Sounds good. I plan to more fully flesh out having multiple bot save files.

## D8 · The viewer's "Drive: ..." list (2026-10-27)

- **Decided:** the viewer's live-drive dropdown lists the TeleOps the harness finds by annotation
  (like Unity's Live mode, D7), and a new config key, `viewer.defaultTeleOp`, names the one it
  starts in. PVI's file sets it to Team A's TeleOp, so the viewer opens exactly as it does today.
  If the key is empty, the viewer starts on the first TeleOp.
- **Why:** WorkerBot suggested keeping the hand-written list of Team A OpModes, which is simplest.
  But that list would break in another team's repo, against your drop-in goal. The key keeps
  today's behavior here and works anywhere.
- **To change it:** set `viewer.defaultTeleOp` in `harness.properties` to any TeleOp's name.
- **User Reply:** Good Idea.

## D9 · The robot can't drive through the HIVE frame or FLOWERs (2026-10-27)

- **Decided:** WorkerBot adds robot-vs-field-element collisions. The simulated robot is stopped by
  the HIVE frame's feet and lip and by the FLOWER brackets, the same way it's stopped by the field
  walls today (it slides along them, and a hit is logged as an event). It's switched on for
  BIOBUZZ and can be turned off in `harness.properties`.
- **Why:** after balls started bouncing off those parts, WorkerBot found that the robot itself
  still drove straight through them, carrying balls through the frame. You asked for balls to
  collide with the HIVE and FLOWERs; a robot passing through them would make those collisions
  look wrong.
- **To change it:** set the new switch to `false` in `harness.properties`.
- **User Reply:** Sounds good. I plan to implement an outtake system later so it is probably a good idea,if possible, to allow the hive to have collisions and tip. The tip can be caused by a preset

## D10 · OpModes the "every OpMode" smoke test skips (2026-10-27)

- **Decided:** the new smoke test (RP10) runs every OpMode the harness finds. A config key,
  `smokeTest.skipOpModes`, lists any it should skip. PVI's file lists **KimCode TeleopV1**. Skipped
  OpModes show as *skipped* in the test report with the reason "listed in
  smokeTest.skipOpModes", never silently.
- **Why:** KimCode TeleopV1 drives a different robot (motors `fl`/`fr`/`bl`/`br`) than the one the
  harness simulates (Team A, D3), so it can only fail with "missing device". The other options
  were a whole second robot profile (a bigger change), or leaving the test red.
- **To change it:** remove the name from `smokeTest.skipOpModes`. If the harness ever simulates
  that robot, the skip isn't needed.
- **User Reply:** This seems fine but I might need a bit more explaining.

## D11 · What another team deletes, since tests stay one set (2026-10-27)

- **Decided:** your rule stands: one test set, no split. So `BRING_YOUR_OWN_ROBOT.md` says
  exactly what a team does in another repo: set `pvi.extras=false` and delete the PVI test files,
  listed one by one (about 26 under `src/test`). The viewer's `Scenarios.java` would otherwise
  also break outside PVI's repo, so WorkerBot changes it: its generic scenarios come from the
  OpModes the harness finds, and PVI's own scenarios move into the PVI extras.
- **Why:** TestBot found that `pvi.extras=false` alone isn't enough. Those 26 test files use PVI's
  TeamCode classes and won't compile elsewhere. Deleting test files is easy and safe, but the
  viewer has to keep working, so it can't be one of the deleted files.
- **To change it:** if the delete list becomes a nuisance, the alternative is the test split you
  declined earlier (PVI's tests moved under the PVI extras, so `pvi.extras=false` is enough).
- **User Reply:** This is fine for now. In  the future thismight be modified to clear out the PVIstuff so no deleting is needed.

## D12 · Team A's Pedro routes drive into the HIVE (2026-10-27) ⚠️ worth reading

- **Found:** once the robot is stopped by the HIVE frame (D9), **3 of Team A's 4 Pedro routes
  can't finish on the BIOBUZZ field.** They are DECODE-season paths that cross the middle of the
  field, where the BIOBUZZ HIVE now stands:
  - **DECODE Route** hits the HIVE frame at 3.4 s and gets stuck. Its end point, Pedro (48, 81),
    is right on the blue HIVE's lip, so it can't be reached on this field at all.
  - **Curving Test** hits the frame at 3.6 s and is still trying at 30 s.
  - **Illegal Path** hits the frame at 2.9 s (17 times).
  - **LP-11 Pilot** (24 in) is fine.

  The real robot would be stopped the same way on a BIOBUZZ field. This is a route problem in
  TeamCode, not a harness bug. The harness can't change TeamCode, so the team needs to redraw
  these routes for BIOBUZZ.
- **Decided:** the existing route tests (and one battery test) keep checking the routes' path
  following, but with the field elements switched off, marked as DECODE-season paths. One new
  test pins the finding: it checks that these routes hit the HIVE on the BIOBUZZ field. When the
  team fixes a route, that test fails and says so, a reminder to switch the field elements back on
  for that route's test.
- **Why:** this keeps the harness's other checks working, and the problem stays visible instead of
  being hidden or leaving the suite red.
- **To change it:** once Team A has BIOBUZZ routes, a bot updates the route tests to run with the
  field elements on.
- **User Reply:** Lets discuss this togeather.

## D13 · Unity bridge tests work in any repo (2026-10-27)

- **Found:** WorkerBot rehearsed the drop-in in a pretend "other team" repo (different motor names,
  no Pedro, one LinearOpMode), following the guide exactly. The harness dropped in and its smoke
  test passed first time. But 5 Unity bridge test classes used PVI's OpMode names and failed there.
- **Decided:** UnityBot makes those bridge tests generic (they use the config's robot and the
  OpModes the harness finds, and only PVI-specific checks are skipped outside PVI), rather than
  adding them to the guide's delete list.
- **Why:** your goal is "drop in with minimal changes". A shorter delete list, and keeping the
  bridge tested in every repo, both serve it.
- **To change it:** nothing to do. (The guide also got 3 fixes from the rehearsal: setting
  `viewer.defaultTeleOp`, not keeping the original `MyFirstRobotTest`, and marking another
  robot's speeds as placeholders until measured.)
- **User Reply:** Sounds good.

## D14 · TeleOps that aren't meant to drive (2026-10-27)

- **Decided:** the smoke test now also checks that pushing the left stick powers a drive motor in
  each TeleOp. A config key, `smokeTest.notDrivingTeleOps`, lists TeleOps that aren't meant to
  drive. They skip only that check, shown as skipped with the reason "listed as not driving",
  and still get the "doesn't crash" check. PVI's file lists **Team A AprilTag Vision Test**,
  **Team A Camera Calibration Capture** and **Team A Pedro Diagnostic** (tool OpModes in the
  "Testing" group, which never read the sticks).
- **Why:** an explicit list is clear and works in any team's repo. Treating the group name
  "Testing" as special would be PVI-only, and the existing skip list would also drop their crash
  check.
- **To change it:** edit `smokeTest.notDrivingTeleOps` in `harness.properties`.
- **User Reply:** Might need more explaining.

## Pedro 3 merge (2026-10-27)

- Merged `origin/Pedro-3-Implementation` into `Team2-Testing-Pedro3` as commit `0f9c602` (D1, D2).
  It changed only TeamCode and `build.dependencies.gradle`; no harness file. WorkerBot is fixing the
  harness for Pedro 3 (list W8).

## Your answers to the manager's follow-up questions (2026-10-28)

- **D1, Pedro 2 and Pedro 3:** "compatible with both" means the **same harness must also work on
  robot code that still uses Pedro 2** (e.g. `master`). A bot tests that in a throwaway copy.
- **D6, ball weights:** enter them as measured, from the AndyMark store page: **POLLEN 0.055 lb =
  24.9 g** (am-5851), **NECTAR 0.091 lb = 41.3 g** (am-5852). Bounce and rolling friction stay
  placeholders.
- **D7, robot save files (later):** a `robots/` folder with **one folder per robot**, each holding
  its **config file next to its 3D model**. Robots are **created in Unity's Visuals screen**, which
  should be **renamed "Settings"**. Not started yet.
- **D9, HIVE tipping (later, with the outtake):** each colored HIVE sits on a bar that **tips like
  a seesaw when enough weight is in its upper basket** (check the manual for details). The tip can
  come from **Unity's physics** or from a **real-life estimate of which POLLEN/NECTAR combinations
  tip it**. Not started yet.
- **D10 and D14:** clear now; keep as they are.
- **D11:** decide later how another team avoids deleting PVI files; the delete list stays for now.
- **D12, old routes: both.** The team redraws the three routes for BIOBUZZ (TeamCode, the team's
  job), **and** the harness gets a **season setting** so old routes can still be tested on last
  season's DECODE field.

## D15 · Last season's DECODE field has walls only, for now (2026-10-28)

- **Decided:** for the season setting you chose in D12, last season's **DECODE field** becomes
  selectable again (BIOBUZZ stays the default), but **with walls and AprilTags only, no field
  elements**. The old DECODE-season routes are tested against the walls on that field.
- **Why:** there is no official DECODE field geometry on this PC (no DECODE manual or CAD; only
  BIOBUZZ's), and the bots never guess measurements.
- **To change it:** give the bots the official DECODE 2025-26 field CAD export and/or the DECODE
  Competition Manual, or OK them downloading the manual from FIRST's site. Then UnityBot adds the
  DECODE field elements as collision shapes.
- **Your answer (2026-10-28):** walls only is fine. DECODE stays without field elements.

## Your direction: who the harness is for (2026-10-28)

- **You said:** most if not all use of this harness will come from **teams using the PVI repo**
  (each on its own branch). Teams outside the PVI repo can have a small section, but it should
  in no way be a big part.
- **What changes:** `BRING_YOUR_OWN_ROBOT.md` is reworked so its main path is **"a PVI team
  setting up the harness on its own branch"**, and the "repo outside PVI" steps (the delete list,
  `pvi.extras=false`) shrink to a short section at the end. The bots' rules (`06-bot-team.md`) now
  say PVI teams come first.

## Your direction: DECODE is very low priority (2026-10-28)

- **You said:** DECODE is a very low priority, as it's an old field. The Pedro paths are just
  leftovers and will be updated.
- **What changes:** the season setting from D12 and the DECODE field work in D15 are **parked**
  (stopped, not built). The three old routes keep today's setup: their tests run with the field
  elements off, and one test flags that they hit the BIOBUZZ HIVE, as a reminder until the team
  updates them.

## Your direction: handover through GitHub (2026-10-30)

- **You said:** the likely handover is through GitHub, with other teams copying the `Harness`
  folder into their code.
- **What changes:** `BRING_YOUR_OWN_ROBOT.md` says to copy `Harness/` from Team 2's branch on
  GitHub into your own branch. Nothing is pushed until you say so.
- **Checked for GitHub:** no harness file is over GitHub's 50 MB warning or 100 MB limit (the
  largest is the moon-base rover model, 46.9 MB). The whole `Harness` folder is about 367 MB in
  git, mostly Unity art. Teams on the same PVI repo share identical files, so copying it into
  several branches doesn't multiply that.

## Your direction: one testing branch, old snapshot tagged (2026-10-30)

- **Done:** the old state (`960534a`, harness before the Pedro 3 round, robot code on Pedro 2) is
  kept as the git **tag `before-pedro3`**. **`Team2-Testing` was moved forward** to the same commit
  as `Team2-Testing-Pedro3` (no conflicts: the second branch only added to the first). Both names
  now point at the same work. Local only; nothing pushed. This replaces D1's "keep them separate".
- **Next (your request):** UnityBot shows the **goBILDA Starter Bot** as the robot in the viewer,
  as the first piece of the robot save-file plan from D7. It waits for your input before starting.

## D16 · Where the camera's point-of-view window goes (2026-11-01)

- **You asked:** tags should appear in the camera automatically, plus a smaller window showing
  the camera's point of view.
- **Decided:** the camera-view window goes in the **Unity app**, as a small picture-in-picture
  window. It renders what the camera would really see, from its real mount position and field of
  view, with the detected tags outlined. The Swing viewer keeps its "tags in view" card for now.
- **Why:** Unity already draws the field, robot and tags in 3D, so it can show the actual camera
  view. The Swing viewer only has a top-down drawing.
- **Who does what:** WorkerBot adds the camera settings (mount, field of view, range) to
  `harness.properties` and the recordings; TestBot makes the fake webcam see field tags
  automatically; UnityBot builds the window.
- **To change it:** say if you also want a camera view in the Swing viewer.

## Shelved for later: turn a recorded drive into an autonomous (2026-11-01)

- **Idea:** convert a recorded TeleOp drive (from the viewer, Unity, or the real robot with
  `robot-recorder`) into an autonomous.
- **Two ways, when you pick it up:**
  1. **Playback autonomous:** a generated OpMode that replays the recorded stick and button inputs
     into your TeleOp. Quick, but drifts on a real robot because nothing corrects the path.
  2. **Recording → Pedro path (suggested):** simplify the driven path into waypoints and curves
     and write a Pedro path/route that your autonomous follows. Pedro corrects itself with the
     Pinpoint, the harness can test it, and you can tune it. Intake/outtake actions could become
     marked steps. For real-robot drives, `robot-recorder` would also need to save the Pinpoint
     position.
- **Status:** shelved by you; nothing started.

## Your direction: published to GitHub (2026-11-01)

- **You said:** rename the branch to `VI_Arena-Harness` and push it to GitHub, and remove the push
  ban from the docs.
- **Done:** the branch `Team2-Testing-Pedro3` was renamed `VI_Arena-Harness` and pushed to
  `PVI-FTC/ftc-robot-framework`. The bots' rule is now "push only when the user asks" (never
  another team's branch, no force-push, no tags unless asked). The local tag `before-pedro3` and
  the local `Team2-Testing` branch were not pushed.

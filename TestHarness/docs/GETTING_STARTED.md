# Getting started with the test harness

The harness runs your team's **real robot code** on a laptop: no robot, no Control Hub, no
Driver Station. This page gets you from nothing to your first test in five steps. The details
for every step are in the [README](README.md); this page links to them.

You need **Android Studio** installed. The first time, you also need internet (it downloads
libraries).

Using another computer? You need Java 17 or newer (Android Studio's built-in Java is fine) and
internet the first time. See the README, [Running on another PC](README.md#running-on-another-pc).

Another PVI team (Team 1, Team 3, …) wanting the harness on **your own branch and robot**? Do
these five steps here first, then follow [BRING_YOUR_OWN_ROBOT.md](BRING_YOUR_OWN_ROBOT.md).

---

## Step 1: Run the tests

The tests check that the robot code does what it should. Run them all once to see that
everything works.

**From Android Studio (easiest)**

1. **File → Open**, and pick the `TestHarness\th_src` folder (not the whole repository).
2. Wait for the sync to finish (the bar at the bottom).
3. Open `src/test/java/pvi/harness/TeamATeleOpScriptTest.java`.
4. Click the green ▶ next to `class TeamATeleOpScriptTest`.

Green ticks mean the tests passed. A whole 2-minute match takes about a second, because the
harness runs on **simulated time**.

**From a terminal (PowerShell)**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
cd C:\Dev\ftc-robot-framework\TestHarness\th_src
.\gradlew.bat test
```

More ways to run tests: [README §2](README.md#2-run-the-tests).

---

## Step 2: Drive the robot on screen

1. Double-click **`TestHarness\view.bat`** (or, in Android Studio, pick **View recording** in the
   run menu at the top and click ▶).
2. A window opens with the field and Team A's TeleOp (**Team A Mecanum Drive**, the one
   `viewer.defaultTeleOp` in `harness.properties` names) waiting in INIT.
3. Click the window, then press **Enter** for PLAY.
4. Drive with **W A S D** (move) and the **Left / Right arrows** (turn). **Esc** stops.

A game controller (Xbox, PlayStation or Logitech) works too: plug it in **before** you pick a
drive. All the keys and buttons: [README, "Watching a run in the viewer
window"](README.md#watching-a-run-in-the-viewer-window).

---

## Step 3: Record a drive and turn it into a test

1. In the viewer, click **REC OFF** (top right) so it says **REC ON**, *before* pressing Enter.
2. Drive something you want to keep working, for example "drive forward, start the intake".
   Press **Esc** to stop.
3. Click **SAVE AS TEST**, keep or change the name, and click **OK**.
4. The window tells you where the new test is (`src\test\java\pvi\harness\recorded\`) and the
   command to run it. Run it: it passes, because it replays exactly what you did.
5. Open the file and find `// TODO: add your own checks here`. Add a check, for example:

   ```java
   assertEquals("Intaking", runner.telemetry().lastValue("Intake State"));
   ```

Now the test fails if someone breaks that behavior. (No viewer? `.\gradlew.bat makeTest` does
the same from the terminal.)

---

## Step 4: Read a failing test

When a test fails, Android Studio shows a red ✗, and the terminal prints something like this
(here for `MyIntakeTest` from [README §3](README.md#3-write-a-test)):

```
MyIntakeTest > tappingAStartsTheIntake() FAILED
    org.opentest4j.AssertionFailedError: expected: <Intaking> but was: <Holding>
        at MyIntakeTest.tappingAStartsTheIntake(MyIntakeTest.java:42)
```

Read it in this order:

1. **Which test:** `tappingAStartsTheIntake`. Test names are sentences, so this says what
   should happen: tapping A starts the intake.
2. **What went wrong:** the test **expected** `Intaking` **but** the robot code said `Holding`.
3. **Where:** line 42 of the test file. Click it to jump there.

Then ask: did the **robot code** change (a bug, so fix the robot code), or did the **behavior
change on purpose** (so update the test)? The test itself never decides that; you do.

If the message doesn't make sense, [README §7 Troubleshooting](README.md#7-troubleshooting)
lists the common ones.

---

## Step 5: Where to look next

| You want to… | Look at |
|---|---|
| Write a test by hand | [README §3](README.md#3-write-a-test), then copy `TeamATeleOpScriptTest.java` |
| Look up how to press a button, read a motor, check telemetry | [README §4 Quick reference](README.md#4-quick-reference) |
| Test an autonomous or a Pedro path | `TeamAAutoOpModeTest.java`, `physics/PedroRouteTest.java` |
| Test vision with pretend AprilTags | [README, "Vision: scripted AprilTags"](README.md#vision-scripted-apriltags) |
| Test sensors: IMU, distance, touch, color, OTOS, Limelight… | [README, "More sensors (RP7) and the sensor script"](README.md#more-sensors-rp7-and-the-sensor-script) |
| Test a `LinearOpMode` | [README, "LinearOpModes"](README.md#linearopmodes-runopmode) (nothing special: it just runs) |
| Check every OpMode at once | `EveryOpModeSmokeTest.java` (one quick run of each, plus "does the left stick drive?") |
| Use the harness on your own team's branch and robot | [BRING_YOUR_OWN_ROBOT.md](BRING_YOUR_OWN_ROBOT.md) |
| Turn a real-robot drive into a test | [robot-recorder](../th_src/robot-recorder/README.md) |
| Know what can't be tested yet | [README §6](README.md#6-what-can-be-tested-yet) |
| Make the simulated robot match the real one | [CALIBRATION.md](CALIBRATION.md) |

Two habits that make tests useful:

- **Make sure a test can fail.** After it passes, change the expected value on purpose and check
  it fails. If it still passes, it isn't checking anything.
- **One behavior per test**, named as a sentence: `holdingAButtonOnlyCountsAsOnePress`.

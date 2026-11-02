# Bring your own robot

This page is for a **PVI team (Team 1, Team 3, …)** that wants the test harness on **its own
branch** of the PVI repository, testing **its own robot**. You add one folder, change one settings
file, and run one test that tells you what's still missing. You don't need to know how the harness
works inside.

The harness in this folder is set up for **Team 2's robot** (the `teamA` code on Team 2's branch).
Your branch has the same PVI framework, so most of it works as it is; this page says what to
change for your robot. (Not on the PVI repo at all? See the short section at the end.)

If you haven't used the harness before, do [GETTING_STARTED.md](GETTING_STARTED.md) first, on
Team 2's setup. Then come back here.

**What the harness can simulate today:** a **mecanum** drive with a **goBILDA Pinpoint**, plus
motors, servos, CR servos, an IMU, distance, touch, color and analog sensors, an OTOS, dead
wheels, a webcam with scripted AprilTags and a Limelight 3A. Both iterative OpModes and
LinearOpModes run, on Pedro 2 or Pedro 3. Other drivetrains (tank, swerve) can't be tracked on the
field yet.

---

## Step 1: Put the `TestHarness` folder on your branch

The harness lives on **Team 2's harness branch on GitHub** (Team 2 will tell you its name). Copy
its `TestHarness` folder into the root of **your own** branch, next to `TeamCode`:

1. Get Team 2's branch onto your computer: on GitHub, pick that branch and use **Code → Download
   ZIP**, or check the branch out in a second folder (for example with GitHub Desktop).
2. Copy the whole `TestHarness` folder from it into the root of your branch:

   ```
   your-branch/
     TeamCode/
     FtcRobotController/
     build.dependencies.gradle
     TestHarness/      <- copied from Team 2's branch
       th_src/         (the Java test harness: harness.properties, src/, ...)
       VI_Arena/       (the Unity 3D app)
       docs/           (this guide and the others)
   ```

3. Commit the `TestHarness` folder on **your** branch, like any other change.

The folder is about **370 MB**, mostly Unity art for the 3D view. Copy it from GitHub (the ZIP or
a checkout), not from someone's working folder: a working folder also holds big files git ignores
(Unity's library, build output, the built VI Arena app). `TestHarness/.gitignore` already keeps
the harness's build output and Unity's generated folders out of git, so committing the folder
doesn't add them.

The harness only **reads** your code. It never changes anything outside `TestHarness/`, and nothing
in it is ever put on your robot. It builds with your branch's own libraries (whatever
`build.dependencies.gradle` lists), so it works on Pedro 2 and Pedro 3 branches alike.

---

## Step 2: Turn off Team 2's extras and remove Team 2's robot tests

The folder comes with checks and tests written for **Team 2's** robot. Do two things:

1. In `TestHarness/th_src/harness.properties`, set:

   ```properties
   pvi.extras=false
   ```

   The extras (`src/pvi*/`) compare the settings with Team 2's own Pedro 3 configuration, so they
   don't fit your robot (and don't compile on a Pedro 2 branch).

2. Delete these test files from `TestHarness/th_src/src/test/java/pvi/harness/`. They test Team 2's robot
   through the framework's `teamA` classes. Your branch has those classes too, so the tests
   compile, but they check how Team 2's robot behaves, and on another team's branch dozens of
   them fail. Delete these test files:

   ```
   GamepadSourceTest.java            HarnessSmokeTest.java
   RunnerAndMenuTest.java            SimulatedTimeTest.java
   TeamAAutoOpModeTest.java          TeamATeleOpScriptTest.java
   hardware/AprilTagVisionTest.java  hardware/AutomaticAprilTagsTest.java
   hardware/SensorsTest.java
   physics/BatteryTest.java          physics/FieldObstacleRecordingsTest.java
   physics/MecanumKinematicsTest.java
   physics/PedroRouteTest.java       physics/PositionReportTest.java
   physics/RealCodeMotionTest.java   physics/TrackCsvTest.java
   physics/WheelReaderTest.java      recorded/RecordedDriveToTestTest.java
   recorded/RecordedTeamATeleOpDrive.java
   runtime/GamePiecesRecordingTest.java  runtime/RunRecordingTest.java
   viewer/FieldDrawingTest.java      viewer/GamePiecesDrawingTest.java
   viewer/GamepadIndicatorTest.java  viewer/RecordingMotionTest.java
   viewer/SaveAsTestTest.java        viewer/TagsInViewCardTest.java
   viewer/TelemetryPanelTest.java    viewer/ViewerDropdownTest.java
   viewer/ViewerReplayTest.java
   ```

   Keep every other test: they test the harness itself, on whatever robot your settings describe.
   (A few of those also check Team 2's OpModes; they skip themselves when `pvi.extras=false`.)

---

## Step 3: Describe your robot in `harness.properties`

Open `TestHarness/th_src/harness.properties`. Every line has a comment saying what it is. Change these to
match **your** robot configuration (the names your code passes to `hardwareMap.get(...)`):

| Setting | What to put |
|---|---|
| `robot.name` | Your robot's name, for reports, for example `Team 3`. |
| `drive.frontLeft` … `drive.rearRight` | Your four drive motors' names. |
| `drive.frontLeft.direction` … | `FORWARD` or `REVERSE`: the direction setting that makes each wheel drive the robot **forward** on positive power, as on your real robot. |
| `pinpoint` | Your Pinpoint's name. No Pinpoint? Leave the name as it is: the harness adds a fake Pinpoint your code never uses, which does no harm. |
| `hardware.otherMotors` | Every other motor, comma-separated: `intake, arm, lift`. (Team 2's file has `intake`; replace it.) |
| `hardware.servos` | Every servo, comma-separated: `claw, wrist`. |
| `devices.imu`, `devices.crServos`, `devices.distanceSensors`, `devices.touchSensors`, `devices.colorSensors`, `devices.analogInputs`, `devices.otos`, `devices.limelights`, `devices.webcams` | The names of those devices on your robot, comma-separated. Leave a line empty if you have none. |
| `webcam.<name>.*` | One block per webcam in `devices.webcams`: where it sits (forward, left, up from the robot's center on the floor, the angles, 0, 0, 0 = level and looking forward), its field of view and picture size. Copy Team 2's block and change the numbers; mark guesses `placeholder`. `webcam.<name>.aprilTags=field` makes it see the field's tags from where the robot is; `script` sees only what a test scripts. |
| `devices.deadWheels` | Odometry pods plugged into motor encoder ports, if you have them (the file shows the format). |
| `drive.forwardSpeedInchesPerSecond` and the other numbers | Your robot's measured speeds, if you have them (for example from your Pedro tuners). Otherwise you may keep Team 2's numbers to start with, **but change `measured` to `placeholder`** on each of those lines: they were measured on Team 2's robot, not yours. |
| `viewer.defaultTeleOp` | The TeleOp the viewer drives when it opens: your TeleOp's Driver Station name, or leave it empty for the first TeleOp by name. (Team 2's file says `TeamATeleOp`.) |
| `smokeTest.skipOpModes` | Leave it empty for now (Team 2's file lists `KimCode TeleopV1`); see Step 4. |
| `smokeTest.notDrivingTeleOps` | Leave it empty for now (Team 2's file lists three of its tool OpModes); see Step 4. |

**Clear Team 2's own values** you don't need: anything in `hardware.*`, `devices.*` and the
`smokeTest.*` lines that names a Team 2 device or OpMode (for example
`devices.webcams=logitechVisionWebcam`, unless your robot has that webcam too).
A line ending in `\` carries on onto the next line (as `host.skipLibraries` does, which you keep):
if you ever empty such a value, delete its following line too, or the harness stops with "not a
harness setting".

Leave the other lines (`teamCode.*`, `host.*`, `field`) as they are: they're right for every PVI
branch. All the settings are explained in the
[README](README.md#the-config-file-harnessproperties).

---

## Step 4: Run the smoke test

The **smoke test** gives every OpMode on your branch one quick run: INIT, half a second of waiting,
PLAY, 2 seconds of running, STOP. Nobody presses anything. It only checks that nothing crashes.

In Android Studio, open `src/test/java/pvi/harness/EveryOpModeSmokeTest.java` and click the green
▶. Or in a terminal, from the `TestHarness\th_src` folder:

```powershell
.\gradlew.bat test --tests "pvi.harness.EveryOpModeSmokeTest"
```

You get **one result per OpMode**. A failing one says what to fix in a plain line, for example:

```
Arm Auto (ArmAuto) crashed during INIT: missing device "armMotor" (DcMotorEx): add it to harness.properties
```

That means your code asks for a motor called `armMotor` that the settings file doesn't list. Add
it to `hardware.otherMotors` and run again. Repeat until every OpMode passes, or until the only
failures are real bugs in your code. Those are worth knowing about!

Your branch also has **other teams' OpModes** (the framework's Team A, B and C ones, for
example). If one is written for a **different robot** than yours, list its Driver Station name in
`smokeTest.skipOpModes`. It then shows as **skipped** in the report, with that reason, instead of
failing.

Each **TeleOp** also gets a second run with the left stick pushed forward for a second; at least
one drive motor must get power. A TeleOp that isn't meant to drive (a camera or tuning tool)
fails that check with `pushing the left stick forward … didn't power any drive motor`. List its
Driver Station name in `smokeTest.notDrivingTeleOps`: it then skips only the drive check, and
still gets the crash check.

Common messages:

| Message | What to do |
|---|---|
| `missing device "x" (DcMotorEx)` or `(DcMotor)` | Add `x` to `hardware.otherMotors`. |
| `missing device "x" (Servo)` | Add `x` to `hardware.servos`. |
| `missing device "x" (IMU)`, `(DistanceSensor)`, `(TouchSensor)`, … | Add `x` to the matching `devices.…` line. |
| `RuntimeException: No OpMode selected!` at PLAY | A menu OpMode (for example Pedro 2's `SelectableOpMode`) that needs a choice during INIT; with nobody pressing anything it stops, as it would on the robot. List its name in `smokeTest.skipOpModes`, and test it in its own test with a `GamepadScript` that makes the choice. |
| `didn't call idle(), opModeIsActive(), sleep() …` | A LinearOpMode has a loop that never gives the SDK a turn, like `while (!gamepad1.a) { }`. Add `opModeIsActive()` to the loop. |
| any other exception | A crash in your code. The full error is printed under the line. |

---

## Step 5: Write your first real test

**Rename** `src/test/java/pvi/harness/MyFirstRobotTest.java` to a name of your own, for example
`Team3DriveTest.java`, and rename the class inside to match (in Android Studio: right-click the
class name, **Refactor → Rename**). Don't keep the original next to it: it looks for Team 2's
TeleOp and would fail. At the top, change

```java
private static final String MY_TELEOP = "Team A Mecanum Drive";
```

to your TeleOp's name, exactly as it appears in its `@TeleOp(name = "...")` line. Run it. It
pushes the left stick forward for one second and checks that the robot drove forward.

Then add your own checks where it says `TODO`. Every test has the same four steps:

1. **Script**: what the drivers do and when (`GamepadScript`), and what the sensors see
   (`SensorScript`).
2. **Set up**: which OpMode, on which robot.
3. **Run**: INIT, PLAY, some seconds, STOP.
4. **Check**: motor powers, telemetry lines, where the robot ended up.

The [README](README.md#3-write-a-test) has more examples, and the quick reference lists everything
you can script and check.

---

## Step 6: Record a drive on your real robot

Drive your real robot, and turn the drive into a test. [robot-recorder](../th_src/robot-recorder/README.md)
has two small robot-code files you copy into your `TeamCode`. Its example wraps Team 2's TeleOp;
follow its "Record a different TeleOp" part to wrap **yours**. Then copy the saved file to your
laptop and run `.\gradlew.bat makeTest` in the `TestHarness\th_src` folder.

---

## When something doesn't work

- **The build says a library is missing:** your `build.dependencies.gradle` has a library the
  harness can't use on a laptop. Add it to `host.skipLibraries` in `harness.properties`.
- **"The harness can't track this robot":** your drive isn't four mecanum motors with the names
  in `drive.frontLeft` and so on. Check the names, or (for another drivetrain) test without
  position checks for now.
- **The robot drives the wrong way in a test:** check the four `drive.*.direction` lines against
  your real robot, and how your TeleOp maps the sticks. `MyFirstRobotTest` says how far it went
  forward and sideways.
- **A Pedro or other autonomous path stops early in the harness:** the simulated robot is
  blocked by the season's field elements, like on the real field. A path drawn for another
  season's field may drive straight into this season's (Team 2's DECODE-season paths hit the
  BIOBUZZ HIVE). Redraw the path, or for a test of the path following itself, turn the obstacles
  off: `field.robotObstacles=false`, or `estimator.withoutRobotObstacles()` in one test.
- **Tests pass on your laptop but not on another computer:** see the README,
  [Running on another PC](README.md#running-on-another-pc).

---

## Not on the PVI repository?

The harness also works in any other FTC repository with a `TeamCode` folder. Do the steps above,
with these differences:

- **Step 1:** copy `TestHarness` next to your `TeamCode` folder in your own repository.
- **Step 2:** the same (`pvi.extras=false` and the same list of test files). Here those tests use
  PVI's `teamA` classes, which your code doesn't have, so they wouldn't even compile.
- **Step 3:** also check `teamCode.sourceDir` (where your robot code is; `../../TeamCode/src/main/java`
  for a normal FTC repository) and `teamCode.opModePackages` (the Java package your OpModes are in).

# Bring your own robot

This page is for a team that wants to use the test harness on **its own robot code**, in its own
FTC repository. You copy one folder, change one settings file, and run one test that tells you
what's still missing. You don't need to know how the harness works inside.

If you haven't used the harness before, do [GETTING_STARTED.md](GETTING_STARTED.md) first, on
this repository. Then come back here.

**What the harness can simulate today:** a **mecanum** drive with a **goBILDA Pinpoint**, plus
motors, servos, CR servos, an IMU, distance, touch, color and analog sensors, an OTOS, dead
wheels, a webcam with scripted AprilTags and a Limelight 3A. Both iterative OpModes and
LinearOpModes run. Other drivetrains (tank, swerve) can't be tracked on the field yet.

---

## Step 1: Copy the `Harness` folder into your repository

Copy the whole `Harness` folder next to your `TeamCode` folder, so your repository looks like
this. Copy it from a fresh **git clone** (or a downloaded ZIP) of PVI's repository, not from
someone's working copy: a working copy also holds big files git ignores (Unity's library, art
downloads, build output), about 4.5 GB instead of about 370 MB.

```
your-repo/
  TeamCode/
  FtcRobotController/
  build.dependencies.gradle
  Harness/          <- the copied folder
```

The harness only **reads** your code. It never changes anything outside `Harness/`, and nothing
in it is ever put on your robot.

---

## Step 2: Remove PVI's own robot tests

The folder you copied comes with tests written for **PVI's** robot (Team A). They use PVI's
TeamCode classes, so they won't compile with yours. Do two things:

1. In `Harness/harness.properties`, set:

   ```properties
   pvi.extras=false
   ```

   That leaves out the extras that read PVI's Pedro settings (`src/pvi/` and `src/pviTest/`).

2. Delete these test files from `Harness/src/test/java/pvi/harness/`. They all test PVI's own
   OpModes:

   ```
   GamepadSourceTest.java            HarnessSmokeTest.java
   RunnerAndMenuTest.java            SimulatedTimeTest.java
   TeamAAutoOpModeTest.java          TeamATeleOpScriptTest.java
   hardware/AprilTagVisionTest.java  hardware/SensorsTest.java
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
   (A few of those also check PVI's own OpModes; they skip themselves when `pvi.extras=false`.)

---

## Step 3: Describe your robot in `harness.properties`

Open `Harness/harness.properties`. Every line has a comment saying what it is. Change these to
match **your** robot configuration (the names you use in `hardwareMap.get(...)`):

| Setting | What to put |
|---|---|
| `teamCode.sourceDir` | Where your robot code is. `../TeamCode/src/main/java` is right for a normal FTC repository. |
| `teamCode.opModePackages` | The Java package your OpModes are in, usually `org.firstinspires.ftc.teamcode`. |
| `robot.name` | Your robot's name, for reports. |
| `drive.frontLeft` … `drive.rearRight` | Your four drive motors' names. |
| `drive.frontLeft.direction` … | `FORWARD` or `REVERSE`: the direction setting that makes each wheel drive the robot **forward** on positive power, as on your real robot. |
| `pinpoint` | Your Pinpoint's name. No Pinpoint? Leave the name as it is: the harness adds a fake Pinpoint your code never uses, which does no harm. |
| `hardware.otherMotors` | Every other motor, comma-separated: `intake, arm, lift`. (PVI's file has `intake`; replace it.) |
| `hardware.servos` | Every servo, comma-separated: `claw, wrist`. |
| `devices.imu`, `devices.crServos`, `devices.distanceSensors`, `devices.touchSensors`, `devices.colorSensors`, `devices.analogInputs`, `devices.otos`, `devices.limelights`, `devices.webcams` | The names of those devices on your robot, comma-separated. Leave a line empty if you have none. |
| `devices.deadWheels` | Odometry pods plugged into motor encoder ports, if you have them (the file shows the format). |
| `drive.forwardSpeedInchesPerSecond` and the other numbers | Your robot's measured speeds, if you have them (for example from Pedro's tuners). Otherwise you may keep PVI's numbers to start with, **but change `measured` to `placeholder`** on each of those lines: they were measured on PVI's robot, not yours. |
| `viewer.defaultTeleOp` | The TeleOp the viewer drives when it opens: your TeleOp's Driver Station name, or leave it empty for the first TeleOp by name. (PVI's file says `TeamATeleOp`, which your code doesn't have.) |
| `smokeTest.skipOpModes` | Leave it empty for now (PVI's file lists `KimCode TeleopV1`); see Step 4. |
| `smokeTest.notDrivingTeleOps` | Leave it empty for now (PVI's file lists three of its tool OpModes); see Step 4. |

**Clear PVI's own values** you don't need: anything in `hardware.*`, `devices.*` and
`smokeTest.skipOpModes` that names a PVI device or OpMode (for example `devices.webcams=logitechVisionWebcam`).
A line ending in `\` carries on onto the next line (as `host.skipLibraries` does, which you keep):
if you ever empty such a value, delete its following line too, or the harness stops with "not a
harness setting".

Leave the build lines (`host.dependenciesFile`, `host.skipLibraries`) as they are unless the
first build tells you a library is missing. All the settings are explained in the
[README](README.md#the-config-file-harnessproperties).

---

## Step 4: Run the smoke test

The **smoke test** gives every OpMode in your code one quick run: INIT, half a second of waiting,
PLAY, 2 seconds of running, STOP. Nobody presses anything. It only checks that nothing crashes.

In Android Studio, open `src/test/java/pvi/harness/EveryOpModeSmokeTest.java` and click the green
▶. Or in a terminal, from the `Harness` folder:

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

If one OpMode is written for a **different robot** than the one in your settings (say, last
year's robot), list its Driver Station name in `smokeTest.skipOpModes`. It then shows as
**skipped** in the report, with that reason, instead of failing.

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
| `didn't call idle(), opModeIsActive(), sleep() …` | A LinearOpMode has a loop that never gives the SDK a turn, like `while (!gamepad1.a) { }`. Add `opModeIsActive()` to the loop. |
| any other exception | A crash in your code. The full error is printed under the line. |

---

## Step 5: Write your first real test

**Rename** `src/test/java/pvi/harness/MyFirstRobotTest.java` to a name of your own, for example
`ArmTest.java`, and rename the class inside to match (in Android Studio: right-click the class
name, **Refactor → Rename**). Don't keep the original next to it: it looks for PVI's TeleOp and
would fail. At the top, change

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

**Want a test from a real drive?** Record a drive on your real robot with
[robot-recorder](robot-recorder/README.md), then turn the recording into a test with `makeTest`.

---

## When something doesn't work

- **The build says a library is missing:** your `build.dependencies.gradle` has a library the
  harness can't use on a laptop. Add it to `host.skipLibraries` in `harness.properties`.
- **"The harness can't track this robot":** your drive isn't four mecanum motors with the names
  in `drive.frontLeft` and so on. Check the names, or (for another drivetrain) test without
  position checks for now.
- **A Pedro or other autonomous path stops early in the harness:** the simulated robot is
  blocked by the season's field elements, like on the real field. A path drawn for another
  season's field may drive straight into this season's (PVI's DECODE-season paths hit the BIOBUZZ
  HIVE). Redraw the path, or for a test of the path following itself, turn the obstacles off:
  `field.robotObstacles=false`, or `estimator.withoutRobotObstacles()` in one test.
- **Tests pass on your laptop but not on another computer:** see the README,
  [Running on another PC](README.md#running-on-another-pc).

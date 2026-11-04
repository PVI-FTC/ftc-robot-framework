# FTC Test Harness

Run your **real, unchanged TeamCode** on a laptop, with fake motors, scripted gamepads and simulated
time. No robot, Control Hub or Driver Station needed.

The harness is its own project. It reads the repository's `TeamCode/src/main/java` but never
changes it, and it is not part of the robot build. Nothing here gets deployed to the robot.

**New here?** Start with [GETTING_STARTED.md](GETTING_STARTED.md): run the tests, drive on
screen, turn a drive into a test and read a failing test, in five short steps.

**Another PVI team (Team 1, Team 3, …)?** See [BRING_YOUR_OWN_ROBOT.md](BRING_YOUR_OWN_ROBOT.md):
put this folder on your branch, describe your robot in `harness.properties`, and run the smoke
test. (It has a short section for repositories outside PVI too.)

**Where things are.** The `TestHarness` folder holds:

| Folder | What |
|---|---|
| `TestHarness/th_src/` | the Java test harness: `build.gradle`, `harness.properties`, `src/`, `fields/`, `robots/`, `robot-recorder/`, and `build/` (test output and recordings) |
| `TestHarness/VI_Arena/` | VI Arena, the Unity 3D app: `src/` is the Unity project, `dist/` the built `VI_Arena.exe` ([its README](../VI_Arena/README.md)) |
| `TestHarness/docs/` | this guide and the others |
| `TestHarness/view.bat`, `TestHarness/VI_Arena.bat` | double-click to open the viewer window or VI Arena |

In this guide, a path like `src/test/java/...`, `harness.properties` or `build\recordings` is
inside `TestHarness/th_src/`, and every `.\gradlew.bat` command runs from `TestHarness\th_src`.

---

## 1. What you need

- **Java 17 or newer.** Android Studio's built-in Java (its `jbr` folder) is the easiest, and
  `view.bat` finds it by itself. Any other Java 17+ JDK works too (for example Eclipse Temurin
  from adoptium.net), with `JAVA_HOME` set to its folder.
- An internet connection the **first** time you build (to download Gradle and the libraries).

### Running on another PC

- Copy the whole repository (the `TestHarness` folder needs `../TeamCode` and
  `../build.dependencies.gradle` next to it; see "The config file" if yours are elsewhere).
- **Don't run it from a USB stick.** Copy the whole `ftc-robot-framework` folder onto the PC's
  own drive (for example `C:\Dev`) and run it from there. A build writes about half a gigabyte of
  files, and on a USB stick (FAT32 or exFAT) it's slow and often fails with "Unable to delete
  directory". `view.bat` checks this: on a removable or FAT32/exFAT drive, or one with under 1 GB
  free, it says so and asks before building (Enter stops, `Y` goes on).
- Install Java 17 or newer: Android Studio, or a JDK with `JAVA_HOME` set. `view.bat` looks
  for Android Studio's Java first (even if `JAVA_HOME` points somewhere else), then `JAVA_HOME`,
  then `java` on the PATH, and says plainly if it finds none or one older than 17. Typing
  `.\gradlew.bat ...` yourself uses `JAVA_HOME` (or `java` on the PATH) instead.
- Be online the first time, while Gradle downloads itself and the robot libraries. After that it
  works offline.
- Logitech F310/F710 controllers: set the switch on the back to **X** (XInput), not D, before
  plugging in.

---

## 2. Run the tests

### From a terminal (PowerShell)

Point the terminal at Android Studio's Java. This lasts until you close the terminal:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

Go to the harness folder:

```powershell
cd C:\Dev\ftc-robot-framework\TestHarness\th_src
```

Run every test:

```powershell
.\gradlew.bat test
```

Run one test class:

```powershell
.\gradlew.bat test --tests "pvi.harness.TeamATeleOpScriptTest"
```

Each test prints `PASSED` or `FAILED`. For a readable report, open
`TestHarness\th_src\build\reports\tests\test\index.html` in a browser.

### From Android Studio

1. **File → Open** and choose the `TestHarness\th_src` folder (not the main repo folder). It opens
   in its own window and does not affect the robot project.
2. If asked about the Gradle JDK, choose **jbr (bundled)**.
3. Wait for the sync to finish, then open a file in `src/test/java/pvi/harness/`.
4. Click the green ▶ next to a test class or a single test.

---

## 3. Write a test

Create a file in `src/test/java/pvi/harness/`, for example `MyIntakeTest.java`:

```java
package pvi.harness;

import org.firstinspires.ftc.teamcode.opmodes.teleop.TeamATeleOp;
import org.junit.jupiter.api.Test;

import pvi.harness.ds.Axis;
import pvi.harness.ds.Button;
import pvi.harness.ds.GamepadScript;
import pvi.harness.hardware.RobotConfigs;
import pvi.harness.runtime.OpModeRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MyIntakeTest {

    @Test
    void tappingAStartsTheIntake() {
        // 1. SCRIPT: what the operator does (times are seconds after PLAY)
        GamepadScript operator = new GamepadScript()
                .at(1.0).tap(Button.A);

        // 2. SET UP: which OpMode, on which robot
        OpModeRunner runner = new OpModeRunner(new TeamATeleOp(), RobotConfigs.teamA())
                .gamepad2(operator);

        // 3. RUN
        runner.init().start();
        runner.loopUntil(2.0);

        // 4. CHECK
        assertEquals("Intaking", runner.telemetry().lastValue("Intake State"));
        assertTrue(runner.hardwareMap().motor("intake").getPower() > 0);
    }
}
```

Every test follows the same four steps: **script → set up → run → check.**

---

## 4. Quick reference

### Scripting a driver (`GamepadScript`)

| You want | Write |
|---|---|
| Push the left stick forward | `.at(0.0).stick(Axis.LEFT_STICK_Y, -1.0)` (**up is negative**) |
| Let go of the stick | `.at(2.0).stick(Axis.LEFT_STICK_Y, 0.0)` |
| Strafe / turn | `Axis.LEFT_STICK_X`, `Axis.RIGHT_STICK_X` |
| Triggers | `Axis.LEFT_TRIGGER`, `Axis.RIGHT_TRIGGER` (0 to 1) |
| Tap a button | `.at(1.0).tap(Button.A)` |
| Tap several times | `.tap(Button.DPAD_DOWN, 3)` |
| Hold a button | `.at(1.0).hold(Button.RIGHT_BUMPER, 2.0)` |
| Press / release separately | `.press(Button.X)` … `.release(Button.X)` |
| Something 0.5 s after the last step | `.after(0.5)` |
| Act during INIT, before PLAY | `.duringInit().at(0.2).tap(Button.DPAD_DOWN)` (times are after INIT) |
| Let go of everything | `.at(5.0).releaseAll()` |

Give the script to the runner with `.gamepad1(script)` (driver) or `.gamepad2(script)` (operator).
For a quick test without a script you can also set fields directly:
`runner.gamepad1().left_stick_y = -1.0f;`

### Running (`OpModeRunner`)

| You want | Write |
|---|---|
| Step by step | `runner.init().start(); runner.loopUntil(3.0);` |
| Some init loops before PLAY | `runner.init().initLoop(10).start();` |
| An exact number of loops | `runner.loop(5);` |
| Run a bit longer | `runner.loopFor(1.5);` |
| Press STOP | `runner.stop();` |
| A whole TeleOp match (2:00) | `RunResult r = runner.run(MatchPeriod.TELEOP);` |
| A whole autonomous (0:30) | `RunResult r = runner.run(MatchPeriod.AUTONOMOUS);` |
| Custom lengths | `runner.run(initSeconds, matchSeconds);` |
| Different loop speed | `runner.loopPeriod(0.010);` (default 0.020 s) |

Step-by-step calls let an OpMode crash fail the test immediately. `run(...)` stores a crash in the
`RunResult` instead, the way the Robot Controller would show an error, so you decide what to check.

### Checking

| You want | Write |
|---|---|
| Motor power | `runner.hardwareMap().motor("frontLeft").getPower()` |
| Motor direction | `runner.hardwareMap().motor("frontLeft").getDirection()` |
| Brake / float | `runner.hardwareMap().motor("frontLeft").getZeroPowerBehavior()` |
| Servo position | `runner.hardwareMap().servo("claw").getPosition()` |
| Latest telemetry value | `runner.telemetry().lastValue("Drive State")` |
| Telemetry earlier in the run | `runner.telemetry().frames()` (each has `matchTimeSeconds()`) |
| Telemetry text on screen | `runner.telemetry().lastFrame().containsText("Select")` |
| Did it crash? | `r.crashed()`, `r.exception()`, `r.exceptionStage()` |
| Fail if it crashed | `r.assertNoException();` |
| Rumbles | `r.gamepad1Rumbles()` (time, duration, number of pulses) |
| Did the OpMode stop itself? | `r.stopRequestedByOpMode()` |
| Current simulated time | `runner.clock().sinceStart()` (seconds after PLAY) |

### Recording what happens every loop

To check how something changes over time (motor power during an autonomous, for example), record
it after every loop with `everyLoop`:

```java
List<Double> powers = new ArrayList<>();
OpModeRunner runner = new OpModeRunner(new TeamAAutoOpMode(), RobotConfigs.teamA())
        .everyLoop(r -> powers.add(r.hardwareMap().motor("frontLeft").getPower()));
runner.run(MatchPeriod.AUTONOMOUS).assertNoException();
```

`TeamAAutoOpModeTest.java` uses this pattern to check every step of the autonomous.

### Saving the robot's path for graphing (CSV)

Every runner estimates where the robot drives. To open that path in a spreadsheet, save it at the
end of a test:

```java
runner.run(MatchPeriod.AUTONOMOUS).assertNoException();
Path file = runner.positionEstimator().saveTrackCsv("myAutonomous");
System.out.println("Track saved to " + file);
```

The file goes to `TestHarness\th_src\build\tracks\myAutonomous.csv` and is replaced each time the test runs.
Names may use letters, digits, `-`, `_` and `.` only. Nothing is saved unless a test asks.
`TrackCsvTest.java` has examples.

**Track CSV format, version 1.** One header line, then one row per OpMode call, oldest first.
Decimals always use a dot, and lines end in `\n`.

| Column | Type | Unit | Meaning |
|---|---|---|---|
| `version` | whole number | — | Format version, `1`. It goes up whenever the columns change. |
| `since_init_s` | decimal, 3 places | seconds | Time since INIT. Plot against this to see the whole run. |
| `match_time_s` | decimal, 3 places | seconds | Time since PLAY; `0.000` for every row before PLAY. |
| `x_in` | decimal, 3 places | inches | Robot center, field frame: origin at the field center, edges at ±72. |
| `y_in` | decimal, 3 places | inches | Same as `x_in`, along the other axis. |
| `heading_deg` | decimal, 2 places | degrees | Counter-clockwise from +x, from -180 to +180. |

These are **estimates** from wheel powers (no slipping, instant speed changes). Team A's turn rate
is still a placeholder, so headings after turning are only rough until it is measured.

### Recording a whole run (JSON)

A run recording keeps everything that happened, after **every** OpMode call: pose, motor powers,
servo positions, the autonomous step, telemetry, plus rumbles. It is the file the desktop viewer
(and later Unity) replays. Attach a recorder **before** `init()`:

```java
OpModeRunner runner = new OpModeRunner(new TeamAAutoOpMode(), RobotConfigs.teamA());
RunRecorder recorder = RunRecorder.attachTo(runner);
runner.run(MatchPeriod.AUTONOMOUS).assertNoException();
Path file = recorder.save("teamAAutonomous");   // TestHarness\th_src\build\recordings\teamAAutonomous.json
```

`recorder.recording()` gives the same data as Java records (`RunRecording`) without saving a file,
and `RunRecordingJson.load(path)` reads a saved file back into them.
The recorder only watches, so a test passes or fails the same with or without it. The same run
always saves exactly the same bytes. A 30 s autonomous is about 0.8 MB. `RunRecordingTest.java`
has examples.

**Run recording format, version 5.** (Version 2 added `events`, version 3 added each loop's
`aprilTags`, version 4 added the game `pieces` in the header and each loop, version 5 added the
header's `cameras` and `field`. Older files, without them, still load.) UTF-8 JSON, lines end in `\n`. Numbers are rounded to 6
decimal places and written with a dot, without trailing zeros (`0.2`, `1`, `-3.141593`). Field
names match the `RunRecording` record components. Devices are arrays of `{name, ...}` objects, not
name-to-value maps, because some readers (Unity's JsonUtility) can't read maps. Distances are in
inches, angles in radians, times in seconds, unless a field says otherwise.

Top level:

| Field | Type | Meaning |
|---|---|---|
| `header` | object | What was run, on which robot (below). |
| `gamepad1Rumbles` | array of rumble | Rumbles sent to the driver's gamepad, oldest first. |
| `gamepad2Rumbles` | array of rumble | Rumbles sent to the operator's gamepad, oldest first. |
| `events` | array of event | Marked moments, oldest first, e.g. a controller disconnecting during a live drive. Usually empty. |
| `loops` | array of loop | One entry per OpMode call (init, each init_loop, start, each loop, stop), oldest first. Each entry is on its own line. |

`header`:

| Field | Type | Unit | Meaning |
|---|---|---|---|
| `version` | whole number | — | Format version, `4`. It goes up whenever any field changes. |
| `distanceUnit` | string | — | Always `"inches"`. |
| `angleUnit` | string | — | Always `"radians"`. |
| `timeUnit` | string | — | Always `"seconds"`. |
| `robotConfig` | string | — | Fake robot configuration name: `"teamA"`, `"teamB"`, `"teamC"`, `"driveOnly"`, or `"custom"` (set your own with `hardwareMap.named("...")`). |
| `opMode` | string | — | OpMode class name, e.g. `"TeamAAutoOpMode"`. |
| `period` | string or `null` | — | `"AUTONOMOUS"` or `"TELEOP"`, from the OpMode's `@Autonomous` / `@TeleOp` annotation; `null` if it has neither. |
| `loopPeriodSeconds` | number | s | Simulated time per OpMode call (default 0.02). |
| `startPose` | pose or `null` | — | Where the position estimate started; `null` if position wasn't tracked (robot without the four drive motors). |
| `driveModel` | drive model or `null` | — | Robot speeds and size used for the estimate; `null` if not tracked. |
| `pieces` | array of piece | — | Every game piece at INIT, in a fixed order (version 4). Empty if position wasn't tracked. |
| `field` | string | — | The field the run was on, `field=` in `harness.properties` (e.g. `"biobuzz-2026"`), so Unity and the viewer show that field and its tags (version 5). |
| `cameras` | array of camera | — | Each webcam's mount and view, from `webcam.<name>.<fact>` in the robot's `robots/<id>/camera.properties`, in the order of `devices.webcams` (version 5). Empty if the robot has no webcam. |

camera (version 5): `name` (string, the webcam's configuration name), then plain numbers:
`forwardInches`, `leftInches`, `upInches` (in, from the robot's center on the floor, robot frame:
forward, left, up), `yawDegrees`, `pitchDegrees`, `rollDegrees` (deg; all 0 = level, looking
forward; yaw + turns left, pitch + tilts up, roll + turns the picture clockwise seen from behind,
applied in that order about the camera's own axes), `horizontalFovDegrees`, `verticalFovDegrees`
(deg, edge to edge), `imageWidthPixels`, `imageHeightPixels` (px), `maxRangeInches` (in, farthest
tag it reads), `maxViewAngleDegrees` (deg, how far a tag may be turned away); `aprilTags` (string,
`"field"` or `"script"`, where the tags it saw came from: the webcam's real mode at the end of
the run, so `"script"` when a test scripted a webcam the settings file sets to `field`); and `placeholders` (array of strings: the names of those numbers that are guesses,
e.g. `["horizontalFovDegrees", "verticalFovDegrees", "maxRangeInches"]`).

drive model: `name` (string, e.g. `"Team A"`) and five model values: `forward` (in/s), `strafe`
(in/s), `turn` (deg/s), `length` (in), `width` (in). Each model value is:

| Field | Type | Meaning |
|---|---|---|
| `value` | number | The number, in `unit`. |
| `unit` | string | `"in/s"`, `"deg/s"` or `"in"`. Model values keep the unit the team measures them in. |
| `measured` | boolean | `true` if measured on the real robot, `false` for a placeholder still to measure. |
| `source` | string | Where the number came from, or why it is a placeholder. |

loop (one OpMode call):

| Field | Type | Unit | Meaning |
|---|---|---|---|
| `sinceInitSeconds` | number | s | Time since INIT. |
| `matchTimeSeconds` | number | s | Time since PLAY; `0` before PLAY. |
| `stage` | string | — | Which call this was: `"INIT"`, `"INIT_LOOP"`, `"START"`, `"LOOP"` or `"STOP"`. |
| `pose` | pose or `null` | — | Estimated robot position after this call; `null` if not tracked. |
| `motors` | array of `{name, power}` | — | Every motor, in configuration order. `power` is what the code set, -1 to 1. |
| `servos` | array of `{name, position}` | — | Every servo, in configuration order. `position` is 0 to 1, or `null` if the code never set one. |
| `autoStep` | string or `null` | — | The value of the `"Auto Step"` telemetry caption sent during this call (TeamAAutoOpMode sends it every loop); `null` if none was sent. |
| `telemetry` | array of `{lines, log}` | — | Every telemetry screen sent during this call, in order (usually one, sometimes none). `lines` and `log` are arrays of strings, as the Driver Station shows them. |
| `gamepad1` | gamepad | — | The driver's gamepad as the OpMode saw it during this call. |
| `gamepad2` | gamepad | — | The operator's gamepad as the OpMode saw it during this call. |
| `aprilTags` | array of AprilTag | — | The tags in each fake webcam's latest picture, i.e. what robot code could detect during this call (version 3). Empty with no webcam. |
| `pieces` | array of piece | — | Only the game pieces that moved more than 0.01 in or changed `state` since the previous loop entry (since the header's list, for the first entry), where they are after this call; in the header's order. Empty if none did (version 4). |

gamepad:

| Field | Type | Meaning |
|---|---|---|
| `buttons` | array of strings | Buttons held down: `"A"`, `"B"`, `"X"`, `"Y"`, `"DPAD_UP"`, `"DPAD_DOWN"`, `"DPAD_LEFT"`, `"DPAD_RIGHT"`, `"LEFT_BUMPER"`, `"RIGHT_BUMPER"`, `"LEFT_STICK_BUTTON"`, `"RIGHT_STICK_BUTTON"`, `"START"`, `"BACK"`, `"GUIDE"`, `"TOUCHPAD"`, in that order. Empty if none. |
| `leftStickX`, `rightStickX` | number | -1 (left) to 1 (right). |
| `leftStickY`, `rightStickY` | number | -1 (up) to 1 (down): the FTC convention, up is negative. |
| `leftTrigger`, `rightTrigger` | number | 0 (released) to 1 (fully pressed). |

pose: `xInches`, `yInches` (numbers, inches, FTC field frame: origin at the field center, edges at
±72) and `headingRadians` (number, counter-clockwise from +x, -π to +π).

AprilTag: `camera` (string, the webcam's configuration name), `id` (whole number), and where the
tag is from the camera lens, as the SDK's `ftcPose`: `rightInches`, `forwardInches`, `upInches`
(numbers, inches; x right, y forward, z up) and `yawDegrees`, `pitchDegrees`, `rollDegrees`
(numbers, degrees, as the SDK reports them). Range along the floor is `hypot(right, forward)` and
bearing (left positive) is `atan2(-right, forward)`.

piece (a game piece, a ball): `id` (string, the same all run, e.g. `"pollen-garden-red-1"`), `kind`
(string: `"pollen"`, 2.8 in, or `"red-nectar"` / `"blue-nectar"`, 3.6 in), `state` (string: `"field"`
loose on the tiles, `"flower"`, `"cell"`, `"alliance-area"`; `"held"` is reserved for an intake
later, and more states may be added without a version change), `xInches`, `yInches` (numbers,
inches, FTC field frame) and `zInches` (number, inches, the ball center's height above the tiles).
To know every piece at a loop, start from `header.pieces` and apply each loop's `pieces` in order.

event: `sinceInitSeconds` (number, seconds since INIT), `matchTimeSeconds` (number, seconds after
PLAY, `0` if before PLAY), `text` (string, what happened, e.g. `"Gamepad 1: controller
disconnected; inputs released, keyboard takes over"`).

rumble: `matchTimeSeconds` (number, seconds after PLAY, `0` if before PLAY), `totalDurationMs`
(whole number, milliseconds), `pulses` (whole number of separate buzzes).

Not recorded yet: a crash's error message (a crashed call adds no loop entry; check the test's
`RunResult`) and motor directions.

### Watching a run in the viewer window

The viewer replays runs in a window: a dropdown at the top, the field on the left, a side panel
on the right, playback controls at the bottom.

**Easiest:** double-click `TestHarness\view.bat`. It sets Java for you and opens straight into
**Drive: Team A TeleOp**, in INIT and waiting for Enter (pick a replay from the dropdown instead
any time).
Drag a recording (`.json`) onto `view.bat` to open that one instead. If something goes wrong, the
window stays open so you can read the message.

Or from PowerShell in `TestHarness\th_src\` (after the `$env:JAVA_HOME = ...` line from section 2):

```powershell
.\gradlew.bat viewRecording                                                   # drive Team A TeleOp
.\gradlew.bat viewRecording "-Pfile=build\recordings\ViewerReplayTest-teleOpDrive.json"
```

With a file, it replays that recording; without one, it starts the Team A drive. Close the window
to finish.

**From Android Studio:** open the `TestHarness\th_src` folder (not the repository root), pick **View
recording** in the run dropdown at the top, and click the green ▶. That shared run configuration
is stored in `TestHarness\th_src\.run\` and runs `viewRecording`, Java agent included. You can also
double-click **viewRecording** under **Gradle → ftc-harness → Tasks → harness**. (Running
`RecordingViewer`'s `main` directly also opens the window, but the "Run: ..." scenarios then need
`-javaagent:build\agent\harness-agent.jar` in the run configuration's VM options.)

**The dropdown** ("Drive / run") lists:

- **Drive: ...** Team A, B or C TeleOp, driven live from the keyboard (see "Driving live from
  the keyboard" below).

- **Run: ...** ready-made scenarios (`Scenarios.java`). With `pvi.extras=true` they are PVI's
  hand-made ones (`src/pviViewer/java/.../PviScenarios.java`): Team A autonomous, a TeleOp demo
  drive with both gamepads, a turn in place, a miswired front-left motor, and the rumble demo
  match, followed by a 30 s run of every other autonomous in TeamCode (for example "Team A Pedro
  Paths"), so a new autonomous shows up by itself. In any other repo there is one per OpMode the harness finds (`OpModeCatalog`): each
  autonomous runs for 30 s, each TeleOp stands still for 3 s (`smokeTest.skipOpModes` are left
  out). Picking one runs the real, unchanged OpMode in the harness right there (simulated time, so even
  a whole match takes about a second), saves it as `build\recordings\viewer-<name>.json`, and
  replays it. To add your own, copy a method in `PviScenarios.java` and add it to its `all()`.

**Replays** are kept apart from the dropdown. Click **REPLAYS...** (or press **Ctrl+O**) for the
Replays window: every recording in `build\recordings`, newest first, with its type (**Drive**
for your live drives, **Scenario** for dropdown runs, **Test** for files saved by tests), when it
was saved and its size. Type in **Filter** to search names, or use **ALL / DRIVES / SCENARIOS /
TESTS** to show one type. **Open** (or double-click, or Enter) replays it; **Open file...** picks
a recording from anywhere on the computer. The list is read fresh each time the window opens,
so recordings saved by tests you just ran show up. While a recording is replaying, the dropdown
reads "Replaying <file>".

**Driving live from the keyboard.** Pick **Drive: Team A TeleOp (keyboard)**. The real, unchanged
TeleOp starts in INIT. Press **Enter** for PLAY, drive, and press **Esc** to STOP (it also stops
by itself at 2:00, like a match).

**Saving a drive: the Record button** (top right). It is **off** by default: the drive is just
practice and is shown afterwards but **not saved**. Click it to **on** *before* pressing Enter,
and the drive is saved as `build\recordings\drive-<teleop>-<date>-<time>.json`. Its state at
PLAY decides, and it is locked until the drive ends. Either way, the drive then replays like any
recording (Space to play).
While driving, the side panel shows the layout and the time left, and the playback controls
are off.

| Gamepad 1 (driver) | Keys | Gamepad 2 (operator) | Keys |
|---|---|---|---|
| Left stick | W A S D (W = forward) | A B X Y | J K U I |
| Right stick X (turn) | Left / Right arrow | LB / RB | O / P |
| Right stick Y | Up / Down arrow | LT / RT | N / M |
| Sticks at 40% | hold Shift | D-pad up, down, left, right | 7 8 9 0 |
| A B X Y | F G R T | Sticks | not on the keyboard |
| LB / RB | Q / E | | |
| LT / RT (full press) | Z / C | | |
| D-pad up, down, left, right | 1 2 3 4 | | |

Click the window first so it has the keyboard. If it loses focus, every key is released.

**Field walls.** The robot can't drive out of the arena: the position estimate stops the robot
box at the field's walls (±72 in), at any heading, and a robot driving into a wall at an angle
slides along it. Each time it runs into a wall, the recording gets an event ("Robot hit the +x
wall", shown under **Events**) and the final-position report counts the hits. In your own tests,
`runner.positionEstimator().wallHits()` lists them; `withoutWalls()` lets the robot leave the
field, only for testing an off-field check.

**Driving with a controller.** Plug in, or pair over Bluetooth, an Xbox, PlayStation
(DualShock 4 / DualSense) or Logitech F310/F710 controller (switch on "X") *before* picking
**Drive: ...**. The first controller is gamepad 1 (driver) and the second is gamepad 2
(operator). A gamepad with no controller uses the keyboard.

**One controller runs both gamepads.** With only one controller connected, its **D-pad** works
the operator's face buttons (by position: up = Y, down = A, left = X, right = B), so one person
can drive and run Team A's intake. The D-pad then isn't sent to the driver, and the operator keys
(J K U I) still work too. The operator's gamepad picture still shows what gamepad 2 is pressing.
When a second controller connects, it takes over gamepad 2 and the D-pad goes back to the
driver. Buttons are by position, like the Driver Station: cross = A, circle = B, square = X,
triangle = Y. Stick movement under 5% is ignored, so a worn stick doesn't creep. The side panel
shows what drives each gamepad (for example `Gamepad 1: PS5 Controller   Gamepad 2: keyboard`).

If a wireless controller drops out mid-drive (battery, sleep, out of range), that gamepad
**releases everything at once** and falls back to the keyboard, so the robot never keeps
"holding" a stick. The window beeps and shows a message, and the moment is saved in the
recording's `events` (shown under **Events so far** in the side panel when you replay it). A
controller that reconnects is picked up again. Controllers are read with
[Jamepad](https://github.com/libgdx/Jamepad) (SDL); if it can't start, the side panel says why
and the keyboard still works.
Robot code still runs on simulated time (exactly 20 ms per loop); the viewer only waits
between loops so it feels real, so a saved drive replays exactly. In your own tests, a
`GamepadSource` passed to `runner.gamepad1(...)` / `gamepad2(...)` feeds live input the same way
(`GamepadSourceTest.java` has examples).

**Turning a drive into a test.** The easiest way: after a drive (or with any run on screen,
including a practice drive or a replay), click **SAVE AS TEST** next to REPLAYS.... Type a class
name (it starts as `RecordedTeamATeleOpDrive`) and click OK. A few seconds later the window says
where the new test went and the command that runs it (**Copy command** puts it on the
clipboard). It writes exactly the same test as `makeTest` below. The button is off while driving.

Or from the command line: drive with **Record on** (above), then, from `TestHarness\th_src\`:

```powershell
.\gradlew.bat makeTest                                                  # the newest drive-*.json
.\gradlew.bat makeTest "-Pfile=build\recordings\drive-teamA-teleop-20261023-181935.json"
.\gradlew.bat makeTest "-Pfile=..." "-PtestName=IntakeDriveTest"        # choose the class name
```

It saves a new test in `src\test\java\pvi\harness\recorded\` (for example
`RecordedTeamATeleOpDrive.java`) and prints the command to run it. An existing file is never
replaced: a second one becomes `...Drive2.java`. Open it and edit it like any other test:

- **Script:** what each gamepad did, **one line per change**, as normal `GamepadScript` lines
  (`s.at(1.24).hold(Button.A, 0.12);`). Sticks are rounded to two decimals and wobble under 0.05
  is left out, so a 30 s controller drive is a few hundred lines, not thousands. Inputs before
  PLAY come after `s.duringInit();`. Marked moments (a controller disconnecting) are comments.
- **Run:** the same OpMode, robot and number of loops before and after PLAY as the drive.
- **Check:** two safety checks every run should pass: motor powers stay within -1 to 1, and the
  robot never runs into a field wall. If your drive already broke one (for example you drove into
  the wall), it is written **turned off**, with the reason, so the new test starts green. Then a
  `// TODO: add your own checks here` spot.
- **Snapshot checks** (commented out): where the robot ended and the last Driver Station screen.
  Remove the `//` in front of the ones you want; they then fail if the robot code starts
  behaving differently. They are measured by replaying the test's own (rounded) inputs while
  the test is made, so they always pass at first.

`makeTest` runs the real OpMode once (with the Java agent, like tests), so it takes a few
seconds. `RecordedDriveToTestTest.java` shows the parts in action.

**What you see.** A dark "mission control" look, inspired by SpaceX's launch displays (colors
and fonts all live in `Theme.java`):

- **Header:** the OpMode, robot and file on the left; a small **T+ match clock** in the middle,
  with REPLAY or LIVE, the period, the stage (amber light = INIT, green = running, grey =
  STOP) and the loop number; the **REC** button on the right (a red dot by the clock while a
  live drive is being recorded).
- **Field:** a dark field with a hairline grid, the robot as a wireframe box (cyan edge =
  front) with its **four mecanum wheels** (each fills toward the front in cyan or the back in
  amber as far as its power, so a wheel spinning the wrong way stands out) and its **intake**
  across the front (cyan with inward chevrons while taking in, amber while ejecting), a white heading arrow, and the path so far in cyan. When the robot hits a wall, that wall **flashes red** for 0.4 s. The legend under it turns amber
  while the robot's size is a placeholder.
- **Telemetry column:** cards for the vehicle, the **position** (large X, Y and heading), the
  **motion** (speed in in/s, turn rate, and a bar showing how hard the robot is speeding up or
  slowing down, full at Team A's 200 in/s² traction limit; worked out from the recorded
  positions, so momentum shows), the autonomous step, a **gauge** for every motor (-1 to 1, filled from the center) and servo, the
  Driver Station screen **as it was at that moment**, and events (rumbles, controller
  connects and disconnects). While driving live, a LIVE card (what to press, recording,
  controllers) and the key layout come first.
- **Gamepads** (bottom right): held buttons light up (A, B, X, Y in their own colors, others
  white); the stick knobs move with the sticks and the triggers fill as they are pressed.

**Controls.** The slider jumps anywhere in the run. Buttons and keys:

| Key | Does |
|---|---|
| Space | Play / pause (at the end, starts over) |
| Left / Right | One loop back / forward (20 ms) |
| Shift + Left / Right | One second back / forward |
| Home / End | Start / end |
| `[` or `-` / `]` or `+` | Slower / faster (0.25x to 4x) |

The viewer replays the finished recording at real speed. It never runs or changes the test, so
test results are the same whether you watch or not, and `gradlew test` never opens a window.
Two ways to open one: `.\gradlew.bat viewRecording` (or `view.bat`), or set `viewer.open=true`
in `harness.properties` (Step 15, option A). Then, when `gradlew test` finishes (pass or fail),
the viewer opens on that run's newest recording, or, if a test failed, on the failed test's own
newest recording (tests save them as `<TestClass>-<name>.json`). Every recording is in the
dropdown. Close the window to let Gradle finish. On a computer with no screen it prints one line
and stays closed. The tests themselves still never open a window.

To save a still picture instead, use `FieldImage.savePng(recording, FieldImage.LAST_LOOP,
"myAutonomous")` (a loop index instead of `LAST_LOOP` picks an earlier moment). It goes to
`TestHarness\th_src\build\field-images\`.

### Watching a recording in Unity (3D)

The same recordings also replay in 3D in VI Arena, the Unity project in `TestHarness\VI_Arena\src\`
(Stage U1). Open that folder in Unity Hub (Unity 6000.3.14f1), open `Assets/VI_Arena/Replay.unity` and
press **Play**: the newest recording in `build\recordings` opens. N / P switch to newer / older
recordings; the other keys match the viewer window (Space, arrows, Home/End, `[` `]`), plus T for
top-down and F to follow the robot. Unity only reads the saved files; nothing connects to the
harness live yet. Details: [`VI_Arena/README.md`](../VI_Arena/README.md).

**Without the Unity Editor:** double-click `TestHarness\VI_Arena.bat` (or drop a `.json` on it) to
open the replay as a Windows app, `VI_Arena\dist\VI_Arena.exe`. Build that app once with
`VI_Arena\src\buildUnity.bat` (Editor closed, a few minutes, needs Unity installed), and again
after changing the Unity scripts. `VI_Arena.bat` offers to build it when it's missing. The built
app isn't in git: to give it to someone without Unity, `VI_Arena\package.bat` zips `dist\` for
Google Drive or a thumb drive.

### Season field files (`fields/`)

Each FTC season's field is a JSON file in `TestHarness\th_src\fields\`: `biobuzz-2026.json` (BIOBUZZ,
2026–27). DECODE (2025–26) is switched off: its file is in `fields\disabled\`, which Unity doesn't read
(move it back up to offer it again). Unity draws the chosen one on top of the plain
tiled field (Unity's **Visuals** screen, or G). The files are plain data with no Unity in them, so harness
Java (for example scripted AprilTags) can read the same numbers. **Format version 2** (version 1, the same
without `pieceObstacles`, still loads):

| Field | Type | Meaning |
|---|---|---|
| `format` | string | Always `"pvi-harness-field"` |
| `version` | int | `2`; goes up whenever the layout changes (2 added `pieceObstacles`, 2026-10-27) |
| `id` | string | The file name without `.json`, e.g. `"biobuzz-2026"` |
| `name`, `season` | string | `"BIOBUZZ"`, `"2026-2027"` |
| `sources` | list of strings | Where every number came from (manual version and figures, SDK method) |
| `frame`, `audienceSide`, `redWall`, `blueWall` | string | The coordinate frame in words. Both seasons are FTC "inverted square" fields: **+x toward the audience, +y from the red wall toward the blue wall**, z up, origin at the field center on the tiles (ftc-docs field coordinate system) |
| `accuracyNote`, `startingNote` | string | How exact the numbers are; what moving parts start like |
| `elements` | list | The fixed parts (below) |
| `aprilTags` | list | The AprilTags (below) |
| `pieceObstaclesNote`, `pieceObstacles` | string, list | What a ball rolling on the tiles can hit, for the harness's ball physics (below). Optional |

Each **element**: `id`, `name`; `shape` (`"box"` or `"cylinder"`); `xInches`, `yInches`, `zInches`
(the part's **center**, z = height above the tiles); `lengthInches` (along +x), `widthInches` (along
+y), `heightInches`; `diameterInches` (cylinders); `tiltDegrees` (a box is tilted first: positive
raises its +x end), then `headingDegrees` (turned counter-clockwise from +x); `color` (`"#RRGGBB"`);
`accuracy`: `"cad"` (measured from FIRST's official field CAD), `"nominal"` (the manual's dimensions,
±1 in), `"approximate"` (worked out from the manual's figures, can be a few inches off) or `"sdk"`.

Each **AprilTag**: `id` (int), `name`, `sizeInches` (the black square); `cluster` (the SDK's cluster
name for BIOBUZZ's 4-tag stickers, else null) and `clusterOffsetInches` `{x, y, z}` (the SDK's
position inside that cluster, copied exactly); `xInches`, `yInches`, `zInches` (the tag's center) and
`facing` `{x, y, z}` (unit vector the printed side points along), all in the field frame and **null
when the field position isn't known**; `moves` (true for BIOBUZZ's HIVE tags, which tip with the HIVE:
their pose is the starting one); `accuracy`. DECODE's tags also keep the SDK's
`sdkQuaternionWXYZ`.

Each **piece obstacle** (version 2) is something a POLLEN or NECTAR ball rolling on the tiles can
hit, besides the perimeter walls. It has straight upright sides from `bottomInches` to `topInches`
above the tiles: `id`, `name`; `shape` `"box"` (lying along the axes: `minXInches`, `maxXInches`,
`minYInches`, `maxYInches`) or `"cylinder"` (upright: `xInches`, `yInches`, `radiusInches`);
`bottomInches`, `topInches`; `accuracy`. The sides are where the CAD is at **ball-center height**
(1.4 to 1.8 in), where a rolling ball's widest point touches, so a 2D circle-against-box or
circle-against-circle test is enough. A ball of radius r can hit a shape only if `bottomInches` is
below its top (2 r). `topInches` 4.0 means "taller than any ball". BIOBUZZ has 18 (measured from the
official CAD on 2026-10-27; Unity's `PieceObstacleTests` checks every reachable side against the CAD
meshes):

- **HIVE frame**, each side: the foot bar's upright lip (along the side, |y| 24.16 to 24.36, 2.15 in
  tall) and a foot at each end where the leg stands (|x| 17.33 to 18.86). The rest of the foot bar is
  under 0.6 in and the HIVE baskets are above 30 in, so they are left out.
- **FLOWERs**: two small wall brackets each (0.5 to 3.75 in), and the FLOWER head as a cylinder
  (radius 3.13) that starts **3.75 in up**, so balls on the tiles roll under it. The 0.5 in base plate
  is left out (balls roll over it).

Where the numbers come from:

- **AprilTag IDs, sizes and cluster offsets:** the FTC SDK (`AprilTagGameDatabase`, Vision 12.0.0),
  the same library TeamCode uses. DECODE's goal tags (20, 24) are exact; the obelisk tags (21–23)
  have no field position in the SDK. BIOBUZZ's clusters have no field position in the SDK either,
  because they move, so their starting positions are worked out from the manual (`approximate`).
- **BIOBUZZ parts:** FIRST's official field CAD (measured on 2026-10-25) for the loading zones,
  gardens, frame base and top bar, flowers and all 16 AprilTag positions (`"cad"`). The rest comes
  from Competition Manual TU06, Section 9, and Figure 10-2 (starting tilt). The frame legs are still
  `approximate`: the CAD's legs also lean inward, which one tilt can't describe. Unity draws the
  CAD itself when it is in the project (`VI_Arena/README.md`, "The official field CAD").
- **DECODE parts:** not added yet (needs the DECODE manual or CAD); only its tags are in the file.

The `"field": "<id>"` in a run recording's header (version 5) makes Unity open that field.

### Live connection to Unity (Stages U2 and U3)

The harness can run TeamCode **live for Unity**: Unity connects, picks an OpMode and asks for one
OpMode call at a time; the harness runs it, **reads your game controller** (the same controller
code as the viewer's live drive), and answers with that moment's robot state, which Unity draws.
Start the harness side from `TestHarness\th_src\`:

```powershell
.\gradlew.bat unityBridge                    # listens on 127.0.0.1:7465
.\gradlew.bat unityBridge -Pport=7500
```

**Or skip the command:** Unity's **Live mode** button (or L) runs `gradlew.bat unityBridge` for
you, hidden, with its output in `build\unity-bridge.log`, and ends it again when switched off or
when Unity closes (`VI_Arena/src/Assets/VI_Arena/Runtime/HarnessLauncher.cs`).

It only accepts connections from this computer, serves one client at a time, and keeps running
for the next client until you press Ctrl+C. Then, in Unity (`VI_Arena/README.md`), open the Replay
chooser and pick **Live: Team A Mecanum Drive** (Team A's TeleOp, or another): Enter = PLAY, Esc = STOP, then **Save** keeps
the run in `build\recordings`. Plug the controller in before choosing; with one controller its
D-pad also works the operator's face buttons, as in the viewer. A controller that disconnects
releases every input at once and the moment is marked in the run's events.

**Unity's physics (Stage U3).** Set Unity's **Physics:** button (or press U) to **Unity** before
choosing a live OpMode. Unity's physics engine then moves the robot instead of the harness's own
`MomentumPhysics`:
- Speeding up, slowing down and coasting follow the same rules (`DriveLaw.cs`, a copy of
  `MomentumPhysics`).
- The walls and the shown season field's solid parts (BIOBUZZ HIVE frame, FLOWERS) really stop the
  robot, and it slides along them.
- Everything that reads the robot's position sees Unity's result, through the `PhysicsBackend` seam
  (`RemotePhysics` in `src/bridge`): the encoders, the Pinpoint, the recording and the wall-hit events.
- The harness's own physics stays the default, and every test uses it.

**Bridge message format, version 5.** (Version 5 added `robotObstacles` in `loaded`'s `robotPhysics` and the optional `obstacleContact` in `physicsState`, so Unity's physics stops the robot at the HIVE frame and FLOWERs like the harness's own and gives the same "Robot hit the HIVE frame" events. Version 2 added `input` in `load` and `controllers` /
`controllerProblem` in `frame`. Version 3 added `physics` in `load`, `physics` / `robotPhysics` in
`loaded`, `driveCommand` in `frame`, and `physicsState` in `step` / `play`. Version 4 added the game
pieces: `pieces` in the `loaded` header and in each frame's `loop`, as in run recording version 4.
An older client is refused.) A local TCP connection; each message is one line of UTF-8
JSON ending in `\n`, with a `type`. Unity sends a request and the harness sends **exactly one
reply**, then waits: the harness never runs ahead, so **Unity drives the clock** (about one
`step` per 20 ms of real time) and the same messages always give the same replies. Numbers and
gamepads use the same rules and layouts as the run recording (above). Every request can get an
`error` reply instead; the connection stays usable unless it says otherwise.

Unity sends:

| `type` | Other fields | Allowed when | Reply |
|---|---|---|---|
| `hello` | `version` (whole number, must be `5`); `client` (string, optional) | first | `welcome`; or `error` and the connection closes (wrong version); or `error` saying why the harness can't list its OpModes (e.g. `"The harness can't list its OpModes: Problems in harness.properties: ..."`), with the connection kept open |
| `load` | `opMode` (string, an `id` from `welcome`); `input` (string, optional: `"controllers"` = the harness reads the game controllers, as Unity does; `"messages"` = each `step`/`play` carries the gamepads; default `"messages"`); `physics` (string, optional: `"harness"` = the harness's own physics moves the robot, the default; `"unity"` = Unity's physics does, see `physicsState`) | after `hello`; again any time to start over | `loaded` |
| `init` | — | after `load` | `frame` (stage `INIT`) |
| `step` | `gamepad1`, `gamepad2` (gamepad, optional: missing means "same as before"; only with `"input": "messages"`, refused with `"controllers"`); `physicsState` (object, **required with `"physics": "unity"`, refused otherwise**: where Unity's physics put the robot after running the last frame's `driveCommand` for one loop period: `xInches`, `yInches` (numbers, field frame), `headingRadians` (number, counter-clockwise from +x), `forwardInchesPerSecond`, `leftInchesPerSecond`, `counterClockwiseRadiansPerSecond` (numbers, the robot's own frame), `wallContact` (string: `"+x"`, `"-x"`, `"+y"`, `"-y"`, two joined like `"+x/-y"`, or `""`), `obstacleContact` (string, optional: the field elements the robot is pressed against, named as the harness names them, `"HIVE frame"` or `"<which> FLOWER"` such as `"far FLOWER"`, joined by `/`, or `""`; it becomes a "Robot hit the ..." event)) | after `init` (runs init_loop) or `play` (runs loop) | `frame` |
| `play` | `gamepad1`, `gamepad2` (optional); `physicsState` (as for `step`) | after `init` | `frame` (stage `START`) |
| `stop` | — | after `init` or `play` | `frame` (stage `STOP`, `finished: true`) |
| `save` | `name` (string: letters, digits, `-`, `_`, `.`) | after `load` | `saved` |
| `bye` | — | any time | `goodbye`, then the connection closes |

The harness sends:

| `type` | Fields |
|---|---|
| `welcome` | `version` (whole number, `5`); `loopPeriodSeconds` (number, seconds per OpMode call, `0.02`); `opModes` (array of `{id, title, period}`: strings; `period` is `"AUTONOMOUS"` or `"TELEOP"`) |
| `loaded` | `opMode` (string, the id); `input` (string, `"controllers"` or `"messages"`); `matchSeconds` (number: `30` or `120`, when the harness stops the run by itself); `physics` (string, `"harness"` or `"unity"`); `robotPhysics` (object, or `null` when the robot isn't tracked: `forwardInchesPerSecond`, `strafeInchesPerSecond`, `turnRadiansPerSecond`, `lengthInches`, `widthInches`, and the momentum numbers `responseTimeSeconds`, `maxAccelerationInchesPerSecondSquared`, `forwardCoastInchesPerSecondSquared`, `lateralCoastInchesPerSecondSquared` (each `null` for a robot without momentum), `robotObstacles` (boolean: `harness.properties` `field.robotObstacles`; when true, Unity blocks the robot with the field file's `pieceObstacles`, as the harness's own physics does), and `start` `{xInches, yInches, headingRadians}`); `header` (the run recording's header object) |
| `frame` | `loop` (one run recording loop entry: time, stage, pose, motors, servos, autoStep, telemetry, gamepads); `gamepad1Rumbles`, `gamepad2Rumbles` (arrays of rumble: only the ones **new since the last frame**); `events` (array of event, new since the last frame, e.g. wall hits); `finished` (boolean: `true` once the run has stopped, by `stop`, at the end of the match, or because the OpMode stopped itself); `controllers` (array of 2 strings saying what drives gamepad 1 and 2, e.g. `["PS5 Controller", "none"]`, or `null` with `"input": "messages"`); `controllerProblem` (string, why controllers don't work, e.g. the library couldn't start; else `null`); `driveCommand` (object, what the drive wheels are told after this call, for Unity's physics: `frontLeft`, `frontRight`, `rearLeft`, `rearRight` (numbers -1 to 1, after motor direction) and `braking` (boolean: the drive motors brake at zero power); `null` without drive motors) |
| `saved` | `path` (string, the recording file under `build\recordings`) |
| `goodbye` | — |
| `error` | `message` (string, what was wrong); `crashed` (boolean, only when the OpMode threw: the run is over, as on the Robot Controller) |

A typical run: `hello` → `load` (with `"input": "controllers"`) → `init` → a few `step`s
(INIT_LOOP) → `play` → `step` every 20 ms → `stop` (or `finished: true`) → optionally `save` → `bye`.
With `"physics": "unity"`, before each `step` and `play` Unity runs its physics for one
`loopPeriodSeconds` with the newest frame's `driveCommand` and sends the result as `physicsState`.
The harness's position estimate takes that state at the same point in the loop where its own
physics would step, so sensors, the OpMode and the recording see it in the usual order.
Unity drives the clock, so a run only moves when Unity asks; with real controllers the drive is as
repeatable as the driver, and its saved recording replays exactly.

**Which OpModes Unity can pick** (`BridgeOpModes`, manager decision D7): every enabled `@TeleOp` and
`@Autonomous` OpMode the harness finds in `teamCode.opModePackages` (`OpModeCatalog`), with no list
to edit, in Driver Station name order. Each `id` is the class's simple name (`"TeamATeleOp"`; the
full class name if two OpModes share one) and each `title` is the Driver Station name (`"Team A
Mecanum Drive"`). Each runs on the robot `RobotConfigs.forOpMode` picks: the one in `harness.properties`;
with `pvi.extras=true`, OpModes in the Driver Station group "Team B" / "Team C" keep PVI's Team B /
Team C fake robots. An OpMode that needs devices the configured robot doesn't have fails at INIT with the
missing name, the same as on a real robot.

### The config file (`harness.properties`)

`TestHarness/th_src/harness.properties` holds every setting a team changes to use the harness in its own
FTC repo. `build.gradle` reads the build settings, and `pvi.harness.runtime.HarnessConfig` reads
the robot profile (`HarnessConfig.load()`). It's a plain Java `.properties` file: `key=value`
lines, `#` starts a comment, and a line ending in `\` carries on the next line. Paths are relative
to the `TestHarness/th_src` folder.

**Format version:** `config.version=6` (version 2, 2026-10-27, added the robot name, drive
directions, speeds, coasting, other motors, OpMode packages and `pvi.extras`; version 3 added
`viewer.defaultTeleOp` and the `devices.*` lists; version 4, 2026-11-01, added the
`webcam.<name>.<fact>` camera setups; version 5, 2026-11-03, added `devices.blinkers`; version 6, 2026-11-03, added `robot.id` and moved
`devices.webcams` and `webcam.*` to each robot's `camera.properties`).
This number goes up whenever keys are added, renamed or removed. Both the build and
`HarnessConfig` refuse a file with a different version and say which one they expect.

Reading fails early with one message listing every problem. That includes a missing key, a key
the harness doesn't know (usually a typo), a value that isn't a number, or a fact without
`measured` / `placeholder`.

| Key | Type | Meaning | PVI's value |
|---|---|---|---|
| `config.version` | integer | Layout of this file (see above) | `6` |
| `teamCode.sourceDir` | path | The robot code the harness compiles and tests (read only) | `../../TeamCode/src/main/java` |
| `teamCode.opModePackages` | package list, comma-separated (at least one) | Java packages searched for `@TeleOp` / `@Autonomous` OpModes (RP8) | `org.firstinspires.ftc.teamcode` |
| `host.dependenciesFile` | path | The robot build's library list. The harness uses every `implementation 'group:name:version'` (or `api`) line in it, with the same versions | `../../build.dependencies.gradle` |
| `host.skipLibraries` | `group:name` list, comma-separated (may be empty) | Libraries in that list that only work on Android, so the harness leaves them out | `org.firstinspires.ftc:Inspection`, `:Blocks`, `:RobotServer`, `:OnBotJava`, `androidx.appcompat:appcompat` |
| `drive.frontLeft`, `drive.frontRight`, `drive.rearLeft`, `drive.rearRight` | text | The mecanum drive motors' names in the robot configuration | `frontLeft`, `frontRight`, `rearLeft`, `rearRight` |
| `pinpoint` | text | The Pinpoint's name in the robot configuration | `pinpoint` |
| `robot.name` | text | The robot's name in reports, recordings and the viewer | `Team A` |
| `robot.id` | folder name | This robot's folder in `robots/`, which must exist. Its cameras are in `robots/<robot.id>/camera.properties` | `team-a` |
| `devices.imu`, `devices.crServos`, `devices.distanceSensors`, `devices.touchSensors`, `devices.colorSensors`, `devices.analogInputs`, `devices.otos`, `devices.limelights` | name lists, comma-separated (each may be empty) | More devices on the fake robot (RP7, TestBot's fakes: `FakeImu`, `FakeCRServo`, `FakeDistanceSensor`, `FakeTouchSensor`, `FakeColorSensor`), named as in the robot configuration | all empty (Team A's TeamCode uses none yet) |
| `devices.blinkers` | name list, comma-separated (may be empty) | A hub's status light (`FakeBlinker`), for robot code that does `hardwareMap.get(Blinker.class, "Control Hub")` | empty |
| `devices.deadWheels` | `name:parallel:inches` or `name:perpendicular:inches`, comma-separated (may be empty) | Dead-wheel odometry pods (TestBot's `FakeDeadWheel`): the motor port its encoder is plugged into, which way it rolls, and how far left (parallel) or forward (perpendicular) of the center it is | empty |
| `hardware.servos` | name list, comma-separated (may be empty) | The fake robot's servos (`FakeServo`), named as in the robot configuration | empty |
| `hardware.otherMotors` | name list, comma-separated (may be empty) | The fake robot's other motors (not the drive) | `intake` |
| `drive.frontLeft.direction` (and `.frontRight`, `.rearLeft`, `.rearRight`) | `FORWARD` / `REVERSE` | The direction setting that makes that wheel drive the robot forward on positive power, as tested on the real robot | left `REVERSE`, right `FORWARD` |
| `drive.forwardSpeedInchesPerSecond` | fact, in/s | Full-power forward speed (Pedro's xVelocity) | `62.61374213751846`, measured |
| `drive.strafeSpeedInchesPerSecond` | fact, in/s | Full-power sideways speed (Pedro's yVelocity) | `49.757958599901585`, measured |
| `drive.forwardCoastInchesPerSecondSquared` | fact, in/s² | How fast the robot slows when coasting forward on FLOAT (Pedro's forward zero-power acceleration, as a positive number) | `42.4832745291992`, measured |
| `drive.lateralCoastInchesPerSecondSquared` | fact, in/s² | The same, sideways | `50.60694780564594`, measured |
| `robot.lengthInches` | fact, inches | Robot length, bumper to bumper | `18`, placeholder |
| `robot.widthInches` | fact, inches | Robot width, bumper to bumper | `18`, placeholder |
| `drive.turnRateDegreesPerSecond` | fact, deg/s | Fastest spin at full power | `180`, placeholder |
| `drive.responseTimeSeconds` | fact, seconds | Drive motors' response time (63% of a speed change) | `0.10`, placeholder |
| `drive.maxAccelerationInchesPerSecondSquared` | fact, in/s² | Traction limit: fastest speed-up before the wheels slip | `200`, placeholder |
| `drive.speedVoltage` | fact, volts | Battery voltage the drive speeds were measured at | `12.5`, placeholder |
| `viewer.defaultTeleOp` | text (may be empty) | The TeleOp the viewer drives when it opens (`view.bat`, `gradlew viewRecording`): a Driver Station name or a class's simple name. Empty means the first TeleOp by Driver Station name; an unknown name stops with a message listing the TeleOps. The live-drive dropdown lists every `@TeleOp` (`OpModeCatalog`), this one first | `TeamATeleOp` |
| `smokeTest.skipOpModes` | Driver Station name list, comma-separated (may be empty) | OpModes TestBot's run-every-OpMode smoke test leaves out (for example one written for a different robot) | empty |
| `smokeTest.notDrivingTeleOps` | Driver Station name list, comma-separated (may be empty) | TeleOps that aren't meant to drive (camera or tuning tools): the smoke test's left-stick drive check skips them; they still get the crash check | `Team A AprilTag Vision Test, Team A Camera Calibration Capture, Team A Pedro Diagnostic` |
| `pvi.extras` | `true` / `false` | Also build PVI's extras, `src/pvi/java` and `src/pviTest/java` (below). Set `false` in any other repo | `true` |
| `field.robotObstacles` | `true` / `false` | The HIVE frame and FLOWERs stop the simulated robot like the walls do (see "How motion is simulated", step 6) | `true` |
| `field` | text | The season field: a file in `fields/` without `.json` | `biobuzz-2026` |
| `viewer.open` | `true` / `false` | Step 15, option A: open the viewer after `gradlew test` finishes, on the run's newest (or failed) recording; skipped with no screen. The tests themselves never open a window | `false` |

**A second robot:** an OpMode written for a different robot gets a robot profile,
`th_src/robots/<id>/drive.properties` (`pvi.harness.runtime.RobotProfile`, details in
[`th_src/robots/README.md`](../th_src/robots/README.md)). It uses the same keys, only the robot's and
only the ones that differ, plus `forOpModes`. `RobotConfigs.forOpMode` picks it, in tests, the
viewer and Live mode alike, and `RobotConfigs.profile("<id>")` makes that robot directly. PVI's is
`robots/kimcode` (TeleopV1, SimpleLeave). A fake robot remembers its settings
(`FakeHardwareMap.settings()`), so the runner reads its wheels and drive model from there.

**Cameras belong to each robot (D21):** a robot's webcams are set only in
`th_src/robots/<id>/camera.properties`, with the keys below. `<id>` is `robot.id` for the main robot
and the folder's name for a profile. No file, or an empty `devices.webcams`, means no webcam, and a
robot never gets another robot's cameras. `devices.webcams` or `webcam.*` in `harness.properties` or a
`drive.properties` stops with a message saying where to move them; a misspelled key or a missing fact in
`camera.properties` stops too, naming the file.

| Key in `camera.properties` | Type | Meaning | Team A's value |
|---|---|---|---|
| `devices.webcams` | name list, comma-separated (may be empty) | The robot's webcams (`FakeWebcam`), named as in the robot configuration | `logitechVisionWebcam` |
| `webcam.<name>.forwardInches`, `.leftInches`, `.upInches`, `.yawDegrees`, `.pitchDegrees`, `.rollDegrees`, `.horizontalFovDegrees`, `.verticalFovDegrees`, `.imageWidthPixels`, `.imageHeightPixels`, `.maxRangeInches`, `.maxViewAngleDegrees`, and `.aprilTags` | facts (any number for the mount; field of view between 0 and 180; whole-number picture size; range above 0; view angle above 0, at most 90); `aprilTags` is `field` or `script` | Required for **each** webcam in `devices.webcams` (`CameraSetup`; keys for a webcam that isn't listed are refused). The mount is from the robot's center on the floor: forward, left (negative: right) and up. Angles 0, 0, 0 mean level and looking straight forward; yaw + turns the lens left, pitch + tilts it up, roll + turns the picture clockwise as seen from behind, applied in that order about the camera's own axes. Field of view edge to edge, picture size, the farthest distance a tag is still read, and how far a tag may be turned away and still be read. `aprilTags=field`: the webcam sees the field's tags that are in view (TestBot's T14); `script`: only a test's `AprilTagScript`. They go into each run recording's header (`cameras`) for Unity and the viewer | `logitechVisionWebcam`: forward 4.875, left -6.875, up 19, yaw 0, roll 0, 640 × 480 (measured from `TeamAAprilTagVisionRobot`); pitch 18 (placeholder: tilted up in the config to see the HIVE's raised tags, UnityBot C24; TeamCode still says level). `harness.properties` owns the mount: PVI's `TeamACameraMatchesTeamCodeTest` prints a note for a mount number chosen differently from TeamCode (and refuses one whose source still claims TeamCode), but keeps the picture size strict; field of view 65.9 × 51.8 degrees (placeholder: Logitech's 78 degree C920 diagonal split for 4:3) range 120 in and view angle 60 degrees (placeholders); `aprilTags=field` (the user wants tags to appear by themselves; tests that need a script choose it) |

A **fact** is written `number | measured or placeholder | where it came from`, for example
`robot.lengthInches=17.5 | measured | tape measure at the robot, 2026-10-30`. The number must be
above zero. Write `measured` only after measuring on the real robot ([CALIBRATION.md](CALIBRATION.md)).
`HarnessConfig` turns each fact into a `ModelValue`, so reports show guesses as `TO MEASURE`.

The test task passes the libraries it really uses to the tests as the system property
`harness.robotLibraries` (`group:name:version`, comma-separated, in the host file's order).

**Who reads it (RP2):** the runner, physics and fake robot get every robot fact from this file:
`WheelReader`, `DriveModels`, `Dynamics` and `RobotConfigs` (each has a `fromConfig(HarnessConfig)`,
and their `teamA()` methods read `harness.properties`). None of them uses Pedro or TeamCode's
robot classes, so the core builds in any repo.

**PVI extras (`pvi.extras=true`):** `src/pvi/java` holds `TeamAPedroHarnessAccess`, which reads
Team A's Pedro configuration, and `src/pviTest/java` holds tests that use it:
`TeamAConfigMatchesPedroTest` checks that the speeds, coasting values, motor names and
directions, Pinpoint name and other motor names here still equal Team A's Pedro configuration
and TeamCode's hardware wrappers. After a re-tune it fails and names the line to update.
`PedroMecanumTest` checks that Pedro 3's own mecanum drivetrain, built from Team A's
configuration, makes every simulated wheel push forward. (Pedro 3: the access file reads Team A's
`ForesightConfig`, `MecanumConfig` and `PinpointConfig`.) In another repo, set `pvi.extras=false`
and type your own robot's numbers in.

### Finding the robot's OpModes (`OpModeCatalog`)

The harness can list TeamCode's OpModes by itself, like the Driver Station menu: every class
marked `@TeleOp` or `@Autonomous` in the packages named by `teamCode.opModePackages`, leaving out
`@Disabled`, abstract and non-OpMode classes. Add an OpMode to TeamCode and it is listed, with no
list to edit.

```java
List<OpModeCatalog.Entry> opModes = OpModeCatalog.fromConfig(HarnessConfig.load());
// or OpModeCatalog.find(List.of("org.firstinspires.ftc.teamcode"))
for (OpModeCatalog.Entry e : opModes) {
    System.out.println(e.name() + " (" + e.group() + ", " + e.period() + ")");
}
OpModeRunner runner = new OpModeRunner(opModes.get(0).newOpMode(), RobotConfigs.teamA());
```

Each `Entry` is plain data: `className`, `name` (the class's simple name if the annotation has
none, as the SDK does), `group`, `period` (`TELEOP` or `AUTONOMOUS`) and `preselectTeleOp`
(empty for a TeleOp). The list is sorted by name, so it's the same every run. Classes are loaded
without running their `static` code. A package with no compiled classes (usually a typo) fails
with a message naming it. `LinearOpMode`s are listed too, and the runner runs them (next section).

An OpMode that **can't be loaded** on this computer (for example a library it needs is missing)
is never dropped quietly. `OpModeCatalog.scan(packages)` returns a `Scan` with the `opModes` and
a `leftOut` list (`className`, `reason`) for every class marked `@TeleOp`/`@Autonomous` that
couldn't load, so a menu can show it. `find` and `fromConfig` print a `Harness: ... is left out of
the OpMode list: <reason>` warning for each one.

### LinearOpModes (`runOpMode()`)

`OpModeRunner` runs a `LinearOpMode` the same way as an iterative one: the same `init()`,
`initLoop(n)`, `start()`, `loop(n)`, `stop()` and `run(...)`, and the same results. Nothing
changes in the test:

```java
RunResult result = new OpModeRunner(new MyLinearAuto(), RobotConfigs.teamA()).run(MatchPeriod.AUTONOMOUS);
```

How it works: `runOpMode()` runs on one extra thread, but only one thread runs at a time.
Each runner step lets the OpMode run until it reaches one of the SDK's waiting calls (a *sync
point*), then simulated time moves on one loop (20 ms by default):

| In `runOpMode()` | Simulated time |
|---|---|
| `waitForStart()` | one loop each time it checks for PLAY, so it waits through every init loop |
| `opModeIsActive()` (while running), `idle()` | one loop per call, so `while (opModeIsActive())` makes one pass per loop |
| `sleep(ms)`, `Thread.sleep(ms)` | exactly `ms` of simulated time: `sleep(1000)` is 50 loops |
| `isStarted()`, `isStopRequested()`, `opModeInInit()` | one loop per 2 checks, so `while (opModeInInit())` makes one pass per init loop |

- **PLAY** makes `waitForStart()` return and `isStarted()` true.
- **STOP** makes `isStopRequested()` true and ends a `sleep` early, as on the robot (the thread is
  interrupted). The OpMode then gets up to 50 more sync points to finish its loops, without time
  moving; if it's still running, the step fails with a message.
- When `runOpMode()` returns by itself, the OpMode stops, as on the robot, and a `run(...)` ends
  early (`RunResult.stopRequestedByOpMode()`).
- A **crash** in `runOpMode()` goes into the `RunResult` (or to the test, in step-by-step use), with
  the stage it happened in.
- **Watchdog:** a loop that never calls the SDK, like `while (!gamepad1.a) { }`, would stop
  simulated time forever. After 3 s of real time the step fails with a message saying so. That
  thread can't be stopped from outside, so it keeps running until the tests end.
- Every run gives exactly the same result, because the two threads never run at the same time.

This needs the harness's Java agent (always loaded by `gradlew test`), which sends those SDK
calls to `SimTime` and on to `runtime/LinearOpModeDriver`. On every other thread, and in
iterative OpModes, `Thread.sleep` and `Thread.yield` still do exactly what they did before. The
tests are in `runtime/LinearOpModeTest`.

### Robot configurations (`RobotConfigs`)

| Method | Devices |
|---|---|
| `RobotConfigs.teamA()` | the robot in `harness.properties`: the four drive motors, the other motors and the goBILDA Pinpoint (PVI: `frontLeft`, `frontRight`, `rearLeft`, `rearRight`, `intake`, `pinpoint`) |
| `RobotConfigs.robot(config)` | the same, for any `HarnessConfig` |
| `RobotConfigs.teamB()` / `teamC()` | same as Team A for now |
| `RobotConfigs.driveOnly()` | the four drive motors (names from `harness.properties`) |
| `.without("intake")` | remove a device to test what happens when it is missing |

Need something else? Build your own:

```java
FakeHardwareMap hw = RobotConfigs.teamA();
hw.addServo("claw");
hw.addMotor("lift");
hw.addPinpoint("odometry");
hw.addWebcam("logitechVisionWebcam");   // sees scripted AprilTags (see "Vision" below)
```

Every fake robot also has the Control Hub's **battery voltage** (12.5 V), which Pedro reads:
`hw.batteryVoltage().setVoltage(11.0)` tests a tired battery.

### Sensors: motor encoders and the Pinpoint

Before every OpMode call the harness moves the sensors on to match the simulated robot:

- **Drive motor encoders** count what the wheels really did (from the physics below), so
  momentum, braking and walls show up in the counts. **Other motors' encoders** count with motor
  power: full power is about 2796 counts per second
  (`FakeDcMotorEx.FULL_SPEED_TICKS_PER_SECOND`, a goBILDA 312 RPM motor, **a placeholder** until
  the team gives real numbers; per motor: `motor("lift").setEncoderModel(ticksPerRev, rpm)`).
  As on the robot, positive power counts up whatever the direction, `RUN_TO_POSITION` drives to
  the target and stops (`isBusy()` turns false), `setVelocity(...)` in `RUN_USING_ENCODER`
  runs at that speed, and `STOP_AND_RESET_ENCODER` zeroes the count. A miswired motor counts
  backwards.
- **The goBILDA Pinpoint** (`FakePinpoint`) reports where the position estimate says the robot
  is, the way a correctly set-up Pinpoint would: from the last `resetPosAndIMU()` (X forward,
  Y left) or `setPosition(...)`, with velocities, unwrapped heading and raw pod counts that
  follow the offsets and pod type the code set. Values only change when the code calls
  `update()`, as on the robot. It doesn't model pod slip or IMU drift.

So **Pedro runs**: pick a route in Team A's "Team A Pedro Paths" OpMode (D-pad up/down during
INIT; it starts on LP-11, and the screen shows "Selected Route") and Pedro 3 follows it, reading
the fake Pinpoint every loop. The OpMode shows "Observed Start" and "Observed End"; a test works
out the end error from those and the route's own end point (`TeamAPedroPathRoute.getEndXInches()`
and the others). **Start the robot where the route starts** on the field
(`PoseEstimator.attachTo(runner, model, wheels, Pose.fromPedro(56, 8, 90))`; `Pose.fromPedro`
converts Pedro's corner-based coordinates), or the field walls won't be where Pedro thinks they
are. Placed that way, Team A's routes finish within about a quarter inch (`PedroRouteTest.java`).

### More sensors (RP7) and the sensor script

Each fake implements the SDK's own type, so robot code reads it with the usual
`hardwareMap.get(...)` and SDK calls. Add them in a test, or list them in `harness.properties`
(`devices.…` lines) so the config robot has them:

| Add with | SDK type robot code asks for | Where its readings come from |
|---|---|---|
| `addImu("imu")` | `IMU` | The robot's real turning: yaw (CCW positive, 0 where it started or after `resetYaw()`), yaw rate; pitch and roll 0. Optional `setDriftDegreesPerSecond`, `setYawOffsetDegrees`. |
| `addCRServo("roller")` | `CRServo` | Records the power and direction the code sets (`getAppliedPower()` after direction). |
| `addDistanceSensor("front")` | `DistanceSensor` | The script; `DistanceUnit.infinity` until it says otherwise. |
| `addTouchSensor("limit")` | `TouchSensor` or `DigitalChannel` | The script. As a digital input, `getState()` is **true when not pressed** (REV wiring). |
| `addColorSensor("intakeColor")` | `NormalizedColorSensor`, `ColorSensor`, `DistanceSensor` | The script: a color (0..1 per part, times the gain) and a distance. |
| `addAnalogInput("pot")` | `AnalogInput` | The script, 0 to 3.3 V. |
| `addOtos("otos")` | `SparkFunOTOS` | The robot's real motion, in the OTOS's own frame (reset / `setPosition`), its units and scalars. Readings only. |
| `addDeadWheel(FakeDeadWheel.parallel("leftOdo", 7.0))` or `.perpendicular("strafeOdo", 3.0)` | `DcMotorEx` encoder on that port | The robot's real motion (forward or sideways roll, plus turning at its offset), in goBILDA pod counts. Readings only. |
| `addLimelight("limelight")` | `Limelight3A` (`LLResult`) | The script (`LimelightView`): a target's tx/ty/ta and AprilTags, read through the SDK's own result parser; only after `start()`. |

The IMU, OTOS and dead wheels follow the simulated robot's motion; position tracking itself
still comes only from the mecanum drive and Pinpoint. The others follow one **`SensorScript`**,
a timeline like `GamepadScript`, attached to the fake robot:

```java
FakeHardwareMap robot = RobotConfigs.teamA();
robot.addDistanceSensor("front");
robot.addTouchSensor("armLimit");
robot.sensorScript(new SensorScript()
        .duringInit().at(0).distanceInches("front", 30)     // seconds since INIT
        .afterStart().at(2.0).distanceInches("front", 8)    // seconds since PLAY
        .at(3.0).press("armLimit")
        .at(4.0).color("intakeColor", 0.8, 0.1, 0.1)
        .at(5.0).set("limelight", LimelightView.target(-4.5, 12.0, 1.8)));
```

Each value holds until the script changes it, and a device can have several kinds at once (a
color sensor's color and distance). Fakes the runner steps implement `SimulatedSensor`; the
runner calls `SimulatedSensors.stepAll` once per loop, before the OpMode. Tests:
`hardware/SensorFakesTest` and `hardware/MoreSensorFakesTest`, each with small TeamCode-style
sample OpModes.

### How motion is simulated, start to finish

There is no real robot, so the harness has to work out where the robot would be. It does that
in small steps: **one step per OpMode call**, every 20 ms of simulated time. This is what happens
in each step, in order (`OpModeRunner.cycle`):

```
        ┌──────────────────────────────────────────────────────────────────────┐
        │ 0. PHYSICS   the robot moves up to "now" with the wheel powers it was  │
        │              given last step (PoseEstimator → MomentumPhysics)         │
        │    SENSORS   drive encoders and the Pinpoint read where it is now      │
        │ 1. GAMEPADS  scripted or live input for this moment; cameras step too │
        │ 2. OPMODE    your code runs: init / init_loop / start / loop / stop    │
        │ 3. TELEMETRY anything the OpMode didn't send yet is sent               │
        │ 4. ESTIMATOR this moment goes in the track, and the NEW wheel powers   │
        │              are read (they drive the robot during the next step)      │
        │    RECORDER  rumbles and the run recording note what happened          │
        │ 5. CLOCK     simulated time moves on 20 ms                             │
        └───────────────────────────── repeat ─────────────────────────────────┘
```

Why this order? On a real robot, the robot keeps moving while your code runs, and the sensors
tell your code where it is **now**. So the harness first moves the robot, then lets the sensors
read it, then runs your code. Your code's new motor powers take effect from the next step on.

**The physics (`MomentumPhysics`).** Each step it gets the four wheel powers and works out the
robot's new speed and position:

1. **What the wheels ask for.** The mecanum math turns four wheel powers into "forward",
   "sideways" and "turn" speeds, using the robot's full-power speeds (Team A: 62.6 in/s forward,
   49.8 in/s sideways, from Pedro's tuning).
2. **Momentum.** The real speed doesn't jump there; it moves toward it. The motors close about
   63% of the gap every 0.1 s (the *response time*), and the tires can't speed the robot up or
   slow it down faster than 200 in/s² before they slip (the *traction limit*). Turning shares
   that limit.
3. **Zero power.** If the motors are on **FLOAT**, the robot coasts and friction slows it at Team
   A's measured rates (42.5 in/s² forward, 50.6 in/s² sideways, from Pedro's zero-power tuners).
   On **BRAKE** it stops much sooner. Below 0.25 in/s with no power, friction holds it still.
4. **Battery.** A motor's speed is about proportional to its voltage, so the wheels push in
   proportion to the battery: at 11 V the robot is 88% as fast as at the 12.5 V its speeds were
   measured at. Set a tired battery with `hardwareMap.batteryVoltage().setVoltage(11.0)`, or one
   that drops during the match with `.setSag(0.5)` (volts per minute of simulated time). Pedro 3
   doesn't read the battery (Pedro 2's voltage compensation is gone); robot code that reads the
   voltage sensor itself gets the same battery.
5. **Moving.** The robot moves by its average speed over the step, turned to the field using the
   heading halfway through the step.
6. **Field elements** (`field.robotObstacles=true`, the default). The same 18 shapes the balls
   bounce off (`BioBuzzObstacles`: the HIVE frame's feet and foot-bar lips, each FLOWER's wall
   brackets and head) stop the robot box: it is pushed back out the shortest way, loses its speed
   into the element and slides along it. The robot counts as a box from the tiles up, so the
   raised FLOWER heads (3.75 in up) stop it too. The motion is checked every 0.1 in, so even a very
   fast robot can't jump through the 0.2 in lip. Each new hit is recorded ("Robot hit the HIVE
   frame", "Robot hit the far FLOWER"), counted in `estimator.obstacleHits()` and in the report's
   walls line. `estimator.withoutRobotObstacles()` turns it off for one run. Unity's physics
   (Stage U3) uses the same shapes from the field file.
7. **Walls.** If any part of the robot box would be past a wall (±72 in), the robot is moved back
   so it just touches, and its speed into the wall is cancelled. Driving into a wall at an angle,
   it slides along it. Each new hit is recorded ("Robot hit the +x wall").

**The sensors** read the physics, so they agree with each other:

- **Drive motor encoders** count what each wheel really turned, worked out back from the robot's
  real speed. A wheel slowed by momentum, or a robot stopped by a wall, shows in the counts.
- **The Pinpoint** reports the robot's position and heading the way a correctly set-up Pinpoint
  would, measured from where the code reset it or set its position.
- **Other motors** (intake, lift) aren't part of the drive, so their encoders count from their
  power.

**What is still a guess.** These values are **placeholders**, shown as `TO MEASURE` in every
report, until the team measures them on the real robot ([CALIBRATION.md](CALIBRATION.md)):

| Value | Placeholder | Where |
|---|---|---|
| Turn rate | 180 °/s | `DriveModels.teamA()` |
| Robot length and width | 18 × 18 in | `DriveModels.teamA()` |
| Response time | 0.1 s | `Dynamics.teamA()` |
| Traction limit | 200 in/s² | `Dynamics.teamA()` |
| Battery voltage the speeds were measured at | 12.5 V | `DriveModels.teamA()` |
| Other motors' encoders | goBILDA 312 RPM | `FakeDcMotorEx` |

**Game pieces.** Every tracked run has BIOBUZZ's 44 balls at their starting spots
(`BioBuzzPieces`; 4 pre-loads touch the robot's back edge, or its front edge if its back is
against a wall). Loose balls on the tiles (`state` `"field"`) are pushed by the robot box: they
leave at least as fast as the bit of robot that hit them, roll and slow down, and bounce off the
walls and each other (`GamePieces`, in small sub-steps so nothing passes through anything). The
robot is never slowed by a ball. Loose balls also bounce off the HIVE frame's feet and foot-bar
lips and each FLOWER's wall brackets (`BioBuzzObstacles`, 18 upright boxes and cylinders copied
from `fields/biobuzz-2026.json` "pieceObstacles", with the wall bounce); a ball only hits an
obstacle that reaches below its top, so balls roll under the raised FLOWER heads. Balls in a
FLOWER, a CELL or an ALLIANCE AREA stay put. Rolling
deceleration (15 in/s²) and the bounces are placeholders (`PieceModel.bioBuzz()`):
the official BIOBUZZ Competition Manual (TU06, section 9.8) gives only the balls' size and
material, approximately 2.8 in (POLLEN) and 3.6 in (NECTAR) Gopher ResisDent polyethylene balls
(`GamePiece.POLLEN_DIAMETER`, `NECTAR_DIAMETER`), and no mass, bounce or friction. The masses
are measured values from AndyMark's store pages: POLLEN (am-5851) 0.055 lb = 24.9 g, NECTAR
(am-5852) 0.091 lb = 41.3 g. Only their ratio matters: it decides how much each ball moves when
two collide. The robot's push doesn't use them. The manual also says
the balls aren't perfectly round and vary in size; the harness uses the nominal sizes.
`estimator.pieces().snapshot()` gives every ball now; `estimator.withPieces(GamePieces.none())`
runs on an empty field.

**What isn't simulated.** Wheel slip below the traction limit, bumps, other robots, picking up or scoring
pieces (an intake would make a piece `"held"`, reserved), voltage dips from heavy load (only
steady sag), pod slip and IMU drift in the Pinpoint.

**For tests.** Round-number test robots (`DriveModel.forTesting`) and `model.withDynamics(null)`
change speed instantly, so simple math tests stay exact. The physics sits behind the
`PhysicsBackend` interface (wheel powers in, position and speed out, one step at a time), so
Unity's physics can replace it later (Phase 7, U3).

### Vision: scripted AprilTags

A **fake webcam** sees the AprilTags a script says, so vision code runs without a camera. Robot
code is unchanged: it still builds a `VisionPortal` and an `AprilTagProcessor` and reads
`getFreshDetections()`.

```java
// 1. SCRIPT: which tags the camera sees, and when (seconds after PLAY)
AprilTagScript tags = new AprilTagScript()
        .at(1.5).see(21)                                // tag 21, 24 in straight ahead
        .at(3.0).lose(21)
        .at(4.0).see(SeenAprilTag.at(22, 2, 30, 5));    // tag 22: 2 in right, 30 ahead, 5 up

// 2. SET UP: add the webcam under the name the robot code looks up
FakeHardwareMap hw = RobotConfigs.teamA();
FakeWebcam camera = hw.addWebcam("logitechVisionWebcam").script(tags);
OpModeRunner runner = new OpModeRunner(new TeamAAprilTagVisionTestOpMode(), hw);
```

| You want | Write |
|---|---|
| A tag comes into view | `.at(1.5).see(21)` (squarely, 24 in straight ahead of the lens) |
| Exactly where it is | `.see(SeenAprilTag.at(id, rightInches, forwardInches, upInches))` |
| Turned, tilted | `.see(new SeenAprilTag(id, right, forward, up, yawDeg, pitchDeg, rollDeg))` |
| A tag leaves the view | `.at(3.0).lose(21)`; every tag: `.loseAll()` |
| Seen before PLAY | `.duringInit().at(0.5).see(20)` (times after INIT) |
| A slower camera | `camera.framesPerSecond(15)` (default 30) |
| What the camera saw | `camera.framesTaken()`, `camera.lastFrameTags()` |

Positions are measured from the camera lens, in inches, in the SDK's `ftcPose` frame: x right,
y forward, z up. The harness turns them into real SDK detections with the SDK's own math, so
robot code gets the same `ftcPose` (range, bearing, elevation), tag `metadata` (from the tag
library the code chose, e.g. DECODE's) and `robotPose` (the robot's field position, from the
tag's field position and the camera mount given to `setCameraPose`) as on the robot. A tag the
library doesn't know comes with no pose, as in the SDK.

Like a real camera, the webcam takes a picture **30 times a second** of simulated time, so with
20 ms loops some loops get `null` from `getFreshDetections()` ("no new picture"). A picture shows
the tags in view at that moment, with no delay. `AprilTagVisionTest.java` has examples.

A run recording saves the tags in each picture (`aprilTags`, below), and the viewer's side
panel shows a **Tags in view** card (id, range, bearing, camera) when you replay a run that saw
any tag.

Not modelled in scripted mode: where the tags are from the robot's position (for that, use
automatic mode below). Not modelled at all: tags hidden behind things, blur, tag clusters, and
the phone's built-in cameras.

### Vision: automatic AprilTags (the field's own tags)

In **automatic mode** nobody scripts the camera. Each picture, the webcam works out which of
the field's tags it can see, from where the robot really is (the physics), where the camera sits
on the robot, and the tags in the season's field file (`fields/<id>.json`). Drive to face a
tag and it appears; turn away and it's lost. Robot code gets the same SDK detections as in
scripted mode, so localisation can be tested against the robot's true position.

```java
FakeHardwareMap hw = RobotConfigs.teamA();
hw.addWebcam("logitechVisionWebcam")
        .seeFieldTags(FieldAprilTags.forField("biobuzz-2026"),
                CameraView.lookingForward(6.875, 4.875, 19));   // right, forward, up, as setCameraPose
// or, with the webcam.<name>.* lines from the robot's camera.properties:  .seeFieldTags("biobuzz-2026")
```

| Setting | Where |
|---|---|
| Scripted or automatic for one test | `.script(...)` or `.seeFieldTags(...)` (the last call wins) |
| The default for live and viewer runs | `webcam.<name>.aprilTags=field` or `script` in the robot's `camera.properties` (Team 2's webcam: `field`) |
| Mount, field of view, range, view angle | `webcam.<name>.*` lines in `camera.properties` (`CameraView.from(setup)`), or a `CameraView` in a test |

A tag is in the picture only if **all** of these hold (`CameraView`):

1. it is no farther than `maxRangeInches` from the lens (default 120 in, placeholder);
2. it faces the camera: the angle between the way the printed side faces and the direction back
   to the lens is at most `maxViewAngleDegrees` (default **60 degrees**, placeholder: detection
   gets unreliable past about 60 to 70 degrees). A tag seen from behind or edge-on isn't read;
3. all four of its corners are in front of the lens and inside the horizontal and vertical field
   of view (default 70.4 × 43.3 degrees in code, 65.9 × 51.8 in Team 2's settings, placeholders).

**Honest limits:** there is **no occlusion at all**. A tag behind the robot's own body, a field
element or a game piece is still seen. No blur, lighting or noise either. Tags marked
`"moves": true` in the field file (all of BIOBUZZ's HIVE tags) are treated as **fixed** where
the file puts them. With the robot not tracked (no drive motors), the camera sees nothing.

**Tag library:** the SDK only gives `ftcPose` and `robotPose` for tags in the library the robot
code chose. Team 2's code uses `AprilTagGameDatabase.getDecodeTagLibrary()`, so BIOBUZZ's tags
30 to 45 come through **with their id but no pose** (the same as on the real robot). The
field-position check in `AutomaticAprilTagsTest` therefore uses DECODE's tag 20 from
`fields/disabled/decode-2025.json`.

---

## 5. Tips

- **Copy an existing test.** `TeamATeleOpScriptTest.java` covers most patterns.
- **Make sure a test can fail.** Once it passes, change an expected value (e.g. `"Intaking"` →
  `"Holding"`) and check that it fails. If it still passes, it isn't really checking anything.
- **Telemetry captions must match exactly**, including capitals and spaces.
- **Name tests as sentences** describing the behavior: `holdingAButtonOnlyCountsAsOnePress`.
- **One behavior per test.** Small tests make it obvious what broke.

---

## 6. What can be tested yet

| You want to test… | Ready? |
|---|---|
| TeleOps, buttons, state machines, hardware config, crashes, telemetry, rumble events | ✅ Now |
| Timed autonomous steps, match-timer rumbles (anything using `ElapsedTime`) | ✅ Now |
| Vision state machine with scripted AprilTags (see "Vision: scripted AprilTags") | ✅ Now |
| Field position (estimated from motor powers), motor encoders | ✅ Now |
| Pedro paths: they run and Pedro sees the robot's position | ✅ Now |
| How well Pedro follows a path: each Team A route ends within 1 in / 2° (`PedroRouteTest.java`) | ✅ Now (robot size and some response values still placeholders until calibrated; Team A's DECODE-season routes run with the BIOBUZZ HIVE switched off, decision D12) |
| `LinearOpMode`s (`waitForStart`, `sleep`, `opModeIsActive`) | ✅ Now (see "LinearOpModes") |
| IMU, CR servos, distance, touch, color and analog sensors, OTOS, dead wheels, Limelight 3A | ✅ Now (see "More sensors (RP7) and the sensor script") |
| BIOBUZZ game pieces pushed by the robot; the robot blocked by walls, the HIVE frame and FLOWERs | ✅ Now (picking pieces up, an intake, isn't simulated yet) |
| Another PVI team's robot (or any team's), from `harness.properties` | ✅ Now for mecanum + Pinpoint robots, on Pedro 2 or Pedro 3 (see BRING_YOUR_OWN_ROBOT.md); other drivetrains aren't tracked yet |

### About time

Robot code runs on **simulated time**: every loop moves the clock forward exactly 20 ms, no matter
how fast the PC is. So:

- A 2-minute TeleOp test finishes in about a second.
- `ElapsedTime`, `getRuntime()` and `System.nanoTime()` in TeamCode all read the simulated clock.
- The same test gives **exactly** the same result every time.

This works through a small Java agent (`harness-agent.jar`) that Gradle loads when tests start. If
a test prints `[harness] WARNING: the harness Java agent is not loaded`, it was run without Gradle;
see Troubleshooting.

---

## 7. Troubleshooting

| Problem | Fix |
|---|---|
| `JAVA_HOME is not set` or `java not found` | Run the `$env:JAVA_HOME = ...` line again in this terminal. |
| `Unable to find a hardware device with name "x"` | The config is missing that device, or the name is spelled differently from TeamCode. |
| Test fails with `null` from `lastValue(...)` | The caption is spelled differently, or that telemetry hasn't been sent yet. Run more loops. |
| `Cannot call loop() when the OpMode is at stage …` | Calls must go in order: `init`, `initLoop`, `start`, `loop`, `stop`. |
| Build can't find a library | First build needs internet. Check the versions in `build.gradle` (next section). |
| `[harness] WARNING: the harness Java agent is not loaded` | The test ran without Gradle, so timed code uses the real clock. Use `gradlew test`, or in Android Studio set **Settings → Build Tools → Gradle → Run tests using: Gradle**. |
| A timed test passes alone but fails with others | Each `OpModeRunner` owns the clock while it runs. Don't interleave loops of two runners in one test. |

---

## 8. For maintainers

- **Library versions come from the robot build.** `build.gradle` reads the main repo's
  `build.dependencies.gradle` (only reads it) and uses the same libraries and versions, minus the
  Android-only ones in `host.skipLibraries` (see "The config file"). After an SDK upgrade,
  `SdkCompatibilityTest` says if the harness copies of SDK classes need updating.
- **The viewer is its own source set** (`src/viewer/java`). It may use the harness core, but the
  core can't even see it, so window code can never leak into the simulation. Tests run with
  `java.awt.headless=true`, so `gradlew test` never opens a window.
- **TeamCode is compiled straight from `../../TeamCode/src/main/java`**, so tests always use the
  current code.
- The FTC libraries are Android `.aar` files; `build.gradle` unpacks their `classes.jar` so a normal
  PC Java can use them. Android classes come from Robolectric's `android-all` jar.
- Layout:

```
src/agent/java/pvi/harness/agent/
  HarnessAgent  Java agent: rewrites clock calls in robot classes as they load
  SimTime       the clock those rewritten calls read
src/main/java/pvi/harness/
  hardware/   FakeHardwareMap, FakeDcMotorEx, FakeServo, RobotConfigs,
              FakeWebcam, AprilTagScript, SeenAprilTag (scripted AprilTags),
              CameraView, FieldAprilTags (automatic AprilTags from the field file)
  ds/         Driver Station side: FakeTelemetry, GamepadScript, GamepadDriver, rumble records
  runtime/    OpModeRunner, SimClock, RunResult, MatchPeriod, RunRecorder / RunRecording / RunRecordingJson,
              JsonReader (small JSON reader, no library), HarnessConfig (reads harness.properties)
src/viewer/java/pvi/harness/viewer/
  FieldCoordinates  inches <-> pixels (the only place the y axis is flipped)
  FieldPainter      draws field, trail, robot box, heading arrow, legend
  FieldImage        draws one moment of a RunRecording off-screen; saves PNGs
  RecordingView     a recording prepared for drawing (trail, caption, robot box per loop)
  SidePanelText     the side panel's text for one moment
  Playback          play/pause, speed, seeking (no window, no clock: testable)
  GamepadIndicator  draws both gamepads with held buttons lit
  FieldPanel, GamepadPanel, ViewerWindow   the Swing window
  RecordingViewer   main(): opens the window (gradlew viewRecording)
  Scenarios         ready-made runs the dropdown can perform
  ViewerChoices     the dropdown's entries (scenarios, then recordings newest first)
  SaveAsTest        the "Save as test" button's work, without the window (testable)
src/test/java/pvi/harness/
  the tests
src/bridge/java/pvi/harness/bridge/
  BridgeServer      the live Unity connection (gradlew unityBridge): local socket, one client
  BridgeSession     the messages (no socket here, so tests drive it line by line)
  BridgeProtocol    message types, version, default port
  BridgeOpModes     the OpModes Unity can run
  BridgeGamepads    Unity's gamepad, through the core's single live-input entry point
```

These are all inside `TestHarness/th_src/`. The Unity project is next to it, in
`TestHarness/VI_Arena/src/` (VI Arena: replays recordings and runs Live mode in 3D; see
[`VI_Arena/README.md`](../VI_Arena/README.md)).

- **The Java agent.** `build.gradle` packages `src/agent` with Byte Buddy into
  `build/agent/harness-agent.jar` and starts the test JVM with `-javaagent:` pointing at it. As
  classes under `org.firstinspires.ftc.`, `com.qualcomm.` and `com.pedropathing.` load,
  their calls to `System.nanoTime()`, `System.currentTimeMillis()` and Android `SystemClock` are
  replaced with calls to `SimTime`. Files on disk are never changed. `OpModeRunner` installs its
  `SimClock` into `SimTime`. Outside a run, `SimTime` falls back to the real clock.
- **Not simulated:** `Thread.sleep()` still really sleeps, and code in other packages (including
  `java.*`) reads the real clock. TeamCode's rules already forbid sleeping in loops.

- **Where the harness differs from the SDK, and why:**
  - `FakeHardwareMap.tryGet` replaces the SDK version, which asks Android which Control Hub it is
    running on. `get()` is still the SDK's own, so error messages match the robot.
  - Gamepads are delivered with the SDK's `Gamepad.copy()`, the same path Driver Station data takes,
    so the SDK's edge detection (`aWasPressed()` and similar) works as on the robot.
  - After each OpMode call the runner sends unsent telemetry, as the SDK does.
  - `requestOpModeStop()` reports to a harness stand-in for the Robot Controller's OpMode manager.
  - **Vision:** the harness has its own copies of two SDK classes,
    `src/main/java/org/firstinspires/ftc/vision/VisionPortal.java` and
    `.../vision/apriltag/AprilTagProcessorImpl.java` (each starts with `HARNESS FILE, not the
    FTC SDK`). The real ones need Android, a camera and native code. Java picks the harness's
    copies because the harness's classes come first on the class path
    (`AprilTagVisionTest.robotCodeGetsTheHarnessVisionPortal` checks this). Their public
    methods must match the SDK's `Vision` version in the repository's `build.dependencies.gradle`: check them when the SDK is
    upgraded. Everything else in the SDK's vision library (builders, tag libraries, detection
    classes, pose math) is the SDK's own.

# FTC Test Harness

Run your **real, unchanged TeamCode** on a laptop, with fake motors, scripted gamepads and simulated
time. No robot, Control Hub or Driver Station needed.

The harness is its own project. It reads `../TeamCode/src/main/java` but never changes it, and it is
not part of the robot build. Nothing here gets deployed to the robot.

---

## 1. What you need

- **Android Studio** installed (the harness uses its built-in Java, the `jbr` folder).
- An internet connection the **first** time you build (to download libraries).

---

## 2. Run the tests

### From a terminal (PowerShell)

Point the terminal at Android Studio's Java. This lasts until you close the terminal:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

Go to the harness folder:

```powershell
cd C:\Dev\ftc-robot-framework\Harness
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
`Harness\build\reports\tests\test\index.html` in a browser.

### From Android Studio

1. **File → Open** and choose the `Harness` folder (not the main repo folder). It opens in its own
   window and does not affect the robot project.
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

### Robot configurations (`RobotConfigs`)

| Method | Devices |
|---|---|
| `RobotConfigs.teamA()` | `frontLeft`, `frontRight`, `rearLeft`, `rearRight`, `intake` |
| `RobotConfigs.teamB()` / `teamC()` | same as Team A for now |
| `RobotConfigs.driveOnly()` | the four drive motors |
| `.without("intake")` | remove a device to test what happens when it is missing |

Need something else? Build your own:

```java
FakeHardwareMap hw = RobotConfigs.teamA();
hw.addServo("claw");
hw.addMotor("lift");
```

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
| Vision state machine with scripted AprilTags | Phase 4 |
| Pedro paths, odometry, field position | Phase 5 |

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

- **Library versions are copied by hand.** When `build.dependencies.gradle` in the main repo changes
  an FTC SDK or Pedro version, update the `robotLibs` lines in `Harness/build.gradle` to match.
- **TeamCode is compiled straight from `../TeamCode/src/main/java`**, so tests always use the
  current code.
- The FTC libraries are Android `.aar` files; `build.gradle` unpacks their `classes.jar` so a normal
  PC Java can use them. Android classes come from Robolectric's `android-all` jar.
- Layout:

```
src/agent/java/pvi/harness/agent/
  HarnessAgent  Java agent: rewrites clock calls in robot classes as they load
  SimTime       the clock those rewritten calls read
src/main/java/pvi/harness/
  hardware/   FakeHardwareMap, FakeDcMotorEx, FakeServo, RobotConfigs
  ds/         Driver Station side: FakeTelemetry, GamepadScript, GamepadDriver, rumble records
  runtime/    OpModeRunner, SimClock, RunResult, MatchPeriod
src/test/java/pvi/harness/
  the tests
```

- **The Java agent.** `build.gradle` packages `src/agent` with Byte Buddy into
  `build/agent/harness-agent.jar` and starts the test JVM with `-javaagent:` pointing at it. As
  classes under `org.firstinspires.ftc.`, `com.qualcomm.`, `com.pedropathing.` and `com.bylazar.` load,
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

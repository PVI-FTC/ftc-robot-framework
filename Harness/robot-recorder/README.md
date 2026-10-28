# Record a drive on the real robot

Drive the **real robot** with a TeleOp, and get the same kind of file the harness viewer saves when
you drive live. Then replay it in the viewer or Unity, or turn it into a test with `makeTest`.

The two files in this folder are **robot code**. They are not part of the harness build. You copy
them into TeamCode yourself.

| File | What it is |
|---|---|
| `HarnessDriveRecorder.java` | Watches the gamepads and saves the drive as a harness run recording (format version 4). It never moves anything. |
| `RecordTeamATeleOp.java` | An example TeleOp: runs Team A's normal TeleOp unchanged, and records it. Shows up on the Driver Station as **"Record: Team A Mecanum Drive"**. |

## 1. Put it on the robot

1. Make a folder `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/harness/`.
2. Copy **both** `.java` files into it.
3. Build and install TeamCode on the Control Hub as usual (Android Studio ▶).

## 2. Record a drive

1. On the Driver Station, pick **Record: Team A Mecanum Drive** (in the TeleOp list).
2. INIT, PLAY, and drive. The robot drives exactly like "Team A Mecanum Drive", because it *is*
   that TeleOp inside.
3. Press STOP. The Driver Station shows **Harness recording:** and where the file went, for
   example `/sdcard/FIRST/harness/drive-TeamATeleOp-20261028-153000.json`.

Every run makes a new file; nothing is overwritten.

## 3. Get the file onto your computer

Connect the Control Hub by USB-C (or the laptop to its Wi-Fi) and copy the file from
`FIRST/harness/` on the hub. In Android Studio: **View → Tool Windows → Device Explorer**, open
`sdcard/FIRST/harness`, right-click the file → **Save As**. Put it in
`Harness/build/recordings/`.

## 4. Use it in the harness

- **Watch it:** drag the file onto `Harness/view.bat`, or pick it in the viewer's dropdown. The
  viewer replays the gamepads; the field shows where the **simulated** robot goes with those
  inputs (the real robot's position isn't recorded, see "Limits").
- **Make a test from it:**

  ```powershell
  .\gradlew.bat makeTest "-Pfile=build\recordings\drive-TeamATeleOp-20261028-153000.json"
  ```

  This writes a new test in `src/test/java/pvi/harness/recorded/` that replays your drive in the
  simulator, with the automatic safety checks and a `// TODO: add your own checks here` spot,
  exactly like a drive recorded in the viewer.

## Record a different TeleOp

Copy `RecordTeamATeleOp.java` (give it a new class name) and change the three marked lines:

1. `@TeleOp(name = "Record: ...")`: the name on the Driver Station.
2. `new TeamATeleOp()`: your TeleOp's class (and fix the `import` above it).
3. `new HarnessDriveRecorder("TeamATeleOp", "teamA")`: your TeleOp's **class name** (the
   harness finds the OpMode by it) and the **harness robot** to replay on: `teamA`, `teamB`,
   `teamC` or `driveOnly`.

### If your TeleOp is a `LinearOpMode`

The wrapper only works for iterative TeleOps (`init()` / `loop()`). For a `LinearOpMode`, call
the recorder from your own code instead:

```java
HarnessDriveRecorder recorder = new HarnessDriveRecorder("MyLinearTeleOp", "teamA");
recorder.init(gamepad1, gamepad2);
while (opModeInInit()) { recorder.initLoop(gamepad1, gamepad2); /* your init code */ }
recorder.start(gamepad1, gamepad2);
while (opModeIsActive()) { /* your drive code */ recorder.loop(gamepad1, gamepad2); }
telemetry.addData("Harness recording", recorder.stop(gamepad1, gamepad2));
telemetry.update();
```

## How it works (and limits)

- The harness simulates time in steps of 20 ms. The real robot's loop runs at its own uneven
  speed, so the recorder writes **one entry per 20 ms of real time**, each with what both
  gamepads looked like then. That makes the file the same shape as a viewer drive, so every
  harness tool reads it.
- It records **only the gamepads** (buttons, sticks, triggers) and the OpMode stages. It doesn't
  record the real robot's position, motor powers or telemetry. When you replay or test it, the
  harness's simulated robot drives with your inputs. Comparing that with where the real robot
  went is a good way to check the simulation (see `Harness/CALIBRATION.md`).
- Timing is to the nearest 20 ms. Very quick taps shorter than one step may be shifted by up to
  one step.
- A 2-minute drive is about 1.5 MB and is kept in memory until STOP, which is fine on a Control
  Hub. If the file can't be written, the TeleOp still works and the Driver Station says why.
- The harness compiles all of TeamCode, so once these files are in TeamCode the harness sees the
  "Record: ..." TeleOp too. In the harness it runs fine. If it's stopped there, it tries to save
  under the SDK's `FIRST` folder, which may not exist on a laptop; then it just reports that it
  couldn't save.

## Checked

Both files were compiled for Java 8 against FTC SDK 12.0.0 and Team A's TeleOp (2026-10-27). A
simulated drive (fake gamepads, real-time loop of 7–13 ms) was saved by the recorder and loaded
back by the harness's own `RunRecordingJson` and `RecordedInputs`. It gave the expected steps
(stick to −1, hold A for 0.2 s, trigger 0.5). It has **not** yet been run on a real Control Hub.

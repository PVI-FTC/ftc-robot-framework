# 05 · Desktop viewer (fallback when Unity can't be used)

Unity is the main plan for watching tests (see [02-unity-readiness.md](02-unity-readiness.md)). If
Unity can't be used, the harness will open **its own window** instead, with:

- a **top-down view of the field** showing the robot's position and heading (and its path so far)
- a **telemetry panel** showing what the Driver Station would show, plus match time

The viewer **has not been built yet.** It is position-estimator Steps 11–15 in
[`Harness/PROGRESS.md`](../PROGRESS.md), with optional extras in Step 16:

| Step | What |
|---|---|
| 11 | Full run recording saved as JSON |
| 12 | Field drawing |
| 13 | Swing window with a side panel |
| 14 | Playback controls |
| 15 | Opt-in switches |
| 16 (optional) | HTML replay, Pedro path overlay, Panels check |

Build them in that order, one step per task. These rules apply to all viewer work.

## 1. Unity and the window use the same data

The window is just another display, like Unity. Both must read the **same plain-data snapshots**
described in [02-unity-readiness.md](02-unity-readiness.md) §4, and come in through the **same
hook**. Never add data that only one display can get.

```
                    ┌──────────────► Unity bridge   (pvi.harness.bridge)
OpModeRunner ──► snapshots
 (core, no UI)      └──────────────► desktop window (pvi.harness.viewer)
```

Hooks that already exist and should be reused:

| Hook | What it gives |
|---|---|
| `runner.everyLoop(r -> ...)` | Called after every OpMode call, so the latest state can be copied out. |
| `PoseEstimator.track()` | One `TrackPoint` (match time + `Pose`) per update: the robot's path. |
| `runner.telemetry().frames()` | Every `TelemetryFrame` sent, each with `matchTimeSeconds()`. |
| `RunResult.gamepad1Rumbles()` (and gamepad 2) | Rumble events with times. |

If the viewer needs something these don't give, add it to the shared snapshot, not to the window.

**Step 11's run recording is that shared snapshot.** It holds, per loop:
- the pose
- motor powers
- autonomous step names
- telemetry

It is saved as JSON. The window replays it, and the Unity bridge should reuse the same records.
Document its fields, units and a `version` number in `Harness/README.md`
([02](02-unity-readiness.md) §8).

## 2. Keep the window out of the core

- All window code goes in its own package, `pvi.harness.viewer`. The core packages (`hardware/`,
  `ds/`, `runtime/`, `physics/`) must never import `java.awt`, `javax.swing`, JavaFX or any other
  UI library, and must never refer to the viewer.
- The viewer may use the core. The core must not use the viewer.
- **Toolkit: Java Swing** (decided, Step 13). It comes with the JDK, so no new library is needed.
  Don't use JavaFX, Python or pygame. The optional HTML replay (Step 16) reads the same JSON
  recording and adds no code to the core.

## 3. Tests never need a screen

- A normal `.\gradlew.bat test` must **never** open a window, and must pass on a computer with no
  display (`java.awt.headless=true`).
- The window opens only when the user asks for it, through the Step 15 opt-in switches:
  `-Pview`, `simView` and `viewRecording`. Their exact behavior is decided in Step 15. Check with
  the user, then document them in `Harness/README.md`. With none of them set, no window may open.
- A test's pass/fail result must be exactly the same with or without the window.

## 4. The window never controls simulated time

The harness runs as fast as it can on simulated time ([02](02-unity-readiness.md) §2). The window
must not slow it down, add `Thread.sleep()` to the runner, or change the results. Two safe ways to
show a run:

1. **Replay (preferred):** let the run finish, then play back the recorded snapshots in the window
   at real speed, with play, pause, a speed control and a time slider. Results stay exactly the
   same and the test stays fast.
2. **Live:** the viewer (not the runner) waits between steps to match real time. Any waiting lives
   in `pvi.harness.viewer`, never in `runtime/`.

## 5. Threads

- Swing draws on its own thread (the Event Dispatch Thread). The window must only ever receive
  **immutable snapshots**. It must never touch the `OpModeRunner`, fake devices or TeamCode
  objects.
- Pass snapshots to Swing with `SwingUtilities.invokeLater(...)` (or a thread-safe queue). Never
  call runner methods from the window's thread.

## 6. Drawing the field

- Use the FTC field frame from [02](02-unity-readiness.md) §6: inches, origin at the center,
  ±72 in, heading counter-clockwise from +x.
- Step 12 draws:
  - a 12 × 12 ft grid
  - the robot box
  - a heading arrow
  - the trail
- The Pedro path overlay (Step 16) uses the same coordinate class.
- Do every inches-to-pixels conversion in **one** class in the viewer. Screen y points down, so
  the y axis must be flipped there, and nowhere else.
- Robot size comes from the `DriveModel` (its `ModelValue`s), never hard-coded in the viewer.
  Placeholder values should be visibly marked (for example in a legend).
- A field background image is optional. If one is added, keep it under `Harness/` (for example
  `src/viewer/resources/`) and work out its scale from the field size, not from fixed pixel
  numbers.
- Show distances to people with `DisplayUnit` and `Pose.toString(...)`, so the window, reports and
  test output all match.

## 7. The telemetry panel

- Show the latest `TelemetryFrame` the way the Driver Station does, in the order it was sent.
- Show the match time, the `MatchPeriod` (AUTONOMOUS or TELEOP) and the OpMode stage (INIT,
  INIT_LOOP, START, LOOP or STOP, as in `RunResult.Stage`).
- In replay, the panel shows the frame for the selected moment, not just the last one.
- Useful extras, once the basics work: rumble events, motor powers and gamepad state.

## 8. Keep it simple

The viewer is read by students too ([04-code-style.md](04-code-style.md)). Prefer one small window
class, one field panel and one telemetry panel over a framework. Viewer code should be covered by
tests that don't need a screen (for example the inches-to-pixels math and the replay timing).

## 9. Live driving and recording tests

Planned as steps D1–D6 in [`Harness/PROGRESS.md`](../PROGRESS.md#drive-and-record-a-test).

- Live input (keyboard, and controllers through **Jamepad**, which the user approved on
  2026-10-18) lives in the viewer. It reaches the robot code only through the core's single
  gamepad input entry point (D1). It never sets `Gamepad` fields directly from the window's
  thread.
- Jamepad is a dependency of the `viewer` source set, and of the `bridge` source set (the user
  decided on 2026-10-24 that for Unity live play the harness reads the controllers and Unity only
  displays; the bridge reuses the viewer's controller classes, never a window). The core and tests
  that don't drive live must build and run without it, and without a controller plugged in.
- Waiting to match real time while someone drives happens in the viewer, as in §4. Simulated time
  still moves exactly one loop period per step, so a recorded drive replays exactly.
- If a controller disconnects, release every input, mark the moment in the recording and tell the
  user (D6).
- A recorded drive is saved as a **generated `.java` test** (D4). The generated code must read
  like a hand-written test ([04-code-style.md](04-code-style.md)):
  - a class comment saying it was recorded, when, and from which OpMode and robot
  - the `GamepadScript` with one line per change
  - the automatic safe checks
  - a clearly marked `// TODO: add your own checks here` spot

  Generate it into `src/test/java/pvi/harness/`, and never overwrite an existing file.

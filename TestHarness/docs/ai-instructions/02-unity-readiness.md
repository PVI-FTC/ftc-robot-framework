# 02 · Unity readiness

The plan is to connect the harness to a **Unity simulator** later: Unity shows the field and robot
and **replaces the harness's physics backend**, while the harness keeps running the real,
unchanged TeamCode. This is **Phase 7** in [`Harness/PROGRESS.md`](../PROGRESS.md):

- the connection will be a **local socket**
- Unity plugs in through the `PhysicsBackend` interface (Phase 5 / Step 17)

The message format over the socket is not designed yet. Write code now so that the bridge can be
added without rewriting the harness.

**The plan, in three stages** (agreed with the user on 2026-10-23; details and status in
[`PROGRESS.md`](../PROGRESS.md#unity-phase-7-in-three-stages)):

1. **U1, replay:** Unity reads the saved run recording JSON files (the same ones the Swing viewer
   replays) and animates them. No socket and no harness Java changes.
2. **U2, live display and controller:** the harness runs TeamCode and talks to Unity over a local
   socket from `pvi.harness.bridge`. It sends each loop's snapshot; Unity sends gamepad input back
   through the single gamepad entry point (§5). Unity asks for each step (§2).
3. **U3, physics:** Unity replaces the built-in position estimate behind `PhysicsBackend` (§5):
   wheel powers in, pose (later encoders and Pinpoint) out.

The Unity project lives in **`Harness/unity/`** (decided 2026-10-23). §1 still applies: C# and
Unity files stay in that folder, and nothing in `src/` may depend on it.

Unity uses C#, so **nothing in Unity can call Java classes directly.** Everything Unity needs has
to go across a boundary as plain data.

If Unity can't be used, a **desktop window** opened by the harness will show the field and
telemetry instead ([05-desktop-viewer.md](05-desktop-viewer.md)). Every rule below is written so
the **same data and hooks** serve both displays. When this file says "Unity", read it as "Unity or
the desktop window" unless it is clearly about C# or Unity's coordinates.

## 1. Keep the core engine-free

The simulation core (`hardware/`, `ds/`, `runtime/`, `physics/`) must not contain:

- Unity, rendering, window, GUI or game-engine code or libraries
- network, socket or HTTP code
- file formats that only one tool reads

Code that talks to the outside world goes in its own package later: `pvi.harness.bridge` for Unity
and `pvi.harness.viewer` for the desktop window. **Dependencies go one way only:** these packages
may use the core, but the core must never use them. A test that runs without Unity installed must keep working forever.

Do not add Unity code, Unity packages or C# files to the harness unless the user asks for that
integration.

## 2. The harness moves time forward one step at a time

- `SimClock` is the **only** clock. Each loop moves it forward a fixed step (20 ms by default,
  `loopPeriod(...)` can change it).
- Never start threads, timers or background loops that run by themselves. Time only advances when
  something calls `loop()` (or a method built on it).
- This lets either side be in charge later: Unity can ask for "one step", or the harness can send
  each step to Unity.
- Never read the real clock (`System.nanoTime()`, `Instant.now()`) in harness code. Read the
  runner's `SimClock`.

## 3. Same input, same result

A run must give exactly the same output every time, because Unity replays and tests depend on it:

- No `Math.random()` or `new Random()` without a fixed seed that the caller passes in.
- Don't let `HashMap`/`HashSet` iteration order decide output. Use `LinkedHashMap`, `TreeMap` or
  a sorted list when order matters.
- Don't depend on the speed of the computer.

## 4. Share state as plain data

Anything Unity might want to show or set (robot pose, wheel and motor powers, servo positions,
telemetry, match time, rumble events) must be readable as **simple values**:

- Use Java `record`s (or small final classes) that only hold numbers, strings, booleans, enums
  and lists of those. `Pose`, `PoseEstimator.TrackPoint`, `TelemetryFrame` and `RumbleRecord` are
  the models to copy.
- Never pass FTC SDK or Android types (`DcMotorEx`, `Gamepad`, `HardwareMap`, `Telemetry`) across
  the boundary. Copy the values you need into a record.
- Every field's unit goes in its name or comment (`xInches`, `headingRadians`,
  `matchTimeSeconds`).
- Snapshots are immutable. Build a new one each step instead of changing an old one.
- Devices are found by the same name strings as the robot configuration (`"frontLeft"`,
  `"intake"`), so an outside program can address them by name.

## 5. Let outside programs supply inputs and models

Write new features so that an outside program could take the place of the scripted parts:

- **Driver input:** `GamepadScript` feeds the gamepads today. Keep gamepad input coming in through
  one place, so a live controller or Unity can feed it later.
- **Sensors and encoders:** fake devices should let a value be **set from outside** (encoder
  ticks, distance readings, AprilTag detections) instead of computing it inside the fake device.
- **Physics:** `PoseEstimator` (dead reckoning) is one way to get the robot's position. Keep the
  question "where is the robot?" separate from how it is answered, so Unity's physics could answer
  it instead. The planned seam is the **`PhysicsBackend`** interface (Step 17):
  - the built-in physics is one implementation
  - the Unity bridge is another

  Until Step 17 is the task, don't create it early. Keep new physics code easy to put behind it:
  wheel powers in, pose / encoder / Pinpoint readings out, one step at a time.

## 6. Units and coordinates

The harness works in the **FTC field frame**:

- inches
- origin at the field center, x and y from -72 to +72
- z up (right-handed)
- heading in radians, counter-clockwise from +x, kept between -π and +π

Unity works in **meters, y up, left-handed**, with yaw in degrees measured clockwise from +z when
viewed from above.

- Calculate **only** in FTC units inside the harness. Never mix Unity units into core math.
- Do every conversion in **one place**, at the boundary, the same way `DisplayUnit` keeps the
  feet conversion in one place.
- **Mapping (confirmed by the user on 2026-10-23** after checking the Stage U1 replay; it lives
  in `unity/Assets/HarnessReplay/Runtime/FtcToUnity.cs`):

  | FTC (harness) | Unity |
  |---|---|
  | `x` inches | `x = x × 0.0254` meters |
  | `y` inches | `z = y × 0.0254` meters |
  | height (z) | `y` |
  | `heading` (CCW from +x) | `yaw = 90° − headingDegrees` |

## 7. No hidden global state

- New state belongs to an object (usually the runner or something it owns), never to a mutable
  `static` field.
- The existing exceptions are:
  - `SimTime`, which the Java agent's rewritten clock calls have to reach
  - `RunnerRegistry`, which the test position report uses

  Don't add more without asking the user.
- The goal is that several simulations could run in one process (for example one per robot in
  Unity) without getting in each other's way. Today `SimTime` allows only **one active clock**
  per process, so only one runner can loop at a time. Don't make that limit worse. If a task needs
  several robots at once, raise it with the user.

## 8. Write down every format you make

The Step 11 JSON run recording and the Phase 7 socket messages both count.

If you add any format that leaves the JVM (JSON, CSV, a message layout), document it in
`Harness/README.md`: each field, its type and unit, and a `version` number that goes up whenever
the format changes.

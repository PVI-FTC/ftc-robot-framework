# Calibrating the harness at the robot (Step 10)

The simulated Team A robot is built from numbers. Some were measured on the real robot (Pedro's
tuners), and some are **placeholders** that the harness marks `TO MEASURE` in every report. This
checklist is for the team at the robot: what to measure, how, and exactly which harness value
each measurement replaces. Bring a tape measure, a phone stopwatch, the robot on a full field,
and a laptop with Android Studio.

Every value lives in `Harness/src/main/java/pvi/harness/physics/`, in a `ModelValue`. To record
a measurement, change `ModelValue.placeholder(value, unit, reason)` to
`ModelValue.measured(value, unit, source)`, where the source says who measured it, when, and how,
for example `"tape measure, bumper to bumper, J. Smith 2026-11-02"`. Then run
`.\gradlew.bat test` from `Harness\`.

## What is already measured (no action needed)

| Value | Where it comes from |
|---|---|
| Forward speed (62.61 in/s) and strafe speed (49.76 in/s) | Pedro `xVelocity` / `yVelocity` in `TeamAPedroConfiguration` (Pedro's velocity tuners), read live |
| Forward coasting (42.48 in/s²) and sideways coasting (50.61 in/s²) | Pedro `forwardZeroPowerAcceleration` / `lateralZeroPowerAcceleration` (Pedro's zero-power acceleration tuners), read live |

If the robot changes (new wheels, more weight), re-run those Pedro tuners and update
`TeamAPedroConfiguration`. The harness picks up the new values by itself.

## Checklist

### 1. Robot length and width (5 minutes)

- **Measure:** bumper to bumper, front to back (length) and side to side (width), in inches.
- **Replaces:** `DriveModels.teamA()`, the two `18.0` `ModelValue.INCHES` placeholders.
- **Why it matters:** where the robot stops against a wall, the robot box in the viewer, and the
  "never hits a wall" check in generated tests.
- **Sanity check:** Pedro's routes start the robot's center 8 in from a wall (`(56, 8)`), so the
  robot must be **at most 16 in** in that direction. If it is longer, the route start needs
  fixing in TeamCode, not here.

### 2. Turn rate (10 minutes)

- **Measure:** spin in place at full power (right stick fully over, nothing else) for exactly
  2 s, starting already spinning (spin 1 s first, then time 2 s). Read the heading from the
  Pinpoint before and after (the TeleOp or a Pedro OpMode shows it on the Driver Station).
  Turn rate = degrees turned ÷ 2 s. Do it three times, both directions, and use the average.
- **Replaces:** `DriveModels.teamA()`, the `180.0` `ModelValue.DEGREES_PER_SECOND` placeholder.
- **Why it matters:** how fast the simulated robot turns, and so how Pedro's heading control
  behaves in the harness.

### 3. Response time and traction limit (15 minutes)

These are the momentum values: how quickly the robot speeds up.

- **Measure:** from standing still, full power forward for 1 s, logging the Pinpoint's forward
  velocity (`getVelX`) **every loop** with the loop time. Pedro's Forward Velocity Tuner already
  drives like this; a phone video of the Driver Station also works if the OpMode shows velocity.
  Do it three times.
- From each log:
  - **Traction limit** = the steepest rise in speed, in in/s², usually in the first 0.2 s
    (speed change ÷ time between two loops, averaged over a few loops).
  - **Response time** = after that first steep part, the time it takes to close 63% of the
    remaining gap to full speed, in seconds.
- **Replaces:** `Dynamics.teamA()`, `0.10` `Dynamics.SECONDS` (response time) and `200.0`
  `Dynamics.INCHES_PER_SECOND_SQUARED` (maximum acceleration).
- **Why it matters:** how far the robot overshoots and how Pedro's braking behaves.

### 4. Other motors' encoders (only if a test needs them)

- **Measure:** for each non-drive motor (lift, arm, intake), the encoder counts per output
  turn and the free speed in RPM, from the motor's spec sheet or by counting.
- **Replaces:** per motor, `motor("lift").setEncoderModel(ticksPerRev, rpm)` in the test (the
  default is a goBILDA 312 RPM placeholder in `FakeDcMotorEx`).

## Comparing a real autonomous run with the harness

1. **On the robot:** run "Team A Pedro Paths" → **DECODE Route** (or another route) from its
   start position. Write down what the Driver Station shows at the end: **Observed End**,
   **End Position Error**, **End Heading Error**, and roughly how long the route took.
2. **In the harness:** run `.\gradlew.bat test --tests "pvi.harness.physics.PedroRouteTest"`. It
   prints the same route's end, e.g. `DECODE Route ended 0.095 in and 0.03 deg from its target`.
   Watch it with the viewer: a recording of the same route shows the path and timing.
3. **Compare:** after steps 1-3 above, the harness's end error and route time should be close to
   the robot's, within about **1 in** and **10%** of the time. If the robot is much worse:
   - it overshoots corners more: the response time or traction limit is still off (step 3);
   - its heading lags or swings: the turn rate is off (step 2);
   - it stops short near walls: the robot size is off (step 1), or the start position is wrong.
4. Write down the comparison (date, route, robot result, harness result) in PROGRESS.md's log.

## A helper OpMode that would make this easier (not added)

A helper OpMode would make steps 2 and 3 a button press, but it would live in `TeamCode/`, which
the harness must never change. If the team wants one, someone working on TeamCode could add a
**Team A Calibration** OpMode with:

- **A:** spin in place at full power for 3 s and show degrees per second from the Pinpoint
  (step 2);
- **B:** drive forward at full power for 1 s, record the Pinpoint's forward velocity every loop,
  then show the steepest speed-up (traction limit) and the time to 63% of full speed (response
  time) (step 3);
- **X:** show the Pinpoint pose live, for checking distances with a tape measure.

The harness could then test that OpMode like any other before it goes on the robot.

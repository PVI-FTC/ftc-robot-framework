# Calibrating the harness at the robot (Step 10)

The simulated Team A robot is built from numbers. Some were measured on the real robot (Pedro's
tuners), and some are **placeholders** that the harness marks `TO MEASURE` in every report. This
checklist is for the team at the robot: what to measure, how, and exactly which harness value
each measurement replaces. Bring a tape measure, a phone stopwatch, the robot on a full field,
and a laptop with Android Studio.

Every value is one line in `Harness/harness.properties`, written
`number | measured or placeholder | where it came from`. To record a measurement, put the number
in, change `placeholder` to `measured`, and replace the text after the last `|` with who measured
it, when, and how, for example
`robot.lengthInches=17.5 | measured | tape measure, bumper to bumper, J. Smith 2026-11-02`. Then
run `.\gradlew.bat test` from `Harness\`.

## What is already measured (no action needed)

| Value | Where it comes from |
|---|---|
| Forward speed (62.61 in/s) and strafe speed (49.76 in/s) | `drive.forwardSpeedInchesPerSecond` / `drive.strafeSpeedInchesPerSecond`: copies of Pedro's `maxAchievableForwardVelocity` / `maxAchievableStrafeVelocity` in `TeamAPedroConfiguration` (Pedro's velocity tuners) |
| Forward coasting (42.48 in/s²) and sideways coasting (50.61 in/s²) | `drive.forwardCoastInchesPerSecondSquared` / `drive.lateralCoastInchesPerSecondSquared`: copies of Pedro's `naturalForwardDeceleration` / `naturalStrafeDeceleration` (Pedro's zero-power acceleration tuners) |

These are copies, not read live: `TeamAConfigMatchesPedroTest` (a PVI extra) fails and names the
line to update when Pedro is re-tuned and the copy isn't.

If the robot changes (new wheels, more weight), re-run those Pedro tuners and update
`TeamAPedroConfiguration`, then copy the new numbers into those four `harness.properties` lines
(`TeamAConfigMatchesPedroTest` tells you which ones).

## Checklist

### 1. Robot length and width (5 minutes)

- **Measure:** bumper to bumper, front to back (length) and side to side (width), in inches.
- **Replaces:** `robot.lengthInches` and `robot.widthInches` in `harness.properties` (both `18 | placeholder` today).
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
- **Replaces:** `drive.turnRateDegreesPerSecond` in `harness.properties` (`180 | placeholder` today).
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
- **Replaces:** `drive.responseTimeSeconds` (`0.10`) and
  `drive.maxAccelerationInchesPerSecondSquared` (`200`) in `harness.properties`, both placeholders today.
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

### Route times and end errors to compare (for step 3)

The harness numbers come from `PedroRouteTest`, which prints them each run (for example
`DECODE Route finished 13.54 s after PLAY, ended 0.192 in and -0.04 deg from its target`). The
time is from PLAY until the Driver Station first shows **Sequence Complete: true**. Fill in the
robot columns at the robot, timing the same way. The harness numbers are from before
calibration (placeholder robot size, response time and traction), so rerun the test after
steps 1-3 and update them.

| Route | Harness time (s) | Harness end error | Robot time (s) | Robot end error | Date |
|---|---|---|---|---|---|
| LP-11 Pilot (24 in) | 1.06 | 0.16 in, 0.0° | | | |
| DECODE Route | 13.54 | 0.19 in, 0.0° | | | |
| Curving Test | 17.78 | 0.16 in, 0.0° | | | |

Harness numbers measured 2026-10-27 with Pedro 3 (Team A's new Foresight path follower). With Pedro 2 (2026-10-24) they were 0.96 s / 0.24 in, 12.86 s / 0.10 in and 19.56 s / 0.25 in, 1.6°.

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

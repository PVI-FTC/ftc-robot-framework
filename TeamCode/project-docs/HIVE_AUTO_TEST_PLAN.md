# Hive Auto Test Plan

How to take `KimCode/HiveAuto` from "it compiles" to match-ready. None of this has been done yet:
the code has never run on a robot, and its numbers come from CAD or are guesses.

**Before any powered test:** have adult supervision, a clear area, and a named person ready to press
Driver Station STOP. Keep the robot on blocks until step 1 passes.

## Test order

Each step depends on the one before it, except that steps 3 and 4 can happen in either order.

| Step | Run | Do this | It passes when |
|---|---|---|---|
| 1 | Pedro tuner "Tests" | With the robot on blocks, settle the motor directions in `pedro/Constants.java`. `TeleopV1` sets different ones, so only one of them can be right. | Forward, strafe, and turn commands spin the wheels the correct way. |
| 2 | `Hive Auto Red` | With no launcher configured and the Limelight unplugged, run on a real field. | It leaves the wall and parks in the LOADING ZONE, and stops before 29 s. Repeat for Blue. |
| 3 | `Hive Vision Test` | Measure the camera mount, set up an AprilTag pipeline, and put the robot at the viewing spot. Try the HIVE tilted each way. | It reads the tags and the Answer is correct both ways. |
| 4 | `Hive Launcher Test` | Check the motor names, spin up, fire, and fill in the speed table from several measured distances. | It holds speed, feeds 4 balls, and shots land. |
| 5 | `Hive Auto Red` | With the Limelight unplugged and the launcher working, run the preset shot. | Shots land from the preset spot, and it still parks. |
| 6 | `Hive Auto Red` | With everything on, run the full Auto. Then cover the camera for one run. | It aims and hits the raised CELL. With the camera covered, it makes the preset shot. |
| 7 | Both OpModes | Raise the path speed in steps and time full runs using "Stage times" on telemetry. Set the cutoff from those times. | Several runs on each alliance finish parked and motionless before 30 s. |

## Numbers to replace

| Value | File | Where it comes from now |
|---|---|---|
| Motor directions | `pedro/Constants.java` | Unsettled; see step 1 |
| `ROBOT_LENGTH_INCHES` | `HiveField` | FTC maximum size, sets the start position against the wall |
| `START_*`, `VIEW_*`, `LAUNCH_Y_INCHES`, `PARK_*` | `HiveField` | Worked out from field CAD |
| CELL positions and tag heights | `HiveField` | Field CAD; also check which CELL starts raised |
| `CAMERA_HEIGHT_INCHES`, `CAMERA_TILT_DEGREES`, `CAMERA_FORWARD_INCHES` | `HiveField` | Guesses, including `TeleopV1`'s "CHANGE THIS" values |
| `APRILTAG_PIPELINE`, `LIMELIGHT_NAME` | `KimHiveVision` | From `TeleopV1`; the pipeline's contents are unknown |
| Motor names and directions | `KimLauncher` | From `TeleopV1`'s commented-out code |
| `FEED_*`, `GAP_SECONDS`, `SPEED_TOLERANCE_TICKS_PER_SECOND` | `KimLauncher` | Guesses |
| Distances and speeds | `HiveShotTable` | Guesses |
| `PATH_SPEED_FRACTION` | `HiveAuto` | 0.30 for first runs; raise it in step 7 |
| `RETURN_CUTOFF_SECONDS` | `HiveAuto` | 23 s guess; set it in step 7 |
| `LOCATE_TIME_LIMIT_SECONDS`, `AIM_*` | `HiveAuto` | Guesses |

## What to watch for

- **Viewing angle:** the tags face mostly downward, so they may be hard to read from a distance. If
  the Limelight can't read them at the viewing spot, move the spot or change the camera tilt. The
  launch spot has to keep the raised tags in the picture too.
- **Aiming:** if the launcher isn't lined up with the camera, shots will miss to one side even when
  the Auto says it's aimed. The code assumes they point the same way.
- **Tip count:** the robot always fires all 4 preloads, and doesn't check for a tip. Once you know how
  many balls tip the HIVE, you can decide whether that should change.
- **Partner robots:** agree with your alliance partner on who goes where, because the Auto doesn't
  look for other robots. It can also end up in the same LOADING ZONE as your partner.
- **Stuck robot:** if the robot is blocked, it won't finish its path. It waits until the cutoff, and
  it stops trying at 29 s.

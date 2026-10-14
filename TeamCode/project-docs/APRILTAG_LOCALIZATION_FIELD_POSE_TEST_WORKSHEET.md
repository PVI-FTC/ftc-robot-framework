# LA-06 Stationary Field-Pose Test Worksheet

## Purpose and limits

Use this worksheet only for LA-06 after every physical prerequisite and the complete LA-06 STOP
warning have been reconfirmed. This test compares stationary AprilTag field-pose candidates with
independently measured robot poses. It does not authorize localization-provider writes, alignment,
autonomous decisions, or drivetrain output.

No physical results have been collected in this worksheet. The current team does not yet have
access to the measured test space.

## Checked-out SDK tag evidence

The checked-out FTC SDK 11.2.1 `AprilTagGameDatabase.getDecodeTagLibrary()` entry supplies:

- ID: 20
- Name: `BlueTarget`
- Black-square size: 6.5 inches
- Tag-center field position: X = -58.3727 in, Y = -55.6425 in, Z = 29.5 in
- SDK orientation quaternion `(w, x, y, z)`:
  `(0.2182149, -0.2182149, -0.6725937, 0.6725937)`

The quaternion describes a vertical, upright tag whose printed face points toward increasing X and
increasing Y. Its face-normal direction is 54 degrees counterclockwise from field +X. The tag-plane
line is therefore 144 degrees counterclockwise from field +X.

## Physical setup

1. Mark one floor point as the official FTC field origin.
2. Mark perpendicular +X and +Y axes using the official FTC field convention.
3. Mark the tag-center floor projection at X = -58.3727 in and Y = -55.6425 in.
4. Secure tag 20 vertically and upright with its center 29.5 in above that floor point.
5. Use a protractor to aim the printed face along the 54-degree direction from +X.
6. Confirm that the black square measures 6.5 by 6.5 in and the complete tag is flat and visible.
7. Mark each robot pose using the drivetrain center projected onto the floor. Do not measure from
   the camera lens, bumper, or wheel edge.
8. At each off-axis pose, rotate the robot to face the center of tag 20.

## Preselected expected poses

| Placement | Expected X (in) | Expected Y (in) | Expected heading (deg) | Planar tag distance (in) |
|---|---:|---:|---:|---:|
| Centered | -30.16 | -16.81 | -126.00 | 48.00 |
| 12 in robot-left | -20.45 | -23.86 | -140.04 | 49.48 |
| 12 in robot-right | -39.87 | -9.76 | -111.96 | 49.48 |

The left/right offsets are perpendicular to the centered robot heading. Their direct tag distances
are about 49.48 in because each combines a 48-in forward component with a 12-in lateral component.

## Preselected acceptance rules

A placement passes only when all applicable checks pass:

- absolute X error is at most 3.00 in;
- absolute Y error is at most 3.00 in;
- wrapped absolute heading error is at most 5.00 degrees;
- X spread across three readings is at most 2.00 in;
- Y spread across three readings is at most 2.00 in; and
- wrapped heading spread across three readings is at most 3.00 degrees.

Do not average away a failed individual reading. Record and stop for an unexplained jump.

For each reading:

- `X error = observed X - expected X`
- `Y error = observed Y - expected Y`
- `absolute error = absolute value of the error`
- `spread = largest observed value - smallest observed value`
- heading error is the smallest angular separation after wrapping by 360 degrees

## Before INIT checklist

- [ ] Adult supervisor is present.
- [ ] Named Driver Station STOP operator is present with immediate control.
- [ ] Drive motors are physically disconnected or movement is equivalently prevented by a
      reviewed method.
- [ ] Powered Control Hub and Driver Station are available.
- [ ] Deployment access is available.
- [ ] Logitech C920 is secure at the recorded mount position and orientation.
- [ ] Robot configuration name is `logitechVisionWebcam`.
- [ ] Camera is connected directly to the Control Hub USB port.
- [ ] VisionPortal is configured for 640 by 480.
- [ ] Matching C920 calibration evidence remains valid.
- [ ] No calibration warning is present.
- [ ] Official tag 20 identity, size, position, height, and orientation match this worksheet.
- [ ] Field origin and perpendicular X/Y axes are marked and checked.
- [ ] Robot and tag are secure and stationary.
- [ ] Tape measure and angle-measurement tool are available.
- [ ] Lighting is good and the complete tag is visible.
- [ ] The area is clear of people and obstacles.

## Required telemetry for every recorded reading

Record only a `FRESH` observation for which all of these are true:

- Tag ID is 20.
- Field Candidate Available is `true`.
- Field Candidate Status says the fresh fixed-tag candidate was accepted.
- Candidate X, Y, and heading are finite.
- Acquisition timestamp is positive.
- Candidate age is non-negative and no greater than 250 ms at acceptance.

Retained observations are not measurements and must not contain a field-pose candidate.

## Centered placement readings

Expected: X = -30.16 in, Y = -16.81 in, heading = -126.00 degrees.

| Reading | Frame | Tag | Age (ms) | X (in) | Y (in) | Heading (deg) | Candidate status | Notes |
|---:|---|---:|---:|---:|---:|---:|---|---|
| 1 |  |  |  |  |  |  |  |  |
| 2 |  |  |  |  |  |  |  |  |
| 3 |  |  |  |  |  |  |  |  |

Calculated result:

- Maximum absolute X error: ______ in — PASS / FAIL
- Maximum absolute Y error: ______ in — PASS / FAIL
- Maximum absolute heading error: ______ deg — PASS / FAIL
- X spread: ______ in — PASS / FAIL
- Y spread: ______ in — PASS / FAIL
- Heading spread: ______ deg — PASS / FAIL
- Placement result: PASS / FAIL / STOPPED

## Robot-left placement readings

Expected: X = -20.45 in, Y = -23.86 in, heading = -140.04 degrees.

| Reading | Frame | Tag | Age (ms) | X (in) | Y (in) | Heading (deg) | Candidate status | Notes |
|---:|---|---:|---:|---:|---:|---:|---|---|
| 1 |  |  |  |  |  |  |  |  |
| 2 |  |  |  |  |  |  |  |  |
| 3 |  |  |  |  |  |  |  |  |

Calculated result:

- Maximum absolute X error: ______ in — PASS / FAIL
- Maximum absolute Y error: ______ in — PASS / FAIL
- Maximum absolute heading error: ______ deg — PASS / FAIL
- X spread: ______ in — PASS / FAIL
- Y spread: ______ in — PASS / FAIL
- Heading spread: ______ deg — PASS / FAIL
- Placement result: PASS / FAIL / STOPPED

## Robot-right placement readings

Expected: X = -39.87 in, Y = -9.76 in, heading = -111.96 degrees.

| Reading | Frame | Tag | Age (ms) | X (in) | Y (in) | Heading (deg) | Candidate status | Notes |
|---:|---|---:|---:|---:|---:|---:|---|---|
| 1 |  |  |  |  |  |  |  |  |
| 2 |  |  |  |  |  |  |  |  |
| 3 |  |  |  |  |  |  |  |  |

Calculated result:

- Maximum absolute X error: ______ in — PASS / FAIL
- Maximum absolute Y error: ______ in — PASS / FAIL
- Maximum absolute heading error: ______ deg — PASS / FAIL
- X spread: ______ in — PASS / FAIL
- Y spread: ______ in — PASS / FAIL
- Heading spread: ______ deg — PASS / FAIL
- Placement result: PASS / FAIL / STOPPED

## Loss and reacquisition checks

1. Record the last accepted acquisition timestamp: ____________________ ns.
2. Fully hide or remove the tag without moving the robot.
3. Confirm a fresh empty frame clears the detection and no candidate remains: PASS / FAIL.
4. Confirm any retained row has no candidate and gives the retained rejection reason: PASS / FAIL.
5. Restore the tag.
6. Confirm reacquisition uses a newer acquisition timestamp: ____________________ ns — PASS / FAIL.
7. Confirm the reacquired candidate again passes age, ID, metadata, and finite-value gates:
   PASS / FAIL.

## Immediate STOP conditions

Press STOP and end the run for any calibration warning, wrong tag or field metadata,
stale-as-fresh data, non-finite result, wrong axis or sign, unexplained jump, camera error,
movement, loss of supervision, or loss of STOP control. Do not change multiple causes at once and
do not enable alignment.

## Session result

- Date: ____________________
- Adult supervisor: ____________________
- STOP operator: ____________________
- Movement-prevention method: ____________________
- Overall result: PASS / FAIL / BLOCKED / STOPPED
- Observed limitations: ________________________________________________________________
- One testable hypothesis for any failure: ______________________________________________

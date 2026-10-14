# AprilTag Vision Pilot Architecture Decision

## Status

Approved planning decision. This document records intended vision-pilot design; it does not claim
that AprilTag code has been implemented. It remains separate from `ARCHITECTURE.md` while the
localization/pathing pilot proceeds in parallel. A later coordinated review may move stable,
shared rules into `ARCHITECTURE.md`.

## Pilot goal and non-goals

The first pilot detects AprilTags and reports each tag ID plus an observation relative to the
robot. It is observation-only.

It must not correct localization, write a field pose, select a path, command the drivetrain, or
affect autonomous behavior. It must not depend on Pedro Pathing, a drivetrain implementation, or
a selected localization provider.

## Required dependency flow

```
Testing OpMode
  -> Team A AprilTag Vision Robot public API
    -> VisionSubsystem FSM
      -> VisionHardware
        -> selected hardware source
          -> FTC VisionPortal APIs or a future FTC Limelight API
```

The OpMode maps inputs and displays telemetry only. It must not access a camera,
`HardwareMap.get(...)`, an FSM, VisionPortal, or Limelight directly.

## FSM ownership

`VisionSubsystem` remains the only owner of vision behavior and state transitions. The existing
disabled, searching, target-acquired, tracking, and lost-target states remain the behavior model.
Hardware only initializes, enables or disables processing, collects detections, reports status,
and releases resources. A hardware source never reads gamepads, selects states, or commands a
robot.

## Optional composition

The first pilot should add a separate Team A vision-only Robot composition and testing OpMode,
leaving `TeamARobot` unchanged. A team without a camera must retain its existing simple Robot and
drive behavior. Missing or unconfigured vision hardware must be an unavailable, safe result, not
an initialization failure for a simple robot.

## Logitech-first, Limelight-ready source boundary

The pilot implements one active Logitech/UVC source through `VisionPortal` and
`AprilTagProcessor`. `VisionHardware` composes a small internal source boundary that returns
library-neutral observations. A later Limelight source can implement the same boundary using the
FTC Limelight API and an approved AprilTag pipeline.

```
VisionHardware
  -> AprilTagVisionSource
       -> VisionPortalAprilTagSource (pilot)
       -> LimelightAprilTagSource (future)
```

This is an internal hardware-layer seam, not a second FSM or a public Robot API. Only one source
is active in the pilot. The Limelight is not a VisionPortal device, so it requires its own source
implementation and validation, but it must not force a change above `VisionHardware`.

## Observation contract

The future public result is an immutable, library-neutral `AprilTagObservation`; FTC vision types
stay behind the vision boundary. The planned fields are tag ID, observation timestamp, position
right/forward/up, pitch/roll/yaw, range/bearing/elevation, pose availability, reference-frame
name, and calibration/quality status.

The robot frame is selected and documented per robot: origin at a stable team-chosen point
(recommended: drivetrain center of rotation projected to the mat), +X right, +Y forward, +Z up.
The camera lens location and orientation relative to that origin are measured team facts. If
calibration or mount values are absent, the software may report ID detection but must label metric
robot-relative pose unavailable or unverified; it must not invent values.

No field-tag metadata, robot field pose, confidence fusion, or localization correction is part of
this contract.

## Stage 4 metric-observation design

Stage 4 must preserve the distinction provided by the FTC SDK's fresh-detection API:

- No new processed frame is a retained-snapshot event. The last ID observation may remain visible
  for diagnostics, but it keeps its original frame-acquisition timestamp, its age continues to
  increase, and its metric pose is unavailable while retained.
- A new processed frame with zero detections is a fresh empty result. It clears observations and
  allows the existing vision FSM to report target loss.
- A new processed frame with detections replaces the prior snapshot. Each observation preserves
  `AprilTagDetection.frameAcquisitionNanoTime`; a Robot-loop timestamp must not replace it.
- Unavailable hardware produces an unavailable empty result, which is distinct from a working
  camera's fresh empty result.

The neutral hardware boundary should expose a beginner-readable immutable snapshot containing a
frame status and immutable observations. Suggested statuses are `FRESH`, `RETAINED`, and
`UNAVAILABLE`. Existing list-returning public APIs may remain as convenience views so the smallest
compatible change can be made. Source-specific FTC objects and status types remain below
`VisionHardware`.

For verified tag 22 observations, the source converts data in this order only:

```
FTC camera-relative pose
  -> neutral camera frame (+X right, +Y forward, +Z up; origin at camera lens)
    -> measured Team A robot frame (+X right, +Y forward, +Z up;
       origin at drivetrain center of rotation projected to the mat)
```

The diagnostic may show both neutral camera-relative and experimental robot-relative pose so the
transform can be checked. These are two named views of one detection, not two detections. The
recorded Team A camera translation is +6.875 inches X, +4.875 inches Y, and +19 inches Z, with
measured yaw, pitch, and roll of 0 degrees. Robot-relative range, bearing, and elevation are
recomputed from the transformed robot-frame translation rather than copied from camera-relative
values.

Metric pose must be unavailable for an unrecognized or non-22 tag, missing or mismatched tag 22
metadata, missing matching camera-calibration evidence, missing FTC pose, retained or otherwise
stale data, a non-finite metric value, an invalid acquisition timestamp, or an unverified frame
conversion. ID detection may remain available with a clear quality status. The Stage 4 diagnostic
must never use these observations to write field pose, localize the robot, or command movement.

## Stage 5 field-pose candidate design

Stage 5 adds an optional field-pose candidate to a fresh neutral observation. A candidate is one
camera measurement, not a maintained localization estimate, correction, fused pose, or movement
request. A future Pedro/odometry adapter may decide whether to consume a reviewed candidate; that
integration is not part of this pilot.

The neutral field frame is the official FTC Field Coordinate System: origin at the center of the
field on the mat, +X to the right when viewed from the Red Wall, +Y away from the Red Wall, +Z up,
and positive rotation by the right-hand rule. Distances are inches and angles are degrees. Any
future Pedro or odometry convention conversion belongs at its adapter boundary rather than inside
the vision source.

The first replaceable field configuration is the DECODE fixed-GOAL configuration. It allows blue
GOAL tag 20 and red GOAL tag 24 only after their checked-out SDK 11.2.1 metadata, size, field
position, and field orientation are verified. DECODE OBELISK tags 21, 22, and 23 are not field
localization references because the OBELISK position varies. The existing tag-22 relative
observation may remain available for motif or target-relative work, but it must never produce a
field-pose candidate.

Do not hard-code DECODE IDs in reusable field-pose math. Use a small immutable localization
configuration that names the field/season, lists the allowed fixed tag IDs, names the official
field frame, and supplies the maximum candidate age. The initial maximum age is 250 milliseconds
and remains a hardware-validation value, not a universal FTC constant. BIOBUZZ and its SDK 12
cluster API require a later migration prompt; moving BIOBUZZ tags cannot be used as absolute field
references.

For SDK 11.2.1, prefer the checked-out official localization path instead of duplicating 3D
transform math:

```
verified fixed-tag metadata in the selected SDK library
  + FTC camera-relative detection
  + measured camera position/orientation supplied with AprilTagProcessor.Builder.setCameraPose(...)
    -> AprilTagDetection.robotPose in the official FTC field frame
      -> immutable library-neutral field pose and candidate
```

FTC `Position`, `YawPitchRollAngles`, tag metadata, and `AprilTagDetection.robotPose` remain inside
the hardware source. The current physical camera mount remains +6.875 inches right, +4.875 inches
forward, and +19 inches up with the camera pointing forward. The SDK localization camera
orientation uses its own documented camera-axis convention: a forward horizontal camera is yaw 0,
pitch -90, and roll 0 degrees. This SDK representation must not silently replace or reinterpret the
existing neutral mount convention of yaw, pitch, and roll 0 degrees.

A field-pose candidate is available only when all of these gates pass:

- the enclosing snapshot is `FRESH`;
- the acquisition timestamp is positive and no more than 250 milliseconds old;
- the ID is allowed by the selected field configuration;
- the SDK metadata ID, size, field position, and field orientation match the verified fixed tag;
- the C920 640-by-480 calibration and measured camera mount remain verified;
- the SDK supplies `robotPose`; and
- every copied field position and orientation value is finite.

Failure produces no candidate and an explicit beginner-readable rejection reason. A retained
snapshot may retain ID-only diagnostic information and its original timestamp, but it cannot
retain or recreate a field-pose candidate. A fresh empty frame clears observations and candidates.
The vision subsystem still owns lifecycle and snapshots; no localization estimator is added.

The smallest planned neutral API is:

- `common.localization.FieldPose`: immutable official-field X/Y/Z and pitch/roll/yaw;
- `common.localization.AprilTagFieldPoseCandidate`: immutable tag ID, acquisition timestamp,
  accepted field pose, and named field frame;
- `common.localization.AprilTagLocalizationConfiguration`: immutable season/configuration name,
  allowed fixed IDs, field-frame name, and maximum age; and
- narrow optional candidate and candidate-status getters on `AprilTagObservation`, preserving its
  existing constructors and camera/robot-relative views.

LA-03 should add and verify the neutral value/configuration types and the exact SDK 11.2.1 tag 20
and 24 metadata evidence. LA-04 should configure the SDK camera pose below the hardware boundary,
copy accepted `robotPose` values into the neutral candidate, expose rejection reasons, and extend
only the stationary diagnostic. `VisionPortalAprilTagSource`, `VisionHardware`, and
`TeamAAprilTagVisionRobot` may receive the narrow configuration plumbing required for that flow.
No TeamARobot, drive, autonomous, Pedro, Limelight, or motor behavior changes are part of LA-03 or
LA-04.

## Lifecycle and safety

Create the selected camera source during Robot initialization, update it once through the active
vision FSM lifecycle, and close/release it during Robot stop. Do not rapidly rebuild or close a
portal in response to a button. The pilot does not command motors; validation is stationary and
supervised.

## Deferred integration decisions

- Exact Limelight model, API, network/hardware configuration, and AprilTag pipeline.
- Camera choice, configured name, resolution, calibration evidence, and physical mount values.
- AprilTag library and physical tag provenance/size.
- Additional season tag maps, timestamp synchronization across different sensors, uncertainty
  model, fusion policy, and any future path or drivetrain influence.

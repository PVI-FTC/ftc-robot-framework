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

## Lifecycle and safety

Create the selected camera source during Robot initialization, update it once through the active
vision FSM lifecycle, and close/release it during Robot stop. Do not rapidly rebuild or close a
portal in response to a button. The pilot does not command motors; validation is stationary and
supervised.

## Deferred integration decisions

- Exact Limelight model, API, network/hardware configuration, and AprilTag pipeline.
- Camera choice, configured name, resolution, calibration evidence, and physical mount values.
- AprilTag library and physical tag provenance/size.
- Field-tag metadata, timestamp synchronization, uncertainty model, fusion policy, and any future
  path or drivetrain influence.

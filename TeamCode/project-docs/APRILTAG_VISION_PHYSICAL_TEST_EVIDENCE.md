# AprilTag Vision Physical Test Evidence

## Scope

This record contains the confirmed equipment and safety facts for the stationary Team A
Logitech/UVC AprilTag pilot. It does not authorize drivetrain movement, field-pose calculation,
localization correction, autonomous behavior, or Limelight use.

Evidence was collected on 2026-08-19 from direct student inspection and measurement unless an
official source is named below.

## Equipment and access

- Powered Robot Controller: Control Hub available.
- Driver Station: compatible, connected, and able to deploy TeamCode.
- Camera: Logitech C920.
- Camera connection: directly connected to a Control Hub USB port.
- Camera mount: secured to a screwed-in mounted pole.
- Active configured hardware name: `logitechVisionWebcam`.
- Camera advertised capability: 1920 by 1080 pixels.
- Actual VisionPortal capture resolution: unknown. The pilot currently uses
  `VisionPortal.easyCreateWithDefaults(...)` and does not request a resolution.
- Calibration source and status: unknown; no calibration has been matched to the actual capture
  resolution.

## AprilTag

- Family: 36h11.
- ID: 22.
- Condition: flat and undamaged.
- Provenance: official FTC DECODE season printable poster.
- Measured black AprilTag square: 6.5 by 6.5 inches; the white border is excluded.
- Official cross-check: FIRST's DECODE printable instructions identify tag 22 as 36h11 and specify
  a 6.5-inch black square:
  `https://ftc-resources.firstinspires.org/ftc/field/apriltag-us`.

## Robot frame and camera mount

- Robot origin: drivetrain center of rotation projected onto the floor.
- Axis convention: +X right, +Y forward, +Z up.
- Lens-center position relative to the robot origin:
  - X: +6.875 inches.
  - Y: +4.875 inches.
  - Z: +19 inches.
- Camera orientation relative to the robot frame:
  - Yaw: 0 degrees.
  - Pitch: 0 degrees.
  - Roll: 0 degrees.
- Orientation measurement method: protractor and level.
- Orientation measurement uncertainty: not quantified; the values are measured nominal values,
  not exact mathematical angles.

## Stationary test controls

- Test area: clear, well lit, and controlled against unexpected movement or obstruction.
- Distance measurement: tape measure.
- Left/right reference: marked floor centerline.
- Angle measurement: protractor.
- Adult supervisor role: Director.
- Assigned Driver Station STOP operator role: Director.
- The vision-only pilot composition contains no drivetrain subsystem and must not command motors.

## Gate decision

### Open: ID-only stationary validation

The equipment, tag identity, tag size, controlled area, measurement tools, supervision, and STOP
operator are available. AV-08 may later validate tag ID, detection count, loss/reacquisition,
timestamp/age behavior, and safe portal shutdown after its own required preview and STOP-plan
confirmation.

### Closed: metric robot-relative validation

Metric range, bearing, elevation, position, and orientation validation remain closed because the
actual VisionPortal capture resolution and matching camera calibration are unknown. The pilot
must continue reporting ID-only observations and must not invent metric values.

## Software-only evidence

- Approved runtime: Microsoft OpenJDK 17.0.20.
- Command: `.\gradlew.bat --no-daemon --console=plain TeamCode:assembleDebug`.
- Result on 2026-08-19: `BUILD SUCCESSFUL`.

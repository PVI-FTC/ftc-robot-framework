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

## Calibration follow-up evidence — 2026-08-20

This addendum supersedes the calibration-unknown statement above for the later Stage 4
software gate; it does not establish physical metric accuracy.

- The Logitech C920 continued detecting tag IDs with a reported 640-by-480 VisionPortal stream.
- No camera-calibration warning was observed in the Driver Station preview or filtered Android
  Studio Logcat.
- Windows reported camera hardware identity `USB\VID_046D&PID_082D&MI_00`. VID `046D` and PID
  `082D` match the Logitech C920 identity used by the SDK's published built-in 640-by-480
  calibration.
- The calibration identity/resolution check is supported by this recorded evidence. MV-06 must
  still reconfirm the camera identity, name, configured stream, warning-free preview, tag
  metadata, mount facts, safety controls, and physical range/bearing accuracy before any physical
  validation result is accepted.

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

## MV-06 interrupted-session handoff — 2026-08-26

- The supervised stationary setup was reconfirmed: drive motors physically disconnected; reviewed
  C920/configured name; 640-by-480 preview with no calibration warning; official flat DECODE 36h11
  tag 22 with 6.5-inch black square; recorded mount; clear/lighted area; measurement tools; adult
  supervision; and a Driver Station STOP operator.
- Accepted validation tolerances: range error no greater than 2 inches or 10% of measured distance,
  whichever is larger; bearing error no greater than 5 degrees; and three-reading spreads no greater
  than 1 inch range and 2 degrees bearing.
- The first centered placement was set to 36 inches forward and 0 lateral. The diagnostic changed
  between `FRESH` and `RETAINED` frame statuses as expected. No fresh Robot range/bearing values
  were recorded before the supervised session ended.
- Resume only after reconfirming every MV-06 STOP prerequisite. Start by recording three `FRESH`,
  pose-available Robot range/bearing readings at the same centered placement; retained rows are not
  physical measurements.

## AV-08 stationary validation results

Validation was performed on 2026-08-19 with the drive motors physically disconnected and the
Director acting as both adult supervisor and Driver Station STOP operator.

- Initialization: diagnostic telemetry appeared without error; camera preview opened.
- Actual VisionPortal stream resolution reported during the run: 640 by 480 pixels.
- Camera availability after start: `true`.
- Visible tag result: ID 22, detection count 1, stable `Tracking` state.
- Example processing timestamp: 4977714512324 nanoseconds.
- Example processing age: 0.0974 milliseconds; age remained non-negative and small.
- Timestamp meaning: source processing time when the detection was copied, not camera exposure
  time.
- Loss test: detection count changed to 0, the old ID cleared, and the stable state became
  `Searching`.
- The one-loop `LostTarget` state was not visible at Driver Station telemetry refresh speed; this
  transient state was not physically confirmed.
- Reacquisition: count returned to 1, ID returned as 22, state returned to `Tracking`, and the
  processing timestamp advanced.
- Short stability check: ID 22, count 1, and `Tracking` remained stable with no reported camera or
  availability error.
- Stop: OpMode stopped without error or movement, and the camera preview closed.
- Resource reacquisition: a second INIT reopened the camera preview without a camera-in-use, USB,
  or portal error; the second stop also completed cleanly.
- Metric validation was not attempted. Matching Logitech C920 calibration at the now-observed
  640-by-480 stream resolution remains unverified.

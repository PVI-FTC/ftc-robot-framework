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

## MV-06 stationary metric validation results — 2026-09-02

The Stage 4 STOP prerequisites were reconfirmed one at a time. The drive motors were physically
disconnected, the Director was the adult supervisor and named Driver Station STOP operator, and
the previously recorded C920, FTC name, USB connection, 640-by-480 stream, calibration evidence,
tag 22, mount, lighting, measurement tools, and clear-area facts were unchanged. The Java 17
TeamCode build succeeded before deployment and again after physical testing.

The team retained its previously selected acceptance limits: robot-range error no greater than
2 inches or 10 percent of the measured placement, whichever is larger; robot-bearing error no
greater than 5 degrees; and three-reading spreads no greater than 1 inch range and 2 degrees
bearing. Every saved measurement below came from a `FRESH`, pose-available tag-22 observation.

### Centered distance observations

The robot origin and tag center shared the marked centerline and the robot remained square to the
tag. Camera expectations use the recorded +6.875-inch-right and +4.875-inch-forward mount.

| Robot placement | Camera range/bearing readings | Robot range/bearing readings | Robot result |
| --- | --- | --- | --- |
| 24 in forward, 0 in lateral | No measurement: only part of the tag was in frame and detection count was 0 | No pose available | Recorded as a close-range visibility limit, not an accuracy failure |
| 36 in forward, 0 in lateral | 28.50/+13.52; 28.54/+13.16; 28.50/+13.33 in/deg | 32.62/-0.54; 32.67/-0.66; 32.60/-0.54 in/deg | Mean 32.63 in/-0.58 deg; 3.37 in range error; 0.58 deg bearing error; 0.07 in/0.12 deg spread; pass |
| 48 in forward, 0 in lateral | 40.75/+9.30; 40.81/+9.31; 40.77/+9.30 in/deg | 45.09/-0.36; 45.15/-0.35; 45.11/-0.36 in/deg | Mean 45.12 in/-0.36 deg; 2.88 in range error; 0.36 deg bearing error; 0.06 in/0.01 deg spread; pass |
| 60 in forward, 0 in lateral | 52.27/+6.75; 52.30/+6.75; 52.27/+6.75 in/deg | 56.79/-0.74; 56.82/-0.73; 56.79/-0.74 in/deg | Mean 56.80 in/-0.74 deg; 3.20 in range error; 0.74 deg bearing error; 0.03 in/0.01 deg spread; pass |

At the 60-inch placement, a direct physical check measured 55.33 inches forward from the camera
lens plane to the tag plane. This agrees within 0.21 inch with the 55.125 inches predicted from
the robot placement and mount, but the mean reported camera range was 52.28 inches. The physical
reference and camera-to-robot translation therefore do not explain the approximately 3-inch-low
camera estimate.

### Image-position observations

The lens-plane-to-tag-plane forward distance remained 55.33 inches. The tag center was placed
12 inches to either side of the camera centerline, giving an expected camera planar range of
approximately 56.62 inches. From the recorded mount, the expected robot pose was approximately
60.42 inches/+4.87 degrees on the camera-left placement and 63.10 inches/-17.40 degrees on the
camera-right placement.

| Tag position | Camera range/bearing readings | Robot range/bearing readings | Robot result |
| --- | --- | --- | --- |
| 12 in left of camera | 52.37/+9.03; 52.42/+9.02; 52.39/+9.03 in/deg | 56.61/+1.36; 56.67/+1.36; 56.63/+1.36 in/deg | Mean 56.64 in/+1.36 deg; 3.79 in range error; 3.51 deg bearing error; 0.06 in/0.00 deg spread; pass |
| 12 in right of camera | 52.87/-11.85; 52.88/-11.85; 52.86/-11.86 in/deg | 59.34/-17.39; 59.34/-17.39; 59.32/-17.40 in/deg | Mean 59.33 in/-17.39 deg; 3.76 in range error; 0.01 deg bearing error; 0.02 in/0.01 deg spread; pass |

All tested robot-relative values were finite, repeatable, sign-correct, and within the selected
tolerances. Range remained biased low by roughly 3 to 4 inches, and camera-left bearing showed
more error than camera-right bearing. The next testable hypothesis is position-dependent error in
the built-in camera calibration or FTC pose estimate. Do not compensate by changing the measured
camera-to-robot mount values. The limitation must be resolved or explicitly accepted before these
experimental observations are used for localization or alignment.

### Freshness and lifecycle observations

- With tag 22 removed, a fresh empty result cleared the detection count, old ID, and pose and the
  vision state returned to `Searching`.
- While the tag remained absent, frame status continued changing between `FRESH` and `RETAINED`
  with zero detections and no stale ID or pose reappearing.
- Restoring tag 22 produced a newer fresh timestamp, detection count 1, pose available, and the
  `Tracking` state.
- STOP produced no error or movement and closed the preview. A second INIT reopened the preview
  without a camera-in-use, USB, or VisionPortal error; the second STOP also closed cleanly.
- No field pose, localization update, path selection, drivetrain command, or autonomous behavior
  was tested or authorized.

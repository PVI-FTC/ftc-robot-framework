package pvi.harness.runtime;

import java.util.List;

import pvi.harness.ds.Button;
import pvi.harness.ds.RumbleRecord;
import pvi.harness.physics.DriveModel;
import pvi.harness.physics.GamePiece;
import pvi.harness.physics.ModelValue;
import pvi.harness.physics.Pose;

/**
 * Everything that happened in one harness run, one entry per OpMode call, as plain data.
 *
 * <p>This is the shared snapshot that both displays read: the desktop viewer (Steps 12–15)
 * and the future Unity bridge (Phase 7). It only holds numbers, strings, booleans, enums and
 * lists of those, never FTC SDK objects, so it can be saved as JSON and read by any program.
 * Build one with {@link RunRecorder}; save it with {@link RunRecordingJson}.</p>
 *
 * <p>Units: distances in inches, angles in radians, times in seconds, unless a field's name
 * says otherwise (drive model values carry their own unit label).</p>
 *
 * @param header          what was run, on which robot, and the format version
 * @param loops           one entry per OpMode call (init, each init_loop, start, each loop, stop), oldest first
 * @param gamepad1Rumbles rumble commands sent to the driver's gamepad, oldest first
 * @param gamepad2Rumbles rumble commands sent to the operator's gamepad, oldest first
 */
public record RunRecording(Header header, List<LoopRecord> loops,
                           List<RumbleRecord> gamepad1Rumbles, List<RumbleRecord> gamepad2Rumbles,
                           List<Event> events) {

    /**
     * Format version. Raise it (and update the README) whenever any field changes.
     * Version 2 added {@link #events()}; version 3 added {@link LoopRecord#aprilTags()}; version 4
     * added the game pieces ({@link Header#pieces()} and {@link LoopRecord#pieces()}). Older files
     * still load (with no events, no tags, or no pieces).
     */
    public static final int VERSION = 4;

    /** Copies the lists, so the recording can't change after it is made. */
    public RunRecording {
        if (header == null) {
            throw new IllegalArgumentException("A run recording needs a header.");
        }
        loops = List.copyOf(loops);
        gamepad1Rumbles = List.copyOf(gamepad1Rumbles);
        gamepad2Rumbles = List.copyOf(gamepad2Rumbles);
        events = List.copyOf(events);
    }

    /** A recording with no events. */
    public RunRecording(Header header, List<LoopRecord> loops,
                        List<RumbleRecord> gamepad1Rumbles, List<RumbleRecord> gamepad2Rumbles) {
        this(header, loops, gamepad1Rumbles, gamepad2Rumbles, List.of());
    }

    /**
     * Something worth marking that isn't part of the robot's state, such as "Gamepad 1
     * controller disconnected" during a live drive.
     *
     * @param sinceInitSeconds seconds since INIT when it happened
     * @param matchTimeSeconds seconds since PLAY, or 0 before PLAY
     * @param text             what happened, in plain words
     */
    public record Event(double sinceInitSeconds, double matchTimeSeconds, String text) {
        public Event {
            if (text == null || text.isBlank()) {
                throw new IllegalArgumentException("An event needs some text.");
            }
        }
    }

    /**
     * Facts about the whole run.
     *
     * @param version           the format version, {@link #VERSION}
     * @param distanceUnit      always "inches"
     * @param angleUnit         always "radians"
     * @param timeUnit          always "seconds"
     * @param robotConfig       the fake robot's configuration name, e.g. "teamA"
     * @param opMode            the OpMode's class name, e.g. "TeamAAutoOpMode"
     * @param period            AUTONOMOUS or TELEOP, from the OpMode's @Autonomous / @TeleOp
     *                          annotation, or null if it has neither
     * @param loopPeriodSeconds simulated seconds per OpMode call
     * @param startPose         where the position estimate started, or null if position wasn't tracked
     * @param driveModel        the robot speeds and size used for the estimate, or null if not tracked
     * @param pieces            every game piece at INIT, in a fixed order (empty if position wasn't tracked)
     */
    public record Header(int version, String distanceUnit, String angleUnit, String timeUnit,
                         String robotConfig, String opMode, MatchPeriod period, double loopPeriodSeconds,
                         Pose startPose, DriveModelValues driveModel, List<GamePiece> pieces) {

        /** Copies the list, so the header can't change after it is made. */
        public Header {
            pieces = List.copyOf(pieces);
        }

        /** A header with no game pieces. */
        public Header(int version, String distanceUnit, String angleUnit, String timeUnit,
                      String robotConfig, String opMode, MatchPeriod period, double loopPeriodSeconds,
                      Pose startPose, DriveModelValues driveModel) {
            this(version, distanceUnit, angleUnit, timeUnit, robotConfig, opMode, period, loopPeriodSeconds,
                    startPose, driveModel, List.of());
        }
    }

    /**
     * The drive model's numbers, each with its unit, source, and measured/placeholder flag.
     *
     * @param name    e.g. "Team A"
     * @param forward full-power forward speed (in/s)
     * @param strafe  full-power sideways speed (in/s)
     * @param turn    full-power turn rate (deg/s, as the team measures it)
     * @param length  robot length, front to back (in)
     * @param width   robot width, side to side (in)
     */
    public record DriveModelValues(String name, ModelValue forward, ModelValue strafe, ModelValue turn,
                                   ModelValue length, ModelValue width) {

        /** Copies the values out of a drive model. */
        public static DriveModelValues of(DriveModel model) {
            return new DriveModelValues(model.name(), model.forward(), model.strafe(), model.turn(),
                    model.length(), model.width());
        }
    }

    /**
     * The robot right after one OpMode call.
     *
     * @param sinceInitSeconds seconds since INIT
     * @param matchTimeSeconds seconds since PLAY (0 before PLAY)
     * @param stage            which OpMode call this was
     * @param pose             estimated robot position, or null if position wasn't tracked
     * @param motors           every motor's power, in the order the configuration added them
     * @param servos           every servo's position, in the order the configuration added them
     * @param autoStep         the "Auto Step" telemetry value sent during this call, or null if none
     * @param telemetry        every telemetry screen sent during this call, in order (often one, sometimes none)
     * @param gamepad1         the driver's gamepad as the OpMode saw it during this call
     * @param gamepad2         the operator's gamepad as the OpMode saw it during this call
     * @param aprilTags        the AprilTags in each fake webcam's latest picture, i.e. what robot
     *                         code could detect during this call (empty with no webcam)
     * @param pieces           the game pieces that moved or changed state since the previous loop
     *                         entry (or since the header's list, for the first entry), where they
     *                         are now; empty if none did. Pieces keep the header's order.
     */
    public record LoopRecord(double sinceInitSeconds, double matchTimeSeconds, RunResult.Stage stage,
                             Pose pose, List<MotorState> motors, List<ServoState> servos,
                             String autoStep, List<TelemetryScreen> telemetry,
                             GamepadState gamepad1, GamepadState gamepad2, List<AprilTagState> aprilTags,
                             List<GamePiece> pieces) {

        /** Copies the lists, so the record can't change after it is made. */
        public LoopRecord {
            motors = List.copyOf(motors);
            servos = List.copyOf(servos);
            telemetry = List.copyOf(telemetry);
            aprilTags = List.copyOf(aprilTags);
            pieces = List.copyOf(pieces);
        }

        /** A loop entry in which no game piece moved. */
        public LoopRecord(double sinceInitSeconds, double matchTimeSeconds, RunResult.Stage stage,
                          Pose pose, List<MotorState> motors, List<ServoState> servos,
                          String autoStep, List<TelemetryScreen> telemetry,
                          GamepadState gamepad1, GamepadState gamepad2, List<AprilTagState> aprilTags) {
            this(sinceInitSeconds, matchTimeSeconds, stage, pose, motors, servos, autoStep, telemetry,
                    gamepad1, gamepad2, aprilTags, List.of());
        }

        /** A loop entry with no AprilTags in view (a robot without a webcam). */
        public LoopRecord(double sinceInitSeconds, double matchTimeSeconds, RunResult.Stage stage,
                          Pose pose, List<MotorState> motors, List<ServoState> servos,
                          String autoStep, List<TelemetryScreen> telemetry,
                          GamepadState gamepad1, GamepadState gamepad2) {
            this(sinceInitSeconds, matchTimeSeconds, stage, pose, motors, servos, autoStep, telemetry,
                    gamepad1, gamepad2, List.of(), List.of());
        }
    }

    /**
     * One AprilTag a fake webcam sees, measured from its lens like the SDK's {@code ftcPose}:
     * x right, y forward, z up. Range and bearing can be worked out from these
     * ({@code hypot(right, forward)}, {@code atan2(-right, forward)}).
     *
     * @param camera        the webcam's configuration name, e.g. "logitechVisionWebcam"
     * @param id            the tag's number, e.g. 21
     * @param rightInches   how far right of the lens (negative = left)
     * @param forwardInches how far in front of the lens
     * @param upInches      how far above the lens (negative = below)
     * @param yawDegrees    the tag's yaw, as the SDK reports it
     * @param pitchDegrees  the tag's pitch, as the SDK reports it
     * @param rollDegrees   the tag's roll, as the SDK reports it
     */
    public record AprilTagState(String camera, int id, double rightInches, double forwardInches, double upInches,
                                double yawDegrees, double pitchDegrees, double rollDegrees) {
    }

    /**
     * A gamepad's buttons and sticks at one moment.
     *
     * @param buttons      the buttons held down, in {@link Button} order
     * @param leftStickX   -1 (left) to 1 (right)
     * @param leftStickY   -1 (up) to 1 (down): the FTC convention, up is negative
     * @param rightStickX  -1 (left) to 1 (right)
     * @param rightStickY  -1 (up) to 1 (down)
     * @param leftTrigger  0 (released) to 1 (fully pressed)
     * @param rightTrigger 0 (released) to 1 (fully pressed)
     */
    public record GamepadState(List<Button> buttons, double leftStickX, double leftStickY,
                               double rightStickX, double rightStickY,
                               double leftTrigger, double rightTrigger) {

        /** Nothing pressed, sticks centered. */
        public static final GamepadState RELEASED = new GamepadState(List.of(), 0, 0, 0, 0, 0, 0);

        /** Copies the list, so the state can't change after it is made. */
        public GamepadState {
            buttons = List.copyOf(buttons);
        }

        /** True when no button is held and every stick and trigger is at rest. */
        public boolean isIdle() {
            return buttons.isEmpty() && leftStickX == 0 && leftStickY == 0 && rightStickX == 0
                    && rightStickY == 0 && leftTrigger == 0 && rightTrigger == 0;
        }
    }

    /**
     * One motor's power.
     *
     * @param name  configuration name, e.g. "frontLeft"
     * @param power the power the code set, -1 to 1
     */
    public record MotorState(String name, double power) {
    }

    /**
     * One servo's position.
     *
     * @param name     configuration name, e.g. "claw"
     * @param position the position the code set, 0 to 1, or NaN if the code never set one
     *                 (saved as {@code null} in JSON)
     */
    public record ServoState(String name, double position) {

        /** True once the code has set a position. */
        public boolean isSet() {
            return !Double.isNaN(position);
        }
    }

    /**
     * One Driver Station screen, as sent by one {@code telemetry.update()}.
     *
     * @param lines the data lines, in order
     * @param log   the log lines shown under the data
     */
    public record TelemetryScreen(List<String> lines, List<String> log) {

        /** Copies the lists, so the screen can't change after it is made. */
        public TelemetryScreen {
            lines = List.copyOf(lines);
            log = List.copyOf(log);
        }
    }
}

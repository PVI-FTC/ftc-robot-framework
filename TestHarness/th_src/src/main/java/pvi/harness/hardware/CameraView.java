package pvi.harness.hardware;

import org.firstinspires.ftc.robotcore.external.matrices.GeneralMatrixF;
import org.firstinspires.ftc.robotcore.external.matrices.MatrixF;
import org.firstinspires.ftc.robotcore.external.matrices.OpenGLMatrix;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder;
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference;
import org.firstinspires.ftc.robotcore.external.navigation.Orientation;

import java.util.ArrayList;
import java.util.List;

import pvi.harness.physics.Pose;

/**
 * Where a webcam sits on the robot and what it can see, so the fake webcam can work out by
 * itself which field AprilTags are in view (instead of a test scripting them).
 *
 * <p>The mount uses the same numbers as the SDK's {@code AprilTagProcessor.Builder.setCameraPose}:
 * the lens's position from the robot's centre on the floor (right, forward, up, inches) and its
 * yaw, pitch and roll (degrees, the SDK's convention: yaw 0, pitch -90, roll 0 is a camera looking
 * straight forward). So a team copies the numbers from its TeamCode, and the SDK's own pose maths
 * in robot code gets the robot's true field position back.</p>
 *
 * <p>A tag counts as in view when all of these hold:</p>
 * <ul>
 *   <li>its four corners are in front of the lens and inside the field of view (so the whole tag
 *       is on screen);</li>
 *   <li>it is no farther than {@code maxRangeInches} from the lens;</li>
 *   <li>its printed side faces the camera: the angle between the tag's facing direction and the
 *       direction back to the camera is at most {@code maxViewAngleDegrees} (so a tag seen from
 *       behind, or almost edge-on, isn't read; real AprilTag detection gets unreliable somewhere
 *       past 60 to 70 degrees).</li>
 * </ul>
 * <p><b>Not modelled:</b> anything blocking the view. No occlusion at all: not the robot's own
 * body, not other field elements, not game pieces. Lighting, blur and pixel noise aren't modelled
 * either, so a visible tag is always read with its exact pose.</p>
 *
 * @param rightInches          lens position right of the robot centre (negative = left)
 * @param forwardInches        lens position in front of the robot centre (negative = behind)
 * @param upInches             lens height above the floor
 * @param yawDegrees           as in {@code setCameraPose}
 * @param pitchDegrees         as in {@code setCameraPose} (-90 = looking forward, level)
 * @param rollDegrees          as in {@code setCameraPose}
 * @param horizontalFovDegrees the picture's full width as an angle
 * @param verticalFovDegrees   the picture's full height as an angle
 * @param maxRangeInches       farthest a tag can be and still be read
 * @param maxViewAngleDegrees  most a tag can be turned away from the camera and still be read
 */
public record CameraView(double rightInches, double forwardInches, double upInches,
                         double yawDegrees, double pitchDegrees, double rollDegrees,
                         double horizontalFovDegrees, double verticalFovDegrees,
                         double maxRangeInches, double maxViewAngleDegrees) {

    /** A Logitech C920-like picture (16:9): about 70 by 43 degrees. Placeholder until measured. */
    public static final double DEFAULT_HORIZONTAL_FOV_DEGREES = 70.4;
    public static final double DEFAULT_VERTICAL_FOV_DEGREES = 43.3;
    /** Placeholder: a 6.5 in tag is readable to about 10 ft at 640 x 480. */
    public static final double DEFAULT_MAX_RANGE_INCHES = 120.0;
    /** Placeholder: tags turned more than this from the camera aren't read. */
    public static final double DEFAULT_MAX_VIEW_ANGLE_DEGREES = 60.0;

    public CameraView {
        if (!(horizontalFovDegrees > 0 && horizontalFovDegrees < 180 && verticalFovDegrees > 0 && verticalFovDegrees < 180)) {
            throw new IllegalArgumentException("The field of view must be between 0 and 180 degrees each way, but was "
                    + horizontalFovDegrees + " by " + verticalFovDegrees);
        }
        if (!(maxRangeInches > 0) || !(maxViewAngleDegrees > 0 && maxViewAngleDegrees <= 90)) {
            throw new IllegalArgumentException("maxRangeInches must be more than 0 and maxViewAngleDegrees between 0 and 90,"
                    + " but were " + maxRangeInches + " and " + maxViewAngleDegrees);
        }
    }

    /**
     * The view of a webcam described in {@code harness.properties} ({@link CameraSetup}). The
     * settings file uses forward/left/up and "0, 0, 0 = level, looking forward" (yaw + left, pitch
     * + up); this converts that to the SDK's {@code setCameraPose} numbers, the one place it's
     * done: right = -left, yaw = yaw, pitch = pitch - 90, roll = roll.
     */
    public static CameraView from(CameraSetup setup) {
        return new CameraView(-setup.leftInches().value(), setup.forwardInches().value(), setup.upInches().value(),
                setup.yawDegrees().value(), setup.pitchDegrees().value() - 90, setup.rollDegrees().value(),
                setup.horizontalFovDegrees().value(), setup.verticalFovDegrees().value(),
                setup.maxRangeInches().value(), setup.maxViewAngleDegrees().value());
    }

    /** A camera at this spot looking straight forward and level, with the default lens and limits. */
    public static CameraView lookingForward(double rightInches, double forwardInches, double upInches) {
        return new CameraView(rightInches, forwardInches, upInches, 0, -90, 0,
                DEFAULT_HORIZONTAL_FOV_DEGREES, DEFAULT_VERTICAL_FOV_DEGREES,
                DEFAULT_MAX_RANGE_INCHES, DEFAULT_MAX_VIEW_ANGLE_DEGREES);
    }

    /**
     * The tags this camera sees with the robot at {@code robot}, as the SDK would measure them
     * ({@link SeenAprilTag}: right/forward/up from the lens and the tag's yaw/pitch/roll), in the
     * tags' list order.
     */
    public List<SeenAprilTag> tagsInView(Pose robot, List<FieldAprilTags.Tag> tags) {
        Frame camera = cameraInField(robot);
        List<SeenAprilTag> seen = new ArrayList<>();
        for (FieldAprilTags.Tag tag : tags) {
            SeenAprilTag view = see(camera, tag);
            if (view != null) {
                seen.add(view);
            }
        }
        return seen;
    }

    // ---- The maths --------------------------------------------------------------------------

    /**
     * A position and three axes in the field frame. For the camera, the axes are the SDK's
     * (OpenCV) camera frame: x right, y down, z out of the lens. For a tag, the SDK's tag frame:
     * x to the right as seen from the front, y down, z into the tag.
     */
    private record Frame(double[] origin, double[] x, double[] y, double[] z) {
    }

    /** Where the lens is on the field, and which way its axes point, with the robot at {@code robot}. */
    private Frame cameraInField(Pose robot) {
        // The camera's axes in the robot's frame (x right, y forward, z up), built exactly as the
        // SDK's AprilTagProcessor.Builder does from setCameraPose: an intrinsic Z-X-Z rotation.
        OpenGLMatrix inRobot = new Orientation(AxesReference.INTRINSIC, AxesOrder.ZXZ, AngleUnit.DEGREES,
                (float) yawDegrees, (float) pitchDegrees, (float) rollDegrees, 0).getRotationMatrix();
        double h = robot.headingRadians();
        // The robot's own axes in the field: forward along its heading, right 90 degrees clockwise.
        double[] robotRight = {Math.sin(h), -Math.cos(h), 0};
        double[] robotForward = {Math.cos(h), Math.sin(h), 0};
        double[] robotUp = {0, 0, 1};
        double[] origin = {
                robot.xInches() + rightInches * robotRight[0] + forwardInches * robotForward[0],
                robot.yInches() + rightInches * robotRight[1] + forwardInches * robotForward[1],
                upInches};
        double[][] axes = new double[3][];
        for (int column = 0; column < 3; column++) {
            double r = inRobot.get(0, column);
            double f = inRobot.get(1, column);
            double u = inRobot.get(2, column);
            axes[column] = new double[] {
                    r * robotRight[0] + f * robotForward[0] + u * robotUp[0],
                    r * robotRight[1] + f * robotForward[1] + u * robotUp[1],
                    r * robotRight[2] + f * robotForward[2] + u * robotUp[2]};
        }
        return new Frame(origin, axes[0], axes[1], axes[2]);
    }

    /**
     * A tag's frame in the field, the way the SDK's tag library defines it: z into the tag
     * (against its facing direction), x to the right as seen from the front, y down. (Checked
     * against the SDK's own orientation for DECODE tag 20.)
     */
    private static Frame tagInField(FieldAprilTags.Tag tag) {
        double[] z = {-tag.facingX(), -tag.facingY(), -tag.facingZ()};
        double[] up = {0, 0, 1};
        double[] x = cross(z, up);
        if (length(x) < 1e-6) {
            x = new double[] {1, 0, 0};   // a tag facing straight up or down: any "right" will do
        }
        x = scale(x, 1 / length(x));
        double[] y = cross(z, x);
        return new Frame(new double[] {tag.xInches(), tag.yInches(), tag.zInches()}, x, y, z);
    }

    /** The tag as the camera sees it, or null if it isn't in view. */
    private SeenAprilTag see(Frame camera, FieldAprilTags.Tag tag) {
        Frame t = tagInField(tag);
        double[] toTag = minus(t.origin(), camera.origin());
        double range = length(toTag);
        if (range > maxRangeInches || range < 1e-6) {
            return null;
        }
        // Facing the camera? The tag's facing direction against the direction back to the lens.
        double[] facing = {tag.facingX(), tag.facingY(), tag.facingZ()};
        double cosine = -dot(facing, toTag) / range;
        if (cosine < Math.cos(Math.toRadians(maxViewAngleDegrees))) {
            return null;
        }
        // All four corners on screen?
        double half = tag.sizeInches() / 2;
        for (int[] corner : new int[][] {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}}) {
            double[] point = plus(t.origin(), plus(scale(t.x(), corner[0] * half), scale(t.y(), corner[1] * half)));
            if (!onScreen(camera, minus(point, camera.origin()))) {
                return null;
            }
        }
        // The pose the SDK's solver would report: the tag centre and axes in the camera frame.
        double rightOfLens = dot(toTag, camera.x());
        double belowLens = dot(toTag, camera.y());
        double outOfLens = dot(toTag, camera.z());
        MatrixF rotation = new GeneralMatrixF(3, 3);
        double[][] tagAxes = {t.x(), t.y(), t.z()};
        for (int column = 0; column < 3; column++) {
            rotation.put(0, column, (float) dot(tagAxes[column], camera.x()));
            rotation.put(1, column, (float) dot(tagAxes[column], camera.y()));
            rotation.put(2, column, (float) dot(tagAxes[column], camera.z()));
        }
        // The same angle split the SDK uses for ftcPose (intrinsic Y-X-Z, yaw negated).
        Orientation angles = Orientation.getOrientation(rotation, AxesReference.INTRINSIC, AxesOrder.YXZ, AngleUnit.DEGREES);
        return new SeenAprilTag(tag.id(), rightOfLens, outOfLens, -belowLens,
                -angles.firstAngle, angles.secondAngle, angles.thirdAngle);
    }

    /** True if a point (from the lens, in field axes) is in front of the lens and inside the field of view. */
    private boolean onScreen(Frame camera, double[] fromLens) {
        double x = dot(fromLens, camera.x());
        double y = dot(fromLens, camera.y());
        double z = dot(fromLens, camera.z());
        if (z <= 0) {
            return false;
        }
        return Math.abs(Math.toDegrees(Math.atan2(x, z))) <= horizontalFovDegrees / 2
                && Math.abs(Math.toDegrees(Math.atan2(y, z))) <= verticalFovDegrees / 2;
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    private static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    private static double length(double[] a) {
        return Math.sqrt(dot(a, a));
    }

    private static double[] scale(double[] a, double k) {
        return new double[] {a[0] * k, a[1] * k, a[2] * k};
    }

    private static double[] plus(double[] a, double[] b) {
        return new double[] {a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    private static double[] minus(double[] a, double[] b) {
        return new double[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }
}

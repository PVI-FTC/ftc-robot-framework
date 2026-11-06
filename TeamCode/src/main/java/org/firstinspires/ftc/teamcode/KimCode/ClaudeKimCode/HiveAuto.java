package org.firstinspires.ftc.teamcode.KimCode.ClaudeKimCode;

import com.pedropathing.api.Paths;
import com.pedropathing.follower.Follower;
import com.pedropathing.math.Pose;
import com.pedropathing.paths.Path;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.pedro.Constants;

/**
 * The BIOBUZZ autonomous: locate the raised CELL, launch into it, and park in the LOADING ZONE.
 *
 * <p>{@link HiveAutoRed} and {@link HiveAutoBlue} only choose the alliance. Each pass through
 * {@link #loop()} checks the clock, runs the current stage for a moment, and updates the follower
 * once. Nothing here waits or sleeps, so the clock is always being watched and the robot can
 * always head for the LOADING ZONE in time.</p>
 *
 * <p>This OpMode owns its follower directly, which is a recorded exception to the usual layers.
 * See "KimCode Hive Auto exception" in ARCHITECTURE.md.</p>
 */
public abstract class HiveAuto extends OpMode {
    private enum Stage {
        LOCATE_DRIVE, LOCATE_READ, LAUNCH_CLEAR_FRAME, LAUNCH_DRIVE, AIM, LAUNCH,
        RETURN_CLEAR_FRAME, RETURN, DONE
    }

    private static final int PRELOADED_BALLS = 4;

    /**
     * From this time on, whatever the robot is doing, it drives to the LOADING ZONE. The drive from
     * the launch spot took about 10 s at the path speed below in the harness, so this leaves time.
     */
    private static final double RETURN_CUTOFF_SECONDS = 18.0;
    /** The robot stops here even if it hasn't arrived, so it is motionless when AUTO ends. */
    private static final double HARD_STOP_SECONDS = 29.0;
    /** Share of the robot's top speed used on paths. Kept low for first runs; raise it later. */
    private static final double PATH_SPEED_FRACTION = 0.30;
    /** How long the robot looks for the HIVE's tags before moving on without an answer. */
    private static final double LOCATE_TIME_LIMIT_SECONDS = 3.0;
    /** How long the robot may spend lining up on the tags before firing anyway. */
    private static final double AIM_TIME_LIMIT_SECONDS = 2.0;
    private static final double AIM_TOLERANCE_DEGREES = 2.0;
    /** Gives each turn time to finish, and the camera time to see it, before the next one. */
    private static final double AIM_CORRECTION_SECONDS = 0.25;

    private final ElapsedTime matchTimer = new ElapsedTime();
    private final ElapsedTime stageTimer = new ElapsedTime();
    private final ElapsedTime aimTimer = new ElapsedTime();
    /** When each stage began, for timing a run. See HIVE_AUTO_TEST_PLAN.md. */
    private final StringBuilder stageTimes = new StringBuilder();
    private Follower follower;
    private KimHiveVision vision;
    private KimLauncher launcher;
    private Stage stage = Stage.LOCATE_DRIVE;
    /** If vision never gives an answer, the robot assumes the HIVE hasn't tipped. */
    private boolean targetIsStartRaisedCell = true;
    private boolean usingVision;

    /** Which alliance this OpMode plays. */
    protected abstract HiveField.Alliance alliance();

    @Override
    public void init() {
        follower = Constants.create(hardwareMap);
        follower.holdEnd.set(true);
        follower.setPose(startPose());
        vision = new KimHiveVision(hardwareMap, alliance());
        vision.start();
        launcher = new KimLauncher(hardwareMap);
        telemetry.addData("Hive Auto", "%s, ready", alliance());
        telemetry.addData("Limelight found", vision.isAvailable());
        telemetry.addData("Launcher found", launcher.isAvailable());
        telemetry.update();
    }

    @Override
    public void start() {
        matchTimer.reset();
        enterStage(Stage.LOCATE_DRIVE);
        // Set again because the Pinpoint may have still been starting up during init.
        follower.setPose(startPose());
        // The follower only learns the new pose on its next update, so name the start here.
        drive(startPose(), fieldPose(HiveField.VIEW_X_INCHES, HiveField.VIEW_Y_INCHES));
    }

    @Override
    public void loop() {
        checkClock();
        runStage();
        launcher.update();
        follower.update();
        publishTelemetry();
    }

    @Override
    public void stop() {
        if (follower != null) {
            stopDriving();
        }
        if (vision != null) {
            vision.stop();
        }
        if (launcher != null) {
            launcher.stop();
        }
    }

    /** The clock outranks every stage: this is what makes sure the robot parks in time. */
    private void checkClock() {
        double seconds = matchTimer.seconds();
        if (stage != Stage.DONE && seconds >= HARD_STOP_SECONDS) {
            enterDone();
        } else if (stage != Stage.DONE && stage != Stage.RETURN_CLEAR_FRAME
                && stage != Stage.RETURN && seconds >= RETURN_CUTOFF_SECONDS) {
            enterReturn();
        }
    }

    private void runStage() {
        switch (stage) {
            case LOCATE_DRIVE:
                if (!follower.following()) {
                    enterStage(Stage.LOCATE_READ);
                }
                break;
            case LOCATE_READ:
                vision.update(follower.pose());
                if (vision.finder().answer() != HiveCellFinder.Answer.NOT_SURE
                        || !vision.isAvailable()
                        || stageTimer.seconds() >= LOCATE_TIME_LIMIT_SECONDS) {
                    chooseTarget();
                    enterLaunchDrive();
                }
                break;
            case LAUNCH_CLEAR_FRAME:
                if (!follower.following()) {
                    enterStage(Stage.LAUNCH_DRIVE);
                    driveTo(launchSpot());
                }
                break;
            case LAUNCH_DRIVE:
                if (!follower.following()) {
                    if (usingVision) {
                        enterStage(Stage.AIM);
                        aimTimer.reset();
                    } else {
                        enterLaunch();
                    }
                }
                break;
            case AIM:
                runAim();
                break;
            case LAUNCH:
                if (launcher.isDoneFiring()) {
                    enterReturn();
                }
                break;
            case RETURN_CLEAR_FRAME:
                if (!follower.following()) {
                    enterStage(Stage.RETURN);
                    driveTo(fieldPose(HiveField.PARK_X_INCHES, HiveField.PARK_Y_INCHES));
                }
                break;
            case RETURN:
                if (!follower.following()) {
                    enterDone();
                }
                break;
            case DONE:
            default:
                break;
        }
    }

    /** Without an answer from vision, the robot makes the preset shot and skips the AIM stage. */
    private void chooseTarget() {
        HiveCellFinder.Answer answer = vision.finder().answer();
        usingVision = answer != HiveCellFinder.Answer.NOT_SURE;
        targetIsStartRaisedCell = answer != HiveCellFinder.Answer.TIPPED;
    }

    /** Spins up during the drive, so the wheel is ready as soon as the robot arrives. */
    private void enterLaunchDrive() {
        enterStage(Stage.LAUNCH_CLEAR_FRAME);
        Pose launchSpot = launchSpot();
        launcher.spinUpTo(HiveShotTable.speedForDistance(HiveField.distanceToCellInches(
                alliance(), targetIsStartRaisedCell, launchSpot.x(), launchSpot.y())));
        // Finishes turning on this first leg, so the last leg is straight at the CELL's opening.
        driveTo(clearOfFrameSpot(HiveField.launchHeadingDegrees(targetIsStartRaisedCell)));
    }

    /**
     * Lines up on the raised CELL and sets the launcher speed from what the camera sees. If the
     * tags can't be seen, the robot keeps its Pinpoint aim and speed, and fires at the time limit.
     */
    private void runAim() {
        Pose pose = follower.pose();
        vision.update(pose);
        KimHiveVision.CellView target = targetIsStartRaisedCell
                ? vision.startRaisedCell() : vision.startLoweredCell();

        if (target.isVisible()) {
            setSpeedFromTags(target);
        }
        // The four tags span 13 inches, so only all of them together show the CELL's center.
        boolean allTagsSeen = target.tagCount() == KimHiveVision.TAGS_PER_CELL;
        boolean aimed = allTagsSeen
                && Math.abs(target.targetXDegrees()) <= AIM_TOLERANCE_DEGREES;

        if (aimed || stageTimer.seconds() >= AIM_TIME_LIMIT_SECONDS) {
            enterLaunch();
        } else if (allTagsSeen && aimTimer.seconds() >= AIM_CORRECTION_SECONDS) {
            aimTimer.reset();
            // A tag to the right has a positive angle, and turning right lowers the heading.
            follower.hold(pose.withHeading(
                    pose.heading() - Math.toRadians(target.targetXDegrees())));
        }
    }

    private void setSpeedFromTags(KimHiveVision.CellView target) {
        double cameraDistance = HiveCellFinder.distanceToRaisedTagInches(target.targetYDegrees());
        if (!Double.isNaN(cameraDistance)) {
            launcher.spinUpTo(HiveShotTable.speedForDistance(
                    cameraDistance + HiveField.CAMERA_FORWARD_INCHES));
        }
    }

    private void enterLaunch() {
        enterStage(Stage.LAUNCH);
        launcher.fire(PRELOADED_BALLS);
    }

    private void enterReturn() {
        enterStage(Stage.RETURN_CLEAR_FRAME);
        launcher.stop();
        driveTo(clearOfFrameSpot(HiveField.FACING_HIVE_DEGREES));
    }

    private void enterDone() {
        enterStage(Stage.DONE);
        launcher.stop();
        follower.stop();
    }

    private void enterStage(Stage newStage) {
        stage = newStage;
        stageTimer.reset();
        stageTimes.append(String.format("%s %.1f  ", newStage, matchTimer.seconds()));
    }

    /** Starts one straight path from wherever the robot is now. */
    private void driveTo(Pose target) {
        drive(follower.pose(), target);
    }

    private void drive(Pose from, Pose target) {
        Path path = Paths.line(from, target).linear(from, target)
                .with(Constants.foresightConfig.maxPathSpeed.at(PATH_SPEED_FRACTION));
        follower.follow(path);
    }

    /** Follower.stop() only changes its mode. The motors stop on the update() after it. */
    private void stopDriving() {
        follower.stop();
        follower.update();
    }

    private Pose startPose() {
        return fieldPose(HiveField.START_X_INCHES, HiveField.START_Y_INCHES);
    }

    private Pose launchSpot() {
        return HiveField.pose(alliance(), HiveField.launchXInches(targetIsStartRaisedCell),
                HiveField.CELL_Y_INCHES, HiveField.launchHeadingDegrees(targetIsStartRaisedCell));
    }

    private Pose clearOfFrameSpot(double headingDegrees) {
        return HiveField.pose(alliance(), HiveField.clearOfFrameXInches(targetIsStartRaisedCell),
                HiveField.CLEAR_OF_FRAME_Y_INCHES, headingDegrees);
    }

    private Pose fieldPose(double xInches, double yInches) {
        return HiveField.pose(alliance(), xInches, yInches, HiveField.FACING_HIVE_DEGREES);
    }

    private void publishTelemetry() {
        Pose pose = follower.pose();
        telemetry.addData("Alliance", alliance());
        telemetry.addData("Stage", stage);
        telemetry.addData("Time", "%.1f s", matchTimer.seconds());
        telemetry.addData("Pose", "(%.1f, %.1f, %.0f deg)", pose.x(), pose.y(),
                Math.toDegrees(pose.heading()));
        telemetry.addData("Raised CELL", vision.finder().answer());
        telemetry.addData("Shot", "%s, %s", usingVision ? "using vision" : "preset",
                targetIsStartRaisedCell ? "start-raised CELL" : "start-lowered CELL");
        telemetry.addData("Launcher", "%s, %.0f / %.0f ticks/s, %d shots left",
                launcher.stepName(), launcher.actualSpeed(), launcher.targetSpeed(),
                launcher.shotsLeft());
        telemetry.addData("Stage times", stageTimes);
        telemetry.update();
    }
}

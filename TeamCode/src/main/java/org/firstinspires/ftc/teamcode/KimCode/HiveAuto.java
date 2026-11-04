package org.firstinspires.ftc.teamcode.KimCode;

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
    private enum Stage { LOCATE_DRIVE, LOCATE_READ, LAUNCH_DRIVE, LAUNCH, RETURN, DONE }

    private static final int PRELOADED_BALLS = 4;

    /** From this time on, whatever the robot is doing, it drives to the LOADING ZONE. */
    private static final double RETURN_CUTOFF_SECONDS = 23.0;
    /** The robot stops here even if it hasn't arrived, so it is motionless when AUTO ends. */
    private static final double HARD_STOP_SECONDS = 29.0;
    /** Share of the robot's top speed used on paths. Kept low for first runs; raise it later. */
    private static final double PATH_SPEED_FRACTION = 0.30;
    /** How long the robot looks for the HIVE's tags before moving on without an answer. */
    private static final double LOCATE_TIME_LIMIT_SECONDS = 3.0;

    private final ElapsedTime matchTimer = new ElapsedTime();
    private final ElapsedTime stageTimer = new ElapsedTime();
    private Follower follower;
    private KimHiveVision vision;
    private KimLauncher launcher;
    private Stage stage = Stage.LOCATE_DRIVE;

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
        // Set again because the Pinpoint may have still been starting up during init.
        follower.setPose(startPose());
        driveTo(fieldPose(HiveField.VIEW_X_INCHES, HiveField.VIEW_Y_INCHES));
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
        } else if (stage != Stage.DONE && stage != Stage.RETURN
                && seconds >= RETURN_CUTOFF_SECONDS) {
            enterReturn();
        }
    }

    private void runStage() {
        switch (stage) {
            case LOCATE_DRIVE:
                if (!follower.following()) {
                    stage = Stage.LOCATE_READ;
                    stageTimer.reset();
                }
                break;
            case LOCATE_READ:
                // For now the answer is only shown on telemetry. The robot keeps holding still.
                vision.update(follower.pose());
                if (vision.finder().answer() != HiveCellFinder.Answer.NOT_SURE
                        || !vision.isAvailable()
                        || stageTimer.seconds() >= LOCATE_TIME_LIMIT_SECONDS) {
                    enterLaunchDrive();
                }
                break;
            case LAUNCH_DRIVE:
                if (!follower.following()) {
                    stage = Stage.LAUNCH;
                    launcher.fire(PRELOADED_BALLS);
                }
                break;
            case LAUNCH:
                if (launcher.isDoneFiring()) {
                    enterReturn();
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

    /** Spins up during the drive, so the wheel is ready as soon as the robot arrives. */
    private void enterLaunchDrive() {
        stage = Stage.LAUNCH_DRIVE;
        boolean targetIsStartRaisedCell = true;
        Pose launchSpot = fieldPose(HiveField.cellXInches(targetIsStartRaisedCell),
                HiveField.LAUNCH_Y_INCHES);
        launcher.spinUpTo(HiveShotTable.speedForDistance(HiveField.distanceToCellInches(
                alliance(), targetIsStartRaisedCell, launchSpot.x(), launchSpot.y())));
        driveTo(launchSpot);
    }

    private void enterReturn() {
        stage = Stage.RETURN;
        launcher.stop();
        driveTo(fieldPose(HiveField.PARK_X_INCHES, HiveField.PARK_Y_INCHES));
    }

    private void enterDone() {
        stage = Stage.DONE;
        launcher.stop();
        follower.stop();
    }

    /** Starts one straight path from wherever the robot is now. */
    private void driveTo(Pose target) {
        Pose from = follower.pose();
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
        telemetry.addData("Launcher", "%s, %.0f / %.0f ticks/s, %d shots left",
                launcher.stepName(), launcher.actualSpeed(), launcher.targetSpeed(),
                launcher.shotsLeft());
        telemetry.update();
    }
}

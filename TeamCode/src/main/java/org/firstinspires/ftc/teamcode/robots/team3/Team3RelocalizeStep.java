package org.firstinspires.ftc.teamcode.robots.team3;

import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.common.autonomous.AutoStep;

/**
 * Non-blocking autonomous step that checks for a visible DECODE Goal AprilTag
 * (ID 20 Blue or ID 24 Red) and updates the Pedro 3 follower pose from the
 * camera's field-pose candidate.
 *
 * <p>If a valid tag is immediately visible, relocalization completes on the first loop.
 * Otherwise it waits up to {@code maxWaitSeconds} before continuing safely using Pinpoint
 * odometry so the autonomous sequence never gets stuck if the tag is blocked.</p>
 */
public final class Team3RelocalizeStep implements AutoStep {
    public static final double DEFAULT_MAX_WAIT_SECONDS = 0.25;

    private final Team3PedroRobot robot;
    private final double maxWaitSeconds;
    private final ElapsedTime timer = new ElapsedTime();
    private boolean started;
    private boolean relocalized;
    private int relocalizedTagId = -1;

    /** Creates a relocalization step that waits up to 0.25 seconds for a fresh goal tag. */
    public Team3RelocalizeStep(Team3PedroRobot robot) {
        this(robot, DEFAULT_MAX_WAIT_SECONDS);
    }

    /** Creates a relocalization step with a custom maximum wait timeout in seconds. */
    public Team3RelocalizeStep(Team3PedroRobot robot, double maxWaitSeconds) {
        if (robot == null) {
            throw new IllegalArgumentException("Team 3 relocalize step needs a robot.");
        }
        if (maxWaitSeconds < 0.0) {
            throw new IllegalArgumentException("Max wait duration must be non-negative.");
        }
        this.robot = robot;
        this.maxWaitSeconds = maxWaitSeconds;
    }

    @Override
    public void start() {
        timer.reset();
        started = true;
        relocalized = false;
        relocalizedTagId = -1;
        robot.enableVision();
        tryRelocalize();
    }

    @Override
    public void update() {
        if (!started || relocalized) {
            return;
        }
        tryRelocalize();
    }

    @Override
    public boolean isFinished() {
        return started && (relocalized || timer.seconds() >= maxWaitSeconds);
    }

    @Override
    public void stop() {
        // Vision remains enabled; no motor outputs are owned by this step.
    }

    @Override
    public String getName() {
        return relocalized ? "Relocalized (Tag " + relocalizedTagId + ")" : "Relocalize (AprilTag)";
    }

    private void tryRelocalize() {
        int tagId = robot.relocalizeFromAprilTag();
        if (tagId >= 0) {
            relocalized = true;
            relocalizedTagId = tagId;
        }
    }
}


package org.firstinspires.ftc.teamcode.robots.team3;

import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.common.autonomous.AutoStep;

/**
 * Reusable non-blocking autonomous step that spins up Team 3's outtake (shooter)
 * and feeds a game element with the midtake to score.
 */
public final class Team3ScoreStep implements AutoStep {
    public static final double DEFAULT_OUTTAKE_POWER = 1.0;
    public static final double DEFAULT_SPINUP_SECONDS = 0.5;
    public static final double DEFAULT_FEED_SECONDS = 1.0;

    private final Team3PedroRobot robot;
    private final double outtakePower;
    private final double spinupSeconds;
    private final double totalDurationSeconds;
    private final ElapsedTime timer = new ElapsedTime();
    private boolean started;

    /** Creates a scoring step with default spin-up (0.5s), feed (1.0s), and full shooter power (1.0). */
    public Team3ScoreStep(Team3PedroRobot robot) {
        this(robot, DEFAULT_OUTTAKE_POWER, DEFAULT_SPINUP_SECONDS, DEFAULT_FEED_SECONDS);
    }

    /**
     * Creates a scoring step with custom shooter power, spin-up delay, and midtake feed duration.
     *
     * @param robot         Team 3's Pedro autonomous robot
     * @param outtakePower  shooter motor power (-1.0 to 1.0)
     * @param spinupSeconds seconds to spin the shooter before starting the midtake feeder
     * @param feedSeconds   seconds to run the midtake feeder while shooting
     */
    public Team3ScoreStep(Team3PedroRobot robot, double outtakePower,
                          double spinupSeconds, double feedSeconds) {
        if (robot == null) {
            throw new IllegalArgumentException("Team 3 score step needs a robot.");
        }
        if (spinupSeconds < 0.0 || feedSeconds < 0.0) {
            throw new IllegalArgumentException("Spin-up and feed durations must be non-negative.");
        }
        this.robot = robot;
        this.outtakePower = outtakePower;
        this.spinupSeconds = spinupSeconds;
        this.totalDurationSeconds = spinupSeconds + feedSeconds;
    }

    @Override
    public void start() {
        timer.reset();
        started = true;
        robot.setOuttakePower(outtakePower);
        robot.setMidtakeFeeding(false);
    }

    @Override
    public void update() {
        if (!started) {
            return;
        }
        if (timer.seconds() >= spinupSeconds) {
            robot.setMidtakeFeeding(true);
        }
    }

    @Override
    public boolean isFinished() {
        return started && timer.seconds() >= totalDurationSeconds;
    }

    @Override
    public void stop() {
        robot.setMidtakeFeeding(false);
        robot.setOuttakePower(0.0);
    }

    @Override
    public String getName() {
        return "Score (Spinup + Feed)";
    }
}


package pvi.harness.hardware;

import pvi.harness.physics.Pose;

/**
 * A dead wheel (odometry pod) plugged into a motor's encoder port: robot code reads it with
 * {@code hardwareMap.get(DcMotorEx.class, "leftOdo").getCurrentPosition()}, and the count
 * follows how the robot really rolled, not the motor's power.
 *
 * <p>Two kinds, as in two- and three-wheel odometry:</p>
 * <ul>
 *   <li>{@link #parallel}: rolls when the robot drives forward; mounted {@code leftInches} to the
 *       left of the robot's center (negative = right), so it also turns when the robot spins;</li>
 *   <li>{@link #perpendicular}: rolls when the robot strafes left; mounted {@code forwardInches}
 *       ahead of the center (negative = behind).</li>
 * </ul>
 * <p>It sets the port's encoder each loop through {@link FakeDcMotorEx#simulateEncoder}. If the
 * port also drives a motor (an intake, say), that motor's power still works; only its encoder
 * reading comes from the pod. Readings only: the harness's own position tracking still comes
 * from the drive motors and Pinpoint (decided 2026-10-25).</p>
 *
 * <p>Add one with {@code hardwareMap.addDeadWheel(FakeDeadWheel.parallel("leftOdo", 7.0))}.</p>
 */
public class FakeDeadWheel extends FakeDevice implements SimulatedSensor {
    /**
     * Counts per inch of a goBILDA swingarm odometry pod (2000 counts per turn of a 48 mm wheel,
     * the same figure {@code FakePinpoint} uses).
     */
    public static final double GOBILDA_TICKS_PER_INCH = FakePinpoint.SWINGARM_TICKS_PER_MM * 25.4;

    private final boolean parallel;
    private final double offsetInches;
    private double ticksPerInch = GOBILDA_TICKS_PER_INCH;
    private boolean reversed;
    /** The motor port whose encoder this pod drives (set when added to a hardware map). */
    private FakeDcMotorEx port;

    private Pose last;
    private double travelInches;
    /** The count this pod last wrote to the port, to notice robot code resetting the encoder. */
    private int lastWrittenTicks;

    private FakeDeadWheel(String portName, boolean parallel, double offsetInches) {
        super(portName);
        this.parallel = parallel;
        this.offsetInches = offsetInches;
    }

    /** A pod that rolls forward with the robot, {@code leftInches} left of center (negative = right). */
    public static FakeDeadWheel parallel(String portName, double leftInches) {
        return new FakeDeadWheel(portName, true, leftInches);
    }

    /** A pod that rolls when the robot strafes left, {@code forwardInches} ahead of center (negative = behind). */
    public static FakeDeadWheel perpendicular(String portName, double forwardInches) {
        return new FakeDeadWheel(portName, false, forwardInches);
    }

    /** Counts per inch of pod travel (default: goBILDA swingarm pod). */
    public FakeDeadWheel ticksPerInch(double ticksPerInch) {
        this.ticksPerInch = ticksPerInch;
        return this;
    }

    /** The pod is mounted the other way round, so it counts down where it would count up. */
    public FakeDeadWheel reversed() {
        this.reversed = true;
        return this;
    }

    /** Called by {@code FakeHardwareMap.addDeadWheel}: the encoder port this pod is plugged into. */
    void plugInto(FakeDcMotorEx port) {
        this.port = port;
    }

    /** How far the pod has rolled, in inches (positive forward or left). */
    public double travelInches() {
        return travelInches;
    }

    @Override
    public void step(Step now) {
        if (now.robot() == null) {
            return;   // not tracked: the pod doesn't move
        }
        Pose robot = now.robot();
        double velocityTicks = 0;
        // Robot code reset the encoder (STOP_AND_RESET_ENCODER zeroes the port): count from 0 again.
        if (port != null && lastWrittenTicks != 0 && port.getCurrentPosition() == 0) {
            travelInches = 0;
        }
        if (last != null) {
            // The robot's move since the last step, in its own frame (forward, left, turn).
            double dx = robot.xInches() - last.xInches();
            double dy = robot.yInches() - last.yInches();
            double turn = Math.IEEEremainder(robot.headingRadians() - last.headingRadians(), 2 * Math.PI);
            double mid = last.headingRadians() + turn / 2;
            double forward = dx * Math.cos(mid) + dy * Math.sin(mid);
            double left = -dx * Math.sin(mid) + dy * Math.cos(mid);
            // A pod off center also rolls when the robot turns (same formula as FakePinpoint's pods).
            double rolled = parallel ? forward - offsetInches * turn : left + offsetInches * turn;
            travelInches += rolled;
            if (now.loopSeconds() > 0) {
                velocityTicks = rolled * ticksPerInch / now.loopSeconds() * (reversed ? -1 : 1);
            }
        }
        last = robot;
        if (port != null) {
            lastWrittenTicks = (int) Math.round(travelInches * ticksPerInch * (reversed ? -1 : 1));
            port.simulateEncoder(lastWrittenTicks, velocityTicks);
        }
    }
}

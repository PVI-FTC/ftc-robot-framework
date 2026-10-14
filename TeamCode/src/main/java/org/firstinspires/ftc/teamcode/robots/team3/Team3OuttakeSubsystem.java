package org.firstinspires.ftc.teamcode.robots.team3;

import org.firstinspires.ftc.teamcode.core.fsm.FSM;
import org.firstinspires.ftc.teamcode.core.fsm.Transition;
import org.firstinspires.ftc.teamcode.core.robot.Subsystem;

/** Controls Team 3's outtake (shooter) motor with a two-state finite-state machine. */
public class Team3OuttakeSubsystem implements Subsystem {
    /** Requests below this power are treated as stopped so a resting trigger does not hum the motor. */
    public static final double POWER_DEADBAND = 0.05;

    private final Team3MechanismHardware hardware;
    private final Team3OuttakeIdleState idleState;
    private final Team3OuttakeSpinningState spinningState;
    private final FSM fsm;

    private double requestedPower;

    public Team3OuttakeSubsystem(Team3MechanismHardware hardware) {
        if (hardware == null) {
            throw new IllegalArgumentException("Outtake subsystem needs mechanism hardware.");
        }
        this.hardware = hardware;
        idleState = new Team3OuttakeIdleState(this);
        spinningState = new Team3OuttakeSpinningState(this);
        fsm = new FSM(idleState);
        fsm.addTransition(new Transition(idleState, spinningState,
                () -> requestedPower > POWER_DEADBAND));
        fsm.addTransition(new Transition(spinningState, idleState,
                () -> requestedPower <= POWER_DEADBAND));
    }

    @Override
    public void initialize() {
        fsm.initialize();
    }

    @Override
    public void update() {
        fsm.update();
    }

    @Override
    public void stop() {
        requestedPower = 0.0;
        hardware.setOuttakePower(0.0);
    }

    @Override
    public String getName() {
        return "Outtake";
    }

    /**
     * Requests the outtake power from 0.0 (stopped) to 1.0 (full power).
     * Values outside that range are clamped, and NaN is treated as stopped.
     */
    public void setPower(double power) {
        if (Double.isNaN(power)) {
            requestedPower = 0.0;
        } else {
            requestedPower = Math.max(0.0, Math.min(1.0, power));
        }
    }

    public String getCurrentStateName() {
        String name = fsm.getCurrentStateName();
        return name == null ? idleState.getName() : name;
    }

    public boolean isAvailable() {
        return hardware.isOuttakeAvailable();
    }

    /** Returns the power currently commanded to the outtake motor (0.0 to 1.0). */
    public double getPower() {
        return requestedPower > POWER_DEADBAND ? requestedPower : 0.0;
    }

    /** Returns the raw requested power (0.0 to 1.0) before deadband. */
    public double getRequestedPower() {
        return requestedPower;
    }

    void runOuttake() {
        hardware.setOuttakePower(requestedPower);
    }

    void stopOutput() {
        hardware.setOuttakePower(0.0);
    }
}

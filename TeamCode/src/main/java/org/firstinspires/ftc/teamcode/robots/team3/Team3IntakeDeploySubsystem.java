package org.firstinspires.ftc.teamcode.robots.team3;

import org.firstinspires.ftc.teamcode.core.fsm.FSM;
import org.firstinspires.ftc.teamcode.core.fsm.Transition;
import org.firstinspires.ftc.teamcode.core.robot.Subsystem;

/** Controls Team 3's intake-deploy servo with a stowed/deployed finite-state machine. */
public class Team3IntakeDeploySubsystem implements Subsystem {
    /** TODO: tune on the real robot. Servo position (0.0 to 1.0) when the intake is stowed. */
    public static final double STOWED_POSITION = 0.0;
    /** TODO: tune on the real robot. Servo position (0.0 to 1.0) when the intake is deployed. */
    public static final double DEPLOYED_POSITION = 0.5;

    private final Team3MechanismHardware hardware;
    private final Team3IntakeStowedState stowedState;
    private final Team3IntakeDeployedState deployedState;
    private final FSM fsm;

    private boolean deployRequested;

    public Team3IntakeDeploySubsystem(Team3MechanismHardware hardware) {
        if (hardware == null) {
            throw new IllegalArgumentException("Intake deploy subsystem needs mechanism hardware.");
        }
        this.hardware = hardware;
        stowedState = new Team3IntakeStowedState(this);
        deployedState = new Team3IntakeDeployedState(this);
        fsm = new FSM(stowedState);
        fsm.addTransition(new Transition(stowedState, deployedState, () -> deployRequested));
        fsm.addTransition(new Transition(deployedState, stowedState, () -> !deployRequested));
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
        // The servo holds its last position when the robot stops.
    }

    @Override
    public String getName() {
        return "Intake Deploy";
    }

    /** Switches the requested position between stowed and deployed. */
    public void toggleDeploy() {
        deployRequested = !deployRequested;
    }

    /** Requests the deployed position. */
    public void deploy() {
        deployRequested = true;
    }

    /** Requests the stowed position. */
    public void stow() {
        deployRequested = false;
    }

    public String getCurrentStateName() {
        String name = fsm.getCurrentStateName();
        return name == null ? stowedState.getName() : name;
    }

    public boolean isAvailable() {
        return hardware.isIntakeDeployAvailable();
    }

    void moveToStowed() {
        hardware.setIntakeDeployPosition(STOWED_POSITION);
    }

    void moveToDeployed() {
        hardware.setIntakeDeployPosition(DEPLOYED_POSITION);
    }
}

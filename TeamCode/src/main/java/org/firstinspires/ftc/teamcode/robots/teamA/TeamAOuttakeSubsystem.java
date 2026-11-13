package org.firstinspires.ftc.teamcode.robots.teamA;

import org.firstinspires.ftc.teamcode.core.fsm.FSM;
import org.firstinspires.ftc.teamcode.core.fsm.Transition;
import org.firstinspires.ftc.teamcode.core.robot.Subsystem;

/** Controls Team A's pollen and nectar outtake motors through independent velocity FSMs. */
final class TeamAOuttakeSubsystem implements Subsystem {
    // 6,000 RPM * 28 encoder ticks per revolution / 60 seconds = 2,800 ticks per second.
    // Each motor has separate constants so its real-world launch and idle speeds are easy to tune.
    static final double POLLEN_IDLE_VELOCITY_TICKS_PER_SECOND = 560.0;
    static final double POLLEN_LAUNCH_VELOCITY_TICKS_PER_SECOND = 1400.0;
    static final double NECTAR_IDLE_VELOCITY_TICKS_PER_SECOND = 560.0;
    static final double NECTAR_LAUNCH_VELOCITY_TICKS_PER_SECOND = 1680.0;

    enum OuttakeMotor {
        POLLEN,
        NECTAR
    }

    private enum RequestedMode {
        OFF,
        IDLE,
        LAUNCH
    }

    private final TeamAMechanismHardware hardware;

    private final TeamAOuttakeVelocityState pollenOffState;
    private final TeamAOuttakeVelocityState pollenIdleState;
    private final TeamAOuttakeVelocityState pollenLaunchState;
    private final FSM pollenFsm;

    private final TeamAOuttakeVelocityState nectarOffState;
    private final TeamAOuttakeVelocityState nectarIdleState;
    private final TeamAOuttakeVelocityState nectarLaunchState;
    private final FSM nectarFsm;

    private RequestedMode requestedPollenMode = RequestedMode.OFF;
    private RequestedMode requestedNectarMode = RequestedMode.OFF;

    TeamAOuttakeSubsystem(TeamAMechanismHardware hardware) {
        if (hardware == null) {
            throw new IllegalArgumentException("Team A outtake subsystem needs mechanism hardware.");
        }

        this.hardware = hardware;

        pollenOffState = createState(OuttakeMotor.POLLEN, "PollenOff", 0.0);
        pollenIdleState = createState(OuttakeMotor.POLLEN, "PollenIdle",
                POLLEN_IDLE_VELOCITY_TICKS_PER_SECOND);
        pollenLaunchState = createState(OuttakeMotor.POLLEN, "PollenLaunch",
                POLLEN_LAUNCH_VELOCITY_TICKS_PER_SECOND);
        pollenFsm = new FSM(pollenOffState);
        addPollenTransitions();

        nectarOffState = createState(OuttakeMotor.NECTAR, "NectarOff", 0.0);
        nectarIdleState = createState(OuttakeMotor.NECTAR, "NectarIdle",
                NECTAR_IDLE_VELOCITY_TICKS_PER_SECOND);
        nectarLaunchState = createState(OuttakeMotor.NECTAR, "NectarLaunch",
                NECTAR_LAUNCH_VELOCITY_TICKS_PER_SECOND);
        nectarFsm = new FSM(nectarOffState);
        addNectarTransitions();
    }

    @Override
    public void initialize() {
        pollenFsm.initialize();
        nectarFsm.initialize();
    }

    @Override
    public void update() {
        pollenFsm.update();
        nectarFsm.update();
    }

    @Override
    public void stop() {
        requestedPollenMode = RequestedMode.OFF;
        requestedNectarMode = RequestedMode.OFF;
        hardware.stopOuttakes();
    }

    @Override
    public String getName() {
        return "Team A Outtakes";
    }

    /** Requests the next pollen state in the Off -> Idle -> Launch -> Off cycle. */
    void cyclePollenMode() {
        requestedPollenMode = nextMode(requestedPollenMode);
    }

    /** Requests the next nectar state in the Off -> Idle -> Launch -> Off cycle. */
    void cycleNectarMode() {
        requestedNectarMode = nextMode(requestedNectarMode);
    }

    String getPollenStateName() {
        String name = pollenFsm.getCurrentStateName();
        return name == null ? pollenOffState.getName() : name;
    }

    String getNectarStateName() {
        String name = nectarFsm.getCurrentStateName();
        return name == null ? nectarOffState.getName() : name;
    }

    boolean isAnyLaunching() {
        return requestedPollenMode == RequestedMode.LAUNCH
                || requestedNectarMode == RequestedMode.LAUNCH
                || pollenFsm.getCurrentState() == pollenLaunchState
                || nectarFsm.getCurrentState() == nectarLaunchState;
    }

    boolean isPollenLaunching() {
        return requestedPollenMode == RequestedMode.LAUNCH
                && pollenFsm.getCurrentState() == pollenLaunchState;
    }

    boolean isNectarLaunching() {
        return requestedNectarMode == RequestedMode.LAUNCH
                && nectarFsm.getCurrentState() == nectarLaunchState;
    }

    void applyVelocity(OuttakeMotor motor, double velocityTicksPerSecond) {
        if (motor == OuttakeMotor.POLLEN) {
            hardware.setPollenVelocity(velocityTicksPerSecond);
        } else {
            hardware.setNectarVelocity(velocityTicksPerSecond);
        }
    }

    private TeamAOuttakeVelocityState createState(
            OuttakeMotor motor, String name, double velocityTicksPerSecond) {
        return new TeamAOuttakeVelocityState(this, motor, name, velocityTicksPerSecond);
    }

    private RequestedMode nextMode(RequestedMode currentMode) {
        switch (currentMode) {
            case OFF:
                return RequestedMode.IDLE;
            case IDLE:
                return RequestedMode.LAUNCH;
            case LAUNCH:
            default:
                return RequestedMode.OFF;
        }
    }

    private void addPollenTransitions() {
        pollenFsm.addTransition(new Transition(pollenOffState, pollenIdleState,
                () -> requestedPollenMode == RequestedMode.IDLE));
        pollenFsm.addTransition(new Transition(pollenOffState, pollenLaunchState,
                () -> requestedPollenMode == RequestedMode.LAUNCH));
        pollenFsm.addTransition(new Transition(pollenIdleState, pollenOffState,
                () -> requestedPollenMode == RequestedMode.OFF));
        pollenFsm.addTransition(new Transition(pollenIdleState, pollenLaunchState,
                () -> requestedPollenMode == RequestedMode.LAUNCH));
        pollenFsm.addTransition(new Transition(pollenLaunchState, pollenOffState,
                () -> requestedPollenMode == RequestedMode.OFF));
        pollenFsm.addTransition(new Transition(pollenLaunchState, pollenIdleState,
                () -> requestedPollenMode == RequestedMode.IDLE));
    }

    private void addNectarTransitions() {
        nectarFsm.addTransition(new Transition(nectarOffState, nectarIdleState,
                () -> requestedNectarMode == RequestedMode.IDLE));
        nectarFsm.addTransition(new Transition(nectarOffState, nectarLaunchState,
                () -> requestedNectarMode == RequestedMode.LAUNCH));
        nectarFsm.addTransition(new Transition(nectarIdleState, nectarOffState,
                () -> requestedNectarMode == RequestedMode.OFF));
        nectarFsm.addTransition(new Transition(nectarIdleState, nectarLaunchState,
                () -> requestedNectarMode == RequestedMode.LAUNCH));
        nectarFsm.addTransition(new Transition(nectarLaunchState, nectarOffState,
                () -> requestedNectarMode == RequestedMode.OFF));
        nectarFsm.addTransition(new Transition(nectarLaunchState, nectarIdleState,
                () -> requestedNectarMode == RequestedMode.IDLE));
    }
}

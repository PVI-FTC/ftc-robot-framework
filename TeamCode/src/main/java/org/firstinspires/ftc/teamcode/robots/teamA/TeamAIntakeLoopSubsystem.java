package org.firstinspires.ftc.teamcode.robots.teamA;

import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.common.subsystems.intake.IntakeSubsystem;
import org.firstinspires.ftc.teamcode.core.fsm.FSM;
import org.firstinspires.ftc.teamcode.core.fsm.Transition;
import org.firstinspires.ftc.teamcode.core.robot.Subsystem;

/** Coordinates Team A's non-blocking intake, color counting, and Pollen sorting loop. */
final class TeamAIntakeLoopSubsystem implements Subsystem {
    // 435 RPM * 384.5 output ticks per revolution / 60 seconds = 2,787.625 ticks/second.
    static final double MIDTAKE_MAX_VELOCITY_TICKS_PER_SECOND = 2787.625;
    static final double POLLEN_SORT_TIME_SECONDS = 0.50;
    static final double NECTAR_SERVO_INTAKE_POWER = 1.0;
    static final int MAX_GAME_PIECES = 4;

    private final IntakeSubsystem intakeSubsystem;
    private final TeamAMechanismHardware hardware;
    private final TeamAGamePieceDetector intakeDetector = new TeamAGamePieceDetector();
    private final TeamAGamePieceDetector pollenDetector = new TeamAGamePieceDetector();
    private final ElapsedTime sortTimer = new ElapsedTime();

    private final TeamAIntakeLoopState stoppedState;
    private final TeamAIntakeLoopState intakingState;
    private final TeamAIntakeLoopState sortingState;
    private final FSM fsm;

    private TeamAMechanismHardware.ColorReading latestIntakeReading;
    private TeamAMechanismHardware.ColorReading latestPollenReading;
    private boolean activeRequested;
    private boolean stopRequested;
    private int pendingPollenSorts;
    private int gamePieceCount;
    private int pollenCount;
    private int nectarCount;

    TeamAIntakeLoopSubsystem(IntakeSubsystem intakeSubsystem, TeamAMechanismHardware hardware) {
        if (intakeSubsystem == null || hardware == null) {
            throw new IllegalArgumentException(
                    "Team A intake loop needs intake and mechanism hardware.");
        }

        this.intakeSubsystem = intakeSubsystem;
        this.hardware = hardware;
        stoppedState = new TeamAIntakeLoopState(
                this, TeamAIntakeLoopState.Mode.STOPPED, "IntakeLoopStopped");
        intakingState = new TeamAIntakeLoopState(
                this, TeamAIntakeLoopState.Mode.INTAKING, "IntakeLoopRunning");
        sortingState = new TeamAIntakeLoopState(
                this, TeamAIntakeLoopState.Mode.SORTING_POLLEN, "SortingPollen");
        fsm = new FSM(stoppedState);
        addTransitions();
    }

    @Override
    public void initialize() {
        fsm.initialize();
    }

    @Override
    public void update() {
        sampleSensors();
        fsm.update();
    }

    @Override
    public void stop() {
        activeRequested = false;
        stopRequested = true;
        pendingPollenSorts = 0;
        stopLoopOutputs();
    }

    @Override
    public String getName() {
        return "Team A Intake Loop";
    }

    void requestStart() {
        activeRequested = true;
        stopRequested = false;
    }

    void requestStopAfterCurrentTask() {
        activeRequested = false;
        stopRequested = true;
    }

    boolean isActive() {
        return activeRequested || (fsm.getCurrentState() != null
                && fsm.getCurrentState() != stoppedState);
    }

    boolean isFull() {
        return gamePieceCount >= MAX_GAME_PIECES;
    }

    String getStateName() {
        String name = fsm.getCurrentStateName();
        return name == null ? stoppedState.getName() : name;
    }

    int getGamePieceCount() {
        return gamePieceCount;
    }

    int getPollenCount() {
        return pollenCount;
    }

    int getNectarCount() {
        return nectarCount;
    }

    void resetGamePieceCounts() {
        gamePieceCount = 0;
        pollenCount = 0;
        nectarCount = 0;
        pendingPollenSorts = 0;
    }

    String getIntakeDetectionName() {
        return intakeDetector.getCurrentReadingName();
    }

    String getPollenDetectionName() {
        return pollenDetector.getCurrentReadingName();
    }

    double getIntakeSensorDistanceCm() {
        return latestIntakeReading == null ? Double.NaN : latestIntakeReading.getDistanceCm();
    }

    float getIntakeSensorHueDegrees() {
        return latestIntakeReading == null ? Float.NaN : latestIntakeReading.getHueDegrees();
    }

    float getIntakeSensorSaturation() {
        return latestIntakeReading == null ? Float.NaN : latestIntakeReading.getSaturation();
    }

    float getIntakeSensorValue() {
        return latestIntakeReading == null ? Float.NaN : latestIntakeReading.getValue();
    }

    double getPollenSensorDistanceCm() {
        return latestPollenReading == null ? Double.NaN : latestPollenReading.getDistanceCm();
    }

    float getPollenSensorHueDegrees() {
        return latestPollenReading == null ? Float.NaN : latestPollenReading.getHueDegrees();
    }

    float getPollenSensorSaturation() {
        return latestPollenReading == null ? Float.NaN : latestPollenReading.getSaturation();
    }

    float getPollenSensorValue() {
        return latestPollenReading == null ? Float.NaN : latestPollenReading.getValue();
    }

    void enterState(TeamAIntakeLoopState.Mode mode) {
        if (mode == TeamAIntakeLoopState.Mode.SORTING_POLLEN) {
            pendingPollenSorts--;
            sortTimer.reset();
        }
        applyOutputs(mode);
    }

    void updateState(TeamAIntakeLoopState.Mode mode) {
        // A stopped loop must not override the legacy manual intake API in other Team A OpModes.
        if (mode != TeamAIntakeLoopState.Mode.STOPPED) {
            applyOutputs(mode);
        }
    }

    private void sampleSensors() {
        latestIntakeReading = hardware.readIntakeSensor();
        latestPollenReading = hardware.readPollenSensor();
        TeamAGamePieceDetector.GamePiece intakeEvent = intakeDetector.update(latestIntakeReading);
        TeamAGamePieceDetector.GamePiece pollenEvent = pollenDetector.update(latestPollenReading);

        if (!isActive()) {
            return;
        }

        if (intakeEvent == TeamAGamePieceDetector.GamePiece.POLLEN) {
            recordPollen();
        } else if (intakeEvent == TeamAGamePieceDetector.GamePiece.NECTAR) {
            recordNectar();
        }

        if (pollenEvent == TeamAGamePieceDetector.GamePiece.POLLEN || isFull()) {
            requestStopAfterCurrentTask();
        }
    }

    private void recordPollen() {
        if (isFull()) {
            return;
        }
        pollenCount++;
        gamePieceCount++;
        pendingPollenSorts++;
    }

    private void recordNectar() {
        if (isFull()) {
            return;
        }
        nectarCount++;
        gamePieceCount++;
    }

    private void applyOutputs(TeamAIntakeLoopState.Mode mode) {
        switch (mode) {
            case INTAKING:
                intakeSubsystem.startIntake();
                hardware.setMidtakeVelocity(0.0);
                hardware.setNectarServoPower(NECTAR_SERVO_INTAKE_POWER);
                break;
            case SORTING_POLLEN:
                intakeSubsystem.startIntake();
                hardware.setMidtakeVelocity(MIDTAKE_MAX_VELOCITY_TICKS_PER_SECOND);
                hardware.setNectarServoPower(NECTAR_SERVO_INTAKE_POWER);
                break;
            case STOPPED:
            default:
                stopLoopOutputs();
                break;
        }
    }

    private void stopLoopOutputs() {
        intakeSubsystem.stopIntake();
        hardware.setMidtakeVelocity(0.0);
        hardware.setNectarServoPower(0.0);
    }

    private boolean sortTimeFinished() {
        return sortTimer.seconds() >= POLLEN_SORT_TIME_SECONDS;
    }

    private void addTransitions() {
        fsm.addTransition(new Transition(stoppedState, intakingState,
                () -> activeRequested && !stopRequested));
        fsm.addTransition(new Transition(intakingState, sortingState,
                () -> pendingPollenSorts > 0));
        fsm.addTransition(new Transition(intakingState, stoppedState,
                () -> stopRequested));
        fsm.addTransition(new Transition(sortingState, sortingState,
                () -> sortTimeFinished() && pendingPollenSorts > 0));
        fsm.addTransition(new Transition(sortingState, stoppedState,
                () -> sortTimeFinished() && pendingPollenSorts == 0 && stopRequested));
        fsm.addTransition(new Transition(sortingState, intakingState,
                () -> sortTimeFinished() && pendingPollenSorts == 0 && !stopRequested));
    }
}

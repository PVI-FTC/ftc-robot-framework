package org.firstinspires.ftc.teamcode.common.subsystems.drive;

import org.firstinspires.ftc.teamcode.common.hardware.DriveHardware;
import org.firstinspires.ftc.teamcode.common.localization.PoseEstimate;
import org.firstinspires.ftc.teamcode.core.fsm.FSM;
import org.firstinspires.ftc.teamcode.core.fsm.Transition;
import org.firstinspires.ftc.teamcode.core.robot.Subsystem;

/**
 * Controls drivetrain modes through a small finite-state machine.
 *
 * <p>Call request methods through a Robot public API. The subsystem selects exactly one active
 * mode during its next {@link #update()} call. A controller owns the implementation details of
 * manual output, optional path following, and pose estimation.</p>
 */
public class DriveSubsystem implements Subsystem {
    private enum RequestedMode {
        DISABLED,
        MANUAL,
        HEADING_HOLD,
        PATH_FOLLOWING
    }

    private final DriveController driveController;
    private final DisabledDriveState disabledState;
    private final ManualDriveState manualDriveState;
    private final HeadingHoldState headingHoldState;
    private final PathFollowingDriveState pathFollowingDriveState;
    private final FSM fsm;

    private RequestedMode requestedMode = RequestedMode.DISABLED;
    private double requestedForward;
    private double requestedStrafe;
    private double requestedRotate;

    /** Creates the existing simple mecanum drivetrain composition. */
    public DriveSubsystem(DriveHardware driveHardware) {
        this(new MecanumDriveController(driveHardware));
    }

    /**
     * Creates a drivetrain subsystem with a library-neutral controller.
     *
     * <p>A future team-specific controller can provide localization and path following without
     * exposing its vendor types to this subsystem.</p>
     */
    public DriveSubsystem(DriveController driveController) {
        if (driveController == null) {
            throw new IllegalArgumentException("Drive subsystem needs a drive controller.");
        }

        this.driveController = driveController;
        disabledState = new DisabledDriveState(this);
        manualDriveState = new ManualDriveState(this);
        headingHoldState = new HeadingHoldState(this);
        pathFollowingDriveState = new PathFollowingDriveState(this);
        fsm = new FSM(disabledState);

        addTransitions();
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
        requestedMode = RequestedMode.DISABLED;
        requestedForward = 0.0;
        requestedStrafe = 0.0;
        requestedRotate = 0.0;
        driveController.stop();
    }

    @Override
    public String getName() {
        return "Drive";
    }

    /** Stores translation and rotation values for the next manual-drive update. */
    public void drive(double forward, double strafe, double rotate) {
        requestedForward = safeInput(forward);
        requestedStrafe = safeInput(strafe);
        requestedRotate = safeInput(rotate);
    }

    /** Requests drivetrain behavior that applies the stored manual-drive values. */
    public void enableRequestedDrive() {
        requestedMode = RequestedMode.MANUAL;
    }

    /** Requests normal driver-controlled mecanum behavior. */
    public void enableManualDrive() {
        enableRequestedDrive();
    }

    /** Requests the safe disabled behavior. */
    public void disableDrive() {
        requestedMode = RequestedMode.DISABLED;
    }

    /** Requests the heading-hold placeholder behavior. */
    public void enableHeadingHold() {
        requestedMode = RequestedMode.HEADING_HOLD;
    }

    /** Requests the controller to update an already-requested path on each loop. */
    public void enablePathFollowing() {
        requestedMode = RequestedMode.PATH_FOLLOWING;
    }

    /** Cancels path following by returning to the safe disabled behavior. */
    public void cancelPathFollowing() {
        disableDrive();
    }

    /** Returns the active state name, or disabled before initialization. */
    public String getCurrentStateName() {
        String currentStateName = fsm.getCurrentStateName();
        return currentStateName == null ? disabledState.getName() : currentStateName;
    }

    public double getRequestedForward() {
        return requestedForward;
    }

    public double getRequestedStrafe() {
        return requestedStrafe;
    }

    public double getRequestedRotate() {
        return requestedRotate;
    }

    /** Returns a neutral pose estimate without exposing a localization library type. */
    public PoseEstimate getPoseEstimate() {
        return driveController.getPoseEstimate();
    }

    /** Returns whether the active pathing controller still reports a path in progress. */
    public boolean isPathFollowingActive() {
        return requestedMode == RequestedMode.PATH_FOLLOWING
                && driveController.isPathFollowingActive();
    }

    void applyRequestedMecanumDrive() {
        driveController.updateManualDrive(requestedForward, requestedStrafe, requestedRotate);
    }

    void updatePathFollowing() {
        driveController.updatePathFollowing();
    }

    void stopDrive() {
        driveController.stop();
    }

    private void addTransitions() {
        fsm.addTransition(new Transition(disabledState, manualDriveState,
                () -> requestedMode == RequestedMode.MANUAL));
        fsm.addTransition(new Transition(disabledState, headingHoldState,
                () -> requestedMode == RequestedMode.HEADING_HOLD));
        fsm.addTransition(new Transition(disabledState, pathFollowingDriveState,
                () -> requestedMode == RequestedMode.PATH_FOLLOWING));
        fsm.addTransition(new Transition(manualDriveState, disabledState,
                () -> requestedMode == RequestedMode.DISABLED));
        fsm.addTransition(new Transition(manualDriveState, headingHoldState,
                () -> requestedMode == RequestedMode.HEADING_HOLD));
        fsm.addTransition(new Transition(manualDriveState, pathFollowingDriveState,
                () -> requestedMode == RequestedMode.PATH_FOLLOWING));
        fsm.addTransition(new Transition(headingHoldState, disabledState,
                () -> requestedMode == RequestedMode.DISABLED));
        fsm.addTransition(new Transition(headingHoldState, manualDriveState,
                () -> requestedMode == RequestedMode.MANUAL));
        fsm.addTransition(new Transition(headingHoldState, pathFollowingDriveState,
                () -> requestedMode == RequestedMode.PATH_FOLLOWING));
        fsm.addTransition(new Transition(pathFollowingDriveState, disabledState,
                () -> requestedMode == RequestedMode.DISABLED));
        fsm.addTransition(new Transition(pathFollowingDriveState, manualDriveState,
                () -> requestedMode == RequestedMode.MANUAL));
        fsm.addTransition(new Transition(pathFollowingDriveState, headingHoldState,
                () -> requestedMode == RequestedMode.HEADING_HOLD));
    }

    private double safeInput(double input) {
        if (Double.isNaN(input) || Double.isInfinite(input)) {
            return 0.0;
        }
        return input;
    }
}
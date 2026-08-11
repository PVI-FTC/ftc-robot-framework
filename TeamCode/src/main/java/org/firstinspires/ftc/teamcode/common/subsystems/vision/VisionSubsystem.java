package org.firstinspires.ftc.teamcode.common.subsystems.vision;

import org.firstinspires.ftc.teamcode.common.hardware.VisionHardware;
import org.firstinspires.ftc.teamcode.common.vision.AprilTagObservation;
import org.firstinspires.ftc.teamcode.core.fsm.FSM;
import org.firstinspires.ftc.teamcode.core.fsm.Transition;
import org.firstinspires.ftc.teamcode.core.robot.Subsystem;

import java.util.Collections;
import java.util.List;

/**
 * Safe vision lifecycle and target-observation state machine.
 *
 * <p>This subsystem does not create cameras or invent target data. Its active states update the
 * hardware wrapper once per robot loop, then use its neutral observations to choose FSM behavior.
 * A positive observation enters TargetAcquired for one update cycle before Tracking. A loss enters
 * LostTarget for one update cycle before returning to Searching.</p>
 */
public class VisionSubsystem implements Subsystem {
    private final VisionHardware visionHardware;
    private final VisionDisabledState disabledState;
    private final SearchingState searchingState;
    private final TargetAcquiredState targetAcquiredState;
    private final TrackingState trackingState;
    private final LostTargetState lostTargetState;
    private final FSM fsm;

    private boolean visionRequested;
    private boolean targetDetected;
    private List<AprilTagObservation> latestObservations = Collections.emptyList();

    /** Creates a vision subsystem that uses the supplied lifecycle-only hardware wrapper. */
    public VisionSubsystem(VisionHardware visionHardware) {
        if (visionHardware == null) {
            throw new IllegalArgumentException("Vision subsystem needs vision hardware.");
        }

        this.visionHardware = visionHardware;
        disabledState = new VisionDisabledState(this);
        searchingState = new SearchingState(this);
        targetAcquiredState = new TargetAcquiredState(this);
        trackingState = new TrackingState(this);
        lostTargetState = new LostTargetState(this);
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
        visionRequested = false;
        targetDetected = false;
        visionHardware.stop();
        latestObservations = Collections.emptyList();
    }

    @Override
    public String getName() {
        return "Vision";
    }

    /** Requests searching only when a vision device is available. */
    public void enableVision() {
        visionRequested = visionHardware.isAvailable();
        if (!visionRequested) {
            targetDetected = false;
        }
    }

    /** Requests the safe disabled vision behavior. */
    public void disableVision() {
        visionRequested = false;
        targetDetected = false;
    }

    /**
     * Records an observation from a future vision processor.
     *
     * <p>Unavailable vision always records no detection so this skeleton cannot manufacture a
     * target result.</p>
     *
     * @param detected whether a future processor observed the target this loop
     */
    public void reportTargetDetected(boolean detected) {
        targetDetected = visionHardware.isAvailable() && detected;
    }

    /** Returns the active vision state name for telemetry. */
    public String getCurrentStateName() {
        String currentStateName = fsm.getCurrentStateName();
        return currentStateName == null ? disabledState.getName() : currentStateName;
    }

    /** Returns whether a vision device is available. */
    public boolean isAvailable() {
        return visionHardware.isAvailable();
    }

    /** Returns the latest neutral observations collected during an active vision update. */
    public List<AprilTagObservation> getLatestObservations() {
        return latestObservations;
    }

    boolean isVisionRequested() {
        return visionRequested;
    }

    boolean isTargetDetected() {
        return targetDetected;
    }

    void updateVisionHardware() {
        visionHardware.update();
        latestObservations = visionHardware.getObservations();
        targetDetected = !latestObservations.isEmpty();
    }

    void stopVisionHardware() {
        visionHardware.stop();
        latestObservations = Collections.emptyList();
        targetDetected = false;
    }

    private void addTransitions() {
        fsm.addTransition(new Transition(disabledState, searchingState, this::isVisionRequested));
        fsm.addTransition(new Transition(searchingState, disabledState,
                () -> !isVisionRequested()));
        fsm.addTransition(new Transition(searchingState, targetAcquiredState,
                this::isTargetDetected));
        fsm.addTransition(new Transition(targetAcquiredState, disabledState,
                () -> !isVisionRequested()));
        fsm.addTransition(new Transition(targetAcquiredState, trackingState,
                this::isVisionRequested));
        fsm.addTransition(new Transition(trackingState, disabledState,
                () -> !isVisionRequested()));
        fsm.addTransition(new Transition(trackingState, lostTargetState,
                () -> !isTargetDetected()));
        fsm.addTransition(new Transition(lostTargetState, disabledState,
                () -> !isVisionRequested()));
        fsm.addTransition(new Transition(lostTargetState, searchingState,
                this::isVisionRequested));
    }
}

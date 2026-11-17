package org.firstinspires.ftc.teamcode.robots.team4.conveyor;

import org.firstinspires.ftc.teamcode.core.fsm.FSM;
import org.firstinspires.ftc.teamcode.core.fsm.State;
import org.firstinspires.ftc.teamcode.core.fsm.Transition;
import org.firstinspires.ftc.teamcode.core.robot.Subsystem;

/** Conveyor-4: independent belt direction and joystick-selected lift endpoints. */
public class Conveyor4Subsystem implements Subsystem {
    public static final double BELT_POWER = 0.30; // Initial trial power, not a measured RPM.
    public static final double STICK_DEADBAND = 0.10;
    private enum BeltMode { STOPPED, FORWARD, REVERSE }
    private enum LiftMode { WAITING_FOR_STICK, EXTEND_TARGET, RETRACT_TARGET }

    private final Conveyor4Hardware hardware;
    private final FSM beltFsm;
    private BeltMode beltRequest = BeltMode.STOPPED;
    private LiftMode liftMode = LiftMode.WAITING_FOR_STICK;
    private double liftTarget;

    public Conveyor4Subsystem(Conveyor4Hardware hardware) {
        if (hardware == null) throw new IllegalArgumentException("Conveyor-4 needs hardware.");
        this.hardware = hardware;
        State stopped = new ConveyorState(BeltMode.STOPPED);
        State forward = new ConveyorState(BeltMode.FORWARD);
        State reverse = new ConveyorState(BeltMode.REVERSE);
        beltFsm = new FSM(stopped);
        beltFsm.addTransition(new Transition(stopped, forward, () -> beltRequest == BeltMode.FORWARD));
        beltFsm.addTransition(new Transition(stopped, reverse, () -> beltRequest == BeltMode.REVERSE));
        beltFsm.addTransition(new Transition(forward, stopped, () -> beltRequest == BeltMode.STOPPED));
        beltFsm.addTransition(new Transition(forward, reverse, () -> beltRequest == BeltMode.REVERSE));
        beltFsm.addTransition(new Transition(reverse, stopped, () -> beltRequest == BeltMode.STOPPED));
        beltFsm.addTransition(new Transition(reverse, forward, () -> beltRequest == BeltMode.FORWARD));
    }

    public void forward() { beltRequest = BeltMode.FORWARD; }
    public void reverse() { beltRequest = BeltMode.REVERSE; }
    public void stopBelt() { beltRequest = BeltMode.STOPPED; }

    /** Positive selects full extension, negative full retraction; zero retains the endpoint. */
    public void requestLift(double input) {
        if (!hardware.isLiftAvailable() || !Double.isFinite(input)) return;
        if (input > STICK_DEADBAND) {
            liftTarget = 1.0;
            liftMode = LiftMode.EXTEND_TARGET;
        } else if (input < -STICK_DEADBAND) {
            liftTarget = 0.0;
            liftMode = LiftMode.RETRACT_TARGET;
        }
    }

    public String getBeltStateName() { return beltFsm.getCurrentStateName(); }
    public String getLiftStateName() { return liftMode.name(); }
    public double getLiftTarget() { return liftTarget; }
    public boolean isMotorAvailable() { return hardware.isMotorAvailable(); }
    public boolean isLiftAvailable() { return hardware.isLiftAvailable(); }

    @Override
    public String getName() { return "conveyor-4"; }

    @Override
    public void initialize() {
        hardware.stop();
        liftMode = LiftMode.WAITING_FOR_STICK;
        beltFsm.initialize();
    }

    @Override
    public void update() {
        beltFsm.update();
    }

    @Override
    public void stop() {
        beltRequest = BeltMode.STOPPED;
        liftMode = LiftMode.WAITING_FOR_STICK;
        hardware.stop();
    }

    /** Each belt state also maintains the independent lift command once per loop. */
    private class ConveyorState implements State {
        private final BeltMode mode;
        ConveyorState(BeltMode mode) { this.mode = mode; }
        @Override public void enter() { }
        @Override public void exit() { }
        @Override public String getName() { return mode.name(); }

        @Override
        public void update() {
            double power = mode == BeltMode.FORWARD ? BELT_POWER
                    : mode == BeltMode.REVERSE ? -BELT_POWER : 0.0;
            hardware.setBeltPower(power);
            if (liftMode == LiftMode.WAITING_FOR_STICK) {
                hardware.disableLiftPwm();
                return;
            }
            hardware.setLiftTarget(liftTarget);
        }
    }
}

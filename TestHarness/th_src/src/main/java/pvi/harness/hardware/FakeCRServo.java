package pvi.harness.hardware;

import com.qualcomm.robotcore.hardware.CRServo;
import com.qualcomm.robotcore.hardware.ServoController;

/**
 * A continuous-rotation servo (the SDK's {@code CRServo}): like a motor without an encoder, it
 * takes a power from -1 to 1. It records the power robot code sets, so a test can check it.
 *
 * <p>{@link #getPower()} returns what the code set, as the SDK does. {@link #getAppliedPower()}
 * is the power after the direction, which is what really turns the servo.</p>
 *
 * <p>Add one with {@code hardwareMap.addCRServo("intakeRoller")}.</p>
 */
public class FakeCRServo extends FakeDevice implements CRServo {
    private Direction direction = Direction.FORWARD;
    private double power;

    public FakeCRServo(String name) {
        super(name);
    }

    /** The power that really turns the servo: negative when the direction is REVERSE. */
    public double getAppliedPower() {
        return direction == Direction.REVERSE ? -power : power;
    }

    /** No hub exists in the harness, so there is no controller object. */
    @Override public ServoController getController() { return null; }
    @Override public int getPortNumber() { return 0; }
    @Override public void setDirection(Direction direction) { this.direction = direction; }
    @Override public Direction getDirection() { return direction; }

    /** Records the power, clamped to -1..1 as the SDK does. */
    @Override
    public void setPower(double power) {
        this.power = Math.max(-1.0, Math.min(1.0, power));
    }

    @Override public double getPower() { return power; }
}

package pvi.harness.hardware;

import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.DigitalChannelController;
import com.qualcomm.robotcore.hardware.TouchSensor;

/**
 * A touch sensor or limit switch, pressed and released by the {@link SensorScript}:
 * {@code script.press("armLimit")}, {@code script.release("armLimit")}.
 *
 * <p>Robot code can read it either way a REV touch sensor is read: as a {@code TouchSensor}
 * ({@code isPressed()}), or as a {@code DigitalChannel} in INPUT mode, where
 * {@code getState()} is <b>true when not pressed</b> (the REV sensor pulls the line low when
 * pressed). {@link #setPressed} presses it directly, for quick tests.</p>
 *
 * <p>Add one with {@code hardwareMap.addTouchSensor("armLimit")}.</p>
 */
public class FakeTouchSensor extends FakeDevice implements TouchSensor, DigitalChannel, SimulatedSensor {
    private boolean pressed;
    private Mode mode = Mode.INPUT;
    /** What robot code wrote in OUTPUT mode (a REV touch sensor ignores it). */
    private boolean outputState;

    public FakeTouchSensor(String name) {
        super(name);
    }

    /** Presses or releases it directly. */
    public FakeTouchSensor setPressed(boolean pressed) {
        this.pressed = pressed;
        return this;
    }

    @Override
    public void step(Step now) {
        Boolean value = now.script().valueAt(getName(), Boolean.class, now.sinceInitSeconds(), now.sinceStartSeconds());
        if (value != null) {
            pressed = value;
        }
    }

    // ---- TouchSensor ---------------------------------------------------------------------------

    /** 1 when pressed, 0 when not. */
    @Override public double getValue() { return pressed ? 1.0 : 0.0; }
    @Override public boolean isPressed() { return pressed; }

    // ---- DigitalChannel ------------------------------------------------------------------------

    @Override public Mode getMode() { return mode; }
    @Override public void setMode(Mode mode) { this.mode = mode; }

    /** The old controller-style mode setting, as the SDK still accepts it. */
    @Override
    public void setMode(DigitalChannelController.Mode mode) {
        this.mode = mode == DigitalChannelController.Mode.OUTPUT ? Mode.OUTPUT : Mode.INPUT;
    }

    /** In INPUT mode: true when NOT pressed, as a REV touch sensor reads. */
    @Override
    public boolean getState() {
        return mode == Mode.OUTPUT ? outputState : !pressed;
    }

    @Override public void setState(boolean state) { this.outputState = state; }
}

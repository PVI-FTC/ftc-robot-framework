package pvi.harness.runtime;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.robocol.TelemetryMessage;

import org.firstinspires.ftc.robotcore.internal.opmode.OpModeServices;

/**
 * Stands in for the Robot Controller's OpMode manager, which the SDK calls when an OpMode
 * runs {@code requestOpModeStop()}.
 */
class HarnessOpModeServices implements OpModeServices {
    /** Set to true when the OpMode asks to stop. */
    private boolean stopRequested;

    /** The runner checks this after each loop to end the run. */
    boolean isStopRequested() {
        return stopRequested;
    }

    /** Called by the SDK when the OpMode runs requestOpModeStop(). */
    @Override
    public void requestOpModeStop(OpMode opMode) {
        stopRequested = true;
    }

    /** The real app uses this to send telemetry over the network; not needed in the harness. */
    @Override
    public void refreshUserTelemetry(TelemetryMessage telemetry, double sInterval) {
        // Telemetry goes through FakeTelemetry instead.
    }
}

package pvi.harness.ds;

/**
 * One rumble command the OpMode sent to a gamepad.
 *
 * @param matchTimeSeconds seconds after PLAY when it was sent (0 if before PLAY)
 * @param totalDurationMs  length of the whole effect
 * @param pulses           number of separate buzzes (rumbleBlips(3) gives 3)
 */
public record RumbleRecord(double matchTimeSeconds, int totalDurationMs, int pulses) {
}

package pvi.harness.viewer;

import java.awt.event.KeyEvent;
import java.nio.file.Path;
import java.util.List;

import com.qualcomm.robotcore.hardware.Gamepad;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import pvi.harness.ds.Axis;
import pvi.harness.ds.Button;
import pvi.harness.runtime.RunRecording;
import pvi.harness.runtime.RunResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drive and record, step D2: driving a real TeleOp live from the keyboard.
 *
 * <p>No window is needed: these tests hold keys in {@link HeldKeys} directly and run the drive
 * unpaced (as fast as possible) instead of in real time.</p>
 */
class LiveDriveTest {

    /** Runs the layout once and returns the gamepad it produced. */
    private static Gamepad read(pvi.harness.ds.GamepadSource source) {
        Gamepad pad = new Gamepad();
        source.update(pad, 0.0, 0.0);
        return pad;
    }

    /**
     * WHAT: W, A, S, D move the driver's left stick; arrows move the right stick; up is negative.
     *
     * <p>WHY: This is the approved layout. A sign mistake would make W drive backwards.</p>
     */
    @Test
    void driverSticksFollowTheLayout() {
        HeldKeys keys = new HeldKeys();
        keys.press(KeyEvent.VK_W);
        keys.press(KeyEvent.VK_D);
        keys.press(KeyEvent.VK_LEFT);
        Gamepad pad = read(KeyboardGamepads.driver(keys));

        assertEquals(-1.0, Axis.LEFT_STICK_Y.read(pad), 1e-6, "W = stick up = negative");
        assertEquals(1.0, Axis.LEFT_STICK_X.read(pad), 1e-6, "D = strafe right");
        assertEquals(-1.0, Axis.RIGHT_STICK_X.read(pad), 1e-6, "Left arrow = turn left");
        assertEquals(0.0, Axis.RIGHT_STICK_Y.read(pad), 1e-6);
    }

    /**
     * WHAT: Opposite keys cancel out, Shift gives 40% sticks, and letting go releases.
     *
     * <p>WHY: Holding W and S together must not pick one at random, fine control needs Shift,
     * and a released key must never leave the stick pushed (a runaway robot).</p>
     */
    @Test
    void oppositeKeysCancelShiftScalesAndReleaseCenters() {
        HeldKeys keys = new HeldKeys();
        pvi.harness.ds.GamepadSource driver = KeyboardGamepads.driver(keys);
        keys.press(KeyEvent.VK_W);
        keys.press(KeyEvent.VK_S);
        assertEquals(0.0, Axis.LEFT_STICK_Y.read(read(driver)), 1e-6);

        keys.release(KeyEvent.VK_S);
        keys.press(KeyEvent.VK_SHIFT);
        assertEquals(-KeyboardGamepads.SHIFT_STICK_SCALE, Axis.LEFT_STICK_Y.read(read(driver)), 1e-6);

        Gamepad pad = new Gamepad();
        driver.update(pad, 0.0, 0.0);
        keys.releaseAll();
        driver.update(pad, 0.0, 0.0);
        assertEquals(0.0, Axis.LEFT_STICK_Y.read(pad), 1e-6, "every input is set each loop, so release centers it");
    }

    /**
     * WHAT: Driver and operator buttons and triggers go to the right gamepad.
     *
     * <p>WHY: Team A's operator runs the intake with A/B/X/Y on gamepad 2 (J/K/U/I); the driver's
     * F/G/R/T and Q/E must not leak into gamepad 2.</p>
     */
    @Test
    void buttonsAndTriggersGoToTheRightGamepad() {
        HeldKeys keys = new HeldKeys();
        keys.press(KeyEvent.VK_F);   // driver A
        keys.press(KeyEvent.VK_E);   // driver RB
        keys.press(KeyEvent.VK_C);   // driver RT
        keys.press(KeyEvent.VK_U);   // operator X
        keys.press(KeyEvent.VK_N);   // operator LT
        Gamepad driver = read(KeyboardGamepads.driver(keys));
        Gamepad operator = read(KeyboardGamepads.operator(keys));

        assertTrue(Button.A.isPressed(driver));
        assertTrue(Button.RIGHT_BUMPER.isPressed(driver));
        assertEquals(1.0, Axis.RIGHT_TRIGGER.read(driver), 1e-6);
        assertFalse(Button.X.isPressed(driver), "U is the operator's X, not the driver's");

        assertTrue(Button.X.isPressed(operator));
        assertEquals(1.0, Axis.LEFT_TRIGGER.read(operator), 1e-6);
        assertFalse(Button.A.isPressed(operator), "F is the driver's A, not the operator's");
    }

    /**
     * WHAT: Holding W through a live drive of the real Team A TeleOp drives the robot forward,
     * records the keys as gamepad input, and ends with STOP at the end of the match.
     *
     * <p>WHY: This is the whole D2 path: keys, gamepad source, real OpMode, recording.</p>
     */
    @Test
    void holdingWDrivesTheRealTeleOpForward() throws InterruptedException {
        HeldKeys keys = new HeldKeys();
        keys.press(KeyEvent.VK_W);
        LiveDrive drive = new LiveDrive(LiveDrive.options().get(0), keys, 1.0, false);
        drive.requestPlay();
        drive.start();

        assertTrue(drive.awaitFinished(30), "the drive should finish at the end of its 1 s match");
        assertNull(drive.error());
        RunRecording recording = drive.latest();
        List<RunRecording.LoopRecord> loops = recording.loops();
        RunRecording.LoopRecord last = loops.get(loops.size() - 1);

        assertEquals("TeamATeleOp", recording.header().opMode());
        assertEquals(RunResult.Stage.STOP, last.stage());
        assertEquals(1.0, last.matchTimeSeconds(), 0.03);
        assertTrue(last.pose().xInches() > 10, "1 s of full forward should move well over 10 in, was "
                + last.pose().xInches());
        assertEquals(-1.0, loops.get(loops.size() - 2).gamepad1().leftStickY(), 1e-6,
                "the recording shows the stick the keyboard pushed");
    }

    /**
     * WHAT: Pressing STOP during INIT ends the drive without ever pressing PLAY.
     *
     * <p>WHY: Esc must always get you out, even before PLAY.</p>
     */
    @Test
    void stopDuringInitNeverPlays() throws InterruptedException {
        LiveDrive drive = new LiveDrive(LiveDrive.options().get(0), new HeldKeys(), 120.0, false);
        drive.requestStop();
        drive.start();

        assertTrue(drive.awaitFinished(30));
        assertTrue(drive.latest().loops().stream().noneMatch(loop -> loop.stage() == RunResult.Stage.LOOP),
                "no LOOP calls without PLAY");
    }

    /**
     * WHAT: Whether a drive is saved is decided when PLAY is pressed (the Record button), and a
     * later press can't change it.
     *
     * <p>WHY: Record must be turned on before Enter. Changing it mid-drive, or pressing Enter
     * again, must not flip a practice drive into a saved one (or the other way round).</p>
     */
    @Test
    void recordIsDecidedAtPlayAndLocked() throws InterruptedException {
        LiveDrive practice = new LiveDrive(LiveDrive.options().get(0), new HeldKeys(), 0.2, false);
        practice.requestPlay(false);
        practice.requestPlay(true);   // ignored: PLAY was already pressed
        practice.start();
        assertTrue(practice.awaitFinished(30));
        assertFalse(practice.keepRecording(), "Record was off at PLAY");
        assertTrue(practice.latest().loops().size() > 5, "a practice drive still shows in the window");

        LiveDrive recorded = new LiveDrive(LiveDrive.options().get(0), new HeldKeys(), 0.2, false);
        recorded.requestPlay(true);
        recorded.start();
        assertTrue(recorded.awaitFinished(30));
        assertTrue(recorded.keepRecording(), "Record was on at PLAY");
    }

    /**
     * WHAT: A drive stopped before PLAY is never kept, even with Record on.
     *
     * <p>WHY: There is nothing to keep: only INIT happened.</p>
     */
    @Test
    void stoppedBeforePlayIsNeverKept() throws InterruptedException {
        LiveDrive drive = new LiveDrive(LiveDrive.options().get(0), new HeldKeys(), 120.0, false);
        drive.requestStop();
        drive.requestPlay(true);   // too late: STOP came first
        drive.start();
        assertTrue(drive.awaitFinished(30));
        assertFalse(drive.keepRecording());
    }

    /**
     * WHAT: The dropdown lists the live drives first, then scenarios, then recordings.
     *
     * <p>WHY: Driving is the new main use; it should be easy to find.</p>
     */
    @Test
    void dropdownListsDrivesFirst(@TempDir Path folder) {
        List<ViewerChoices.Choice> choices = ViewerChoices.list(LiveDrive.options(), Scenarios.all(), folder);

        assertEquals(LiveDrive.options().size() + Scenarios.all().size(), choices.size());
        assertTrue(choices.get(0).isDrive());
        assertEquals("Drive: Team A TeleOp (keyboard)", choices.get(0).label());
        assertTrue(choices.get(LiveDrive.options().size()).isScenario());
    }
}

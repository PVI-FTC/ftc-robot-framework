package org.firstinspires.ftc.teamcode.opmodes.teleop;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.common.localization.PoseEstimate;
import org.firstinspires.ftc.teamcode.core.input.InputManager;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroConfiguration;
import org.firstinspires.ftc.teamcode.robots.teamA.TeamAPedroRobot;

/** Team A's Pinpoint-backed TeleOp, including active heading hold. */
@TeleOp(name = "Team A Pedro Drive", group = "Team A")
public final class TeamAPedroTeleOp extends OpMode {
    private static final double TWO_PI = 2.0 * Math.PI;

    private TeamAPedroRobot robot;
    private InputManager driverInput;
    private boolean headingHoldEnabled;
    private double startHeadingRadians;
    private Double selectedPresetHeadingRadians;
    private int selectedPresetSector = Integer.MIN_VALUE;

    @Override
    public void init() {
        robot = new TeamAPedroRobot();
        robot.initialize(hardwareMap);
        driverInput = new InputManager(gamepad1);
        telemetry.addData("Status", "Team A Pedro robot initialized");
        telemetry.addLine("Y: toggle heading hold; X: reset forward; hold RB + aim right stick for preset");
        telemetry.update();
    }

    @Override
    public void start() {
        driverInput.update();
        headingHoldEnabled = false;
        selectedPresetHeadingRadians = null;
        selectedPresetSector = Integer.MIN_VALUE;
        captureStartHeading();
        robot.enableManualDrive();
    }

    @Override
    public void loop() {
        driverInput.update();
        if (driverInput.wasXJustPressed()) {
            captureStartHeading();
            selectedPresetHeadingRadians = null;
            selectedPresetSector = Integer.MIN_VALUE;
            if (headingHoldEnabled) {
                robot.setHeadingTarget(startHeadingRadians);
            }
        }
        applyPresetHeadingSelection();
        if (driverInput.wasYJustPressed()) {
            headingHoldEnabled = !headingHoldEnabled;
            if (headingHoldEnabled) {
                if (selectedPresetHeadingRadians == null) {
                    robot.enableHeadingHold();
                } else {
                    robot.enableHeadingHold(selectedPresetHeadingRadians);
                }
            } else {
                selectedPresetHeadingRadians = null;
                selectedPresetSector = Integer.MIN_VALUE;
                robot.enableManualDrive();
            }
        }

        robot.drive(-driverInput.getLeftStickY(), driverInput.getLeftStickX(),
                driverInput.getRightStickX());
        robot.update();
        publishTelemetry();
    }

    @Override
    public void stop() {
        if (robot != null) robot.stop();
    }

    private void publishTelemetry() {
        PoseEstimate pose = robot.getPoseEstimate();
        telemetry.addData("Drive State", robot.getDriveStateName());
        telemetry.addData("Pinpoint Heading (deg)", Math.toDegrees(
                pose.getHeadingRadians() - TeamAPedroConfiguration.HEADING_FRAME_OFFSET_RADIANS));
        telemetry.addData("Heading Hold", headingHoldEnabled ? "ON" : "OFF");
        telemetry.addData("Start Heading (deg)", Math.toDegrees(
                startHeadingRadians - TeamAPedroConfiguration.HEADING_FRAME_OFFSET_RADIANS));
        telemetry.addData("Preset Heading (deg)", selectedPresetHeadingRadians == null
                ? "none" : Math.toDegrees(selectedPresetHeadingRadians));
        telemetry.update();
    }

    private void applyPresetHeadingSelection() {
        if (!driverInput.isRightBumperHeld()) {
            return;
        }

        double stickX = driverInput.getRightStickX();
        double stickY = -driverInput.getRightStickY();
        if (Math.hypot(stickX, stickY) < TeamAPedroConfiguration.PRESET_DIRECTION_DEADZONE) {
            return;
        }

        double joystickAngle = Math.atan2(stickY, stickX);
        int presetSector = selectPresetSector(joystickAngle);
        if (presetSector == Integer.MIN_VALUE) {
            return;
        }
        // Stick-forward is zero offset from startup; stick-right is 90 degrees clockwise.
        double headingOffset = presetSector * (Math.PI / 4.0) - Math.PI / 2.0;
        double selectedHeading = normalizeAngle(startHeadingRadians + headingOffset);
        if (selectedPresetHeadingRadians == null
                || Math.abs(normalizeAngle(selectedHeading - selectedPresetHeadingRadians)) > 1e-9) {
            boolean wasHeadingHoldEnabled = headingHoldEnabled;
            selectedPresetSector = presetSector;
            selectedPresetHeadingRadians = selectedHeading;
            headingHoldEnabled = true;
            if (wasHeadingHoldEnabled) {
                robot.setHeadingTarget(selectedHeading);
            } else {
                robot.enableHeadingHold(selectedHeading);
            }
        }
    }

    private int selectPresetSector(double joystickAngleRadians) {
        int candidateSector = (int) Math.round(joystickAngleRadians / (Math.PI / 4.0));
        if (selectedPresetSector == Integer.MIN_VALUE) {
            return candidateSector;
        }

        double currentSectorHeading = selectedPresetSector * (Math.PI / 4.0);
        double distanceFromCurrent = Math.abs(normalizeAngle(joystickAngleRadians
                - currentSectorHeading));
        double sectorBoundary = Math.PI / 8.0
                + TeamAPedroConfiguration.PRESET_DIRECTION_HYSTERESIS_RADIANS;
        return distanceFromCurrent > sectorBoundary ? candidateSector : Integer.MIN_VALUE;
    }

    private double normalizeAngle(double angleRadians) {
        double normalized = angleRadians % TWO_PI;
        if (normalized <= -Math.PI) normalized += TWO_PI;
        if (normalized > Math.PI) normalized -= TWO_PI;
        return normalized;
    }

    private void captureStartHeading() {
        PoseEstimate pose = robot.getPoseEstimate();
        if (pose.isAvailable() && Double.isFinite(pose.getHeadingRadians())) {
            startHeadingRadians = pose.getHeadingRadians();
        }
    }
}

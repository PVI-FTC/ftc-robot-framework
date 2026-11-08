package org.firstinspires.ftc.teamcode.robots.team3;

import com.qualcomm.hardware.lynx.LynxModule;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.common.autonomous.AutonomousRobotControl;
import org.firstinspires.ftc.teamcode.common.hardware.DriveHardware;
import org.firstinspires.ftc.teamcode.common.hardware.RobotHardware;
import org.firstinspires.ftc.teamcode.common.hardware.VisionHardware;
import org.firstinspires.ftc.teamcode.common.subsystems.drive.DriveSubsystem;
import org.firstinspires.ftc.teamcode.common.subsystems.intake.IntakeSubsystem;
import org.firstinspires.ftc.teamcode.common.subsystems.vision.VisionSubsystem;
import org.firstinspires.ftc.teamcode.common.vision.AprilTagObservation;
import org.firstinspires.ftc.teamcode.common.vision.AprilTagObservationSnapshot;
import org.firstinspires.ftc.teamcode.core.robot.Robot;

import java.util.ArrayList;
import java.util.List;

/**
 * Team 3's robot composition and public drivetrain API.
 *
 * <p>OpModes call this robot's public methods instead of manipulating mechanisms directly. This
 * keeps FTC entry points focused on mapping controls, while the robot and subsystem layers retain
 * responsibility for behavior and hardware access.</p>
 */
public class Team3Robot extends Robot implements AutonomousRobotControl {
    private final RobotHardware robotHardware;
    private final VisionHardware visionHardware;
    private final Team3MechanismHardware mechanismHardware = new Team3MechanismHardware();
    private final Team3PinpointHardware pinpointHardware = new Team3PinpointHardware();
    private final Team3StabilizedDriveController driveController;
    private final DriveSubsystem driveSubsystem;
    private final IntakeSubsystem intakeSubsystem;
    private final Team3IntakeDeploySubsystem intakeDeploySubsystem;
    private final Team3MidtakeSubsystem midtakeSubsystem;
    private final Team3OuttakeSubsystem outtakeSubsystem;
    private final VisionSubsystem visionSubsystem;
    private boolean hardwareInitialized;
    private Team3HiveGoal lastSnappedGoal;
    private int lastSnappedTagCount;
    private double lastSnappedGoalRangeInches;
    private double lastSnappedGoalBearingDegrees;

    /** Creates Team 3's robot with the shared baseline hardware composition and Team 3 vision. */
    public Team3Robot() {
        this(new RobotHardware(), Team3VisionConfiguration.createVisionHardware());
    }

    /**
     * Creates Team 3's robot with the supplied hardware composition.
     *
     * @param robotHardware the hardware wrappers owned by this robot
     */
    public Team3Robot(RobotHardware robotHardware) {
        this(robotHardware, robotHardware == null ? null : robotHardware.getVisionHardware());
    }

    /**
     * Creates Team 3's robot with the supplied hardware and vision wrappers.
     *
     * @param robotHardware the hardware wrappers owned by this robot
     * @param visionHardware the vision hardware wrapper owned by this robot
     */
    public Team3Robot(RobotHardware robotHardware, VisionHardware visionHardware) {
        if (robotHardware == null || visionHardware == null) {
            throw new IllegalArgumentException("Team 3 robot needs robot and vision hardware.");
        }

        this.robotHardware = robotHardware;
        this.visionHardware = visionHardware;
        driveController = new Team3StabilizedDriveController(robotHardware.getDriveHardware(), pinpointHardware);
        driveSubsystem = new DriveSubsystem(driveController);
        intakeSubsystem = new IntakeSubsystem(robotHardware.getIntakeHardware());
        intakeDeploySubsystem = new Team3IntakeDeploySubsystem(mechanismHardware);
        midtakeSubsystem = new Team3MidtakeSubsystem(mechanismHardware);
        outtakeSubsystem = new Team3OuttakeSubsystem(mechanismHardware);
        visionSubsystem = new VisionSubsystem(this.visionHardware);
        registerSubsystem(driveSubsystem);
        registerSubsystem(intakeSubsystem);
        registerSubsystem(intakeDeploySubsystem);
        registerSubsystem(midtakeSubsystem);
        registerSubsystem(outtakeSubsystem);
        registerSubsystem(visionSubsystem);
    }

    /**
     * Initializes hardware first, then initializes every registered subsystem once.
     *
     * @param hardwareMap the FTC hardware map supplied by the OpMode
     */
    public void initialize(HardwareMap hardwareMap) {
        if (hardwareInitialized) {
            return;
        }

        // Enable Lynx bulk caching mode AUTO to minimize I2C/CAN bus latency and maximize loop speed
        try {
            List<LynxModule> allHubs = hardwareMap.getAll(LynxModule.class);
            for (LynxModule hub : allHubs) {
                hub.setBulkCachingMode(LynxModule.BulkCachingMode.AUTO);
            }
        } catch (Exception ignored) {
            // Safe fallback if running in a mock environment
        }

        robotHardware.initialize(hardwareMap);
        visionHardware.initialize(hardwareMap);
        mechanismHardware.initialize(hardwareMap);
        pinpointHardware.initialize(hardwareMap);
        hardwareInitialized = true;
        super.initialize();
    }

    /** Requests mecanum drive values for the next robot update. */
    public void drive(double forward, double strafe, double rotate) {
        driveSubsystem.drive(forward, strafe, rotate);
    }

    /**
     * Requests zero drive power and resets the pose-lock anchor to current position.
     */
    public void brake() {
        driveController.brake();
        driveSubsystem.drive(0.0, 0.0, 0.0);
    }

    /** Requests drivetrain behavior that applies the stored drive values. */
    public void enableRequestedDrive() {
        driveSubsystem.enableRequestedDrive();
    }

    /**
     * Requests normal manual drivetrain behavior.
     */
    public void enableManualDrive() {
        driveController.getPoseLock().setAssistLevel(Team3DriveAssistLevel.OFF);
        enableRequestedDrive();
    }

    /** Requests the safe disabled drivetrain behavior. */
    public void disableDrive() {
        driveSubsystem.disableDrive();
    }

    /** Requests heading hold behavior (corrects rotation only). */
    public void enableHeadingHold() {
        driveController.getPoseLock().setAssistLevel(Team3DriveAssistLevel.HEADING);
        driveSubsystem.enableHeadingHold();
    }

    /** Sets the drive assist level (OFF, HEADING, or FULL). */
    public void setDriveAssistLevel(Team3DriveAssistLevel level) {
        driveController.getPoseLock().setAssistLevel(level);
        if (level == Team3DriveAssistLevel.OFF) {
            driveSubsystem.enableManualDrive();
        } else {
            driveSubsystem.enableHeadingHold();
        }
    }

    /** Returns current drive assist level. */
    public Team3DriveAssistLevel getDriveAssistLevel() {
        return driveController.getPoseLock().getAssistLevel();
    }

    /** Toggles XY pose lock between HEADING (direction holding only) and FULL (direction + XY hold). */
    public Team3DriveAssistLevel toggleDriveAssist() {
        if (getDriveAssistLevel() == Team3DriveAssistLevel.FULL) {
            setDriveAssistLevel(Team3DriveAssistLevel.HEADING);
        } else {
            setDriveAssistLevel(Team3DriveAssistLevel.FULL);
            if (pinpointHardware.isAvailable()) {
                driveController.getPoseLock().resetAnchor(
                        pinpointHardware.getXInches(),
                        pinpointHardware.getYInches(),
                        pinpointHardware.getHeadingRadians());
            }
        }
        return getDriveAssistLevel();
    }

    /** Returns whether full XY pose lock is currently active (FULL level). */
    public boolean isPoseLockActive() {
        return driveController.getPoseLock().getAssistLevel() == Team3DriveAssistLevel.FULL;
    }

    /** Cycles drive assist level: OFF -> HEADING -> FULL -> OFF. */
    public Team3DriveAssistLevel cycleDriveAssistLevel() {
        Team3DriveAssistLevel nextLevel = driveController.getPoseLock().getAssistLevel().next();
        setDriveAssistLevel(nextLevel);
        return nextLevel;
    }

    /**
     * Snaps the heading hold target to a specified angle in degrees and activates heading hold.
     *
     * @param headingDegrees target angle in degrees (e.g. 0 forward, 90 left, 180 back, -90 right)
     */
    public void snapToHeading(double headingDegrees) {
        driveController.getPoseLock().snapToHeading(
                Math.toRadians(headingDegrees),
                pinpointHardware.getXInches(),
                pinpointHardware.getYInches());
        driveSubsystem.enableHeadingHold();
    }

    /**
     * Automatically aims the robot's target heading at the best visible AprilTag (rotation only).
     *
     * <p>Prefers HIVE Goal tags (30-45) and DECODE Goal tags (20, 24) first, accounting for the
     * tag's horizontal offset within its 4-tag HIVE cluster so the robot aims at the true center
     * of the goal. Updates {@link Team3PoseLock}'s target heading so the closed-loop heading
     * controller smoothly rotates the shooter to face the goal.</p>
     *
     * @return the AprilTag ID that was aimed at, or {@code -1} if no tag with a valid bearing is visible
     */
    public int aimAtVisibleAprilTag() {
        List<AprilTagObservation> observations = getAprilTagObservations();
        if (observations.isEmpty() || !pinpointHardware.isAvailable()) {
            return -1;
        }

        // First check if any of the 4 HIVE goals is visible so we can aim at the goal
        Team3HiveGoal visibleGoal = findBestVisibleHiveGoal(observations);
        if (visibleGoal != null) {
            int clusterCount = 0;
            for (AprilTagObservation obs : observations) {
                if (obs.isRobotRelativePoseAvailable()
                        && Team3HiveGoal.fromTagId(obs.getTagId()) == visibleGoal) {
                    clusterCount++;
                }
            }
            double sumGoalFwd = 0.0;
            double sumGoalRight = 0.0;
            int count = 0;
            int firstTagId = -1;
            for (AprilTagObservation obs : observations) {
                if (obs.isRobotRelativePoseAvailable()
                        && Team3HiveGoal.fromTagId(obs.getTagId()) == visibleGoal) {
                    // When 2+ tags of a HIVE cluster are in view, shift each tag by its cluster offset
                    // to aim at the cluster center; when 1 tag is in view, aim directly at that tag.
                    double offset = clusterCount >= 2
                            ? Team3HiveGoal.getTagClusterLateralOffsetInches(obs.getTagId())
                            : 0.0;
                    double yawRad = Math.toRadians(obs.getYawDegrees());
                    sumGoalFwd += obs.getForwardInches() - offset * Math.sin(yawRad);
                    sumGoalRight += obs.getRightInches() - offset * Math.cos(yawRad);
                    count++;
                    if (firstTagId < 0) {
                        firstTagId = obs.getTagId();
                    }
                }
            }
            if (count > 0) {
                double goalFwd = sumGoalFwd / count;
                double goalRight = sumGoalRight / count;
                double bearingDegrees = Math.toDegrees(Math.atan2(-goalRight, goalFwd));
                double targetHeadingDegrees = getHeadingDegrees()
                        + bearingDegrees
                        + visibleGoal.getTargetHeadingOffsetDegrees();
                snapToHeading(targetHeadingDegrees);
                lastSnappedGoal = visibleGoal;
                lastSnappedTagCount = count;
                lastSnappedGoalRangeInches = Math.hypot(goalFwd, goalRight);
                lastSnappedGoalBearingDegrees = bearingDegrees;
                return firstTagId;
            }
        }

        // Fallback to any non-goal AprilTag with a valid robot-relative bearing
        for (AprilTagObservation obs : observations) {
            if (obs.isRobotRelativePoseAvailable()) {
                double targetHeadingDegrees = getHeadingDegrees() + obs.getBearingDegrees();
                snapToHeading(targetHeadingDegrees);
                return obs.getTagId();
            }
        }

        return -1;
    }

    /**
     * Automatically snaps and navigates the robot in 3-DOF (distance, lateral alignment, and angle)
     * to the configured shooting spot in front of whichever of the 4 HIVE Goals is visible.
     *
     * <p>How it works:
     * <ol>
     *   <li>Finds the best visible {@link Team3HiveGoal} from the current AprilTag observations.</li>
     *   <li>For each visible tag belonging to that HIVE goal (1 to 4 tags out of the 4-tag cluster),
     *       shifts from the individual tag's position by its exact horizontal cluster offset
     *       ({@code -6.50, -2.75, +2.75, +6.50 in}) along the goal face to compute the true 2D
     *       center of that HIVE goal, then averages across all visible tags of that goal.</li>
     *   <li>Calculates the target anchor pose at {@link Team3HiveGoal#getTargetDistanceInches()}
     *       in front of the goal center (plus any configured lateral or heading offset) and
     *       transforms it into Pinpoint field coordinates {@code (targetX, targetY, targetHeading)}.</li>
     *   <li>Commands {@link Team3PoseLock#setTargetPoseAnchor} in {@code FULL} 3-DOF mode so the
     *       closed-loop controller drives, strafes, and rotates smoothly to the exact spot and holds it.</li>
     * </ol></p>
     *
     * @return the {@link Team3HiveGoal} that the robot snapped to, or {@code null} if no goal tag
     *         with a metric pose is currently in view or Pinpoint is unavailable
     */
    public Team3HiveGoal snapToVisibleGoalSpot() {
        List<AprilTagObservation> observations = getAprilTagObservations();
        if (observations.isEmpty() || !pinpointHardware.isAvailable()) {
            return null;
        }

        Team3HiveGoal targetGoal = findBestVisibleHiveGoal(observations);
        if (targetGoal == null) {
            // Classroom / lab fallback: if a non-HIVE tag (e.g. tag 1-19 or 22) is visible,
            // snap to DEFAULT_GOAL_DISTANCE_INCHES directly in front of that tag.
            for (AprilTagObservation obs : observations) {
                if (obs.isRobotRelativePoseAvailable()) {
                    double tagFwd = obs.getForwardInches();
                    double tagLeft = -obs.getRightInches();
                    double range = Math.hypot(tagFwd, tagLeft);
                    if (range < 1.0) {
                        continue;
                    }
                    double bearingRad = Math.atan2(tagLeft, tagFwd);
                    double moveDist = range - Team3VisionConfiguration.DEFAULT_GOAL_DISTANCE_INCHES;
                    double moveFwd = moveDist * Math.cos(bearingRad);
                    double moveLeft = moveDist * Math.sin(bearingRad);

                    double currentOdoX = pinpointHardware.getXInches();
                    double currentOdoY = pinpointHardware.getYInches();
                    double currentOdoHeading = pinpointHardware.getHeadingRadians();
                    double targetOdoX = currentOdoX
                            + moveFwd * Math.cos(currentOdoHeading)
                            - moveLeft * Math.sin(currentOdoHeading);
                    double targetOdoY = currentOdoY
                            + moveFwd * Math.sin(currentOdoHeading)
                            + moveLeft * Math.cos(currentOdoHeading);
                    double targetOdoHeading = Team3PoseLock.normalizeAngle(currentOdoHeading + bearingRad);

                    driveController.getPoseLock().setTargetPoseAnchor(
                            targetOdoX, targetOdoY, targetOdoHeading,
                            currentOdoX, currentOdoY, currentOdoHeading);
                    driveSubsystem.enableHeadingHold();
                    lastSnappedTagCount = 1;
                    lastSnappedGoalRangeInches = range;
                    lastSnappedGoalBearingDegrees = Math.toDegrees(bearingRad);
                    return Team3HiveGoal.RED_AUDIENCE;
                }
            }
            return null;
        }

        // First compute cluster face yaw: when 2+ tags of the cluster are visible, derive the
        // exact horizontal goal-face tangent from the outermost visible tags' floor positions.
        double minClusterOffset = Double.POSITIVE_INFINITY;
        double maxClusterOffset = Double.NEGATIVE_INFINITY;
        double minOffsetTagFwd = 0.0;
        double minOffsetTagRight = 0.0;
        double maxOffsetTagFwd = 0.0;
        double maxOffsetTagRight = 0.0;
        double sumSinYaw = 0.0;
        double sumCosYaw = 0.0;
        int tagCount = 0;

        for (AprilTagObservation obs : observations) {
            if (obs.isRobotRelativePoseAvailable()
                    && Team3HiveGoal.fromTagId(obs.getTagId()) == targetGoal) {
                double tagRight = obs.getRightInches();
                double tagFwd = obs.getForwardInches();
                double yawRad = Math.toRadians(obs.getYawDegrees());
                double clusterOffsetInches = Team3HiveGoal.getTagClusterLateralOffsetInches(obs.getTagId());

                if (clusterOffsetInches < minClusterOffset) {
                    minClusterOffset = clusterOffsetInches;
                    minOffsetTagFwd = tagFwd;
                    minOffsetTagRight = tagRight;
                }
                if (clusterOffsetInches > maxClusterOffset) {
                    maxClusterOffset = clusterOffsetInches;
                    maxOffsetTagFwd = tagFwd;
                    maxOffsetTagRight = tagRight;
                }
                sumSinYaw += Math.sin(yawRad);
                sumCosYaw += Math.cos(yawRad);
                tagCount++;
            }
        }

        if (tagCount == 0) {
            return null;
        }

        double avgYawRad;
        if (tagCount >= 2 && (maxClusterOffset - minClusterOffset) > 1.0) {
            avgYawRad = Math.atan2(
                    maxOffsetTagFwd - minOffsetTagFwd,
                    maxOffsetTagRight - minOffsetTagRight);
        } else {
            avgYawRad = Math.atan2(sumSinYaw, sumCosYaw);
        }
        double maxYawRad = Math.toRadians(60.0);
        avgYawRad = Math.max(-maxYawRad, Math.min(maxYawRad, avgYawRad));

        // Average the estimated HIVE goal center across all visible tags in this cluster
        double sumGoalFwd = 0.0;
        double sumGoalRight = 0.0;
        for (AprilTagObservation obs : observations) {
            if (obs.isRobotRelativePoseAvailable()
                    && Team3HiveGoal.fromTagId(obs.getTagId()) == targetGoal) {
                double tagRight = obs.getRightInches();
                double tagFwd = obs.getForwardInches();
                double clusterOffsetInches = Team3HiveGoal.getTagClusterLateralOffsetInches(obs.getTagId());
                double goalCenterFwd = tagFwd - clusterOffsetInches * Math.sin(avgYawRad);
                double goalCenterRight = tagRight - clusterOffsetInches * Math.cos(avgYawRad);
                sumGoalFwd += goalCenterFwd;
                sumGoalRight += goalCenterRight;
            }
        }

        double goalFwdInches = sumGoalFwd / tagCount;
        double goalRightInches = sumGoalRight / tagCount;
        double goalLeftInches = -goalRightInches;
        double goalRangeInches = Math.hypot(goalFwdInches, goalLeftInches);
        if (goalRangeInches < 1.0) {
            return null;
        }

        // 1. Reconstruct robot's current [0, 144] BIOBUZZ field pose from the observed HIVE goal
        double goalFieldX = targetGoal.getGoalFieldXInches();
        double goalFieldY = targetGoal.getGoalFieldYInches();
        double outwardNormalRad = Math.toRadians(targetGoal.getOutwardNormalDegrees());
        double facingGoalFieldHeadingRad = Team3PoseLock.normalizeAngle(outwardNormalRad + Math.PI);
        double bearingToGoalRad = Math.atan2(goalLeftInches, goalFwdInches);
        double robotFieldHeadingRad = Team3PoseLock.normalizeAngle(
                facingGoalFieldHeadingRad - avgYawRad);

        double cosRf = Math.cos(robotFieldHeadingRad);
        double sinRf = Math.sin(robotFieldHeadingRad);
        double robotFieldX = goalFieldX - (goalFwdInches * cosRf - goalLeftInches * sinRf);
        double robotFieldY = goalFieldY - (goalFwdInches * sinRf + goalLeftInches * cosRf);

        // 2. Compute ideal perpendicular shooting spot in front of the HIVE goal in [0, 144] field coords
        double standoffInches = targetGoal.getTargetDistanceInches();
        double lateralRightInches = targetGoal.getTargetLateralOffsetInches();
        double idealSpotFieldX = goalFieldX
                + standoffInches * Math.cos(outwardNormalRad)
                - lateralRightInches * Math.sin(outwardNormalRad);
        double idealSpotFieldY = goalFieldY
                + standoffInches * Math.sin(outwardNormalRad)
                + lateralRightInches * Math.cos(outwardNormalRad);

        // 3. Plan collision-free route around the 6 BIOBUZZ floor obstacles and 4 perimeter walls
        List<double[]> fieldRoute = Team3FieldObstacles.planSafeFieldRoute(
                robotFieldX, robotFieldY, idealSpotFieldX, idealSpotFieldY);

        // 4. Transform the planned field route into Pinpoint odometry coordinates
        double currentOdoX = pinpointHardware.getXInches();
        double currentOdoY = pinpointHardware.getYInches();
        double currentOdoHeadingRad = pinpointHardware.getHeadingRadians();
        double odoMinusFieldHeadingRad = Team3PoseLock.normalizeAngle(currentOdoHeadingRad - robotFieldHeadingRad);
        double cosT = Math.cos(odoMinusFieldHeadingRad);
        double sinT = Math.sin(odoMinusFieldHeadingRad);

        List<double[]> odoWaypoints = new ArrayList<>(fieldRoute.size());
        for (double[] fWp : fieldRoute) {
            double dFx = fWp[0] - robotFieldX;
            double dFy = fWp[1] - robotFieldY;
            double odoWpX = currentOdoX + dFx * cosT - dFy * sinT;
            double odoWpY = currentOdoY + dFx * sinT + dFy * cosT;
            odoWaypoints.add(new double[]{odoWpX, odoWpY});
        }

        double[] finalFieldSpot = fieldRoute.get(fieldRoute.size() - 1);
        double finalTargetFieldHeadingRad = Math.atan2(
                goalFieldY - finalFieldSpot[1],
                goalFieldX - finalFieldSpot[0])
                + Math.toRadians(targetGoal.getTargetHeadingOffsetDegrees());
        double targetOdoHeadingRad = Team3PoseLock.normalizeAngle(
                finalTargetFieldHeadingRad + odoMinusFieldHeadingRad);

        driveController.getPoseLock().setTargetPoseRoute(
                odoWaypoints,
                targetOdoHeadingRad,
                currentOdoX,
                currentOdoY,
                currentOdoHeadingRad,
                robotFieldX,
                robotFieldY,
                odoMinusFieldHeadingRad);
        driveSubsystem.enableHeadingHold();

        lastSnappedGoal = targetGoal;
        lastSnappedTagCount = tagCount;
        lastSnappedGoalRangeInches = goalRangeInches;
        lastSnappedGoalBearingDegrees = Math.toDegrees(bearingToGoalRad);
        return targetGoal;
    }

    /**
     * Selects the best visible {@link Team3HiveGoal} from the current observations, preferring the
     * goal with the most visible cluster tags (or closest range on ties).
     */
    private Team3HiveGoal findBestVisibleHiveGoal(List<AprilTagObservation> observations) {
        Team3HiveGoal bestGoal = null;
        int bestCount = 0;
        double bestMinRange = Double.POSITIVE_INFINITY;

        for (Team3HiveGoal candidate : Team3HiveGoal.values()) {
            int count = 0;
            double minRange = Double.POSITIVE_INFINITY;
            for (AprilTagObservation obs : observations) {
                if (obs.isRobotRelativePoseAvailable()
                        && Team3HiveGoal.fromTagId(obs.getTagId()) == candidate) {
                    count++;
                    if (obs.getRangeInches() < minRange) {
                        minRange = obs.getRangeInches();
                    }
                }
            }
            if (count > bestCount || (count > 0 && count == bestCount && minRange < bestMinRange)) {
                bestGoal = candidate;
                bestCount = count;
                bestMinRange = minRange;
            }
        }
        return bestGoal;
    }

    /** Returns a compact summary of currently visible AprilTags (IDs, range, bearing) for telemetry. */
    public String getVisibleTagsTelemetrySummary() {
        List<AprilTagObservation> obsList = getAprilTagObservations();
        if (obsList.isEmpty()) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < obsList.size(); i++) {
            if (i > 0) {
                sb.append(", ");
            }
            AprilTagObservation obs = obsList.get(i);
            if (obs.isRobotRelativePoseAvailable()) {
                sb.append(String.format("#%d(%.0fin,%.0fdeg)",
                        obs.getTagId(), obs.getRangeInches(), obs.getBearingDegrees()));
            } else {
                sb.append(String.format("#%d(ID-only)", obs.getTagId()));
            }
        }
        return sb.toString();
    }

    /** Returns the most recently snapped {@link Team3HiveGoal}, or {@code null} if none yet. */
    public Team3HiveGoal getLastSnappedGoal() {
        return lastSnappedGoal;
    }

    /** Returns a formatted telemetry summary of the last snapped HIVE goal. */
    public String getLastSnappedGoalSummary() {
        if (lastSnappedGoal == null) {
            return "NONE (Press D-Right w/ tag in view)";
        }
        int wpCount = driveController.getPoseLock().getActiveRouteWaypointCount();
        String routeMode = wpCount > 1 ? "DETOUR (" + wpCount + " wps)" : "DIRECT";
        return String.format("%s [%s] (%d tag%s, %.1f in, %.1f deg)",
                lastSnappedGoal.getDisplayName(),
                routeMode,
                lastSnappedTagCount,
                lastSnappedTagCount == 1 ? "" : "s",
                lastSnappedGoalRangeInches,
                lastSnappedGoalBearingDegrees);
    }

    /** Returns whether heading hold or full pose stabilization is currently active. */
    public boolean isHeadingHoldActive() {
        return driveController.getPoseLock().getAssistLevel() != Team3DriveAssistLevel.OFF;
    }

    /** Returns current X position in inches reported by Pinpoint. */
    public double getXInches() {
        return pinpointHardware.getXInches();
    }

    /** Returns current Y position in inches reported by Pinpoint. */
    public double getYInches() {
        return pinpointHardware.getYInches();
    }

    /** Returns locked target X in inches when assist is active. */
    public double getTargetXInches() {
        return driveController.getPoseLock().getTargetXInches();
    }

    /** Returns locked target Y in inches when assist is active. */
    public double getTargetYInches() {
        return driveController.getPoseLock().getTargetYInches();
    }

    /** Returns current heading in degrees reported by Pinpoint. */
    public double getHeadingDegrees() {
        return pinpointHardware.getHeadingDegrees();
    }

    /** Returns locked target heading in degrees when assist is active. */
    public double getTargetHeadingDegrees() {
        return driveController.getPoseLock().getTargetHeadingDegrees();
    }

    /** Returns current translational error in inches for telemetry. */
    public double getPoseLockErrorInches() {
        return driveController.getPoseLock().getErrorInches();
    }

    /** Returns current heading error in degrees for telemetry. */
    public double getPoseLockHeadingErrorDegrees() {
        return driveController.getPoseLock().getHeadingErrorDegrees();
    }

    /** Returns active pose lock translation state name for telemetry. */
    public String getPoseLockTranslationStateName() {
        return driveController.getPoseLock().getTranslationStateName();
    }

    /** Returns current physical speed in inches per second. */
    public double getRobotSpeedIps() {
        return driveController.getPoseLock().getRobotSpeedIps();
    }

    /** Returns the loop execution time in milliseconds. */
    public double getLoopTimeMs() {
        return driveController.getLastLoopTimeMs();
    }

    /** Returns whether the Pinpoint computer was found and is available. */
    public boolean isPinpointAvailable() {
        return pinpointHardware.isAvailable();
    }

    /** Requests normal forward intake behavior. */
    public void startIntake() {
        intakeSubsystem.startIntake();
    }

    /** Requests the safe stopped intake behavior. */
    public void stopIntake() {
        intakeSubsystem.stopIntake();
    }

    /** Sets a low forward power the intake keeps running while idle (0.0 = stopped). */
    public void setIntakeIdlePower(double power) {
        intakeSubsystem.setIdlePower(power);
    }

    /** Sets the forward intake power (0.0 to 1.0). */
    public void setIntakePower(double power) {
        intakeSubsystem.setIntakePower(power);
    }

    /** Sets the reverse eject intake power (0.0 to 1.0). */
    public void setIntakeEjectPower(double power) {
        intakeSubsystem.setEjectPower(power);
    }

    /** Requests the baseline intake holding behavior. */
    public void holdIntake() {
        intakeSubsystem.hold();
    }

    /** Switches the intake-deploy servo between stowed and deployed. */
    public void toggleIntakeDeploy() {
        intakeDeploySubsystem.toggleDeploy();
    }

    /** Requests the deployed position for the intake-deploy servo. */
    public void deployIntake() {
        intakeDeploySubsystem.deploy();
    }

    /** Requests the stowed position for the intake-deploy servo. */
    public void stowIntake() {
        intakeDeploySubsystem.stow();
    }

    /** Requests that the midtake feed (true) or stop (false). */
    public void setMidtakeFeeding(boolean feeding) {
        midtakeSubsystem.setFeeding(feeding);
    }

    /** Requests that the midtake reverse (true) or stop (false). */
    public void setMidtakeReversing(boolean reversing) {
        midtakeSubsystem.setReversing(reversing);
    }

    /** Requests the outtake power from -1.0 (full reverse) to 1.0 (full forward). */
    public void setOuttakePower(double power) {
        outtakeSubsystem.setPower(power);
    }

    /** Requests reverse intake eject behavior. */
    public void ejectIntake() {
        intakeSubsystem.eject();
    }

    /** Requests vision searching when optional vision hardware is available. */
    public void enableVision() {
        visionSubsystem.enableVision();
    }

    /** Requests the safe disabled vision behavior. */
    public void disableVision() {
        visionSubsystem.disableVision();
    }

    /** Returns the active drivetrain state name for telemetry. */
    public String getDriveStateName() {
        return driveSubsystem.getCurrentStateName();
    }

    public double getRequestedForward() {
        return driveSubsystem.getRequestedForward();
    }

    public double getRequestedStrafe() {
        return driveSubsystem.getRequestedStrafe();
    }

    public double getRequestedRotate() {
        return driveSubsystem.getRequestedRotate();
    }

    public double getFrontLeftMotorPower() {
        return getDriveHardware().getFrontLeftPower();
    }

    public double getFrontRightMotorPower() {
        return getDriveHardware().getFrontRightPower();
    }

    public double getRearLeftMotorPower() {
        return getDriveHardware().getRearLeftPower();
    }

    public double getRearRightMotorPower() {
        return getDriveHardware().getRearRightPower();
    }

    /** Returns the active intake state name for telemetry. */
    public String getIntakeStateName() {
        return intakeSubsystem.getCurrentStateName();
    }

    /** Returns whether the optional intake hardware was found during initialization. */
    public boolean isIntakeAvailable() {
        return intakeSubsystem.isAvailable();
    }

    public String getIntakeDeployStateName() {
        return intakeDeploySubsystem.getCurrentStateName();
    }

    public String getMidtakeStateName() {
        return midtakeSubsystem.getCurrentStateName();
    }

    public String getOuttakeStateName() {
        return outtakeSubsystem.getCurrentStateName();
    }

    /** Returns the commanded power for the outtake motor (-1.0 to 1.0). */
    public double getOuttakePower() {
        return outtakeSubsystem.getPower();
    }

    /** Returns whether the outtake, midtake, and intake-deploy devices were all found. */
    public boolean areMechanismsAvailable() {
        return outtakeSubsystem.isAvailable() && midtakeSubsystem.isAvailable()
                && intakeDeploySubsystem.isAvailable();
    }

    /** Returns the active vision state name for telemetry. */
    public String getVisionStateName() {
        return visionSubsystem.getCurrentStateName();
    }

    /** Returns whether optional vision hardware is available. */
    public boolean isVisionAvailable() {
        return visionSubsystem.isAvailable();
    }

    /** Returns the latest immutable, library-neutral AprilTag observations. */
    public List<AprilTagObservation> getAprilTagObservations() {
        return visionSubsystem.getLatestObservations();
    }

    /** Returns observations plus their neutral fresh, retained, or unavailable status. */
    public AprilTagObservationSnapshot getAprilTagObservationSnapshot() {
        return visionSubsystem.getLatestSnapshot();
    }

    @Override
    protected void onStop() {
        robotHardware.stopAll();
        visionHardware.stop();
        mechanismHardware.stop();
    }

    private DriveHardware getDriveHardware() {
        return robotHardware.getDriveHardware();
    }
}

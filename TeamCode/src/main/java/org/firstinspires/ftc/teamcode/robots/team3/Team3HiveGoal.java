package org.firstinspires.ftc.teamcode.robots.team3;

/**
 * Represents the 4 BIOBUZZ HIVE Goals (2 Red, 2 Blue) and their 4-tag AprilTag clusters
 * (plus DECODE Goal fallback tags 20 and 24).
 *
 * <p>From the official BIOBUZZ CAD ({@code biobuzz-2026.json}):
 * <ul>
 *   <li>{@link #RED_SCORING} (Far Red Cell): Tags 30, 31, 32, 33</li>
 *   <li>{@link #RED_AUDIENCE} (Near Red Cell): Tags 34, 35, 36, 37</li>
 *   <li>{@link #BLUE_AUDIENCE} (Near Blue Cell): Tags 38, 39, 40, 41</li>
 *   <li>{@link #BLUE_SCORING} (Far Blue Cell): Tags 42, 43, 44, 45</li>
 * </ul>
 * Both Red HIVE cells face the Audience (+X in FTC CAD / -Y in the [0, 144] visualizer frame),
 * and both Blue HIVE cells face the Scoring side (-X in FTC CAD / +Y in the [0, 144] frame).</p>
 */
public enum Team3HiveGoal {
    /** Red Alliance Far / Scoring Side HIVE Cell (AprilTags 30, 31, 32, 33; DECODE fallback 24). */
    RED_SCORING(
            "Red Scoring Hive",
            true,
            30, 33,
            false,
            59.26, 82.23, -90.0,
            Team3VisionConfiguration.DEFAULT_GOAL_DISTANCE_INCHES,
            Team3VisionConfiguration.DEFAULT_GOAL_LATERAL_OFFSET_INCHES,
            Team3VisionConfiguration.DEFAULT_GOAL_HEADING_OFFSET_DEGREES),

    /** Red Alliance Near / Audience Side HIVE Cell (AprilTags 34, 35, 36, 37). */
    RED_AUDIENCE(
            "Red Audience Hive",
            true,
            34, 37,
            true,
            59.26, 60.26, -90.0,
            Team3VisionConfiguration.DEFAULT_GOAL_DISTANCE_INCHES,
            Team3VisionConfiguration.DEFAULT_GOAL_LATERAL_OFFSET_INCHES,
            Team3VisionConfiguration.DEFAULT_GOAL_HEADING_OFFSET_DEGREES),

    /** Blue Alliance Near / Audience Side HIVE Cell (AprilTags 38, 39, 40, 41). */
    BLUE_AUDIENCE(
            "Blue Audience Hive",
            false,
            38, 41,
            false,
            84.76, 61.77, 90.0,
            Team3VisionConfiguration.DEFAULT_GOAL_DISTANCE_INCHES,
            Team3VisionConfiguration.DEFAULT_GOAL_LATERAL_OFFSET_INCHES,
            Team3VisionConfiguration.DEFAULT_GOAL_HEADING_OFFSET_DEGREES),

    /** Blue Alliance Far / Scoring Side HIVE Cell (AprilTags 42, 43, 44, 45; DECODE fallback 20). */
    BLUE_SCORING(
            "Blue Scoring Hive",
            false,
            42, 45,
            true,
            84.76, 83.74, 90.0,
            Team3VisionConfiguration.DEFAULT_GOAL_DISTANCE_INCHES,
            Team3VisionConfiguration.DEFAULT_GOAL_LATERAL_OFFSET_INCHES,
            Team3VisionConfiguration.DEFAULT_GOAL_HEADING_OFFSET_DEGREES);

    /**
     * Official BIOBUZZ horizontal tag offsets in inches along the goal face relative to the
     * center of the HIVE cell.
     */
    private static final double[] CLUSTER_OFFSETS_INCHES = {
            -6.50, -2.75, 2.75, 6.50
    };

    private final String displayName;
    private final boolean redAlliance;
    private final int firstTagId;
    private final int lastTagId;
    private final boolean leftToRightTagOrder;
    private final double goalFieldXInches;
    private final double goalFieldYInches;
    private final double outwardNormalDegrees;
    private final double targetDistanceInches;
    private final double targetLateralOffsetInches;
    private final double targetHeadingOffsetDegrees;

    Team3HiveGoal(String displayName,
                  boolean redAlliance,
                  int firstTagId,
                  int lastTagId,
                  boolean leftToRightTagOrder,
                  double goalFieldXInches,
                  double goalFieldYInches,
                  double outwardNormalDegrees,
                  double targetDistanceInches,
                  double targetLateralOffsetInches,
                  double targetHeadingOffsetDegrees) {
        this.displayName = displayName;
        this.redAlliance = redAlliance;
        this.firstTagId = firstTagId;
        this.lastTagId = lastTagId;
        this.leftToRightTagOrder = leftToRightTagOrder;
        this.goalFieldXInches = goalFieldXInches;
        this.goalFieldYInches = goalFieldYInches;
        this.outwardNormalDegrees = outwardNormalDegrees;
        this.targetDistanceInches = targetDistanceInches;
        this.targetLateralOffsetInches = targetLateralOffsetInches;
        this.targetHeadingOffsetDegrees = targetHeadingOffsetDegrees;
    }

    /** Returns the human-readable name of this HIVE goal for Driver Station telemetry. */
    public String getDisplayName() {
        return displayName;
    }

    /** Returns true if this is one of the 2 Red HIVE goals, or false if Blue. */
    public boolean isRedAlliance() {
        return redAlliance;
    }

    /** Returns the first AprilTag ID in this goal's 4-tag cluster. */
    public int getFirstTagId() {
        return firstTagId;
    }

    /** Returns the last AprilTag ID in this goal's 4-tag cluster. */
    public int getLastTagId() {
        return lastTagId;
    }

    /** Returns the goal center's X coordinate in the [0, 144] BIOBUZZ field frame. */
    public double getGoalFieldXInches() {
        return goalFieldXInches;
    }

    /** Returns the goal center's Y coordinate in the [0, 144] BIOBUZZ field frame. */
    public double getGoalFieldYInches() {
        return goalFieldYInches;
    }

    /**
     * Returns the outward normal direction (in degrees, in the [0, 144] field frame) pointing
     * straight out from the front face of this HIVE goal toward its shooting zone.
     */
    public double getOutwardNormalDegrees() {
        return outwardNormalDegrees;
    }

    /** Returns the desired standoff distance (in inches) from the goal center for shooting. */
    public double getTargetDistanceInches() {
        return targetDistanceInches;
    }

    /** Returns the desired lateral offset (in inches, positive = right) from the goal center. */
    public double getTargetLateralOffsetInches() {
        return targetLateralOffsetInches;
    }

    /** Returns the desired heading offset (in degrees, positive = CCW/left) when facing the goal. */
    public double getTargetHeadingOffsetDegrees() {
        return targetHeadingOffsetDegrees;
    }

    /**
     * Resolves an AprilTag ID to its corresponding {@link Team3HiveGoal}.
     *
     * @param tagId detected AprilTag ID (BIOBUZZ 30-45, or DECODE Goal 20/24)
     * @return matching {@link Team3HiveGoal}, or {@code null} if the tag is not a goal tag
     */
    public static Team3HiveGoal fromTagId(int tagId) {
        for (Team3HiveGoal goal : values()) {
            if (tagId >= goal.firstTagId && tagId <= goal.lastTagId) {
                return goal;
            }
        }
        if (tagId == 24) {
            return RED_SCORING;
        }
        if (tagId == 20) {
            return BLUE_SCORING;
        }
        return null;
    }

    /**
     * Returns whether the given AprilTag ID belongs to any of the 4 HIVE goals (or DECODE goal tags).
     */
    public static boolean isGoalTag(int tagId) {
        return fromTagId(tagId) != null;
    }

    /**
     * Returns the horizontal offset (in inches, positive right when facing the front of the goal)
     * of a specific AprilTag from the true center of its HIVE goal, matching {@code biobuzz-2026.json}.
     *
     * @param tagId AprilTag ID
     * @return lateral offset of the tag relative to the HIVE cell center in inches (+right when facing goal)
     */
    public static double getTagClusterLateralOffsetInches(int tagId) {
        Team3HiveGoal goal = fromTagId(tagId);
        if (goal != null && tagId >= 30 && tagId <= 45) {
            int clusterIndex = (tagId - 30) % 4;
            double rawOffset = CLUSTER_OFFSETS_INCHES[clusterIndex];
            return goal.leftToRightTagOrder ? rawOffset : -rawOffset;
        }
        return 0.0;
    }
}

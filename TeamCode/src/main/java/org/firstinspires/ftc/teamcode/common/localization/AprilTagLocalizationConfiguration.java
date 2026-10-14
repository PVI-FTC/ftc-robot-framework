package org.firstinspires.ftc.teamcode.common.localization;

import java.util.Arrays;

/** Immutable neutral rules for accepting fixed AprilTags as field-pose references. */
public final class AprilTagLocalizationConfiguration {
    public static final long DEFAULT_MAX_CANDIDATE_AGE_NANOS = 250_000_000L;
    public static final String FTC_FIELD_FRAME_NAME =
            "Official FTC field frame: center origin, +X right from Red Wall, +Y away, +Z up";

    private final String configurationName;
    private final int[] fixedTagIds;
    private final String fieldFrameName;
    private final long maxCandidateAgeNanos;

    public AprilTagLocalizationConfiguration(String configurationName, int[] fixedTagIds,
                                             String fieldFrameName,
                                             long maxCandidateAgeNanos) {
        if (configurationName == null || configurationName.isEmpty()) {
            throw new IllegalArgumentException("Localization configuration needs a name.");
        }
        if (fixedTagIds == null || fixedTagIds.length == 0) {
            throw new IllegalArgumentException(
                    "Localization configuration needs at least one fixed tag ID.");
        }
        if (fieldFrameName == null || fieldFrameName.isEmpty()) {
            throw new IllegalArgumentException(
                    "Localization configuration needs a field-frame name.");
        }
        if (maxCandidateAgeNanos <= 0) {
            throw new IllegalArgumentException(
                    "Maximum candidate age must be positive.");
        }

        int[] copiedTagIds = Arrays.copyOf(fixedTagIds, fixedTagIds.length);
        validateTagIds(copiedTagIds);

        this.configurationName = configurationName;
        this.fixedTagIds = copiedTagIds;
        this.fieldFrameName = fieldFrameName;
        this.maxCandidateAgeNanos = maxCandidateAgeNanos;
    }

    /** Returns the reviewed DECODE fixed-GOAL localization configuration. */
    public static AprilTagLocalizationConfiguration decodeGoalTags() {
        return new AprilTagLocalizationConfiguration(
                "DECODE fixed GOAL tags",
                new int[] {20, 24},
                FTC_FIELD_FRAME_NAME,
                DEFAULT_MAX_CANDIDATE_AGE_NANOS);
    }

    private static void validateTagIds(int[] tagIds) {
        for (int index = 0; index < tagIds.length; index++) {
            if (tagIds[index] < 0) {
                throw new IllegalArgumentException("Fixed AprilTag IDs cannot be negative.");
            }
            for (int earlier = 0; earlier < index; earlier++) {
                if (tagIds[earlier] == tagIds[index]) {
                    throw new IllegalArgumentException("Fixed AprilTag IDs cannot be duplicated.");
                }
            }
        }
    }

    public String getConfigurationName() { return configurationName; }

    /** Returns a defensive copy so callers cannot change the accepted tag IDs. */
    public int[] getFixedTagIds() {
        return Arrays.copyOf(fixedTagIds, fixedTagIds.length);
    }

    public boolean allowsTagId(int tagId) {
        for (int fixedTagId : fixedTagIds) {
            if (fixedTagId == tagId) {
                return true;
            }
        }
        return false;
    }

    public String getFieldFrameName() { return fieldFrameName; }
    public long getMaxCandidateAgeNanos() { return maxCandidateAgeNanos; }
}

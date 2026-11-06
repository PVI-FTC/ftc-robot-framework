package org.firstinspires.ftc.teamcode.KimCode.ClaudeKimCode;

/**
 * Decides which CELL of a HIVE is raised, from how high its tags appear to be.
 *
 * <p>Each CELL has its own tag IDs, and a raised CELL's tags sit about 13 inches higher than a
 * lowered CELL's. The robot knows how far away a CELL is from its Pinpoint position, so the angle
 * up to a tag gives the tag's height. One reading can be wrong, so readings count as votes, and an
 * answer needs a clear lead.</p>
 *
 * <p>This class uses no hardware, so it can be checked without a robot.</p>
 */
final class HiveCellFinder {
    enum Answer {
        /** Not enough readings yet, or they disagree. */
        NOT_SURE,
        /** The CELL raised at the start of a match is still raised. */
        START_TILT,
        /** The HIVE has tipped, so the CELL lowered at the start is now raised. */
        TIPPED
    }

    /** How far ahead one answer must be. */
    private static final int VOTE_LEAD_NEEDED = 5;
    /** Below this, a tiny angle error makes a huge distance error. */
    private static final double MIN_ANGLE_FOR_DISTANCE_DEGREES = 5.0;
    /** Halfway between a raised tag and a lowered tag. */
    private static final double RAISED_IF_ABOVE_INCHES =
            (HiveField.RAISED_TAG_HEIGHT_INCHES + HiveField.LOWERED_TAG_HEIGHT_INCHES) / 2.0;

    private int startTiltVotes;
    private int tippedVotes;

    /** Works out a tag's height from its distance and how far above the crosshair it appears. */
    static double tagHeightInches(double horizontalDistanceInches, double targetYDegrees) {
        double angleUpRadians = Math.toRadians(HiveField.CAMERA_TILT_DEGREES + targetYDegrees);
        return HiveField.CAMERA_HEIGHT_INCHES
                + horizontalDistanceInches * Math.tan(angleUpRadians);
    }

    /**
     * Works out the level distance from the camera to a raised CELL's tags, using how far above
     * the crosshair they appear. Returns NaN if the angle is too low to give a useful distance.
     */
    static double distanceToRaisedTagInches(double targetYDegrees) {
        double angleUpDegrees = HiveField.CAMERA_TILT_DEGREES + targetYDegrees;
        if (angleUpDegrees < MIN_ANGLE_FOR_DISTANCE_DEGREES) {
            return Double.NaN;
        }
        return (HiveField.RAISED_TAG_HEIGHT_INCHES - HiveField.CAMERA_HEIGHT_INCHES)
                / Math.tan(Math.toRadians(angleUpDegrees));
    }

    /** Adds one reading of a CELL's tags. */
    void addReading(boolean isStartRaisedCell, double horizontalDistanceInches,
            double targetYDegrees) {
        double height = tagHeightInches(horizontalDistanceInches, targetYDegrees);
        boolean looksRaised = height >= RAISED_IF_ABOVE_INCHES;
        // The start-raised CELL looking high and the other CELL looking low mean the same thing.
        if (looksRaised == isStartRaisedCell) {
            startTiltVotes++;
        } else {
            tippedVotes++;
        }
    }

    Answer answer() {
        if (startTiltVotes - tippedVotes >= VOTE_LEAD_NEEDED) {
            return Answer.START_TILT;
        }
        if (tippedVotes - startTiltVotes >= VOTE_LEAD_NEEDED) {
            return Answer.TIPPED;
        }
        return Answer.NOT_SURE;
    }

    /** Forgets all votes, so a new look at the HIVE starts fresh. */
    void reset() {
        startTiltVotes = 0;
        tippedVotes = 0;
    }

    int startTiltVotes() {
        return startTiltVotes;
    }

    int tippedVotes() {
        return tippedVotes;
    }
}

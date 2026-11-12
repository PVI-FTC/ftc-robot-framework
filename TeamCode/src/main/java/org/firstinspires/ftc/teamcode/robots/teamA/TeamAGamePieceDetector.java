package org.firstinspires.ftc.teamcode.robots.teamA;

/** Debounces close-range HSV readings into one event for each passing game piece. */
final class TeamAGamePieceDetector {
    // Tune these values from telemetry with real Pollen and both red and blue Nectar.
    static final double MAX_GAME_PIECE_DISTANCE_CM = 5.0;
    static final float MINIMUM_SATURATION = 0.30f;
    static final float MINIMUM_VALUE = 0.03f;
    static final float YELLOW_HUE_MIN_DEGREES = 35.0f;
    static final float YELLOW_HUE_MAX_DEGREES = 80.0f;
    static final float RED_HUE_MAX_DEGREES = 30.0f;
    static final float RED_HUE_MIN_DEGREES = 330.0f;
    static final float BLUE_HUE_MIN_DEGREES = 180.0f;
    static final float BLUE_HUE_MAX_DEGREES = 260.0f;
    static final int REQUIRED_MATCHING_SAMPLES = 3;
    static final int REQUIRED_CLEAR_SAMPLES = 3;

    enum GamePiece {
        NONE,
        POLLEN,
        NECTAR,
        UNKNOWN
    }

    private GamePiece candidate = GamePiece.NONE;
    private GamePiece currentReading = GamePiece.NONE;
    private int matchingSamples;
    private int clearSamples = REQUIRED_CLEAR_SAMPLES;
    private boolean armed = true;

    /** Returns one non-NONE event after a stable new game piece enters the sensor. */
    GamePiece update(TeamAMechanismHardware.ColorReading reading) {
        currentReading = classify(reading);

        if (currentReading == GamePiece.NONE) {
            clearSamples++;
            candidate = GamePiece.NONE;
            matchingSamples = 0;
            if (clearSamples >= REQUIRED_CLEAR_SAMPLES) {
                armed = true;
            }
            return GamePiece.NONE;
        }

        clearSamples = 0;
        if (!armed || currentReading == GamePiece.UNKNOWN) {
            candidate = GamePiece.NONE;
            matchingSamples = 0;
            return GamePiece.NONE;
        }

        if (currentReading != candidate) {
            candidate = currentReading;
            matchingSamples = 1;
        } else {
            matchingSamples++;
        }

        if (matchingSamples < REQUIRED_MATCHING_SAMPLES) {
            return GamePiece.NONE;
        }

        armed = false;
        return currentReading;
    }

    String getCurrentReadingName() {
        return currentReading.name();
    }

    private GamePiece classify(TeamAMechanismHardware.ColorReading reading) {
        if (reading == null || !reading.isAvailable()
                || !Double.isFinite(reading.getDistanceCm())
                || reading.getDistanceCm() > MAX_GAME_PIECE_DISTANCE_CM) {
            return GamePiece.NONE;
        }

        if (reading.getSaturation() < MINIMUM_SATURATION
                || reading.getValue() < MINIMUM_VALUE) {
            return GamePiece.UNKNOWN;
        }

        float hue = reading.getHueDegrees();
        if (hue >= YELLOW_HUE_MIN_DEGREES && hue <= YELLOW_HUE_MAX_DEGREES) {
            return GamePiece.POLLEN;
        }

        boolean red = hue <= RED_HUE_MAX_DEGREES || hue >= RED_HUE_MIN_DEGREES;
        boolean blue = hue >= BLUE_HUE_MIN_DEGREES && hue <= BLUE_HUE_MAX_DEGREES;
        return red || blue ? GamePiece.NECTAR : GamePiece.UNKNOWN;
    }
}

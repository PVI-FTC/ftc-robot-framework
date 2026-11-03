package pvi.harness.viewer;

import java.util.List;

import pvi.harness.runtime.RunRecording;

/**
 * The replay controls' brain: which moment of the recording is shown, whether it is playing,
 * and how fast. No window and no clock here: the window tells it how much real time passed
 * ({@link #advance}), so tests can drive it exactly.
 *
 * <p>The timeline is the recording's "seconds since INIT", so INIT is replayed too. At 1x speed
 * one second of real time shows one second of the run.</p>
 */
public final class Playback {
    /** The speeds the viewer offers, slowest first. */
    public static final double[] SPEEDS = {0.25, 0.5, 1.0, 2.0, 4.0};
    /** Which entry of {@link #SPEEDS} is normal speed (1x). */
    private static final int NORMAL_SPEED = 2;

    /** Seconds since INIT of each loop entry, oldest first. */
    private final double[] timesSeconds;
    /** The loop entry shown. */
    private int index;
    /** Where the playhead is, in seconds since INIT (between loop times while playing). */
    private double playheadSeconds;
    /** True while playing. */
    private boolean playing;
    /** Index into {@link #SPEEDS}. */
    private int speedIndex = NORMAL_SPEED;

    /** Starts paused at the first loop, at normal speed. */
    public Playback(RunRecording recording) {
        List<RunRecording.LoopRecord> loops = recording.loops();
        if (loops.isEmpty()) {
            throw new IllegalArgumentException("The recording has no loops to play.");
        }
        timesSeconds = new double[loops.size()];
        for (int i = 0; i < loops.size(); i++) {
            timesSeconds[i] = loops.get(i).sinceInitSeconds();
        }
        playheadSeconds = timesSeconds[0];
    }

    // ---- What is shown ----------------------------------------------------------------------

    /** The loop entry to show. */
    public int index() { return index; }
    /** Number of loop entries. */
    public int size() { return timesSeconds.length; }
    /** True while playing. */
    public boolean isPlaying() { return playing; }
    /** Current speed, e.g. 1.0 for real time. */
    public double speed() { return SPEEDS[speedIndex]; }
    /** True when showing the last loop entry. */
    public boolean atEnd() { return index == timesSeconds.length - 1; }

    // ---- Controls -----------------------------------------------------------------------------

    /** Starts playing. From the end, starts again from the beginning. */
    public void play() {
        if (atEnd()) {
            seek(0);
        }
        playing = true;
    }

    /** Stops playing; the shown moment stays. */
    public void pause() {
        playing = false;
    }

    /** Play if paused, pause if playing (the space bar). */
    public void togglePlay() {
        if (playing) {
            pause();
        } else {
            play();
        }
    }

    /** One speed step faster, up to the fastest. */
    public void faster() {
        speedIndex = Math.min(SPEEDS.length - 1, speedIndex + 1);
    }

    /** One speed step slower, down to the slowest. */
    public void slower() {
        speedIndex = Math.max(0, speedIndex - 1);
    }

    /** Picks a speed from {@link #SPEEDS} by its position in that list. */
    public void setSpeedIndex(int speedIndex) {
        if (speedIndex < 0 || speedIndex >= SPEEDS.length) {
            throw new IllegalArgumentException("Speed index " + speedIndex + " must be 0 to " + (SPEEDS.length - 1));
        }
        this.speedIndex = speedIndex;
    }

    /** Index into {@link #SPEEDS} of the current speed. */
    public int speedIndex() {
        return speedIndex;
    }

    /** Shows loop entry {@code newIndex} (kept inside the recording). Keeps playing if it was. */
    public void seek(int newIndex) {
        index = Math.max(0, Math.min(timesSeconds.length - 1, newIndex));
        playheadSeconds = timesSeconds[index];
    }

    /** Pauses and moves by whole loop entries (the arrow keys): +1 is the next one. */
    public void step(int loops) {
        pause();
        seek(index + loops);
    }

    /** Pauses and moves by about {@code seconds} of run time (Shift + arrow keys). */
    public void jumpSeconds(double seconds) {
        pause();
        seek(indexAt(timesSeconds[index] + seconds));
    }

    /**
     * Moves the playhead forward by {@code realSeconds} of real time times the speed. Stops at
     * the end. Does nothing while paused.
     *
     * @return true if the shown loop entry changed (the window then redraws)
     */
    public boolean advance(double realSeconds) {
        if (!playing || realSeconds <= 0) {
            return false;
        }
        int before = index;
        playheadSeconds += realSeconds * speed();
        index = indexAt(playheadSeconds);
        if (atEnd()) {
            playing = false;
            playheadSeconds = timesSeconds[index];
        }
        return index != before;
    }

    /** The last loop entry at or before {@code seconds} since INIT (the first if before it). */
    int indexAt(double seconds) {
        int low = 0;
        int high = timesSeconds.length - 1;
        if (seconds <= timesSeconds[0]) {
            return 0;
        }
        // Binary search: find the last time that is <= seconds (1e-9 absorbs rounding).
        while (low < high) {
            int middle = (low + high + 1) / 2;
            if (timesSeconds[middle] <= seconds + 1e-9) {
                low = middle;
            } else {
                high = middle - 1;
            }
        }
        return low;
    }
}

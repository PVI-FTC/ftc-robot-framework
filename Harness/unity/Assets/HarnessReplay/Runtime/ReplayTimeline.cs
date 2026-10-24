using System;
using System.Collections.Generic;

namespace PviHarness.Replay
{
    /// <summary>
    /// The replay controls' brain, the same rules as the Swing viewer's Playback class: which
    /// moment is shown, whether it is playing, and how fast. No Unity objects and no clock: the
    /// controller tells it how much real time passed (<see cref="Advance"/>), so tests can drive
    /// it exactly.
    ///
    /// <para>The timeline is the recording's "seconds since INIT", so INIT is replayed too. At 1x,
    /// one second of real time shows one second of the run. Between two recorded loops (20 ms
    /// apart) <see cref="PoseAtPlayhead"/> blends the two poses, so the robot moves smoothly at
    /// any frame rate.</para>
    /// </summary>
    public sealed class ReplayTimeline
    {
        /// <summary>The speeds offered, slowest first.</summary>
        public static readonly float[] Speeds = { 0.25f, 0.5f, 1f, 2f, 4f };
        private const int NormalSpeed = 2;

        private readonly List<LoopData> loops;
        private readonly List<double> times = new List<double>();
        private int speedIndex = NormalSpeed;

        /// <summary>Starts paused at the first loop, at normal speed.</summary>
        public ReplayTimeline(RunRecordingData recording)
        {
            if (recording == null || recording.loops == null || recording.loops.Count == 0)
            {
                throw new ArgumentException("The recording has no loops to play.");
            }
            loops = recording.loops;
            Sync();
            PlayheadSeconds = times[0];
        }

        /// <summary>The last loop at or before the playhead: the one whose telemetry, motors and gamepads are shown.</summary>
        public int Index { get; private set; }
        /// <summary>Number of loops.</summary>
        public int Count => times.Count;
        /// <summary>The playhead, in seconds since INIT (between loop times while playing).</summary>
        public double PlayheadSeconds { get; private set; }
        /// <summary>True while playing.</summary>
        public bool IsPlaying { get; private set; }
        /// <summary>The current speed, e.g. 1 for real time.</summary>
        public float Speed => Speeds[speedIndex];
        /// <summary>Position of <see cref="Speed"/> in <see cref="Speeds"/>.</summary>
        public int SpeedIndex => speedIndex;
        /// <summary>True when showing the last loop.</summary>
        public bool AtEnd => Index == times.Count - 1;
        /// <summary>The loop shown.</summary>
        public LoopData Current => loops[Index];
        /// <summary>Seconds since INIT of the last loop.</summary>
        public double EndSeconds => times[times.Count - 1];

        /// <summary>
        /// Picks up loops added to the recording since the last call (live play adds one per
        /// step). The shown loop stays where it is; call <see cref="Seek"/> to follow the newest.
        /// </summary>
        public void Sync()
        {
            for (int i = times.Count; i < loops.Count; i++)
            {
                times.Add(loops[i].sinceInitSeconds);
            }
        }

        /// <summary>Starts playing. From the end, starts again from the beginning.</summary>
        public void Play()
        {
            if (AtEnd)
            {
                Seek(0);
            }
            IsPlaying = true;
        }

        /// <summary>Stops playing; the shown moment stays.</summary>
        public void Pause()
        {
            IsPlaying = false;
        }

        /// <summary>Play if paused, pause if playing (the space bar).</summary>
        public void TogglePlay()
        {
            if (IsPlaying)
            {
                Pause();
            }
            else
            {
                Play();
            }
        }

        /// <summary>One speed step faster, up to the fastest.</summary>
        public void Faster()
        {
            speedIndex = Math.Min(Speeds.Length - 1, speedIndex + 1);
        }

        /// <summary>One speed step slower, down to the slowest.</summary>
        public void Slower()
        {
            speedIndex = Math.Max(0, speedIndex - 1);
        }

        /// <summary>Picks a speed by its position in <see cref="Speeds"/> (the speed dropdown).</summary>
        public void SetSpeedIndex(int index)
        {
            if (index < 0 || index >= Speeds.Length)
            {
                throw new ArgumentOutOfRangeException(nameof(index), "Speed index must be 0 to " + (Speeds.Length - 1));
            }
            speedIndex = index;
        }

        /// <summary>Shows loop <paramref name="index"/> (kept inside the recording). Keeps playing if it was.</summary>
        public void Seek(int index)
        {
            Index = Math.Max(0, Math.Min(times.Count - 1, index));
            PlayheadSeconds = times[Index];
        }

        /// <summary>Moves the playhead to a time since INIT (kept inside the recording), e.g. from the slider.</summary>
        public void SeekSeconds(double seconds)
        {
            PlayheadSeconds = Math.Max(times[0], Math.Min(EndSeconds, seconds));
            Index = IndexAt(PlayheadSeconds);
        }

        /// <summary>Pauses and moves by whole loops (the arrow keys): +1 is the next one.</summary>
        public void Step(int loopCount)
        {
            Pause();
            Seek(Index + loopCount);
        }

        /// <summary>Pauses and moves by about this many seconds of run time (Shift + arrow keys).</summary>
        public void JumpSeconds(double seconds)
        {
            Pause();
            Seek(IndexAt(times[Index] + seconds));
        }

        /// <summary>
        /// Moves the playhead by real time times the speed. Stops at the end. Does nothing while paused.
        /// </summary>
        /// <returns>True if the shown loop changed.</returns>
        public bool Advance(double realSeconds)
        {
            if (!IsPlaying || realSeconds <= 0)
            {
                return false;
            }
            int before = Index;
            PlayheadSeconds += realSeconds * Speed;
            Index = IndexAt(PlayheadSeconds);
            if (AtEnd)
            {
                IsPlaying = false;
                PlayheadSeconds = times[Index];
            }
            return Index != before;
        }

        /// <summary>
        /// The robot's pose at the playhead, blended between the loop shown and the next one, or
        /// null if position wasn't tracked there. FTC units, like the recording.
        /// </summary>
        public PoseData PoseAtPlayhead()
        {
            PoseData here = loops[Index].pose;
            if (here == null || AtEnd)
            {
                return here;
            }
            PoseData next = loops[Index + 1].pose;
            double span = times[Index + 1] - times[Index];
            if (next == null || span <= 0)
            {
                return here;
            }
            double blend = Math.Max(0, Math.Min(1, (PlayheadSeconds - times[Index]) / span));
            return Blend(here, next, blend);
        }

        /// <summary>
        /// A pose part of the way from <paramref name="from"/> to <paramref name="to"/> (0 = from,
        /// 1 = to). The heading turns the short way round, so 170 deg to -170 deg passes 180, not 0.
        /// </summary>
        public static PoseData Blend(PoseData from, PoseData to, double amount)
        {
            double turn = Math.IEEERemainder(to.headingRadians - from.headingRadians, 2 * Math.PI);
            return new PoseData
            {
                xInches = from.xInches + (to.xInches - from.xInches) * amount,
                yInches = from.yInches + (to.yInches - from.yInches) * amount,
                headingRadians = from.headingRadians + turn * amount,
            };
        }

        /// <summary>The last loop at or before <paramref name="seconds"/> since INIT (the first if before it).</summary>
        public int IndexAt(double seconds)
        {
            if (seconds <= times[0])
            {
                return 0;
            }
            // Binary search for the last time that is <= seconds (1e-9 absorbs rounding).
            int low = 0;
            int high = times.Count - 1;
            while (low < high)
            {
                int middle = (low + high + 1) / 2;
                if (times[middle] <= seconds + 1e-9)
                {
                    low = middle;
                }
                else
                {
                    high = middle - 1;
                }
            }
            return low;
        }
    }
}

using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using System.Linq;
using UnityEngine;

namespace PviHarness.Replay
{
    /// <summary>
    /// Measures how smoothly the window draws, for checking that a heavy view (like the full
    /// competition-hall crowd) stays above 30 frames per second.
    ///
    /// <para>Only used when the app is started with <c>-fpslog &lt;file&gt;</c>: it waits
    /// <see cref="WarmUpSeconds"/> for loading to settle, records every frame for
    /// <c>-fpsseconds</c> (default 10), writes the result to the file and closes the app. Without
    /// <c>-fpslog</c> nothing is added.</para>
    /// </summary>
    public sealed class FrameRateLog : MonoBehaviour
    {
        /// <summary>Seconds to wait before measuring, so loading and camera moves don't count.</summary>
        public const float WarmUpSeconds = 5f;
        /// <summary>Seconds measured when <c>-fpsseconds</c> isn't given.</summary>
        public const float DefaultSeconds = 10f;

        private string file;
        private float seconds;
        private float waited;
        private float measured;
        private readonly List<float> frames = new List<float>();

        /// <summary>Frame-rate numbers for a list of frame times.</summary>
        public readonly struct Summary
        {
            public Summary(int frames, double averageFps, double minimumFps, double onePercentLowFps)
            {
                Frames = frames;
                AverageFps = averageFps;
                MinimumFps = minimumFps;
                OnePercentLowFps = onePercentLowFps;
            }

            /// <summary>How many frames were measured.</summary>
            public int Frames { get; }
            /// <summary>Frames drawn divided by the time they took.</summary>
            public double AverageFps { get; }
            /// <summary>The slowest single frame, as frames per second.</summary>
            public double MinimumFps { get; }
            /// <summary>The average of the slowest 1% of frames, as frames per second (a steadier "worst case").</summary>
            public double OnePercentLowFps { get; }
        }

        /// <summary>Works out the numbers for these frame times (in seconds).</summary>
        public static Summary Summarize(IReadOnlyList<float> frameSeconds)
        {
            if (frameSeconds == null || frameSeconds.Count == 0)
            {
                return new Summary(0, 0, 0, 0);
            }
            double total = frameSeconds.Sum(f => (double)f);
            List<float> slowestFirst = frameSeconds.OrderByDescending(f => f).ToList();
            int slowCount = Math.Max(1, slowestFirst.Count / 100);
            double slowAverage = slowestFirst.Take(slowCount).Average(f => (double)f);
            return new Summary(frameSeconds.Count, frameSeconds.Count / total, 1.0 / slowestFirst[0], 1.0 / slowAverage);
        }

        /// <summary>
        /// Adds the measurer to <paramref name="host"/> if <paramref name="args"/> has <c>-fpslog &lt;file&gt;</c>
        /// (and optionally <c>-fpsseconds &lt;n&gt;</c>).
        /// </summary>
        public static void AttachIfAsked(GameObject host, string[] args)
        {
            string path = null;
            float measure = DefaultSeconds;
            for (int i = 0; i < args.Length - 1; i++)
            {
                if (string.Equals(args[i], "-fpslog", StringComparison.OrdinalIgnoreCase))
                {
                    path = args[i + 1];
                }
                else if (string.Equals(args[i], "-fpsseconds", StringComparison.OrdinalIgnoreCase)
                    && float.TryParse(args[i + 1], NumberStyles.Float, CultureInfo.InvariantCulture, out float s) && s > 0)
                {
                    measure = s;
                }
            }
            if (path != null)
            {
                FrameRateLog log = host.AddComponent<FrameRateLog>();
                log.file = path;
                log.seconds = measure;
            }
        }

        private void Update()
        {
            float dt = Time.unscaledDeltaTime;
            if (waited < WarmUpSeconds)
            {
                waited += dt;
                return;
            }
            frames.Add(dt);
            measured += dt;
            if (measured < seconds)
            {
                return;
            }
            Summary s = Summarize(frames);
            string text = string.Format(CultureInfo.InvariantCulture,
                "frames={0} seconds={1:0.00} averageFps={2:0.0} minimumFps={3:0.0} onePercentLowFps={4:0.0}\n"
                + "gpu={5} screen={6}x{7} vSyncCount={8} quality={9}\n",
                s.Frames, measured, s.AverageFps, s.MinimumFps, s.OnePercentLowFps,
                SystemInfo.graphicsDeviceName, Screen.width, Screen.height, QualitySettings.vSyncCount,
                QualitySettings.names[QualitySettings.GetQualityLevel()]);
            File.WriteAllText(file, text);
            enabled = false;
            Application.Quit();
        }
    }
}

using NUnit.Framework;
using UnityEngine;

namespace VI_Arena.Tests
{
    /// <summary>The <c>-fpslog</c> frame-rate measurer used to check heavy views (like the hall crowd).</summary>
    public class FrameRateLogTests
    {
        /// <summary>
        /// WHAT: The average is frames divided by time, the minimum is the slowest frame, and the
        /// "1% low" averages the slowest 1% of frames.
        /// </summary>
        [Test]
        public void SummaryUsesAverageSlowestAndOnePercentLow()
        {
            var frames = new float[200];
            for (int i = 0; i < frames.Length; i++)
            {
                frames[i] = 1f / 50f;
            }
            frames[10] = 1f / 10f;
            frames[20] = 1f / 20f;

            FrameRateLog.Summary s = FrameRateLog.Summarize(frames);

            Assert.AreEqual(200, s.Frames);
            Assert.AreEqual(200 / (198 / 50.0 + 0.1 + 0.05), s.AverageFps, 1e-3);
            Assert.AreEqual(10, s.MinimumFps, 1e-3, "the slowest frame");
            Assert.AreEqual(1 / ((0.1 + 0.05) / 2), s.OnePercentLowFps, 1e-3, "the 2 slowest of 200 frames");
        }

        /// <summary>WHAT: The measurer is added only with "-fpslog &lt;file&gt;" on the command line.</summary>
        [Test]
        public void AddedOnlyWhenAskedFor()
        {
            var host = new GameObject("fps host");
            try
            {
                FrameRateLog.AttachIfAsked(host, new[] { "VI_Arena.exe", "-setting", "competition-hall" });
                Assert.IsNull(host.GetComponent<FrameRateLog>());

                FrameRateLog.AttachIfAsked(host, new[] { "VI_Arena.exe", "-fpslog", "fps.txt", "-fpsseconds", "12" });
                Assert.IsNotNull(host.GetComponent<FrameRateLog>());
            }
            finally
            {
                Object.DestroyImmediate(host);
            }
        }
    }
}

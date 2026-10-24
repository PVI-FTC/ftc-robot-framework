using System.IO;
using NUnit.Framework;

namespace PviHarness.Replay.Tests
{
    /// <summary>Playback timing: the same rules as the Swing viewer's Playback, plus smooth blending.</summary>
    public class ReplayTimelineTests
    {
        private static ReplayTimeline SampleTimeline()
        {
            return new ReplayTimeline(RecordingLoader.Load(RecordingLoaderTests.SamplePath));
        }

        /// <summary>
        /// WHAT: At 1x, half a second of real time lands on the loop half a second after INIT.
        /// HOW: The sample has one loop every 20 ms, so that is loop 25.
        /// WHY: The replay must run at the real pace, or a quick move would look slow.
        /// </summary>
        [Test]
        public void NormalSpeedFollowsRealTime()
        {
            ReplayTimeline timeline = SampleTimeline();

            timeline.Play();
            for (int i = 0; i < 10; i++)
            {
                timeline.Advance(0.05);
            }

            Assert.AreEqual(25, timeline.Index);
            Assert.IsTrue(timeline.IsPlaying);
        }

        /// <summary>WHAT: 2x covers twice the run; paused, nothing moves.</summary>
        [Test]
        public void SpeedScalesAndPauseStops()
        {
            ReplayTimeline timeline = SampleTimeline();
            timeline.Faster();
            timeline.Play();

            timeline.Advance(0.5);
            Assert.AreEqual(50, timeline.Index, "0.5 s at 2x = 1 s of run = 50 loops");

            timeline.Pause();
            Assert.IsFalse(timeline.Advance(1));
            Assert.AreEqual(50, timeline.Index);
        }

        /// <summary>WHAT: Playback stops at the end; play again starts over.</summary>
        [Test]
        public void StopsAtTheEndAndPlayStartsOver()
        {
            ReplayTimeline timeline = SampleTimeline();
            timeline.Play();

            timeline.Advance(100);
            Assert.IsTrue(timeline.AtEnd);
            Assert.IsFalse(timeline.IsPlaying);

            timeline.Play();
            Assert.AreEqual(0, timeline.Index);
        }

        /// <summary>WHAT: Steps and jumps pause, move the right amount and stay inside the recording.</summary>
        [Test]
        public void StepsAndJumpsStayInside()
        {
            ReplayTimeline timeline = SampleTimeline();
            timeline.Play();

            timeline.Step(1);
            Assert.AreEqual(1, timeline.Index);
            Assert.IsFalse(timeline.IsPlaying);
            timeline.JumpSeconds(1);
            Assert.AreEqual(51, timeline.Index);
            timeline.Step(-1000);
            Assert.AreEqual(0, timeline.Index);
            timeline.Step(1000);
            Assert.AreEqual(timeline.Count - 1, timeline.Index);
        }

        /// <summary>
        /// WHAT: Halfway between two loops, the robot is halfway between their poses.
        /// HOW: In the sample the robot turns 3.6 deg per 20 ms loop during the turn, so 10 ms
        /// after loop 60 the heading is halfway between loops 60 and 61.
        /// WHY: The screen draws far more often than every 20 ms; without blending the robot jumps.
        /// </summary>
        [Test]
        public void PoseIsBlendedBetweenLoops()
        {
            ReplayTimeline timeline = SampleTimeline();
            RunRecordingData recording = RecordingLoader.Load(RecordingLoaderTests.SamplePath);
            double h60 = recording.loops[60].pose.headingRadians;
            double h61 = recording.loops[61].pose.headingRadians;
            Assert.AreNotEqual(h60, h61, "the sample should be turning at loop 60");

            timeline.SeekSeconds(recording.loops[60].sinceInitSeconds + 0.010);

            Assert.AreEqual(60, timeline.Index);
            Assert.AreEqual((h60 + h61) / 2, timeline.PoseAtPlayhead().headingRadians, 1e-9);
        }

        /// <summary>
        /// WHAT: Blending turns the short way round: from 170 deg to -170 deg it passes 180 deg, not 0.
        /// WHY: Headings wrap at ±180 deg; a naive average would spin the robot the long way round.
        /// </summary>
        [Test]
        public void BlendTurnsTheShortWayRound()
        {
            var from = new PoseData { headingRadians = 170 * System.Math.PI / 180 };
            var to = new PoseData { headingRadians = -170 * System.Math.PI / 180 };

            double halfway = ReplayTimeline.Blend(from, to, 0.5).headingRadians * 180 / System.Math.PI;

            Assert.AreEqual(180, System.Math.Abs(halfway), 1e-9);
        }

        /// <summary>
        /// WHAT: The "done" check for Stage U1: Team A's autonomous ends about 0.24 m (9.4 in) forward in Unity.
        /// HOW: Uses Harness/build/recordings/viewer-teamA-autonomous.json, which the harness tests
        /// (or the Swing viewer) write; skipped if it isn't there yet.
        /// </summary>
        [Test]
        public void TeamAAutonomousEndsAboutQuarterMeterForward()
        {
            string path = Path.Combine(RecordingLoader.DefaultFolder, "viewer-teamA-autonomous.json");
            if (!File.Exists(path))
            {
                Assert.Ignore("Run the harness tests first (.\\gradlew.bat test) to create " + path);
            }
            ReplayTimeline timeline = new ReplayTimeline(RecordingLoader.Load(path));

            timeline.Seek(timeline.Count - 1);
            PoseData end = timeline.PoseAtPlayhead();

            Assert.AreEqual(0.239f, FtcToUnity.Position(end.xInches, end.yInches).x, 0.013f);
            Assert.AreEqual(0f, FtcToUnity.Position(end.xInches, end.yInches).z, 1e-5f);
        }
    }
}

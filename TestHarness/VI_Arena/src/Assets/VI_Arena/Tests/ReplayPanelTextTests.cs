using System.Collections.Generic;
using NUnit.Framework;

namespace VI_Arena.Tests
{
    /// <summary>The side panel's text for one moment of a recording.</summary>
    public class ReplayPanelTextTests
    {
        /// <summary>
        /// WHAT: During the turn, the panel shows the OpMode, stage, the right stick held right and
        /// the turning motor powers.
        /// HOW: Loop 60 of the sample is in the middle of the 0.5 s full-right-stick turn.
        /// </summary>
        [Test]
        public void PanelShowsTheChosenMoment()
        {
            RunRecordingData recording = RecordingLoader.Load(RecordingLoaderTests.SamplePath);

            List<string> lines = ReplayPanelText.Describe(recording, 60);

            Assert.AreEqual("TeamATeleOp   TELEOP   robot: teamA", lines[0]);
            StringAssert.StartsWith("Stage: LOOP", lines[1]);
            CollectionAssert.Contains(lines, "Driver   R(1.00,0.00)");
            CollectionAssert.Contains(lines, "Operator -");
            CollectionAssert.Contains(lines, "  frontLeft      1.00");
            CollectionAssert.Contains(lines, "  frontRight    -1.00");
        }

        /// <summary>
        /// WHAT: The Driver Station screen is the one for that moment: at INIT, the INIT message.
        /// WHY: Moving the slider back must show what the Driver Station showed back then.
        /// </summary>
        [Test]
        public void PanelShowsTheScreenOfThatMoment()
        {
            RunRecordingData recording = RecordingLoader.Load(RecordingLoaderTests.SamplePath);

            List<string> atInit = ReplayPanelText.Describe(recording, 0);

            CollectionAssert.Contains(atInit, "  Status : Team A robot initialized");
        }
    }
}

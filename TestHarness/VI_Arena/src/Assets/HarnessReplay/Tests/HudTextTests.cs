using System.Collections.Generic;
using NUnit.Framework;

namespace PviHarness.Replay.Tests
{
    /// <summary>
    /// The words and numbers the panels show. Each expected string is what the Swing viewer shows
    /// for the same moment, so the two viewers stay identical.
    /// </summary>
    public class HudTextTests
    {
        private static RunRecordingData Sample => RecordingLoader.Load(RecordingLoaderTests.SamplePath);

        /// <summary>WHAT: The mission clock reads minutes and seconds with two decimals, like the Swing header.</summary>
        [Test]
        public void ClockReadsMinutesAndSeconds()
        {
            Assert.AreEqual("00:00.00", HudText.Clock(0));
            Assert.AreEqual("01:23.46", HudText.Clock(83.456));
            Assert.AreEqual("02:00.00", HudText.Clock(120));
            Assert.AreEqual("00:00.00", HudText.Clock(-1), "never negative");
        }

        /// <summary>WHAT: Motor names and powers are written the way the gauges show them.</summary>
        [Test]
        public void MotorNamesAndPowersMatchTheGauges()
        {
            Assert.AreEqual("FRONT LEFT", HudText.SpacedName("frontLeft"));
            Assert.AreEqual("INTAKE", HudText.SpacedName("intake"));
            Assert.AreEqual("+0.20", HudText.Power(0.2));
            Assert.AreEqual("-1.00", HudText.Power(-1));
            Assert.AreEqual("+0.00", HudText.Power(0));
        }

        /// <summary>
        /// WHAT: The header and field texts for a moment of the sample match the Swing viewer's.
        /// HOW: Loop 60 of the sample (Team A TeleOp, turning).
        /// </summary>
        [Test]
        public void HeaderAndFieldTextsMatchTheSwingViewer()
        {
            RunRecordingData recording = Sample;

            Assert.AreEqual("TeamATeleOp  ·  teamA  ·  file.json", HudText.Subtitle(recording.header, "file.json"));
            Assert.AreEqual("Replay  ·  TELEOP  ·  LOOP  ·  loop 61 / 151", HudText.StageLine(recording, 60));
            StringAssert.StartsWith("TeamATeleOp  LOOP  t=0.20 s   x=0.00 ft, y=0.00 ft, heading=",
                HudText.FieldCaption(recording, 60, recording.loops[60].pose));
            Assert.AreEqual("Robot 18 x 18 in (Team A), PLACEHOLDER size, not measured. Cyan = path, white = facing.",
                HudText.Legend(recording.header.driveModel));
            Assert.IsTrue(HudText.SizeIsPlaceholder(recording.header.driveModel));
        }

        /// <summary>
        /// WHAT: The Driver Station card shows the screen sent at or before the moment, with its time;
        /// the Events card starts with the rumble counts.
        /// </summary>
        [Test]
        public void DriverStationAndEventsCardsMatchTheSwingViewer()
        {
            RunRecordingData recording = Sample;

            List<string> atInit = HudText.DriverStationLines(recording, 0);
            Assert.AreEqual(new List<string> { "Status : Team A robot initialized" }, atInit);
            Assert.AreEqual("  ·  sent 0.00 s", HudText.ScreenTime(recording, 0));
            Assert.AreEqual("Gamepad 1 rumbles: 0    Gamepad 2 rumbles: 0", HudText.EventLines(recording, 0)[0]);
        }

        /// <summary>WHAT: The speed dropdown can set any listed speed, and refuses others.</summary>
        [Test]
        public void SpeedCanBeSetByIndex()
        {
            var timeline = new ReplayTimeline(Sample);

            timeline.SetSpeedIndex(0);

            Assert.AreEqual(0.25f, timeline.Speed);
            Assert.Throws<System.ArgumentOutOfRangeException>(() => timeline.SetSpeedIndex(ReplayTimeline.Speeds.Length));
        }

        /// <summary>
        /// WHAT: A long OpMode name (TeamAAprilTagVisionTestOpMode) is cut with "..." to fit the Vehicle
        /// card's OpMode column, so it no longer runs into PERIOD; a short one (TeamATeleOp) is unchanged.
        /// </summary>
        [Test]
        public void LongOpModeNamesFitTheVehicleCard()
        {
            const float cardWidth = 300; // about the side panel's width
            float column = cardWidth * 2 / 5 - 12;

            string longName = ReplayHud.VehicleText("TeamAAprilTagVisionTestOpMode", cardWidth, 0);

            StringAssert.EndsWith("...", longName);
            StringAssert.StartsWith("TeamA", longName);
            Assert.LessOrEqual(ReplayHud.ReadoutWidth(longName), column, "fits before PERIOD: " + longName);
            Assert.AreEqual("TeamATeleOp", ReplayHud.VehicleText("TeamATeleOp", cardWidth, 0));
        }
    }
}

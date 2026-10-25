using System.IO;
using NUnit.Framework;
using UnityEngine;

namespace PviHarness.Replay.Tests
{
    /// <summary>
    /// Reading harness run recordings. The sample is a real recording from the harness: Team A's
    /// TeleOp with full right stick for 0.5 s (the viewer's "turn in place" scenario), copied
    /// into Tests/Samples so these tests never depend on what is in Harness/build.
    /// </summary>
    public class RecordingLoaderTests
    {
        internal static string SamplePath =>
            Path.Combine(Application.dataPath, "HarnessReplay", "Tests", "Samples", "teamA-teleop-turn.json");

        /// <summary>
        /// WHAT: The sample loads with its header, every loop and the drive model.
        /// WHY: If a field name in the C# classes didn't match the JSON, it would silently stay empty.
        /// </summary>
        [Test]
        public void RealRecordingLoadsWithItsHeaderAndLoops()
        {
            RunRecordingData recording = RecordingLoader.Load(SamplePath);

            Assert.AreEqual(2, recording.header.version);
            Assert.AreEqual("TeamATeleOp", recording.header.opMode);
            Assert.AreEqual("TELEOP", recording.header.period);
            Assert.AreEqual("teamA", recording.header.robotConfig);
            Assert.AreEqual(151, recording.loops.Count);
            Assert.AreEqual("INIT", recording.loops[0].stage);
            Assert.AreEqual("STOP", recording.loops[150].stage);
            Assert.AreEqual(5, recording.loops[0].motors.Count, "four drive motors and the intake");
            Assert.AreEqual("Team A", recording.header.driveModel.name);
            Assert.IsTrue(recording.header.driveModel.forward.measured, "forward speed is from Pedro tuning");
            Assert.IsFalse(recording.header.driveModel.turn.measured, "turn rate is still a placeholder");
        }

        /// <summary>
        /// WHAT: The sample ends turned about 86 degrees clockwise, without moving, as in the harness.
        /// </summary>
        [Test]
        public void RealRecordingEndsTurnedClockwiseInPlace()
        {
            PoseData end = RecordingLoader.Load(SamplePath).loops[150].pose;

            Assert.AreEqual(-86.4, end.headingRadians * 180 / System.Math.PI, 0.01);
            Assert.AreEqual(0, end.xInches, 1e-9);
            Assert.AreEqual(0, end.yInches, 1e-9);
        }

        /// <summary>
        /// WHAT: A pose written as null stays null instead of becoming (0, 0, 0).
        /// WHY: "Position not tracked" must not look like "at the field center". This is why the
        /// project uses Newtonsoft JSON rather than Unity's JsonUtility.
        /// </summary>
        [Test]
        public void NullPoseStaysNull()
        {
            RunRecordingData recording = RecordingLoader.Parse(MinimalJson(2, "null"));

            Assert.IsNull(recording.loops[0].pose);
            Assert.IsNull(recording.header.driveModel);
        }

        /// <summary>
        /// WHAT: Version 1 files (no "events") load, with an empty events list.
        /// </summary>
        [Test]
        public void VersionOneFileLoadsWithNoEvents()
        {
            RunRecordingData recording = RecordingLoader.Parse(MinimalJson(1, "null"));
            RunRecordingData nullEvents = RecordingLoader.Parse(MinimalJson(2, "null").Replace("\"loops\":", "\"events\": null, \"loops\":"));

            Assert.AreEqual(0, recording.events.Count);
            Assert.IsNotNull(nullEvents.events, "\"events\": null becomes an empty list");
            Assert.AreEqual(0, nullEvents.events.Count);
        }

        /// <summary>
        /// WHAT: An unknown format version is refused, saying which versions this project reads.
        /// WHY: Reading a newer layout as if it were version 4 would show wrong values silently.
        /// </summary>
        [Test]
        public void UnknownVersionIsRefusedClearly()
        {
            var error = Assert.Throws<RecordingFormatException>(() => RecordingLoader.Parse(MinimalJson(5, "null"), "future.json"));

            StringAssert.Contains("future.json is format version 5", error.Message);
            StringAssert.Contains("versions 1, 2, 3, 4", error.Message);
        }

        /// <summary>
        /// WHAT: Version 3 recordings (each loop may also list the AprilTags the camera saw) load; the
        /// replay doesn't show the tags yet, so it skips them.
        /// </summary>
        [Test]
        public void VersionThreeLoads()
        {
            Assert.AreEqual(3, RecordingLoader.Parse(MinimalJson(3, "null"), "v3.json").header.version);
        }

        /// <summary>WHAT: Files that aren't recordings give a clear message.</summary>
        [Test]
        public void NonRecordingsAreRefusedClearly()
        {
            StringAssert.Contains("not valid JSON", Assert.Throws<RecordingFormatException>(() => RecordingLoader.Parse("{oops")).Message);
            StringAssert.Contains("no header", Assert.Throws<RecordingFormatException>(() => RecordingLoader.Parse("{}")).Message);
            StringAssert.Contains("no loops", Assert.Throws<RecordingFormatException>(
                () => RecordingLoader.Parse("{\"header\": {\"version\": 2}, \"loops\": []}")).Message);
        }

        /// <summary>A one-loop recording with the given version and pose JSON, and no drive model.</summary>
        private static string MinimalJson(int version, string poseJson)
        {
            return "{\"header\": {\"version\": " + version + ", \"opMode\": \"Test\", \"startPose\": null, \"driveModel\": null},"
                + " \"gamepad1Rumbles\": [], \"gamepad2Rumbles\": [],"
                + " \"loops\": [{\"sinceInitSeconds\": 0, \"matchTimeSeconds\": 0, \"stage\": \"INIT\", \"pose\": " + poseJson
                + ", \"motors\": [], \"servos\": [], \"autoStep\": null, \"telemetry\": []}]}";
        }
    }
}

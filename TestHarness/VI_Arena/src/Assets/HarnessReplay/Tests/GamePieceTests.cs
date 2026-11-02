using NUnit.Framework;
using UnityEngine;

namespace PviHarness.Replay.Tests
{
    /// <summary>The game pieces (POLLEN, NECTAR) in run recordings, format version 4.</summary>
    public class GamePieceTests
    {
        // Two pieces at INIT; the POLLEN moves in loop 1 and again in loop 3; nothing changes in loop 2.
        private const string Version4 = @"{
            ""header"": { ""version"": 4, ""distanceUnit"": ""inches"", ""angleUnit"": ""radians"", ""timeUnit"": ""seconds"",
                ""robotConfig"": ""teamA"", ""opMode"": ""TeamATeleOp"", ""period"": ""TELEOP"", ""loopPeriodSeconds"": 0.02,
                ""pieces"": [
                    { ""id"": ""pollen-garden-red-1"", ""kind"": ""pollen"", ""state"": ""field"", ""xInches"": 70.6, ""yInches"": -70.6, ""zInches"": 1.4 },
                    { ""id"": ""red-nectar-cell-1"", ""kind"": ""red-nectar"", ""state"": ""cell"", ""xInches"": 7.37, ""yInches"": -20.95, ""zInches"": 51.8 } ] },
            ""loops"": [
                { ""sinceInitSeconds"": 0, ""stage"": ""INIT"" },
                { ""sinceInitSeconds"": 0.02, ""stage"": ""LOOP"", ""pieces"": [
                    { ""id"": ""pollen-garden-red-1"", ""kind"": ""pollen"", ""state"": ""field"", ""xInches"": 69.0, ""yInches"": -70.6, ""zInches"": 1.4 } ] },
                { ""sinceInitSeconds"": 0.04, ""stage"": ""LOOP"", ""pieces"": [] },
                { ""sinceInitSeconds"": 0.06, ""stage"": ""LOOP"", ""pieces"": [
                    { ""id"": ""pollen-garden-red-1"", ""kind"": ""pollen"", ""state"": ""field"", ""xInches"": 66.0, ""yInches"": -70.0, ""zInches"": 1.4 } ] } ] }";

        private GameObject parent;

        [SetUp]
        public void MakeParent()
        {
            parent = new GameObject("Test pieces");
        }

        [TearDown]
        public void RemoveParent()
        {
            Object.DestroyImmediate(parent);
        }

        /// <summary>
        /// WHAT: A version 4 recording loads its pieces, and each loop shows the latest change of every
        /// piece (a piece not listed stays where it was).
        /// WHY: Recordings only list the pieces that moved, to stay small.
        /// </summary>
        [Test]
        public void EachLoopAddsUpThePieceChanges()
        {
            var track = new GamePieceTrack(RecordingLoader.Parse(Version4));

            Assert.AreEqual(2, track.Start.Count);
            Assert.AreEqual(70.6, track.At(0)[0].xInches, 1e-9, "INIT: where it starts");
            Assert.AreEqual(69.0, track.At(1)[0].xInches, 1e-9, "loop 1 moved it");
            Assert.AreEqual(69.0, track.At(2)[0].xInches, 1e-9, "loop 2 left it");
            Assert.AreEqual(66.0, track.At(3)[0].xInches, 1e-9);
            Assert.AreEqual(-70.0, track.At(3)[0].yInches, 1e-9);
            Assert.AreEqual(69.0, track.At(1)[0].xInches, 1e-9, "going back works");
            Assert.AreEqual("cell", track.At(3)[1].state, "the NECTAR never moved");
            Assert.AreEqual(51.8, track.At(3)[1].zInches, 1e-9);
        }

        /// <summary>WHAT: A live run's recording keeps growing, and the track catches up with new loops.</summary>
        [Test]
        public void ALiveRecordingCatchesUp()
        {
            RunRecordingData recording = RecordingLoader.Parse(Version4);
            var track = new GamePieceTrack(recording);
            Assert.AreEqual(66.0, track.At(3)[0].xInches, 1e-9);

            var next = new LoopData { sinceInitSeconds = 0.08, stage = "LOOP" };
            next.pieces.Add(new PieceData { id = "pollen-garden-red-1", kind = "pollen", state = "field", xInches = 60, yInches = -70, zInches = 1.4 });
            recording.loops.Add(next);

            Assert.AreEqual(60.0, track.At(4)[0].xInches, 1e-9);
        }

        /// <summary>WHAT: Recordings older than version 4 load with no pieces.</summary>
        [Test]
        public void OlderRecordingsHaveNoPieces()
        {
            const string version3 = @"{ ""header"": { ""version"": 3, ""robotConfig"": ""teamA"", ""loopPeriodSeconds"": 0.02 },
                ""loops"": [ { ""sinceInitSeconds"": 0, ""stage"": ""INIT"" } ] }";
            RunRecordingData recording = RecordingLoader.Parse(version3);

            Assert.IsEmpty(new GamePieceTrack(recording).Start);
        }

        /// <summary>
        /// WHAT: Each piece is drawn as a ball of its real size at its FTC position (converted to Unity),
        /// without colliders, and a ball that moves turns as it rolls.
        /// </summary>
        [Test]
        public void PiecesAreDrawnAtTheirSpotsAndRoll()
        {
            var track = new GamePieceTrack(RecordingLoader.Parse(Version4));
            var view = new GamePieceView(parent.transform, track.Start);
            view.Show(track.At(0));

            Transform pollen = view.Ball("pollen-garden-red-1");
            Transform nectar = view.Ball("red-nectar-cell-1");
            Vector3 expected = FtcToUnity.Position(70.6, -70.6, FtcToUnity.Meters(1.4));
            Assert.Less(Vector3.Distance(expected, pollen.position), 1e-4f, "POLLEN on its spot");
            Assert.AreEqual(FtcToUnity.Meters(2.8), SizeOf(pollen), FtcToUnity.Meters(0.15), "POLLEN is 2.8 in");
            Assert.AreEqual(FtcToUnity.Meters(3.6), SizeOf(nectar), FtcToUnity.Meters(0.15), "NECTAR is 3.6 in");
            Assert.IsEmpty(parent.GetComponentsInChildren<Collider>());

            Quaternion pollenBefore = pollen.rotation;
            Quaternion nectarBefore = nectar.rotation;
            view.Show(track.At(1));
            Assert.Greater(Quaternion.Angle(pollenBefore, pollen.rotation), 30f, "rolled about 65 degrees for 1.6 in");
            Assert.AreEqual(0f, Quaternion.Angle(nectarBefore, nectar.rotation), 1e-3f, "the NECTAR didn't move, so didn't turn");
        }

        private static float SizeOf(Transform ball)
        {
            Bounds b = ball.GetComponentInChildren<Renderer>().bounds;
            return b.size.x;
        }
    }
}

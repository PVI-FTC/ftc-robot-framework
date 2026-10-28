using System.Collections.Generic;
using System.Linq;
using NUnit.Framework;
using UnityEngine;

namespace PviHarness.Replay.Tests
{
    /// <summary>What the webcams saw (recording format version 3 "aprilTags"): the card text and the sight lines.</summary>
    public class AprilTagSightTests
    {
        private const string Webcam = "logitechVisionWebcam";

        private static AprilTagSeenData Tag(int id, double right, double forward, double up = 0)
        {
            return new AprilTagSeenData { camera = Webcam, id = id, rightInches = right, forwardInches = forward, upInches = up };
        }

        /// <summary>
        /// WHAT: The "Tags in view" lines are exactly the Swing viewer's (TagsInViewCardTest: tag 22, 2 in
        /// right and 30 in ahead is "Tag 22    30.1 in   -3.8 deg"), and "none" with no tag in view.
        /// WHY: Both viewers must say the same thing about the same recording.
        /// </summary>
        [Test]
        public void CardLinesMatchTheSwingViewer()
        {
            var loop = new LoopData();
            loop.aprilTags.Add(Tag(22, 2.0, 30.0));

            Assert.AreEqual(new List<string> { "Tag 22    30.1 in   -3.8 deg  " + Webcam }, HudText.AprilTagLines(loop));
            Assert.AreEqual(new List<string> { "none" }, HudText.AprilTagLines(new LoopData()));
        }

        /// <summary>
        /// WHAT: A tag 30 in ahead and 5 in right of a robot at (10, 20) facing +y (90°) is at (15, 50),
        /// at the camera's height plus its "up".
        /// </summary>
        [Test]
        public void TagIsPlacedFromTheRobotsPose()
        {
            var robot = new PoseData { xInches = 10, yInches = 20, headingRadians = Mathf.PI / 2 };

            Vector3 at = AprilTagSightView.TagPositionInches(robot, Tag(7, 5, 30, 4));

            Assert.AreEqual(15f, at.x, 1e-4f, "right of a robot facing +y is +x");
            Assert.AreEqual(50f, at.y, 1e-4f);
            Assert.AreEqual((float)AprilTagSightView.CameraHeightInches + 4f, at.z, 1e-4f);
        }

        /// <summary>WHAT: One sight line per tag in view, labelled with its id; none when switched off or with no pose.</summary>
        [Test]
        public void OneLinePerTagAndNoneWhenOff()
        {
            var parent = new GameObject("Test sights");
            try
            {
                var view = new AprilTagSightView(parent.transform);
                var loop = new LoopData();
                loop.aprilTags.Add(Tag(22, 2, 30));
                loop.aprilTags.Add(Tag(34, -6, 40));
                var robot = new PoseData();

                view.Show(loop, robot, true);
                List<Transform> shown = Active(view);
                Assert.AreEqual(2, shown.Count);
                Assert.AreEqual("34", shown[1].Find("Label").GetComponent<TextMesh>().text);
                Vector3 expected = FtcToUnity.Position(30, -2, FtcToUnity.Meters(AprilTagSightView.CameraHeightInches));
                Assert.Less(Vector3.Distance(expected, shown[0].GetComponent<LineRenderer>().GetPosition(1)), 1e-4f,
                    "the line ends where the camera put tag 22");

                view.Show(loop, robot, false);
                Assert.AreEqual(0, Active(view).Count, "switched off");
                view.Show(loop, null, true);
                Assert.AreEqual(0, Active(view).Count, "no pose");
            }
            finally
            {
                Object.DestroyImmediate(parent);
            }
        }

        /// <summary>WHAT: A version 3+ recording's loops load their "aprilTags".</summary>
        [Test]
        public void RecordingsLoadTheirAprilTags()
        {
            const string json = @"{ ""header"": { ""version"": 3, ""robotConfig"": ""teamA"", ""loopPeriodSeconds"": 0.02 },
                ""loops"": [ { ""sinceInitSeconds"": 0, ""stage"": ""LOOP"", ""aprilTags"": [
                    { ""camera"": ""logitechVisionWebcam"", ""id"": 22, ""rightInches"": 2, ""forwardInches"": 30, ""upInches"": 5,
                      ""yawDegrees"": 0, ""pitchDegrees"": 0, ""rollDegrees"": 0 } ] } ] }";

            RunRecordingData recording = RecordingLoader.Parse(json);

            Assert.AreEqual(22, recording.loops[0].aprilTags[0].id);
            Assert.AreEqual(30.0, recording.loops[0].aprilTags[0].forwardInches, 1e-9);
            Assert.IsTrue(HudText.AnyAprilTags(recording));
        }

        private static List<Transform> Active(AprilTagSightView view)
        {
            return view.Root.Cast<Transform>().Where(t => t.gameObject.activeSelf).ToList();
        }
    }
}

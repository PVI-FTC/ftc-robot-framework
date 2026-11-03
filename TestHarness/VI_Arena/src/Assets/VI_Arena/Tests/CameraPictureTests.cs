using System.Collections.Generic;
using NUnit.Framework;
using UnityEngine;

namespace VI_Arena.Tests
{
    /// <summary>
    /// The webcam picture (C key): the camera mount from the recording (<see cref="CameraMount"/>),
    /// the Unity camera placed at it (<see cref="CameraPictureView"/>), the tag boxes drawn on the
    /// picture, and the sight lines starting at the lens.
    /// </summary>
    public class CameraPictureTests
    {
        private const float Tolerance = 1e-4f;

        /// <summary>
        /// WHAT: Angles 0, 0, 0 look straight forward, level, with the picture's right on the robot's right.
        /// WHY: That's the harness's camera convention; reading it wrong would point the picture elsewhere.
        /// </summary>
        [Test]
        public void ZeroAnglesLookLevelAndForward()
        {
            CameraMount m = Mount(yaw: 0, pitch: 0, roll: 0);

            AssertNear(new Vector3(0, 1, 0), m.Look, "look");
            AssertNear(new Vector3(0, 0, 1), m.Up, "picture up");
            AssertNear(new Vector3(1, 0, 0), m.Right, "picture right");
        }

        /// <summary>
        /// WHAT: Yaw +90 turns the camera to look left (counter-clockwise, like the FTC heading), pitch +30
        /// tips it 30° up, and roll +90 turns the picture clockwise (its top to the robot's right).
        /// </summary>
        [Test]
        public void YawTurnsLeftPitchTipsUpRollTurnsClockwise()
        {
            AssertNear(new Vector3(-1, 0, 0), Mount(yaw: 90, pitch: 0, roll: 0).Look, "yaw 90 looks left");
            AssertNear(new Vector3(0, Mathf.Cos(30 * Mathf.Deg2Rad), Mathf.Sin(30 * Mathf.Deg2Rad)),
                Mount(yaw: 0, pitch: 30, roll: 0).Look, "pitch 30 looks 30 degrees up");
            AssertNear(new Vector3(1, 0, 0), Mount(yaw: 0, pitch: 0, roll: 90).Up, "roll 90: picture top on the right");
        }

        /// <summary>
        /// WHAT: A camera 6 in right (left -6), 4 in forward and 8 in up, turned 90° left, looks along the robot's
        /// left (-x) and its picture's right is the robot's forward (+y). A tag it measured 30 in ahead
        /// and 5 in right is at lens (6, 4, 8) + 30 × (-1, 0, 0) + 5 × (0, 1, 0) = (-24, 9, 8).
        /// WHY: The sight lines and the field markers use this; the mount has to move and turn the measurement.
        /// </summary>
        [Test]
        public void ATagIsPlacedFromTheLensAlongItsDirections()
        {
            CameraMount m = Mount(yaw: 90, pitch: 0, roll: 0, left: -6, forward: 4, up: 8);

            AssertNear(new Vector3(-24, 9, 8), m.TagInRobotInches(Tag(7, right: 5, forward: 30, up: 0)), "tag in the robot's frame");
        }

        /// <summary>
        /// WHAT: Without a recorded mount the camera is at the robot's center, at the old sight-line
        /// height, facing forward, so older recordings keep drawing their sight lines where they did.
        /// </summary>
        [Test]
        public void WithoutAMountTheOldSightLinesStay()
        {
            var robot = new PoseData { xInches = 10, yInches = -20, headingRadians = Mathf.PI / 2 };
            AprilTagSeenData tag = Tag(7, right: 5, forward: 30, up: 4);

            Vector3 old = CameraMount.RobotToFieldInches(robot,
                new Vector3(5, 30, (float)AprilTagSightView.CameraHeightInches + 4));
            AssertNear(old, AprilTagSightView.TagPositionInches(robot, tag), "same as before camera setups");
            Assert.IsFalse(CameraMount.Unknown("cam").MountKnown);
        }

        /// <summary>
        /// WHAT: A tag straight ahead lands on the lens center; one to the right lands right of it, one
        /// above lands higher (smaller y, since pictures count down from the top).
        /// </summary>
        [Test]
        public void TagsLandWhereAPinholeCameraPutsThem()
        {
            CameraMount m = Mount600();

            Assert.IsTrue(m.Project(Tag(1, 0, 40, 0), out Vector2 center));
            Assert.IsTrue(m.Project(Tag(2, 4, 40, 0), out Vector2 right));
            Assert.IsTrue(m.Project(Tag(3, 0, 40, 4), out Vector2 up));
            Assert.IsFalse(m.Project(Tag(4, 0, -10, 0), out _), "behind the lens");

            AssertNear(new Vector2(320, 240), center, "center");
            AssertNear(new Vector2(380, 240), right, "600 x 4 / 40 = 60 px right");
            AssertNear(new Vector2(320, 180), up, "60 px up");
            Assert.AreEqual(60f, m.PixelsAcross(Tag(1, 0, 40, 0), 4), Tolerance, "a 4 in tag at 40 in");
        }

        /// <summary>
        /// WHAT: The Unity camera at the mount draws a tag exactly where the box goes on the picture.
        /// HOW: Puts a robot at a pose with a yawed, tipped camera; works out where a detection is on the
        /// field; asks the Unity camera where it draws that point; compares with the box's center.
        /// WHY: The picture is only useful if the boxes (the harness's detections) sit on the camera's
        /// view of the same tags.
        /// </summary>
        [Test]
        public void TheUnityCameraDrawsTagsWhereTheBoxesGo()
        {
            var parent = new GameObject("test parent");
            var view = new CameraPictureView(parent.transform);
            try
            {
                var pose = new PoseData { xInches = 12, yInches = -30, headingRadians = 0.7 };
                var robot = new GameObject("Robot").transform;
                robot.SetParent(parent.transform, false);
                FtcToUnity.Pose(pose, 0f, out Vector3 position, out Quaternion rotation);
                robot.SetPositionAndRotation(position, rotation);
                CameraMount m = Mount(yaw: 20, pitch: 10, roll: 15, left: -5, forward: 7, up: 9);
                AprilTagSeenData tag = Tag(7, right: -6, forward: 50, up: 3);

                view.Show(robot, m);
                Vector3 field = CameraMount.RobotToFieldInches(pose, m.TagInRobotInches(tag));
                Vector3 seen = view.Camera.WorldToViewportPoint(FtcToUnity.Position(field.x, field.y, FtcToUnity.Meters(field.z)));
                var picture = new Rect(0, 0, m.WidthPixels, m.HeightPixels);
                Assert.IsTrue(CameraPictureView.TagBox(m, tag, 4, picture, out Rect box));

                Assert.IsTrue(view.Showing);
                Assert.AreEqual(box.center.x, seen.x * m.WidthPixels, 0.5f, "across");
                Assert.AreEqual(box.center.y, (1 - seen.y) * m.HeightPixels, 0.5f, "down");
            }
            finally
            {
                view.Destroy();
                Object.DestroyImmediate(parent);
            }
        }

        /// <summary>
        /// WHAT: The webcam shows the setting's sky even while the main window is in the top-down view
        /// (orthographic, plain background), and the plain background in a setting without a sky.
        /// WHY: The picture used to copy the main camera's choice, so in the top-down view its sky was black.
        /// </summary>
        [Test]
        public void TheWebcamShowsTheSkyInTheTopDownViewToo()
        {
            var parent = new GameObject("test parent");
            var view = new CameraPictureView(parent.transform);
            Material oldSky = RenderSettings.skybox;
            var sky = new Material(Shader.Find("Skybox/Procedural"));
            try
            {
                var robot = new GameObject("Robot").transform;
                robot.SetParent(parent.transform, false);
                Camera topDown = new GameObject("Main camera, top-down").AddComponent<Camera>();
                topDown.transform.SetParent(parent.transform, false);
                topDown.orthographic = true;
                topDown.clearFlags = CameraClearFlags.SolidColor;
                topDown.backgroundColor = Color.red;

                RenderSettings.skybox = sky;
                view.Show(robot, Mount(0, 18, 0), topDown);
                Assert.AreEqual(CameraClearFlags.Skybox, view.Camera.clearFlags, "a setting with a sky");
                Assert.IsFalse(view.Camera.orthographic);

                RenderSettings.skybox = null;
                view.Show(robot, Mount(0, 18, 0), topDown);
                Assert.AreEqual(CameraClearFlags.SolidColor, view.Camera.clearFlags, "a setting without a sky");
                Assert.AreEqual(Color.red, view.Camera.backgroundColor);
            }
            finally
            {
                RenderSettings.skybox = oldSky;
                Object.DestroyImmediate(sky);
                view.Destroy();
                Object.DestroyImmediate(parent);
            }
        }

        /// <summary>
        /// WHAT: A box is scaled from the webcam's pixels to the picture's size on screen.
        /// </summary>
        [Test]
        public void BoxesScaleToThePictureOnScreen()
        {
            CameraMount m = Mount600();

            Assert.IsTrue(CameraPictureView.TagBox(m, Tag(1, 4, 40, 0), 4, new Rect(100, 50, 320, 240), out Rect box));

            AssertNear(new Vector2(100 + 190, 50 + 120), box.center, "half of (380, 240), moved to the picture");
            Assert.AreEqual(30f, box.width, Tolerance, "half of 60 px");
        }

        /// <summary>
        /// WHAT: The field of view sets the lens (a 90° wide, 640 px picture has a 320 px focal length);
        /// a field of view listed in "placeholders" is marked as not measured; with none, the C920's
        /// (about 54° wide) is used.
        /// </summary>
        [Test]
        public void TheFieldOfViewSetsTheLensAndGuessesAreMarked()
        {
            CameraMount measured = Mount(0, 0, 0, hfov: 90, vfov: 2 * Mathf.Atan(240f / 320f) * Mathf.Rad2Deg);
            CameraMount guessed = CameraMount.From(new CameraData { name = "cam", horizontalFovDegrees = 65.9, verticalFovDegrees = 51.8,
                imageWidthPixels = 640, imageHeightPixels = 480,
                placeholders = new List<string> { "horizontalFovDegrees", "verticalFovDegrees", "maxRangeInches", "maxViewAngleDegrees" } });
            CameraMount guessedMount = CameraMount.From(new CameraData { name = "cam", placeholders = new List<string> { "upInches" } });
            CameraMount none = CameraMount.From(new CameraData { name = "cam" });

            Assert.AreEqual(320.0, measured.Fx, 1e-3);
            Assert.AreEqual(320.0, measured.Fy, 1e-3);
            Assert.IsTrue(measured.LensKnown && measured.MountMeasured);
            Assert.IsFalse(guessed.LensKnown, "FOV listed as a placeholder");
            Assert.IsTrue(guessed.MountMeasured, "range and view angle aren't the mount");
            Assert.IsFalse(guessedMount.MountMeasured, "a guessed height is");
            Assert.AreEqual(51.8f, guessed.VerticalFovDegrees, 1e-3f);
            Assert.IsFalse(none.LensKnown);
            Assert.AreEqual(622.001, none.Fx, 1e-3, "C920 fallback");
        }

        /// <summary>
        /// WHAT: A sight line starts at the webcam's lens when the recording has its mount.
        /// </summary>
        [Test]
        public void SightLinesStartAtTheLens()
        {
            var parent = new GameObject("test parent");
            try
            {
                var sights = new AprilTagSightView(parent.transform);
                var pose = new PoseData { xInches = 0, yInches = 0, headingRadians = 0 };
                var loop = new LoopData { aprilTags = new List<AprilTagSeenData> { Tag(7, 0, 40, 0) } };
                var cameras = new List<CameraData>
                {
                    new CameraData { name = "cam", leftInches = -5, forwardInches = 7, upInches = 9 },
                };

                sights.Show(loop, pose, true, cameras, new List<FieldTagData> { FieldTag(7, 47, -5, 9) });

                LineRenderer line = sights.Root.GetComponentInChildren<LineRenderer>();
                // Heading 0 faces FTC +x, so the robot's right is FTC -y: lens at x 7, y -5, 9 in up.
                Vector3 lens = FtcToUnity.Position(7, -5, FtcToUnity.Meters(9));
                AssertNear(lens, line.GetPosition(0), "line start");
            }
            finally
            {
                Object.DestroyImmediate(parent);
            }
        }

        /// <summary>
        /// WHAT: A line goes where the camera measured a tag when that's on the field's tag; to the real
        /// tag (amber) when it was measured elsewhere; and, for an id the field doesn't have, to where it
        /// was measured, marked "not on this field" (dashed).
        /// WHY: The user saw lines "that don't point at anything" from scripted detections (DECODE ids
        /// made up 30 in ahead of the robot on the BIOBUZZ field); they must be told apart from real tags.
        /// </summary>
        [Test]
        public void LinesGoToFieldTagsOrAreMarkedNotOnTheField()
        {
            var pose = new PoseData { xInches = 0, yInches = 0, headingRadians = 0 };
            CameraMount m = Mount(0, 0, 0, forward: 5, up: 10);
            // Heading 0: ahead is FTC +x, the robot's right is FTC -y. Measured 40 ahead, 2 right, 1 up:
            // field (45, -2, 11).
            AprilTagSeenData measured = Tag(20, right: 2, forward: 40, up: 1);
            var onTag = new List<FieldTagData> { FieldTag(20, 46, -3, 12) };
            var elsewhere = new List<FieldTagData> { FieldTag(20, -50, 60, 30) };

            Assert.AreEqual(AprilTagSightView.TargetKind.OnTag, AprilTagSightView.Target(pose, measured, m, onTag, out Vector3 a));
            AssertNear(new Vector3(45, -2, 11), a, "ends where measured");
            Assert.AreEqual(AprilTagSightView.TargetKind.MeasuredElsewhere, AprilTagSightView.Target(pose, measured, m, elsewhere, out Vector3 b));
            AssertNear(new Vector3(-50, 60, 30), b, "ends on the real tag");
            Assert.AreEqual(AprilTagSightView.TargetKind.NotOnField, AprilTagSightView.Target(pose, measured, m, new List<FieldTagData>(), out _));
            Assert.AreEqual(AprilTagSightView.TargetKind.NotOnField,
                AprilTagSightView.Target(pose, Tag(22, 2, 40, 1), m, onTag, out Vector3 c), "tag 22 isn't on this field");
            AssertNear(new Vector3(45, -2, 11), c, "a tag not on the field is drawn where measured");
            Assert.AreEqual("22 · not on this field", AprilTagSightView.LabelText(22, AprilTagSightView.TargetKind.NotOnField));
            Assert.AreEqual("20", AprilTagSightView.LabelText(20, AprilTagSightView.TargetKind.OnTag));
        }

        /// <summary>
        /// WHAT: A field point turned into the camera's measurement and back lands where it started.
        /// WHY: Boxes for tags measured elsewhere are drawn at the real tag through this.
        /// </summary>
        [Test]
        public void AFieldPointSeenFromTheCameraRoundTrips()
        {
            var pose = new PoseData { xInches = 12, yInches = -30, headingRadians = 0.7 };
            CameraMount m = Mount(yaw: 20, pitch: 10, roll: 15, left: -5, forward: 7, up: 9);
            var spot = new Vector3(40, 10, 25);

            AprilTagSeenData seen = m.SeenFrom(pose, spot, 9);

            AssertNear(spot, AprilTagSightView.TagPositionInches(pose, seen, m), "round trip");
            Assert.AreEqual(9, seen.id);
        }

        /// <summary>
        /// WHAT: Without camera data the picture says why: an old recording (before format 5) never has
        /// it; a newer one means the robot has no webcam. Tags not on the field are listed under the picture.
        /// </summary>
        [Test]
        public void ThePictureSaysWhyNothingIsShown()
        {
            StringAssert.Contains("before camera setups", HudText.NoCameraText(4));
            StringAssert.Contains("No webcam on this robot", HudText.NoCameraText(5));
            Assert.IsNull(HudText.NotOnFieldText(new List<int>(), "BIOBUZZ"));
            Assert.AreEqual("Not on BIOBUZZ (dashed): tag 22, 20, scripted or another season's", HudText.NotOnFieldText(new List<int> { 22, 20 }, "BIOBUZZ"));
        }

        /// <summary>
        /// WHAT: When the harness side can't list OpModes, the Live row says why on one line (T16: a
        /// mixed USB copy showed an empty list and no reason).
        /// </summary>
        [Test]
        public void LiveProblemsAreShownOnOneLine()
        {
            Assert.AreEqual("Live mode can't list OpModes: The harness can't list its OpModes: Problems in harness.properties: config.version is 3",
                HudText.LiveProblemText("The harness can't list its OpModes: Problems in harness.properties:\n  config.version is 3"));
            StringAssert.Contains("no reason", HudText.LiveProblemText(null));
        }

        /// <summary>
        /// WHAT: The webcam picture leaves out the sight lines and their markers (they're drawn for
        /// people), so a tag's pattern isn't hidden behind its own marker.
        /// </summary>
        [Test]
        public void TheWebcamPictureLeavesOutTheSightLines()
        {
            var parent = new GameObject("test parent");
            var view = new CameraPictureView(parent.transform);
            try
            {
                var sights = new AprilTagSightView(parent.transform);
                var loop = new LoopData { aprilTags = new List<AprilTagSeenData> { Tag(7, 0, 40, 0) } };
                sights.Show(loop, new PoseData(), true, null, new List<FieldTagData>());

                foreach (Transform t in sights.Root.GetComponentsInChildren<Transform>())
                {
                    if (t != sights.Root)
                    {
                        Assert.AreEqual(AprilTagSightView.Layer, t.gameObject.layer, t.name);
                    }
                }
                Assert.AreEqual(0, view.Camera.cullingMask & (1 << AprilTagSightView.Layer), "the webcam doesn't draw that layer");
            }
            finally
            {
                view.Destroy();
                Object.DestroyImmediate(parent);
            }
        }

        /// <summary>
        /// WHAT: A version 5 recording's camera setups are read; a recording without them (or with null) has none.
        /// </summary>
        [Test]
        public void CameraSetupsAreReadFromTheRecording()
        {
            string with = "{\"header\":{\"version\":5,\"cameras\":[{\"name\":\"logitechVisionWebcam\",\"leftInches\":-6.875,"
                + "\"forwardInches\":4.875,\"upInches\":19,\"yawDegrees\":0,\"pitchDegrees\":0,\"rollDegrees\":0,"
                + "\"horizontalFovDegrees\":65.9,\"verticalFovDegrees\":51.8,\"imageWidthPixels\":640,\"imageHeightPixels\":480,"
                + "\"maxRangeInches\":120,\"placeholders\":[\"horizontalFovDegrees\"]}]},\"loops\":[{}]}";
            string without = "{\"header\":{\"version\":4},\"loops\":[{}]}";
            string asNull = "{\"header\":{\"version\":4,\"cameras\":null},\"loops\":[{}]}";

            List<CameraData> cameras = RecordingLoader.Parse(with).header.cameras;
            Assert.AreEqual(1, cameras.Count);
            Assert.AreEqual("logitechVisionWebcam", cameras[0].name);
            Assert.AreEqual(-6.875, cameras[0].leftInches);
            Assert.AreEqual(480, cameras[0].imageHeightPixels);
            Assert.AreEqual(120, cameras[0].maxRangeInches);
            CollectionAssert.AreEqual(new[] { "horizontalFovDegrees" }, cameras[0].placeholders);
            Assert.AreEqual(0, RecordingLoader.Parse(without).header.cameras.Count);
            Assert.AreEqual(0, RecordingLoader.Parse(asNull).header.cameras.Count, "cameras: null");
        }

        private static CameraMount Mount(double yaw, double pitch, double roll, double left = 0, double forward = 0, double up = 0,
            double hfov = 0, double vfov = 0)
        {
            return CameraMount.From(new CameraData
            {
                name = "cam", leftInches = left, forwardInches = forward, upInches = up,
                yawDegrees = yaw, pitchDegrees = pitch, rollDegrees = roll,
                imageWidthPixels = 640, imageHeightPixels = 480, horizontalFovDegrees = hfov, verticalFovDegrees = vfov,
            });
        }

        /// <summary>A level, forward 640 x 480 camera with a 600 px focal length.</summary>
        private static CameraMount Mount600()
        {
            return Mount(0, 0, 0, hfov: 2 * Mathf.Atan(320f / 600f) * Mathf.Rad2Deg, vfov: 2 * Mathf.Atan(240f / 600f) * Mathf.Rad2Deg);
        }

        private static FieldTagData FieldTag(int id, double x, double y, double z)
        {
            return new FieldTagData { id = id, xInches = x, yInches = y, zInches = z, facing = new FieldVectorData(), sizeInches = 4 };
        }

        private static AprilTagSeenData Tag(int id, double right, double forward, double up)
        {
            return new AprilTagSeenData { camera = "cam", id = id, rightInches = right, forwardInches = forward, upInches = up };
        }

        private static void AssertNear(Vector3 expected, Vector3 actual, string what)
        {
            Assert.AreEqual(expected.x, actual.x, 1e-3f, what + " x");
            Assert.AreEqual(expected.y, actual.y, 1e-3f, what + " y");
            Assert.AreEqual(expected.z, actual.z, 1e-3f, what + " z");
        }

        private static void AssertNear(Vector2 expected, Vector2 actual, string what)
        {
            Assert.AreEqual(expected.x, actual.x, 1e-3f, what + " x");
            Assert.AreEqual(expected.y, actual.y, 1e-3f, what + " y");
        }
    }
}

using System.Collections.Generic;
using NUnit.Framework;
using UnityEngine;

namespace VI_Arena.Tests
{
    /// <summary>
    /// Which of the field's AprilTags the webcam picture marks as in view (<see cref="TagsInView"/>),
    /// with or without a detection, and where the ids go on the picture
    /// (<see cref="CameraPictureView.TagIdSpot"/>).
    /// </summary>
    public class TagsInViewTests
    {
        /// <summary>The robot at the field's center, facing +x.</summary>
        private static readonly PoseData Robot = new PoseData { xInches = 0, yInches = 0, headingRadians = 0 };

        /// <summary>
        /// WHAT: A 4 in tag 40 in straight ahead, facing the lens, is in view with no detection at all. Its
        /// printed square with the white edge is 5 in, so 600 x 5 / 40 = 75 px across, centered on the picture.
        /// WHY: The boxes come from the field file and the mount, so an OpMode without vision gets them too.
        /// </summary>
        [Test]
        public void ATagAheadIsInViewWithoutADetection()
        {
            List<TagsInView> found = TagsInView.Find(Lens(), Robot, new[] { TagFacing(7, 40, 0, 0, -1, 0, 0) }, new int[0]);

            Assert.AreEqual(1, found.Count);
            Assert.AreEqual(7, found[0].Id);
            Rect box = found[0].BoxOn(Lens(), new Rect(0, 0, 640, 480), 6);
            Assert.AreEqual(320f, box.center.x, 0.01f);
            Assert.AreEqual(240f, box.center.y, 0.01f);
            Assert.AreEqual(75f, box.width, 0.01f);
            Assert.AreEqual(75f, box.height, 0.01f);
        }

        /// <summary>WHAT: The robot's heading and the lens's place on the robot both count: facing +y, the same kind of tag is ahead at +y.</summary>
        [Test]
        public void TheRobotsPoseAndTheMountCount()
        {
            var facingUpField = new PoseData { xInches = 0, yInches = 0, headingRadians = Mathf.PI / 2 };
            FieldTagData tag = TagFacing(7, 6, 40, 0, 0, -1, 0);

            List<TagsInView> found = TagsInView.Find(Lens(left: -6), facingUpField, new[] { tag }, null);
            Rect box = found[0].BoxOn(Lens(), new Rect(0, 0, 640, 480), 6);

            Assert.AreEqual(320f, box.center.x, 0.01f, "the lens is 6 in right of the robot's center, straight behind the tag");
            Assert.AreEqual(0, TagsInView.Find(Lens(left: -6), Robot, new[] { tag }, null).Count, "facing +x, the tag is out to the side");
        }

        /// <summary>WHAT: A tag behind the lens isn't in view, even with its printed side toward the robot.</summary>
        [Test]
        public void ATagBehindTheLensIsNotInView()
        {
            Assert.AreEqual(0, TagsInView.Find(Lens(), Robot, new[] { TagFacing(7, -40, 0, 0, 1, 0, 0) }, null).Count);
        }

        /// <summary>WHAT: A tag ahead but off the side of the picture isn't in view; one whose center is just on the picture is.</summary>
        [Test]
        public void ATagOutOfFrameIsNotInView()
        {
            // At 40 in the picture reaches 320 / 600 x 40 = 21.3 in to each side.
            Assert.AreEqual(1, TagsInView.Find(Lens(), Robot, new[] { TagFacing(7, 40, 21, 0, -1, 0, 0) }, null).Count, "center on the picture");
            Assert.AreEqual(0, TagsInView.Find(Lens(), Robot, new[] { TagFacing(7, 40, 22, 0, -1, 0, 0) }, null).Count, "center off the picture");
        }

        /// <summary>WHAT: A tag farther than the webcam's range isn't in view; a recording that gives no range has no limit.</summary>
        [Test]
        public void ATagOutOfRangeIsNotInView()
        {
            FieldTagData tag = TagFacing(7, 40, 0, 0, -1, 0, 0);

            Assert.AreEqual(0, TagsInView.Find(Lens(range: 30), Robot, new[] { tag }, null).Count, "range 30 in");
            Assert.AreEqual(1, TagsInView.Find(Lens(range: 50), Robot, new[] { tag }, null).Count, "range 50 in");
            Assert.AreEqual(1, TagsInView.Find(Lens(range: 0), Robot, new[] { tag }, null).Count, "no range given");
            Assert.AreEqual(1, TagsInView.Find(Lens(forward: 15, range: 30), Robot, new[] { tag }, null).Count, "measured from the lens, 15 in ahead of the robot's center");
        }

        /// <summary>
        /// WHAT: A tag turned away by more than the webcam's view angle isn't in view, and its back never is,
        /// even when the recording gives no view angle.
        /// WHY: These are the harness's own limits; a tag seen from behind is only a blank plate.
        /// </summary>
        [Test]
        public void ATagTurnedAwayIsNotInView()
        {
            // Turned 70 degrees from the lens.
            FieldTagData turned = TagFacing(7, 40, 0, 0, -Mathf.Cos(70 * Mathf.Deg2Rad), Mathf.Sin(70 * Mathf.Deg2Rad), 0);

            Assert.AreEqual(0, TagsInView.Find(Lens(viewAngle: 60), Robot, new[] { turned }, null).Count, "limit 60");
            Assert.AreEqual(1, TagsInView.Find(Lens(viewAngle: 80), Robot, new[] { turned }, null).Count, "limit 80");
            Assert.AreEqual(1, TagsInView.Find(Lens(), Robot, new[] { turned }, null).Count, "no limit given");
            Assert.AreEqual(0, TagsInView.Find(Lens(), Robot, new[] { TagFacing(7, 40, 0, 0, 1, 0, 0) }, null).Count, "its back");
        }

        /// <summary>WHAT: A tag the webcam detected in this loop isn't listed again, so it never gets two boxes; the others still are.</summary>
        [Test]
        public void ADetectedTagWinsOverInView()
        {
            FieldTagData[] tags = { TagFacing(7, 40, 0, 0, -1, 0, 0), TagFacing(8, 40, 8, 0, -1, 0, 0) };

            List<TagsInView> found = TagsInView.Find(Lens(), Robot, tags, new[] { 7 });

            Assert.AreEqual(1, found.Count);
            Assert.AreEqual(8, found[0].Id);
        }

        /// <summary>WHAT: A tag without a field position is left out, and no pose or no mount finds nothing.</summary>
        [Test]
        public void NothingIsFoundWithoutTheFacts()
        {
            FieldTagData[] noPose = { new FieldTagData { id = 7, sizeInches = 4 } };
            FieldTagData[] tag = { TagFacing(7, 40, 0, 0, -1, 0, 0) };

            Assert.AreEqual(0, TagsInView.Find(Lens(), Robot, noPose, null).Count, "no position");
            Assert.AreEqual(0, TagsInView.Find(Lens(), null, tag, null).Count, "no pose");
            Assert.AreEqual(0, TagsInView.Find(null, Robot, tag, null).Count, "no mount");
        }

        /// <summary>WHAT: A tag seen at an angle has its real outline, not a square: its near side looks taller than its far side.</summary>
        [Test]
        public void ATagAtAnAngleHasItsRealOutline()
        {
            // Turned 45 degrees: its corners 0 and 3 are one vertical side, 1 and 2 the other.
            FieldTagData tag = TagFacing(7, 40, 0, 0, -Mathf.Cos(45 * Mathf.Deg2Rad), Mathf.Sin(45 * Mathf.Deg2Rad), 0);

            Vector2[] c = TagsInView.Find(Lens(), Robot, new[] { tag }, null)[0].Corners;

            Assert.Greater(Mathf.Abs(Mathf.Abs(c[0].y - c[3].y) - Mathf.Abs(c[1].y - c[2].y)), 1f);
        }

        /// <summary>WHAT: A far-off tag's box keeps a smallest size, so it and its id still show.</summary>
        [Test]
        public void AFarOffTagStillGetsABoxYouCanSee()
        {
            List<TagsInView> found = TagsInView.Find(Lens(), Robot, new[] { TagFacing(7, 1000, 0, 0, -1, 0, 0) }, null);

            Rect box = found[0].BoxOn(Lens(), new Rect(0, 0, 320, 240), 6);

            Assert.AreEqual(6f, box.width, 0.01f);
            Assert.AreEqual(6f, box.height, 0.01f);
        }

        /// <summary>WHAT: The webcam's view angle is read from the recording's camera setup.</summary>
        [Test]
        public void TheViewAngleIsReadFromTheRecording()
        {
            string json = "{\"header\":{\"version\":5,\"cameras\":[{\"name\":\"cam\",\"maxViewAngleDegrees\":60}]},\"loops\":[{}]}";

            Assert.AreEqual(60, CameraMount.From(RecordingLoader.Parse(json).header.cameras[0]).MaxViewAngleDegrees);
        }

        /// <summary>WHAT: An id goes centered above its box when there's room.</summary>
        [Test]
        public void AnIdGoesAboveItsBox()
        {
            Rect spot = CameraPictureView.TagIdSpot(new Rect(100, 100, 40, 40), new Vector2(20, 16), new Rect(0, 0, 360, 270), new List<Rect>());

            Assert.AreEqual(83f, spot.y, 0.01f);
            Assert.AreEqual(110f, spot.x, 0.01f);
        }

        /// <summary>
        /// WHAT: Near the picture's top or side edge the id stays whole inside the picture.
        /// WHY: The HIVE's tags are high up in the picture, and an id cut off at the edge can't be read.
        /// </summary>
        [Test]
        public void AnIdStaysInsideThePicture()
        {
            var inside = new Rect(0, 0, 360, 270);
            var size = new Vector2(20, 16);

            Rect top = CameraPictureView.TagIdSpot(new Rect(100, 2, 40, 40), size, inside, new List<Rect>());
            Rect right = CameraPictureView.TagIdSpot(new Rect(352, 100, 6, 6), size, inside, new List<Rect>());
            Rect tooBig = CameraPictureView.TagIdSpot(new Rect(100, -5, 40, 280), size, inside, new List<Rect>());

            Assert.AreEqual(43f, top.y, 0.01f, "below the box");
            Assert.AreEqual(360f, right.xMax, 0.01f, "against the right edge");
            Assert.GreaterOrEqual(tooBig.y, 0f, "a box filling the picture");
            Assert.LessOrEqual(tooBig.yMax, 270f, "a box filling the picture");
        }

        /// <summary>WHAT: Tags side by side, seen from far off, don't put their ids on top of each other.</summary>
        [Test]
        public void IdsDoNotCoverEachOther()
        {
            var inside = new Rect(0, 0, 360, 270);
            var size = new Vector2(20, 16);
            var taken = new List<Rect>();

            Rect first = CameraPictureView.TagIdSpot(new Rect(100, 100, 6, 6), size, inside, taken);
            Rect second = CameraPictureView.TagIdSpot(new Rect(108, 100, 6, 6), size, inside, taken);
            Rect third = CameraPictureView.TagIdSpot(new Rect(116, 100, 6, 6), size, inside, taken);

            Assert.IsFalse(first.Overlaps(second));
            Assert.IsFalse(first.Overlaps(third));
            Assert.IsFalse(second.Overlaps(third));
        }

        /// <summary>A level, forward 640 x 480 camera with a 600 px focal length.</summary>
        private static CameraMount Lens(double left = 0, double forward = 0, double range = 0, double viewAngle = 0)
        {
            return CameraMount.From(new CameraData
            {
                name = "cam", leftInches = left, forwardInches = forward, imageWidthPixels = 640, imageHeightPixels = 480,
                horizontalFovDegrees = 2 * Mathf.Atan(320f / 600f) * Mathf.Rad2Deg,
                verticalFovDegrees = 2 * Mathf.Atan(240f / 600f) * Mathf.Rad2Deg,
                maxRangeInches = range, maxViewAngleDegrees = viewAngle,
            });
        }

        private static FieldTagData TagFacing(int id, double x, double y, double z, double facingX, double facingY, double facingZ)
        {
            return new FieldTagData
            {
                id = id, xInches = x, yInches = y, zInches = z, sizeInches = 4,
                facing = new FieldVectorData { x = facingX, y = facingY, z = facingZ },
            };
        }
    }
}

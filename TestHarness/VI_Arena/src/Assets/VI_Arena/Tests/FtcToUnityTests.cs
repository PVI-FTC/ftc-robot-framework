using NUnit.Framework;
using UnityEngine;

namespace VI_Arena.Tests
{
    /// <summary>The one place that converts harness (FTC) units to Unity units.</summary>
    public class FtcToUnityTests
    {
        private const float Tolerance = 1e-5f;

        /// <summary>
        /// WHAT: Field edges land 1.8288 m (72 in) from the center: FTC +x on Unity +x, FTC +y on Unity +z.
        /// WHY: If x and y were swapped or mirrored, every replay would show the robot in the wrong place.
        /// </summary>
        [Test]
        public void FieldEdgesLandOnTheRightUnityAxes()
        {
            Vector3 plusX = FtcToUnity.Position(72, 0);
            Vector3 plusY = FtcToUnity.Position(0, 72);

            Assert.AreEqual(1.8288f, plusX.x, Tolerance);
            Assert.AreEqual(0f, plusX.z, Tolerance);
            Assert.AreEqual(0f, plusY.x, Tolerance);
            Assert.AreEqual(1.8288f, plusY.z, Tolerance, "FTC +y is Unity +z");
            Assert.AreEqual(3.6576f, FtcToUnity.FieldSizeMeters, Tolerance, "12 ft field");
        }

        /// <summary>
        /// WHAT: Height in meters becomes Unity y, the up axis.
        /// </summary>
        [Test]
        public void HeightIsUnityY()
        {
            Assert.AreEqual(0.3f, FtcToUnity.Position(10, -20, 0.3f).y, Tolerance);
        }

        /// <summary>
        /// WHAT: A robot facing FTC +x (heading 0) faces Unity +x; facing FTC +y (heading 90°) faces Unity +z.
        /// HOW: Turns the robot's local forward (+z) by the rotation for each heading.
        /// WHY: The heading turns counter-clockwise in FTC but Unity's yaw turns clockwise; a
        /// sign mistake would make every turn go the wrong way.
        /// </summary>
        [Test]
        public void RobotFacesTheSameWayAsInTheHarness()
        {
            Vector3 facingPlusX = FtcToUnity.Rotation(0) * Vector3.forward;
            Vector3 facingPlusY = FtcToUnity.Rotation(Mathf.PI / 2f) * Vector3.forward;
            Vector3 facingMinusY = FtcToUnity.Rotation(-Mathf.PI / 2f) * Vector3.forward;

            Assert.AreEqual(1f, facingPlusX.x, Tolerance);
            Assert.AreEqual(0f, facingPlusX.z, Tolerance);
            Assert.AreEqual(1f, facingPlusY.z, Tolerance);
            Assert.AreEqual(-1f, facingMinusY.z, Tolerance, "a clockwise turn (negative heading) faces -y");
        }
    }
}

using System.IO;
using NUnit.Framework;
using UnityEngine;

namespace VI_Arena.Tests
{
    /// <summary>
    /// Stage U3: Unity's physics moving the robot. <see cref="DriveLaw"/> must follow the harness's
    /// MomentumPhysics rules, and <see cref="UnityRobotPhysics"/> must move the robot like it while
    /// letting walls and field parts really stop it.
    /// </summary>
    public class UnityRobotPhysicsTests
    {
        private const double Loop = 0.02;
        private static readonly DriveCommandData FullForward = new DriveCommandData { frontLeft = 1, frontRight = 1, rearLeft = 1, rearRight = 1 };

        /// <summary>Team A-like numbers: 62.6 in/s, 0.1 s response, 200 in/s² traction, 18 in square.</summary>
        private static RobotPhysicsData Robot(double x = 0, double y = 0, double headingDegrees = 0, bool dynamics = true)
        {
            return new RobotPhysicsData
            {
                forwardInchesPerSecond = 62.6, strafeInchesPerSecond = 50, turnRadiansPerSecond = Mathf.PI,
                lengthInches = 18, widthInches = 18,
                responseTimeSeconds = dynamics ? 0.1 : (double?)null,
                maxAccelerationInchesPerSecondSquared = dynamics ? 200 : (double?)null,
                forwardCoastInchesPerSecondSquared = dynamics ? 40 : (double?)null,
                lateralCoastInchesPerSecondSquared = dynamics ? 60 : (double?)null,
                start = new PoseData { xInches = x, yInches = y, headingRadians = headingDegrees * Mathf.Deg2Rad },
            };
        }

        private static PhysicsStateData Drive(UnityRobotPhysics physics, DriveCommandData command, double seconds)
        {
            PhysicsStateData state = physics.State;
            for (int i = 0; i < Mathf.RoundToInt((float)(seconds / Loop)); i++)
            {
                state = physics.Step(command, Loop);
            }
            return state;
        }

        /// <summary>
        /// WHAT: The motor rules match MomentumPhysics: from rest, full forward gains at most the traction
        /// limit (200 in/s² × 0.02 s = 4 in/s) in one loop; coasting on FLOAT loses 40 in/s² × 0.02 s;
        /// a robot without momentum numbers is at full speed at once.
        /// </summary>
        [Test]
        public void DriveLawFollowsTheHarnessRules()
        {
            Assert.AreEqual(4.0, DriveLaw.After(new RobotSpeed(), FullForward, Loop, Robot()).forward, 1e-9);
            var coasting = new DriveCommandData { braking = false };
            Assert.AreEqual(29.2, DriveLaw.After(new RobotSpeed(30, 0, 0), coasting, Loop, Robot()).forward, 1e-9);
            Assert.AreEqual(62.6, DriveLaw.After(new RobotSpeed(), FullForward, Loop, Robot(dynamics: false)).forward, 1e-9);
            Assert.AreEqual(0.0, DriveLaw.After(new RobotSpeed(0.2, 0, 0), DriveCommandData.Stopped, Loop, Robot()).forward,
                "static friction holds a nearly stopped robot");
        }

        /// <summary>
        /// WHAT: Strafing right gives a negative "left" speed and turning clockwise a negative
        /// counter-clockwise one, as in the harness's MecanumKinematics.
        /// </summary>
        [Test]
        public void DriveLawUsesTheMecanumMixing()
        {
            var strafeRight = new DriveCommandData { frontLeft = 1, frontRight = -1, rearLeft = -1, rearRight = 1 };
            var turnClockwise = new DriveCommandData { frontLeft = 1, frontRight = -1, rearLeft = 1, rearRight = -1 };

            Assert.AreEqual(-50.0, DriveLaw.Target(strafeRight, Robot()).left, 1e-9);
            Assert.AreEqual(-Mathf.PI, DriveLaw.Target(turnClockwise, Robot()).counterClockwise, 1e-6);
        }

        /// <summary>
        /// WHAT: On an empty field, PhysX moves the robot exactly as the speeds say: one second of full
        /// forward from the center ends where adding up DriveLaw's average speeds puts it (within 0.2 in),
        /// still on the x axis and facing +x.
        /// </summary>
        [Test]
        public void MovesLikeTheHarnessOnAnEmptyField()
        {
            double expected = 0;
            var speed = new RobotSpeed();
            for (int i = 0; i < 50; i++)
            {
                RobotSpeed after = DriveLaw.After(speed, FullForward, Loop, Robot());
                expected += (speed.forward + after.forward) / 2 * Loop;
                speed = after;
            }

            using (var physics = new UnityRobotPhysics(Robot(), null))
            {
                PhysicsStateData end = Drive(physics, FullForward, 1.0);

                Assert.AreEqual(expected, end.xInches, 0.2, "forward distance");
                Assert.AreEqual(0.0, end.yInches, 0.05);
                Assert.AreEqual(0.0, end.headingRadians, 0.001);
                Assert.AreEqual(speed.forward, end.forwardInchesPerSecond, 0.01);
            }
        }

        /// <summary>
        /// WHAT: Driving into the +x wall stops the robot with its front at the wall (center 63 in for
        /// an 18 in robot), reports "+x" contact, and its forward speed drops to almost nothing.
        /// </summary>
        [Test]
        public void TheWallStopsTheRobot()
        {
            using (var physics = new UnityRobotPhysics(Robot(x: 40), null))
            {
                PhysicsStateData end = Drive(physics, FullForward, 2.0);

                Assert.AreEqual(63.0, end.xInches, 0.3, "front edge against the wall");
                Assert.AreEqual("+x", end.wallContact);
                Assert.Less(end.forwardInchesPerSecond, 2.0, "pushing into the wall doesn't count as moving");
            }
        }

        private static FieldLayoutData BioBuzz()
        {
            string path = Path.Combine(FieldLayouts.ResolveFolder(new string[0], TestHarnessFolder.Search(Application.dataPath), Application.dataPath), "biobuzz-2026.json");
            return FieldLayouts.Load(path);
        }

        private static RobotPhysicsData WithObstacles(RobotPhysicsData robot, bool on)
        {
            robot.robotObstacles = on;
            return robot;
        }

        /// <summary>
        /// WHAT: On the BIOBUZZ field, a robot driving toward +y under the HIVE is stopped by the red
        /// frame's foot-bar lip (the field file's pieceObstacles, y -24.36), the same place the harness's
        /// own physics stops it, and reports "HIVE frame" like the harness does.
        /// WHY: Both physics modes must stop the robot in the same places and give the same events.
        /// </summary>
        [Test]
        public void HiveFrameBlocksTheRobot()
        {
            using (var physics = new UnityRobotPhysics(WithObstacles(Robot(x: 0, y: -50, headingDegrees: 90), true), BioBuzz()))
            {
                PhysicsStateData end = Drive(physics, FullForward, 2.0);

                Assert.AreEqual(-24.36 - 9, end.yInches, 0.3, "front edge against the lip");
                Assert.AreEqual("HIVE frame", end.obstacleContact);
            }
        }

        /// <summary>
        /// WHAT: A robot driving into the far FLOWER stops at its head (radius 3.13, from 3.75 in up, which
        /// the 14 in tall robot can't pass under) and reports "far FLOWER".
        /// </summary>
        [Test]
        public void FlowerHeadBlocksTheRobot()
        {
            using (var physics = new UnityRobotPhysics(WithObstacles(Robot(x: -40, y: -23.39, headingDegrees: 180), true), BioBuzz()))
            {
                PhysicsStateData end = Drive(physics, FullForward, 2.0);

                Assert.AreEqual(-68.04 + 3.13 + 9, end.xInches, 0.4, "front edge against the head");
                Assert.AreEqual("far FLOWER", end.obstacleContact);
            }
        }

        /// <summary>
        /// WHAT: With field.robotObstacles off in the harness, nothing but the walls stops the robot, as
        /// in the harness's own physics.
        /// </summary>
        [Test]
        public void FieldElementsOnlyBlockWhenTheHarnessSays()
        {
            using (var physics = new UnityRobotPhysics(WithObstacles(Robot(x: 0, y: -50, headingDegrees: 90), false), BioBuzz()))
            {
                PhysicsStateData end = Drive(physics, FullForward, 1.0);

                Assert.Greater(end.yInches, -24.36 - 9 + 3, "drove on past the lip");
                Assert.AreEqual("", end.obstacleContact);
            }
        }

        /// <summary>WHAT: Field elements are named the harness's way: every frame part is "HIVE frame", a FLOWER part is "&lt;which&gt; FLOWER".</summary>
        [Test]
        public void ObstacleNamesMatchTheHarness()
        {
            Assert.AreEqual("HIVE frame", UnityRobotPhysics.ObstacleName("frame-red-foot-far"));
            Assert.AreEqual("far FLOWER", UnityRobotPhysics.ObstacleName("flower-far-head"));
            Assert.AreEqual("blue-wall FLOWER", UnityRobotPhysics.ObstacleName("flower-blue-wall-bracket-2"));
        }

        /// <summary>
        /// WHAT: Wall contact is named the way MomentumPhysics names it, including corners and a robot
        /// turned 45° (which reaches further toward the walls).
        /// </summary>
        [Test]
        public void WallContactNamesMatchTheHarness()
        {
            RobotPhysicsData robot = Robot();
            Assert.AreEqual("", UnityRobotPhysics.WallContact(new PhysicsStateData(), robot));
            Assert.AreEqual("+x/-y", UnityRobotPhysics.WallContact(new PhysicsStateData { xInches = 63, yInches = -63 }, robot));
            Assert.AreEqual("-x", UnityRobotPhysics.WallContact(
                new PhysicsStateData { xInches = -72 + 9 * Mathf.Sqrt(2), headingRadians = Mathf.PI / 4 }, robot));
        }
    }
}

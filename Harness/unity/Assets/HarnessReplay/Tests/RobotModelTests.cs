using System;
using System.Collections.Generic;
using NUnit.Framework;
using UnityEngine;
using Object = UnityEngine.Object;

namespace PviHarness.Replay.Tests
{
    /// <summary>
    /// A robot's 3D model (robots/&lt;id&gt;/robot.properties + <see cref="RobotModelView"/>): read the
    /// file, then place the model at its true size, facing the right way, on the floor, with spinning
    /// wheels. Uses a small stand-in model made of cubes, built like a CAD export: millimeters, z up,
    /// front along +x, robot center at (100, 0, 0). It is built in Unity's left-handed axes, so with
    /// +x forward and +z up the robot's left is -y.
    /// </summary>
    public class RobotModelTests
    {
        private const string StandInProfile =
            "# a stand-in robot\n" +
            "name=Stand-in bot\n" +
            "source=made in the test\n" +
            "model=Robots/stand-in\n" +
            "model.metersPerUnit=0.001\n" +
            "model.forward=+x\n" +
            "model.up=+z\n" +
            "model.centerX=100\n" +
            "wheel.frontLeft=FL, FL2\n" +
            "wheel.frontRight=FR\n" +
            "wheel.rearLeft=RL\n" +
            "wheel.rearRight=RR\n";

        private readonly List<Object> made = new List<Object>();

        [TearDown]
        public void CleanUp()
        {
            foreach (Object o in made)
            {
                Object.DestroyImmediate(o);
            }
            made.Clear();
        }

        /// <summary>
        /// WHAT: Every key in robot.properties is read: name, model, units, axes, center and wheel parts.
        /// </summary>
        [Test]
        public void ReadsEveryKey()
        {
            RobotProfile p = RobotProfile.Parse(StandInProfile, "stand-in");

            Assert.AreEqual("stand-in", p.Id);
            Assert.AreEqual("Stand-in bot", p.Name);
            Assert.AreEqual("Robots/stand-in", p.Model);
            Assert.AreEqual(0.001f, p.MetersPerUnit, 1e-9f);
            Assert.AreEqual("+x", p.Forward);
            Assert.AreEqual("+z", p.Up);
            Assert.AreEqual(new Vector3(100, 0, 0), p.Center);
            CollectionAssert.AreEqual(new[] { "FL, FL2", "FR", "RL", "RR" }, p.Wheels);
        }

        /// <summary>
        /// WHAT: A file without model=, with a bad axis, or with forward and up on the same axis is refused
        /// with a message naming the key.
        /// WHY: A typo would otherwise draw the robot lying on its side or not at all, with no hint why.
        /// </summary>
        [Test]
        public void MistakesAreRefusedWithTheKeyNamed()
        {
            StringAssert.Contains("model=", Refused("name=x\n"));
            StringAssert.Contains("model.forward", Refused("model=a\nmodel.forward=north\n"));
            StringAssert.Contains("different axes", Refused("model=a\nmodel.forward=+z\nmodel.up=-z\n"));
            StringAssert.Contains("model.metersPerUnit", Refused("model=a\nmodel.metersPerUnit=mm\n"));
        }

        /// <summary>
        /// WHAT: The model is drawn at its true size (400 x 370 mm here), not stretched to the robot box.
        /// WHY: The user asked for the real robot's size; the robot box in harness.properties can differ.
        /// </summary>
        [Test]
        public void ModelKeepsItsTrueSize()
        {
            RobotModelView view = Place(Vector3.zero, 0f);

            Vector2 footprint = view.FootprintInches();
            Assert.AreEqual(400 / 25.4f, footprint.x, 0.01f, "length, front to back");
            Assert.AreEqual(370 / 25.4f, footprint.y, 0.01f, "width, side to side");
        }

        /// <summary>
        /// WHAT: The model's front (+x in the model) points along the robot's heading and its left (-y)
        /// to the robot's left, and the profile's center lands on the robot's position.
        /// HOW: The robot faces FTC +y (Unity +z) at (1, 0, 2) m; the front-left wheel must end up ahead
        /// and to the left, and the chassis centered over the robot.
        /// WHY: A wrong turn would drive the model sideways or backwards across the field.
        /// </summary>
        [Test]
        public void ModelFacesTheHeadingAndIsCentered()
        {
            Transform robot = NewRobot(new Vector3(1f, 0f, 2f), 0f);
            RobotModelView view = RobotModelView.Attach(robot, Profile(), StandIn());
            Vector3 frontLeft = robot.InverseTransformPoint(RobotModelView.WorldBounds(Find(view, "FL")).center);
            Vector3 chassis = RobotModelView.WorldBounds(Find(view, "Chassis")).center;

            Assert.Greater(frontLeft.z, 0.1f, "front-left wheel is ahead of the center");
            Assert.Less(frontLeft.x, -0.1f, "front-left wheel is on the robot's left (Unity -x)");
            Assert.AreEqual(1f, chassis.x, 1e-4f);
            Assert.AreEqual(2f, chassis.z, 1e-4f);
        }

        /// <summary>
        /// WHAT: The model's lowest point (the wheel bottoms) touches the robot's floor height.
        /// WHY: A CAD model's origin is rarely on the floor; without this the robot floats or sinks.
        /// </summary>
        [Test]
        public void ModelStandsOnTheFloor()
        {
            RobotModelView view = Place(new Vector3(0f, 0.02f, 0f), 30f);

            Assert.AreEqual(0.02f, RobotModelView.WorldBounds(view.Holder).min.y, 1e-4f);
        }

        /// <summary>
        /// WHAT: At full forward power a wheel turns 1800°/s, its top rolling forward; a wheel at zero
        /// power stays still.
        /// HOW: Front-left at +1 for 0.05 s of recorded time turns it 90°, so its top points forward.
        /// </summary>
        [Test]
        public void WheelsSpinWithTheirMotorPower()
        {
            RobotModelView view = Place(Vector3.zero, 0f);
            LoopData loop = Loop(1.0, 0.0);

            view.Spin(loop, 1.00);
            view.Spin(loop, 1.05);

            Transform robot = view.Holder.parent;
            Vector3 frontLeftTop = robot.InverseTransformDirection(view.WheelPivots[0][0].up);
            Vector3 frontRightTop = robot.InverseTransformDirection(view.WheelPivots[1][0].up);
            Vector3 secondLeftTop = robot.InverseTransformDirection(view.WheelPivots[0][1].up);
            Assert.AreEqual(1f, frontLeftTop.z, 1e-3f, "front-left top rolled forward");
            Assert.AreEqual(1f, secondLeftTop.z, 1e-3f, "the second wheel on the front-left motor rolled too");
            Assert.AreEqual(1f, frontRightTop.y, 1e-4f, "front-right didn't move");
        }

        /// <summary>
        /// WHAT: Pausing (the same time again), going back, or jumping ahead with the slider doesn't spin.
        /// WHY: Only real playback should spin the wheels, or they'd whirl whenever the slider moves.
        /// </summary>
        [Test]
        public void WheelsDontSpinWhenPausedOrJumping()
        {
            RobotModelView view = Place(Vector3.zero, 0f);
            LoopData loop = Loop(1.0, 1.0);

            Quaternion before = view.WheelPivots[0][0].localRotation;

            view.Spin(loop, 1.0);
            view.Spin(loop, 1.0);
            view.Spin(loop, 0.5);
            view.Spin(loop, 9.0);

            Assert.AreEqual(before, view.WheelPivots[0][0].localRotation);
        }

        /// <summary>
        /// WHAT: A model without separate wheel parts still draws; there's just nothing to spin.
        /// </summary>
        [Test]
        public void AModelWithoutWheelPartsStillDraws()
        {
            RobotProfile profile = RobotProfile.Parse("model=Robots/stand-in\nmodel.metersPerUnit=0.001\n" +
                "model.forward=+x\nmodel.up=+z\n", "plain");
            RobotModelView view = RobotModelView.Attach(NewRobot(Vector3.zero, 0f), profile, StandIn());

            Assert.IsTrue(Array.TrueForAll(view.WheelPivots, w => w.Count == 0));
            Assert.DoesNotThrow(() => view.Spin(Loop(1.0, 1.0), 0.0));
        }

        /// <summary>
        /// WHAT: The goBILDA Starter Bot in Harness/robots loads with its model at its true size (426 x 452
        /// mm, 16.8 x 17.8 in), its omni wheels at the front left, and all six wheels found.
        /// WHY: Checks the real robot.properties against the real model, so a wrong axis or a renamed
        /// part shows up here instead of as a sideways robot in the app.
        /// </summary>
        [Test]
        public void TheGoBildaStarterBotLoadsAtItsTrueSize()
        {
            string folder = RobotProfile.ResolveFolder(Application.dataPath, isEditor: true);
            var problems = new List<string>();
            RobotProfile profile = RobotProfile.Load(folder, "gobilda-starter-bot", problems);
            Assert.IsNotNull(profile, "robots/gobilda-starter-bot/robot.properties: " + string.Join("; ", problems));
            GameObject model = Resources.Load<GameObject>(profile.Model);
            Assert.IsNotNull(model, "no model at Resources/" + profile.Model);

            Transform robot = NewRobot(Vector3.zero, 0f);
            RobotModelView view = RobotModelView.Attach(robot, profile, model);
            Vector2 footprint = view.FootprintInches();
            Vector3 frontLeft = robot.InverseTransformPoint(RobotModelView.WorldBounds(Find(view, "wheelLeftFront")).center);

            Assert.AreEqual(452 / 25.4f, footprint.x, 0.1f, "length, front to back");
            Assert.AreEqual(426 / 25.4f, footprint.y, 0.1f, "width, side to side");
            Assert.Greater(frontLeft.z, 0.1f, "the omni wheels are at the front");
            Assert.Less(frontLeft.x, -0.1f, "wheelLeftFront is on the robot's left (Unity -x)");
            CollectionAssert.AreEqual(new[] { 2, 2, 1, 1 }, Array.ConvertAll(view.WheelPivots, w => w.Count));
        }

        private static string Refused(string text)
        {
            return Assert.Throws<FormatException>(() => RobotProfile.Parse(text, "bad")).Message;
        }

        private static RobotProfile Profile()
        {
            return RobotProfile.Parse(StandInProfile, "stand-in");
        }

        private RobotModelView Place(Vector3 position, float yawDegrees)
        {
            return RobotModelView.Attach(NewRobot(position, yawDegrees), Profile(), StandIn());
        }

        private Transform NewRobot(Vector3 position, float yawDegrees)
        {
            var robot = new GameObject("Robot");
            made.Add(robot);
            robot.transform.position = position;
            robot.transform.rotation = Quaternion.Euler(0f, yawDegrees, 0f);
            return robot.transform;
        }

        /// <summary>A 400 x 300 x 100 mm chassis and five 96 mm wheels (two on the front-left motor), 30 mm wide, z up, front +x.</summary>
        private GameObject StandIn()
        {
            var root = new GameObject("stand-in model");
            made.Add(root);
            Cube(root.transform, "Chassis", new Vector3(100, 0, 80), new Vector3(400, 300, 100));
            Cube(root.transform, "FL", new Vector3(250, -170, 50), new Vector3(96, 30, 96));
            Cube(root.transform, "FL2", new Vector3(100, -170, 50), new Vector3(96, 30, 96));
            Cube(root.transform, "FR", new Vector3(250, 170, 50), new Vector3(96, 30, 96));
            Cube(root.transform, "RL", new Vector3(-50, -170, 50), new Vector3(96, 30, 96));
            Cube(root.transform, "RR", new Vector3(-50, 170, 50), new Vector3(96, 30, 96));
            return root;
        }

        private static void Cube(Transform parent, string name, Vector3 position, Vector3 size)
        {
            GameObject cube = GameObject.CreatePrimitive(PrimitiveType.Cube);
            cube.name = name;
            cube.transform.SetParent(parent, false);
            cube.transform.localPosition = position;
            cube.transform.localScale = size;
        }

        private static Transform Find(RobotModelView view, string name)
        {
            foreach (Transform t in view.Holder.GetComponentsInChildren<Transform>())
            {
                if (t.name == name)
                {
                    return t;
                }
            }
            throw new AssertionException("no part named " + name);
        }

        private static LoopData Loop(double frontLeft, double frontRight)
        {
            var loop = new LoopData();
            loop.motors.Add(new MotorData { name = "frontLeft", power = frontLeft });
            loop.motors.Add(new MotorData { name = "frontRight", power = frontRight });
            return loop;
        }
    }
}

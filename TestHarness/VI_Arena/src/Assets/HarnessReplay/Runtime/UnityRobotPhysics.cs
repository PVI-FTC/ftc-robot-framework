using System;
using System.Collections.Generic;
using UnityEngine;

namespace PviHarness.Replay
{
    /// <summary>
    /// Stage U3: Unity's physics engine (PhysX) moves the robot for a live run loaded with
    /// <c>"physics": "unity"</c>. The robot is an invisible box with a Rigidbody; the four walls and,
    /// when the harness says so (<see cref="RobotPhysicsData.robotObstacles"/>), the season field's
    /// solid parts are invisible colliders, so the robot really stops against them and slides along
    /// them. The field parts are the field file's <c>pieceObstacles</c> (the HIVE frame's feet and
    /// lips, the FLOWERs' brackets and heads), the same shapes the harness's own physics uses, so both
    /// stop the robot in the same places and name the contact the same way ("HIVE frame").
    ///
    /// <para>Each loop: <see cref="DriveLaw"/> turns the wheels' command into the speed the motors
    /// and tires allow (the same rules as the harness's MomentumPhysics), PhysX moves the box one loop
    /// period at that speed and pushes it out of anything it hits, and <see cref="Step"/> returns
    /// where it ended up as the "physicsState" the harness reads. Time only moves when
    /// <see cref="Step"/> is called (the scene's physics runs by script, never by itself).</para>
    ///
    /// <para>The robot stays flat on the floor: it can't tip, climb or fall. Game pieces aren't
    /// simulated yet.</para>
    /// </summary>
    public sealed class UnityRobotPhysics : IDisposable
    {
        /// <summary>The robot's collision box height (in). Parts higher than this off the floor don't block it.</summary>
        public const double RobotHeightInches = 14;
        /// <summary>Wall height for the colliders (in). Only needs to be taller than the robot.</summary>
        private const double WallHeightInches = 24;
        /// <summary>How close to a wall (in) counts as touching it, for "wallContact".</summary>
        public const double ContactInches = 0.25;

        private readonly RobotPhysicsData robot;
        private readonly GameObject root;
        private readonly Rigidbody body;
        private RobotSpeed speed;
        private string wallContact = "";
        private string obstacleContact = "";
        // Each field-element collider and the name the harness gives it ("HIVE frame", "far FLOWER").
        private readonly Dictionary<Collider, string> obstacleNames = new Dictionary<Collider, string>();
        private readonly List<string> obstacleOrder = new List<string>();

        /// <summary>
        /// Builds the physics world: walls, the solid parts of <paramref name="field"/> (or none), and
        /// the robot at the harness's start pose, standing still.
        /// </summary>
        public UnityRobotPhysics(RobotPhysicsData robot, FieldLayoutData field)
        {
            this.robot = robot ?? throw new ArgumentNullException(nameof(robot));
            // Physics moves only when Step asks (Unity drives the harness's clock one loop at a time).
            Physics.simulationMode = SimulationMode.Script;
            root = new GameObject("Unity physics (invisible)");
            var slippery = new PhysicsMaterial("No friction")
            {
                dynamicFriction = 0, staticFriction = 0, bounciness = 0,
                frictionCombine = PhysicsMaterialCombine.Minimum, bounceCombine = PhysicsMaterialCombine.Minimum,
            };

            double half = FtcToUnity.FieldHalfWidthInches;
            const double thickness = 12;
            double length = 2 * half + 2 * thickness;
            AddSolid("Wall +x", half + thickness / 2, 0, WallHeightInches / 2, thickness, length, WallHeightInches, 0, slippery);
            AddSolid("Wall -x", -half - thickness / 2, 0, WallHeightInches / 2, thickness, length, WallHeightInches, 0, slippery);
            AddSolid("Wall +y", 0, half + thickness / 2, WallHeightInches / 2, length, thickness, WallHeightInches, 0, slippery);
            AddSolid("Wall -y", 0, -half - thickness / 2, WallHeightInches / 2, length, thickness, WallHeightInches, 0, slippery);
            if (robot.robotObstacles && field != null)
            {
                if (field.pieceObstacles.Count > 0)
                {
                    foreach (PieceObstacleData obstacle in field.pieceObstacles)
                    {
                        AddObstacle(obstacle, slippery);
                    }
                }
                else
                {
                    // A field file without pieceObstacles (format version 1): its drawn parts block instead.
                    foreach (FieldElementData part in field.elements)
                    {
                        AddFieldPart(part, slippery);
                    }
                }
            }

            var robotObject = new GameObject("Robot body");
            robotObject.transform.SetParent(root.transform, false);
            var box = robotObject.AddComponent<BoxCollider>();
            box.size = new Vector3(FtcToUnity.Meters(robot.widthInches), FtcToUnity.Meters(RobotHeightInches), FtcToUnity.Meters(robot.lengthInches));
            box.contactOffset = 0.001f;
            box.material = slippery;
            body = robotObject.AddComponent<Rigidbody>();
            body.mass = 15f;
            body.useGravity = false;
            body.collisionDetectionMode = CollisionDetectionMode.ContinuousDynamic;
            body.constraints = RigidbodyConstraints.FreezePositionY | RigidbodyConstraints.FreezeRotationX | RigidbodyConstraints.FreezeRotationZ;
            PoseData start = robot.start ?? new PoseData();
            body.position = FtcToUnity.Position(start.xInches, start.yInches, FtcToUnity.Meters(RobotHeightInches / 2));
            body.rotation = FtcToUnity.Rotation(start.headingRadians);
            robotObject.transform.SetPositionAndRotation(body.position, body.rotation);
            Physics.SyncTransforms();
        }

        /// <summary>The robot right now, as the harness's "physicsState".</summary>
        public PhysicsStateData State
        {
            get
            {
                Vector3 p = body.position;
                return new PhysicsStateData
                {
                    xInches = FtcToUnity.Inches(p.x),
                    yInches = FtcToUnity.Inches(p.z),
                    headingRadians = FtcToUnity.HeadingRadians(body.rotation.eulerAngles.y),
                    forwardInchesPerSecond = speed.forward,
                    leftInchesPerSecond = speed.left,
                    counterClockwiseRadiansPerSecond = speed.counterClockwise,
                    wallContact = wallContact,
                    obstacleContact = obstacleContact,
                };
            }
        }

        /// <summary>Runs one step of <paramref name="seconds"/> with the wheels doing <paramref name="command"/>; returns the new state.</summary>
        public PhysicsStateData Step(DriveCommandData command, double seconds)
        {
            if (seconds <= 0)
            {
                return State;
            }
            RobotSpeed before = speed;
            RobotSpeed after = DriveLaw.After(before, command ?? DriveCommandData.Stopped, seconds, robot);
            // Move at the average speed over the step, like MomentumPhysics.
            var average = new RobotSpeed((before.forward + after.forward) / 2, (before.left + after.left) / 2,
                (before.counterClockwise + after.counterClockwise) / 2);
            double heading = FtcToUnity.HeadingRadians(body.rotation.eulerAngles.y)
                + average.counterClockwise * seconds / 2; // the heading halfway through the step
            Vector3 wanted = WorldVelocity(average, heading);
            body.linearVelocity = wanted;
            body.angularVelocity = new Vector3(0f, -(float)average.counterClockwise, 0f); // Unity yaw is clockwise

            Physics.Simulate((float)seconds);

            // Where PhysX held the robot back (a wall or a HIVE leg in the way), its speed that way is
            // what PhysX left, like MomentumPhysics cancelling the speed into a wall. Elsewhere the
            // robot simply has the speed the motors gave it.
            double headingAfter = FtcToUnity.HeadingRadians(body.rotation.eulerAngles.y);
            double cos = Math.Cos(headingAfter), sin = Math.Sin(headingAfter);
            double vx = FtcToUnity.Inches(body.linearVelocity.x), vy = FtcToUnity.Inches(body.linearVelocity.z);
            double actualForward = vx * cos + vy * sin;
            double actualLeft = -vx * sin + vy * cos;
            double actualTurn = -body.angularVelocity.y; // Unity yaw is clockwise
            speed = new RobotSpeed(
                Math.Abs(actualForward - average.forward) > 0.5 ? actualForward : after.forward,
                Math.Abs(actualLeft - average.left) > 0.5 ? actualLeft : after.left,
                Math.Abs(actualTurn - average.counterClockwise) > 0.01 ? actualTurn : after.counterClockwise);
            body.linearVelocity = Vector3.zero;
            body.angularVelocity = Vector3.zero;
            wallContact = WallContact(State, robot);
            obstacleContact = ObstacleContact();
            return State;
        }

        /// <summary>
        /// The harness's name for a field element, from its id in the field file: every HIVE frame part
        /// is "HIVE frame", a FLOWER's bracket or head is "&lt;which&gt; FLOWER" ("far FLOWER", "blue-wall
        /// FLOWER"). The same rule as MomentumPhysics.displayName, so events read the same.
        /// </summary>
        public static string ObstacleName(string id)
        {
            if (id.StartsWith("frame-", StringComparison.Ordinal))
            {
                return "HIVE frame";
            }
            if (id.StartsWith("flower-", StringComparison.Ordinal))
            {
                string rest = id.Substring("flower-".Length);
                int part = Math.Max(rest.LastIndexOf("-bracket", StringComparison.Ordinal), rest.LastIndexOf("-head", StringComparison.Ordinal));
                return (part > 0 ? rest.Substring(0, part) : rest) + " FLOWER";
            }
            return id;
        }

        /// <summary>
        /// Which field elements the robot box touches now (within <see cref="ContactInches"/>), by the
        /// harness's names in the field file's order, joined by "/"; "" for none.
        /// </summary>
        private string ObstacleContact()
        {
            if (obstacleNames.Count == 0)
            {
                return "";
            }
            float margin = FtcToUnity.Meters(ContactInches);
            Vector3 half = new Vector3(FtcToUnity.Meters(robot.widthInches) / 2 + margin,
                FtcToUnity.Meters(RobotHeightInches) / 2, FtcToUnity.Meters(robot.lengthInches) / 2 + margin);
            var touched = new HashSet<string>();
            foreach (Collider hit in Physics.OverlapBox(body.position, half, body.rotation))
            {
                if (obstacleNames.TryGetValue(hit, out string name))
                {
                    touched.Add(name);
                }
            }
            return string.Join("/", obstacleOrder.FindAll(touched.Contains));
        }

        /// <summary>
        /// Which walls the robot box touches, the way MomentumPhysics names them: "+x", "-y", "+x/-y"
        /// or "". The box's reach toward each wall depends on its heading.
        /// </summary>
        public static string WallContact(PhysicsStateData s, RobotPhysicsData robot)
        {
            double cos = Math.Abs(Math.Cos(s.headingRadians)), sin = Math.Abs(Math.Sin(s.headingRadians));
            double reachX = cos * robot.lengthInches / 2 + sin * robot.widthInches / 2;
            double reachY = sin * robot.lengthInches / 2 + cos * robot.widthInches / 2;
            double limit = FtcToUnity.FieldHalfWidthInches - ContactInches;
            string x = s.xInches + reachX >= limit ? "+x" : s.xInches - reachX <= -limit ? "-x" : "";
            string y = s.yInches + reachY >= limit ? "+y" : s.yInches - reachY <= -limit ? "-y" : "";
            return x.Length > 0 && y.Length > 0 ? x + "/" + y : x + y;
        }

        /// <summary>Removes the invisible physics objects.</summary>
        public void Dispose()
        {
            if (root == null)
            {
                return;
            }
            Physics.simulationMode = SimulationMode.FixedUpdate; // Unity's normal setting again
            if (Application.isPlaying)
            {
                UnityEngine.Object.Destroy(root);
            }
            else
            {
                UnityEngine.Object.DestroyImmediate(root);
            }
        }

        /// <summary>A robot-frame speed (in/s) as a Unity world velocity (m/s) for a robot facing <paramref name="heading"/>.</summary>
        private static Vector3 WorldVelocity(RobotSpeed s, double heading)
        {
            double cos = Math.Cos(heading), sin = Math.Sin(heading);
            // The robot's "forward" points along (cos, sin) and its "left" along (-sin, cos).
            double vx = s.forward * cos - s.left * sin;
            double vy = s.forward * sin + s.left * cos;
            return FtcToUnity.Direction(FtcToUnity.Meters(vx), FtcToUnity.Meters(vy), 0);
        }

        /// <summary>
        /// A field element (pieceObstacles) as an upright collider from its bottom to its top, named so a
        /// touch can be reported. A FLOWER head (3.75 in up) still stops the 14 in tall robot box.
        /// </summary>
        private void AddObstacle(PieceObstacleData o, PhysicsMaterial material)
        {
            double heightInches = o.topInches - o.bottomInches;
            double centerInches = (o.topInches + o.bottomInches) / 2;
            Collider collider;
            if (o.shape == "cylinder")
            {
                var solid = new GameObject(o.id);
                solid.transform.SetParent(root.transform, false);
                solid.transform.localPosition = FtcToUnity.Position(o.xInches, o.yInches, FtcToUnity.Meters(centerInches));
                var capsule = solid.AddComponent<CapsuleCollider>();
                capsule.direction = 1; // upright
                capsule.radius = FtcToUnity.Meters(o.radiusInches);
                capsule.height = Mathf.Max(FtcToUnity.Meters(heightInches), 2 * capsule.radius);
                capsule.contactOffset = 0.001f;
                capsule.material = material;
                collider = capsule;
            }
            else
            {
                GameObject solid = AddSolid(o.id, (o.minXInches + o.maxXInches) / 2, (o.minYInches + o.maxYInches) / 2, centerInches,
                    o.maxXInches - o.minXInches, o.maxYInches - o.minYInches, heightInches, 0, material);
                collider = solid.GetComponent<Collider>();
            }
            string name = ObstacleName(o.id);
            obstacleNames[collider] = name;
            if (!obstacleOrder.Contains(name))
            {
                obstacleOrder.Add(name);
            }
            Physics.SyncTransforms();
        }

        /// <summary>A field part as a collider, if it is solid and low enough for the robot to hit.</summary>
        private void AddFieldPart(FieldElementData part, PhysicsMaterial material)
        {
            if (part.heightInches < 0.5)
            {
                return; // tape and painted areas: the robot drives over them
            }
            GameObject solid = part.shape == "cylinder"
                ? AddCylinder(part, material)
                : AddSolid(part.id, part.xInches, part.yInches, part.zInches, part.lengthInches, part.widthInches,
                    part.heightInches, part.tiltDegrees, material, part.headingDegrees);
            Physics.SyncTransforms();
            if (solid.GetComponent<Collider>().bounds.min.y > FtcToUnity.Meters(RobotHeightInches))
            {
                UnityEngine.Object.DestroyImmediate(solid); // above the robot (a raised CELL): it can drive under it
            }
        }

        private GameObject AddSolid(string name, double x, double y, double z, double lengthInches, double widthInches,
            double heightInches, double tiltDegrees, PhysicsMaterial material, double headingDegrees = 0)
        {
            var solid = new GameObject(name);
            solid.transform.SetParent(root.transform, false);
            solid.transform.localPosition = FtcToUnity.Position(x, y, FtcToUnity.Meters(z));
            solid.transform.localRotation = FtcToUnity.Rotation(headingDegrees * Mathf.Deg2Rad, tiltDegrees);
            var box = solid.AddComponent<BoxCollider>();
            box.size = new Vector3(FtcToUnity.Meters(widthInches), FtcToUnity.Meters(heightInches), FtcToUnity.Meters(lengthInches));
            box.contactOffset = 0.001f;
            box.material = material;
            return solid;
        }

        private GameObject AddCylinder(FieldElementData part, PhysicsMaterial material)
        {
            var solid = new GameObject(part.id);
            solid.transform.SetParent(root.transform, false);
            solid.transform.localPosition = FtcToUnity.Position(part.xInches, part.yInches, FtcToUnity.Meters(part.zInches));
            var capsule = solid.AddComponent<CapsuleCollider>();
            capsule.direction = 1; // upright
            capsule.radius = FtcToUnity.Meters(part.diameterInches / 2);
            capsule.height = FtcToUnity.Meters(part.heightInches);
            capsule.contactOffset = 0.001f;
            capsule.material = material;
            return solid;
        }
    }
}

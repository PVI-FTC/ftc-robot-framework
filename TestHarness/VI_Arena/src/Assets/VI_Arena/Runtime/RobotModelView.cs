using System;
using System.Collections.Generic;
using UnityEngine;

namespace VI_Arena
{
    /// <summary>
    /// Draws a robot's 3D model (from its <see cref="RobotProfile"/>) in place of the plain robot box, at
    /// its true size: the model is turned so its front faces the robot's heading and its top is up,
    /// scaled from its own units to meters, moved so the robot's center (the profile's
    /// <see cref="RobotProfile.Center"/>) is at the robot's position, and set down so its lowest point
    /// is on the floor. The drive wheels, when the model has them as separate parts, spin with their
    /// motors' power.
    ///
    /// <para>Only a picture: physics, recordings and the harness still use the robot box from
    /// harness.properties.</para>
    /// </summary>
    public sealed class RobotModelView
    {
        /// <summary>How fast a wheel at full power turns in the picture (degrees per second, about 300 rpm, like a goBILDA drive motor).</summary>
        public const float FullPowerDegreesPerSecond = 1800f;

        private static readonly string[] MotorNames = { "frontLeft", "frontRight", "rearLeft", "rearRight" };

        private readonly Transform holder;
        private readonly List<Transform>[] wheelPivots = { new List<Transform>(), new List<Transform>(), new List<Transform>(), new List<Transform>() };
        private double lastSeconds = double.NaN;

        private RobotModelView(Transform holder)
        {
            this.holder = holder;
        }

        /// <summary>The object holding the model under the robot (for tests).</summary>
        public Transform Holder => holder;

        /// <summary>
        /// The wheel pivots that spin with each motor, front-left, front-right, rear-left, rear-right
        /// (a list each, since a six-wheel robot turns two wheels with one motor; empty where the model
        /// has no such part).
        /// </summary>
        public List<Transform>[] WheelPivots => wheelPivots;

        /// <summary>
        /// Puts a copy of <paramref name="model"/> under <paramref name="robot"/> (the robot object, whose
        /// local +z is its front and +y up), placed as described above.
        /// </summary>
        public static RobotModelView Attach(Transform robot, RobotProfile profile, GameObject model)
        {
            var holder = new GameObject("Model " + profile.Id).transform;
            holder.SetParent(robot, false);
            holder.localRotation = profile.ModelRotation();
            holder.localScale = Vector3.one * profile.MetersPerUnit;
            GameObject copy = UnityEngine.Object.Instantiate(model, holder, false);
            copy.name = profile.Id;
            copy.transform.localPosition = -profile.Center;

            // Set it down on the floor: its lowest point at the robot's height.
            Bounds bounds = WorldBounds(copy.transform);
            holder.position += Vector3.up * (robot.position.y - bounds.min.y);

            var view = new RobotModelView(holder);
            for (int i = 0; i < 4; i++)
            {
                foreach (string name in (profile.Wheels[i] ?? "").Split(','))
                {
                    Transform wheel = name.Trim().Length == 0 ? null : FindDeep(copy.transform, name.Trim());
                    if (wheel != null)
                    {
                        view.wheelPivots[i].Add(MakePivot(wheel, robot));
                    }
                }
            }
            return view;
        }

        /// <summary>
        /// The model's footprint seen from above, in inches: length (front to back) and width (side to
        /// side). For comparing with the robot box in harness.properties.
        /// </summary>
        public Vector2 FootprintInches()
        {
            Transform robot = holder.parent;
            Bounds world = WorldBounds(holder);
            Vector3[] corners =
            {
                new Vector3(world.min.x, 0, world.min.z), new Vector3(world.max.x, 0, world.min.z),
                new Vector3(world.min.x, 0, world.max.z), new Vector3(world.max.x, 0, world.max.z),
            };
            float minX = float.MaxValue, maxX = float.MinValue, minZ = float.MaxValue, maxZ = float.MinValue;
            foreach (Vector3 c in corners)
            {
                Vector3 local = robot.InverseTransformPoint(c);
                minX = Mathf.Min(minX, local.x); maxX = Mathf.Max(maxX, local.x);
                minZ = Mathf.Min(minZ, local.z); maxZ = Mathf.Max(maxZ, local.z);
            }
            return new Vector2((float)FtcToUnity.Inches(maxZ - minZ), (float)FtcToUnity.Inches(maxX - minX));
        }

        /// <summary>
        /// Turns each drive wheel by its motor's power over the recorded time since the last call
        /// (<paramref name="sinceInitSeconds"/>), so a paused replay doesn't spin and a jump in the
        /// slider doesn't spin either.
        /// </summary>
        public void Spin(LoopData loop, double sinceInitSeconds)
        {
            double step = sinceInitSeconds - lastSeconds;
            lastSeconds = sinceInitSeconds;
            if (loop == null || double.IsNaN(step) || step <= 0 || step > 0.25)
            {
                return;
            }
            for (int i = 0; i < 4; i++)
            {
                float degrees = (float)(FieldBuilder.MotorPower(loop, MotorNames[i]) * FullPowerDegreesPerSecond * step);
                foreach (Transform pivot in wheelPivots[i])
                {
                    // Forward power rolls the wheel forward: around the robot's right (+x) axis.
                    pivot.localRotation *= Quaternion.Euler(degrees, 0f, 0f);
                }
            }
        }

        /// <summary>Everything drawn under <paramref name="root"/>, in world space.</summary>
        public static Bounds WorldBounds(Transform root)
        {
            Renderer[] renderers = root.GetComponentsInChildren<Renderer>();
            if (renderers.Length == 0)
            {
                return new Bounds(root.position, Vector3.zero);
            }
            Bounds b = renderers[0].bounds;
            foreach (Renderer r in renderers)
            {
                b.Encapsulate(r.bounds);
            }
            return b;
        }

        /// <summary>
        /// A pivot at the wheel's middle, lined up with the robot, holding the wheel, so turning the
        /// pivot about its x axis spins the wheel on its axle.
        /// </summary>
        private static Transform MakePivot(Transform wheel, Transform robot)
        {
            var pivot = new GameObject(wheel.name + " (spins)").transform;
            pivot.SetParent(wheel.parent, false);
            pivot.position = WorldBounds(wheel).center;
            pivot.rotation = robot.rotation;
            wheel.SetParent(pivot, true);
            return pivot;
        }

        private static Transform FindDeep(Transform root, string name)
        {
            if (string.Equals(root.name, name, StringComparison.Ordinal))
            {
                return root;
            }
            foreach (Transform child in root)
            {
                Transform found = FindDeep(child, name);
                if (found != null)
                {
                    return found;
                }
            }
            return null;
        }
    }
}

using System;
using UnityEngine;

namespace VI_Arena
{
    /// <summary>
    /// The ONE place that turns harness (FTC) units into Unity units. Every other script calls
    /// this; none converts on its own (TestHarness/docs/ai-instructions/02-unity-readiness.md §6).
    ///
    /// <para>FTC field frame (the harness): inches, origin at the field center, x and y from -72
    /// to +72, z up, heading counter-clockwise from +x when seen from above.</para>
    ///
    /// <para>Unity: meters, y up, left-handed, yaw (rotation around y) clockwise from +z when
    /// seen from above.</para>
    ///
    /// <code>
    ///   FTC x inches  ->  Unity x = x * 0.0254 m
    ///   FTC y inches  ->  Unity z = y * 0.0254 m
    ///   FTC height    ->  Unity y
    ///   FTC heading   ->  Unity yaw = 90 deg - heading in degrees
    /// </code>
    ///
    /// Seen from above, both have x to the right and FTC +y / Unity +z away from you, so the
    /// field is not mirrored. Heading 0 (facing FTC +x) is Unity yaw 90 (facing +x); heading 90
    /// (facing FTC +y) is yaw 0 (facing +z).
    /// </summary>
    public static class FtcToUnity
    {
        /// <summary>Exactly 0.0254 meters in an inch.</summary>
        public const float MetersPerInch = 0.0254f;
        /// <summary>The field reaches 72 inches from its center in every direction.</summary>
        public const float FieldHalfWidthInches = 72f;
        /// <summary>The whole field is 144 inches (12 ft, 3.6576 m) across.</summary>
        public const float FieldSizeMeters = 2f * FieldHalfWidthInches * MetersPerInch;

        /// <summary>A length in inches as meters.</summary>
        public static float Meters(double inches)
        {
            return (float)(inches * MetersPerInch);
        }

        /// <summary>A field position (FTC inches) as a Unity position, at a height in meters above the floor.</summary>
        public static Vector3 Position(double xInches, double yInches, float heightMeters = 0f)
        {
            return new Vector3(Meters(xInches), heightMeters, Meters(yInches));
        }

        /// <summary>An FTC heading (radians, counter-clockwise from +x) as a Unity yaw in degrees (clockwise from +z).</summary>
        public static float YawDegrees(double headingRadians)
        {
            return 90f - (float)(headingRadians * Mathf.Rad2Deg);
        }

        /// <summary>The Unity rotation for a robot with this FTC heading: its local +z (forward) points the way it faces.</summary>
        public static Quaternion Rotation(double headingRadians)
        {
            return Quaternion.Euler(0f, YawDegrees(headingRadians), 0f);
        }

        /// <summary>
        /// The rotation for a field part (from a season field file): tilted first (positive raises
        /// the part's +x end, its local +z here), then turned to the FTC heading.
        /// </summary>
        public static Quaternion Rotation(double headingRadians, double tiltDegrees)
        {
            return Rotation(headingRadians) * Quaternion.Euler(-(float)tiltDegrees, 0f, 0f);
        }

        /// <summary>
        /// The rotation for a 3D model whose points are in the FTC field frame (x, y, z up), such as
        /// the field CAD. Unity's OBJ importer reads a point (x, y, z) as Unity (-x, y, z); this turns
        /// that into Unity (x, z, y), the same mapping as <see cref="Position"/>. Scale the model by
        /// <see cref="MetersPerInch"/> when its points are in inches.
        /// </summary>
        public static readonly Quaternion FtcModelRotation = Quaternion.LookRotation(new Vector3(0f, 1f, 0f), new Vector3(0f, 0f, 1f));

        /// <summary>Meters as inches (the other way from <see cref="Meters"/>).</summary>
        public static double Inches(double meters)
        {
            return meters / MetersPerInch;
        }

        /// <summary>A Unity yaw (degrees, clockwise from +z) as an FTC heading (radians, counter-clockwise from +x), -π to π.</summary>
        public static double HeadingRadians(float yawDegrees)
        {
            double heading = (90.0 - yawDegrees) * Mathf.Deg2Rad;
            return Math.Atan2(Math.Sin(heading), Math.Cos(heading));
        }

        /// <summary>An FTC direction (x, y, z up) as a Unity direction (x, y up, z). Not scaled.</summary>
        public static Vector3 Direction(double x, double y, double z)
        {
            return new Vector3((float)x, (float)z, (float)y);
        }

        /// <summary>A recorded pose as a Unity position (on the floor, or at a height) and rotation.</summary>
        public static void Pose(PoseData pose, float heightMeters, out Vector3 position, out Quaternion rotation)
        {
            position = Position(pose.xInches, pose.yInches, heightMeters);
            rotation = Rotation(pose.headingRadians);
        }
    }
}

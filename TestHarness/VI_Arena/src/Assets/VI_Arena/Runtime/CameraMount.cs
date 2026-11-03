using System;
using System.Collections.Generic;
using UnityEngine;

namespace VI_Arena
{
    /// <summary>
    /// Where one webcam sits on the robot and how it sees, worked out from the recording's
    /// <see cref="CameraData"/>: its position and three directions in the robot's frame (x right,
    /// y forward, z up, inches), its picture size and its lens. Used to place the camera picture's
    /// Unity camera, to start the AprilTag sight lines at the lens, and to mark each detected tag on
    /// the picture.
    ///
    /// <para>The recording gives the mount in the harness's robot frame (x forward, y left, z up, from
    /// the robot's center on the floor) with angles where all 0 is level and looking forward: yaw +
    /// turns the lens left, then pitch + tilts it up, then roll + turns the picture clockwise as seen
    /// from behind the camera, each about the camera's own axes. Here the robot frame is kept as
    /// right, forward, up (like ftcPose), so left becomes minus right.</para>
    /// </summary>
    public sealed class CameraMount
    {
        /// <summary>
        /// Used when the recording gives no field of view: the FTC SDK's built-in calibration for a
        /// Logitech C920 at 640 x 480 (focal length 622 px, about 54° wide).
        /// </summary>
        public const double PlaceholderFx = 622.001;
        public const int PlaceholderWidth = 640, PlaceholderHeight = 480;

        /// <summary>The camera setup's fields that say where the camera sits and which way it looks.</summary>
        private static readonly string[] MountFields =
            { "forwardInches", "leftInches", "upInches", "yawDegrees", "pitchDegrees", "rollDegrees" };

        /// <summary>The webcam's configuration name, e.g. "logitechVisionWebcam".</summary>
        public string Name { get; private set; }
        /// <summary>True if the recording gave the mount; false means the robot's center, at the old sight-line height, facing forward.</summary>
        public bool MountKnown { get; private set; }
        /// <summary>True if the mount's position and angles are measured (none listed in the recording's placeholders).</summary>
        public bool MountMeasured { get; private set; }
        /// <summary>True if the field of view is measured (not a placeholder and not the C920 fallback).</summary>
        public bool LensKnown { get; private set; }
        /// <summary>How far away the camera can still read a tag (inches), or 0 if not given.</summary>
        public double MaxRangeInches { get; private set; }
        /// <summary>How far a tag can be turned away from the camera and still be read (degrees), or 0 if not given.</summary>
        public double MaxViewAngleDegrees { get; private set; }
        /// <summary>The lens's place on the robot: right, forward, up (inches; up from the floor).</summary>
        public Vector3 PositionInches { get; private set; }
        /// <summary>The picture's right, in the robot's frame.</summary>
        public Vector3 Right { get; private set; }
        /// <summary>The way the camera looks, in the robot's frame.</summary>
        public Vector3 Look { get; private set; }
        /// <summary>The picture's up, in the robot's frame.</summary>
        public Vector3 Up { get; private set; }
        public int WidthPixels { get; private set; }
        public int HeightPixels { get; private set; }
        public double Fx { get; private set; }
        public double Fy { get; private set; }
        public double Cx { get; private set; }
        public double Cy { get; private set; }

        /// <summary>A webcam's mount and lens from the recording's camera setup.</summary>
        public static CameraMount From(CameraData data)
        {
            List<string> guesses = data.placeholders ?? new List<string>();
            bool hasFov = data.horizontalFovDegrees > 0;
            var m = new CameraMount
            {
                Name = data.name,
                MountKnown = true,
                MountMeasured = !guesses.Exists(g => Array.IndexOf(MountFields, g) >= 0),
                LensKnown = hasFov && !guesses.Contains("horizontalFovDegrees") && !guesses.Contains("verticalFovDegrees"),
                MaxRangeInches = data.maxRangeInches,
                MaxViewAngleDegrees = data.maxViewAngleDegrees,
                PositionInches = new Vector3((float)-data.leftInches, (float)data.forwardInches, (float)data.upInches),
                WidthPixels = data.imageWidthPixels > 0 ? data.imageWidthPixels : PlaceholderWidth,
                HeightPixels = data.imageHeightPixels > 0 ? data.imageHeightPixels : PlaceholderHeight,
            };
            // Focal lengths from the field of view; without one, the C920's scaled to the picture size.
            m.Fx = hasFov ? m.WidthPixels / 2.0 / Math.Tan(data.horizontalFovDegrees * Math.PI / 360)
                : PlaceholderFx * m.WidthPixels / PlaceholderWidth;
            m.Fy = data.verticalFovDegrees > 0 ? m.HeightPixels / 2.0 / Math.Tan(data.verticalFovDegrees * Math.PI / 360) : m.Fx;
            m.Cx = m.WidthPixels / 2.0;
            m.Cy = m.HeightPixels / 2.0;
            m.Turn(data.yawDegrees, data.pitchDegrees, data.rollDegrees);
            return m;
        }

        /// <summary>The setup for the webcam called <paramref name="name"/>, or null if the recording has none.</summary>
        public static CameraMount Find(List<CameraData> cameras, string name)
        {
            CameraData data = cameras?.Find(c => c.name == name);
            return data == null ? null : From(data);
        }

        /// <summary>A camera at the robot's center, at the old sight-line height, facing forward: for recordings without camera setups.</summary>
        public static CameraMount Unknown(string name)
        {
            CameraMount m = From(new CameraData { name = name, upInches = AprilTagSightView.CameraHeightInches });
            m.MountKnown = false;
            m.MountMeasured = false;
            return m;
        }

        /// <summary>
        /// Where a detected tag is in the robot's frame (inches): the lens position plus the tag's
        /// ftcPose (right, forward, up from the lens) along the picture's right, view and up.
        /// </summary>
        public Vector3 TagInRobotInches(AprilTagSeenData tag)
        {
            return PositionInches + Right * (float)tag.rightInches + Look * (float)tag.forwardInches + Up * (float)tag.upInches;
        }

        /// <summary>A point in the robot's frame (inches) moved to the FTC field frame (x, y inches; z up from the tiles).</summary>
        public static Vector3 RobotToFieldInches(PoseData robot, Vector3 inRobot)
        {
            double cos = Math.Cos(robot.headingRadians), sin = Math.Sin(robot.headingRadians);
            // Forward is (cos, sin); right is (sin, -cos).
            double x = robot.xInches + inRobot.y * cos + inRobot.x * sin;
            double y = robot.yInches + inRobot.y * sin - inRobot.x * cos;
            return new Vector3((float)x, (float)y, inRobot.z);
        }

        /// <summary>
        /// A point on the field (FTC frame inches, z up from the tiles) as this camera would measure it
        /// with the robot at <paramref name="robot"/>: right, forward and up from the lens, like ftcPose.
        /// </summary>
        public AprilTagSeenData SeenFrom(PoseData robot, Vector3 fieldInches, int id)
        {
            double cos = Math.Cos(robot.headingRadians), sin = Math.Sin(robot.headingRadians);
            double dx = fieldInches.x - robot.xInches, dy = fieldInches.y - robot.yInches;
            // The inverse of RobotToFieldInches: forward is (cos, sin), right is (sin, -cos).
            var inRobot = new Vector3((float)(dx * sin - dy * cos), (float)(dx * cos + dy * sin), fieldInches.z);
            Vector3 fromLens = inRobot - PositionInches;
            return new AprilTagSeenData
            {
                camera = Name, id = id,
                rightInches = Vector3.Dot(fromLens, Right),
                forwardInches = Vector3.Dot(fromLens, Look),
                upInches = Vector3.Dot(fromLens, Up),
            };
        }

        /// <summary>The lens's place under the Unity robot object (meters; Unity +z is the robot's front).</summary>
        public Vector3 UnityLocalPosition => ToUnity(PositionInches) * (float)FtcToUnity.Meters(1);

        /// <summary>The turn for a Unity camera under the robot object so it looks like the webcam.</summary>
        public Quaternion UnityLocalRotation => Quaternion.LookRotation(ToUnity(Look), ToUnity(Up));

        /// <summary>The picture's height seen as an angle (degrees), for Unity's Camera.fieldOfView.</summary>
        public float VerticalFovDegrees => (float)(2 * Math.Atan(HeightPixels / 2.0 / Fy) * 180 / Math.PI);

        /// <summary>Width over height of the picture.</summary>
        public float Aspect => WidthPixels / (float)HeightPixels;

        /// <summary>
        /// Where a detected tag's center lands in the picture, in pixels from the top-left corner, as a
        /// pinhole camera with this lens would put it. False when the tag isn't in front of the lens.
        /// </summary>
        public bool Project(AprilTagSeenData tag, out Vector2 pixel)
        {
            pixel = Vector2.zero;
            if (tag.forwardInches <= 0)
            {
                return false;
            }
            pixel = new Vector2((float)(Cx + Fx * tag.rightInches / tag.forwardInches),
                (float)(Cy - Fy * tag.upInches / tag.forwardInches));
            return true;
        }

        /// <summary>How many pixels across a tag <paramref name="sizeInches"/> wide looks, face on, at the tag's distance ahead.</summary>
        public float PixelsAcross(AprilTagSeenData tag, double sizeInches)
        {
            return tag.forwardInches <= 0 ? 0f : (float)(Fx * sizeInches / tag.forwardInches);
        }

        /// <summary>Robot frame (right, forward, up) to Unity's robot-local axes (right, up, forward).</summary>
        private static Vector3 ToUnity(Vector3 v) => new Vector3(v.x, v.z, v.y);

        /// <summary>
        /// Sets Right, Look and Up (robot frame: right, forward, up). Starts level and looking forward,
        /// then turns by yaw about up (left is +), pitch about the camera's right (up is +) and roll
        /// about its view (clockwise from behind is +).
        /// </summary>
        private void Turn(double yawDegrees, double pitchDegrees, double rollDegrees)
        {
            double yaw = yawDegrees * Math.PI / 180, pitch = pitchDegrees * Math.PI / 180, roll = rollDegrees * Math.PI / 180;
            Vector3 look = new Vector3((float)-Math.Sin(yaw), (float)Math.Cos(yaw), 0f);
            Vector3 right = new Vector3((float)Math.Cos(yaw), (float)Math.Sin(yaw), 0f);
            Vector3 up = new Vector3(0f, 0f, 1f);
            Vector3 pitchedLook = look * (float)Math.Cos(pitch) + up * (float)Math.Sin(pitch);
            Vector3 pitchedUp = up * (float)Math.Cos(pitch) - look * (float)Math.Sin(pitch);
            Look = pitchedLook;
            Up = pitchedUp * (float)Math.Cos(roll) + right * (float)Math.Sin(roll);
            Right = right * (float)Math.Cos(roll) - pitchedUp * (float)Math.Sin(roll);
        }
    }
}

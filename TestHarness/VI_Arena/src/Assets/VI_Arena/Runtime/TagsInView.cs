using System;
using System.Collections.Generic;
using UnityEngine;

namespace VI_Arena
{
    /// <summary>
    /// One of the shown field's AprilTags that a webcam can see from where the robot is, worked out from
    /// the field file and the webcam's mount. It doesn't need the robot's code to run a vision
    /// processor, so the webcam picture can mark every tag in view, also in a TeleOp that never looks
    /// for tags.
    ///
    /// <para>A tag counts when its center is on the picture (a tag cut off by the picture's edge still
    /// counts), it is within the webcam's range, and its printed side is turned toward the lens by no
    /// more than the webcam's view angle. The range and view angle are the harness's own limits
    /// (CameraView.java), which also asks for all four corners on the picture before it "detects" a
    /// tag.</para>
    ///
    /// <para><b>Not handled:</b> anything blocking the view (the robot's body, the HIVE's frame, game
    /// pieces), the same as in the harness.</para>
    /// </summary>
    public sealed class TagsInView
    {
        /// <summary>The tag's id.</summary>
        public int Id { get; private set; }
        /// <summary>Where the printed tag's four corners land in the picture, in the webcam's pixels from the top-left corner, in order around the tag.</summary>
        public Vector2[] Corners { get; private set; }

        /// <summary>
        /// The tags of <paramref name="fieldTags"/> in view of <paramref name="mount"/> with the robot at
        /// <paramref name="robot"/>, leaving out the ids in <paramref name="detectedIds"/> (those already
        /// have the box of a detection).
        /// </summary>
        public static List<TagsInView> Find(CameraMount mount, PoseData robot, IReadOnlyList<FieldTagData> fieldTags, ICollection<int> detectedIds)
        {
            var found = new List<TagsInView>();
            if (mount == null || robot == null || fieldTags == null)
            {
                return found;
            }
            foreach (FieldTagData tag in fieldTags)
            {
                if (tag.HasFieldPose && (detectedIds == null || !detectedIds.Contains(tag.id)) && InView(mount, robot, tag, out Vector2[] corners))
                {
                    found.Add(new TagsInView { Id = tag.id, Corners = corners });
                }
            }
            return found;
        }

        /// <summary>
        /// The rectangle around the corners on <paramref name="picture"/> (on screen, y down), at least
        /// <paramref name="minimumSide"/> each way so a far-off tag still shows.
        /// </summary>
        public Rect BoxOn(CameraMount mount, Rect picture, float minimumSide)
        {
            Vector2 low = Corners[0], high = Corners[0];
            foreach (Vector2 corner in Corners)
            {
                low = Vector2.Min(low, corner);
                high = Vector2.Max(high, corner);
            }
            float scale = picture.width / mount.WidthPixels;
            Vector2 center = picture.position + (low + high) / 2f * scale;
            float width = Mathf.Max(minimumSide, (high.x - low.x) * scale), height = Mathf.Max(minimumSide, (high.y - low.y) * scale);
            return new Rect(center.x - width / 2f, center.y - height / 2f, width, height);
        }

        private static bool InView(CameraMount mount, PoseData robot, FieldTagData tag, out Vector2[] corners)
        {
            corners = null;
            var center = new Vector3((float)tag.xInches.Value, (float)tag.yInches.Value, (float)tag.zInches.Value);
            var facing = new Vector3((float)tag.facing.x, (float)tag.facing.y, (float)tag.facing.z);
            Vector3 toLens = CameraMount.RobotToFieldInches(robot, mount.PositionInches) - center;
            float range = toLens.magnitude;
            if (facing.sqrMagnitude < 1e-6f || range < 1e-3f || (mount.MaxRangeInches > 0 && range > mount.MaxRangeInches))
            {
                return false;
            }
            facing.Normalize();
            double maxAngle = mount.MaxViewAngleDegrees > 0 ? mount.MaxViewAngleDegrees : 90;
            if (Vector3.Dot(facing, toLens) / range < (float)Math.Cos(maxAngle * Math.PI / 180))
            {
                return false;
            }
            if (!mount.Project(mount.SeenFrom(robot, center, tag.id), out Vector2 middle)
                || middle.x < 0 || middle.x > mount.WidthPixels || middle.y < 0 || middle.y > mount.HeightPixels)
            {
                return false;
            }

            // The printed tag with its white edge, its top toward the sky, as FieldBuilder.AddTag draws it.
            Vector3 up = Mathf.Abs(facing.z) > 0.95f ? Vector3.up : Vector3.forward;
            Vector3 across = Vector3.Cross(up, facing).normalized;
            Vector3 tall = Vector3.Cross(facing, across);
            float half = (float)(tag.sizeInches * AprilTag36h11.Cells / 8.0 / 2.0);
            corners = new Vector2[4];
            for (int i = 0; i < 4; i++)
            {
                Vector3 corner = center + across * (i == 0 || i == 3 ? -half : half) + tall * (i < 2 ? half : -half);
                if (!mount.Project(mount.SeenFrom(robot, corner, tag.id), out corners[i]))
                {
                    corners = null;
                    return false;
                }
            }
            return true;
        }
    }
}

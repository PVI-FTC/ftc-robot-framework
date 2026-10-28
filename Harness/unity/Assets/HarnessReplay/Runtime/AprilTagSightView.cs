using System;
using System.Collections.Generic;
using UnityEngine;

namespace PviHarness.Replay
{
    /// <summary>
    /// Shows what the robot's webcams saw at the moment shown: a thin line from the robot to each
    /// AprilTag in the recording's loop (format version 3 "aprilTags"), a small marker where the
    /// camera put the tag, and its id. The same tags the "Tags in view" card lists.
    ///
    /// <para>The tag's place comes from what the camera measured (ftcPose: right, forward, up from the
    /// lens), turned into the field with the robot's pose. Where the webcam sits on the robot isn't in
    /// the recording or the config, so the lines start at the middle of the robot's top
    /// (<see cref="CameraHeightInches"/>), facing the robot's front, rather than at a made-up mount.</para>
    /// </summary>
    public sealed class AprilTagSightView
    {
        /// <summary>Where the lines start, above the floor: the top of the drawn robot box (the mount isn't recorded).</summary>
        public static readonly double CameraHeightInches =
            FtcToUnity.Inches(FieldBuilder.RobotClearanceMeters + FieldBuilder.RobotHeightMeters);
        /// <summary>The sight-line color: the HUD accent, a little see-through so it stays subtle.</summary>
        public static readonly Color LineColor = new Color(HudTheme.Accent.r, HudTheme.Accent.g, HudTheme.Accent.b, 0.75f);

        private readonly Transform root;
        private readonly List<Transform> sights = new List<Transform>();
        private readonly Material material;

        /// <summary>Makes an empty "AprilTag sight lines" object under <paramref name="parent"/>.</summary>
        public AprilTagSightView(Transform parent)
        {
            root = new GameObject("AprilTag sight lines").transform;
            root.SetParent(parent, false);
            material = new Material(Shader.Find("Unlit/Color")) { color = LineColor };
        }

        /// <summary>The object holding the lines (for tests).</summary>
        public Transform Root => root;

        /// <summary>
        /// Where the camera put a tag, in the FTC field frame (x, y inches; z = inches above the tiles),
        /// for a robot at <paramref name="robot"/> with its camera at the robot's center facing forward.
        /// </summary>
        public static Vector3 TagPositionInches(PoseData robot, AprilTagSeenData tag)
        {
            double cos = Math.Cos(robot.headingRadians), sin = Math.Sin(robot.headingRadians);
            // Forward is (cos, sin); right is (sin, -cos).
            double x = robot.xInches + tag.forwardInches * cos + tag.rightInches * sin;
            double y = robot.yInches + tag.forwardInches * sin - tag.rightInches * cos;
            return new Vector3((float)x, (float)y, (float)(CameraHeightInches + tag.upInches));
        }

        /// <summary>
        /// Shows <paramref name="loop"/>'s tags for a robot at <paramref name="robot"/>, or nothing when
        /// <paramref name="visible"/> is false, there's no pose, or no tag was in view.
        /// </summary>
        public void Show(LoopData loop, PoseData robot, bool visible)
        {
            int count = visible && robot != null && loop != null ? loop.aprilTags.Count : 0;
            while (sights.Count < count)
            {
                sights.Add(MakeSight());
            }
            for (int i = 0; i < sights.Count; i++)
            {
                bool used = i < count;
                sights[i].gameObject.SetActive(used);
                if (!used)
                {
                    continue;
                }
                AprilTagSeenData tag = loop.aprilTags[i];
                Vector3 camera = FtcToUnity.Position(robot.xInches, robot.yInches, FtcToUnity.Meters(CameraHeightInches));
                Vector3 at = TagPositionInches(robot, tag);
                Vector3 seen = FtcToUnity.Position(at.x, at.y, FtcToUnity.Meters(at.z));
                var line = sights[i].GetComponent<LineRenderer>();
                line.SetPosition(0, camera);
                line.SetPosition(1, seen);
                Transform marker = sights[i].Find("Marker");
                marker.position = seen;
                Transform label = sights[i].Find("Label");
                label.position = seen + Vector3.up * FtcToUnity.Meters(2.5);
                label.GetComponent<TextMesh>().text = tag.id.ToString();
                sights[i].name = "Tag " + tag.id;
            }
        }

        /// <summary>Removes the lines.</summary>
        public void Destroy()
        {
            if (Application.isPlaying)
            {
                UnityEngine.Object.Destroy(root.gameObject);
            }
            else
            {
                UnityEngine.Object.DestroyImmediate(root.gameObject);
            }
        }

        /// <summary>One sight: a line, a small square marker where the tag is, and its id.</summary>
        private Transform MakeSight()
        {
            var sight = new GameObject("Tag").transform;
            sight.SetParent(root, false);
            var line = sight.gameObject.AddComponent<LineRenderer>();
            line.useWorldSpace = true;
            line.positionCount = 2;
            line.widthMultiplier = FtcToUnity.Meters(0.35);
            line.sharedMaterial = material;
            line.shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.Off;

            GameObject marker = GameObject.CreatePrimitive(PrimitiveType.Cube);
            marker.name = "Marker";
            if (Application.isPlaying)
            {
                UnityEngine.Object.Destroy(marker.GetComponent<Collider>());
            }
            else
            {
                UnityEngine.Object.DestroyImmediate(marker.GetComponent<Collider>());
            }
            marker.transform.SetParent(sight, false);
            marker.transform.localScale = Vector3.one * FtcToUnity.Meters(1.5);
            Renderer renderer = marker.GetComponent<Renderer>();
            renderer.sharedMaterial = material;
            renderer.shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.Off;

            var label = new GameObject("Label").transform;
            label.SetParent(sight, false);
            var text = label.gameObject.AddComponent<TextMesh>();
            text.anchor = TextAnchor.LowerCenter;
            text.characterSize = 0.02f;
            text.fontSize = 48;
            text.color = LineColor;
            // A TextMesh needs a font in the built app; Unity's built-in one is always there.
            text.font = Resources.GetBuiltinResource<Font>("LegacyRuntime.ttf");
            label.GetComponent<MeshRenderer>().sharedMaterial = text.font.material;
            return sight;
        }
    }
}

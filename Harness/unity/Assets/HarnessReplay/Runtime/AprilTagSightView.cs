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
    /// lens), turned into the field with the webcam's mount (<see cref="CameraMount"/>, from the
    /// recording's camera setups, version 5) and the robot's pose. The line starts at the lens. A
    /// recording without camera setups starts the lines at the middle of the robot's top
    /// (<see cref="CameraHeightInches"/>), facing the robot's front, rather than at a made-up mount.</para>
    ///
    /// <para>What each line means (<see cref="Target"/>): a solid cyan line ends where the camera
    /// measured a tag of the shown field, within <see cref="OnTagToleranceInches"/> of it (automatic
    /// mode always is). A solid amber line goes to the real tag when the camera measured it somewhere
    /// else. A tag id the shown field doesn't have (a scripted detection, e.g. DECODE ids on BIOBUZZ,
    /// or any tag on the plain field) gets a dashed amber line to where it was measured, labelled
    /// "22 · not on this field", so nobody takes it for a bug.</para>
    /// </summary>
    public sealed class AprilTagSightView
    {
        /// <summary>Where the lines start, above the floor, when the mount isn't recorded: the top of the drawn robot box.</summary>
        public static readonly double CameraHeightInches =
            FtcToUnity.Inches(FieldBuilder.RobotClearanceMeters + FieldBuilder.RobotHeightMeters);
        /// <summary>The sight-line color: the HUD accent, a little see-through so it stays subtle.</summary>
        public static readonly Color LineColor = new Color(HudTheme.Accent.r, HudTheme.Accent.g, HudTheme.Accent.b, 0.75f);
        /// <summary>The color of a line to a tag the camera measured somewhere else: amber.</summary>
        public static readonly Color OffTagColor = new Color(HudTheme.Amber.r, HudTheme.Amber.g, HudTheme.Amber.b, 0.85f);
        /// <summary>
        /// The Unity layer the sight lines are drawn on, so the webcam picture (<see cref="CameraPictureView"/>)
        /// can leave them out: they're a drawing for people, not something the camera would see.
        /// </summary>
        public const int Layer = 31;
        /// <summary>How close a measured tag must be to the real one to count as on it (inches).</summary>
        public const double OnTagToleranceInches = 4.0;

        /// <summary>Where a detection's line or picture box goes, if anywhere.</summary>
        public enum TargetKind
        {
            /// <summary>The shown field has no tag with this id: drawn dashed, where the camera measured it, and labelled.</summary>
            NotOnField,
            /// <summary>Drawn where the camera measured it, which is on the real tag.</summary>
            OnTag,
            /// <summary>Drawn at the real tag, in amber: the camera measured it somewhere else (scripted, or wrong).</summary>
            MeasuredElsewhere,
        }

        private readonly Transform root;
        private readonly List<Transform> sights = new List<Transform>();
        private readonly Material material;
        private readonly Material offTagMaterial;
        private readonly Material dashedMaterial;

        /// <summary>Makes an empty "AprilTag sight lines" object under <paramref name="parent"/>.</summary>
        public AprilTagSightView(Transform parent)
        {
            root = new GameObject("AprilTag sight lines").transform;
            root.SetParent(parent, false);
            material = new Material(Shader.Find("Unlit/Color")) { color = LineColor };
            offTagMaterial = new Material(Shader.Find("Unlit/Color")) { color = OffTagColor };
            dashedMaterial = DashedMaterial();
        }

        /// <summary>The object holding the lines (for tests).</summary>
        public Transform Root => root;

        /// <summary>
        /// Where the camera put a tag, in the FTC field frame (x, y inches; z = inches above the tiles),
        /// for a robot at <paramref name="robot"/> with its camera at the robot's center facing forward.
        /// </summary>
        public static Vector3 TagPositionInches(PoseData robot, AprilTagSeenData tag)
        {
            return TagPositionInches(robot, tag, CameraMount.Unknown(tag.camera));
        }

        /// <summary>Where the camera put a tag, in the FTC field frame, for a webcam on <paramref name="mount"/>.</summary>
        public static Vector3 TagPositionInches(PoseData robot, AprilTagSeenData tag, CameraMount mount)
        {
            return CameraMount.RobotToFieldInches(robot, mount.TagInRobotInches(tag));
        }

        /// <summary>
        /// Where to draw a detection: the measured spot when it is on the field's tag with that id,
        /// the field's tag when the measurement is somewhere else, or nowhere when the field has no
        /// such tag. <paramref name="fieldInches"/> is the spot to draw (FTC field frame, z up).
        /// </summary>
        public static TargetKind Target(PoseData robot, AprilTagSeenData tag, CameraMount mount,
            IReadOnlyList<FieldTagData> fieldTags, out Vector3 fieldInches)
        {
            fieldInches = TagPositionInches(robot, tag, mount);
            FieldTagData real = null;
            if (fieldTags != null)
            {
                foreach (FieldTagData t in fieldTags)
                {
                    if (t.id == tag.id && t.HasFieldPose)
                    {
                        real = t;
                        break;
                    }
                }
            }
            if (real == null)
            {
                return TargetKind.NotOnField;
            }
            var at = new Vector3((float)real.xInches.Value, (float)real.yInches.Value, (float)real.zInches.Value);
            if (Vector3.Distance(at, fieldInches) <= OnTagToleranceInches)
            {
                return TargetKind.OnTag;
            }
            fieldInches = at;
            return TargetKind.MeasuredElsewhere;
        }

        /// <summary>
        /// Shows <paramref name="loop"/>'s tags for a robot at <paramref name="robot"/>, or nothing when
        /// <paramref name="visible"/> is false, there's no pose, or no tag was in view. Each line starts
        /// at its webcam's lens from <paramref name="cameras"/> (the recording's camera setups), or at
        /// the robot's top when that webcam has none, and goes only to tags in
        /// <paramref name="fieldTags"/> (the shown field's tags; see <see cref="Target"/>).
        /// </summary>
        public void Show(LoopData loop, PoseData robot, bool visible, List<CameraData> cameras, IReadOnlyList<FieldTagData> fieldTags)
        {
            var drawn = new List<(AprilTagSeenData tag, CameraMount mount, Vector3 at, TargetKind kind)>();
            if (visible && robot != null && loop != null)
            {
                foreach (AprilTagSeenData tag in loop.aprilTags)
                {
                    CameraMount mount = CameraMount.Find(cameras, tag.camera) ?? CameraMount.Unknown(tag.camera);
                    TargetKind kind = Target(robot, tag, mount, fieldTags, out Vector3 spot);
                    drawn.Add((tag, mount, spot, kind));
                }
            }
            int count = drawn.Count;
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
                AprilTagSeenData tag = drawn[i].tag;
                CameraMount mount = drawn[i].mount;
                Vector3 lens = CameraMount.RobotToFieldInches(robot, mount.PositionInches);
                Vector3 camera = FtcToUnity.Position(lens.x, lens.y, FtcToUnity.Meters(lens.z));
                Vector3 at = drawn[i].at;
                Vector3 seen = FtcToUnity.Position(at.x, at.y, FtcToUnity.Meters(at.z));
                var line = sights[i].GetComponent<LineRenderer>();
                line.SetPosition(0, camera);
                line.SetPosition(1, seen);
                Transform marker = sights[i].Find("Marker");
                marker.position = seen;
                Transform label = sights[i].Find("Label");
                label.position = seen + Vector3.up * FtcToUnity.Meters(2.5);
                TargetKind kind = drawn[i].kind;
                bool amber = kind != TargetKind.OnTag;
                label.GetComponent<TextMesh>().text = LabelText(tag.id, kind);
                label.GetComponent<TextMesh>().color = amber ? OffTagColor : LineColor;
                line.sharedMaterial = kind == TargetKind.NotOnField ? dashedMaterial : amber ? offTagMaterial : material;
                line.textureMode = LineTextureMode.Tile;
                line.startColor = line.endColor = kind == TargetKind.NotOnField ? OffTagColor : Color.white;
                marker.GetComponent<Renderer>().sharedMaterial = amber ? offTagMaterial : material;
                sights[i].name = "Tag " + tag.id;
            }
        }

        /// <summary>The words by a tag's marker: its id, plus "not on this field" when the field has no such tag.</summary>
        public static string LabelText(int id, TargetKind kind)
        {
            return kind == TargetKind.NotOnField ? id + " · not on this field" : id.ToString();
        }

        /// <summary>
        /// A material that draws a line as dashes: a small texture, half solid and half clear,
        /// repeated along the line (the line's color tints it).
        /// </summary>
        private static Material DashedMaterial()
        {
            var dash = new Texture2D(8, 1, TextureFormat.RGBA32, false) { wrapMode = TextureWrapMode.Repeat, filterMode = FilterMode.Point };
            for (int x = 0; x < 8; x++)
            {
                dash.SetPixel(x, 0, x < 4 ? Color.white : Color.clear);
            }
            dash.Apply();
            // About 20 dashes per meter of line.
            return new Material(Shader.Find("Sprites/Default")) { mainTexture = dash, mainTextureScale = new Vector2(20f, 1f), name = "Dashed sight line" };
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
            sight.gameObject.layer = Layer;
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
            marker.layer = Layer;
            marker.transform.localScale = Vector3.one * FtcToUnity.Meters(1.5);
            Renderer renderer = marker.GetComponent<Renderer>();
            renderer.sharedMaterial = material;
            renderer.shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.Off;

            var label = new GameObject("Label").transform;
            label.SetParent(sight, false);
            label.gameObject.layer = Layer;
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

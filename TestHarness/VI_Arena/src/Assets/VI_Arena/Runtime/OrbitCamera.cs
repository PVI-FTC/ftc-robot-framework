using System.Collections.Generic;
using UnityEngine;

namespace VI_Arena
{
    /// <summary>
    /// The replay camera. It starts looking straight down with no perspective ("top-down"), so the
    /// field looks like the Swing viewer's, with FTC +y up the screen. T switches to an angled 3D
    /// view you can turn (right-drag) and zoom (scroll); F follows the robot.
    ///
    /// <para><b>Preset viewpoints</b> (keys 1–7, or the View buttons above the field): Audience, Red
    /// drive team, Blue drive team, Top-down, Corner, HIVE close-up and Robot chase
    /// (<see cref="Presets"/>). Picking one glides the camera there; turning or zooming afterwards
    /// works as usual, starting from that view.</para>
    ///
    /// <para>Mouse and keys are read through Unity's OnGUI events, so this works whichever input
    /// system the project uses. The mouse only acts over the camera's part of the screen, not over
    /// the panels.</para>
    /// </summary>
    [RequireComponent(typeof(Camera))]
    public sealed class OrbitCamera : MonoBehaviour
    {
        /// <summary>What a preset does.</summary>
        public enum PresetKind
        {
            /// <summary>Look from <see cref="ViewPreset.eye"/> at <see cref="ViewPreset.target"/>.</summary>
            Look,
            /// <summary>The straight-down view (like T).</summary>
            TopDown,
            /// <summary>Behind the robot, following it and turning with it (like F, but from behind).</summary>
            Chase,
        }

        /// <summary>One preset viewpoint. Positions are in the FTC field frame, in inches (z up).</summary>
        public sealed class ViewPreset
        {
            public string name;
            /// <summary>The short label on its button.</summary>
            public string label;
            public PresetKind kind;
            /// <summary>Where the camera is (Look presets): FTC x, y, height, in inches.</summary>
            public Vector3 eye;
            /// <summary>What it looks at (Look presets): FTC x, y, height, in inches.</summary>
            public Vector3 target;
        }

        /// <summary>
        /// The preset viewpoints, in key order (key 1 is the first). FTC frame, inches: +x toward the
        /// audience, +y toward the blue wall, the third number is height.
        /// </summary>
        public static readonly IReadOnlyList<ViewPreset> Presets = new List<ViewPreset>
        {
            new ViewPreset { name = "Audience", label = "Audience", kind = PresetKind.Look, eye = new Vector3(130f, 0f, 75f), target = new Vector3(0f, 0f, 6f) },
            new ViewPreset { name = "Red drive team", label = "Red", kind = PresetKind.Look, eye = new Vector3(0f, -99f, 60f), target = new Vector3(0f, 0f, 10f) },
            new ViewPreset { name = "Blue drive team", label = "Blue", kind = PresetKind.Look, eye = new Vector3(0f, 99f, 60f), target = new Vector3(0f, 0f, 10f) },
            new ViewPreset { name = "Top-down", label = "Top", kind = PresetKind.TopDown },
            new ViewPreset { name = "Corner", label = "Corner", kind = PresetKind.Look, eye = new Vector3(105f, -105f, 115f), target = new Vector3(0f, 0f, 0f) },
            new ViewPreset { name = "HIVE close-up", label = "HIVE", kind = PresetKind.Look, eye = new Vector3(34f, -40f, 16f), target = new Vector3(0f, -4f, 44f) },
            new ViewPreset { name = "Robot chase", label = "Chase", kind = PresetKind.Chase },
        };

        /// <summary>The robot, for follow mode. Set by the replay controller.</summary>
        public Transform followTarget;

        [SerializeField] private float distanceMeters = 5.2f;
        [SerializeField] private float yawDegrees = 0f;
        [SerializeField] private float pitchDegrees = 55f;
        /// <summary>Space shown around the field in top-down view, in meters.</summary>
        [SerializeField] private float topDownMarginMeters = 0.20f;

        /// <summary>Lowest and highest camera angle (degrees down from level). Below zero looks up.</summary>
        public const float MinPitch = -30f, MaxPitch = 89f;
        /// <summary>Closest and farthest zoom (times the starting distance). Far enough to see a whole setting.</summary>
        private const float MinZoom = 0.05f, MaxZoom = 8f;
        /// <summary>How quickly a preset move settles (larger is faster).</summary>
        private const float Glide = 5f;
        /// <summary>The chase view: distance behind the robot (m) and angle down (degrees).</summary>
        private const float ChaseDistanceMeters = 1.5f, ChasePitch = 24f;

        private Camera view;
        private bool topDown = true;
        private bool follow;
        private bool chase;
        private float zoom = 1f;
        private Vector3 center = Vector3.zero;
        // Where a preset is gliding to (only while gliding).
        private bool gliding;
        private float goalYaw, goalPitch, goalZoom;
        private Vector3 goalCenter;

        /// <summary>True while showing the straight-down view.</summary>
        public bool TopDown => topDown;
        /// <summary>True while following the robot.</summary>
        public bool Following => follow;
        /// <summary>The preset last picked (0-based), or -1 once the view has been turned or zoomed by hand.</summary>
        public int CurrentPreset { get; private set; } = -1;

        /// <summary>Switches between the straight-down view and the angled 3D view.</summary>
        public void ToggleTopDown()
        {
            topDown = !topDown;
            CurrentPreset = -1;
        }

        /// <summary>Switches between looking at the field center and following the robot.</summary>
        public void ToggleFollow()
        {
            follow = !follow;
            chase = false;
            CurrentPreset = -1;
        }

        /// <summary>Glides to preset <paramref name="index"/> (0-based; key 1 is 0).</summary>
        public void ApplyPreset(int index)
        {
            if (index < 0 || index >= Presets.Count)
            {
                return;
            }
            ViewPreset preset = Presets[index];
            CurrentPreset = index;
            chase = false;
            gliding = false;
            switch (preset.kind)
            {
                case PresetKind.TopDown:
                    topDown = true;
                    follow = false;
                    center = Vector3.zero;
                    zoom = 1f;
                    break;
                case PresetKind.Chase:
                    topDown = false;
                    follow = true;
                    chase = true;
                    GlideTo(followTarget != null ? followTarget.position : Vector3.zero,
                        followTarget != null ? followTarget.eulerAngles.y : yawDegrees, ChasePitch, ChaseDistanceMeters / distanceMeters);
                    break;
                default:
                    topDown = false;
                    follow = false;
                    Vector3 eye = FtcToUnity.Position(preset.eye.x, preset.eye.y, FtcToUnity.Meters(preset.eye.z));
                    Vector3 target = FtcToUnity.Position(preset.target.x, preset.target.y, FtcToUnity.Meters(preset.target.z));
                    LookFrom(eye, target, out float yaw, out float pitch, out float distance);
                    GlideTo(target, yaw, pitch, distance / distanceMeters);
                    break;
            }
        }

        /// <summary>
        /// The orbit angles and distance that put the camera at <paramref name="eye"/> looking at
        /// <paramref name="target"/> (Unity meters): yaw clockwise from +z, pitch down from level.
        /// </summary>
        public static void LookFrom(Vector3 eye, Vector3 target, out float yawDeg, out float pitchDeg, out float distance)
        {
            Vector3 toTarget = target - eye;
            distance = toTarget.magnitude;
            Vector3 dir = toTarget / Mathf.Max(1e-6f, distance);
            yawDeg = Mathf.Atan2(dir.x, dir.z) * Mathf.Rad2Deg;
            pitchDeg = Mathf.Clamp(-Mathf.Asin(dir.y) * Mathf.Rad2Deg, MinPitch, MaxPitch);
        }

        /// <summary>Where the camera is for an orbit around <paramref name="target"/> (the other way from <see cref="LookFrom"/>).</summary>
        public static Vector3 EyeFor(Vector3 target, float yawDeg, float pitchDeg, float distance)
        {
            return target - Quaternion.Euler(pitchDeg, yawDeg, 0f) * Vector3.forward * distance;
        }

        private void GlideTo(Vector3 goal, float yaw, float pitch, float goalZoomFactor)
        {
            goalCenter = goal;
            goalYaw = yaw;
            goalPitch = pitch;
            goalZoom = Mathf.Clamp(goalZoomFactor, MinZoom, MaxZoom);
            gliding = true;
        }

        private void Awake()
        {
            view = GetComponent<Camera>();
            // Start-up choices (handy for demos and pictures): "-view 3d", "-zoom 2.5", "-yaw 30", "-pitch 20", "-preset 2".
            string[] args = System.Environment.GetCommandLineArgs();
            for (int i = 0; i < args.Length - 1; i++)
            {
                string value = args[i + 1];
                switch (args[i].ToLowerInvariant())
                {
                    case "-view":
                        topDown = !string.Equals(value, "3d", System.StringComparison.OrdinalIgnoreCase);
                        break;
                    case "-zoom":
                        if (float.TryParse(value, System.Globalization.NumberStyles.Float, System.Globalization.CultureInfo.InvariantCulture, out float z))
                        {
                            zoom = Mathf.Clamp(z, MinZoom, MaxZoom);
                        }
                        break;
                    case "-yaw":
                        float.TryParse(value, System.Globalization.NumberStyles.Float, System.Globalization.CultureInfo.InvariantCulture, out yawDegrees);
                        break;
                    case "-pitch":
                        if (float.TryParse(value, System.Globalization.NumberStyles.Float, System.Globalization.CultureInfo.InvariantCulture, out float p))
                        {
                            pitchDegrees = Mathf.Clamp(p, MinPitch, MaxPitch);
                        }
                        break;
                    case "-preset":
                        if (int.TryParse(value, out int preset))
                        {
                            ApplyPreset(preset - 1);
                            SnapToGoal(); // start there, without gliding
                        }
                        break;
                }
            }
        }

        private void SnapToGoal()
        {
            if (!gliding)
            {
                return;
            }
            center = goalCenter;
            yawDegrees = goalYaw;
            pitchDegrees = goalPitch;
            zoom = goalZoom;
            gliding = false;
        }

        private void LateUpdate()
        {
            if (follow && followTarget != null)
            {
                goalCenter = followTarget.position;
                if (chase)
                {
                    goalYaw = followTarget.eulerAngles.y; // stay behind the robot as it turns
                }
                if (!gliding)
                {
                    center = followTarget.position;
                    if (chase)
                    {
                        yawDegrees = Mathf.LerpAngle(yawDegrees, goalYaw, 1f - Mathf.Exp(-Time.unscaledDeltaTime * Glide));
                    }
                }
            }
            if (gliding)
            {
                float t = 1f - Mathf.Exp(-Time.unscaledDeltaTime * Glide);
                center = Vector3.Lerp(center, goalCenter, t);
                yawDegrees = Mathf.LerpAngle(yawDegrees, goalYaw, t);
                pitchDegrees = Mathf.Lerp(pitchDegrees, goalPitch, t);
                zoom = Mathf.Lerp(zoom, goalZoom, t);
                if (Vector3.Distance(center, goalCenter) < 0.002f && Mathf.Abs(Mathf.DeltaAngle(yawDegrees, goalYaw)) < 0.1f
                    && Mathf.Abs(pitchDegrees - goalPitch) < 0.1f && Mathf.Abs(zoom - goalZoom) < 0.001f)
                {
                    SnapToGoal();
                }
            }

            view.orthographic = topDown;
            if (topDown)
            {
                // Straight down, turned so FTC +y (Unity +z) points up the screen, as in the Swing viewer.
                // Fit the whole field in the camera's area whatever its shape.
                float half = FtcToUnity.FieldSizeMeters / 2f + topDownMarginMeters;
                view.orthographicSize = Mathf.Max(half, half / Mathf.Max(0.01f, view.aspect)) * Mathf.Max(zoom, 0.2f);
                Vector3 over = follow && followTarget != null ? followTarget.position : Vector3.zero;
                transform.SetPositionAndRotation(over + Vector3.up * 10f, Quaternion.Euler(90f, 0f, 0f));
            }
            else
            {
                Quaternion rotation = Quaternion.Euler(pitchDegrees, yawDegrees, 0f);
                Vector3 eye = EyeFor(center, yawDegrees, pitchDegrees, distanceMeters * zoom);
                eye.y = Mathf.Max(eye.y, 0.05f); // never below the floor
                transform.SetPositionAndRotation(eye, rotation);
            }
        }

        private void OnGUI()
        {
            Event e = Event.current;
            if (e.type == EventType.KeyDown)
            {
                int preset = PresetForKey(e.keyCode);
                if (preset >= 0)
                {
                    ApplyPreset(preset);
                    e.Use();
                    return;
                }
                if (e.keyCode == KeyCode.T)
                {
                    ToggleTopDown();
                    e.Use();
                    return;
                }
                if (e.keyCode == KeyCode.F)
                {
                    ToggleFollow();
                    e.Use();
                    return;
                }
            }
            if (!OverView(e.mousePosition))
            {
                return;
            }
            if (e.type == EventType.MouseDrag && (e.button == 1 || e.button == 2))
            {
                StopPresetMove();
                if (topDown)
                {
                    topDown = false; // dragging means "show me it in 3D"
                }
                yawDegrees += e.delta.x * 0.3f;
                pitchDegrees = Mathf.Clamp(pitchDegrees + e.delta.y * 0.3f, MinPitch, MaxPitch);
                e.Use();
            }
            else if (e.type == EventType.ScrollWheel)
            {
                StopPresetMove();
                zoom = Mathf.Clamp(zoom * (1f + e.delta.y * 0.05f), MinZoom, MaxZoom);
                e.Use();
            }
        }

        /// <summary>The preset a key picks: 1–7 (top row or keypad) give 0–6; anything else -1.</summary>
        public static int PresetForKey(KeyCode key)
        {
            if (key >= KeyCode.Alpha1 && key <= KeyCode.Alpha9)
            {
                int index = key - KeyCode.Alpha1;
                return index < Presets.Count ? index : -1;
            }
            if (key >= KeyCode.Keypad1 && key <= KeyCode.Keypad9)
            {
                int index = key - KeyCode.Keypad1;
                return index < Presets.Count ? index : -1;
            }
            return -1;
        }

        /// <summary>Turning or zooming by hand: stop gliding (keep where we are) and leave the chase.</summary>
        private void StopPresetMove()
        {
            gliding = false;
            chase = false;
            CurrentPreset = -1;
        }

        /// <summary>True if a GUI position (y down from the top) is over this camera's part of the screen.</summary>
        private bool OverView(Vector2 guiPosition)
        {
            Rect pixels = view.pixelRect; // y up from the bottom
            var asGui = new Rect(pixels.x, Screen.height - pixels.yMax, pixels.width, pixels.height);
            return asGui.Contains(guiPosition);
        }
    }
}

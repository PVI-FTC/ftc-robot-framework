using UnityEngine;

namespace PviHarness.Replay
{
    /// <summary>
    /// The replay camera. It starts looking straight down with no perspective ("top-down"), so the
    /// field looks like the Swing viewer's, with FTC +y up the screen. T switches to an angled 3D
    /// view you can turn (right-drag) and zoom (scroll); F follows the robot.
    ///
    /// <para>Mouse and keys are read through Unity's OnGUI events, so this works whichever input
    /// system the project uses. The mouse only acts over the camera's part of the screen, not over
    /// the panels.</para>
    /// </summary>
    [RequireComponent(typeof(Camera))]
    public sealed class OrbitCamera : MonoBehaviour
    {
        /// <summary>The robot, for follow mode. Set by the replay controller.</summary>
        public Transform followTarget;

        [SerializeField] private float distanceMeters = 5.2f;
        [SerializeField] private float yawDegrees = 0f;
        [SerializeField] private float pitchDegrees = 55f;
        /// <summary>Space shown around the field in top-down view, in meters.</summary>
        [SerializeField] private float topDownMarginMeters = 0.20f;

        private Camera view;
        private bool topDown = true;
        private bool follow;
        private float zoom = 1f;

        /// <summary>True while showing the straight-down view.</summary>
        public bool TopDown => topDown;
        /// <summary>True while following the robot.</summary>
        public bool Following => follow;

        /// <summary>Switches between the straight-down view and the angled 3D view.</summary>
        public void ToggleTopDown()
        {
            topDown = !topDown;
        }

        /// <summary>Switches between looking at the field center and following the robot.</summary>
        public void ToggleFollow()
        {
            follow = !follow;
        }

        private void Awake()
        {
            view = GetComponent<Camera>();
        }

        private void LateUpdate()
        {
            Vector3 center = follow && followTarget != null ? followTarget.position : Vector3.zero;
            view.orthographic = topDown;
            if (topDown)
            {
                // Straight down, turned so FTC +y (Unity +z) points up the screen, as in the Swing viewer.
                // Fit the whole field in the camera's area whatever its shape.
                float half = FtcToUnity.FieldSizeMeters / 2f + topDownMarginMeters;
                view.orthographicSize = Mathf.Max(half, half / Mathf.Max(0.01f, view.aspect)) * zoom;
                Quaternion down = Quaternion.Euler(90f, 0f, 0f);
                transform.SetPositionAndRotation(center + Vector3.up * 10f, down);
            }
            else
            {
                Quaternion rotation = Quaternion.Euler(pitchDegrees, yawDegrees, 0f);
                transform.SetPositionAndRotation(center - rotation * Vector3.forward * distanceMeters * zoom, rotation);
            }
        }

        private void OnGUI()
        {
            Event e = Event.current;
            if (e.type == EventType.KeyDown && e.keyCode == KeyCode.T)
            {
                ToggleTopDown();
                e.Use();
                return;
            }
            if (e.type == EventType.KeyDown && e.keyCode == KeyCode.F)
            {
                ToggleFollow();
                e.Use();
                return;
            }
            if (!OverView(e.mousePosition))
            {
                return;
            }
            if (e.type == EventType.MouseDrag && (e.button == 1 || e.button == 2))
            {
                if (topDown)
                {
                    topDown = false; // dragging means "show me it in 3D"
                }
                yawDegrees += e.delta.x * 0.3f;
                pitchDegrees = Mathf.Clamp(pitchDegrees + e.delta.y * 0.3f, 10f, 89f);
                e.Use();
            }
            else if (e.type == EventType.ScrollWheel)
            {
                zoom = Mathf.Clamp(zoom * (1f + e.delta.y * 0.05f), 0.2f, 3f);
                e.Use();
            }
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

using System.Collections.Generic;
using UnityEngine;

namespace VI_Arena
{
    /// <summary>
    /// The small "what the webcam sees" picture: a second Unity camera placed at a webcam's mount on
    /// the robot (<see cref="CameraMount"/>), with the webcam's field of view and shape, drawing into a
    /// small texture that the HUD shows in a corner of the field. The HUD draws a box with the id on
    /// each AprilTag the harness says that webcam detected (<see cref="TagBox"/>), so you can see at a
    /// glance whether the detections match what the camera would really see, and a thin white outline
    /// on every other tag in view (<see cref="TagsInView"/>).
    ///
    /// <para>The texture is small (<see cref="TextureWidthPixels"/> wide) so the extra drawing stays
    /// cheap. The camera only draws while the picture is shown.</para>
    /// </summary>
    public sealed class CameraPictureView
    {
        /// <summary>How wide the picture is drawn, in pixels (its height follows the webcam's shape).</summary>
        public const int TextureWidthPixels = 320;
        /// <summary>The tag size used when the field file doesn't list a tag (inches, the printed black square).</summary>
        public const double DefaultTagSizeInches = 4.0;

        private readonly Camera camera;
        private RenderTexture texture;

        /// <summary>Makes the (switched off) webcam camera under <paramref name="parent"/>.</summary>
        public CameraPictureView(Transform parent)
        {
            camera = new GameObject("Webcam picture camera").AddComponent<Camera>();
            camera.transform.SetParent(parent, false);
            camera.nearClipPlane = 0.02f;
            camera.farClipPlane = 60f;
            // The sight lines and their markers are for people; the camera wouldn't see them.
            camera.cullingMask &= ~(1 << AprilTagSightView.Layer);
            camera.enabled = false;
        }

        /// <summary>The Unity camera (for tests).</summary>
        public Camera Camera => camera;

        /// <summary>The picture, or null before it was first shown.</summary>
        public RenderTexture Texture => texture;

        /// <summary>True while the camera is drawing the picture.</summary>
        public bool Showing => camera.enabled;

        /// <summary>
        /// Puts the camera at <paramref name="mount"/> on <paramref name="robot"/> (the Unity robot object,
        /// local +z its front) with the webcam's field of view and shape, and starts drawing. It shows the
        /// setting's sky whenever the setting has one (<see cref="ClearFlagsFor"/>); the background color
        /// is copied from <paramref name="like"/> (the main camera), if given.
        /// </summary>
        public void Show(Transform robot, CameraMount mount, Camera like = null)
        {
            if (robot == null || mount == null || !robot.gameObject.activeInHierarchy)
            {
                Hide();
                return;
            }
            int height = Mathf.Max(1, Mathf.RoundToInt(TextureWidthPixels / mount.Aspect));
            if (texture == null || texture.height != height)
            {
                if (texture != null)
                {
                    camera.targetTexture = null;
                    Release(texture);
                }
                texture = new RenderTexture(TextureWidthPixels, height, 16) { name = "Webcam picture" };
                camera.targetTexture = texture;
            }
            camera.fieldOfView = mount.VerticalFovDegrees;
            camera.aspect = mount.Aspect;
            camera.transform.SetPositionAndRotation(robot.TransformPoint(mount.UnityLocalPosition),
                robot.rotation * mount.UnityLocalRotation);
            // Not the main camera's choice: its top-down view hides the sky, which left the webcam
            // picture black there.
            camera.clearFlags = ClearFlagsFor(RenderSettings.skybox);
            if (like != null)
            {
                camera.backgroundColor = like.backgroundColor;
            }
            camera.enabled = true;
        }

        /// <summary>
        /// What the webcam draws behind everything: the setting's sky (<paramref name="skybox"/>, from
        /// RenderSettings) when there is one, else the plain background color. The same in every view
        /// of the main window (top-down, 3D, the presets), because the webcam is on the robot either way.
        /// </summary>
        public static CameraClearFlags ClearFlagsFor(Material skybox)
        {
            return skybox != null ? CameraClearFlags.Skybox : CameraClearFlags.SolidColor;
        }

        /// <summary>Stops drawing the picture.</summary>
        public void Hide()
        {
            camera.enabled = false;
        }

        /// <summary>
        /// The box to draw on the picture for a detected tag: centered where the lens puts the tag's
        /// center and as wide as a <paramref name="sizeInches"/> tag looks at that distance, scaled
        /// from the webcam's pixels to <paramref name="picture"/> (the picture's rectangle on screen,
        /// y down). False when the tag isn't in front of the lens.
        /// </summary>
        public static bool TagBox(CameraMount mount, AprilTagSeenData tag, double sizeInches, Rect picture, out Rect box)
        {
            box = Rect.zero;
            if (!mount.Project(tag, out Vector2 pixel))
            {
                return false;
            }
            float scale = picture.width / mount.WidthPixels;
            float side = Mathf.Max(6f, mount.PixelsAcross(tag, sizeInches) * scale);
            Vector2 center = picture.position + pixel * scale;
            box = new Rect(center.x - side / 2f, center.y - side / 2f, side, side);
            return true;
        }

        /// <summary>
        /// Where to put a tag's id of <paramref name="size"/> so it is readable: above
        /// <paramref name="box"/>, else below it, else further up or down, always inside
        /// <paramref name="inside"/> and clear of the ids in <paramref name="taken"/> when there is room
        /// (tags printed side by side look close together from far off). The spot is added to
        /// <paramref name="taken"/>.
        /// </summary>
        public static Rect TagIdSpot(Rect box, Vector2 size, Rect inside, List<Rect> taken)
        {
            float x = Mathf.Clamp(box.center.x - size.x / 2f, inside.x, Mathf.Max(inside.x, inside.xMax - size.x));
            Rect? spot = null;
            for (int row = 0; row < 8 && spot == null; row++)
            {
                foreach (float y in new[] { box.y - size.y - 1 - row * size.y, box.yMax + 1 + row * size.y })
                {
                    var tried = new Rect(x, y, size.x, size.y);
                    if (tried.y >= inside.y && tried.yMax <= inside.yMax && !taken.Exists(t => t.Overlaps(tried)))
                    {
                        spot = tried;
                        break;
                    }
                }
            }
            // No free spot at all: as near the box as still fits inside.
            Rect result = spot ?? new Rect(x, Mathf.Clamp(box.y - size.y - 1, inside.y, Mathf.Max(inside.y, inside.yMax - size.y)), size.x, size.y);
            taken.Add(result);
            return result;
        }

        /// <summary>Removes the camera and its texture.</summary>
        public void Destroy()
        {
            camera.targetTexture = null;
            if (texture != null)
            {
                Release(texture);
            }
            if (Application.isPlaying)
            {
                Object.Destroy(camera.gameObject);
            }
            else
            {
                Object.DestroyImmediate(camera.gameObject);
            }
        }

        private static void Release(RenderTexture old)
        {
            old.Release();
            if (Application.isPlaying)
            {
                Object.Destroy(old);
            }
            else
            {
                Object.DestroyImmediate(old);
            }
        }
    }
}

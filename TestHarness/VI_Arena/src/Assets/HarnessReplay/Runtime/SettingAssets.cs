using System.Collections.Generic;
using UnityEngine;

namespace PviHarness.Replay
{
    /// <summary>
    /// The art for each setting: its 360° sky, surface textures and detailed models, from
    /// Resources/Settings/&lt;setting id&gt;/ (Poly Haven, ambientCG and NASA; see Art/CREDITS.md).
    /// Every lookup returns null when a file isn't there, and the setting then keeps its plain
    /// shapes and colors, so a copy of the project without the art still works.
    ///
    /// <para>Folder layout: <c>sky.hdr</c> or <c>sky.exr</c>; <c>textures/&lt;surface&gt;_color</c>,
    /// <c>_normal</c> and optional <c>_ao</c>; <c>models/&lt;name&gt;/&lt;name&gt;</c> with optional
    /// <c>color</c> and <c>normal</c> textures next to it; <c>earth</c> for the moon base.</para>
    /// </summary>
    public static class SettingAssets
    {
        private static readonly Dictionary<string, Material> Skies = new Dictionary<string, Material>();

        private static string Folder(string settingId) => "Settings/" + settingId + "/";

        /// <summary>The setting's skybox material (its 360° sky), or null if it has none.</summary>
        public static Material Sky(string settingId)
        {
            if (Skies.TryGetValue(settingId, out Material cached) && cached != null)
            {
                return cached;
            }
            var cube = Resources.Load<Cubemap>(Folder(settingId) + "sky");
            if (cube == null)
            {
                return null;
            }
            var sky = new Material(Shader.Find("Skybox/Cubemap")) { name = settingId + " sky" };
            sky.SetTexture("_Tex", cube);
            // How bright each sky shows: the night-stadium picture is exposed far brighter than a real night.
            float exposure = settingId == "night-stadium" ? 0.12f : settingId == "moon-base" ? 1.3f : 1.0f;
            sky.SetFloat("_Exposure", exposure);
            Skies[settingId] = sky;
            return sky;
        }

        /// <summary>A texture from the setting's folder (e.g. "earth"), or null.</summary>
        public static Texture2D Texture(string settingId, string name)
        {
            return Resources.Load<Texture2D>(Folder(settingId) + name);
        }

        /// <summary>
        /// Gives a part a textured surface (color, bumps and shading in the cracks), tinted, with the
        /// pattern repeating every <paramref name="tileMeters"/> across a face <paramref name="faceMeters"/>
        /// big. Does nothing (the part keeps its plain color) when the texture isn't there.
        /// </summary>
        public static void ApplySurface(GameObject part, string settingId, string surface, Vector2 faceMeters, float tileMeters,
            Color tint, float smoothness = 0.15f, bool useColorMap = true)
        {
            Texture2D color = Resources.Load<Texture2D>(Folder(settingId) + "textures/" + surface + "_color");
            if (color == null)
            {
                return;
            }
            // Without the color map, the surface is the tint with the texture's bumps (e.g. a red rubber track
            // from a black rubber texture).
            var material = new Material(Shader.Find("Standard")) { name = surface, color = tint, mainTexture = useColorMap ? color : null };
            material.mainTextureScale = new Vector2(Mathf.Max(1f, faceMeters.x / tileMeters), Mathf.Max(1f, faceMeters.y / tileMeters));
            Texture2D normal = Resources.Load<Texture2D>(Folder(settingId) + "textures/" + surface + "_normal");
            if (normal != null)
            {
                material.SetTexture("_BumpMap", normal);
                material.EnableKeyword("_NORMALMAP");
            }
            Texture2D ao = Resources.Load<Texture2D>(Folder(settingId) + "textures/" + surface + "_ao");
            if (ao != null)
            {
                material.SetTexture("_OcclusionMap", ao);
            }
            material.SetFloat("_Glossiness", smoothness);
            part.GetComponent<Renderer>().material = material;
        }

        /// <summary>
        /// Places one of the setting's detailed models, standing on the ground at <paramref name="position"/>,
        /// turned <paramref name="yawDegrees"/>, and scaled so its largest side is <paramref name="sizeMeters"/>
        /// (the downloaded models use different units). Returns null if the model isn't there.
        /// </summary>
        public static GameObject PlaceModel(Transform parent, string settingId, string name, Vector3 position, float yawDegrees,
            float sizeMeters)
        {
            var prefab = Resources.Load<GameObject>(Folder(settingId) + "models/" + name + "/" + name);
            if (prefab == null)
            {
                return null;
            }
            GameObject model = Object.Instantiate(prefab, parent, false);
            model.name = name;
            foreach (Collider collider in model.GetComponentsInChildren<Collider>())
            {
                Object.DestroyImmediate(collider); // scenery: Unity's physics only sees the field
            }
            Texture2D color = Resources.Load<Texture2D>(Folder(settingId) + "models/" + name + "/color");
            Texture2D normal = Resources.Load<Texture2D>(Folder(settingId) + "models/" + name + "/normal");
            Renderer[] renderers = model.GetComponentsInChildren<Renderer>();
            if (color != null)
            {
                // Poly Haven models: one texture set for the whole model.
                var material = new Material(Shader.Find("Standard")) { name = name, mainTexture = color };
                if (normal != null)
                {
                    material.SetTexture("_BumpMap", normal);
                    material.EnableKeyword("_NORMALMAP");
                }
                material.SetFloat("_Glossiness", 0.3f);
                foreach (Renderer renderer in renderers)
                {
                    renderer.sharedMaterial = material;
                }
            }
            // Size it: largest side to sizeMeters, then stand it on the ground.
            model.transform.localRotation = Quaternion.Euler(0f, yawDegrees, 0f);
            Bounds bounds = WorldBounds(renderers);
            float largest = Mathf.Max(bounds.size.x, bounds.size.y, bounds.size.z);
            if (largest > 0f)
            {
                model.transform.localScale *= sizeMeters / largest;
            }
            bounds = WorldBounds(renderers);
            model.transform.position += position - new Vector3(bounds.center.x, bounds.min.y, bounds.center.z);
            return model;
        }

        private static Bounds WorldBounds(Renderer[] renderers)
        {
            if (renderers.Length == 0)
            {
                return new Bounds();
            }
            Bounds bounds = renderers[0].bounds;
            foreach (Renderer renderer in renderers)
            {
                bounds.Encapsulate(renderer.bounds);
            }
            return bounds;
        }
    }
}

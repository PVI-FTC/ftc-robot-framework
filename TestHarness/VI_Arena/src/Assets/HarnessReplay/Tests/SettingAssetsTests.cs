using NUnit.Framework;
using UnityEngine;

namespace PviHarness.Replay.Tests
{
    /// <summary>
    /// The settings' art (Resources/Settings): every sky, texture and model the settings use is in
    /// the project, and the code puts it on correctly.
    /// </summary>
    public class SettingAssetsTests
    {
        /// <summary>
        /// WHAT: Each setting's 360° sky, surface textures and models load.
        /// WHY: A misnamed file silently falls back to plain shapes, so only a test notices it's missing.
        /// </summary>
        [Test]
        public void EverySettingsArtIsInTheProject()
        {
            foreach (string id in new[] { "competition-hall", "night-stadium", "moon-base" })
            {
                Assert.IsNotNull(SettingAssets.Sky(id), id + " sky");
            }
            (string setting, string surface)[] surfaces =
            {
                ("competition-hall", "floor"), ("competition-hall", "wall"), ("night-stadium", "grass"), ("night-stadium", "track"),
                ("night-stadium", "concrete"), ("moon-base", "ground"), ("moon-base", "pad"),
            };
            foreach (var (setting, surface) in surfaces)
            {
                Assert.IsNotNull(Resources.Load<Texture2D>("Settings/" + setting + "/textures/" + surface + "_color"), setting + " " + surface);
                Assert.IsNotNull(Resources.Load<Texture2D>("Settings/" + setting + "/textures/" + surface + "_normal"), setting + " " + surface + " normal");
            }
            (string setting, string model)[] models =
            {
                ("competition-hall", "gamepad"), ("competition-hall", "classic_laptop"), ("competition-hall", "tool_cart"),
                ("competition-hall", "metal_tool_chest"), ("moon-base", "lunar_rover"), ("moon-base", "moon_rock_01"),
                ("moon-base", "moon_rock_03"), ("moon-base", "moon_rock_05"), ("moon-base", "lunar_module"), ("moon-base", "astronaut"),
                ("moon-base", "habitat_part1"), ("moon-base", "habitat_part2"),
            };
            foreach (var (setting, model) in models)
            {
                Assert.IsNotNull(Resources.Load<GameObject>("Settings/" + setting + "/models/" + model + "/" + model), setting + " " + model);
            }
            Assert.IsNotNull(SettingAssets.Texture("moon-base", "earth"), "Earth map");
        }

        /// <summary>
        /// WHAT: A placed model is scaled so its largest side is the size asked for (the downloads use
        /// different units: NASA's astronaut is about 74 units tall) and stands on the ground at the spot.
        /// </summary>
        [Test]
        public void ModelsAreSizedAndStandOnTheGround()
        {
            var parent = new GameObject("Test models");
            try
            {
                GameObject astronaut = SettingAssets.PlaceModel(parent.transform, "moon-base", "astronaut", new Vector3(5f, 0f, -3f), 30f, 1.85f);
                Bounds b = astronaut.GetComponentsInChildren<Renderer>()[0].bounds;
                foreach (Renderer r in astronaut.GetComponentsInChildren<Renderer>())
                {
                    b.Encapsulate(r.bounds);
                }

                Assert.AreEqual(1.85f, Mathf.Max(b.size.x, b.size.y, b.size.z), 0.01f, "largest side");
                Assert.AreEqual(0f, b.min.y, 0.01f, "stands on the ground");
                Assert.AreEqual(5f, b.center.x, 0.01f);
                Assert.AreEqual(-3f, b.center.z, 0.01f);
                Assert.IsNull(SettingAssets.PlaceModel(parent.transform, "moon-base", "no_such_model", Vector3.zero, 0f, 1f));
            }
            finally
            {
                Object.DestroyImmediate(parent);
            }
        }

        /// <summary>
        /// WHAT: A textured surface gets its color, normal and AO maps, repeating once per tile across the
        /// face; a surface without art keeps its plain material.
        /// </summary>
        [Test]
        public void SurfacesGetTheirTextures()
        {
            GameObject floor = GameObject.CreatePrimitive(PrimitiveType.Cube);
            try
            {
                Material plain = floor.GetComponent<Renderer>().sharedMaterial;
                SettingAssets.ApplySurface(floor, "moon-base", "nothing-here", new Vector2(10f, 10f), 2f, Color.white);
                Assert.AreSame(plain, floor.GetComponent<Renderer>().sharedMaterial, "no art: unchanged");

                SettingAssets.ApplySurface(floor, "moon-base", "ground", new Vector2(12f, 6f), 3f, Color.white);
                Material ground = floor.GetComponent<Renderer>().sharedMaterial;
                Assert.IsNotNull(ground.mainTexture);
                Assert.IsNotNull(ground.GetTexture("_BumpMap"));
                Assert.IsNotNull(ground.GetTexture("_OcclusionMap"));
                Assert.AreEqual(new Vector2(4f, 2f), ground.mainTextureScale);
            }
            finally
            {
                Object.DestroyImmediate(floor);
            }
        }
    }
}

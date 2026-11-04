using System.Linq;
using NUnit.Framework;
using UnityEngine;

namespace VI_Arena.Tests
{
    /// <summary>
    /// The settings around the arena (Visuals screen): the list, the choice, and that the scenery
    /// never gets in the way of the field.
    /// </summary>
    public class SettingBuilderTests
    {
        /// <summary>
        /// WHAT: Mission control (the plain dark look) comes first and builds nothing; every setting has a
        /// different id, a name and a description; an unknown id falls back to Mission control.
        /// </summary>
        [Test]
        public void SettingsListStartsWithMissionControl()
        {
            Assert.AreEqual(SettingBuilder.MissionControlId, SettingBuilder.All[0].id);
            Assert.IsNull(SettingBuilder.Build(null, SettingBuilder.All[0]));
            Assert.AreEqual(SettingBuilder.All.Count, SettingBuilder.All.Select(s => s.id).Distinct().Count());
            Assert.IsTrue(SettingBuilder.All.All(s => s.name.Length > 0 && s.description.Length > 0));
            Assert.AreEqual(SettingBuilder.MissionControlId, SettingBuilder.Find("atlantis").id);
            Assert.AreEqual("moon-base", SettingBuilder.SettingFromCommandLine(new[] { "VI_Arena.exe", "-setting", "moon-base" }));
        }

        /// <summary>
        /// WHAT: No setting puts anything above the floor inside the field's square (4.2 m across): the
        /// robot, field parts and trail are never hidden by scenery. Floors under the field are fine.
        /// WHY: The settings are decoration; the field is what the replay is for.
        /// </summary>
        [Test]
        public void EverySettingLeavesTheFieldClear()
        {
            var parent = new GameObject("Test settings");
            try
            {
                foreach (SettingInfo setting in SettingBuilder.All.Skip(1))
                {
                    Transform root = SettingBuilder.Build(parent.transform, setting);
                    Renderer[] parts = root.GetComponentsInChildren<Renderer>();
                    Assert.Greater(parts.Length, 50, setting.id + " builds its surroundings");
                    foreach (Renderer part in parts)
                    {
                        Bounds b = part.bounds;
                        bool overField = b.min.x < SettingBuilder.KeepClearMeters && b.max.x > -SettingBuilder.KeepClearMeters
                            && b.min.z < SettingBuilder.KeepClearMeters && b.max.z > -SettingBuilder.KeepClearMeters;
                        Assert.IsFalse(overField && b.max.y > 0f, setting.id + ": " + part.name + " sticks up into the field at " + b.center);
                    }
                    Assert.IsEmpty(root.GetComponentsInChildren<Collider>(), setting.id + ": scenery has no colliders");
                }
            }
            finally
            {
                Object.DestroyImmediate(parent);
            }
        }

        /// <summary>
        /// WHAT: Building a setting twice gives exactly the same scene (same parts in the same places):
        /// the crowd, craters and stars use a fixed seed.
        /// </summary>
        [Test]
        public void ASettingLooksTheSameEveryTime()
        {
            var parent = new GameObject("Test settings");
            try
            {
                SettingInfo hall = SettingBuilder.Find("competition-hall");
                Vector3[] first = SettingBuilder.Build(parent.transform, hall).GetComponentsInChildren<Renderer>().Select(r => r.bounds.center).ToArray();
                Vector3[] second = SettingBuilder.Build(parent.transform, hall).GetComponentsInChildren<Renderer>().Select(r => r.bounds.center).ToArray();

                CollectionAssert.AreEqual(first, second);
            }
            finally
            {
                Object.DestroyImmediate(parent);
            }
        }

        /// <summary>WHAT: Picking a setting sets its sky color and the sun's strength.</summary>
        [Test]
        public void ASettingSetsTheSkyAndSun()
        {
            var cameraObject = new GameObject("Test camera");
            var sunObject = new GameObject("Test sun");
            try
            {
                Camera camera = cameraObject.AddComponent<Camera>();
                Light sun = sunObject.AddComponent<Light>();
                SettingInfo moon = SettingBuilder.Find("moon-base");

                SettingBuilder.ApplyLighting(moon, camera, sun);

                Assert.AreEqual(moon.sky, camera.backgroundColor);
                Assert.AreEqual(moon.sunIntensity, sun.intensity, 1e-6f);
            }
            finally
            {
                Object.DestroyImmediate(cameraObject);
                Object.DestroyImmediate(sunObject);
            }
        }
    }
}

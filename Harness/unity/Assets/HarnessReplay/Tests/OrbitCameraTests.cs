using NUnit.Framework;
using UnityEngine;

namespace PviHarness.Replay.Tests
{
    /// <summary>The camera's preset viewpoints (keys 1–7 and the View buttons).</summary>
    public class OrbitCameraTests
    {
        /// <summary>
        /// WHAT: For every "look from here at there" preset, the orbit angles and distance worked out for it
        /// put the camera exactly at the preset's eye point.
        /// WHY: A sign slip would put the audience view behind the far wall or under the floor.
        /// </summary>
        [Test]
        public void EveryLookPresetPutsTheCameraWhereItSays()
        {
            foreach (OrbitCamera.ViewPreset preset in OrbitCamera.Presets)
            {
                if (preset.kind != OrbitCamera.PresetKind.Look)
                {
                    continue;
                }
                Vector3 eye = FtcToUnity.Position(preset.eye.x, preset.eye.y, FtcToUnity.Meters(preset.eye.z));
                Vector3 target = FtcToUnity.Position(preset.target.x, preset.target.y, FtcToUnity.Meters(preset.target.z));

                OrbitCamera.LookFrom(eye, target, out float yaw, out float pitch, out float distance);

                Vector3 placed = OrbitCamera.EyeFor(target, yaw, pitch, distance);
                Assert.AreEqual(eye.x, placed.x, 1e-3f, preset.name);
                Assert.AreEqual(eye.y, placed.y, 1e-3f, preset.name);
                Assert.AreEqual(eye.z, placed.z, 1e-3f, preset.name);
            }
        }

        /// <summary>
        /// WHAT: The drive-team views stand in their alliance areas behind their own walls (red at -y, blue at
        /// +y), at about eye height (60 in), mirror images of each other; the audience view is behind the
        /// audience (+x) wall.
        /// </summary>
        [Test]
        public void DriveTeamAndAudienceViewsStandWhereTheTeamsDo()
        {
            OrbitCamera.ViewPreset red = OrbitCamera.Presets[1], blue = OrbitCamera.Presets[2], audience = OrbitCamera.Presets[0];

            Assert.Less(red.eye.y, -72f, "red stands behind the red (-y) wall");
            Assert.Greater(blue.eye.y, 72f, "blue stands behind the blue (+y) wall");
            Assert.AreEqual(60f, red.eye.z, 1e-3f, "eye height");
            Assert.AreEqual(-red.eye.y, blue.eye.y, 1e-3f, "mirror images");
            Assert.Greater(audience.eye.x, 72f, "the audience is behind the +x wall");
            Assert.AreEqual(OrbitCamera.PresetKind.TopDown, OrbitCamera.Presets[3].kind);
            Assert.AreEqual(OrbitCamera.PresetKind.Chase, OrbitCamera.Presets[6].kind);
        }

        /// <summary>WHAT: Keys 1–7 (top row and keypad) pick presets 1–7; other keys pick none.</summary>
        [Test]
        public void NumberKeysPickPresets()
        {
            Assert.AreEqual(0, OrbitCamera.PresetForKey(KeyCode.Alpha1));
            Assert.AreEqual(6, OrbitCamera.PresetForKey(KeyCode.Alpha7));
            Assert.AreEqual(2, OrbitCamera.PresetForKey(KeyCode.Keypad3));
            Assert.AreEqual(-1, OrbitCamera.PresetForKey(KeyCode.Alpha8));
            Assert.AreEqual(-1, OrbitCamera.PresetForKey(KeyCode.T));
        }
    }
}

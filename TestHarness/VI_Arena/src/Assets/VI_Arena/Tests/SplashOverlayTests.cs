using System.IO;
using NUnit.Framework;
using UnityEngine;

namespace VI_Arena.Tests
{
    /// <summary>
    /// The start-up splash with the VI Arena logo (<see cref="SplashOverlay"/>): when it shows, how it
    /// fades, skipping it, and that the scene really points at the logo.
    /// </summary>
    public class SplashOverlayTests
    {
        /// <summary>
        /// WHAT: The splash shows only in the built app opened normally: not in the Editor, not in batch
        /// mode, and not with -nosplash, -at or -fpslog (pictures and frame-rate checks).
        /// </summary>
        [Test]
        public void TheSplashShowsOnlyWhenTheAppIsOpenedNormally()
        {
            Assert.IsTrue(SplashOverlay.ShouldShow(new[] { "VI_Arena.exe" }, false, false));
            Assert.IsTrue(SplashOverlay.ShouldShow(new[] { "VI_Arena.exe", "-recordings", "x", "run.json" }, false, false));
            Assert.IsFalse(SplashOverlay.ShouldShow(new[] { "Unity.exe" }, true, false), "Editor");
            Assert.IsFalse(SplashOverlay.ShouldShow(new[] { "VI_Arena.exe" }, false, true), "batch mode");
            Assert.IsFalse(SplashOverlay.ShouldShow(new[] { "VI_Arena.exe", "-NoSplash" }, false, false));
            Assert.IsFalse(SplashOverlay.ShouldShow(new[] { "VI_Arena.exe", "-at", "6" }, false, false));
            Assert.IsFalse(SplashOverlay.ShouldShow(new[] { "VI_Arena.exe", "-fpslog", "fps.txt" }, false, false));
        }

        /// <summary>
        /// WHAT: The logo fades in on black, stays, then the logo and the black fade out together and the
        /// splash is gone by 2 s.
        /// </summary>
        [Test]
        public void TheLogoFadesInThenEverythingFadesOutByTwoSeconds()
        {
            Assert.AreEqual(1f, SplashOverlay.BackdropAlpha(0f), 1e-5f, "black from the start");
            Assert.AreEqual(0f, SplashOverlay.LogoAlpha(0f), 1e-5f);
            Assert.AreEqual(0.5f, SplashOverlay.LogoAlpha(SplashOverlay.FadeInSeconds / 2), 1e-5f, "fading in");
            Assert.AreEqual(1f, SplashOverlay.LogoAlpha(1f), 1e-5f, "fully shown");
            Assert.AreEqual(0.5f, SplashOverlay.BackdropAlpha(1.8f), 1e-5f, "fading out");
            Assert.AreEqual(0.5f, SplashOverlay.LogoAlpha(1.8f), 1e-5f);
            Assert.AreEqual(0f, SplashOverlay.BackdropAlpha(2f), 1e-5f, "gone at 2 s");
            Assert.AreEqual(2f, SplashOverlay.FadeOutStartSeconds + SplashOverlay.FadeOutSeconds, 1e-5f);
        }

        /// <summary>WHAT: A click or key starts the fade-out at once; it never makes the splash last longer.</summary>
        [Test]
        public void SkippingStartsTheFadeOutAtOnce()
        {
            float start = SplashOverlay.SkipAt(0.5f, SplashOverlay.FadeOutStartSeconds);
            Assert.AreEqual(0.5f, start, 1e-5f);
            Assert.AreEqual(0f, SplashOverlay.BackdropAlpha(0.5f + SplashOverlay.FadeOutSeconds, start), 1e-5f, "gone 0.4 s after the click");
            Assert.AreEqual(SplashOverlay.FadeOutStartSeconds, SplashOverlay.SkipAt(1.9f, SplashOverlay.FadeOutStartSeconds), 1e-5f,
                "a click during the fade-out doesn't restart it");
        }

        /// <summary>
        /// WHAT: A slow frame (the first ones after loading take seconds) moves the splash on by at most
        /// 0.05 s, so the logo is still seen; normal frames count in full.
        /// </summary>
        [Test]
        public void ASlowFirstFrameDoesNotUseUpTheSplash()
        {
            Assert.AreEqual(0.05f, SplashOverlay.Advance(0f, 4.2f), 1e-5f);
            Assert.AreEqual(1.016f, SplashOverlay.Advance(1f, 0.016f), 1e-5f);
        }

        /// <summary>
        /// WHAT: The Replay scene's ReplayController points at Art/ArenaLogo.png, which isn't in a
        /// Resources folder (so the build carries it once, through the scene).
        /// </summary>
        [Test]
        public void TheSceneShowsTheArenaLogo()
        {
            string logoMeta = Path.Combine(Application.dataPath, "VI_Arena", "Art", "ArenaLogo.png.meta");
            Assert.IsTrue(File.Exists(Path.Combine(Application.dataPath, "VI_Arena", "Art", "ArenaLogo.png")));
            string guid = null;
            foreach (string line in File.ReadAllLines(logoMeta))
            {
                if (line.StartsWith("guid: "))
                {
                    guid = line.Substring("guid: ".Length).Trim();
                }
            }
            string scene = File.ReadAllText(Path.Combine(Application.dataPath, "VI_Arena", "Replay.unity"));

            StringAssert.Contains("splashLogo: {fileID: 2800000, guid: " + guid + ", type: 3}", scene);
            Assert.IsInstanceOf<Texture2D>(UnityEditor.AssetDatabase.LoadMainAssetAtPath("Assets/VI_Arena/Art/ArenaLogo.png"),
                "imported as a plain 2D picture (a two-line .meta once made it a cubemap, and the app showed no logo)");
        }
    }
}

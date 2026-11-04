using System;
using UnityEngine;

namespace VI_Arena
{
    /// <summary>
    /// The start-up splash: the VI Arena logo centered on black for about two seconds, fading in and
    /// out, over the app while it opens. A click or any key skips it (it fades out at once).
    ///
    /// <para>Only in the built app, opened normally: not in the Unity Editor, not in batch mode, and not
    /// with <c>-nosplash</c>, <c>-at</c> or <c>-fpslog</c> on the command line (pictures and frame-rate
    /// checks start at once). The app keeps loading underneath, so the splash never delays anything.</para>
    /// </summary>
    public sealed class SplashOverlay : MonoBehaviour
    {
        /// <summary>How long the logo fades in, in seconds.</summary>
        public const float FadeInSeconds = 0.4f;
        /// <summary>When the fade-out starts if nobody skips it, in seconds.</summary>
        public const float FadeOutStartSeconds = 1.6f;
        /// <summary>How long the fade-out takes, in seconds.</summary>
        public const float FadeOutSeconds = 0.4f;
        /// <summary>
        /// The most one frame moves the splash on, in seconds. WHY: the first frames after the app loads can
        /// take seconds each, which would use up the whole splash before anyone saw it.
        /// </summary>
        public const float MaxStepSeconds = 0.05f;
        /// <summary>How tall the logo is drawn, as a part of the window's height.</summary>
        public const float LogoHeightFraction = 0.6f;

        private Texture logo;
        private float shownSeconds;
        private float fadeOutStart = FadeOutStartSeconds;

        /// <summary>True if the splash should show for this start of the app.</summary>
        public static bool ShouldShow(string[] args, bool isEditor, bool isBatchMode)
        {
            if (isEditor || isBatchMode)
            {
                return false;
            }
            return !Array.Exists(args, arg => string.Equals(arg, "-nosplash", StringComparison.OrdinalIgnoreCase)
                || string.Equals(arg, "-at", StringComparison.OrdinalIgnoreCase)
                || string.Equals(arg, "-fpslog", StringComparison.OrdinalIgnoreCase));
        }

        /// <summary>
        /// How opaque the black backdrop is <paramref name="seconds"/> after the start (1 until the fade-out,
        /// then down to 0), with the fade-out starting at <paramref name="fadeOutStart"/>.
        /// </summary>
        public static float BackdropAlpha(float seconds, float fadeOutStart = FadeOutStartSeconds)
        {
            return 1f - Mathf.Clamp01((seconds - fadeOutStart) / FadeOutSeconds);
        }

        /// <summary>How opaque the logo is: it fades in over <see cref="FadeInSeconds"/>, then out with the backdrop.</summary>
        public static float LogoAlpha(float seconds, float fadeOutStart = FadeOutStartSeconds)
        {
            return Mathf.Clamp01(seconds / FadeInSeconds) * BackdropAlpha(seconds, fadeOutStart);
        }

        /// <summary>
        /// When a skip at <paramref name="seconds"/> starts the fade-out: now, or the planned time if
        /// that's sooner. The fade-out then takes its usual <see cref="FadeOutSeconds"/>.
        /// </summary>
        public static float SkipAt(float seconds, float fadeOutStart)
        {
            return Mathf.Min(seconds, fadeOutStart);
        }

        /// <summary>The splash time after a frame that took <paramref name="frameSeconds"/> (at most <see cref="MaxStepSeconds"/> counts).</summary>
        public static float Advance(float seconds, float frameSeconds)
        {
            return seconds + Mathf.Min(frameSeconds, MaxStepSeconds);
        }

        /// <summary>Shows <paramref name="logoTexture"/> on <paramref name="host"/> (it removes itself when done).</summary>
        public static SplashOverlay Attach(GameObject host, Texture logoTexture)
        {
            SplashOverlay splash = host.AddComponent<SplashOverlay>();
            splash.logo = logoTexture;
            return splash;
        }

        private void Update()
        {
            shownSeconds = Advance(shownSeconds, Time.unscaledDeltaTime);
            if (shownSeconds >= fadeOutStart + FadeOutSeconds)
            {
                Destroy(this);
            }
        }

        private void OnGUI()
        {
            // Drawn after (on top of) the rest of the window.
            GUI.depth = -1000;
            Event e = Event.current;
            if (e.type == EventType.MouseDown || e.type == EventType.KeyDown)
            {
                fadeOutStart = SkipAt(shownSeconds, fadeOutStart);
                e.Use();
            }
            if (e.type != EventType.Repaint)
            {
                return;
            }
            Color old = GUI.color;
            GUI.color = new Color(0f, 0f, 0f, BackdropAlpha(shownSeconds, fadeOutStart));
            GUI.DrawTexture(new Rect(0, 0, Screen.width, Screen.height), Texture2D.whiteTexture);
            if (logo != null)
            {
                float side = Mathf.Min(Screen.height * LogoHeightFraction, Screen.width * 0.9f);
                GUI.color = new Color(1f, 1f, 1f, LogoAlpha(shownSeconds, fadeOutStart));
                GUI.DrawTexture(new Rect((Screen.width - side) / 2f, (Screen.height - side) / 2f, side, side),
                    logo, ScaleMode.ScaleToFit, true);
            }
            GUI.color = old;
        }
    }
}

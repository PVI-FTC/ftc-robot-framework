using System.Collections.Generic;
using UnityEngine;

namespace VI_Arena
{
    /// <summary>
    /// The replay's look, copied from the Swing viewer's Theme class: a dark "mission control"
    /// style with near-black panels, hairline borders, white and grey text, uppercase labels and
    /// a few accent colors (cyan for data, amber for warnings, red for recording).
    ///
    /// <para>Every color and font the on-screen panels use comes from here, plus small drawing
    /// helpers for Unity's immediate-mode GUI (OnGUI). Keep the colors in step with
    /// TestHarness/th_src/src/viewer/java/pvi/harness/viewer/Theme.java so both viewers look the same.</para>
    /// </summary>
    public static class HudTheme
    {
        // ---- Colors (the same values as the Swing Theme) -------------------------------------------

        public static readonly Color Background = Hex(0x05070A);
        public static readonly Color Panel = Hex(0x0B0E12);
        public static readonly Color Raised = Hex(0x14191F);
        public static readonly Color Line = Hex(0x222931);
        public static readonly Color Text = Hex(0xE8ECEF);
        public static readonly Color Muted = Hex(0x7A858F);
        public static readonly Color Faint = Hex(0x4A535C);
        public static readonly Color Accent = Hex(0x4FC3F7);
        public static readonly Color Amber = Hex(0xFFB300);
        public static readonly Color Red = Hex(0xFF3B30);
        public static readonly Color Green = Hex(0x34C759);

        // ---- Fonts --------------------------------------------------------------------------------

        // A DIN-style face like the Swing viewer: Bahnschrift ships with Windows 10 and 11. Only
        // plain family names that Unity can load: "Bahnschrift Light" is not a separate family,
        // and Cascadia Mono (a "variable" font) draws the wrong text in Unity's IMGUI, so the
        // fixed-width font is Consolas.
        private static readonly string[] TextFamilies = { "Bahnschrift", "Segoe UI", "Arial" };
        private static readonly string[] NumberFamilies = TextFamilies;
        private static readonly string[] MonoFamilies = { "Consolas", "Courier New" };

        private static readonly Dictionary<string, GUIStyle> Styles = new Dictionary<string, GUIStyle>();

        /// <summary>A text style: normal, bold, "number" (light, for big readouts) or "mono".</summary>
        public static GUIStyle Style(string kind, int size)
        {
            string key = kind + size;
            if (Styles.TryGetValue(key, out GUIStyle style))
            {
                return style;
            }
            string[] families = kind == "mono" ? MonoFamilies : kind == "number" ? NumberFamilies : TextFamilies;
            style = new GUIStyle
            {
                font = Font.CreateDynamicFontFromOSFont(families, size),
                fontSize = size,
                fontStyle = kind == "bold" ? FontStyle.Bold : FontStyle.Normal,
                alignment = TextAnchor.UpperLeft,
                clipping = TextClipping.Overflow,
                wordWrap = false,
                richText = false,
                padding = new RectOffset(0, 0, 0, 0),
                margin = new RectOffset(0, 0, 0, 0),
            };
            style.normal.textColor = Text;
            Styles[key] = style;
            return style;
        }

        // ---- Drawing helpers ---------------------------------------------------------------------

        /// <summary>A filled rectangle.</summary>
        public static void Fill(Rect rect, Color color)
        {
            GUI.DrawTexture(rect, Texture2D.whiteTexture, ScaleMode.StretchToFill, true, 0, color, 0, 0);
        }

        /// <summary>A filled rectangle with round corners (radius in pixels); a circle when it is half the size.</summary>
        public static void Round(Rect rect, float radius, Color color)
        {
            GUI.DrawTexture(rect, Texture2D.whiteTexture, ScaleMode.StretchToFill, true, 0, color, 0, radius);
        }

        /// <summary>An outline with round corners.</summary>
        public static void RoundOutline(Rect rect, float radius, float width, Color color)
        {
            GUI.DrawTexture(rect, Texture2D.whiteTexture, ScaleMode.StretchToFill, true, 0, color, width, radius);
        }

        /// <summary>A filled circle.</summary>
        public static void Circle(Vector2 center, float radius, Color color)
        {
            Round(new Rect(center.x - radius, center.y - radius, 2 * radius, 2 * radius), radius, color);
        }

        /// <summary>A one-pixel frame around a rectangle.</summary>
        public static void Hairline(Rect rect, Color color)
        {
            Fill(new Rect(rect.x, rect.y, rect.width, 1), color);
            Fill(new Rect(rect.x, rect.yMax - 1, rect.width, 1), color);
            Fill(new Rect(rect.x, rect.y, 1, rect.height), color);
            Fill(new Rect(rect.xMax - 1, rect.y, 1, rect.height), color);
        }

        /// <summary>
        /// Text with its baseline at <paramref name="baselineY"/> (like Java's drawString), so the
        /// layout numbers can be copied from the Swing panels.
        /// </summary>
        public static void DrawText(string text, float x, float baselineY, GUIStyle style, Color color)
        {
            GUIStyle tinted = style;
            Color old = tinted.normal.textColor;
            tinted.normal.textColor = color;
            Vector2 size = tinted.CalcSize(new GUIContent(text));
            GUI.Label(new Rect(x, baselineY - Ascent(style), size.x + 2, size.y + 2), text, tinted);
            tinted.normal.textColor = old;
        }

        /// <summary>An uppercase label, the house style for titles and captions.</summary>
        public static void Label(string text, float x, float baselineY, int size, Color color)
        {
            DrawText(text.ToUpperInvariant(), x, baselineY, Style("normal", size), color);
        }

        /// <summary>How wide a piece of text is in a style, in pixels.</summary>
        public static float Width(string text, GUIStyle style)
        {
            return style.CalcSize(new GUIContent(text)).x;
        }

        /// <summary>How wide an uppercase label is.</summary>
        public static float LabelWidth(string text, int size)
        {
            return Width(text.ToUpperInvariant(), Style("normal", size));
        }

        /// <summary><paramref name="text"/> cut short with "..." so it fits <paramref name="maxWidth"/> pixels.</summary>
        public static string Ellipsize(string text, GUIStyle style, float maxWidth)
        {
            if (Width(text, style) <= maxWidth)
            {
                return text;
            }
            string shorter = text;
            while (shorter.Length > 0 && Width(shorter + "...", style) > maxWidth)
            {
                shorter = shorter.Substring(0, shorter.Length - 1);
            }
            return shorter + "...";
        }

        /// <summary>Distance from a text style's top to its baseline, in pixels.</summary>
        private static float Ascent(GUIStyle style)
        {
            return style.fontSize * 0.86f;
        }

        private static Color Hex(int rgb)
        {
            return new Color(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f);
        }
    }
}

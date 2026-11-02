using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using UnityEngine;

namespace VI_Arena
{
    /// <summary>
    /// The on-screen panels around the 3D field, laid out like the Swing viewer window:
    ///
    /// <code>
    ///   +---------------------------------------------------------------+
    ///   | FTC HARNESS         T+ 00:01.24   REPLAY · TELEOP · LOOP      |  header
    ///   | REPLAY [ Replaying file.json                         v ] [..] |  chooser
    ///   +----------------------------------------+----------------------+
    ///   | caption                                | VEHICLE              |
    ///   |                                        | POSITION             |
    ///   |              3D field                  | MOTORS               |
    ///   |                                        | DRIVER STATION       |
    ///   | legend                                 | EVENTS               |
    ///   |                                        | [driver] [operator]  |
    ///   +----------------------------------------+----------------------+
    ///   | |&lt; &lt; PLAY &gt; &gt;|  SPEED 1x   T+ 00:01.24 / 00:07.00  ======o---- |  controls
    ///   +---------------------------------------------------------------+
    /// </code>
    ///
    /// The sizes, offsets and words are copied from the Swing classes MissionHeader,
    /// TelemetryPanel, GamepadIndicator and ViewerWindow, so the two viewers match. Drawn with
    /// Unity's immediate-mode GUI (OnGUI); colors and fonts come from <see cref="HudTheme"/>.
    /// </summary>
    public sealed class ReplayHud
    {
        // ---- Layout (pixels), from the Swing viewer ------------------------------------------------

        public const float HeaderHeight = 58f;
        public const float ChooserHeight = 66f;
        public const float ControlsHeight = 56f;
        public const float RightColumnWidth = 424f;
        public const float GamepadPanelHeight = 146f;
        public const float FieldCaptionBand = 30f;
        public const float FieldLegendBand = 30f;
        private const float Pad = 14f;
        private const float CardGap = 10f;

        private static readonly CultureInfo Dot = CultureInfo.InvariantCulture;

        /// <summary>Where each part goes for the current window size.</summary>
        public struct Layout
        {
            public Rect header, chooser, controls, fieldPanel, field, cards, gamepads;

            /// <summary>Lays the parts out for a window <paramref name="width"/> x <paramref name="height"/> pixels.</summary>
            public static Layout For(float width, float height)
            {
                float top = HeaderHeight + ChooserHeight;
                float middleHeight = Mathf.Max(100, height - top - ControlsHeight);
                var layout = new Layout
                {
                    header = new Rect(0, 0, width, HeaderHeight),
                    chooser = new Rect(0, HeaderHeight, width, ChooserHeight),
                    controls = new Rect(0, height - ControlsHeight, width, ControlsHeight),
                    fieldPanel = new Rect(0, top, width - RightColumnWidth, middleHeight),
                };
                layout.field = new Rect(layout.fieldPanel.x + 8, layout.fieldPanel.y + FieldCaptionBand,
                    layout.fieldPanel.width - 16, layout.fieldPanel.height - FieldCaptionBand - FieldLegendBand);
                var column = new Rect(width - RightColumnWidth, top, RightColumnWidth, middleHeight);
                layout.gamepads = new Rect(column.x, column.yMax - GamepadPanelHeight, column.width, GamepadPanelHeight);
                layout.cards = new Rect(column.x, column.y, column.width, column.height - GamepadPanelHeight);
                return layout;
            }
        }

        // Dropdowns and dragging state.
        private bool recordingMenuOpen;
        /// <summary>True while the Visuals screen (arena and setting) is open. V or Esc closes it.</summary>
        public bool VisualsOpen { get; set; }
        private bool speedMenuOpen;
        private Vector2 recordingMenuScroll;
        private Vector2 cardsScroll;
        private float cardsContentHeight = 900;
        private int sliderControlId;

        /// <summary>
        /// Draws every panel. Background parts first, then dropdown lists last so they sit on top.
        /// </summary>
        public void Draw(ReplayController replay, Layout layout)
        {
            RunRecordingData recording = replay.Recording;
            ReplayTimeline timeline = replay.Timeline;

            // Everything except the 3D field area is painted over, like the Swing window's panels.
            DrawFrameAroundField(layout);
            DrawHeader(layout.header, recording, timeline, replay.ShownFileName, replay.IsLive);
            DrawChooser(layout.chooser, replay);
            DrawViewButtons(layout, replay.Orbit);
            if (recording != null)
            {
                DrawFieldTexts(layout, recording, timeline);
                DrawCameraPicture(layout, replay, recording.loops[timeline.Index], recording, timeline.PoseAtPlayhead());
                DrawCards(layout.cards, recording, timeline.Index, replay.ShowAprilTags);
                DrawGamepads(layout.gamepads, recording.loops[timeline.Index]);
                DrawControls(layout.controls, recording, timeline, replay);
            }
            else
            {
                HudTheme.Fill(layout.cards, HudTheme.Background);
                HudTheme.Fill(layout.gamepads, HudTheme.Panel);
                HudTheme.Fill(layout.controls, HudTheme.Panel);
            }
            if (replay.ErrorMessage != null)
            {
                var box = new Rect(layout.field.x + 20, layout.field.y + 20, Mathf.Min(640, layout.field.width - 40), 96);
                HudTheme.Fill(box, HudTheme.Panel);
                HudTheme.Hairline(box, HudTheme.Amber);
                GUI.Label(new Rect(box.x + 12, box.y + 10, box.width - 24, box.height - 20), replay.ErrorMessage,
                    WrapStyle(HudTheme.Style("normal", 13), HudTheme.Amber));
            }

            // Lists that drop down over everything else.
            if (recordingMenuOpen)
            {
                DrawRecordingMenu(layout.chooser, replay);
            }
            if (speedMenuOpen && timeline != null)
            {
                DrawSpeedMenu(timeline);
            }
            if (VisualsOpen)
            {
                DrawVisuals(layout, replay);
            }
        }

        // ---- Header --------------------------------------------------------------------------------

        private void DrawHeader(Rect r, RunRecordingData recording, ReplayTimeline timeline, string fileName, bool isLive)
        {
            HudTheme.Fill(r, HudTheme.Panel);
            HudTheme.Fill(new Rect(r.x, r.yMax - 1, r.width, 1), HudTheme.Line);

            // Left: the mark and what is shown.
            HudTheme.Fill(new Rect(20, 14, 3, 30), HudTheme.Text);
            HudTheme.Label("VI Arena", 34, 27, 14, HudTheme.Text);
            if (recording != null)
            {
                HudTheme.Label(HudText.Subtitle(recording.header, fileName), 34, 43, 10, HudTheme.Muted);

                // Middle: the mission clock, kept quiet, with the stage under it.
                string clock = "T+ " + HudText.Clock(timeline.Current.matchTimeSeconds);
                GUIStyle clockStyle = HudTheme.Style("number", 20);
                float clockWidth = HudTheme.Width(clock, clockStyle);
                float centerX = r.width / 2f;
                HudTheme.DrawText(clock, centerX - clockWidth / 2f, 28, clockStyle, HudTheme.Muted);
                HudTheme.Fill(new Rect(centerX - clockWidth / 2f - 46, 21, 30, 1), HudTheme.Line);
                HudTheme.Fill(new Rect(centerX + clockWidth / 2f + 26, 21, 30, 1), HudTheme.Line);

                string stage = HudText.StageLine(recording, timeline.Index, isLive ? "Live" : "Replay");
                float stageWidth = HudTheme.LabelWidth(stage, 9);
                float stageX = centerX - stageWidth / 2f + 7;
                HudTheme.Circle(new Vector2(stageX - 10, 41), 3, StageColor(timeline.Current.stage));
                HudTheme.Label(stage, stageX, 45, 9, HudTheme.Faint);
            }

            // Right: where the Swing viewer has its Record button: "LIVE" with a lit dot, or "3D replay".
            var badge = new Rect(r.xMax - 16 - 120, 15, 120, 28);
            HudTheme.Hairline(badge, isLive ? HudTheme.Green : HudTheme.Line);
            if (isLive)
            {
                HudTheme.Circle(new Vector2(badge.x + 17, badge.y + 14), 5, HudTheme.Green);
                HudTheme.Label("Live", badge.x + 30, badge.y + 19, 11, HudTheme.Text);
            }
            else
            {
                HudTheme.RoundOutline(new Rect(badge.x + 12, badge.y + 9, 10, 10), 5, 1.2f, HudTheme.Muted);
                HudTheme.Label("3D replay", badge.x + 30, badge.y + 19, 11, HudTheme.Muted);
            }
        }

        /// <summary>Amber before PLAY, green while running, grey once stopped (as in the Swing header).</summary>
        private static Color StageColor(string stage)
        {
            switch (stage)
            {
                case "INIT":
                case "INIT_LOOP":
                    return HudTheme.Amber;
                case "START":
                case "LOOP":
                    return HudTheme.Green;
                default:
                    return HudTheme.Muted;
            }
        }

        // ---- Recording chooser ------------------------------------------------------------------

        private void DrawChooser(Rect r, ReplayController replay)
        {
            HudTheme.Fill(r, HudTheme.Background);
            HudTheme.Fill(new Rect(r.x, r.yMax - 1, r.width, 1), HudTheme.Line);
            HudTheme.Label(replay.IsLive ? "Live" : "Replay", 28, r.y + 30, 10, replay.IsLive ? HudTheme.Green : HudTheme.Muted);

            // The dropdown box: shows what is replayed; click to pick another recording.
            var box = new Rect(112, r.y + 10, r.width - 112 - 16 - OpenButtonWidth - 12, 28);
            HudTheme.Fill(box, HudTheme.Panel);
            HudTheme.Hairline(box, HudTheme.Line);
            string shown = replay.ShownFileName == null ? "No recording open"
                : replay.IsLive ? replay.ShownFileName : "Replaying " + replay.ShownFileName;
            HudTheme.DrawText(HudTheme.Ellipsize(shown, HudTheme.Style("normal", 13), box.width - 40), box.x + 9, box.y + 19,
                HudTheme.Style("normal", 13), HudTheme.Text);
            DrawDownArrow(new Vector2(box.xMax - 16, box.y + 13), HudTheme.Muted);
            if (Clicked(box))
            {
                recordingMenuOpen = !recordingMenuOpen;
                speedMenuOpen = false;
                if (recordingMenuOpen)
                {
                    replay.RefreshRecordingList();
                    replay.RefreshLiveChoices();
                }
            }

            // "Open recording..." opens a file picker, like the Swing viewer's button (Unity's in the
            // Editor, Windows' own in the built app), for recordings that aren't in the list.
            var button = new Rect(r.xMax - 16 - OpenButtonWidth, r.y + 10, OpenButtonWidth, 28);
            if (FlatButton(button, "Open recording..."))
            {
                recordingMenuOpen = false;
                replay.OpenFilePicker();
            }

            // Under the box: what is shown, or the live status (what to press, which controllers).
            string showing = replay.ShownFileName == null ? "" : "Showing " + replay.ShownFileName;
            Color showingColor = HudTheme.Muted;
            if (replay.IsLive)
            {
                showing = HudText.LiveStatus(replay.Live, replay.LiveSaved);
                showingColor = replay.Live.Initialized ? HudTheme.Amber : replay.Live.Playing ? HudTheme.Green : HudTheme.Muted;
                if (replay.Live.ControllerProblem != null)
                {
                    showing += "  ·  " + replay.Live.ControllerProblem;
                    showingColor = HudTheme.Amber;
                }
            }
            if (replay.Notice != null)
            {
                showing += "  ·  " + replay.Notice;
            }
            HudTheme.DrawText(HudTheme.Ellipsize(showing, HudTheme.Style("normal", 12),
                r.width - 56 - FieldButtonWidth - PhysicsButtonWidth - LiveModeButtonWidth - 32), 28, r.y + 56,
                HudTheme.Style("normal", 12), showingColor);

            // Visuals: opens the screen for picking the arena and the setting around it (V).
            var fieldButton = new Rect(r.xMax - 16 - FieldButtonWidth, r.y + 42, FieldButtonWidth, 22);
            if (FlatButton(fieldButton, "Visuals: " + replay.FieldName + " · " + replay.SettingName + "  (V)"))
            {
                VisualsOpen = !VisualsOpen;
                recordingMenuOpen = false;
                speedMenuOpen = false;
            }

            // Who moves the robot in the next live run: the harness's physics or Unity's (U).
            var physicsButton = new Rect(fieldButton.x - 8 - PhysicsButtonWidth, r.y + 42, PhysicsButtonWidth, 22);
            if (FlatButton(physicsButton, "Physics: " + (replay.UnityPhysicsChosen ? "Unity" : "Harness") + "  (U)"))
            {
                replay.TogglePhysics();
            }

            // Live mode: starts (or stops) the harness side, so nobody has to type gradlew unityBridge.
            var liveButton = new Rect(physicsButton.x - 8 - LiveModeButtonWidth, r.y + 42, LiveModeButtonWidth, 22);
            HarnessLauncher.LaunchState state = replay.LiveModeState;
            string liveText = state == HarnessLauncher.LaunchState.Running ? "Live mode: On"
                : state == HarnessLauncher.LaunchState.Starting ? "Live mode: Starting..."
                : state == HarnessLauncher.LaunchState.Failed ? "Live mode: Failed (retry)" : "Live mode: Off";
            if (FlatButton(liveButton, liveText + "  (L)"))
            {
                replay.ToggleLiveMode();
            }
            Color lamp = state == HarnessLauncher.LaunchState.Running ? HudTheme.Green
                : state == HarnessLauncher.LaunchState.Starting ? HudTheme.Amber
                : state == HarnessLauncher.LaunchState.Failed ? HudTheme.Red : HudTheme.Faint;
            HudTheme.Circle(new Vector2(liveButton.x + 10, liveButton.y + 11), 3.5f, lamp);
        }

        private const float OpenButtonWidth = 136;
        private const float FieldButtonWidth = 330;
        private const float PhysicsButtonWidth = 170;
        private const float LiveModeButtonWidth = 200;

        // ---- Visuals screen -------------------------------------------------------------------------

        /// <summary>
        /// The Visuals screen, over the field: pick the arena (the plain field or a season field file),
        /// the setting around it (<see cref="SettingBuilder.All"/>) and how the robot looks (the box or
        /// a robot's 3D model from TestHarness/th_src/robots). Choices apply at once and are remembered. Clicking outside it, Done, V or Esc closes it.
        /// </summary>
        private void DrawVisuals(Layout layout, ReplayController replay)
        {
            Rect area = layout.fieldPanel;
            HudTheme.Fill(area, new Color(HudTheme.Background.r, HudTheme.Background.g, HudTheme.Background.b, 0.72f));
            float width = Mathf.Min(780, area.width - 40), height = Mathf.Min(580, area.height - 40);
            var panel = new Rect(area.x + (area.width - width) / 2f, area.y + (area.height - height) / 2f, width, height);
            HudTheme.Fill(panel, HudTheme.Panel);
            HudTheme.Hairline(panel, HudTheme.Line);
            HudTheme.Fill(new Rect(panel.x + 20, panel.y + 18, 3, 24), HudTheme.Accent);
            HudTheme.Label("Visuals", panel.x + 32, panel.y + 35, 14, HudTheme.Text);
            HudTheme.DrawText("Pick the arena, what surrounds it and the robot. Turn the view with T (3D), right-drag and scroll.",
                panel.x + 32, panel.y + 58, HudTheme.Style("normal", 12), HudTheme.Muted);

            // Arena: the plain field, then each season field file.
            float x = panel.x + 24, y = panel.y + 78;
            HudTheme.Label("Arena", x, y + 12, 10, HudTheme.Muted);
            y += 22;
            var arenas = new List<(string id, string name, string detail)> { (FieldLayouts.PlainFieldId, "Plain field", "Tiles and walls only") };
            foreach (FieldLayoutData field in replay.Fields)
            {
                arenas.Add((field.id, field.name, field.season + " season"));
            }
            float gap = 12, cardWidth = (panel.width - 48 - gap * 2) / 3f;
            for (int i = 0; i < arenas.Count; i++)
            {
                var card = new Rect(x + (i % 3) * (cardWidth + gap), y + (i / 3) * 64, cardWidth, 54);
                if (ChoiceCard(card, arenas[i].name, arenas[i].detail, arenas[i].id == replay.FieldId))
                {
                    replay.SelectField(arenas[i].id);
                }
            }
            y += ((arenas.Count + 2) / 3) * 64 + 14;

            // Setting: two cards per row.
            HudTheme.Label("Setting", x, y + 12, 10, HudTheme.Muted);
            y += 22;
            float settingWidth = (panel.width - 48 - gap) / 2f;
            for (int i = 0; i < SettingBuilder.All.Count; i++)
            {
                SettingInfo setting = SettingBuilder.All[i];
                var card = new Rect(x + (i % 2) * (settingWidth + gap), y + (i / 2) * 78, settingWidth, 68);
                if (ChoiceCard(card, setting.name, setting.description, setting.id == replay.SettingId, setting.sky))
                {
                    replay.SelectSetting(setting.id);
                }
            }
            y += ((SettingBuilder.All.Count + 1) / 2) * 78 + 6;

            // Robot: the plain box (the recording's size), then each robot's 3D model (its true size).
            HudTheme.Label("Robot", x, y + 12, 10, HudTheme.Muted);
            y += 22;
            var looks = new List<(string id, string name, string detail)> { (RobotProfile.BoxId, "Box", "The recording's robot size") };
            foreach (RobotProfile robot in replay.Robots)
            {
                looks.Add((robot.Id, robot.Name, "3D model, true size"));
            }
            for (int i = 0; i < looks.Count; i++)
            {
                var card = new Rect(x + (i % 3) * (cardWidth + gap), y + (i / 3) * 64, cardWidth, 54);
                if (ChoiceCard(card, looks[i].name, looks[i].detail, looks[i].id == replay.RobotId))
                {
                    replay.SelectRobot(looks[i].id);
                }
            }

            // What the webcams saw: on or off (the A key does the same).
            var tagsSwitch = new Rect(panel.x + 24, panel.yMax - 20 - 28, 260, 28);
            if (FlatButton(tagsSwitch, "AprilTag sight lines: " + (replay.ShowAprilTags ? "On" : "Off") + "  (A)"))
            {
                replay.SetShowAprilTags(!replay.ShowAprilTags);
            }
            var pictureSwitch = new Rect(tagsSwitch.xMax + 12, tagsSwitch.y, 230, 28);
            if (FlatButton(pictureSwitch, "Webcam picture: " + (replay.ShowCameraPicture ? "On" : "Off") + "  (C)"))
            {
                replay.SetShowCameraPicture(!replay.ShowCameraPicture);
            }

            var done = new Rect(panel.xMax - 24 - 110, panel.yMax - 20 - 28, 110, 28);
            if (FlatButton(done, "Done  (V)"))
            {
                VisualsOpen = false;
            }
            else if (Event.current.type == EventType.MouseDown && area.Contains(Event.current.mousePosition)
                && !panel.Contains(Event.current.mousePosition))
            {
                VisualsOpen = false; // a click outside the panel closes it
                Event.current.Use();
            }
        }

        /// <summary>A choice on the Visuals screen: name and a line under it, lit when chosen, with an optional color swatch.</summary>
        private static bool ChoiceCard(Rect r, string name, string detail, bool chosen, Color? swatch = null)
        {
            bool hover = r.Contains(Event.current.mousePosition);
            HudTheme.Fill(r, chosen ? HudTheme.Raised : hover ? HudTheme.Raised : HudTheme.Background);
            HudTheme.Hairline(r, chosen ? HudTheme.Accent : HudTheme.Line);
            float textX = r.x + 12;
            if (swatch.HasValue)
            {
                var chip = new Rect(r.x + 10, r.y + 10, 18, 18);
                HudTheme.Fill(chip, swatch.Value);
                HudTheme.Hairline(chip, HudTheme.Line);
                textX = chip.xMax + 10;
            }
            if (chosen)
            {
                HudTheme.Circle(new Vector2(r.xMax - 14, r.y + 14), 4, HudTheme.Accent);
            }
            HudTheme.DrawText(name, textX, r.y + 23, HudTheme.Style("normal", 14), chosen ? HudTheme.Text : HudTheme.Muted);
            GUI.Label(new Rect(r.x + 12, r.y + 31, r.width - 24, r.height - 33), detail, WrapStyle(HudTheme.Style("normal", 11), HudTheme.Faint));
            return Clicked(r);
        }

        private void DrawRecordingMenu(Rect chooser, ReplayController replay)
        {
            // Live entries first (OpModes the harness offers, or how to start it), then recordings.
            List<string> files = replay.RecordingsNewestFirst;
            List<LiveSession.OpModeChoice> liveChoices = replay.LiveChoices;
            int liveRows = liveChoices == null ? 1 : liveChoices.Count;
            int rows = liveRows + files.Count;
            const float rowHeight = 24;
            var box = new Rect(112, chooser.y + 38, chooser.width - 112 - 16 - 104, Mathf.Min(rows, 16) * rowHeight);
            HudTheme.Fill(box, HudTheme.Panel);
            HudTheme.Hairline(box, HudTheme.Line);
            recordingMenuScroll = GUI.BeginScrollView(box, recordingMenuScroll,
                new Rect(0, 0, box.width - 16, rows * rowHeight), false, rows > 16, GUIStyle.none, GUI.skin.verticalScrollbar);
            float rowWidth = box.width - (rows > 16 ? 16 : 0);
            GUIStyle text = HudTheme.Style("normal", 13);
            for (int i = 0; i < liveRows; i++)
            {
                var row = new Rect(0, i * rowHeight, rowWidth, rowHeight);
                if (liveChoices == null && replay.LiveProblem != null)
                {
                    // The harness side runs but can't offer OpModes: show why (the error box has the full text).
                    HudTheme.Fill(row, HudTheme.Panel);
                    HudTheme.DrawText(HudTheme.Ellipsize(replay.LiveProblem, text, rowWidth - 16), row.x + 8, row.y + 17, text, HudTheme.Amber);
                    continue;
                }
                if (liveChoices == null)
                {
                    // Not running yet: this row is a button that switches Live mode on.
                    bool startable = replay.LiveModeState != HarnessLauncher.LaunchState.Starting;
                    bool hoverHint = startable && row.Contains(Event.current.mousePosition);
                    HudTheme.Fill(row, hoverHint ? HudTheme.Raised : HudTheme.Panel);
                    string hint = startable ? "Start live mode (L): starts the harness side, then the live OpModes appear here"
                        : replay.LiveUnavailable ?? "Live mode is starting...";
                    HudTheme.DrawText(HudTheme.Ellipsize(hint, text, rowWidth - 16), row.x + 8, row.y + 17, text,
                        startable ? HudTheme.Green : HudTheme.Faint);
                    if (startable && Clicked(row))
                    {
                        recordingMenuOpen = false;
                        replay.ToggleLiveMode();
                    }
                    continue;
                }
                LiveSession.OpModeChoice choice = liveChoices[i];
                bool hover = row.Contains(Event.current.mousePosition);
                HudTheme.Fill(row, hover ? HudTheme.Raised : HudTheme.Panel);
                HudTheme.DrawText("Live: " + choice.title + "  (" + choice.period + ", controllers)", row.x + 8, row.y + 17, text,
                    HudTheme.Green);
                if (Clicked(row))
                {
                    recordingMenuOpen = false;
                    replay.StartLive(choice);
                }
            }
            for (int i = 0; i < files.Count; i++)
            {
                var row = new Rect(0, (liveRows + i) * rowHeight, rowWidth, rowHeight);
                bool current = !replay.IsLive && replay.ShownFileName == Path.GetFileName(files[i]);
                bool hover = row.Contains(Event.current.mousePosition);
                HudTheme.Fill(row, current ? HudTheme.Accent : hover ? HudTheme.Raised : HudTheme.Panel);
                HudTheme.DrawText("Replay: " + Path.GetFileName(files[i]), row.x + 8, row.y + 17, text,
                    current ? HudTheme.Background : HudTheme.Text);
                if (Clicked(row))
                {
                    recordingMenuOpen = false;
                    replay.Open(files[i]);
                }
            }
            GUI.EndScrollView();
            CloseMenusOnOutsideClick(box);
        }

        // ---- Field captions ------------------------------------------------------------------------

        private static void DrawFrameAroundField(Layout layout)
        {
            // The bands above and below the 3D view, and the left/right strips, are panel colored.
            Rect p = layout.fieldPanel;
            Rect f = layout.field;
            HudTheme.Fill(new Rect(p.x, p.y, p.width, f.y - p.y), HudTheme.Background);
            HudTheme.Fill(new Rect(p.x, f.yMax, p.width, p.yMax - f.yMax), HudTheme.Background);
            HudTheme.Fill(new Rect(p.x, f.y, f.x - p.x, f.height), HudTheme.Background);
            HudTheme.Fill(new Rect(f.xMax, f.y, p.xMax - f.xMax, f.height), HudTheme.Background);
            HudTheme.Fill(new Rect(p.xMax - 1, p.y, 1, p.height), HudTheme.Line);
            // Corner ticks around the view, like the Swing field frame.
            HudTheme.Hairline(f, HudTheme.Line);
            const float t = 6;
            Color tick = HudTheme.Muted;
            HudTheme.Fill(new Rect(f.x, f.y, t, 1), tick);
            HudTheme.Fill(new Rect(f.x, f.y, 1, t), tick);
            HudTheme.Fill(new Rect(f.xMax - t, f.y, t, 1), tick);
            HudTheme.Fill(new Rect(f.xMax - 1, f.y, 1, t), tick);
            HudTheme.Fill(new Rect(f.x, f.yMax - 1, t, 1), tick);
            HudTheme.Fill(new Rect(f.x, f.yMax - t, 1, t), tick);
            HudTheme.Fill(new Rect(f.xMax - t, f.yMax - 1, t, 1), tick);
            HudTheme.Fill(new Rect(f.xMax - 1, f.yMax - t, 1, t), tick);
        }

        private static void DrawFieldTexts(Layout layout, RunRecordingData recording, ReplayTimeline timeline)
        {
            Rect f = layout.field;
            string caption = HudText.FieldCaption(recording, timeline.Index, timeline.PoseAtPlayhead());
            HudTheme.Label(HudTheme.Ellipsize(caption.ToUpperInvariant(), HudTheme.Style("normal", 12), f.width - ViewButtonsWidth - 16),
                f.x + 2, f.y - 10, 12, HudTheme.Text);
            DriveModelData model = recording.header.driveModel;
            HudTheme.Label(HudTheme.Ellipsize(HudText.Legend(model).ToUpperInvariant(), HudTheme.Style("normal", 12), f.width),
                f.x + 2, f.yMax + 20, 12, HudText.SizeIsPlaceholder(model) ? HudTheme.Amber : HudTheme.Muted);
        }

        // ---- Webcam picture --------------------------------------------------------------------------

        /// <summary>
        /// The C key's small picture in the field's bottom-right corner: what the shown webcam sees, from
        /// its mount, with a box and id on each tag the harness says it detected in this loop (only tags
        /// on the shown field; see <see cref="AprilTagSightView.Target"/>). Says so when the recording
        /// has no webcam setup (older recordings, or a robot without a webcam).
        /// </summary>
        private static void DrawCameraPicture(Layout layout, ReplayController replay, LoopData loop, RunRecordingData recording, PoseData pose)
        {
            if (!replay.ShowCameraPicture)
            {
                return;
            }
            Rect f = layout.field;
            const float margin = 12, titleHeight = 22;
            CameraMount mount = replay.ShownCamera;
            float width = Mathf.Min(360, f.width * 0.34f);
            if (mount == null)
            {
                var note = new Rect(f.xMax - margin - width, f.yMax - margin - 44, width, 44);
                HudTheme.Fill(note, HudTheme.Panel);
                HudTheme.Hairline(note, HudTheme.Line);
                HudTheme.Label("WEBCAM", note.x + 10, note.y + 17, 10, HudTheme.Muted);
                HudTheme.Label(HudTheme.Ellipsize(HudText.NoCameraText(recording.header.version) + "  (C hides)", HudTheme.Style("normal", 11), width - 20),
                    note.x + 10, note.y + 35, 11, HudTheme.Faint);
                return;
            }
            float height = width / mount.Aspect;
            var picture = new Rect(f.xMax - margin - width, f.yMax - margin - height, width, height);
            var title = new Rect(picture.x, picture.y - titleHeight, width, titleHeight);
            HudTheme.Fill(title, HudTheme.Panel);
            string text = "WEBCAM · " + mount.Name + (replay.CameraMounts.Count > 1 ? "  " + (replay.CameraIndex + 1) + "/" + replay.CameraMounts.Count : "")
                + " · " + mount.WidthPixels + "×" + mount.HeightPixels + (mount.LensKnown ? "" : " · FOV NOT MEASURED")
                + (mount.MountMeasured ? "" : " · MOUNT NOT MEASURED");
            HudTheme.Label(HudTheme.Ellipsize(text.ToUpperInvariant(), HudTheme.Style("normal", 10), width - 16), title.x + 8, title.y + 15, 10,
                mount.LensKnown && mount.MountMeasured ? HudTheme.Muted : HudTheme.Amber);
            if (replay.CameraPictureTexture != null)
            {
                GUI.DrawTexture(picture, replay.CameraPictureTexture, ScaleMode.StretchToFill, false);
            }
            else
            {
                HudTheme.Fill(picture, HudTheme.Background);
            }
            HudTheme.Hairline(picture, HudTheme.Line);

            // A box and id on each tag this webcam detected in this loop that is on the shown field:
            // where it was measured (cyan), or on the real tag when measured elsewhere (amber).
            var notOnField = new List<int>();
            GUI.BeginGroup(picture);
            var inside = new Rect(0, 0, picture.width, picture.height);
            foreach (AprilTagSeenData tag in loop.aprilTags)
            {
                if (tag.camera != mount.Name || pose == null)
                {
                    continue;
                }
                AprilTagSightView.TargetKind kind = AprilTagSightView.Target(pose, tag, mount, replay.FieldTags, out Vector3 spot);
                if (kind == AprilTagSightView.TargetKind.NotOnField)
                {
                    notOnField.Add(tag.id);
                }
                AprilTagSeenData shown = kind == AprilTagSightView.TargetKind.MeasuredElsewhere ? mount.SeenFrom(pose, spot, tag.id) : tag;
                if (!CameraPictureView.TagBox(mount, shown, replay.TagSizeInches(tag.id), inside, out Rect box))
                {
                    continue;
                }
                Color color = kind == AprilTagSightView.TargetKind.OnTag ? HudTheme.Accent : HudTheme.Amber;
                if (kind == AprilTagSightView.TargetKind.NotOnField)
                {
                    DashedBox(box, color);
                }
                else
                {
                    HudTheme.Hairline(box, color);
                    HudTheme.Hairline(new Rect(box.x + 1, box.y + 1, box.width - 2, box.height - 2), color);
                }
                HudTheme.Label(AprilTagSightView.LabelText(tag.id, kind), box.x, box.y - 3, 11, color);
            }
            GUI.EndGroup();
            string note2 = HudText.NotOnFieldText(notOnField, replay.FieldName);
            if (note2 != null)
            {
                var strip = new Rect(picture.x, picture.yMax - 20, picture.width, 20);
                HudTheme.Fill(strip, new Color(HudTheme.Panel.r, HudTheme.Panel.g, HudTheme.Panel.b, 0.85f));
                HudTheme.Label(HudTheme.Ellipsize(note2, HudTheme.Style("normal", 10), picture.width - 16), strip.x + 8, strip.y + 14, 10, HudTheme.Amber);
            }
        }

        /// <summary>A box outline drawn as short dashes, for a detection that isn't a tag on the shown field.</summary>
        private static void DashedBox(Rect r, Color color)
        {
            const float dash = 4, gap = 3;
            for (float x = r.x; x < r.xMax; x += dash + gap)
            {
                float w = Mathf.Min(dash, r.xMax - x);
                HudTheme.Fill(new Rect(x, r.y, w, 2), color);
                HudTheme.Fill(new Rect(x, r.yMax - 2, w, 2), color);
            }
            for (float y = r.y; y < r.yMax; y += dash + gap)
            {
                float h = Mathf.Min(dash, r.yMax - y);
                HudTheme.Fill(new Rect(r.x, y, 2, h), color);
                HudTheme.Fill(new Rect(r.xMax - 2, y, 2, h), color);
            }
        }

        private const float ViewButtonWidth = 74, ViewButtonGap = 4;
        private static float ViewButtonsWidth => OrbitCamera.Presets.Count * (ViewButtonWidth + ViewButtonGap);

        /// <summary>
        /// The preset viewpoint buttons, right-aligned above the field: "1 Audience" ... "7 Chase". The one
        /// last picked is lit; turning the view by hand clears it.
        /// </summary>
        private static void DrawViewButtons(Layout layout, OrbitCamera orbit)
        {
            if (orbit == null)
            {
                return;
            }
            float x = layout.field.xMax - ViewButtonsWidth;
            float y = layout.fieldPanel.y + 5;
            for (int i = 0; i < OrbitCamera.Presets.Count; i++)
            {
                var button = new Rect(x + i * (ViewButtonWidth + ViewButtonGap), y, ViewButtonWidth, 20);
                bool chosen = orbit.CurrentPreset == i;
                bool hover = button.Contains(Event.current.mousePosition);
                HudTheme.Fill(button, chosen || hover ? HudTheme.Raised : HudTheme.Panel);
                HudTheme.Hairline(button, chosen ? HudTheme.Accent : HudTheme.Line);
                string text = (i + 1) + " " + OrbitCamera.Presets[i].label;
                float w = HudTheme.LabelWidth(text, 10);
                HudTheme.Label(text, button.x + (button.width - w) / 2f, button.y + 14, 10, chosen ? HudTheme.Text : HudTheme.Muted);
                if (Clicked(button))
                {
                    orbit.ApplyPreset(i);
                }
            }
        }

        // ---- Cards (the right-hand column) ---------------------------------------------------------

        private void DrawCards(Rect area, RunRecordingData recording, int index, bool showAprilTags)
        {
            HudTheme.Fill(area, HudTheme.Background);
            bool scroll = cardsContentHeight > area.height;
            float width = area.width - 2 * Pad - (scroll ? 8 : 0);
            cardsScroll = GUI.BeginScrollView(area, cardsScroll, new Rect(0, 0, area.width - (scroll ? 12 : 0), cardsContentHeight),
                false, scroll, GUIStyle.none, scroll ? GUI.skin.verticalScrollbar : GUIStyle.none);

            HeaderData header = recording.header;
            LoopData loop = recording.loops[index];
            float y = Pad;

            // Vehicle: what is running.
            y = Card("Vehicle", y, width, 44, top =>
            {
                // Each value is cut to its column ("TeamAAprilTagVisi...") so a long OpMode name can't run
                // into PERIOD; the full name is in the header line above.
                Readout(Pad + 12, top, "OpMode", VehicleText(header.opMode, width, 0));
                Readout(Pad + 12 + width * 2 / 5, top, "Period", VehicleText(header.period ?? "-", width, 1));
                Readout(Pad + 12 + width * 7 / 10, top, "Robot", VehicleText(header.robotConfig, width, 2));
            }) + CardGap;

            // Position: big numbers.
            PoseData pose = loop.pose;
            y = Card("Position", y, width, 58, top =>
            {
                if (pose == null)
                {
                    HudTheme.DrawText("NOT TRACKED", Pad + 12, top + 32, HudTheme.Style("normal", 16), HudTheme.Muted);
                    return;
                }
                BigReadout(Pad + 12, top, "X", (pose.xInches / 12.0).ToString("0.00", Dot), "ft");
                BigReadout(Pad + 12 + width / 3, top, "Y", (pose.yInches / 12.0).ToString("0.00", Dot), "ft");
                BigReadout(Pad + 12 + width * 2 / 3, top, "Heading",
                    (pose.headingRadians * 180 / Math.PI).ToString("0.0", Dot), "deg");
            }) + CardGap;

            if (loop.autoStep != null)
            {
                y = Card("Auto step", y, width, 28, top =>
                    HudTheme.DrawText(HudTheme.Ellipsize(loop.autoStep, HudTheme.Style("normal", 15), width - 24), Pad + 12, top + 20,
                        HudTheme.Style("normal", 15), HudTheme.Accent)) + CardGap;
            }

            // AprilTags the camera saw at this moment (only for runs that ever saw one), as the Swing viewer shows them.
            if (showAprilTags && HudText.AnyAprilTags(recording))
            {
                List<string> tags = HudText.AprilTagLines(loop);
                y = Card("Tags in view", y, width, tags.Count * 17 + 2, top =>
                {
                    GUIStyle mono = HudTheme.Style("mono", 12);
                    float lineY = top + 13;
                    foreach (string line in tags)
                    {
                        HudTheme.DrawText(HudTheme.Ellipsize(line, mono, width - 24), Pad + 12, lineY, mono,
                            loop.aprilTags.Count == 0 ? HudTheme.Muted : HudTheme.Accent);
                        lineY += 17;
                    }
                }) + CardGap;
            }

            // Motors and servos: a gauge each.
            int rows = loop.motors.Count + loop.servos.Count;
            y = Card("Motors" + (loop.servos.Count == 0 ? "" : " / servos"), y, width, Mathf.Max(1, rows) * 22, top =>
            {
                float rowY = top;
                foreach (MotorData motor in loop.motors)
                {
                    GaugeRow(rowY, width, motor.name, motor.power, true, HudText.Power(motor.power));
                    rowY += 22;
                }
                foreach (ServoData servo in loop.servos)
                {
                    GaugeRow(rowY, width, servo.name, servo.position ?? 0, false,
                        servo.position.HasValue ? servo.position.Value.ToString("0.00", Dot) : "unset");
                    rowY += 22;
                }
                if (rows == 0)
                {
                    HudTheme.DrawText("NO MOTORS", Pad + 12, top + 15, HudTheme.Style("normal", 12), HudTheme.Muted);
                }
            }) + CardGap;

            // Driver Station screen as it looked at this moment.
            List<string> screen = HudText.DriverStationLines(recording, index);
            y = Card("Driver Station" + HudText.ScreenTime(recording, index), y, width, Mathf.Max(1, screen.Count) * 17 + 2, top =>
            {
                GUIStyle mono = HudTheme.Style("mono", 12);
                float lineY = top + 13;
                if (screen.Count == 0)
                {
                    HudTheme.DrawText("nothing sent yet", Pad + 12, lineY, mono, HudTheme.Muted);
                }
                foreach (string line in screen)
                {
                    HudTheme.DrawText(HudTheme.Ellipsize(line, mono, width - 24), Pad + 12, lineY, mono, HudTheme.Text);
                    lineY += 17;
                }
            }) + CardGap;

            // Events: rumbles and marked moments so far.
            List<string> events = HudText.EventLines(recording, index);
            y = Card("Events", y, width, events.Count * 17 + 2, top =>
            {
                GUIStyle text = HudTheme.Style("normal", 12);
                float lineY = top + 13;
                foreach (string line in events)
                {
                    HudTheme.DrawText(HudTheme.Ellipsize(line, text, width - 24), Pad + 12, lineY, text,
                        line.Contains("disconnected") ? HudTheme.Amber : HudTheme.Muted);
                    lineY += 17;
                }
            });

            cardsContentHeight = y + Pad;
            GUI.EndScrollView();
        }

        /// <summary>A card: dark box, hairline frame, cyan tick, spaced title. Returns the y just below it.</summary>
        private static float Card(string title, float y, float width, float contentHeight, Action<float> contents)
        {
            float height = 30 + contentHeight + 10;
            var box = new Rect(Pad, y, width, height);
            HudTheme.Fill(box, HudTheme.Panel);
            HudTheme.Hairline(box, HudTheme.Line);
            HudTheme.Fill(new Rect(Pad, y, 2, 14), HudTheme.Accent);
            HudTheme.Label(title, Pad + 12, y + 18, 10, HudTheme.Muted);
            HudTheme.Fill(new Rect(Pad + 12, y + 25, width - 24, 1), HudTheme.Line);
            contents(y + 30);
            return y + height;
        }

        /// <summary>A small label over a value.</summary>
        /// <summary>
        /// A Vehicle card value cut with "..." to fit its column (0 OpMode, 1 Period, 2 Robot) of a card
        /// <paramref name="cardWidth"/> wide, in the readout font.
        /// </summary>
        public static string VehicleText(string value, float cardWidth, int column)
        {
            float[] starts = { 0f, cardWidth * 2 / 5, cardWidth * 7 / 10, cardWidth - 24 };
            float room = starts[column + 1] - starts[column] - 12;
            return HudTheme.Ellipsize(value ?? "", HudTheme.Style("normal", 15), room);
        }

        /// <summary>How wide a Vehicle card value is in the readout font (for tests).</summary>
        public static float ReadoutWidth(string value)
        {
            return HudTheme.Width(value, HudTheme.Style("normal", 15));
        }

        private static void Readout(float x, float top, string label, string value)
        {
            HudTheme.Label(label, x, top + 10, 9, HudTheme.Faint);
            HudTheme.DrawText(value, x, top + 31, HudTheme.Style("normal", 15), HudTheme.Text);
        }

        /// <summary>A small label over a big number and its unit.</summary>
        private static void BigReadout(float x, float top, string label, string value, string unit)
        {
            HudTheme.Label(label, x, top + 10, 9, HudTheme.Faint);
            GUIStyle big = HudTheme.Style("number", 30);
            HudTheme.DrawText(value, x, top + 44, big, HudTheme.Text);
            HudTheme.Label(unit, x + HudTheme.Width(value, big) + 4, top + 44, 9, HudTheme.Muted);
        }

        /// <summary>
        /// One gauge row: name, a thin track with the value filled in, and the number. Centered
        /// gauges go from -1 to 1 with 0 in the middle (motors); others from 0 to 1 (servos).
        /// </summary>
        private static void GaugeRow(float top, float width, string name, double value, bool centered, string number)
        {
            HudTheme.Label(HudText.SpacedName(name), Pad + 12, top + 14, 10, HudTheme.Muted);
            float trackX = Pad + 12 + width * 2 / 5;
            float trackWidth = width - (trackX - Pad) - 70;
            float trackY = top + 9;
            HudTheme.Fill(new Rect(trackX, trackY, trackWidth, 4), HudTheme.Raised);
            float clamped = Mathf.Clamp((float)value, centered ? -1f : 0f, 1f);
            Color fill = Mathf.Approximately(clamped, 0f) ? HudTheme.Faint : HudTheme.Accent;
            if (centered)
            {
                float mid = trackX + trackWidth / 2f;
                float end = mid + clamped * trackWidth / 2f;
                HudTheme.Fill(new Rect(Mathf.Min(mid, end), trackY, Mathf.Max(1, Mathf.Abs(end - mid)), 4), fill);
                HudTheme.Fill(new Rect(mid - 0.5f, trackY - 3, 1, 10), HudTheme.Muted);
            }
            else
            {
                HudTheme.Fill(new Rect(trackX, trackY, Mathf.Max(1, clamped * trackWidth), 4), fill);
            }
            GUIStyle mono = HudTheme.Style("mono", 12);
            HudTheme.DrawText(number, Pad + width - 12 - HudTheme.Width(number, mono), top + 14, mono,
                Mathf.Approximately(clamped, 0f) ? HudTheme.Muted : HudTheme.Text);
        }

        // ---- Gamepads ----------------------------------------------------------------------------

        // Controller colors, from the Swing GamepadIndicator.
        private static readonly Color Body = new Color(0x0E / 255f, 0x12 / 255f, 0x16 / 255f);
        private static readonly Color BodyEdge = new Color(0x3A / 255f, 0x44 / 255f, 0x4F / 255f);
        private static readonly Color Unpressed = new Color(0x1F / 255f, 0x26 / 255f, 0x2E / 255f);
        private static readonly Color StickBase = new Color(0x06 / 255f, 0x08 / 255f, 0x0B / 255f);
        private const float ControllerWidth = 200f;
        private const float StickTravel = 7f;

        private static void DrawGamepads(Rect area, LoopData loop)
        {
            HudTheme.Fill(area, HudTheme.Panel);
            HudTheme.Fill(new Rect(area.x, area.y, area.width, 1), HudTheme.Line);
            float left = area.xMax - 8 - 2 * ControllerWidth;
            float top = area.yMax - 8 - 130;
            DrawController(new Vector2(left, top), loop.gamepad1, "Driver");
            DrawController(new Vector2(left + ControllerWidth, top), loop.gamepad2, "Operator");
        }

        /// <summary>One controller with held buttons lit, in the Swing GamepadIndicator's design coordinates.</summary>
        private static void DrawController(Vector2 o, GamepadData pad, string label)
        {
            HudTheme.Label(label, o.x + 8, o.y + 13, 10, HudTheme.Muted);

            // Triggers fill as pressed; bumpers light when held.
            Trigger(new Rect(o.x + 30, o.y + 18, 36, 10), pad.leftTrigger);
            Trigger(new Rect(o.x + 134, o.y + 18, 36, 10), pad.rightTrigger);
            HudTheme.Round(new Rect(o.x + 26, o.y + 31, 44, 10), 4, Held(pad, "LEFT_BUMPER") ? HudTheme.Text : Unpressed);
            HudTheme.Round(new Rect(o.x + 130, o.y + 31, 44, 10), 4, Held(pad, "RIGHT_BUMPER") ? HudTheme.Text : Unpressed);

            // Body and grips: the edge color one pixel larger underneath gives the outline.
            BodyShape(o, 1, BodyEdge);
            BodyShape(o, 0, Body);

            Stick(o + new Vector2(55, 66), pad.leftStickX, pad.leftStickY, Held(pad, "LEFT_STICK_BUTTON"));
            Stick(o + new Vector2(125, 90), pad.rightStickX, pad.rightStickY, Held(pad, "RIGHT_STICK_BUTTON"));

            DpadKey(o + new Vector2(75, 81), Held(pad, "DPAD_UP"));
            DpadKey(o + new Vector2(75, 99), Held(pad, "DPAD_DOWN"));
            DpadKey(o + new Vector2(66, 90), Held(pad, "DPAD_LEFT"));
            DpadKey(o + new Vector2(84, 90), Held(pad, "DPAD_RIGHT"));

            Face(o + new Vector2(145, 54), HudTheme.Amber, Held(pad, "Y"));
            Face(o + new Vector2(145, 78), HudTheme.Green, Held(pad, "A"));
            Face(o + new Vector2(133, 66), HudTheme.Accent, Held(pad, "X"));
            Face(o + new Vector2(157, 66), HudTheme.Red, Held(pad, "B"));

            HudTheme.Round(new Rect(o.x + 82, o.y + 55, 10, 6), 3, Held(pad, "BACK") ? HudTheme.Text : Unpressed);
            HudTheme.Round(new Rect(o.x + 108, o.y + 55, 10, 6), 3, Held(pad, "START") ? HudTheme.Text : Unpressed);
            HudTheme.Circle(o + new Vector2(100, 54), 5, Held(pad, "GUIDE") ? HudTheme.Text : Unpressed);
            HudTheme.Round(new Rect(o.x + 91, o.y + 66, 18, 8), 3, Held(pad, "TOUCHPAD") ? HudTheme.Text : Unpressed);
        }

        private static void BodyShape(Vector2 o, float grow, Color color)
        {
            HudTheme.Round(new Rect(o.x + 14 - grow, o.y + 42 - grow, 172 + 2 * grow, 60 + 2 * grow), 20 + grow, color);
            HudTheme.Round(new Rect(o.x + 16 - grow, o.y + 66 - grow, 58 + 2 * grow, 56 + 2 * grow), 28 + grow, color);
            HudTheme.Round(new Rect(o.x + 126 - grow, o.y + 66 - grow, 58 + 2 * grow, 56 + 2 * grow), 28 + grow, color);
        }

        private static void Stick(Vector2 basePoint, double x, double y, bool clicked)
        {
            HudTheme.Circle(basePoint, 13, StickBase);
            bool moved = Math.Abs(x) > 0.005 || Math.Abs(y) > 0.005;
            Vector2 knob = basePoint + new Vector2((float)x * StickTravel, (float)y * StickTravel);
            HudTheme.Circle(knob, 8, moved || clicked ? HudTheme.Text : Unpressed);
            if (clicked)
            {
                HudTheme.RoundOutline(new Rect(knob.x - 8, knob.y - 8, 16, 16), 8, 2, HudTheme.Accent);
            }
        }

        private static void Trigger(Rect outline, double pressed)
        {
            HudTheme.Fill(outline, Unpressed);
            if (pressed > 0.005)
            {
                HudTheme.Fill(new Rect(outline.x, outline.y, outline.width * Mathf.Min(1f, (float)pressed), outline.height), HudTheme.Text);
            }
        }

        private static void DpadKey(Vector2 center, bool held)
        {
            HudTheme.Fill(new Rect(center.x - 4.5f, center.y - 4.5f, 9, 9), held ? HudTheme.Text : Unpressed);
        }

        private static void Face(Vector2 center, Color color, bool held)
        {
            HudTheme.Circle(center, 6, held ? color : new Color(color.r / 3, color.g / 3, color.b / 3));
        }

        private static bool Held(GamepadData pad, string button)
        {
            return pad.buttons.Contains(button);
        }

        // ---- Controls (the bottom bar) -----------------------------------------------------------

        private void DrawControls(Rect r, RunRecordingData recording, ReplayTimeline timeline, ReplayController replay)
        {
            HudTheme.Fill(r, HudTheme.Panel);
            HudTheme.Fill(new Rect(r.x, r.y, r.width, 1), HudTheme.Line);
            float y = r.y + 14;
            float x = 28;
            LiveSession live = replay.Live;
            if (live != null && (live.Initialized || live.Playing))
            {
                // Live: the harness is running; only PLAY and STOP make sense (like the Swing live drive).
                if (live.Initialized && FlatButton(new Rect(x, y, 120, 28), "Play (Enter)")) replay.LivePlay();
                x += 128;
                if (FlatButton(new Rect(x, y, 120, 28), "Stop (Esc)")) replay.LiveStop();
                x += 144;
                GUIStyle liveMono = HudTheme.Style("mono", 13);
                string liveTime = "T+ " + HudText.Clock(timeline.Current.matchTimeSeconds) + "  /  "
                    + HudText.Clock(recording.header.period == "AUTONOMOUS" ? 30 : 120);
                HudTheme.DrawText(liveTime, x, y + 19, liveMono, HudTheme.Text);
                return;
            }
            if (live != null && FlatButton(new Rect(r.xMax - 28 - 92, y, 92, 28), replay.LiveSaved ? "Saved" : "Save"))
            {
                if (!replay.LiveSaved) replay.SaveLive();
            }
            if (FlatButton(new Rect(x, y, 34, 28), "|<")) timeline.Seek(0);
            x += 42;
            if (FlatButton(new Rect(x, y, 34, 28), "<")) timeline.Step(-1);
            x += 42;
            if (FlatButton(new Rect(x, y, 92, 28), timeline.IsPlaying ? "Pause" : "Play")) timeline.TogglePlay();
            x += 100;
            if (FlatButton(new Rect(x, y, 34, 28), ">")) timeline.Step(1);
            x += 42;
            if (FlatButton(new Rect(x, y, 34, 28), ">|")) timeline.Seek(timeline.Count - 1);
            x += 60;

            HudTheme.Label("Speed", x, y + 18, 10, HudTheme.Muted);
            x += 44;
            var speedBox = new Rect(x, y, 72, 28);
            HudTheme.Fill(speedBox, HudTheme.Panel);
            HudTheme.Hairline(speedBox, HudTheme.Line);
            HudTheme.DrawText(SpeedText(timeline.Speed), speedBox.x + 9, speedBox.y + 19, HudTheme.Style("normal", 13), HudTheme.Text);
            DrawDownArrow(new Vector2(speedBox.xMax - 14, speedBox.y + 13), HudTheme.Muted);
            speedBoxRect = speedBox;
            if (Clicked(speedBox))
            {
                speedMenuOpen = !speedMenuOpen;
                recordingMenuOpen = false;
            }
            x += 100;

            GUIStyle mono = HudTheme.Style("mono", 13);
            string time = "T+ " + HudText.Clock(timeline.Current.matchTimeSeconds) + "  /  "
                + HudText.Clock(recording.loops[recording.loops.Count - 1].matchTimeSeconds);
            HudTheme.DrawText(time, x, y + 19, mono, HudTheme.Text);
            x += HudTheme.Width(time, mono) + 24;

            DrawSlider(new Rect(x, y + 2, r.xMax - 28 - x - (live != null ? 108 : 0), 24), recording, timeline);
        }

        private Rect speedBoxRect;

        private void DrawSpeedMenu(ReplayTimeline timeline)
        {
            const float rowHeight = 24;
            var box = new Rect(speedBoxRect.x, speedBoxRect.y - rowHeight * ReplayTimeline.Speeds.Length, speedBoxRect.width,
                rowHeight * ReplayTimeline.Speeds.Length);
            HudTheme.Fill(box, HudTheme.Panel);
            HudTheme.Hairline(box, HudTheme.Line);
            for (int i = 0; i < ReplayTimeline.Speeds.Length; i++)
            {
                var row = new Rect(box.x, box.y + i * rowHeight, box.width, rowHeight);
                bool current = i == timeline.SpeedIndex;
                bool hover = row.Contains(Event.current.mousePosition);
                HudTheme.Fill(row, current ? HudTheme.Accent : hover ? HudTheme.Raised : HudTheme.Panel);
                HudTheme.DrawText(SpeedText(ReplayTimeline.Speeds[i]), row.x + 9, row.y + 17, HudTheme.Style("normal", 13),
                    current ? HudTheme.Background : HudTheme.Text);
                if (Clicked(row))
                {
                    timeline.SetSpeedIndex(i);
                    speedMenuOpen = false;
                }
            }
            CloseMenusOnOutsideClick(box);
        }

        /// <summary>The timeline: a hairline track, the played part in cyan, a small white playhead. Click or drag to seek.</summary>
        private void DrawSlider(Rect r, RunRecordingData recording, ReplayTimeline timeline)
        {
            double start = recording.loops[0].sinceInitSeconds;
            double end = timeline.EndSeconds;
            float fraction = end > start ? (float)((timeline.PlayheadSeconds - start) / (end - start)) : 0f;
            float midY = r.y + r.height / 2f;
            HudTheme.Fill(new Rect(r.x, midY - 1, r.width, 2), HudTheme.Line);
            HudTheme.Fill(new Rect(r.x, midY - 1, Mathf.Max(0, fraction * r.width), 2), HudTheme.Accent);
            HudTheme.Fill(new Rect(r.x + fraction * r.width - 1, midY - 7, 3, 14), HudTheme.Text);

            sliderControlId = GUIUtility.GetControlID(FocusType.Passive, r);
            Event e = Event.current;
            bool seek = false;
            if (e.type == EventType.MouseDown && e.button == 0 && r.Contains(e.mousePosition))
            {
                GUIUtility.hotControl = sliderControlId;
                seek = true;
            }
            else if (e.type == EventType.MouseDrag && GUIUtility.hotControl == sliderControlId)
            {
                seek = true;
            }
            else if (e.type == EventType.MouseUp && GUIUtility.hotControl == sliderControlId)
            {
                GUIUtility.hotControl = 0;
                e.Use();
            }
            if (seek)
            {
                float picked = Mathf.Clamp01((e.mousePosition.x - r.x) / r.width);
                timeline.Pause();
                timeline.SeekSeconds(start + picked * (end - start));
                e.Use();
            }
        }

        private static string SpeedText(float speed)
        {
            return speed.ToString("0.##", Dot) + "x";
        }

        // ---- Small widgets -----------------------------------------------------------------------

        /// <summary>A flat button: dark, hairline border, uppercase label, lighter under the mouse.</summary>
        private static bool FlatButton(Rect r, string text)
        {
            bool hover = r.Contains(Event.current.mousePosition);
            HudTheme.Fill(r, hover ? HudTheme.Raised : HudTheme.Panel);
            HudTheme.Hairline(r, HudTheme.Line);
            float w = HudTheme.LabelWidth(text, 11);
            HudTheme.Label(text, r.x + (r.width - w) / 2f, r.y + r.height / 2f + 5, 11, HudTheme.Text);
            return Clicked(r);
        }

        private static void DrawDownArrow(Vector2 center, Color color)
        {
            // A small downward triangle made of three shrinking bars.
            HudTheme.Fill(new Rect(center.x - 4, center.y - 1, 9, 1.5f), color);
            HudTheme.Fill(new Rect(center.x - 2.5f, center.y + 1, 6, 1.5f), color);
            HudTheme.Fill(new Rect(center.x - 1, center.y + 3, 3, 1.5f), color);
        }

        /// <summary>True on a left click inside <paramref name="r"/> (and uses up the click).</summary>
        private static bool Clicked(Rect r)
        {
            Event e = Event.current;
            if (e.type == EventType.MouseDown && e.button == 0 && r.Contains(e.mousePosition))
            {
                e.Use();
                return true;
            }
            return false;
        }

        private void CloseMenusOnOutsideClick(Rect menu)
        {
            Event e = Event.current;
            if (e.type == EventType.MouseDown && !menu.Contains(e.mousePosition))
            {
                recordingMenuOpen = false;
                speedMenuOpen = false;
            }
        }

        private static GUIStyle WrapStyle(GUIStyle style, Color color)
        {
            var wrap = new GUIStyle(style) { wordWrap = true };
            wrap.normal.textColor = color;
            return wrap;
        }

        /// <summary>True while a dropdown list is open (the controller then ignores clicks on the field).</summary>
        public bool MenuOpen => recordingMenuOpen || speedMenuOpen;
    }
}

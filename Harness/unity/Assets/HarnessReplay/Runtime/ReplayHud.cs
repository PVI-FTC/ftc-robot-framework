using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using UnityEngine;

namespace PviHarness.Replay
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
            DrawHeader(layout.header, recording, timeline, replay.ShownFileName);
            DrawChooser(layout.chooser, replay);
            if (recording != null)
            {
                DrawFieldTexts(layout, recording, timeline);
                DrawCards(layout.cards, recording, timeline.Index);
                DrawGamepads(layout.gamepads, recording.loops[timeline.Index]);
                DrawControls(layout.controls, recording, timeline);
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
        }

        // ---- Header --------------------------------------------------------------------------------

        private void DrawHeader(Rect r, RunRecordingData recording, ReplayTimeline timeline, string fileName)
        {
            HudTheme.Fill(r, HudTheme.Panel);
            HudTheme.Fill(new Rect(r.x, r.yMax - 1, r.width, 1), HudTheme.Line);

            // Left: the mark and what is shown.
            HudTheme.Fill(new Rect(20, 14, 3, 30), HudTheme.Text);
            HudTheme.Label("FTC Harness", 34, 27, 14, HudTheme.Text);
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

                string stage = HudText.StageLine(recording, timeline.Index);
                float stageWidth = HudTheme.LabelWidth(stage, 9);
                float stageX = centerX - stageWidth / 2f + 7;
                HudTheme.Circle(new Vector2(stageX - 10, 41), 3, StageColor(timeline.Current.stage));
                HudTheme.Label(stage, stageX, 45, 9, HudTheme.Faint);
            }

            // Right: where the Swing viewer has its Record button, this says it is a replay.
            var badge = new Rect(r.xMax - 16 - 120, 15, 120, 28);
            HudTheme.Hairline(badge, HudTheme.Line);
            HudTheme.RoundOutline(new Rect(badge.x + 12, badge.y + 9, 10, 10), 5, 1.2f, HudTheme.Muted);
            HudTheme.Label("3D replay", badge.x + 30, badge.y + 19, 11, HudTheme.Muted);
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
            HudTheme.Label("Replay", 28, r.y + 30, 10, HudTheme.Muted);

            // The dropdown box: shows what is replayed; click to pick another recording.
            var box = new Rect(112, r.y + 10, r.width - 112 - 16 - 104, 28);
            HudTheme.Fill(box, HudTheme.Panel);
            HudTheme.Hairline(box, HudTheme.Line);
            string shown = replay.ShownFileName == null ? "No recording open" : "Replaying " + replay.ShownFileName;
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
                }
            }

            // "REPLAYS..." opens a file picker (in the editor), like the Swing viewer's button.
            var button = new Rect(r.xMax - 16 - 92, r.y + 10, 92, 28);
            if (FlatButton(button, "Replays..."))
            {
                recordingMenuOpen = false;
                replay.OpenFilePicker();
            }

            string showing = replay.ShownFileName == null ? "" : "Showing " + replay.ShownFileName;
            HudTheme.DrawText(showing, 28, r.y + 56, HudTheme.Style("normal", 12), HudTheme.Muted);
        }

        private void DrawRecordingMenu(Rect chooser, ReplayController replay)
        {
            List<string> files = replay.RecordingsNewestFirst;
            const float rowHeight = 24;
            var box = new Rect(112, chooser.y + 38, chooser.width - 112 - 16 - 104, 0);
            float listHeight = Mathf.Min(files.Count, 16) * rowHeight;
            box.height = Mathf.Max(rowHeight, listHeight);
            HudTheme.Fill(box, HudTheme.Panel);
            HudTheme.Hairline(box, HudTheme.Line);
            if (files.Count == 0)
            {
                HudTheme.DrawText("No recordings in " + RecordingLoader.DefaultFolder, box.x + 9, box.y + 17,
                    HudTheme.Style("normal", 13), HudTheme.Muted);
                CloseMenusOnOutsideClick(box);
                return;
            }
            recordingMenuScroll = GUI.BeginScrollView(box, recordingMenuScroll,
                new Rect(0, 0, box.width - 16, files.Count * rowHeight), false, files.Count > 16, GUIStyle.none, GUI.skin.verticalScrollbar);
            for (int i = 0; i < files.Count; i++)
            {
                var row = new Rect(0, i * rowHeight, box.width - (files.Count > 16 ? 16 : 0), rowHeight);
                bool current = replay.ShownFileName == Path.GetFileName(files[i]);
                bool hover = row.Contains(Event.current.mousePosition);
                HudTheme.Fill(row, current ? HudTheme.Accent : hover ? HudTheme.Raised : HudTheme.Panel);
                HudTheme.DrawText("Replay: " + Path.GetFileName(files[i]), row.x + 8, row.y + 17, HudTheme.Style("normal", 13),
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
            HudTheme.Label(HudTheme.Ellipsize(caption.ToUpperInvariant(), HudTheme.Style("normal", 12), f.width * 0.85f),
                f.x + 2, f.y - 10, 12, HudTheme.Text);
            DriveModelData model = recording.header.driveModel;
            HudTheme.Label(HudTheme.Ellipsize(HudText.Legend(model).ToUpperInvariant(), HudTheme.Style("normal", 12), f.width),
                f.x + 2, f.yMax + 20, 12, HudText.SizeIsPlaceholder(model) ? HudTheme.Amber : HudTheme.Muted);
        }

        // ---- Cards (the right-hand column) ---------------------------------------------------------

        private void DrawCards(Rect area, RunRecordingData recording, int index)
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
                Readout(Pad + 12, top, "OpMode", header.opMode);
                Readout(Pad + 12 + width * 2 / 5, top, "Period", header.period ?? "-");
                Readout(Pad + 12 + width * 7 / 10, top, "Robot", header.robotConfig);
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

        private void DrawControls(Rect r, RunRecordingData recording, ReplayTimeline timeline)
        {
            HudTheme.Fill(r, HudTheme.Panel);
            HudTheme.Fill(new Rect(r.x, r.y, r.width, 1), HudTheme.Line);
            float y = r.y + 14;
            float x = 28;
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

            DrawSlider(new Rect(x, y + 2, r.xMax - 28 - x, 24), recording, timeline);
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

using System.Collections.Generic;
using System.Globalization;
using System.Text.RegularExpressions;

namespace PviHarness.Replay
{
    /// <summary>
    /// The words and numbers the on-screen panels show, worked out without drawing anything, so
    /// tests can check them. Each matches the Swing viewer (MissionHeader, TelemetryPanel,
    /// FieldPainter), so both viewers say exactly the same thing.
    /// </summary>
    public static class HudText
    {
        private static readonly CultureInfo Dot = CultureInfo.InvariantCulture;

        /// <summary>83.456 s -> "01:23.45", the mission clock (shown as "T+ 01:23.45").</summary>
        public static string Clock(double seconds)
        {
            double s = System.Math.Max(0, seconds);
            int minutes = (int)(s / 60);
            double rest = s - minutes * 60;
            return minutes.ToString("00", Dot) + ":" + rest.ToString("00.00", Dot);
        }

        /// <summary>"TeamATeleOp  ·  teamA  ·  file.json": what is shown, under the FTC HARNESS mark.</summary>
        public static string Subtitle(HeaderData header, string fileName)
        {
            return header.opMode + "  ·  " + header.robotConfig + (string.IsNullOrEmpty(fileName) ? "" : "  ·  " + fileName);
        }

        /// <summary>"Replay  ·  TELEOP  ·  LOOP  ·  loop 12 / 401", under the mission clock ("Live" while driving live).</summary>
        public static string StageLine(RunRecordingData recording, int index, string mode = "Replay")
        {
            LoopData loop = recording.loops[index];
            return mode + "  ·  " + (recording.header.period == null ? "" : recording.header.period + "  ·  ")
                + loop.stage + string.Format(Dot, "  ·  loop {0} / {1}", index + 1, recording.loops.Count);
        }

        /// <summary>The field caption: "TeamATeleOp  LOOP  t=1.24 s   x=0.38 ft, y=0.00 ft, heading=0.0 deg".</summary>
        public static string FieldCaption(RunRecordingData recording, int index, PoseData pose)
        {
            LoopData loop = recording.loops[index];
            string caption = string.Format(Dot, "{0}  {1}  t={2:0.00} s", recording.header.opMode, loop.stage, loop.matchTimeSeconds);
            return pose == null ? caption : caption + "   " + ReplayPanelText.PoseText(pose);
        }

        /// <summary>The legend under the field: robot size, whether it is a guess, and what the colors mean.</summary>
        public static string Legend(DriveModelData model)
        {
            if (model == null || model.length == null || model.width == null)
            {
                return "Robot 18 x 18 in (unknown robot), PLACEHOLDER size, not measured. Cyan = path, white = facing.";
            }
            bool measured = model.length.measured && model.width.measured;
            return string.Format(Dot, "Robot {0:0} x {1:0} in ({2}), {3}. Cyan = path, white = facing.",
                model.length.value, model.width.value, model.name, measured ? "measured size" : "PLACEHOLDER size, not measured");
        }

        /// <summary>True when the robot size in the legend is a guess (the legend is then amber).</summary>
        public static bool SizeIsPlaceholder(DriveModelData model)
        {
            return model == null || model.length == null || model.width == null || !(model.length.measured && model.width.measured);
        }

        /// <summary>
        /// The live status line: what to do next, and which controller drives each gamepad, e.g.
        /// "INIT: press Enter for PLAY  ·  Gamepad 1: PS5 Controller  ·  Gamepad 2: none".
        /// </summary>
        public static string LiveStatus(LiveSession live, bool saved)
        {
            string step = live.Initialized ? "INIT: press Enter for PLAY (Esc stops)"
                : live.Playing ? "Driving: Esc for STOP"
                : saved ? "Stopped and saved" : "Stopped: Save keeps this run";
            for (int i = 0; i < live.Controllers.Count; i++)
            {
                step += "  ·  Gamepad " + (i + 1) + ": " + live.Controllers[i];
            }
            return step;
        }

        /// <summary>"frontLeft" -> "FRONT LEFT", the way motor names are labeled.</summary>
        public static string SpacedName(string name)
        {
            return Regex.Replace(name, "([a-z0-9])([A-Z])", "$1 $2").ToUpperInvariant();
        }

        /// <summary>A motor power the way the gauges show it: "+0.20", "-1.00", "+0.00".</summary>
        public static string Power(double power)
        {
            return (power >= 0 ? "+" : "-") + System.Math.Abs(power).ToString("0.00", Dot);
        }

        /// <summary>The Driver Station screen as it looked at this loop (the last one sent at or before it), log lines last.</summary>
        public static List<string> DriverStationLines(RunRecordingData recording, int index)
        {
            for (int i = index; i >= 0; i--)
            {
                List<TelemetryScreenData> screens = recording.loops[i].telemetry;
                if (screens.Count > 0)
                {
                    TelemetryScreenData screen = screens[screens.Count - 1];
                    var lines = new List<string>(screen.lines);
                    foreach (string log in screen.log)
                    {
                        lines.Add("log: " + log);
                    }
                    return lines;
                }
            }
            return new List<string>();
        }

        /// <summary>"  ·  sent 1.24 s" for the Driver Station card's title, or "" if nothing was sent yet.</summary>
        public static string ScreenTime(RunRecordingData recording, int index)
        {
            for (int i = index; i >= 0; i--)
            {
                if (recording.loops[i].telemetry.Count > 0)
                {
                    return string.Format(Dot, "  ·  sent {0:0.00} s", recording.loops[i].matchTimeSeconds);
                }
            }
            return "";
        }

        /// <summary>Rumble counts so far, then every marked event so far ("T+  1.24  text").</summary>
        public static List<string> EventLines(RunRecordingData recording, int index)
        {
            LoopData loop = recording.loops[index];
            var lines = new List<string>
            {
                "Gamepad 1 rumbles: " + RumblesSoFar(recording.gamepad1Rumbles, loop)
                    + "    Gamepad 2 rumbles: " + RumblesSoFar(recording.gamepad2Rumbles, loop),
            };
            foreach (EventData e in recording.events)
            {
                if (e.sinceInitSeconds <= loop.sinceInitSeconds + 1e-9)
                {
                    lines.Add(string.Format(Dot, "T+{0,6:0.00}  {1}", e.matchTimeSeconds, e.text));
                }
            }
            return lines;
        }

        private static int RumblesSoFar(List<RumbleData> rumbles, LoopData loop)
        {
            int count = 0;
            foreach (RumbleData rumble in rumbles)
            {
                if (rumble.matchTimeSeconds <= loop.matchTimeSeconds + 1e-9)
                {
                    count++;
                }
            }
            return count;
        }
    }
}

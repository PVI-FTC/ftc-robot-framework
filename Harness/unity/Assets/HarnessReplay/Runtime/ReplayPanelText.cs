using System.Collections.Generic;
using System.Globalization;
using System.Text;

namespace PviHarness.Replay
{
    /// <summary>
    /// The text of the replay's side panel for one loop: what ran, the time and stage, where the
    /// robot was, motors, servos, gamepads, the Driver Station screen as it was at that moment,
    /// and rumbles and events so far. Plain text, so tests can check it without a screen; it
    /// shows the same things as the Swing viewer's side panel.
    /// </summary>
    public static class ReplayPanelText
    {
        private static readonly CultureInfo Dot = CultureInfo.InvariantCulture;

        /// <summary>Every line of the panel for loop <paramref name="index"/>, top to bottom.</summary>
        public static List<string> Describe(RunRecordingData recording, int index)
        {
            HeaderData header = recording.header;
            LoopData loop = recording.loops[index];
            var lines = new List<string>
            {
                header.opMode + "   " + (header.period ?? "period unknown") + "   robot: " + header.robotConfig,
                string.Format(Dot, "Stage: {0,-10} Loop {1} of {2}", loop.stage, index + 1, recording.loops.Count),
                string.Format(Dot, "Match time: {0:0.00} s   (since INIT {1:0.00} s)", loop.matchTimeSeconds, loop.sinceInitSeconds),
                "Robot: " + (loop.pose == null ? "position not tracked" : PoseText(loop.pose)),
                "Auto step: " + (loop.autoStep ?? "-"),
                "",
                "Motors (power)",
            };
            foreach (MotorData motor in loop.motors)
            {
                lines.Add(string.Format(Dot, "  {0,-12} {1,6:0.00}", motor.name, motor.power));
            }
            if (loop.servos.Count > 0)
            {
                lines.Add("Servos (position)");
                foreach (ServoData servo in loop.servos)
                {
                    lines.Add(servo.position.HasValue
                        ? string.Format(Dot, "  {0,-12} {1,6:0.00}", servo.name, servo.position.Value)
                        : string.Format(Dot, "  {0,-12} {1,6}", servo.name, "not set"));
                }
            }

            lines.Add("");
            lines.Add("Driver   " + GamepadText(loop.gamepad1));
            lines.Add("Operator " + GamepadText(loop.gamepad2));

            lines.Add("");
            AddDriverStation(lines, recording, index);

            lines.Add("");
            AddRumbles(lines, "Gamepad 1", recording.gamepad1Rumbles, loop);
            AddRumbles(lines, "Gamepad 2", recording.gamepad2Rumbles, loop);
            AddEvents(lines, recording.events, loop);
            return lines;
        }

        /// <summary>A pose the way the harness prints it: feet, two decimals, heading in degrees.</summary>
        public static string PoseText(PoseData pose)
        {
            // Display only (the harness reports in feet). Unity positions use FtcToUnity.
            return string.Format(Dot, "x={0:0.00} ft, y={1:0.00} ft, heading={2:0.0} deg",
                pose.xInches / 12.0, pose.yInches / 12.0, pose.headingRadians * 180.0 / System.Math.PI);
        }

        /// <summary>Held buttons and moved sticks/triggers, or "-" when the gamepad is at rest.</summary>
        public static string GamepadText(GamepadData pad)
        {
            var text = new StringBuilder();
            foreach (string button in pad.buttons)
            {
                text.Append(button).Append(' ');
            }
            AddStick(text, "L", pad.leftStickX, pad.leftStickY);
            AddStick(text, "R", pad.rightStickX, pad.rightStickY);
            AddTrigger(text, "LT", pad.leftTrigger);
            AddTrigger(text, "RT", pad.rightTrigger);
            return text.Length == 0 ? "-" : text.ToString().TrimEnd();
        }

        private static void AddStick(StringBuilder text, string name, double x, double y)
        {
            if (System.Math.Abs(x) > 0.005 || System.Math.Abs(y) > 0.005)
            {
                text.Append(string.Format(Dot, "{0}({1:0.00},{2:0.00}) ", name, x, y));
            }
        }

        private static void AddTrigger(StringBuilder text, string name, double value)
        {
            if (value > 0.005)
            {
                text.Append(string.Format(Dot, "{0} {1:0.00} ", name, value));
            }
        }

        /// <summary>The last Driver Station screen sent at or before this loop (it stays up until a new one arrives).</summary>
        private static void AddDriverStation(List<string> lines, RunRecordingData recording, int index)
        {
            for (int i = index; i >= 0; i--)
            {
                List<TelemetryScreenData> screens = recording.loops[i].telemetry;
                if (screens.Count > 0)
                {
                    TelemetryScreenData screen = screens[screens.Count - 1];
                    lines.Add(string.Format(Dot, "Driver Station (sent at {0:0.00} s)", recording.loops[i].matchTimeSeconds));
                    foreach (string line in screen.lines)
                    {
                        lines.Add("  " + line);
                    }
                    foreach (string line in screen.log)
                    {
                        lines.Add("  log: " + line);
                    }
                    return;
                }
            }
            lines.Add("Driver Station: nothing sent yet");
        }

        private static void AddRumbles(List<string> lines, string gamepad, List<RumbleData> rumbles, LoopData loop)
        {
            RumbleData latest = null;
            int count = 0;
            foreach (RumbleData rumble in rumbles)
            {
                if (rumble.matchTimeSeconds <= loop.matchTimeSeconds + 1e-9)
                {
                    latest = rumble;
                    count++;
                }
            }
            lines.Add(latest == null
                ? gamepad + " rumbles: none yet"
                : string.Format(Dot, "{0} rumbles: {1} (latest {2} ms, {3} pulse{4} at {5:0.00} s)", gamepad, count,
                    latest.totalDurationMs, latest.pulses, latest.pulses == 1 ? "" : "s", latest.matchTimeSeconds));
        }

        private static void AddEvents(List<string> lines, List<EventData> events, LoopData loop)
        {
            bool any = false;
            foreach (EventData e in events)
            {
                if (e.sinceInitSeconds <= loop.sinceInitSeconds + 1e-9)
                {
                    if (!any)
                    {
                        lines.Add("Events so far");
                        any = true;
                    }
                    lines.Add(string.Format(Dot, "  {0:0.00} s: {1}", e.matchTimeSeconds, e.text));
                }
            }
        }
    }
}

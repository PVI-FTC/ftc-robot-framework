using System;
using System.Collections.Generic;
using System.Globalization;
using System.IO;
using UnityEngine;

namespace PviHarness.Replay
{
    /// <summary>
    /// One robot's look, read from <c>Harness/robots/&lt;id&gt;/robot.properties</c>: its name, where its 3D
    /// model came from, which Unity model to draw (in Resources), the model's units and which way it
    /// faces, and which parts of the model are the four drive wheels (so they can spin).
    ///
    /// <para>This is only how the robot looks. How big it is and how it drives still come from
    /// harness.properties (the robot box Unity's physics and the harness use).</para>
    ///
    /// <para>The file is plain <c>key=value</c> lines, like harness.properties; <c>#</c> starts a comment.</para>
    /// </summary>
    public sealed class RobotProfile
    {
        /// <summary>The choice that draws the plain robot box instead of a model.</summary>
        public const string BoxId = "box";

        /// <summary>The folder name, e.g. "gobilda-starter-bot".</summary>
        public string Id { get; private set; }
        /// <summary>Shown to people, e.g. "goBILDA FTC Starter Bot (2026-2027)".</summary>
        public string Name { get; private set; }
        /// <summary>Where the model came from (for the credits).</summary>
        public string Source { get; private set; }
        /// <summary>The model's path under Unity's Resources, without the extension, e.g. "Robots/gobilda-starter-bot/gobilda-starter-bot".</summary>
        public string Model { get; private set; }
        /// <summary>How many meters one model unit is (0.001 for a model in millimeters).</summary>
        public float MetersPerUnit { get; private set; } = 1f;
        /// <summary>The model's axis that points to the robot's front, e.g. "+x" or "-z".</summary>
        public string Forward { get; private set; } = "+z";
        /// <summary>The model's axis that points up, e.g. "+y" or "+z".</summary>
        public string Up { get; private set; } = "+y";
        /// <summary>
        /// The robot's center in the model's own coordinates (model units), usually the middle of the
        /// four drive wheels. The model is moved so this point is at the robot's position.
        /// </summary>
        public Vector3 Center { get; private set; }
        /// <summary>
        /// Names of the model's parts that are the drive wheels, front-left, front-right, rear-left,
        /// rear-right (null if not split out). Each may list several parts with commas, for wheels
        /// that turn with the same motor.
        /// </summary>
        public string[] Wheels { get; private set; } = new string[4];

        /// <summary>Reads a robot.properties text. Throws <see cref="FormatException"/> saying what is wrong.</summary>
        public static RobotProfile Parse(string text, string id)
        {
            var values = new Dictionary<string, string>(StringComparer.Ordinal);
            foreach (string raw in text.Split('\n'))
            {
                string line = raw.Trim();
                if (line.Length == 0 || line.StartsWith("#", StringComparison.Ordinal))
                {
                    continue;
                }
                int equals = line.IndexOf('=');
                if (equals <= 0)
                {
                    throw new FormatException(id + "/robot.properties: \"" + line + "\" isn't key=value.");
                }
                values[line.Substring(0, equals).Trim()] = line.Substring(equals + 1).Trim();
            }
            var p = new RobotProfile { Id = id };
            p.Name = Get(values, "name", id);
            p.Source = Get(values, "source", "");
            p.Model = Get(values, "model", null)
                ?? throw new FormatException(id + "/robot.properties needs model= (the model's path under Resources).");
            p.MetersPerUnit = Number(values, "model.metersPerUnit", 1f, id);
            p.Forward = Axis(values, "model.forward", "+z", id);
            p.Up = Axis(values, "model.up", "+y", id);
            if (p.Forward.Substring(1) == p.Up.Substring(1))
            {
                throw new FormatException(id + "/robot.properties: model.forward and model.up must be different axes.");
            }
            p.Center = new Vector3(Number(values, "model.centerX", 0f, id), Number(values, "model.centerY", 0f, id),
                Number(values, "model.centerZ", 0f, id));
            p.Wheels = new[]
            {
                Get(values, "wheel.frontLeft", null), Get(values, "wheel.frontRight", null),
                Get(values, "wheel.rearLeft", null), Get(values, "wheel.rearRight", null),
            };
            return p;
        }

        /// <summary>Reads Harness/robots/&lt;id&gt;/robot.properties from <paramref name="robotsFolder"/>, or null if there's none.</summary>
        public static RobotProfile Load(string robotsFolder, string id, List<string> problems)
        {
            string path = Path.Combine(robotsFolder, id, "robot.properties");
            if (!File.Exists(path))
            {
                return null;
            }
            try
            {
                return Parse(File.ReadAllText(path), id);
            }
            catch (Exception e) when (e is FormatException || e is IOException)
            {
                problems?.Add(e.Message);
                return null;
            }
        }

        /// <summary>Every robot in <paramref name="robotsFolder"/> (one folder each with a robot.properties), by id.</summary>
        public static List<RobotProfile> LoadAll(string robotsFolder, List<string> problems)
        {
            var robots = new List<RobotProfile>();
            if (!Directory.Exists(robotsFolder))
            {
                return robots;
            }
            var ids = new List<string>();
            foreach (string folder in Directory.GetDirectories(robotsFolder))
            {
                ids.Add(Path.GetFileName(folder));
            }
            ids.Sort(StringComparer.Ordinal);
            foreach (string id in ids)
            {
                RobotProfile robot = Load(robotsFolder, id, problems);
                if (robot != null)
                {
                    robots.Add(robot);
                }
            }
            return robots;
        }

        /// <summary>"-robot &lt;id&gt;" (or "-robot box") from the command line, or null.</summary>
        public static string RobotFromCommandLine(string[] args)
        {
            for (int i = 0; i < args.Length - 1; i++)
            {
                if (string.Equals(args[i], "-robot", StringComparison.OrdinalIgnoreCase))
                {
                    return args[i + 1];
                }
            }
            return null;
        }

        /// <summary>Harness/robots, found from Harness/unity/Assets (the Editor) or Harness/unity/Build/HarnessReplay_Data (the built app).</summary>
        public static string ResolveFolder(string dataPath, bool isEditor)
        {
            return isEditor
                ? Path.GetFullPath(Path.Combine(dataPath, "..", "..", "robots"))
                : Path.GetFullPath(Path.Combine(dataPath, "..", "..", "..", "robots"));
        }

        /// <summary>
        /// The turn that takes the model's own axes to the robot's: its <see cref="Forward"/> axis to
        /// Unity +z (the robot's front) and its <see cref="Up"/> axis to Unity +y.
        /// </summary>
        public Quaternion ModelRotation()
        {
            return Quaternion.Inverse(Quaternion.LookRotation(AxisVector(Forward), AxisVector(Up)));
        }

        /// <summary>"+x" as (1, 0, 0), "-z" as (0, 0, -1), and so on.</summary>
        public static Vector3 AxisVector(string axis)
        {
            float sign = axis[0] == '-' ? -1f : 1f;
            switch (axis[1])
            {
                case 'x': return new Vector3(sign, 0, 0);
                case 'y': return new Vector3(0, sign, 0);
                default: return new Vector3(0, 0, sign);
            }
        }

        private static string Get(Dictionary<string, string> values, string key, string fallback)
        {
            return values.TryGetValue(key, out string value) && value.Length > 0 ? value : fallback;
        }

        private static float Number(Dictionary<string, string> values, string key, float fallback, string id)
        {
            string text = Get(values, key, null);
            if (text == null)
            {
                return fallback;
            }
            if (!float.TryParse(text, NumberStyles.Float, CultureInfo.InvariantCulture, out float number))
            {
                throw new FormatException(id + "/robot.properties: " + key + " must be a number, not \"" + text + "\".");
            }
            return number;
        }

        private static string Axis(Dictionary<string, string> values, string key, string fallback, string id)
        {
            string text = Get(values, key, fallback).ToLowerInvariant();
            if (text.Length != 2 || (text[0] != '+' && text[0] != '-') || "xyz".IndexOf(text[1]) < 0)
            {
                throw new FormatException(id + "/robot.properties: " + key + " must be +x, -x, +y, -y, +z or -z, not \"" + text + "\".");
            }
            return text;
        }
    }
}

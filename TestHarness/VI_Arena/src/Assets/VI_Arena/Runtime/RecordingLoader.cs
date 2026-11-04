using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using Newtonsoft.Json;
using UnityEngine;

namespace VI_Arena
{
    /// <summary>Thrown when a file is not a run recording this project can read, with a message saying why.</summary>
    public sealed class RecordingFormatException : Exception
    {
        public RecordingFormatException(string message, Exception cause = null) : base(message, cause)
        {
        }
    }

    /// <summary>
    /// Reads run recordings saved by the harness (TestHarness/th_src/build/recordings/*.json).
    /// Uses Newtonsoft JSON, which keeps null values (a pose that wasn't tracked) instead of
    /// turning them into zeros the way Unity's built-in JsonUtility would.
    /// </summary>
    public static class RecordingLoader
    {
        /// <summary>The format versions this project can read.</summary>
        public static readonly int[] SupportedVersions = { 1, 2, 3, 4, 5 };

        /// <summary>
        /// Where the recordings are: usually TestHarness/th_src/build/recordings (see
        /// <see cref="ResolveFolder"/> and <see cref="TestHarnessFolder"/>), or null when this app
        /// doesn't know where any are (an unzipped copy with no TestHarness folder found).
        /// </summary>
        public static string DefaultFolder => ResolveFolder(Environment.GetCommandLineArgs(), TestHarnessFolder.Current);

        /// <summary>
        /// The recordings folder, in order of preference:
        /// <list type="number">
        ///   <item><c>-recordings &lt;folder&gt;</c> on the command line (VI_Arena.bat passes it);</item>
        ///   <item>the folder of a <c>.json</c> recording given on the command line (a file dropped on VI_Arena.exe or VI_Arena.bat);</item>
        ///   <item>TestHarness/th_src/build/recordings, when the TestHarness folder is known;</item>
        ///   <item>otherwise null (no list; "Open recording..." still works).</item>
        /// </list>
        /// </summary>
        /// <param name="args">The command line (Environment.GetCommandLineArgs()).</param>
        /// <param name="testHarness">The TestHarness folder (<see cref="TestHarnessFolder.Find"/>), or null.</param>
        public static string ResolveFolder(string[] args, string testHarness)
        {
            for (int i = 0; i < args.Length - 1; i++)
            {
                if (string.Equals(args[i], "-recordings", StringComparison.OrdinalIgnoreCase))
                {
                    return Path.GetFullPath(args[i + 1]);
                }
            }
            string file = FileFromCommandLine(args);
            if (file != null)
            {
                return Path.GetDirectoryName(Path.GetFullPath(file));
            }
            return testHarness == null ? null : Path.Combine(TestHarnessFolder.JavaHarness(testHarness), "build", "recordings");
        }

        /// <summary>A <c>.json</c> recording named on the command line (e.g. dropped on VI_Arena.exe), or null.</summary>
        public static string FileFromCommandLine(string[] args)
        {
            for (int i = 1; i < args.Length; i++)
            {
                if (args[i].EndsWith(".json", StringComparison.OrdinalIgnoreCase)
                    && !string.Equals(args[i - 1], "-recordings", StringComparison.OrdinalIgnoreCase))
                {
                    return args[i];
                }
            }
            return null;
        }

        /// <summary>Reads and checks a recording file.</summary>
        public static RunRecordingData Load(string path)
        {
            if (!File.Exists(path))
            {
                throw new RecordingFormatException("No recording file at " + Path.GetFullPath(path));
            }
            return Parse(File.ReadAllText(path), Path.GetFileName(path));
        }

        /// <summary>
        /// Turns recording JSON into data, and checks the parts the replay depends on: the format
        /// version, the header, and at least one loop with times in order.
        /// </summary>
        /// <param name="json">The file's text.</param>
        /// <param name="name">A name for error messages, e.g. the file name.</param>
        public static RunRecordingData Parse(string json, string name = "recording")
        {
            RunRecordingData data;
            try
            {
                data = JsonConvert.DeserializeObject<RunRecordingData>(json);
            }
            catch (JsonException e)
            {
                throw new RecordingFormatException(name + " is not valid JSON: " + e.Message, e);
            }
            if (data == null || data.header == null)
            {
                throw new RecordingFormatException(name + " has no header, so it is not a harness run recording.");
            }
            if (!SupportedVersions.Contains(data.header.version))
            {
                throw new RecordingFormatException(name + " is format version " + data.header.version
                    + ", but this project reads versions " + string.Join(", ", SupportedVersions)
                    + ". Update the Unity replay code (and TestHarness/docs/README.md) for the new version.");
            }
            if (data.loops == null || data.loops.Count == 0)
            {
                throw new RecordingFormatException(name + " has no loops to replay.");
            }

            // Lists the file left out (or wrote as null) become empty, so replay code never
            // has to check for null lists. Version 1 files have no "events" at all.
            data.gamepad1Rumbles ??= new List<RumbleData>();
            data.gamepad2Rumbles ??= new List<RumbleData>();
            data.events ??= new List<EventData>();
            data.header.pieces ??= new List<PieceData>();
            data.header.cameras ??= new List<CameraData>();
            data.header.cameras.RemoveAll(c => c == null);
            data.header.cameras.ForEach(c => c.placeholders ??= new List<string>());
            for (int i = 0; i < data.loops.Count; i++)
            {
                LoopData loop = data.loops[i];
                if (loop == null)
                {
                    throw new RecordingFormatException(name + ": loops[" + i + "] is null.");
                }
                if (i > 0 && loop.sinceInitSeconds < data.loops[i - 1].sinceInitSeconds)
                {
                    throw new RecordingFormatException(name + ": loops[" + i + "].sinceInitSeconds goes back in time ("
                        + data.loops[i - 1].sinceInitSeconds + " s then " + loop.sinceInitSeconds + " s).");
                }
                loop.motors ??= new List<MotorData>();
                loop.servos ??= new List<ServoData>();
                loop.telemetry ??= new List<TelemetryScreenData>();
                loop.gamepad1 ??= new GamepadData();
                loop.gamepad2 ??= new GamepadData();
                loop.pieces ??= new List<PieceData>();
            }
            return data;
        }

        /// <summary>Every .json file in a folder, most recently changed first; empty if the folder doesn't exist.</summary>
        public static List<string> RecordingsNewestFirst(string folder)
        {
            if (!Directory.Exists(folder))
            {
                return new List<string>();
            }
            return Directory.GetFiles(folder, "*.json")
                .OrderByDescending(File.GetLastWriteTimeUtc)
                .ThenBy(f => f, StringComparer.Ordinal)
                .ToList();
        }
    }
}

using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using Newtonsoft.Json;
using UnityEngine;

namespace PviHarness.Replay
{
    /// <summary>Thrown when a file is not a run recording this project can read, with a message saying why.</summary>
    public sealed class RecordingFormatException : Exception
    {
        public RecordingFormatException(string message, Exception cause = null) : base(message, cause)
        {
        }
    }

    /// <summary>
    /// Reads run recordings saved by the harness (Harness/build/recordings/*.json).
    /// Uses Newtonsoft JSON, which keeps null values (a pose that wasn't tracked) instead of
    /// turning them into zeros the way Unity's built-in JsonUtility would.
    /// </summary>
    public static class RecordingLoader
    {
        /// <summary>The format versions this project can read.</summary>
        public static readonly int[] SupportedVersions = { 1, 2 };

        /// <summary>
        /// Harness/build/recordings, found from this Unity project's folder (Harness/unity),
        /// so it works on any computer that has the whole repository.
        /// </summary>
        public static string DefaultFolder =>
            Path.GetFullPath(Path.Combine(Application.dataPath, "..", "..", "build", "recordings"));

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
                    + ", but this project reads versions " + string.Join(" and ", SupportedVersions)
                    + ". Update the Unity replay code (and Harness/README.md) for the new version.");
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

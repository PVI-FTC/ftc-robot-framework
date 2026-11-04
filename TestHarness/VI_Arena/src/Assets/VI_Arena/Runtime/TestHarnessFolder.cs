using System;
using System.IO;
using UnityEngine;

namespace VI_Arena
{
    /// <summary>
    /// Finds the <b>TestHarness</b> folder, the one with the Java harness in <c>th_src</c>
    /// (<c>th_src\gradlew.bat</c>, <c>th_src\fields</c>, <c>th_src\robots</c>, <c>th_src\build\recordings</c>).
    /// Live mode, the recordings list, the field files and the robot looks all start from it.
    ///
    /// <para>The order: <c>-harness &lt;folder&gt;</c> on the command line; else the app's own folder and every
    /// folder above it, each checked for being a TestHarness folder or having one inside (so the app finds
    /// it from <c>TestHarness\VI_Arena\dist</c>, from the Editor's <c>TestHarness\VI_Arena\src\Assets</c>,
    /// and from an unzipped copy put next to a TestHarness folder); else the folder the user picked
    /// last time ("Where is your TestHarness folder?"), if it is still there. Otherwise null: the
    /// app still replays recordings it is given, with its own copy of the field and robot files.</para>
    /// </summary>
    public static class TestHarnessFolder
    {
        /// <summary>The Java harness's folder inside TestHarness.</summary>
        public const string JavaFolderName = "th_src";
        /// <summary>The folder name looked for next to the app and above it.</summary>
        public const string FolderName = "TestHarness";
        /// <summary>Where the folder the user picked is remembered.</summary>
        public const string PreferenceKey = "VI_Arena.testHarnessFolder";
        /// <summary>
        /// The built app's own copy of the field and robot files (VI_Arena_Data\harness-files\fields and
        /// \robots, copied in by the build), used when no TestHarness folder is known.
        /// </summary>
        public const string BundledFolderName = "harness-files";

        /// <summary>True if <paramref name="folder"/> is a TestHarness folder: it has th_src\gradlew.bat.</summary>
        public static bool IsTestHarness(string folder)
        {
            return !string.IsNullOrEmpty(folder) && File.Exists(Path.Combine(folder, JavaFolderName, "gradlew.bat"));
        }

        /// <summary>
        /// A picked or typed folder as a TestHarness folder: the folder itself, or its parent when it is
        /// the <c>th_src</c> folder inside one. Null if it is neither.
        /// </summary>
        public static string Accept(string folder)
        {
            if (string.IsNullOrWhiteSpace(folder))
            {
                return null;
            }
            string full = Path.GetFullPath(folder.Trim().Trim('"'));
            if (IsTestHarness(full))
            {
                return full;
            }
            string parent = Path.GetDirectoryName(full.TrimEnd(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar));
            return IsTestHarness(parent) ? parent : null;
        }

        /// <summary>
        /// The TestHarness folder in <paramref name="start"/> or a folder above it: a folder that is one,
        /// or that has a TestHarness folder inside. Null if there is none all the way up.
        /// </summary>
        public static string Search(string start)
        {
            if (string.IsNullOrEmpty(start))
            {
                return null;
            }
            for (DirectoryInfo folder = new DirectoryInfo(Path.GetFullPath(start)); folder != null; folder = folder.Parent)
            {
                if (IsTestHarness(folder.FullName))
                {
                    return folder.FullName;
                }
                string inside = Path.Combine(folder.FullName, FolderName);
                if (IsTestHarness(inside))
                {
                    return inside;
                }
            }
            return null;
        }

        /// <summary>
        /// The TestHarness folder: <c>-harness &lt;folder&gt;</c>, else <see cref="Search"/> from
        /// <paramref name="dataPath"/> (Application.dataPath), else <paramref name="remembered"/> if it
        /// still is one. Null if none of them is.
        /// </summary>
        public static string Find(string[] args, string dataPath, string remembered)
        {
            for (int i = 0; i < args.Length - 1; i++)
            {
                if (string.Equals(args[i], "-harness", StringComparison.OrdinalIgnoreCase))
                {
                    string given = Accept(args[i + 1]);
                    if (given != null)
                    {
                        return given;
                    }
                }
            }
            return Search(dataPath) ?? Accept(remembered);
        }

        /// <summary>The folder for this app: <see cref="Find"/> with the real command line, data folder and remembered pick.</summary>
        public static string Current => Find(Environment.GetCommandLineArgs(), Application.dataPath, Remembered);

        /// <summary>The folder the user picked last time ("" if none).</summary>
        public static string Remembered => PlayerPrefs.GetString(PreferenceKey, "");

        /// <summary>Remembers a picked folder for the next time the app opens.</summary>
        public static void Remember(string testHarness)
        {
            PlayerPrefs.SetString(PreferenceKey, testHarness);
            PlayerPrefs.Save();
        }

        /// <summary>TestHarness\th_src: the Java harness (gradlew.bat), or null without a TestHarness folder.</summary>
        public static string JavaHarness(string testHarness)
        {
            return testHarness == null ? null : Path.Combine(testHarness, JavaFolderName);
        }

        /// <summary>
        /// A folder of harness files (<c>fields</c> or <c>robots</c>): TestHarness\th_src\&lt;name&gt; when
        /// the TestHarness folder is known, else the built app's own copy in
        /// &lt;dataPath&gt;\harness-files\&lt;name&gt;.
        /// </summary>
        public static string HarnessFiles(string testHarness, string dataPath, string name)
        {
            return testHarness != null
                ? Path.Combine(testHarness, JavaFolderName, name)
                : Path.GetFullPath(Path.Combine(dataPath, BundledFolderName, name));
        }
    }
}

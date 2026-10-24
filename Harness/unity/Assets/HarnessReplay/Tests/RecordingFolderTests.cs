using System.IO;
using NUnit.Framework;

namespace PviHarness.Replay.Tests
{
    /// <summary>
    /// Finding Harness/build/recordings both in the Unity Editor and in the built app
    /// (Harness/unity/Build/HarnessReplay.exe), and from the command line (unity-view.bat).
    /// </summary>
    public class RecordingFolderTests
    {
        private static readonly string Harness = Path.GetFullPath(Path.Combine(Path.GetTempPath(), "pvi-test", "Harness"));
        private static readonly string Expected = Path.Combine(Harness, "build", "recordings");

        /// <summary>
        /// WHAT: In the Editor, the Assets folder is Harness/unity/Assets, two levels below Harness.
        /// </summary>
        [Test]
        public void EditorLayoutFindsHarnessBuildRecordings()
        {
            string assets = Path.Combine(Harness, "unity", "Assets");

            Assert.AreEqual(Expected, RecordingLoader.ResolveFolder(new[] { "Unity.exe" }, assets, true));
        }

        /// <summary>
        /// WHAT: In the built app, the data folder is Harness/unity/Build/HarnessReplay_Data, three levels below Harness.
        /// WHY: The Editor rule would look in Harness/unity/build/recordings there, which doesn't exist,
        /// so a double-clicked app would show no recordings.
        /// </summary>
        [Test]
        public void BuiltAppLayoutFindsHarnessBuildRecordings()
        {
            string data = Path.Combine(Harness, "unity", "Build", "HarnessReplay_Data");

            Assert.AreEqual(Expected, RecordingLoader.ResolveFolder(new[] { "HarnessReplay.exe" }, data, false));
        }

        /// <summary>
        /// WHAT: "-recordings &lt;folder&gt;" on the command line wins; otherwise a dropped .json's folder is used,
        /// and that file is the one to open first.
        /// </summary>
        [Test]
        public void CommandLineChoosesTheFolderAndFile()
        {
            string data = Path.Combine(Harness, "unity", "Build", "HarnessReplay_Data");
            string other = Path.Combine(Path.GetTempPath(), "pvi-test", "elsewhere");
            string dropped = Path.Combine(other, "my-drive.json");

            Assert.AreEqual(Path.GetFullPath(other),
                RecordingLoader.ResolveFolder(new[] { "HarnessReplay.exe", "-recordings", other }, data, false));
            Assert.AreEqual(Path.GetFullPath(other),
                RecordingLoader.ResolveFolder(new[] { "HarnessReplay.exe", dropped }, data, false));
            Assert.AreEqual(dropped, RecordingLoader.FileFromCommandLine(new[] { "HarnessReplay.exe", "-recordings", other, dropped }));
            Assert.IsNull(RecordingLoader.FileFromCommandLine(new[] { "HarnessReplay.exe", "-recordings", other }));
        }
    }
}

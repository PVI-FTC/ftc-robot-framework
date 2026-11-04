using System.IO;
using NUnit.Framework;

namespace VI_Arena.Tests
{
    /// <summary>
    /// The recordings folder from the command line (VI_Arena.bat, or a recording dropped on
    /// VI_Arena.exe). Finding TestHarness/th_src/build/recordings itself: <see cref="TestHarnessFolderTests"/>.
    /// </summary>
    public class RecordingFolderTests
    {
        private static readonly string TestHarness = Path.GetFullPath(Path.Combine(Path.GetTempPath(), "pvi-test", "TestHarness"));

        /// <summary>
        /// WHAT: "-recordings &lt;folder&gt;" on the command line wins; otherwise a dropped .json's folder is used,
        /// and that file is the one to open first. Both work without a TestHarness folder too.
        /// </summary>
        [Test]
        public void CommandLineChoosesTheFolderAndFile()
        {
            string other = Path.Combine(Path.GetTempPath(), "pvi-test", "elsewhere");
            string dropped = Path.Combine(other, "my-drive.json");

            Assert.AreEqual(Path.GetFullPath(other),
                RecordingLoader.ResolveFolder(new[] { "VI_Arena.exe", "-recordings", other }, TestHarness));
            Assert.AreEqual(Path.GetFullPath(other),
                RecordingLoader.ResolveFolder(new[] { "VI_Arena.exe", dropped }, TestHarness));
            Assert.AreEqual(Path.GetFullPath(other),
                RecordingLoader.ResolveFolder(new[] { "VI_Arena.exe", dropped }, null));
            Assert.AreEqual(dropped, RecordingLoader.FileFromCommandLine(new[] { "VI_Arena.exe", "-recordings", other, dropped }));
            Assert.IsNull(RecordingLoader.FileFromCommandLine(new[] { "VI_Arena.exe", "-recordings", other }));
        }
    }
}

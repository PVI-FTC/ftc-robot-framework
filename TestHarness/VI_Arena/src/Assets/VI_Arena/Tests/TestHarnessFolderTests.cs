using System.IO;
using NUnit.Framework;
using UnityEngine;

namespace VI_Arena.Tests
{
    /// <summary>
    /// Finding the TestHarness folder (<see cref="TestHarnessFolder"/>) from the Editor, from the built
    /// app in VI_Arena/dist, and from an unzipped copy somewhere else, and the folders read from it.
    /// The tests make small pretend folders in the temp folder (only th_src/gradlew.bat is needed).
    /// </summary>
    public class TestHarnessFolderTests
    {
        private string root;

        [SetUp]
        public void MakeRoot()
        {
            root = Path.Combine(Path.GetTempPath(), "vi-arena-test-" + System.Guid.NewGuid().ToString("N"));
            Directory.CreateDirectory(root);
        }

        [TearDown]
        public void RemoveRoot()
        {
            Directory.Delete(root, true);
        }

        /// <summary>A pretend TestHarness folder at root/&lt;path&gt;, with th_src/gradlew.bat.</summary>
        private string MakeTestHarness(string path)
        {
            string folder = Path.Combine(root, path);
            Directory.CreateDirectory(Path.Combine(folder, "th_src"));
            File.WriteAllText(Path.Combine(folder, "th_src", "gradlew.bat"), "@echo off");
            return folder;
        }

        private string MakeFolder(string path)
        {
            string folder = Path.Combine(root, path);
            Directory.CreateDirectory(folder);
            return folder;
        }

        /// <summary>
        /// WHAT: The Editor (TestHarness/VI_Arena/src/Assets) and the built app
        /// (TestHarness/VI_Arena/dist/VI_Arena_Data) both find the TestHarness folder above them.
        /// </summary>
        [Test]
        public void TheEditorAndTheBuiltAppFindTheTestHarnessAboveThem()
        {
            string testHarness = MakeTestHarness(Path.Combine("repo", "TestHarness"));
            string assets = MakeFolder(Path.Combine("repo", "TestHarness", "VI_Arena", "src", "Assets"));
            string data = MakeFolder(Path.Combine("repo", "TestHarness", "VI_Arena", "dist", "VI_Arena_Data"));

            Assert.AreEqual(testHarness, TestHarnessFolder.Find(new[] { "Unity.exe" }, assets, ""));
            Assert.AreEqual(testHarness, TestHarnessFolder.Find(new[] { "VI_Arena.exe" }, data, ""));
        }

        /// <summary>
        /// WHAT: An unzipped copy put next to a TestHarness folder (or in a folder above one) finds it.
        /// </summary>
        [Test]
        public void AnUnzippedCopyFindsATestHarnessNextToIt()
        {
            string testHarness = MakeTestHarness(Path.Combine("team", "TestHarness"));
            string data = MakeFolder(Path.Combine("team", "VI_Arena-2026-11-02", "VI_Arena_Data"));

            Assert.AreEqual(testHarness, TestHarnessFolder.Find(new[] { "VI_Arena.exe" }, data, ""));
        }

        /// <summary>
        /// WHAT: An unzipped copy with no TestHarness nearby uses the folder picked last time, while it
        /// still is one; otherwise it has none (null), and Live mode asks.
        /// </summary>
        [Test]
        public void AnUnzippedCopyAloneUsesThePickedFolderWhileItIsStillThere()
        {
            string data = MakeFolder(Path.Combine("Downloads", "VI_Arena", "VI_Arena_Data"));
            string picked = MakeTestHarness(Path.Combine("elsewhere", "TestHarness"));

            Assert.IsNull(TestHarnessFolder.Find(new[] { "VI_Arena.exe" }, data, ""), "nothing nearby, nothing picked");
            Assert.AreEqual(picked, TestHarnessFolder.Find(new[] { "VI_Arena.exe" }, data, picked));
            Assert.IsNull(TestHarnessFolder.Find(new[] { "VI_Arena.exe" }, data, Path.Combine(root, "gone")), "a picked folder that's gone");
        }

        /// <summary>
        /// WHAT: "-harness &lt;folder&gt;" and the folder picker take the TestHarness folder or its th_src
        /// folder, and refuse a folder that is neither.
        /// </summary>
        [Test]
        public void ThePickedFolderCanBeTestHarnessOrItsThSrc()
        {
            string testHarness = MakeTestHarness("TestHarness");
            string other = MakeFolder("Documents");
            string data = MakeFolder(Path.Combine("Downloads", "VI_Arena_Data"));

            Assert.AreEqual(testHarness, TestHarnessFolder.Accept(testHarness));
            Assert.AreEqual(testHarness, TestHarnessFolder.Accept(Path.Combine(testHarness, "th_src")));
            Assert.AreEqual(testHarness, TestHarnessFolder.Accept(Path.Combine(testHarness, "th_src") + Path.DirectorySeparatorChar));
            Assert.IsNull(TestHarnessFolder.Accept(other));
            Assert.IsNull(TestHarnessFolder.Accept(""));
            Assert.AreEqual(testHarness, TestHarnessFolder.Find(new[] { "VI_Arena.exe", "-harness", Path.Combine(testHarness, "th_src") }, data, ""));
        }

        /// <summary>
        /// WHAT: With a TestHarness folder, the recordings, field files and robots come from its th_src;
        /// without one, there is no recordings list and the fields and robots come from the app's own copy.
        /// WHY: An unzipped copy alone must still draw the season field and the robot.
        /// </summary>
        [Test]
        public void FoldersComeFromThSrcOrTheAppsOwnCopy()
        {
            string testHarness = MakeTestHarness("TestHarness");
            string data = MakeFolder(Path.Combine("Downloads", "VI_Arena_Data"));
            string[] args = { "VI_Arena.exe" };

            Assert.AreEqual(Path.Combine(testHarness, "th_src", "build", "recordings"), RecordingLoader.ResolveFolder(args, testHarness));
            Assert.AreEqual(Path.Combine(testHarness, "th_src", "fields"), FieldLayouts.ResolveFolder(args, testHarness, data));
            Assert.AreEqual(Path.Combine(testHarness, "th_src", "robots"), RobotProfile.ResolveFolder(testHarness, data));

            Assert.IsNull(RecordingLoader.ResolveFolder(args, null));
            Assert.AreEqual(Path.Combine(data, "harness-files", "fields"), FieldLayouts.ResolveFolder(args, null, data));
            Assert.AreEqual(Path.Combine(data, "harness-files", "robots"), RobotProfile.ResolveFolder(null, data));
        }

        /// <summary>WHAT: This Unity project itself sits in a TestHarness folder (the repository's layout).</summary>
        [Test]
        public void ThisProjectIsInsideATestHarnessFolder()
        {
            string found = TestHarnessFolder.Search(Application.dataPath);

            Assert.IsNotNull(found);
            Assert.AreEqual("TestHarness", Path.GetFileName(found));
            Assert.IsTrue(File.Exists(Path.Combine(found, "th_src", "fields", "biobuzz-2026.json")));
        }
    }
}

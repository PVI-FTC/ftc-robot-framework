using System.IO;
using NUnit.Framework;

namespace PviHarness.Replay.Tests
{
    /// <summary>
    /// The Live mode button's launcher: finding the Harness folder, the Gradle command it runs, and
    /// finding the bridge's process to stop it. (Starting the real bridge is checked by
    /// <b>Harness → Live Mode Smoke Test</b>.)
    /// </summary>
    public class HarnessLauncherTests
    {
        private static readonly string Harness = Path.GetFullPath(Path.Combine(Path.GetTempPath(), "pvi-test", "Harness"));

        /// <summary>
        /// WHAT: The Harness folder is found from the Editor (Harness/unity/Assets) and from the built
        /// app (Harness/unity/Build/HarnessReplay_Data); "-harness &lt;folder&gt;" overrides both.
        /// </summary>
        [Test]
        public void HarnessFolderIsFoundInBothLayouts()
        {
            Assert.AreEqual(Harness, HarnessLauncher.ResolveHarnessFolder(new[] { "Unity.exe" }, Path.Combine(Harness, "unity", "Assets"), true));
            Assert.AreEqual(Harness, HarnessLauncher.ResolveHarnessFolder(new[] { "HarnessReplay.exe" },
                Path.Combine(Harness, "unity", "Build", "HarnessReplay_Data"), false));
            string other = Path.GetFullPath(Path.Combine(Path.GetTempPath(), "pvi-test", "other"));
            Assert.AreEqual(other, HarnessLauncher.ResolveHarnessFolder(new[] { "HarnessReplay.exe", "-harness", other }, "x", false));
        }

        /// <summary>
        /// WHAT: The command runs Harness\gradlew.bat unityBridge hidden, from the Harness folder, with
        /// its output in build\unity-bridge.log, Android Studio's Java when given, and -Pport only for a
        /// port other than 7465.
        /// </summary>
        [Test]
        public void CommandRunsGradleUnityBridgeHidden()
        {
            var info = HarnessLauncher.Command(Harness, TcpBridgeTransport.DefaultPort, @"C:\Java");

            Assert.AreEqual("cmd.exe", info.FileName);
            StringAssert.Contains(Path.Combine(Harness, "gradlew.bat") + "\" unityBridge >", info.Arguments);
            StringAssert.Contains(Path.Combine(Harness, "build", "unity-bridge.log"), info.Arguments);
            StringAssert.DoesNotContain("-Pport", info.Arguments);
            Assert.AreEqual(Harness, info.WorkingDirectory);
            Assert.IsTrue(info.CreateNoWindow);
            Assert.AreEqual(@"C:\Java", info.EnvironmentVariables["JAVA_HOME"]);
            StringAssert.Contains("unityBridge -Pport=7500 >", HarnessLauncher.Command(Harness, 7500, null).Arguments);
        }

        /// <summary>
        /// WHAT: The bridge's process is the one listening on 127.0.0.1:7465 in netstat's output; other
        /// ports, other states and a missing listener are told apart.
        /// WHY: Gradle runs the bridge in its own Java process, so switching Live mode off has to end
        /// that process, not just Gradle.
        /// </summary>
        [Test]
        public void FindsTheProcessListeningOnThePort()
        {
            string netstat = "\r\nActive Connections\r\n\r\n  Proto  Local Address          Foreign Address        State           PID\r\n"
                + "  TCP    127.0.0.1:7465         127.0.0.1:50122        ESTABLISHED     1111\r\n"
                + "  TCP    127.0.0.1:7465         0.0.0.0:0              LISTENING       4242\r\n"
                + "  TCP    0.0.0.0:7500           0.0.0.0:0              LISTENING       5555\r\n";

            Assert.AreEqual(4242, HarnessLauncher.ListeningProcess(netstat, 7465));
            Assert.AreEqual(5555, HarnessLauncher.ListeningProcess(netstat, 7500));
            Assert.AreEqual(-1, HarnessLauncher.ListeningProcess(netstat, 7600));
        }

        /// <summary>
        /// WHAT: Without gradlew.bat in the Harness folder, Live mode fails with a message saying where it looked.
        /// </summary>
        [Test]
        public void MissingGradleFailsClearly()
        {
            string empty = Path.Combine(Path.GetTempPath(), "pvi-test", "no-harness");
            Directory.CreateDirectory(empty);
            var launcher = new HarnessLauncher(empty, 7699); // a port nothing listens on

            launcher.Start(0);

            Assert.AreEqual(HarnessLauncher.LaunchState.Failed, launcher.State);
            StringAssert.Contains("Can't find gradlew.bat in " + empty, launcher.Problem);
        }

        /// <summary>
        /// WHAT: A usable Java is found the way gradlew.bat finds it (JAVA_HOME, else java.exe on the
        /// PATH), and one older than 17, a JAVA_HOME without java.exe, or no Java at all is a problem.
        /// The version comes from the Java folder's "release" file ("1.8.0_392" is Java 8).
        /// </summary>
        [Test]
        public void JavaOlderThan17OrMissingIsAProblem()
        {
            string java25 = FakeJava("jdk-25", "25.0.1");
            string java17 = FakeJava("jdk-17", "17.0.9");
            string java11 = FakeJava("jdk-11", "11.0.21");
            string java8 = FakeJava("jdk-8", "1.8.0_392");
            string notJava = Path.Combine(Harness, "..", "java", "empty");
            Directory.CreateDirectory(notJava);

            Assert.IsNull(HarnessLauncher.JavaProblem(java25, null));
            Assert.IsNull(HarnessLauncher.JavaProblem(java17, null), "17 is new enough");
            StringAssert.Contains("is Java 11", HarnessLauncher.JavaProblem(java11, null));
            StringAssert.Contains("is Java 8", HarnessLauncher.JavaProblem(java8, null));
            StringAssert.Contains("has no bin\\java.exe", HarnessLauncher.JavaProblem(notJava, null));

            string path = notJava + ";" + Path.Combine(java17, "bin");
            Assert.IsNull(HarnessLauncher.JavaProblem(null, path), "java on the PATH, after a folder without one");
            StringAssert.Contains("java on PATH", HarnessLauncher.JavaProblem("", Path.Combine(java11, "bin")));
            StringAssert.Contains("no java on PATH", HarnessLauncher.JavaProblem(null, notJava));
        }

        /// <summary>
        /// WHAT: With no usable Java, Live mode fails at once with a plain "needs Java 17 or newer"
        /// message (what to install), and doesn't try to run Gradle.
        /// WHY: Otherwise it only said "The harness side didn't start." with Gradle's log line.
        /// </summary>
        [Test]
        public void NoUsableJavaSaysWhatToInstall()
        {
            string harness = Path.Combine(Path.GetTempPath(), "pvi-test", "harness-no-java");
            Directory.CreateDirectory(harness);
            File.WriteAllText(Path.Combine(harness, "gradlew.bat"), "@exit /b 1\r\n");
            if (Directory.Exists(Path.Combine(harness, "build")))
            {
                Directory.Delete(Path.Combine(harness, "build"), true); // a log from an earlier run
            }
            string javaHome = System.Environment.GetEnvironmentVariable("JAVA_HOME");
            try
            {
                System.Environment.SetEnvironmentVariable("JAVA_HOME", FakeJava("jdk-11-start", "11.0.21"));
                var launcher = new HarnessLauncher(harness, 7699); // a port nothing listens on

                launcher.Start(0);

                Assert.AreEqual(HarnessLauncher.LaunchState.Failed, launcher.State);
                StringAssert.StartsWith(HarnessLauncher.NeedsJavaMessage, launcher.Problem);
                StringAssert.Contains("is Java 11", launcher.Problem);
                Assert.IsFalse(File.Exists(Path.Combine(harness, "build", "unity-bridge.log")), "Gradle was never started");
            }
            finally
            {
                System.Environment.SetEnvironmentVariable("JAVA_HOME", javaHome);
            }
        }

        /// <summary>
        /// WHAT: A Harness folder on a USB stick (removable, or FAT32 / FAT / exFAT) or on a drive with under
        /// 1 GB free is a problem, named in plain words; a normal NTFS drive with room, or a drive Windows
        /// can't describe, is fine.
        /// WHY: Gradle fails part-way on a USB stick ("Unable to delete directory ... process has files open").
        /// </summary>
        [Test]
        public void UsbSticksAndFullDrivesAreAProblem()
        {
            const long gb = 1L << 30;
            // The same sentences view.bat prints for the same drive.
            Assert.AreEqual("This copy of the harness is on a USB stick or other removable drive (FAT32).",
                HarnessLauncher.DriveProblem(new HarnessLauncher.DriveFacts(true, "FAT32", 50 * gb)));
            Assert.AreEqual("This copy of the harness is on a drive formatted exFAT, like a USB stick.",
                HarnessLauncher.DriveProblem(new HarnessLauncher.DriveFacts(false, "exFAT", 50 * gb)));
            Assert.AreEqual("The drive has only 290 MB free; building needs about 1 GB.",
                HarnessLauncher.DriveProblem(new HarnessLauncher.DriveFacts(false, "NTFS", 290L * 1024 * 1024)));
            Assert.AreEqual("This copy of the harness is on a USB stick or other removable drive (FAT32). "
                + "The drive has only 290 MB free; building needs about 1 GB.",
                HarnessLauncher.DriveProblem(new HarnessLauncher.DriveFacts(true, "FAT32", 290L * 1024 * 1024)), "both, like the user's stick");
            Assert.IsNull(HarnessLauncher.DriveProblem(new HarnessLauncher.DriveFacts(false, "NTFS", 50 * gb)), "a normal PC drive");
            Assert.IsNull(HarnessLauncher.DriveProblem(null), "unknown: don't block");
        }

        /// <summary>
        /// WHAT: On a USB stick, Live mode fails at once with the "copy it to this PC" message and never
        /// starts Gradle.
        /// </summary>
        [Test]
        public void LiveModeRefusesAUsbStick()
        {
            string harness = Path.Combine(Path.GetTempPath(), "pvi-test", "harness-on-usb");
            Directory.CreateDirectory(harness);
            File.WriteAllText(Path.Combine(harness, "gradlew.bat"), "@exit /b 1\r\n");
            if (Directory.Exists(Path.Combine(harness, "build")))
            {
                Directory.Delete(Path.Combine(harness, "build"), true);
            }
            var launcher = new HarnessLauncher(harness, 7699) // a port nothing listens on
            {
                DriveReader = folder => new HarnessLauncher.DriveFacts(true, "FAT32", 290L * 1024 * 1024),
            };

            launcher.Start(0);

            Assert.AreEqual(HarnessLauncher.LaunchState.Failed, launcher.State);
            StringAssert.StartsWith("This copy of the harness is on a USB stick", launcher.Problem);
            StringAssert.Contains("only 290 MB free", launcher.Problem);
            StringAssert.EndsWith(HarnessLauncher.UsbDriveAdvice, launcher.Problem);
            Assert.IsFalse(File.Exists(Path.Combine(harness, "build", "unity-bridge.log")), "Gradle was never started");
        }

        /// <summary>A folder that looks like a Java install: bin\java.exe (empty) and a release file.</summary>
        private static string FakeJava(string name, string version)
        {
            string folder = Path.GetFullPath(Path.Combine(Harness, "..", "java", name));
            Directory.CreateDirectory(Path.Combine(folder, "bin"));
            File.WriteAllText(Path.Combine(folder, "bin", "java.exe"), "");
            File.WriteAllText(Path.Combine(folder, "release"), "IMPLEMENTOR=\"Test\"\nJAVA_VERSION=\"" + version + "\"\n");
            return folder;
        }
    }
}

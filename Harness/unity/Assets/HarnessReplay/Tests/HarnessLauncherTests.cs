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
    }
}

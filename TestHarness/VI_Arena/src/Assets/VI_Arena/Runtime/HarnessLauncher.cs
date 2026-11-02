using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Net.NetworkInformation;

namespace VI_Arena
{
    /// <summary>
    /// Starts and stops the harness side of live play (<c>.\gradlew.bat unityBridge</c> in
    /// TestHarness\th_src) for the <b>Live mode</b> button, so nobody has to type the command. It runs
    /// Gradle hidden in the background (output in th_src/build/unity-bridge.log), watches for the bridge to start
    /// listening, and when switched off (or when Unity closes) ends the bridge it started.
    ///
    /// <para>Nothing here blocks: <see cref="Poll"/> is called every frame and only looks.
    /// A bridge someone started by hand is used as it is and never stopped from here.</para>
    /// </summary>
    public sealed class HarnessLauncher : IDisposable
    {
        /// <summary>Where live mode is.</summary>
        public enum LaunchState
        {
            /// <summary>Not running (or switched off).</summary>
            Off,
            /// <summary>Gradle is starting the bridge (the first time takes up to a minute).</summary>
            Starting,
            /// <summary>The bridge is listening: live OpModes can be picked.</summary>
            Running,
            /// <summary>It didn't start; <see cref="Problem"/> says why.</summary>
            Failed,
        }

        /// <summary>How long to wait for Gradle to start the bridge before giving up.</summary>
        public const double StartTimeoutSeconds = 180;
        /// <summary>Android Studio's Java, which the harness docs use for Gradle.</summary>
        public const string AndroidStudioJava = @"C:\Program Files\Android\Android Studio\jbr";
        /// <summary>The oldest Java the harness build runs on.</summary>
        public const int MinimumJava = 17;
        /// <summary>Less free space than this on the Harness folder's drive and Live mode won't start (Gradle's build folder grows to about 0.5 GB).</summary>
        public const long MinimumFreeBytes = 1L << 30;
        /// <summary>
        /// What Live mode adds after <see cref="DriveProblem"/>'s sentences, worded like view.bat's own
        /// drive check (the window puts "Live mode: " first).
        /// </summary>
        public const string UsbDriveAdvice = "Building here is slow and often fails (for example \"Unable to delete "
            + "directory\"). Copy the whole ftc-robot-framework folder to this PC's own drive (for example C:\\Dev) "
            + "and open the app from there.";

        /// <summary>
        /// What Live mode needs to know about the drive the Harness folder is on: whether Windows calls
        /// it removable, its file system ("NTFS", "FAT32", "exFAT") and its free space.
        /// </summary>
        public readonly struct DriveFacts
        {
            public DriveFacts(bool removable, string format, long freeBytes)
            {
                Removable = removable;
                Format = format ?? "";
                FreeBytes = freeBytes;
            }

            public bool Removable { get; }
            public string Format { get; }
            public long FreeBytes { get; }
        }

        /// <summary>What Live mode says when there is no Java it can use (the window puts "Live mode: " first).</summary>
        public const string NeedsJavaMessage = "Needs Java 17 or newer: install Android Studio or a "
            + "Java 17+ JDK (adoptium.net) and set JAVA_HOME.";

        private readonly string harnessFolder;
        private readonly int port;
        private Process gradle;
        private bool ownBridge;
        private double startedAt;

        /// <summary>
        /// A launcher for the Java harness in <paramref name="harnessFolder"/> (TestHarness\th_src, the folder
        /// with gradlew.bat; see <see cref="TestHarnessFolder.JavaHarness"/>), listening on <paramref name="port"/>.
        /// </summary>
        public HarnessLauncher(string harnessFolder, int port)
        {
            this.harnessFolder = harnessFolder;
            this.port = port;
        }

        /// <summary>Where live mode is now.</summary>
        public LaunchState State { get; private set; } = LaunchState.Off;
        /// <summary>Reads the Harness folder's drive (<see cref="ReadDrive"/>); tests put in a pretend drive.</summary>
        public Func<string, DriveFacts?> DriveReader { get; set; } = ReadDrive;
        /// <summary>Why it failed (with the end of the Gradle log when there is one), or null.</summary>
        public string Problem { get; private set; }
        /// <summary>True if the bridge was already running when live mode was switched on (started by hand).</summary>
        public bool StartedOutside => State == LaunchState.Running && !ownBridge;
        /// <summary>Gradle's output: TestHarness/th_src/build/unity-bridge.log.</summary>
        public string LogPath => Path.Combine(harnessFolder, "build", "unity-bridge.log");

        /// <summary>
        /// The command that starts the bridge: <c>cmd.exe</c> running th_src\gradlew.bat unityBridge
        /// (with <c>-Pport</c> for a port other than 7465), its output going to the log file.
        /// </summary>
        public static ProcessStartInfo Command(string harnessFolder, int port, string javaHome)
        {
            string gradlew = Path.Combine(harnessFolder, "gradlew.bat");
            string log = Path.Combine(harnessFolder, "build", "unity-bridge.log");
            string portArgument = port == TcpBridgeTransport.DefaultPort ? "" : " -Pport=" + port;
            var info = new ProcessStartInfo("cmd.exe",
                "/c \"\"" + gradlew + "\" unityBridge" + portArgument + " > \"" + log + "\" 2>&1\"")
            {
                WorkingDirectory = harnessFolder,
                UseShellExecute = false,
                CreateNoWindow = true,
            };
            if (javaHome != null)
            {
                info.EnvironmentVariables["JAVA_HOME"] = javaHome;
            }
            return info;
        }

        /// <summary>
        /// The process listening on 127.0.0.1:<paramref name="port"/> in <c>netstat -ano</c> output, or
        /// -1. (Gradle runs the bridge in its own Java process, so stopping Gradle alone can leave it.)
        /// </summary>
        public static int ListeningProcess(string netstatOutput, int port)
        {
            foreach (string line in netstatOutput.Split('\n'))
            {
                string[] parts = line.Split(new[] { ' ', '\t', '\r' }, StringSplitOptions.RemoveEmptyEntries);
                if (parts.Length >= 5 && parts[0] == "TCP" && parts[3] == "LISTENING"
                    && (parts[1] == "127.0.0.1:" + port || parts[1] == "0.0.0.0:" + port)
                    && int.TryParse(parts[4], out int pid))
                {
                    return pid;
                }
            }
            return -1;
        }

        /// <summary>
        /// Why there is no Java that gradlew.bat could use, or null if there is one. gradlew.bat uses
        /// <paramref name="javaHome"/> when it is set, else the first java.exe on
        /// <paramref name="pathVariable"/> (the PATH). A Java whose version can be read from its
        /// <c>release</c> file must be 17 or newer; one without that file is given the benefit of the
        /// doubt (Gradle then says what is wrong in the log).
        /// </summary>
        public static string JavaProblem(string javaHome, string pathVariable)
        {
            if (!string.IsNullOrEmpty(javaHome))
            {
                if (!File.Exists(Path.Combine(javaHome, "bin", "java.exe")))
                {
                    return "JAVA_HOME (" + javaHome + ") has no bin\\java.exe";
                }
                return TooOld(javaHome, "JAVA_HOME (" + javaHome + ")");
            }
            foreach (string folder in (pathVariable ?? "").Split(new[] { ';' }, StringSplitOptions.RemoveEmptyEntries))
            {
                string java;
                try
                {
                    java = Path.Combine(folder.Trim().Trim('"'), "java.exe");
                }
                catch (ArgumentException)
                {
                    continue; // a PATH entry with characters a path can't have
                }
                if (File.Exists(java))
                {
                    return TooOld(Path.GetDirectoryName(Path.GetDirectoryName(java)), "The java on PATH (" + java + ")");
                }
            }
            return "no JAVA_HOME, no Android Studio Java and no java on PATH";
        }

        /// <summary>
        /// The Java version in a Java folder's <c>release</c> file (JAVA_VERSION="17.0.9" is 17, and
        /// "1.8.0_392" is 8), or -1 if the file is missing or can't be read.
        /// </summary>
        public static int JavaMajorVersion(string javaFolder)
        {
            try
            {
                string release = Path.Combine(javaFolder ?? "", "release");
                if (!File.Exists(release))
                {
                    return -1;
                }
                string line = File.ReadAllLines(release).FirstOrDefault(l => l.StartsWith("JAVA_VERSION=", StringComparison.Ordinal));
                if (line == null)
                {
                    return -1;
                }
                string[] parts = line.Substring("JAVA_VERSION=".Length).Trim().Trim('"').Split('.', '_', '-', '+');
                if (!int.TryParse(parts[0], out int major))
                {
                    return -1;
                }
                return major == 1 && parts.Length > 1 && int.TryParse(parts[1], out int old) ? old : major;
            }
            catch (Exception e) when (e is IOException || e is UnauthorizedAccessException || e is ArgumentException)
            {
                return -1;
            }
        }

        private static string TooOld(string javaFolder, string which)
        {
            int major = JavaMajorVersion(javaFolder);
            return major >= 0 && major < MinimumJava ? which + " is Java " + major : null;
        }

        /// <summary>
        /// The drive <paramref name="folder"/> is on, or null if Windows can't say (a network path, a
        /// drive that isn't ready, any error): then Live mode doesn't block.
        /// </summary>
        public static DriveFacts? ReadDrive(string folder)
        {
            try
            {
                var drive = new DriveInfo(Path.GetPathRoot(Path.GetFullPath(folder)));
                if (!drive.IsReady)
                {
                    return null;
                }
                return new DriveFacts(drive.DriveType == DriveType.Removable, drive.DriveFormat, drive.AvailableFreeSpace);
            }
            catch (Exception e) when (e is IOException || e is ArgumentException || e is UnauthorizedAccessException
                || e is NotSupportedException || e is System.Security.SecurityException)
            {
                return null;
            }
        }

        /// <summary>
        /// Why Gradle shouldn't build on this drive, in view.bat's words, or null if it's fine (or unknown).
        /// The same rules as view.bat: a removable drive, or a FAT32 / FAT / exFAT one (a USB stick: Gradle
        /// fails part-way with "Unable to delete directory ... process has files open"), or under 1024 MB
        /// free (the build folder needs about 0.5 GB). Both sentences when both are true.
        /// </summary>
        public static string DriveProblem(DriveFacts? drive)
        {
            if (!drive.HasValue)
            {
                return null;
            }
            DriveFacts d = drive.Value;
            string format = d.Format.ToUpperInvariant();
            bool fat = format == "FAT32" || format == "FAT" || format == "EXFAT";
            var sentences = new List<string>();
            if (d.Removable)
            {
                sentences.Add("This copy of the harness is on a USB stick or other removable drive (" + d.Format + ").");
            }
            else if (fat)
            {
                sentences.Add("This copy of the harness is on a drive formatted " + d.Format + ", like a USB stick.");
            }
            if (d.FreeBytes < MinimumFreeBytes)
            {
                sentences.Add("The drive has only " + d.FreeBytes / (1024 * 1024) + " MB free; building needs about 1 GB.");
            }
            return sentences.Count == 0 ? null : string.Join(" ", sentences);
        }

        /// <summary>True if something is listening on this computer's port (no connection is made).</summary>
        public static bool IsListening(int port)
        {
            try
            {
                return IPGlobalProperties.GetIPGlobalProperties().GetActiveTcpListeners().Any(e => e.Port == port);
            }
            catch (NetworkInformationException)
            {
                return false;
            }
        }

        /// <summary>
        /// Switches live mode on: uses a bridge that is already listening, else starts one with Gradle.
        /// <paramref name="nowSeconds"/> is the current time (Time.realtimeSinceStartup), for the timeout.
        /// </summary>
        public void Start(double nowSeconds)
        {
            if (State == LaunchState.Starting || State == LaunchState.Running)
            {
                return;
            }
            Problem = null;
            if (IsListening(port))
            {
                ownBridge = false;
                State = LaunchState.Running;
                return;
            }
            if (!File.Exists(Path.Combine(harnessFolder, "gradlew.bat")))
            {
                Fail("Can't find gradlew.bat in " + harnessFolder + ". Live mode needs the whole TestHarness folder.");
                return;
            }
            string javaHome = Environment.GetEnvironmentVariable("JAVA_HOME");
            if (string.IsNullOrEmpty(javaHome) && Directory.Exists(AndroidStudioJava))
            {
                javaHome = AndroidStudioJava;
            }
            string badDrive = DriveProblem(DriveReader(harnessFolder));
            if (badDrive != null)
            {
                // Gradle would fail part-way with "Unable to delete directory": say what to do instead.
                Problem = badDrive + " " + UsbDriveAdvice;
                State = LaunchState.Failed;
                return;
            }
            string noJava = JavaProblem(javaHome, Environment.GetEnvironmentVariable("PATH"));
            if (noJava != null)
            {
                // Gradle can't run at all, so its log would only be an old one: say what to install instead.
                Problem = NeedsJavaMessage + " (" + noJava + ")";
                State = LaunchState.Failed;
                return;
            }
            try
            {
                Directory.CreateDirectory(Path.GetDirectoryName(LogPath));
                gradle = Process.Start(Command(harnessFolder, port, javaHome));
            }
            catch (Exception e) when (e is System.ComponentModel.Win32Exception || e is IOException || e is InvalidOperationException)
            {
                Fail("Could not run gradlew.bat: " + e.Message);
                return;
            }
            ownBridge = true;
            startedAt = nowSeconds;
            State = LaunchState.Starting;
        }

        /// <summary>Checks on a starting bridge (call every frame; it only looks, never waits).</summary>
        public void Poll(double nowSeconds)
        {
            if (State == LaunchState.Running && ownBridge && gradle != null && gradle.HasExited && !IsListening(port))
            {
                Fail("The harness side stopped.");
                return;
            }
            if (State != LaunchState.Starting)
            {
                return;
            }
            if (IsListening(port))
            {
                State = LaunchState.Running;
            }
            else if (gradle.HasExited)
            {
                Fail("The harness side didn't start.");
            }
            else if (nowSeconds - startedAt > StartTimeoutSeconds)
            {
                Stop();
                Fail("The harness side took over " + StartTimeoutSeconds + " s to start.");
            }
        }

        /// <summary>Switches live mode off, ending the bridge this launcher started (a hand-started one is left running).</summary>
        public void Stop()
        {
            if (ownBridge)
            {
                if (gradle != null && !gradle.HasExited)
                {
                    RunQuietly("taskkill", "/PID " + gradle.Id + " /T /F");
                }
                int bridge = ListeningProcess(RunQuietly("netstat", "-ano -p TCP"), port);
                if (bridge > 0)
                {
                    RunQuietly("taskkill", "/PID " + bridge + " /T /F");
                }
            }
            gradle = null;
            ownBridge = false;
            State = LaunchState.Off;
        }

        public void Dispose()
        {
            Stop();
        }

        private void Fail(string why)
        {
            string tail = "";
            try
            {
                if (File.Exists(LogPath))
                {
                    string last = File.ReadAllLines(LogPath).LastOrDefault(l => l.Trim().Length > 0);
                    tail = last == null ? "" : " Last log line: " + last.Trim();
                }
            }
            catch (IOException)
            {
                // The log is still open by Gradle; skip it.
            }
            Problem = why + tail + " (log: " + LogPath + ")";
            State = LaunchState.Failed;
        }

        /// <summary>Runs a short Windows command hidden and returns its output ("" if it fails).</summary>
        private static string RunQuietly(string file, string arguments)
        {
            try
            {
                var info = new ProcessStartInfo(file, arguments)
                {
                    UseShellExecute = false, CreateNoWindow = true, RedirectStandardOutput = true,
                };
                using (Process process = Process.Start(info))
                {
                    string output = process.StandardOutput.ReadToEnd();
                    process.WaitForExit(5000);
                    return output;
                }
            }
            catch (Exception e) when (e is System.ComponentModel.Win32Exception || e is InvalidOperationException)
            {
                return "";
            }
        }
    }
}

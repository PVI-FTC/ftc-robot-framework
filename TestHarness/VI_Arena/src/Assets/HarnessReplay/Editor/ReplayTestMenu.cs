using System.IO;
using System.Text;
using UnityEditor;
using UnityEditor.TestTools.TestRunner.Api;
using UnityEngine;

namespace PviHarness.Replay.Editor
{
    /// <summary>
    /// <b>Harness → Run Replay Tests</b> in the Unity menu: runs the HarnessReplay tests and writes
    /// a plain-text summary to Harness/build/unity-test-results.txt (one line per test), so the
    /// result can be read outside Unity too. The Test Runner window (Window → General → Test
    /// Runner) runs the same tests.
    ///
    /// <para>Also usable from the command line with the editor closed:
    /// <c>Unity.exe -batchmode -projectPath unity -executeMethod PviHarness.Replay.Editor.ReplayTestMenu.RunFromCommandLine</c></para>
    /// </summary>
    public static class ReplayTestMenu
    {
        /// <summary>Where the summary is written.</summary>
        public static string ResultsPath =>
            Path.GetFullPath(Path.Combine(Application.dataPath, "..", "..", "build", "unity-test-results.txt"));

        [MenuItem("Harness/Run Replay Tests")]
        public static void Run()
        {
            Start(exitWhenDone: false);
        }

        /// <summary>For -executeMethod in batch mode: runs the tests, then quits with 0 (all passed) or 1.</summary>
        public static void RunFromCommandLine()
        {
            Start(exitWhenDone: true);
        }

        private static void Start(bool exitWhenDone)
        {
            if (File.Exists(ResultsPath))
            {
                File.Delete(ResultsPath);
            }
            var api = ScriptableObject.CreateInstance<TestRunnerApi>();
            api.RegisterCallbacks(new Report(exitWhenDone));
            api.Execute(new ExecutionSettings(new Filter
            {
                testMode = TestMode.EditMode,
                assemblyNames = new[] { "HarnessReplay.Tests" },
            }));
            Debug.Log("[HarnessReplay] Running replay tests; summary will be written to " + ResultsPath);
        }

        /// <summary>Collects the results and writes the summary when the run finishes.</summary>
        private sealed class Report : ICallbacks
        {
            private readonly bool exitWhenDone;

            public Report(bool exitWhenDone)
            {
                this.exitWhenDone = exitWhenDone;
            }

            public void RunStarted(ITestAdaptor testsToRun)
            {
            }

            public void TestStarted(ITestAdaptor test)
            {
            }

            public void TestFinished(ITestResultAdaptor result)
            {
            }

            public void RunFinished(ITestResultAdaptor result)
            {
                var text = new StringBuilder();
                text.AppendLine("Overall: " + result.TestStatus + "  passed=" + result.PassCount
                    + " failed=" + result.FailCount + " skipped=" + result.SkipCount);
                AppendLeaves(result, text);
                Directory.CreateDirectory(Path.GetDirectoryName(ResultsPath));
                File.WriteAllText(ResultsPath, text.ToString());
                Debug.Log("[HarnessReplay] Replay tests: " + result.TestStatus + " (" + result.PassCount + " passed, "
                    + result.FailCount + " failed, " + result.SkipCount + " skipped). Summary: " + ResultsPath);
                if (exitWhenDone)
                {
                    EditorApplication.Exit(result.FailCount == 0 ? 0 : 1);
                }
            }

            private static void AppendLeaves(ITestResultAdaptor result, StringBuilder text)
            {
                if (!result.HasChildren)
                {
                    text.AppendLine(result.TestStatus + "  " + result.Test.FullName
                        + (string.IsNullOrEmpty(result.Message) ? "" : "  -- " + result.Message.Trim()));
                    return;
                }
                foreach (ITestResultAdaptor child in result.Children)
                {
                    AppendLeaves(child, text);
                }
            }
        }
    }
}

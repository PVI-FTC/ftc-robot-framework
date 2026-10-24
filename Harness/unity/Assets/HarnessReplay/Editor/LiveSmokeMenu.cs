using System;
using System.IO;
using System.Text;
using UnityEditor;
using UnityEngine;

namespace PviHarness.Replay.Editor
{
    /// <summary>
    /// <b>Harness → Live Smoke Test</b>: checks the real live connection end to end. With
    /// <c>.\gradlew.bat unityBridge</c> running, it connects, loads Team A TeleOp (the harness reads
    /// the controllers), presses INIT and PLAY, runs one second of steps, stops, and writes what
    /// happened to Harness/build/unity-live-smoke.txt. No controller is needed (the robot then just
    /// sits still).
    ///
    /// <para>From the command line, with the editor closed:
    /// <c>Unity.exe -batchmode -projectPath unity -executeMethod PviHarness.Replay.Editor.LiveSmokeMenu.RunFromCommandLine</c></para>
    /// </summary>
    public static class LiveSmokeMenu
    {
        /// <summary>Where the result is written.</summary>
        public static string ResultPath =>
            Path.GetFullPath(Path.Combine(Application.dataPath, "..", "..", "build", "unity-live-smoke.txt"));

        [MenuItem("Harness/Live Smoke Test")]
        public static void Run()
        {
            bool ok = RunSmoke(out string report);
            File.WriteAllText(ResultPath, report);
            Debug.Log("[HarnessReplay] Live smoke test " + (ok ? "passed" : "FAILED") + ":\n" + report);
        }

        /// <summary>For -executeMethod in batch mode: runs the check, then quits with 0 (passed) or 1.</summary>
        public static void RunFromCommandLine()
        {
            bool ok = RunSmoke(out string report);
            Directory.CreateDirectory(Path.GetDirectoryName(ResultPath));
            File.WriteAllText(ResultPath, report);
            EditorApplication.Exit(ok ? 0 : 1);
        }

        private static bool RunSmoke(out string report)
        {
            var text = new StringBuilder();
            try
            {
                using (var live = new LiveSession(new TcpBridgeTransport()))
                {
                    live.Hello();
                    text.AppendLine("Connected; OpModes offered: " + live.OpModes.Count);
                    live.Load("teamA-teleop");
                    live.Init();
                    for (int i = 0; i < 5; i++)
                    {
                        live.Step();
                    }
                    live.Play();
                    for (int i = 0; i < 50; i++)
                    {
                        live.Step();
                    }
                    LoopData last = live.Recording.loops[live.Recording.loops.Count - 1];
                    text.AppendLine("After PLAY + 50 steps: stage " + last.stage + ", match time " + last.matchTimeSeconds
                        + " s, loops " + live.Recording.loops.Count);
                    text.AppendLine("Controllers: " + string.Join(", ", live.Controllers)
                        + (live.ControllerProblem == null ? "" : " (problem: " + live.ControllerProblem + ")"));
                    live.Stop();
                    text.AppendLine("Stopped: finished = " + live.Finished);
                    bool ok = live.Finished && last.stage == "LOOP" && Math.Abs(last.matchTimeSeconds - 1.0) < 1e-6
                        && live.Recording.loops.Count == 1 + 5 + 1 + 50 + 1;
                    text.AppendLine(ok ? "Harness physics: passed" : "Harness physics: FAILED (unexpected stage, time or loop count)");

                    // Second run: Unity's physics moves the robot (Stage U3). Each step sends where it
                    // put the robot; the harness's pose must then be Unity's.
                    live.Load("teamA-teleop", unityPhysics: true);
                    using (var physics = new UnityRobotPhysics(live.RobotPhysics, null))
                    {
                        live.Init();
                        live.Play(physics.Step(live.LastDriveCommand, live.LoopPeriodSeconds));
                        PhysicsStateData state = null;
                        for (int i = 0; i < 50; i++)
                        {
                            state = physics.Step(live.LastDriveCommand, live.LoopPeriodSeconds);
                            live.Step(state);
                        }
                        PoseData pose = live.Recording.loops[live.Recording.loops.Count - 1].pose;
                        bool same = live.UnityPhysics && pose != null && Math.Abs(pose.xInches - state.xInches) < 1e-3
                            && Math.Abs(pose.yInches - state.yInches) < 1e-3;
                        text.AppendLine("Unity physics: physics = " + (live.UnityPhysics ? "unity" : "harness") + ", harness pose ("
                            + pose?.xInches + ", " + pose?.yInches + ") vs Unity (" + state.xInches + ", " + state.yInches + "): "
                            + (same ? "passed" : "FAILED"));
                        live.Stop();
                        ok &= same;
                    }
                    text.AppendLine(ok ? "PASSED" : "FAILED");
                    report = text.ToString();
                    return ok;
                }
            }
            catch (Exception e)
            {
                text.AppendLine("FAILED: " + e.GetType().Name + ": " + e.Message);
                report = text.ToString();
                return false;
            }
        }
    }
}

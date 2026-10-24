using System.Collections.Generic;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;
using NUnit.Framework;

namespace PviHarness.Replay.Tests
{
    /// <summary>
    /// Live play's Unity side (Stage U2 piece 3), against a fake harness that answers like
    /// <c>gradlew unityBridge</c> (message format v3), using the sample recording's loops as frames.
    /// </summary>
    public class LiveSessionTests
    {
        /// <summary>Answers each request the way the harness bridge does, and remembers what it was sent.</summary>
        private sealed class FakeHarness : IBridgeTransport
        {
            public readonly List<JObject> Received = new List<JObject>();
            private readonly RunRecordingData sample = RecordingLoader.Load(RecordingLoaderTests.SamplePath);
            private int next;
            public string NextError;
            public bool NextCrash;
            public bool FinishAfterNextFrame;
            public bool Disposed;

            public string Request(string line)
            {
                JObject message = JObject.Parse(line);
                Received.Add(message);
                if (NextError != null)
                {
                    var error = new JObject { ["type"] = "error", ["message"] = NextError };
                    if (NextCrash)
                    {
                        error["crashed"] = true;
                    }
                    NextError = null;
                    return error.ToString(Formatting.None);
                }
                switch ((string)message["type"])
                {
                    case "hello":
                        return new JObject
                        {
                            ["type"] = "welcome", ["version"] = 3, ["loopPeriodSeconds"] = 0.02,
                            ["opModes"] = new JArray(new JObject { ["id"] = "teamA-teleop", ["title"] = "Team A TeleOp", ["period"] = "TELEOP" }),
                        }.ToString(Formatting.None);
                    case "load":
                        return new JObject
                        {
                            ["type"] = "loaded", ["opMode"] = message["opMode"], ["input"] = message["input"], ["matchSeconds"] = 120,
                            ["physics"] = message["physics"],
                            ["robotPhysics"] = new JObject
                            {
                                ["forwardInchesPerSecond"] = 62.6, ["strafeInchesPerSecond"] = 50, ["turnRadiansPerSecond"] = 3.14,
                                ["lengthInches"] = 18, ["widthInches"] = 18, ["responseTimeSeconds"] = 0.1,
                                ["maxAccelerationInchesPerSecondSquared"] = 200, ["forwardCoastInchesPerSecondSquared"] = 40,
                                ["lateralCoastInchesPerSecondSquared"] = 60,
                                ["start"] = new JObject { ["xInches"] = 0, ["yInches"] = 0, ["headingRadians"] = 0 },
                            },
                            ["header"] = JObject.FromObject(sample.header),
                        }.ToString(Formatting.None);
                    case "save":
                        return new JObject { ["type"] = "saved", ["path"] = "C:/x/" + message["name"] + ".json" }.ToString(Formatting.None);
                    case "bye":
                        return "{\"type\": \"goodbye\"}";
                    default:
                        // init, step, play, stop: the next sample loop, with a rumble on the 3rd frame.
                        var frame = new JObject
                        {
                            ["type"] = "frame",
                            ["loop"] = JObject.FromObject(sample.loops[next]),
                            ["gamepad1Rumbles"] = next == 2 ? new JArray(new JObject { ["matchTimeSeconds"] = 0, ["totalDurationMs"] = 500, ["pulses"] = 1 }) : new JArray(),
                            ["gamepad2Rumbles"] = new JArray(),
                            ["events"] = new JArray(),
                            ["finished"] = (string)message["type"] == "stop" || FinishAfterNextFrame,
                            ["controllers"] = new JArray("PS5 Controller", "none"),
                            ["controllerProblem"] = null,
                            ["driveCommand"] = new JObject
                            {
                                ["frontLeft"] = 0.5, ["frontRight"] = 0.5, ["rearLeft"] = 0.5, ["rearRight"] = 0.5, ["braking"] = false,
                            },
                        };
                        next++;
                        return frame.ToString(Formatting.None);
                }
            }

            public void Dispose()
            {
                Disposed = true;
            }
        }

        private FakeHarness harness;
        private LiveSession session;

        /// <summary>
        /// WHAT: With Unity's physics, load says so and reads the robot's numbers; each frame's
        /// driveCommand is kept; step and play carry the physicsState Unity passes in.
        /// WHY: This is the Unity side of Stage U3's message exchange; a missing field would stop the run.
        /// </summary>
        [Test]
        public void UnityPhysicsSendsStatesAndReadsDriveCommands()
        {
            session.Hello();
            session.Load("teamA-teleop", unityPhysics: true);
            session.Init();
            session.Play(new PhysicsStateData());
            session.Step(new PhysicsStateData { xInches = 12.5, wallContact = "+x" });

            Assert.AreEqual("unity", (string)harness.Received[1]["physics"]);
            Assert.IsTrue(session.UnityPhysics);
            Assert.AreEqual(62.6, session.RobotPhysics.forwardInchesPerSecond, 1e-9);
            Assert.IsTrue(session.RobotPhysics.HasDynamics);
            Assert.AreEqual(0.02, session.LoopPeriodSeconds, 1e-9);
            Assert.IsNull(harness.Received[2]["physicsState"], "init carries no state");
            Assert.AreEqual(12.5, (double)harness.Received[4]["physicsState"]["xInches"], 1e-9);
            Assert.AreEqual("+x", (string)harness.Received[4]["physicsState"]["wallContact"]);
            Assert.AreEqual(0.5, session.LastDriveCommand.frontLeft, 1e-9);
            Assert.IsFalse(session.LastDriveCommand.braking);
        }

        [SetUp]
        public void Connect()
        {
            harness = new FakeHarness();
            session = new LiveSession(harness);
        }

        /// <summary>
        /// WHAT: Hello speaks version 3 and reads the OpModes; load asks the harness to read the controllers
        /// and, unless asked otherwise, to use its own physics.
        /// WHY: The user chose "the harness reads the controllers"; a wrong version would silently misread frames.
        /// </summary>
        [Test]
        public void HelloAndLoadUseVersionThreeAndControllers()
        {
            session.Hello();
            session.Load("teamA-teleop");

            Assert.AreEqual(3, (int)harness.Received[0]["version"]);
            Assert.AreEqual("harness", (string)harness.Received[1]["physics"]);
            Assert.IsFalse(session.UnityPhysics);
            Assert.AreEqual("teamA-teleop", session.OpModes[0].id);
            Assert.AreEqual("controllers", (string)harness.Received[1]["input"]);
            Assert.AreEqual("TeamATeleOp", session.Recording.header.opMode);
            Assert.AreEqual(0, session.Recording.loops.Count, "a fresh run starts empty");
        }

        /// <summary>
        /// WHAT: Each frame adds one loop to the recording, and INIT / PLAY / STOP move the state along.
        /// HOW: init, 3 steps, play, 2 steps, stop: 8 loops; Initialized before PLAY, Playing after, Finished at STOP.
        /// WHY: The replay screen draws the live run from this growing recording.
        /// </summary>
        [Test]
        public void FramesGrowTheRecordingAndButtonsMoveTheState()
        {
            session.Hello();
            session.Load("teamA-teleop");

            session.Init();
            Assert.IsTrue(session.Initialized);
            for (int i = 0; i < 3; i++)
            {
                session.Step();
            }
            session.Play();
            Assert.IsTrue(session.Playing);
            Assert.IsFalse(session.Initialized);
            session.Step();
            session.Step();
            session.Stop();

            Assert.AreEqual(8, session.Recording.loops.Count);
            Assert.AreEqual("INIT", session.Recording.loops[0].stage);
            Assert.IsTrue(session.Finished);
            Assert.IsFalse(session.Playing);
            CollectionAssert.AreEqual(new[] { "init", "step", "step", "step", "play", "step", "step", "stop" },
                harness.Received.GetRange(2, 8).ConvertAll(m => (string)m["type"]));
        }

        /// <summary>WHAT: Rumbles are added once each, and each frame updates which controller drives each gamepad.</summary>
        [Test]
        public void RumblesAddedOnceAndControllersShown()
        {
            session.Hello();
            session.Load("teamA-teleop");
            session.Init();
            for (int i = 0; i < 5; i++)
            {
                session.Step();
            }

            Assert.AreEqual(1, session.Recording.gamepad1Rumbles.Count);
            CollectionAssert.AreEqual(new[] { "PS5 Controller", "none" }, session.Controllers);
            Assert.IsNull(session.ControllerProblem);
        }

        /// <summary>
        /// WHAT: An "error" reply becomes an exception with the harness's message; a crash also ends the run.
        /// WHY: The screen shows the reason instead of freezing; a crashed OpMode can't keep running.
        /// </summary>
        [Test]
        public void ErrorsAreRaisedAndACrashEndsTheRun()
        {
            session.Hello();
            session.Load("teamA-teleop");
            session.Init();
            session.Play();

            harness.NextError = "PLAY was already pressed.";
            var refused = Assert.Throws<BridgeErrorException>(() => session.Play());
            Assert.AreEqual("PLAY was already pressed.", refused.Message);
            Assert.IsTrue(session.Playing, "a refused request doesn't end the run");

            harness.NextError = "TeamATeleOp crashed during LOOP: boom";
            harness.NextCrash = true;
            var crash = Assert.Throws<BridgeErrorException>(() => session.Step());
            Assert.IsTrue(crash.Crashed);
            Assert.IsTrue(session.Finished);
            Assert.IsFalse(session.Playing);
        }

        /// <summary>WHAT: A frame marked finished (end of match) stops the run; Save returns the path; Dispose says bye.</summary>
        [Test]
        public void EndOfMatchSaveAndGoodbye()
        {
            session.Hello();
            session.Load("teamA-teleop");
            session.Init();
            session.Play();
            harness.FinishAfterNextFrame = true;

            session.Step();
            string path = session.Save("unity-live-test");
            session.Dispose();

            Assert.IsTrue(session.Finished);
            Assert.AreEqual("C:/x/unity-live-test.json", path);
            Assert.AreEqual("bye", (string)harness.Received[harness.Received.Count - 1]["type"]);
            Assert.IsTrue(harness.Disposed);
        }

        /// <summary>
        /// WHAT: The pacer asks for one step per 20 ms of real time, and never more than 5 at once.
        /// WHY: Real-time driving; a slow frame slows the run down instead of racing to catch up.
        /// </summary>
        [Test]
        public void PacerGivesOneStepPerTwentyMilliseconds()
        {
            var pacer = new LivePacer();

            Assert.AreEqual(2, pacer.StepsDue(0.050), "50 ms = 2 steps, 10 ms owed");
            Assert.AreEqual(1, pacer.StepsDue(0.010), "the owed 10 ms + 10 ms = 1 step");
            Assert.AreEqual(0, pacer.StepsDue(0.019));
            Assert.AreEqual(LivePacer.MaxStepsPerFrame, pacer.StepsDue(1.0), "a 1 s hiccup gives at most 5");
            Assert.AreEqual(0, pacer.StepsDue(0.0), "and the backlog is dropped");
        }

        /// <summary>WHAT: The timeline picks up loops added to its recording (live play) and can show the newest.</summary>
        [Test]
        public void TimelineFollowsAGrowingRecording()
        {
            session.Hello();
            session.Load("teamA-teleop");
            session.Init();
            var timeline = new ReplayTimeline(session.Recording);
            Assert.AreEqual(1, timeline.Count);

            session.Step();
            session.Step();
            timeline.Sync();
            timeline.Seek(timeline.Count - 1);

            Assert.AreEqual(3, timeline.Count);
            Assert.AreEqual(2, timeline.Index);
        }

        /// <summary>WHAT: The live status line says what to press and which controller drives each gamepad.</summary>
        [Test]
        public void LiveStatusSaysWhatToPress()
        {
            session.Hello();
            session.Load("teamA-teleop");
            session.Init();

            Assert.AreEqual("INIT: press Enter for PLAY (Esc stops)  ·  Gamepad 1: PS5 Controller  ·  Gamepad 2: none",
                HudText.LiveStatus(session, false));
            session.Play();
            StringAssert.StartsWith("Driving: Esc for STOP", HudText.LiveStatus(session, false));
            session.Stop();
            StringAssert.StartsWith("Stopped: Save keeps this run", HudText.LiveStatus(session, false));
            StringAssert.StartsWith("Stopped and saved", HudText.LiveStatus(session, true));
        }
    }
}

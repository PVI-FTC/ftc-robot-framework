using System;
using System.Collections.Generic;
using System.IO;
using System.Net.Sockets;
using System.Text;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace VI_Arena
{
    /// <summary>Something that sends one message line to the harness and returns its one reply line.</summary>
    public interface IBridgeTransport : IDisposable
    {
        /// <summary>Sends <paramref name="line"/> (no "\n") and waits for the reply line.</summary>
        string Request(string line);
    }

    /// <summary>
    /// The real connection: TCP to the harness's <c>gradlew unityBridge</c> on this computer.
    /// Replies come back within a few milliseconds (it is local, and one OpMode call is quick), so
    /// it simply waits for each one: no background threads to reason about.
    /// </summary>
    public sealed class TcpBridgeTransport : IBridgeTransport
    {
        /// <summary>The harness's default port (BridgeProtocol.DEFAULT_PORT in Java).</summary>
        public const int DefaultPort = 7465;

        private readonly TcpClient client;
        private readonly StreamReader reader;
        private readonly StreamWriter writer;

        /// <summary>Connects to 127.0.0.1, giving up after <paramref name="timeoutMs"/> if nothing is listening.</summary>
        public TcpBridgeTransport(int port = DefaultPort, int timeoutMs = 500)
        {
            client = new TcpClient { NoDelay = true, ReceiveTimeout = 5000, SendTimeout = 5000 };
            IAsyncResult attempt = client.BeginConnect("127.0.0.1", port, null, null);
            if (!attempt.AsyncWaitHandle.WaitOne(timeoutMs) || !client.Connected)
            {
                client.Close();
                throw new IOException("Nothing is listening on 127.0.0.1:" + port
                    + ". Start the harness side first: .\\gradlew.bat unityBridge (in TestHarness\\th_src).");
            }
            client.EndConnect(attempt);
            var utf8 = new UTF8Encoding(false);
            reader = new StreamReader(client.GetStream(), utf8);
            writer = new StreamWriter(client.GetStream(), utf8) { NewLine = "\n", AutoFlush = true };
        }

        public string Request(string line)
        {
            writer.WriteLine(line);
            string reply = reader.ReadLine();
            if (reply == null)
            {
                throw new IOException("The harness closed the connection.");
            }
            return reply;
        }

        public void Dispose()
        {
            client.Close();
        }
    }

    /// <summary>Thrown when the harness answers with an "error" message.</summary>
    public sealed class BridgeErrorException : Exception
    {
        /// <summary>True if the OpMode crashed (the run is over).</summary>
        public bool Crashed { get; }

        public BridgeErrorException(string message, bool crashed) : base(message)
        {
            Crashed = crashed;
        }
    }

    /// <summary>
    /// A live run over the harness bridge (Stages U2 and U3, message format v3; see TestHarness/docs/README.md,
    /// "Live connection to Unity"). Each call sends one message and waits for its reply. The run's
    /// <see cref="Recording"/> grows by one loop per frame, in exactly the saved-recording layout,
    /// so the replay screen shows it with no changes.
    ///
    /// <para>The harness reads the game controllers (<c>"input": "controllers"</c>, the user's
    /// choice); Unity only asks for each step and draws. Unity drives the clock: nothing moves
    /// unless <see cref="Step"/> is called, about every 20 ms (<see cref="LivePacer"/>).</para>
    ///
    /// <para>With <c>"physics": "unity"</c> (Stage U3) Unity's physics moves the robot: each frame
    /// brings the wheels' <see cref="LastDriveCommand"/>; Unity runs its physics one loop with it
    /// (<see cref="UnityRobotPhysics"/>) and sends the result with the next step or play.</para>
    /// </summary>
    public sealed class LiveSession : IDisposable
    {
        /// <summary>The message format this client speaks.</summary>
        public const int ProtocolVersion = 5;

        /// <summary>One OpMode the harness offers.</summary>
        public sealed class OpModeChoice
        {
            public string id;
            public string title;
            public string period;
        }

        private readonly IBridgeTransport transport;

        /// <summary>What the harness offers, from its "welcome".</summary>
        public List<OpModeChoice> OpModes { get; private set; } = new List<OpModeChoice>();
        /// <summary>The run so far (header from "loaded", one loop per frame), or null before <see cref="Load"/>.</summary>
        public RunRecordingData Recording { get; private set; }
        /// <summary>The loaded OpMode's id, e.g. "teamA-teleop".</summary>
        public string OpModeId { get; private set; }
        /// <summary>What drives each gamepad, e.g. ["PS5 Controller", "none"]; empty if the harness didn't say.</summary>
        public List<string> Controllers { get; private set; } = new List<string>();
        /// <summary>Why the controllers don't work (e.g. the library couldn't start), or null.</summary>
        public string ControllerProblem { get; private set; }
        /// <summary>True once the run has stopped (STOP, end of match, or the OpMode stopped itself).</summary>
        public bool Finished { get; private set; }
        /// <summary>True after INIT, before PLAY.</summary>
        public bool Initialized { get; private set; }
        /// <summary>True after PLAY, until finished.</summary>
        public bool Playing { get; private set; }
        /// <summary>True if the run was loaded with Unity's physics moving the robot.</summary>
        public bool UnityPhysics { get; private set; }
        /// <summary>The robot's speeds, size and start for Unity's physics (from "loaded"), or null.</summary>
        public RobotPhysicsData RobotPhysics { get; private set; }
        /// <summary>What the wheels were told in the newest frame (all off before the first).</summary>
        public DriveCommandData LastDriveCommand { get; private set; } = DriveCommandData.Stopped;
        /// <summary>Seconds per OpMode call, from "welcome" (0.02 unless the harness says otherwise).</summary>
        public double LoopPeriodSeconds { get; private set; } = 0.02;

        /// <summary>Starts a session on a transport (the TCP connection, or a fake one in tests).</summary>
        public LiveSession(IBridgeTransport transport)
        {
            this.transport = transport ?? throw new ArgumentNullException(nameof(transport));
        }

        /// <summary>Says hello and reads the OpModes on offer.</summary>
        public void Hello()
        {
            JObject welcome = Send(new JObject { ["type"] = "hello", ["version"] = ProtocolVersion, ["client"] = "Unity" }, "welcome");
            OpModes = welcome["opModes"]?.ToObject<List<OpModeChoice>>() ?? new List<OpModeChoice>();
            LoopPeriodSeconds = welcome["loopPeriodSeconds"]?.Value<double>() ?? 0.02;
        }

        /// <summary>
        /// Loads an OpMode with the harness reading the controllers; starts a new, empty recording.
        /// With <paramref name="unityPhysics"/>, Unity's physics moves the robot (Stage U3).
        /// </summary>
        public void Load(string opModeId, bool unityPhysics = false)
        {
            JObject loaded = Send(new JObject
            {
                ["type"] = "load", ["opMode"] = opModeId, ["input"] = "controllers",
                ["physics"] = unityPhysics ? "unity" : "harness",
            }, "loaded");
            OpModeId = opModeId;
            UnityPhysics = (string)loaded["physics"] == "unity";
            RobotPhysics = loaded["robotPhysics"]?.Type == JTokenType.Object ? loaded["robotPhysics"].ToObject<RobotPhysicsData>() : null;
            LastDriveCommand = DriveCommandData.Stopped;
            Recording = new RunRecordingData { header = loaded["header"].ToObject<HeaderData>() };
            Finished = false;
            Initialized = false;
            Playing = false;
        }

        /// <summary>Presses INIT.</summary>
        public void Init()
        {
            Frame(new JObject { ["type"] = "init" });
            Initialized = !Finished;
        }

        /// <summary>
        /// One OpMode call: init_loop before PLAY, loop after. With Unity's physics, pass where it
        /// put the robot after running <see cref="LastDriveCommand"/> for one loop.
        /// </summary>
        public void Step(PhysicsStateData physicsState = null)
        {
            Frame(WithPhysics(new JObject { ["type"] = "step" }, physicsState));
        }

        /// <summary>Presses PLAY (with Unity's physics, pass its newest state as for <see cref="Step"/>).</summary>
        public void Play(PhysicsStateData physicsState = null)
        {
            Frame(WithPhysics(new JObject { ["type"] = "play" }, physicsState));
            Initialized = false;
            Playing = !Finished;
        }

        /// <summary>Presses STOP.</summary>
        public void Stop()
        {
            Frame(new JObject { ["type"] = "stop" });
        }

        /// <summary>Saves the run in TestHarness/th_src/build/recordings and returns the file's path.</summary>
        public string Save(string name)
        {
            return (string)Send(new JObject { ["type"] = "save", ["name"] = name }, "saved")["path"];
        }

        /// <summary>Says goodbye (best effort) and closes the connection.</summary>
        public void Dispose()
        {
            try
            {
                transport.Request(new JObject { ["type"] = "bye" }.ToString(Formatting.None));
            }
            catch (Exception)
            {
                // Already gone: nothing to say goodbye to.
            }
            transport.Dispose();
        }

        /// <summary>Sends a frame request and adds the frame's loop, rumbles and events to the recording.</summary>
        private void Frame(JObject request)
        {
            if (Recording == null)
            {
                throw new InvalidOperationException("Load an OpMode first.");
            }
            JObject frame = Send(request, "frame");
            Recording.loops.Add(frame["loop"].ToObject<LoopData>());
            AddAll(Recording.gamepad1Rumbles, frame["gamepad1Rumbles"]);
            AddAll(Recording.gamepad2Rumbles, frame["gamepad2Rumbles"]);
            AddAll(Recording.events, frame["events"]);
            Controllers = frame["controllers"] is JArray names ? names.ToObject<List<string>>() : new List<string>();
            ControllerProblem = frame["controllerProblem"]?.Type == JTokenType.String ? (string)frame["controllerProblem"] : null;
            Finished = frame["finished"]?.Value<bool>() ?? false;
            LastDriveCommand = frame["driveCommand"]?.Type == JTokenType.Object
                ? frame["driveCommand"].ToObject<DriveCommandData>() : DriveCommandData.Stopped;
            if (Finished)
            {
                Playing = false;
                Initialized = false;
            }
        }

        private static JObject WithPhysics(JObject request, PhysicsStateData physicsState)
        {
            if (physicsState != null)
            {
                request["physicsState"] = JObject.FromObject(physicsState);
            }
            return request;
        }

        private static void AddAll<T>(List<T> into, JToken items)
        {
            if (items is JArray array)
            {
                into.AddRange(array.ToObject<List<T>>());
            }
        }

        /// <summary>Sends one message and returns the reply, which must be of type <paramref name="expected"/>.</summary>
        private JObject Send(JObject message, string expected)
        {
            JObject reply = JObject.Parse(transport.Request(message.ToString(Formatting.None)));
            string type = (string)reply["type"];
            if (type == "error")
            {
                bool crashed = reply["crashed"]?.Value<bool>() ?? false;
                if (crashed)
                {
                    Finished = true;
                    Playing = false;
                }
                throw new BridgeErrorException((string)reply["message"], crashed);
            }
            if (type != expected)
            {
                throw new BridgeErrorException("Expected a \"" + expected + "\" reply from the harness, got \"" + type + "\".", false);
            }
            return reply;
        }
    }

    /// <summary>
    /// Real-time pacing for live play: how many 20 ms steps to ask for after some real time has
    /// passed. Never more than <see cref="MaxStepsPerFrame"/> at once, so a hiccup (window dragged,
    /// a slow frame) slows the run down instead of fast-forwarding it.
    /// </summary>
    public sealed class LivePacer
    {
        /// <summary>The most steps asked for in one frame.</summary>
        public const int MaxStepsPerFrame = 5;

        private readonly double stepSeconds;
        private double owed;

        public LivePacer(double stepSeconds = 0.02)
        {
            this.stepSeconds = stepSeconds;
        }

        /// <summary>Adds <paramref name="realSeconds"/> and returns how many steps are due now.</summary>
        public int StepsDue(double realSeconds)
        {
            owed += Math.Max(0, realSeconds);
            int steps = (int)Math.Floor(owed / stepSeconds + 1e-9);
            if (steps > MaxStepsPerFrame)
            {
                owed = 0; // drop the backlog: running late is better than racing ahead
                return MaxStepsPerFrame;
            }
            owed -= steps * stepSeconds;
            return steps;
        }

        /// <summary>Forgets any time owed (e.g. after a pause).</summary>
        public void Reset()
        {
            owed = 0;
        }
    }
}

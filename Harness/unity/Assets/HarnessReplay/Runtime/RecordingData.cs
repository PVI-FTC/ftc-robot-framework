using System.Collections.Generic;

namespace PviHarness.Replay
{
    // These classes mirror the harness's run recording JSON, format version 2 (described field by
    // field in Harness/README.md, "Recording a whole run (JSON)"). Field names are written exactly
    // as in the JSON (camelCase), so anyone can match a C# field to the file at a glance.
    //
    // Units are the harness's FTC units: inches, radians, seconds. Nothing here converts to Unity
    // units; FtcToUnity does that, in one place.

    /// <summary>A whole recorded run: what was run, and one entry per OpMode call.</summary>
    public sealed class RunRecordingData
    {
        public HeaderData header;
        public List<RumbleData> gamepad1Rumbles = new List<RumbleData>();
        public List<RumbleData> gamepad2Rumbles = new List<RumbleData>();
        /// <summary>Marked moments (version 2). Version 1 files have none.</summary>
        public List<EventData> events = new List<EventData>();
        public List<LoopData> loops = new List<LoopData>();
    }

    /// <summary>What was run, on which robot, and the format version.</summary>
    public sealed class HeaderData
    {
        public int version;
        public string distanceUnit;
        public string angleUnit;
        public string timeUnit;
        public string robotConfig;
        public string opMode;
        /// <summary>"AUTONOMOUS", "TELEOP", or null.</summary>
        public string period;
        public double loopPeriodSeconds;
        /// <summary>Null if position wasn't tracked.</summary>
        public PoseData startPose;
        /// <summary>Null if position wasn't tracked.</summary>
        public DriveModelData driveModel;
    }

    /// <summary>A robot position on the FTC field: inches from the center, heading counter-clockwise from +x.</summary>
    public sealed class PoseData
    {
        public double xInches;
        public double yInches;
        public double headingRadians;
    }

    /// <summary>The robot's speeds and size, each with its unit and whether it was measured.</summary>
    public sealed class DriveModelData
    {
        public string name;
        public ModelValueData forward;
        public ModelValueData strafe;
        public ModelValueData turn;
        public ModelValueData length;
        public ModelValueData width;
    }

    /// <summary>One robot fact: the number, its unit, where it came from, and if it was measured.</summary>
    public sealed class ModelValueData
    {
        public double value;
        public string unit;
        public bool measured;
        public string source;
    }

    /// <summary>The robot right after one OpMode call.</summary>
    public sealed class LoopData
    {
        public double sinceInitSeconds;
        public double matchTimeSeconds;
        /// <summary>"INIT", "INIT_LOOP", "START", "LOOP" or "STOP".</summary>
        public string stage;
        /// <summary>Null if position wasn't tracked.</summary>
        public PoseData pose;
        public List<MotorData> motors = new List<MotorData>();
        public List<ServoData> servos = new List<ServoData>();
        /// <summary>The "Auto Step" telemetry value sent during this call, or null.</summary>
        public string autoStep;
        public List<TelemetryScreenData> telemetry = new List<TelemetryScreenData>();
        public GamepadData gamepad1;
        public GamepadData gamepad2;
    }

    /// <summary>One motor's power, -1 to 1.</summary>
    public sealed class MotorData
    {
        public string name;
        public double power;
    }

    /// <summary>One servo's position, 0 to 1, or null if the code never set one.</summary>
    public sealed class ServoData
    {
        public string name;
        public double? position;
    }

    /// <summary>One Driver Station screen, as sent by one telemetry.update().</summary>
    public sealed class TelemetryScreenData
    {
        public List<string> lines = new List<string>();
        public List<string> log = new List<string>();
    }

    /// <summary>A gamepad's held buttons, sticks (up is negative) and triggers.</summary>
    public sealed class GamepadData
    {
        public List<string> buttons = new List<string>();
        public double leftStickX;
        public double leftStickY;
        public double rightStickX;
        public double rightStickY;
        public double leftTrigger;
        public double rightTrigger;
    }

    /// <summary>One rumble command sent to a gamepad.</summary>
    public sealed class RumbleData
    {
        public double matchTimeSeconds;
        public int totalDurationMs;
        public int pulses;
    }

    /// <summary>A marked moment, e.g. a controller disconnecting during a live drive.</summary>
    public sealed class EventData
    {
        public double sinceInitSeconds;
        public double matchTimeSeconds;
        public string text;
    }
}

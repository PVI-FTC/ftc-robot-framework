using System;

namespace PviHarness.Replay
{
    /// <summary>What the harness says about the robot for Unity's physics (the "robotPhysics" in "loaded").</summary>
    public sealed class RobotPhysicsData
    {
        public double forwardInchesPerSecond;
        public double strafeInchesPerSecond;
        public double turnRadiansPerSecond;
        public double lengthInches;
        public double widthInches;
        /// <summary>Null for a robot without momentum (it changes speed instantly).</summary>
        public double? responseTimeSeconds;
        public double? maxAccelerationInchesPerSecondSquared;
        public double? forwardCoastInchesPerSecondSquared;
        public double? lateralCoastInchesPerSecondSquared;
        public PoseData start;

        /// <summary>True if the robot has momentum numbers (response time, traction, coasting).</summary>
        public bool HasDynamics => responseTimeSeconds.HasValue && maxAccelerationInchesPerSecondSquared.HasValue
            && forwardCoastInchesPerSecondSquared.HasValue && lateralCoastInchesPerSecondSquared.HasValue;
    }

    /// <summary>What the wheels do this loop (each frame's "driveCommand"): pushes -1 to 1 after motor direction.</summary>
    public sealed class DriveCommandData
    {
        public double frontLeft;
        public double frontRight;
        public double rearLeft;
        public double rearRight;
        /// <summary>True if the drive motors brake at zero power; false means they coast (FLOAT).</summary>
        public bool braking = true;

        /// <summary>All wheels off, braking.</summary>
        public static DriveCommandData Stopped => new DriveCommandData();
    }

    /// <summary>What Unity's physics sends back with each step ("physicsState"), in FTC units.</summary>
    public sealed class PhysicsStateData
    {
        public double xInches;
        public double yInches;
        public double headingRadians;
        public double forwardInchesPerSecond;
        public double leftInchesPerSecond;
        public double counterClockwiseRadiansPerSecond;
        /// <summary>"+x", "-x", "+y", "-y", two of them in a corner ("+x/-y"), or "" for none.</summary>
        public string wallContact = "";
    }

    /// <summary>A robot's speed in its own frame: forward, left (positive) and counter-clockwise (positive).</summary>
    public struct RobotSpeed
    {
        public double forward;
        public double left;
        public double counterClockwise;

        public RobotSpeed(double forward, double left, double counterClockwise)
        {
            this.forward = forward;
            this.left = left;
            this.counterClockwise = counterClockwise;
        }
    }

    /// <summary>
    /// How the motors change the robot's speed, copied rule for rule from the harness's
    /// MomentumPhysics (Java), so Unity's robot speeds up, slows down and coasts the same way. Unity's
    /// physics (<see cref="UnityRobotPhysics"/>) then does the moving and the bumping into things.
    /// Keep the two in step: if MomentumPhysics changes, change this too.
    /// </summary>
    public static class DriveLaw
    {
        /// <summary>Below this speed with no power for that motion, static friction holds the robot still (in/s).</summary>
        public const double StopInchesPerSecond = 0.25;

        /// <summary>The speed the wheels ask for (MecanumKinematics.toChassisPower then toVelocity in Java).</summary>
        public static RobotSpeed Target(DriveCommandData c, RobotPhysicsData robot)
        {
            double forward = (c.frontLeft + c.frontRight + c.rearLeft + c.rearRight) / 4.0;
            double strafeRight = (c.frontLeft - c.frontRight - c.rearLeft + c.rearRight) / 4.0;
            double turnClockwise = (c.frontLeft - c.frontRight + c.rearLeft - c.rearRight) / 4.0;
            return new RobotSpeed(forward * robot.forwardInchesPerSecond, -strafeRight * robot.strafeInchesPerSecond,
                -turnClockwise * robot.turnRadiansPerSecond);
        }

        /// <summary>The robot's speed after <paramref name="seconds"/>, moving from <paramref name="now"/> toward the command.</summary>
        public static RobotSpeed After(RobotSpeed now, DriveCommandData c, double seconds, RobotPhysicsData robot)
        {
            RobotSpeed target = Target(c, robot);
            if (!robot.HasDynamics)
            {
                return target; // no momentum: at the commanded speed at once
            }
            // Inches of wheel travel per radian of turning: lets turning share the traction limit.
            double lever = robot.forwardInchesPerSecond / robot.turnRadiansPerSecond;
            bool noPower = c.frontLeft == 0 && c.frontRight == 0 && c.rearLeft == 0 && c.rearRight == 0;
            if (!c.braking && noPower)
            {
                // No power on FLOAT: friction slows each motion at the measured coasting rate.
                double forwardDecel = robot.forwardCoastInchesPerSecondSquared.Value * seconds;
                double lateralDecel = robot.lateralCoastInchesPerSecondSquared.Value * seconds;
                return new RobotSpeed(TowardZero(now.forward, forwardDecel), TowardZero(now.left, lateralDecel),
                    TowardZero(now.counterClockwise, forwardDecel / lever));
            }
            // The motors pull the speed toward the command (a first-order response)...
            double share = 1.0 - Math.Exp(-seconds / robot.responseTimeSeconds.Value);
            double dForward = (target.forward - now.forward) * share;
            double dLeft = (target.left - now.left) * share;
            double dTurn = (target.counterClockwise - now.counterClockwise) * share;
            // ...but the tires can only take so much before they slip.
            double change = Math.Sqrt(dForward * dForward + dLeft * dLeft + dTurn * lever * (dTurn * lever));
            double limit = robot.maxAccelerationInchesPerSecondSquared.Value * seconds;
            if (change > limit)
            {
                double scale = limit / change;
                dForward *= scale;
                dLeft *= scale;
                dTurn *= scale;
            }
            return new RobotSpeed(
                Settle(now.forward + dForward, target.forward, StopInchesPerSecond),
                Settle(now.left + dLeft, target.left, StopInchesPerSecond),
                Settle(now.counterClockwise + dTurn, target.counterClockwise, StopInchesPerSecond / lever));
        }

        private static double Settle(double speed, double target, double stopSpeed)
        {
            return target == 0 && Math.Abs(speed) < stopSpeed ? 0.0 : speed;
        }

        private static double TowardZero(double speed, double amount)
        {
            return Math.Abs(speed) <= amount ? 0.0 : speed - Math.Sign(speed) * amount;
        }
    }
}

# 04 · Code style

The harness is read by beginning Java students. Match the code that is already there.

## Readability

- Use plain, short sentences in comments and docs. Explain **why**, not just what.
- Every public class has a Javadoc comment saying what it is for, written for someone new to the
  project (see `Pose`, `PoseEstimator` and `WheelReader`).
- Give names that say what something is, and put the unit in the name: `xInches`,
  `matchTimeSeconds`, `loopPeriodSeconds`.
- Name tests as sentences describing behavior: `holdingAButtonOnlyCountsAsOnePress`.
- One behavior per test.

## Structure

- Packages:
  - `hardware/`: fake devices and robot configurations
  - `ds/`: Driver Station side (gamepads, telemetry, rumble)
  - `runtime/`: running OpModes and the clock
  - `physics/`: motion and position math
  - `agent/`: the Java agent (`src/agent/java`)
- Future outside connections go in their own packages, never inside the packages above:
  `bridge/` for Unity and `viewer/` for the desktop window.
- Prefer small final classes and `record`s with narrow public methods. Avoid reflection, deep
  inheritance, dependency-injection frameworks and concurrency unless the user agrees there is a
  need.
- Check inputs and fail early with a clear message that says what was wrong and what was
  expected, the way `Pose` rejects non-finite numbers.
- Robot facts (speeds, sizes) are stored as `ModelValue`s marked `measured(...)` or
  `placeholder(...)`, with where the number came from. Never present a guess as a measurement.

## Java version

- Harness code compiles with `--release 17`. Records, switch expressions and `var` are fine.
- TeamCode is compiled into the same build, so never require TeamCode to use newer Java.

# Localization and Pathing Teacher Guide

## Purpose

This guide accompanies `LOCALIZATION_PATHING_STUDENT_GUIDE.md`. It helps a teacher facilitate the
same three-session localization and pathing progression without giving students a finished Pedro
implementation or predetermined architecture answer.

The target learning experience is a pilot using Pedro Pathing, a goBILDA Pinpoint Odometry Computer
with two odometry pods, and a four-wheel mecanum chassis. Students should make and explain the design
decisions, inspect every generated change, and perform the first real hardware configuration,
tuning, and troubleshooting.

Treat the checked-out repository, `AGENTS.md`, `ARCHITECTURE.md`, `IMPLEMENTATION_STATUS.md`, and
`LOCALIZATION_PATHING_PROMPT_PROGRESS.md` as the source of truth. Do not rely on an earlier chat.

## Artifact boundaries

- The student guide contains the prompts students execute.
- This teacher guide contains rationale, expected evidence, pacing, and intervention points.
- The progress record begins with `Working branch: UNCONFIRMED` and LP-01 through LP-11 marked
  `Not started`.
- Do not distribute a completed implementation, measured constants from another robot, a fabricated
  Visualizer export, or a pre-completed progress record.
- A build proves software compatibility, not localization accuracy, safe movement, or path quality.
- A blocked checkpoint with preserved evidence is a valid learning result.

## Starting or resuming work

Students create and check out one local branch for the entire progression. `Start Here` confirms the
current branch and records it; it never creates or switches branches. The progress record, not chat
history, determines which prompt is next.

When another student or a new chat continues the same branch:

- Read the progress record and its repository evidence.
- Resume a `Results ready` prompt at student review.
- Do not advance past `Blocked` until the recorded prerequisite is resolved.
- Do not reset progress merely to bypass a decision, failed build, or safety gate.

For a genuinely fresh team branch, begin with the initialized progress template. Do not reuse a
record that contains another team's decisions or evidence.

## Three-session facilitation plan

### Session 1 — Understand and design, 60 minutes

- LP-01: architecture and ownership discovery.
- LP-02: record the team's approved design in `ARCHITECTURE.md`.

Expected checkpoint: students can explain the current drivetrain ownership, compare integration
designs, select one, and record design intent without claiming implementation.

### Session 2 — Integrate and validate without hardware, 60 minutes

- LP-03, about 8 minutes: current dependency and environment research.
- LP-04, about 10 minutes: narrow dependency installation and Gradle/Android setup.
- LP-05, about 15 minutes: optional library-neutral seams.
- LP-06, about 20 minutes: separate team-specific Pedro/Pinpoint composition.
- LP-07, about 7 minutes: software-only lifecycle, ownership, and build review.

Expected checkpoint: the branch compiles, simple robots remain usable, the selected team's original
Robot class remains unchanged, and every unverified physical value is clearly gated.

### Session 3 — Safe hardware bring-up and pilot path, 60 minutes

- LP-08, about 12 minutes: measured hardware facts and staged readiness gates.
- LP-09, about 18 minutes: unpowered localization checks and restricted manual-drive checks.
- LP-10, about 18 minutes: version-matched tuning with one evidence category at a time.
- LP-11, about 12 minutes: Visualizer design, thin autonomous integration, and one cautious path.

These times are review targets, not deadlines for powered work. Hardware discovery and tuning may
require another meeting. Never rush or combine tests to preserve the schedule.

## Learning-check policy

After each LP result, the assistant creates five simple questions based on that execution and asks
them one at a time. The purpose is engagement and comprehension, not grading.

- Do not pre-populate or require repeatable questions.
- Do not record answers or scores.
- Explain an incorrect answer immediately and plainly.
- An incorrect answer does not block progress after the explanation.
- The student must attempt all five questions before the result can be marked `Reviewed`.
- A quiz answer never substitutes for a required engineering or safety decision.

## Environment and permission policy

- Use Android Studio and the repository's Gradle wrapper under the student's normal account.
- Use JDK 17 as the Gradle runtime unless current repository and official compatibility evidence
  requires a separately approved change.
- Do not run Android Studio, PowerShell, Git, or Gradle as Administrator.
- Do not change PowerShell execution policy, system-wide environment variables, network security,
  or antivirus settings.
- If a USB driver, SDK component, protected location, license, proxy, or certificate requires
  elevated access, students stop and give the exact request or error to the teacher or IT staff.
- On Windows, the expected repository build is
  `.\gradlew.bat TeamCode:assembleDebug`.

## Session 3 minimum-hardware gate

Before the assistant even previews LP-08, students must acknowledge the complete minimum-hardware
warning in the student guide. The required setup includes a secure four-wheel mecanum chassis,
mounted and wired drive motors, safe battery and Control Hub installation, real Driver Station
hardware names, mounted Pinpoint and two engaged odometry pods, measurement access, deployment
access, a clear test area, adult supervision, raised-wheel support, and a named Driver Station STOP
operator.

Intervene immediately if anyone proposes using invented names, motor directions, offsets, pod
models, encoder directions, mass, gains, velocities, or path constraints. Incorrect configuration
can prevent initialization, produce false pose data, or cause unexpected powered movement.

Before every powered action, require the student to state:

- Expected motion.
- Maximum power.
- Travel or rotation limit.
- Automatic and manual stop conditions.
- The person controlling Driver Station STOP.

Stop for wrong direction, unavailable or unstable pose, oscillation, excessive speed, unsafe sound
or wiring, entanglement risk, or movement outside the approved area.

## Prompt rationale and expected evidence

### LP-01 v1.2 — Architecture and ownership discovery

Rationale: students must identify the existing drivetrain owner and lifecycle before introducing a
follower that could compete for the same motors.

Expected strong result:

- Reconstructs the current OpMode-to-hardware and autonomous flows from actual classes.
- Identifies where `HardwareMap` enters and where periodic updates occur.
- Compares at least two integration designs with tradeoffs.
- Preserves one drivetrain owner, Robot APIs, subsystem FSM ownership, non-blocking updates, and no
  new scheduler.
- Separates software evidence from hardware questions.

Intervene if the assistant edits files beyond the progress-status exception, assumes classes that do
not exist, or recommends two owners for the same motors.

### LP-02 v1.1 — Record the approved architecture

Rationale: design intent must be reviewed before implementation begins.

Expected strong result:

- Changes only `ARCHITECTURE.md`.
- Records responsibilities, dependency direction, containment, optional simple-robot support,
  team-selectable localization/pathing, lifecycle, visualization, cancellation, and safety.
- Leaves physical configuration and tuning explicitly deferred.
- Does not describe the design as implemented.

Intervene if implementation code or `IMPLEMENTATION_STATUS.md` is changed, or if the document
records activity instead of durable design intent.

### LP-03 v1.3 — Select dependencies and environment requirements

Rationale: Pedro, Android SDK, FTC SDK, Gradle, and JDK requirements change. Students must verify
current official sources rather than copy versions from a prompt or old example.

Expected strong result:

- Runs the baseline TeamCode build.
- Compares repository versions with current official Pedro installation, release, Quickstart, and
  Pinpoint sources.
- Distinguishes Gradle JDK 17 from TeamCode's Java language level.
- Recommends a pinned, minimal dependency plan and classifies manual actions by permission level.

Intervene if the assistant edits files, proposes an unapproved broad upgrade, or instructs students
to elevate privileges.

### LP-04 v1.2 — Add the approved Pedro dependency

Rationale: dependency setup should be isolated so build failures are not mixed with architecture or
hardware work.

Expected strong result:

- Changes only the approved Gradle/Android files plus `IMPLEMENTATION_STATUS.md`.
- Uses current verified versions and performs Gradle Sync and JDK 17 builds.
- Clearly states that no drivetrain, localization, or path behavior exists yet.

Intervene if the assistant copies an entire Quickstart, adds Ivy without approval, upgrades the FTC
project broadly, or treats successful dependency resolution as robot validation.

### LP-05 v1.2 — Add optional library-neutral seams

Rationale: common APIs should describe the baseline's needs without importing Pedro, RoadRunner, or
Pinpoint types.

Expected strong result:

- Adds narrow, beginner-readable composition boundaries.
- Keeps simple Team A, B, and C robots usable without localization or pathing.
- Keeps drive modes mutually exclusive under the existing subsystem FSM.
- Adds no vendor controller, path, tuning values, OpMode, or scheduler.

Intervene if an existing team Robot is replaced, common packages import vendor types, or the change
introduces a deep abstraction hierarchy.

### LP-06 v1.3 — Integrate the team-specific Pedro/Pinpoint pilot

Rationale: the pilot needs a separate composition that selects Pedro as the sole drivetrain and
localization owner while preserving the selected team's simple Robot.

Expected strong result:

- Asks the team to select the pilot package when that decision is not recorded.
- Leaves the existing team Robot class unchanged and adds a separate Pedro Robot beside it.
- Creates the follower during Robot initialization and updates it once per Robot loop.
- Provides manual, follow, cancel, completion, pose, and stop behavior through neutral boundaries.
- Fails clearly on missing physical configuration and makes no hardware claims.

Intervene if both Pedro and `DriveHardware` initialize the same motors, an OpMode owns the follower,
or sample constants are saved as team facts.

### LP-07 v1.2 — Validate Session 2 without hardware

Rationale: compilation and static inspection can validate important boundaries while remaining
honest about everything that still requires a robot.

Expected strong result:

- Confirms simple drive remains available and only one controller owns the pilot drivetrain.
- Confirms manual/path exclusivity, one update per loop, non-blocking cancellation, safe stop, and
  vendor-neutral common APIs.
- Reconciles `IMPLEMENTATION_STATUS.md` with actual files and build results.
- Produces a precise Session 3 prerequisite list.

Intervene if software inspection is described as localization, direction, tuning, or path success.

### LP-08 v1.0 — Record hardware facts and stage readiness

Rationale: localization initialization must be possible before tuning, but it must not silently
authorize manual motion or path following.

Expected strong result:

- Collects one real repository or robot fact at a time and records its source, units, and status.
- Separates localization-initialization, restricted-manual, and final path-following permissions.
- Keeps drivetrain output disabled while preparing localization.
- Leaves unknown values unknown and stops before deployment or powered movement.

Intervene if minimum hardware is unavailable, a value is guessed, or one gate authorizes every
later capability.

### LP-09 v1.0 — Verify pose and restricted manual-drive safety

Rationale: teams must validate coordinate signs and stop behavior before tuning or following paths.

Expected strong result:

- Begins with unpowered hand movement and records expected versus observed X, Y, and heading.
- Changes only one configuration fact per retest.
- Opens restricted manual testing only after localization behaves consistently.
- Uses a stable raised-wheel setup for direction checks and verifies cancel, Driver Station STOP,
  and Robot stop.
- Keeps path following closed.

Intervene if tests begin on the floor, multiple directions/offsets are changed together, or anyone
reaches into a powered drivetrain.

### LP-10 v1.1 — Collect tuning evidence

Rationale: the Maven dependency may not contain the Quickstart tuning entry point, and default
tuners may request unsafe power or distance. Tuning source must match the pinned Pedro APIs.

Expected strong result:

- Verifies the exact tuning source/version and maintains one drivetrain owner.
- Explains supported drive-tuning approaches and records the team's choice.
- Follows setup/mass, localization, velocity, heading, selected drive algorithm, and validation in
  that order rather than Driver Station menu order.
- Reviews power, travel, and stop behavior before every moving tuner.
- Preserves raw observations, units, repeatability, and before/after values.
- Opens path readiness only after the student and supervising adult accept complete evidence.

Intervene if a mismatched `Tuning.java` is pasted, a default full-power/long-distance test is run
blindly, or a first/inconsistent result is marked tuned.

### LP-11 v1.1 — Build and test one cautious pilot path

Rationale: the final step connects an external visual design artifact to a team path, neutral
request, non-blocking autonomous step, Robot lifecycle, and one supervised physical run.

Expected strong result:

- Uses `https://visualizer.pedropathing.com/` and saves a real, re-uploadable team artifact.
- Stops as Blocked if the site or browser is unavailable; it does not fabricate an export.
- Reviews and adapts generated code so vendor types remain team-specific.
- Uses a thin testing OpMode and the existing `AutoSequence`/Robot lifecycle.
- Runs one short path at restricted power and records completion, pose error, cancellation, stop,
  and unexpected behavior.
- Finishes with either a demonstrated pilot or a documented failure with one next hypothesis.

Intervene if generated code bypasses the Robot/subsystem/controller boundary, the path does not fit
the clear area, or success is claimed beyond observed evidence.

## Completion review

Before accepting the progression as complete, verify:

- LP-01 through LP-11 are `Reviewed`, or a final `Blocked` state records actionable evidence.
- `ARCHITECTURE.md` contains approved intent rather than an implementation diary.
- `IMPLEMENTATION_STATUS.md` matches actual code, builds, physical observations, and TODOs.
- Simple robots still work without localization or pathing.
- The selected pilot has one drivetrain owner and one non-blocking update path.
- Every physical constant is traceable to a team measurement or observed tuning result.
- Students can explain the final class relationships and the purpose of each safety gate.
- No commit, push, merge, or pull request occurs until the team reviews the complete diff.

## Prompt versions

- LP-01 v1.2
- LP-02 v1.1
- LP-03 v1.3
- LP-04 v1.2
- LP-05 v1.2
- LP-06 v1.3
- LP-07 v1.2
- LP-08 v1.0
- LP-09 v1.0
- LP-10 v1.1
- LP-11 v1.1

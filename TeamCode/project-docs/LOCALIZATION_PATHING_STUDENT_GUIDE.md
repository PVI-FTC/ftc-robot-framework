# Localization and Pathing Student Guide

## Start here by executing this prompt in your AI Agent

> - Read `TeamCode/project-docs/LOCALIZATION_PATHING_STUDENT_GUIDE.md`.
> - Assume the student has already created and checked out the one local branch they intend to use
>   for all localization and pathing work. Never create or switch branches.
> - Determine the current branch with `git branch --show-current`. If it returns no branch, stop and
>   explain that the repository is not currently on a named branch.
> - Show the exact current branch name and ask: `Is this the branch you intend to use for all
>   localization and pathing work?` Wait for the student's answer before doing anything else.
> - Read `TeamCode/project-docs/LOCALIZATION_PATHING_PROMPT_PROGRESS.md` after the student confirms.
> - If its working branch is `UNCONFIRMED`, update only that field to the confirmed current branch.
>   If it already matches, continue. If it names a different branch, stop and ask the student or
>   teacher to resolve whether this is an existing branch handoff or a fresh branch; do not overwrite
>   or reset progress automatically.
> - Treat the progress file and its cited repository evidence as the source of truth; do not infer
>   prompt completion from chat history.
> - If the progress record says `Blocked`, explain that blocker and do not advance. If it says
>   `Results ready`, resume student review of that prompt before executing another prompt.
> - Otherwise, find the first prompt whose status is not `Reviewed`, beginning with LP-01.
> - If the first prompt not marked `Reviewed` is LP-08, do not display, summarize, or execute LP-08
>   yet. First present the complete `STOP — Minimum physical robot required for Session 3` warning
>   from this guide. Explain that invented hardware names can prevent initialization, invented motor
>   directions can cause unexpected movement, and invented Pinpoint names, pod types, offsets, or
>   encoder directions can produce unavailable or dangerously incorrect pose data that pathing may
>   try to follow. Ask the student to explicitly confirm that the real robot meets every listed
>   minimum build, inspection, measurement, supervision, and stop-control requirement. Wait for the
>   answer. If the student cannot confirm every item, mark LP-08 `Blocked`, record the missing
>   prerequisite, and do not display the LP-08 prompt. This acknowledgment is a current safety check,
>   so ask again whenever Start Here reaches LP-08 in a new chat; do not infer it from chat history.
> - Before executing it, present a short bulleted explanation of what the prompt will ask you to
>   inspect, research, decide, change, and produce.
> - Clearly state whether the prompt is read-only or permits file changes.
> - Ask the student whether they understand what the prompt will ask you to do and whether they want
>   you to proceed; wait for their answer.
> - If the student confirms, execute that prompt exactly.
> - Present the results for a high school student with little or no Java experience: use short
>   bullets, plain language, simple class-relationship flows when helpful, and briefly explain any
>   technical term the student needs to understand.
> - Clearly separate what was learned, the recommendation, what can be checked without a robot, and
>   what must be checked on physical hardware.
> - After presenting an LP prompt's results, create a fresh five-question learning check based on
>   the work and evidence from that specific execution. Use only simple true/false or multiple-choice
>   questions suitable for a student with little or no Java experience. Do not read questions from
>   the guide or require the questions to be repeatable between chats.
> - Ask only one learning-check question at a time and wait for the student's answer. Briefly confirm
>   a correct answer before asking the next question. For an incorrect answer, give the correct
>   answer and explain in plain language why that idea matters to the robot architecture or
>   development work before asking the next question. Continue until all five have been attempted.
> - Treat the learning check as engagement, not grading. Do not calculate or report a score, do not
>   retain answer history, and do not write questions or answers to the repository or progress
>   record. The student must attempt the learning check before the prompt can be marked `Reviewed`,
>   but an incorrect answer does not block progress after the explanation is provided.
> - After the learning-check feedback, continue with questions that are actually required before the
>   next prompt. Quiz questions are not design decisions and do not replace required decisions.
> - At the end, divide remaining questions into `Required before the next prompt` and `Can wait until
>   later`. Do not make the student answer a question now unless its answer is actually required to
>   execute the next prompt safely and correctly.
> - If a required question remains, explain in plain language why it matters, present the reasonable
>   choices and their main tradeoffs, state a recommendation when the evidence supports one, and ask
>   the student to decide. Ask one required question at a time and wait for each answer.
> - After all required questions are answered, summarize the student's decisions and state that the
>   student is ready for the next prompt. Do not execute the next prompt automatically.
> - If no required questions remain, state that clearly and stop for student review. Do not execute
>   the next prompt automatically.
> - Running a prompt does not complete it. Immediately before presenting results, update only its
>   progress-record row to `Results ready`, or `Blocked` if work cannot continue. This status-only
>   recordkeeping edit is allowed even for a read-only prompt.
> - Mark a prompt `Reviewed` only in a later turn, after the student explicitly accepts the evidence
>   and all questions required for the next prompt are resolved. Never execute the next prompt
>   automatically.

## Purpose

Use AI as a guided engineering partner to add a pilot localization and pathing capability without
being handed a packaged implementation. The target pilot uses Pedro Pathing, a goBILDA Pinpoint
Odometry Computer with two odometry pods, and a four-wheel mecanum chassis.

Teachers may use `LOCALIZATION_PATHING_TEACHER_GUIDE.md` for pacing, rationale, expected evidence,
and intervention points. Students and AI assistants execute prompts only from this student guide;
the teacher guide and chat history do not determine prompt completion.

The prompts in this guide are versioned. Use them in order, inspect every proposed change, and keep
notes about evidence, decisions, build results, and questions. Do not accept generated code merely
because it compiles.

Prompt completion is stored in `LOCALIZATION_PATHING_PROMPT_PROGRESS.md`, not in chat memory. A new
student branch begins with its recorded branch set to `UNCONFIRMED` and every prompt set to `Not
started`. `Start Here` records the actual branch only after the student confirms it. A student
continuing that branch preserves its current record.

Before every prompt, the assistant should explain the work in concise bullets and wait for the
student to confirm understanding. The explanation is not a substitute for the full prompt; it is a
checkpoint that lets the student ask questions before work begins.

## Three-session progression

### Session 1: Understand and design (60 minutes)

- Inspect the existing architecture and current public APIs.
- Research current official Pedro Pathing and Pinpoint guidance.
- Identify lifecycle, dependency, and drivetrain-ownership conflicts.
- Compare integration designs and record an architecture decision before editing code.
- After team review, use a separate documentation prompt to update `ARCHITECTURE.md` with the
  approved design requirements, dependency flow, ownership boundaries, and deferred decisions.

Expected checkpoint: an evidence-backed design that preserves one drivetrain owner, non-blocking
updates, Robot public APIs, subsystem FSM ownership, and hardware-access boundaries, with the
approved intent recorded in `ARCHITECTURE.md` before implementation begins.

### Session 2: Integrate and validate without hardware (60 minutes)

- LP-03, about 8 minutes: select compatible dependency and Android build requirements.
- LP-04, about 10 minutes: add only the approved dependency and perform the Android Studio/Gradle
  setup checkpoint.
- LP-05, about 15 minutes: add the library-neutral drive and localization seams while preserving
  simple robots.
- LP-06, about 20 minutes: add the team-specific Pedro mecanum and Pinpoint pilot integration
  without inventing physical values.
- LP-07, about 7 minutes: validate lifecycle and safety without hardware and reconcile
  `IMPLEMENTATION_STATUS.md`.
- Build after each small change and review the complete diff. If a prompt takes longer than its
  suggested time, stop at its review checkpoint rather than rushing into the next prompt.

Expected checkpoint: a compiling pilot whose unverified physical constants are clearly identified,
not guessed, and an `IMPLEMENTATION_STATUS.md` that matches the checked-out code.

#### Windows permissions and JDK rules for Session 2

- Normal work in these prompts does not require an Administrator account: editing files on the
  working branch, Gradle Sync, assembling the `TeamCode` module in Android Studio, Gradle wrapper
  builds, and dependency downloads to the student's user profile should all run as the student.
- Do not run Android Studio, PowerShell, Git, or Gradle as Administrator. Do not change PowerShell's
  execution policy, disable security software, edit system-wide environment variables, or install a
  separate system-wide Gradle.
- Use JDK 17 as the Gradle JDK for this FTC project. In Android Studio, check `File > Settings >
  Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK`. Also run
  `.\gradlew.bat --version` in Windows PowerShell and confirm its JVM is version 17 before LP-04.
- The Gradle JDK is the Java runtime that runs the build. It is separate from the Java language and
  bytecode level used by TeamCode, so selecting Gradle JDK 17 does not require changing the existing
  Java source or target compatibility.
- Prefer an existing Android Studio-managed JDK 17 and the existing per-user Android SDK location.
  If school policy permits and the tool is missing, Android Studio can normally download it into the
  student's profile without a system-wide installation.
- If Windows displays a User Account Control administrator prompt, the SDK/JDK location is not
  writable, a download is blocked, or school policy prevents accepting an SDK license, cancel the
  operation. Save the requested component, destination path, and complete error, then ask the teacher
  or IT administrator to provision it. Do not attempt a permissions workaround.
- Maven or Gradle download failures caused by a school proxy, firewall, certificate, or blocked
  repository are teacher/IT issues, not reasons to elevate PowerShell or weaken network security.

### Session 3: Build a path and perform safe hardware bring-up (60 minutes)

> **STOP — Minimum physical robot required for Session 3**
>
> Do not begin LP-08 unless the team can inspect a real, minimally complete robot containing:
>
> - A structurally secure four-wheel mecanum chassis
> - Four mounted and wired drive motors with wheels installed
> - A mounted Control Hub, secured wiring, main power switch, and safe battery
> - A Driver Station hardware configuration containing the four drive motors
> - A firmly mounted Pinpoint computer connected to a suitable I2C port
> - Two mounted and engaged odometry pods: forward pod connected to Pinpoint X and strafe pod
>   connected to Pinpoint Y
> - Known pod model or encoder resolution
> - Physical access to measure pod locations from the robot's center of rotation and measure robot
>   mass
> - A computer with Android Studio, deployment access, compatible Robot Controller/Driver Station
>   apps, and a gamepad
> - A stable raised-wheel support, clear ground-test area, adult supervision, and a named person
>   responsible for Driver Station **STOP**
>
> Other mechanisms such as an intake, arm, camera, or scoring system are not required.
>
> If the robot, its Driver Station configuration, Pinpoint installation, measurement access, or
> safety setup is unavailable, mark LP-08 `Blocked`. Do not assume names, wiring, dimensions,
> directions, or tuning values, and do not proceed to later Session 3 prompts.
>
> Making up values is not a harmless way to continue. A false hardware name can stop initialization;
> a false motor direction can make a wheel or chassis move unexpectedly; and false Pinpoint names,
> pod types, offsets, or encoder directions can report an unavailable or incorrect pose. A path
> follower may then steer using that incorrect pose, creating collision, tip-over, entanglement,
> equipment-damage, or injury risk. Use only facts inspected or measured on the real robot and keep
> later readiness gates closed until their physical checks pass.

- LP-08, about 12 minutes: record real robot configuration and replace the single all-or-nothing
  configuration gate with staged readiness gates; do not power the drivetrain.
- LP-09, about 18 minutes: deploy a narrow diagnostic, verify Pinpoint pose behavior by moving the
  unpowered robot, then verify manual motor directions at restricted power.
- LP-10, about 18 minutes: use Pedro's version-matched tuning guidance to collect and record only
  values actually observed on the robot.
- LP-11, about 12 minutes: use Pedro's external visualizer to design and cautiously test one short
  team-specific pilot path, then reconcile the documentation.
- Treat these times as review targets. Hardware discovery, tuning, or troubleshooting may require
  another meeting; never rush a physical test to keep the schedule.

Expected checkpoint: a small demonstrated path or a well-documented failure with enough telemetry
and observations to identify the next troubleshooting step.

#### Session 3 hardware, Android Studio, and safety rules

- A teacher or approved adult must supervise every powered test. Use a clear test area, stable robot
  stand when wheels must be raised, safety glasses where required by team policy, and one named
  person ready to press Driver Station **STOP**. Never reach into a powered drivetrain.
- Before each powered action, state the expected motion, maximum power, travel limit, stop signal,
  and who controls the stop. Run only one new motion or changed value at a time.
- Do not use sample motor directions, offsets, mass, gains, velocities, braking values, or path
  constraints as if they were measured. Label official starting values as starting values and keep
  the relevant readiness gate closed until the team verifies them.
- Normal Android Studio builds, USB deployment, Driver Station configuration, and browser access to
  Panels should use the student's standard account. Do not run Android Studio or PowerShell as
  Administrator. If Windows requests an administrator password for a USB driver, SDK component, or
  protected installation, cancel and ask the teacher or IT staff to provision it.
- Keep Gradle on JDK 17. After each code increment, sync if necessary, assemble the `TeamCode` module,
  and run `.\gradlew.bat TeamCode:assembleDebug` from Windows PowerShell.
- A hardware test that cannot proceed safely is a valid `Blocked` result. Record the exact failed
  checkpoint, telemetry, observed motion, and one next hypothesis; do not bypass a gate.

Official references to re-check when the prompts run include Pedro's [installation
guide](https://pedropathing.com/docs/pathing/installation), [mecanum setup
guide](https://pedropathing.com/docs/pathing/tuning/setup), [Pinpoint localization
guide](https://pedropathing.com/docs/pathing/tuning/localization/pinpoint), [localization test
guide](https://pedropathing.com/docs/pathing/tuning/localization), and [tuning
sequence](https://pedropathing.com/docs/pathing/tuning). The assistant must still verify that any
copied Quickstart or tuning source matches the dependency version resolved by the branch.

## Prompt register

### LP-01 v1.2 — Architecture and ownership discovery

Use this prompt before making any code changes:

> - Do not change code, architecture, or implementation documentation; this is a read-only discovery
>   prompt. The required status-only update to `LOCALIZATION_PATHING_PROMPT_PROGRESS.md` is the sole
>   recordkeeping exception.
> - Inspect `AGENTS.md`, `ARCHITECTURE.md`, `IMPLEMENTATION_STATUS.md`, and the current drivetrain,
>   hardware, Robot, FSM, and autonomous classes.
> - Research the current official Pedro Pathing documentation for mecanum setup, Pinpoint
>   localization, follower lifecycle, and visualization.
> - Explain where Pedro and Pinpoint could fit in this repository.
> - Identify any competing ownership of motors or localization state.
> - Compare at least two integration designs.
> - Preserve the repository's dependency direction, non-blocking loop, Robot public API,
>   subsystem-owned FSM, hardware boundaries, and prohibition on a new scheduler.
> - Cite repository files and official Pedro sources.
> - End with open questions and a recommendation, then stop for student review.

Evidence to save:

- A diagram or concise dependency flow for each design.
- The class that currently owns drivetrain output.
- Where `HardwareMap` and the Pedro follower would be created and updated.
- How manual drive, path following, cancellation, and stop avoid competing motor commands.
- What can be verified without hardware and what must remain explicitly unverified.

### LP-02 v1.1 — Record the approved architecture

Use this only after the team has reviewed LP-01 and selected an integration design:

> - Do not implement localization or pathing code.
> - Reinspect the current repository and the approved LP-01 decision.
> - Update only `TeamCode/project-docs/ARCHITECTURE.md` to record the intended localization and
>   pathing architecture.
> - Describe responsibilities, containment and dependency direction, single drivetrain ownership,
>   team-selectable pathing and localization implementations, non-blocking lifecycle, autonomous
>   and TeleOp entry paths, visualization placement, safety, and deliberately deferred hardware
>   configuration or tuning.
> - Preserve the distinction that `ARCHITECTURE.md` records design intent while
>   `IMPLEMENTATION_STATUS.md` records implemented reality.
> - Show the complete diff and run `git diff --check`.
> - Do not claim that the design is implemented; stop for student review.

Evidence to save:

- The reviewed architecture diff.
- A check that every new dependency still points downward.
- A list of decisions intentionally left to implementation or physical testing.

### LP-03 v1.3 — Select compatible dependencies and environment requirements

Use this read-only prompt before changing Gradle files:

> - Do not change code, architecture, or implementation documentation. The required status-only
>   update to `LOCALIZATION_PATHING_PROMPT_PROGRESS.md` is the sole recordkeeping exception.
> - Show the current branch name, compare it with the progress record, ask the student to confirm it
>   is their intended localization and pathing branch, and wait. Do not create or switch branches.
> - Reinspect `AGENTS.md`, `ARCHITECTURE.md`, `IMPLEMENTATION_STATUS.md`, the root and TeamCode Gradle
>   files, Gradle wrapper version, Android Gradle Plugin version, compile SDK, FTC SDK version, and
>   current Java compatibility.
> - Run the documented baseline TeamCode build from the repository root. On Windows PowerShell use
>   `.\gradlew.bat TeamCode:assembleDebug`.
> - Research the current official Pedro installation documentation, official release list,
>   Quickstart dependency file, and Pinpoint setup documentation. Cite those sources.
> - Identify the exact Pedro version, repository, required companion dependencies, and Android SDK
>   requirements that are current at execution time; do not copy versions from this guide.
> - Compare those requirements with this repository and explain every mismatch in plain language.
> - Compare the smallest follower-only dependency set with the official dashboard-enabled setup.
>   Recommend only what the pilot needs; do not make Panels or FTC Dashboard mandatory for simple
>   robots.
> - List the exact files a later prompt would need to change. Do not propose an FTC SDK upgrade or a
>   broad Gradle rewrite unless official compatibility evidence makes it necessary.
> - Treat JDK 17 as the expected Gradle runtime for this repository unless current official evidence
>   shows otherwise. Distinguish the Gradle runtime from TeamCode's Java source/target compatibility;
>   do not change the language level merely because Gradle runs on JDK 17.
> - Provide a short Windows and Android Studio readiness checklist. It must include selecting an
>   existing Android Studio-managed Gradle JDK 17, confirming JVM 17 with
>   `.\gradlew.bat --version`, checking the Android SDK platform, Gradle Sync, and the command-line
>   build.
> - Classify every manual action as normal student access or teacher/IT administrator assistance.
>   Do not instruct the student to run any program as Administrator or to alter system security,
>   system-wide environment variables, or PowerShell execution policy.
> - If the current FTC SDK, Android Gradle Plugin, Gradle wrapper, Java, or compile SDK cannot support
>   the selected Pedro version without a larger upgrade, stop and present that as a required team
>   decision before LP-04.
> - End with one recommended, pinned dependency plan and stop for student review.

Evidence to save:

- Links to the official Pedro installation, release, Quickstart, and Pinpoint sources.
- Current repository versions compared with Pedro's requirements.
- The selected dependency set and why each dependency is needed.
- Any required manual Android Studio preparation, its permission classification, and any
  compatibility blocker.

### LP-04 v1.2 — Add the approved Pedro dependency

Use this only after the team approves LP-03's dependency plan:

> - Show the current branch name, compare it with the progress record, ask the student to confirm it
>   is their intended localization and pathing branch, and wait. Do not create or switch branches.
> - Reinspect the repository and LP-03 evidence; verify exact current dependency versions against
>   official Pedro sources instead of relying on chat memory.
> - Run the baseline TeamCode build before editing. On Windows PowerShell use
>   `.\gradlew.bat TeamCode:assembleDebug`.
> - Change only the narrow Gradle or Android configuration files required by the approved plan.
> - Do not copy the Pedro Quickstart project, replace the FTC SDK project, add Ivy, or add an
>   unselected dashboard dependency.
> - Do not upgrade the FTC SDK, Gradle wrapper, Android Gradle Plugin, or Java language level unless
>   LP-03 explicitly approved that separate compatibility change. Stop if an unapproved upgrade is
>   required.
> - Ask the student to confirm Android Studio uses an existing JDK 17 under `File > Settings > Build,
>   Execution, Deployment > Build Tools > Gradle > Gradle JDK`. Do not install a system-wide JDK or
>   change system `PATH` for this task.
> - Ask the student to perform these local manual steps when needed: use Android Studio's SDK Manager
>   with the existing per-user SDK location to install the approved Android SDK platform; accept only
>   the needed SDK license when school policy permits; click `Sync Project with Gradle Files`; and
>   copy the first complete error if sync fails.
> - If any step requests an Administrator login, displays a User Account Control prompt, targets a
>   protected location, or is blocked by school policy or networking, cancel it and record the
>   component, path, and complete error for the teacher or IT administrator. Do not run Android
>   Studio or PowerShell as Administrator and do not weaken system or network security.
> - After Android Studio sync, run `.\gradlew.bat --version` and confirm JVM 17, then run
>   `.\gradlew.bat TeamCode:assembleDebug` from Windows PowerShell at the repository root. Do not use
>   `gradlew` without `.bat` in PowerShell.
> - Update `IMPLEMENTATION_STATUS.md` with the pinned dependencies, configuration changes, build
>   result, and the fact that no Pedro drivetrain or localization behavior exists yet.
> - Run `git diff --check`, show the complete diff, and stop for student review.

Evidence to save:

- The exact repository and dependency lines added.
- Gradle JDK/JVM version, Android Studio Gradle Sync result, and Windows command-line build result.
- Any teacher/IT-provisioned component and why student access was insufficient.
- Proof that no Java integration or physical configuration was added.
- The matching `IMPLEMENTATION_STATUS.md` entry.

### LP-05 v1.2 — Add optional library-neutral architecture seams

Use this only after LP-04 builds successfully:

> - Show the current branch name, compare it with the progress record, ask the student to confirm it
>   is their intended localization and pathing branch, and wait. Do not create or switch branches.
> - Reinspect the approved architecture and actual drive, hardware, Robot, FSM, autonomous, and test
>   APIs. Do not assume proposed class or method names already exist.
> - Run the baseline TeamCode build before editing.
> - Add the smallest beginner-readable, library-neutral drive and pose boundaries required by the
>   approved architecture. Prefer composition and narrow interfaces.
> - Preserve the existing simple mecanum implementation as the default for teams that do not select
>   localization or pathing. Team A, B, and C must not require Pedro or Pinpoint merely to drive.
> - Do not change or replace any team's existing Robot class to make it a Pedro robot. The team
>   selected for the pilot will keep that class as its simple option; LP-06 will add a separate,
>   clearly named Pedro Robot class beside it.
> - Keep `DriveSubsystem` responsible for its FSM and mutually exclusive disabled, manual, and
>   path-following requests. Do not expose FSM state objects through Robot APIs.
> - Keep vendor classes such as Pedro `Follower`, `Path`, `PathChain`, and `Pose` out of common Robot,
>   subsystem, autonomous, and localization APIs.
> - Do not add the Pedro controller, Pinpoint constants, a path, a pathing OpMode, or a scheduler in
>   this prompt.
> - Add focused tests or compile-time fakes when the repository's current test setup supports them;
>   otherwise document the specific behavior that remains compile-checked only.
> - Build with `.\gradlew.bat TeamCode:assembleDebug` on Windows, run `git diff --check`, and review
>   the complete diff for one-way dependencies and unchanged simple-robot behavior.
> - Update `IMPLEMENTATION_STATUS.md` with only the seams and validation that now exist, then stop for
>   student review.

Evidence to save:

- A short class containment and dependency flow for simple and pathing-capable compositions.
- Proof that common packages contain no Pedro imports.
- Proof that Team A, B, and C still have a simple-drive composition available.
- Build/test results and the matching `IMPLEMENTATION_STATUS.md` entry.

### LP-06 v1.3 — Integrate the Pedro mecanum and Pinpoint pilot

Use this only after LP-05 is reviewed and builds successfully:

> - Show the current branch name, compare it with the progress record, ask the student to confirm it
>   is their intended localization and pathing branch, and wait. Do not create or switch branches.
> - Identify which team's package will contain the pilot. If that decision is not recorded, ask the
>   student one required question and wait before editing. Explain that this choice does not authorize
>   changing or replacing the team's existing Robot class.
> - Reinspect the installed Pedro version's actual classes, constructors, and public APIs from the
>   resolved dependency or official source. Do not rely on examples from another Pedro version.
> - Run the baseline TeamCode build before editing.
> - Add one team-specific Pedro controller/factory and configuration location for a four-mecanum-wheel
>   drivetrain with Pinpoint localization, using the LP-05 neutral boundaries.
> - Leave the selected team's existing Robot class unchanged as its simple, non-pathing option. Add a
>   separate, clearly named Pedro Robot class in the same team package to compose the pilot. For
>   example, selecting Team A means keeping `TeamARobot` and adding `TeamAPedroRobot`.
> - Make that Pedro integration the only owner of the pilot's four drive motors and Pinpoint device.
>   Do not initialize the same motors through `DriveHardware` in the pathing-enabled composition.
> - Create the follower during the team Robot initialization path, update it exactly once from the
>   drive-subsystem lifecycle, and provide safe manual, follow, cancel, completion, pose, and stop
>   behavior through the neutral boundary.
> - Keep path-following mode under the existing drive FSM and requests under the Robot public API.
>   Do not add Ivy or another scheduler.
> - Do not invent motor directions, Pinpoint hardware name, pod model, pod offsets, encoder
>   directions, robot mass, power limits, path constraints, or tuning gains. Use team-provided values
>   only. For values that require Session 3 hardware work, leave an obvious unconfigured value or
>   validation gate that compiles but fails clearly before powered pathing.
> - Do not add a competition path or autonomous pathing OpMode yet.
> - In Android Studio, ask the student to run `Sync Project with Gradle Files` if imports do not
>   resolve, then assemble the `TeamCode` module. Depending on the Android Studio version, this may
>   appear as `Build > Assemble Module 'TeamCode'`, `Assemble Selected Modules`, or the older
>   `Make Project`. Also run
>   `.\gradlew.bat TeamCode:assembleDebug` from Windows PowerShell.
> - Add focused software-only tests or fakes where feasible, run `git diff --check`, and inspect the
>   complete diff for duplicate motor ownership and forbidden `HardwareMap` access.
> - Update `IMPLEMENTATION_STATUS.md` with implemented classes and APIs, validation results, every
>   unconfigured physical value, and the explicit statement that no hardware behavior has been
>   verified. Stop for student review.

Evidence to save:

- The pilot composition and its single drivetrain owner.
- Where the follower is created, updated, cancelled, and stopped.
- Proof that shared APIs contain no Pedro types and simple robots remain available.
- A diff check showing that the selected team's existing Robot class was not changed, plus the name
  of the separate Pedro Robot class that was added.
- The list of configuration gates that Session 3 must resolve.
- Build/test results and the matching `IMPLEMENTATION_STATUS.md` entry.

### LP-07 v1.2 — Validate Session 2 without hardware

Use this after LP-06 builds and its diff is approved:

> - Show the current branch name, compare it with the progress record, ask the student to confirm it
>   is their intended localization and pathing branch, and wait. Do not create or switch branches.
> - Reinspect all Session 2 changes, `ARCHITECTURE.md`, `IMPLEMENTATION_STATUS.md`, and relevant tests;
>   do not assume earlier chat summaries are correct.
> - Do not change production behavior unless a focused validation exposes a defect directly caused
>   by Session 2. Report unrelated defects instead of repairing them.
> - Verify with tests, fakes, static inspection, or compile evidence that simple drive remains usable
>   without localization; only one controller can command the pilot motors; manual and path modes are
>   exclusive; the follower updates once per robot loop; cancellation is non-blocking; stop requests
>   zero output; and common APIs contain no Pedro types.
> - If the repository has a supported local unit-test task, run the narrow relevant tests. Always run
>   `.\gradlew.bat TeamCode:assembleDebug` from Windows PowerShell and `git diff --check`.
> - In Android Studio, ask the student to confirm that Gradle Sync completed, the Project view shows
>   no unresolved imports, and assembling the `TeamCode` module completes. Depending on the Android
>   Studio version, the action may be named `Assemble Module 'TeamCode'`, `Assemble Selected
>   Modules`, or the older `Make Project`. Save the first complete error if any check fails.
> - Review the complete Session 2 diff for unrelated changes and architecture violations.
> - Reconcile `IMPLEMENTATION_STATUS.md` with the actual files, APIs, dependency versions, validation
>   results, limitations, and Session 3 hardware TODOs. Do not claim localization or path-following
>   success without a robot test.
> - End with a short Session 2 checkpoint and a hardware-bring-up prerequisite list, then stop for
>   student review.

Evidence to save:

- Commands run and their results.
- The lifecycle, cancellation, stop, ownership, and dependency checks performed.
- Android Studio Sync/Make results.
- A list separating software-verified behavior from hardware-unverified behavior.
- The final Session 2 `IMPLEMENTATION_STATUS.md` diff.

### LP-08 v1.0 — Record hardware configuration and stage the safety gates

Use this only after LP-07 is reviewed and before any powered Pedro test:

> - Show the current branch name, compare it with the progress record, ask the student to confirm it
>   is their intended localization and pathing branch, and wait. Do not create or switch branches.
> - Reinspect `AGENTS.md`, the architecture and implementation documents, the LP-06/LP-07 evidence,
>   the actual Team A Pedro classes, and the resolved Pedro version. Run the baseline TeamCode build.
> - Re-read the current official Pedro mecanum setup, Pinpoint, localization, tuning, dashboard, and
>   installation guidance. Cite official sources and distinguish library code from Quickstart files
>   that are not supplied by the Maven dependency.
> - Explain the current sequencing problem in plain language: the pilot requires tuning to be marked
>   verified before it creates a follower, but a follower is needed to perform localization checks
>   and tuning. Recommend the smallest beginner-readable staged readiness design that solves this
>   without weakening the final path-following gate.
> - Before editing configuration code, ask one required question at a time for repository and robot
>   facts that cannot be discovered safely: exact Driver Station hardware configuration names for
>   all four motors and Pinpoint, motor/controller arrangement, Pinpoint I2C port, pod model, pod
>   connections, current pod-location measurements, measured robot mass, and the agreed starting
>   pose. Record who measured or verified each fact. A fact that is not needed for safe initialization
>   may remain explicitly unknown for a later tuner; do not invent it. For pod offsets, ask whether
>   the team will use manual measurements or Pedro's version-matched offset tuner. Temporary zero
>   offsets required by that tuner are test setup, not verified final offsets. Do not request or store
>   passwords, network credentials, serial numbers, or unrelated personal information.
> - Do not treat motor directions, encoder directions, offsets, or tuning values as verified merely
>   because they were entered. Mark each as recorded, physically verified, tuned, or still unknown.
> - After the facts are supplied, make the smallest team-specific changes needed to hold those real
>   values and separate at least these permissions: safe follower/localizer initialization,
>   restricted manual-drive testing, and path following after tuning. Initialization may enable
>   localization while commanding zero drive output; it must not silently enable powered pathing.
> - Keep `TeamARobot` unchanged, retain one drivetrain owner, keep Pedro types team-specific, and do
>   not add a path, competition OpMode, Ivy, scheduler, or sample physical values.
> - Add focused software checks when the repository supports them. Assemble `TeamCode`, run the
>   Windows Gradle build and `git diff --check`, inspect the complete diff, and update
>   `IMPLEMENTATION_STATUS.md`. Update `ARCHITECTURE.md` only if the staged gates change approved
>   design intent. Stop before deploying to or powering a robot.

Evidence to save:

- A table of required values showing source, unit, and status: recorded, verified, tuned, or unknown.
- A class flow showing where each readiness gate is enforced.
- Proof that localization initialization cannot also authorize path following.
- Proof that the simple Team A Robot is unchanged and the Pedro composition has one drivetrain owner.
- Build results, documentation diffs, and every fact still required for LP-09.

### LP-09 v1.0 — Verify Pinpoint pose and restricted manual-drive safety

Use this only after LP-08 is reviewed and its initialization gate is satisfied:

> - Show the current branch name, compare it with the progress record, ask the student to confirm it
>   is their intended localization and pathing branch, and wait. Do not create or switch branches.
> - Reinspect the staged gates, actual Robot APIs, testing OpModes, input flow, and current official
>   Pedro Pinpoint/localization guidance. Run the baseline TeamCode build before editing.
> - Ask the student to confirm adult supervision, a stable robot and wiring inspection, a clear test
>   area, an approved raised-wheel setup for the motor-direction check, a charged battery, and one
>   named person ready to press Driver Station **STOP**. Ask one item at a time and mark the prompt
>   `Blocked` if a safe prerequisite is unavailable.
> - Add only the narrow team-specific configuration factory and testing/diagnostic OpMode needed for
>   this checkpoint. The OpMode must create `TeamAPedroRobot`, use its public API and neutral pose,
>   call `robot.update()` once per loop, show clear telemetry, and call `robot.stop()` on stop. It
>   must not access motors, Pinpoint, the follower, the drive FSM, or `HardwareMap.get(...)` directly.
> - Sync and assemble in Android Studio, run the Windows Gradle build, and have the student deploy
>   normally from Android Studio. If a USB driver or protected component requests administrator
>   access, cancel and ask teacher/IT staff; do not work around the restriction.
> - Begin with drivetrain output disabled. Have the student move the robot by hand forward, left, and
>   through a measured rotation while recording expected and observed x, y, and heading changes.
>   Check Pinpoint connection, pod selection, coordinate signs, approximate distance, return-to-start
>   error, and pose availability. Change only one configuration fact per retest and save telemetry.
> - Only after localization behaves consistently, use the raised-wheel setup and a deliberately
>   restricted power to check forward, strafe, rotate, cancel, Driver Station **STOP**, and Robot
>   stop. State the expected wheel/motion direction and stop plan before each action. Never reach
>   into the powered drivetrain.
> - Promote readiness gates only for checks the student actually observed. Do not enable path
>   following. Run `git diff --check`, review the complete diff, and update
>   `IMPLEMENTATION_STATUS.md` with passes, failures, measurements, telemetry, and remaining TODOs.

Evidence to save:

- The safety setup and named stop method used for every powered check.
- Expected-versus-observed pose changes and measured return-to-start error.
- Expected-versus-observed wheel or chassis motion at restricted power.
- Evidence that cancel, Driver Station STOP, and `robot.stop()` remove motor output.
- Gate changes supported by observation, build results, and the first unresolved hypothesis if blocked.

### LP-10 v1.1 — Collect Pedro tuning evidence without bypassing safety

Use this only after LP-09 is reviewed and localization plus restricted manual drive are verified:

> - Show the current branch name, compare it with the progress record, ask the student to confirm it
>   is their intended localization and pathing branch, and wait. Do not create or switch branches.
> - Reinspect the resolved Pedro version, team configuration, staged gates, and official tuning order.
>   Determine whether the required tuning entry point is present in the dependency or must come from
>   a version-matched official Quickstart. Do not paste a current or older `Tuning.java` until its
>   compatibility with the pinned APIs is verified.
> - Explain the reasonable supported tuning approaches and ask the team to choose when a decision is
>   required. Preserve the repository's Robot/subsystem lifecycle and single drivetrain owner; do not
>   introduce Ivy or let a tuning OpMode and `TeamAPedroRobot` initialize the same motors together.
> - If tuning support must be added or adapted, keep it team-specific or under testing entry points,
>   make the smallest reviewable change, and ensure FTC OpModes do not directly own normal robot
>   hardware behavior. Build and review that increment before any movement.
> - Reconfirm adult supervision, clear travel space, Driver Station STOP responsibility, maximum
>   power, expected distance, and stop condition before every tuner that can move the robot. Inspect
>   the exact tuner defaults first; never run a full-power or long-distance default blindly.
> - Follow the official order explicitly: accept setup and mass evidence, accept localization,
>   measure forward and lateral velocity, tune heading, tune the selected drive algorithm, then run
>   its validation tests and select path-end constraints. Do not run a later category merely because
>   it appears earlier in the Driver Station menu. Record measured mass, forward/lateral velocities,
>   braking or zero-power acceleration values, heading/translational/drive correction values, and
>   path-end constraints only when the corresponding test produced usable evidence. Change and test
>   one category at a time; preserve raw observations and units.
> - A build, copied sample, or first motion does not make tuning verified. Keep the path-following
>   gate closed if required tests are unsafe, incomplete, oscillatory, inconsistent, or unexplained.
>   Record one next hypothesis and stop instead of trying several changes at once.
> - After each accepted value, assemble `TeamCode`, run the Windows Gradle build and
>   `git diff --check`, review the complete diff, and update `IMPLEMENTATION_STATUS.md`. Open the
>   final path-following gate only after the student and supervising adult accept the required tuning
>   evidence and restricted-power limit.

Evidence to save:

- The exact version-matched tuning source or API evidence used.
- A safety and results log for each powered tuner, including units and repeatability.
- A before/after table for each accepted value; never save unexplained sample constants as results.
- Build results and proof that only one drivetrain owner exists during each tuning composition.
- Whether the path-following gate remains closed or the evidence that allowed it to open.

### LP-11 v1.1 — Build, run, and document one cautious pilot path

Use this only after LP-10 is reviewed and the path-following gate is explicitly open:

> - Show the current branch name, compare it with the progress record, ask the student to confirm it
>   is their intended localization and pathing branch, and wait. Do not create or switch branches.
> - Reinspect the approved architecture, actual neutral path APIs, Team A Pedro adapter, autonomous
>   interfaces, resolved Pedro path-building APIs, and current official visualizer and PathChain
>   guidance. Run the baseline TeamCode build before editing.
> - Open the official web visualizer at `https://visualizer.pedropathing.com/`. If it is unavailable
>   or browser access is denied, record LP-11 as Blocked and stop; do not invent a project/export or
>   treat handwritten coordinates as visualizer evidence. In the visualizer, have the student create
>   one short, unobstructed pilot path whose
>   start pose, end pose, heading behavior, maximum power, and field clearance match the available
>   test area. Save the visualizer project/export as team-owned design evidence; do not run generated
>   code without reviewing and adapting it.
> - Add the path definition under `robots.teamA` and wrap it behind the existing neutral request.
>   Add only the narrow non-blocking autonomous step/control role needed to start once, check
>   completion on later loops, and cancel safely. Keep the testing OpMode thin: it may initialize the
>   Robot, advance the step/sequence, call `robot.update()` once per loop, report telemetry, and stop;
>   it must not own the follower, motors, Pinpoint, or drive FSM. Do not add Ivy or another scheduler.
> - Build and inspect the complete diff before deployment. Reconfirm adult supervision, clear space,
>   restricted path power, starting-pose placement, Driver Station STOP responsibility, and the
>   maximum allowed travel before the first ground test.
> - Run the path once at restricted power. Record expected and observed start/end pose, path state,
>   completion, position and heading error, cancel behavior, stop behavior, and any unexpected motion.
>   Stop immediately for wrong direction, unstable localization, oscillation, excessive speed, an
>   unavailable pose, or travel outside the approved area.
> - Change only one cause per retest. If the path cannot be demonstrated safely, preserve telemetry
>   and record one testable next hypothesis; a well-documented failure is an acceptable pilot result.
> - Run the Android Studio and Windows builds plus `git diff --check`. Reconcile
>   `ARCHITECTURE.md` only if approved design intent changed, and reconcile
>   `IMPLEMENTATION_STATUS.md` with actual files and observed hardware results. Do not claim more
>   accuracy or reliability than the test demonstrated.

Evidence to save:

- The visualizer design/export and reviewed team-specific path definition.
- The final Robot/subsystem/step/OpMode relationship flow.
- The complete safety plan and expected-versus-observed path telemetry.
- Cancel, stop, completion, pose-error, and restricted-power results.
- Final builds and documentation diffs, plus either a demonstrated pilot or one next hypothesis.

## Working rules for later prompts

- Begin each prompt by showing the current branch, comparing it with the progress record, asking the
  student to confirm it is the one branch intended for all localization and pathing work, and waiting
  for the answer. Never create or switch a branch for the student.
- Write every student prompt as a concise bulleted list rather than a dense paragraph.
- Before executing any prompt, require the assistant to summarize its work as bullets, identify
  whether changes are allowed, ask whether the student understands and wants to proceed, and wait
  for confirmation.
- Ask for one small, reviewable outcome at a time.
- Require official, current documentation for vendor APIs and dependency versions.
- Require a baseline build before edits and a build plus `git diff --check` afterward.
- Require every code-changing prompt to update `IMPLEMENTATION_STATUS.md` in the same change, using
  evidence from the actual diff and validation rather than planned class names.
- Update `ARCHITECTURE.md` only when approved design intent changes; do not rewrite it as an activity
  log.
- Never ask the assistant to invent motor directions, pod offsets, encoder resolution, PID values,
  mass, or other physical constants.
- Stop when prerequisites or the baseline build fail; do not mix unrelated repairs into the task.
- Do not commit, push, or merge until the team has reviewed the changes.

## Prompt change log

| Date | Prompt | Version | Change |
| --- | --- | --- | --- |
| 2026-08-02 | LP-01 | 1.0 | Initial architecture and ownership discovery prompt recorded. |
| 2026-08-02 | LP-02 | 1.0 | Added the approved-architecture documentation gate. |
| 2026-08-02 | Guide | 1.0 | Added the self-contained prompt for starting a fresh student chat. |
| 2026-08-02 | LP-01 | 1.1 | Reformatted as bullets and added the student confirmation gate. |
| 2026-08-02 | LP-02 | 1.1 | Reformatted as bullets and added the student confirmation gate. |
| 2026-08-02 | Guide | 1.1 | Start prompt now previews each prompt and waits for student approval. |
| 2026-08-02 | Guide | 1.2 | Required beginner-friendly results and guided resolution of questions that block the next prompt. |
| 2026-08-02 | LP-03 | 1.0 | Added the read-only Pedro dependency and Android environment compatibility gate. |
| 2026-08-02 | LP-04 | 1.0 | Added pinned dependency installation, Windows, Android Studio, and status-update steps. |
| 2026-08-02 | LP-05 | 1.0 | Added optional library-neutral seams while preserving simple robot compositions. |
| 2026-08-02 | LP-06 | 1.0 | Added team-specific Pedro mecanum and Pinpoint pilot integration guidance. |
| 2026-08-02 | LP-07 | 1.0 | Added software-only validation and Session 2 status reconciliation. |
| 2026-08-02 | Guide | 1.3 | Added non-admin Windows rules, UAC stop conditions, and teacher/IT escalation guidance. |
| 2026-08-02 | LP-03 | 1.1 | Added JDK 17 verification and permission classification to compatibility discovery. |
| 2026-08-02 | LP-04 | 1.1 | Added per-user setup, UAC cancellation, and Gradle JVM 17 checks. |
| 2026-08-02 | Guide | 1.4 | Added a durable branch-local prompt-progress record for new chats and student handoffs. |
| 2026-08-02 | LP-01 | 1.2 | Allowed only the required progress-status record during read-only discovery. |
| 2026-08-02 | LP-03 | 1.2 | Allowed only the required progress-status record during read-only compatibility discovery. |
| 2026-08-02 | Guide | 1.5 | Added student confirmation and durable recording of the already-checked-out working branch. |
| 2026-08-02 | LP-03 | 1.3 | Added explicit student confirmation of the localization and pathing branch. |
| 2026-08-02 | LP-04 | 1.2 | Added explicit student confirmation of the localization and pathing branch. |
| 2026-08-02 | LP-05 | 1.1 | Added explicit student confirmation of the localization and pathing branch. |
| 2026-08-02 | LP-06 | 1.1 | Added explicit student confirmation of the localization and pathing branch. |
| 2026-08-02 | LP-07 | 1.1 | Added explicit student confirmation of the localization and pathing branch. |
| 2026-08-02 | Guide | 1.6 | Start Here now generates a fresh five-question learning check after every LP result without storing questions, scores, or answer history. |
| 2026-08-02 | LP-05 | 1.2 | Clarified that selecting a pilot team does not change or replace its existing Robot class. |
| 2026-08-02 | LP-06 | 1.2 | Required a separate team-specific Pedro Robot composition beside the unchanged simple Robot. |
| 2026-08-02 | LP-06 | 1.3 | Made the Android Studio build checkpoint work with current Assemble Module wording and older Make Project wording. |
| 2026-08-02 | LP-07 | 1.2 | Made final Android Studio validation use version-neutral TeamCode module assembly wording. |
| 2026-08-02 | Guide | 1.7 | Changed each five-question learning check to ask, evaluate, and explain one question at a time. |
| 2026-08-02 | LP-08 | 1.0 | Added measured configuration intake and staged initialization, manual-drive, and pathing gates. |
| 2026-08-02 | LP-09 | 1.0 | Added supervised Pinpoint pose and restricted manual-drive hardware checks. |
| 2026-08-02 | LP-10 | 1.0 | Added version-matched, evidence-based Pedro tuning with per-test safety gates. |
| 2026-08-02 | LP-11 | 1.0 | Added external path design, thin autonomous integration, cautious test, and final reconciliation. |
| 2026-08-02 | Guide | 1.8 | Added the minimum physical robot and safety prerequisites before Session 3 may begin. |
| 2026-08-02 | Guide | 1.9 | Start Here now withholds LP-08 until students acknowledge every minimum-hardware requirement and understand the risk of invented values. |
| 2026-08-02 | LP-10 | 1.1 | Made Pedro's setup, localization, velocity, heading, drive-algorithm, and validation order explicit instead of relying on Driver Station menu order. |
| 2026-08-02 | LP-11 | 1.1 | Added the official Visualizer URL and a fail-closed rule when the site or browser is unavailable. |

Later prompts and revisions will be added here as the mentor dry-run reveals the smallest useful
learning steps.

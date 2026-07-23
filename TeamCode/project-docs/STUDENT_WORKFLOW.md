# PVI-FTC Sequential Codex Student Workflow
## Purpose
Each student completes one numbered prompt in a fresh feature branch and a fresh Codex session. The
repository is the shared memory between sessions.
## Rule: one architecture prompt at a time
Do not begin Prompt N+1 until Prompt N has been reviewed and merged into main. Students may review
together, but dependent implementation branches are serialized.
## Before creating a branch
1. Open a terminal in the repository (NOTE: the git commands below can be replaced by use of buttons
   in Android Studio.
2. Run:
   git switch main
   git pull origin main
   git status
   git log --oneline -5
3. The working tree must be clean.
4. Read:
- AGENTS.md
- TeamCode/project-docs/ARCHITECTURE.md
- TeamCode/project-docs/IMPLEMENTATION_STATUS.md
5. Confirm the preceding prompt appears in main and the documented build passes.
## Create the assigned branch
Use:
git switch -c prompt-NN/short-description
Always branch from updated main. Never branch from another student’s feature branch.
## Use Codex
1. Start a new Codex session in the repository.
2. Submit the assigned hardened prompt from the master instructions.
3. Allow Codex to perform preflight checks.
4. If Codex reports missing prerequisites or a failing baseline build, stop and involve the
   reviewer. Do not tell Codex to improvise around the problem.
5. Do not ask Codex to commit or merge during implementation.
## Student review
Run:




git status
git diff --check
git diff
Read every changed file. Confirm:
- only expected files changed;
- generated code is understandable;
- no SDK source was modified unnecessarily;
- dependency boundaries are preserved;
- no duplicate responsibility was introduced;
- IMPLEMENTATION_STATUS.md was updated accurately.
## Request a non-editing Codex review
Ask Codex:
Review the current branch against the assigned prompt, AGENTS.md,
TeamCode/project-docs/ARCHITECTURE.md, TeamCode/project-docs/IMPLEMENTATION_STATUS.md, and existing
public APIs.
Do not edit yet. Report missing requirements, unnecessary changes,
architecture violations, beginner-readability concerns, build gaps, and API
changes that affect the next prompt.
After reviewing the report, explicitly request only necessary corrections.
## Independent build
Run the exact build command documented in IMPLEMENTATION_STATUS.md. Do not rely solely on Codex’s
build claim.
## Commit
Stage only intended files. Example:
git add TeamCode docs AGENTS.md
git status
git commit -m &quot;Prompt 04: add hardware abstraction layer&quot;
git push -u origin prompt-04/hardware-abstraction
## Pull request
The pull request must include:
- prompt number and title;
- concise changes summary;
- build command and result;
- architecture checks performed;
- known limitations;
- confirmation that the student reviewed the complete diff.
  At least one software lead, mentor, or assigned peer reviewer must approve.
## Merge





Prefer squash merge so main contains one readable commit per prompt. Delete the merged branch.
## Handoff
The next student starts by pulling the newly merged main and repeats this workflow. No private Codex
conversation is considered part of the handoff.

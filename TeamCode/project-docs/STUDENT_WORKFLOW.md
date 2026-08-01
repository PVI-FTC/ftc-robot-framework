# PVI-FTC Sequential Codex Student Workflow
## Purpose
Each student completes one prompt in a fresh feature branch and a fresh Codex session. The
repository is the shared memory between sessions.
## Rule: one architecture prompt at a time
Do not begin Prompt N+1 until any dependent Prompt N has been reviewed and merged into main. Students may review
together, but dependent implementation branches are serialized.
## Before creating a branch
1. Open a terminal in the repository (NOTE: the git commands below can be replaced by use of buttons
   in Android Studio by pulling the latest main branch.)
2. Run:
   git switch main
   git pull origin main
   git status
   git log --oneline -5
3. The working tree must be clean.
4. Read these files for understanding:
- [AGENTS.md](../../AGENTS.md)
- [ARCHITECTURE.md](ARCHITECTURE.md)
- [IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md)
5. Confirm any necessary preceding prompt appears in main and the documented build passes.
## Create the assigned branch
Use:
git switch -c issue-NN/short-description

Or use Android Studio to switch to the new branch

Always branch from updated main. 

Never branch from another student’s feature branch.
## Using Codex
1. Start a new Codex session in the repository.
2. Review the prompt tasks with a mentor before submitting it:
```Read AGENTS.md and follow all repository rules.
   Read ARCHITECTURE.md and preserve the documented architecture.
   Task: <enter your specific feature, bug, troubleshooting request, or question here>
   ``` 
3. Make sure Codex performs preflight checks by using this format for all prompts:
4. If Codex reports missing prerequisites or a failing baseline build, stop and involve the
   reviewer. Do not tell Codex to improvise around the problem.
5. Do not ask Codex to commit or merge during implementation. 
6. Do not edit yet. 
7. Report missing requirements, unnecessary changes, architecture violations, beginner-readability concerns, build gaps, and API
  changes that affect future enhancements. 
8. After reviewing the report, explicitly request only necessary corrections.

## Student review
Run (or use the Android Studio commit view):

```agsl
git status
git diff --check
git diff
```
Read every changed file. Confirm:
- only expected files changed;
- generated code is understandable;
- no SDK source was modified unnecessarily;
- dependency boundaries are preserved;
- no duplicate responsibility was introduced;
- IMPLEMENTATION_STATUS.md was updated accurately.

## Independent build
Run the exact build command documented in IMPLEMENTATION_STATUS.md or using the Android build (hammer icon). 

Do not rely solely on Codex’s
build claim.

## Commit
Stage only intended files. 

Check the Commit tab in Android Studio

Example for terminal command line:
```
git add TeamCode docs AGENTS.md
git status
git commit -m "Prompt 04: add hardware abstraction layer"
git push -u origin prompt-04/hardware-abstraction
```
## Pull Request (PR)
The pull request must include:
- prompt or issue number and title;
- concise changes summary;
- build command and result;
- architecture checks performed;
- known limitations;
- confirmation that the student reviewed the complete diff.
- At least one software lead, mentor, or assigned peer reviewer must approve.

## Merge

Prefer squash merge so main contains one readable commit per prompt. 

Delete the merged branch after it has been merged into main.

## Handoff
The next student starts by pulling the newly merged main and repeats this workflow. 

No private Codex conversation is considered part of the handoff.

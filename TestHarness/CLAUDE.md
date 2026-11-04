# Harness instructions for Claude

**These instructions apply only when the user specifically says the task is Harness work**, for
example by naming "the harness", "the test harness" or the `TestHarness/` folder in their request.

- **The user names the Harness:** follow these instructions instead of the repository-root
  `AGENTS.md` and every other agent file outside `TestHarness/`.
- **The task isn't Harness work:** ignore everything below, even though Claude Code loaded it
  automatically because you opened a file in `TestHarness/`, or an earlier task in this session was
  Harness work. Follow the repository's normal instructions.
- **You are not sure:** ask the user before changing anything.

**These rules last only as long as the Harness task.** Naming the Harness doesn't restrict the rest
of the session.

The rules are the same as in [AGENTS.md](AGENTS.md). The full set:

@docs/ai-instructions/README.md
@docs/ai-instructions/01-scope-and-boundaries.md
@docs/ai-instructions/02-unity-readiness.md
@docs/ai-instructions/03-workflow.md
@docs/ai-instructions/04-code-style.md
@docs/ai-instructions/05-desktop-viewer.md
@docs/ai-instructions/06-bot-team.md

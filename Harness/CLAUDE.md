# Harness instructions for Claude

**These instructions apply only when the user specifically says the task is Harness work**, for
example by naming "the harness", "the test harness" or the `Harness/` folder in their request.

- **The user names the Harness:** follow these instructions instead of the repository-root
  `AGENTS.md` and every other agent file outside `Harness/`.
- **The user has not named the Harness at any point in this session:** ignore everything below,
  even though Claude Code loaded it automatically because you opened a file in `Harness/`. Follow
  the repository's normal instructions.
- **You are not sure:** ask the user before changing anything.

**Once the Harness has been named, the rest of the session is a Harness session.** Until the
session ends, add, edit or delete nothing outside `Harness/`, even if a later request doesn't
mention the Harness or asks for an outside change. Tell the user what would need changing and
suggest a new session for it. Keep "this is a Harness session" in any summary of the
conversation.

The rules are the same as in [AGENTS.md](AGENTS.md). The full set:

@ai-instructions/README.md
@ai-instructions/01-scope-and-boundaries.md
@ai-instructions/02-unity-readiness.md
@ai-instructions/03-workflow.md
@ai-instructions/04-code-style.md
@ai-instructions/05-desktop-viewer.md

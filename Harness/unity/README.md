# Harness Unity project

A Unity project for watching harness runs in 3D. It is part of the FTC test harness and follows
the rules in [`../ai-instructions/`](../ai-instructions/README.md), especially
[02-unity-readiness.md](../ai-instructions/02-unity-readiness.md).

**Status:** set up only (Stage U1, step U1.1 in [`../PROGRESS.md`](../PROGRESS.md)). There is no
scene or replay code yet. Stage U1 will replay the run recordings the harness saves in
`Harness/build/recordings/` (the same `.json` files the Swing viewer opens). Nothing here talks
to the harness live yet; that is Stage U2.

## Open it

1. Install **Unity 6000.3.14f1** with Unity Hub (it is already installed on the team laptop) and
   sign in to Unity Hub once (the free Personal licence is enough).
2. In Unity Hub: **Add → Add project from disk**, and pick this `Harness/unity` folder.
3. Open it. The first time, Unity downloads the packages and builds its `Library/` folder, which
   takes a few minutes.

## What is in it

| Package | Why |
|---|---|
| `com.unity.nuget.newtonsoft-json` | Reads the run recording JSON. Unlike Unity's built-in `JsonUtility`, it keeps `null` values (a pose that wasn't tracked) instead of turning them into zeros. |
| `com.unity.test-framework` | Unity's own tests for the replay code (run from **Window → General → Test Runner**). |
| `com.unity.ai.assistant` | Unity's AI Assistant. Its MCP bridge lets an AI assistant (Claude Code's `unity-mcp` tool) look at and change this project while the editor is open. Optional: remove it from `Packages/manifest.json` if you don't want it. |
| `com.unity.ugui`, `com.unity.ide.visualstudio` | On-screen panels; opening scripts in Visual Studio. |

## What git keeps

Only `Assets/`, `Packages/manifest.json`, `Packages/packages-lock.json` and `ProjectSettings/`
are kept. Unity's generated folders (`Library/`, `Temp/`, `Logs/`, `obj/`, `UserSettings/`,
`Build/`) are ignored in `Harness/.gitignore`; Unity rebuilds them.

## Units

The harness calculates in FTC units (inches, heading counter-clockwise from +x). All conversion
to Unity units happens in **one** C# class, never spread through the scripts
([02-unity-readiness.md](../ai-instructions/02-unity-readiness.md) §6).

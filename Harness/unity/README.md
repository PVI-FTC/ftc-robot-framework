# Harness Unity project

A Unity project for watching harness runs in 3D. It is part of the FTC test harness and follows
the rules in [`../ai-instructions/`](../ai-instructions/README.md), especially
[02-unity-readiness.md](../ai-instructions/02-unity-readiness.md).

**Status:** Stage U1 (replay) works: it replays the run recordings the harness saves in
`Harness/build/recordings/` (the same `.json` files the Swing viewer opens). Nothing here talks
to the harness live yet; that is Stage U2. See [`../PROGRESS.md`](../PROGRESS.md).

## Open it

1. Install **Unity 6000.3.14f1** with Unity Hub (it is already installed on the team laptop) and
   sign in to Unity Hub once (the free Personal licence is enough).
2. In Unity Hub: **Add → Add project from disk**, and pick this `Harness/unity` folder.
3. Open it. The first time, Unity downloads the packages and builds its `Library/` folder, which
   takes a few minutes.

## Watch a recording

1. Make sure there are recordings: run the harness tests (`.\gradlew.bat test` in `Harness/`), or
   save a run from the Swing viewer.
2. Open `Assets/HarnessReplay/Replay.unity` and press **Play**. The newest recording opens.

The window is laid out like the Swing viewer, in the same dark "mission control" style: the
**FTC HARNESS** header with the mission clock (`T+ 00:02.00`) and stage, the **Replay** chooser
(click it to pick a recording; **Replays...** opens any file), the field with its caption and
legend, the cards on the right (Vehicle, Position, Auto step, Motors, Driver Station, Events), both
gamepads with held buttons lit, and the playback bar at the bottom (buttons, Speed, time, slider).
The field starts in a straight-down view that looks like the Swing field; T switches to an angled
3D view. Wheels light cyan when driving forward and amber in reverse, like the Swing robot.

3. Controls (the same as the Swing viewer where they overlap):

| Key / mouse | Does |
|---|---|
| Space | Play / pause (at the end, starts over) |
| Left / Right | One loop back / forward (20 ms) |
| Shift + Left / Right | One second back / forward |
| Home / End | Start / end |
| `[` or `-` / `]` or `+` | Slower / faster (0.25x to 4x) |
| N / P | Next newer / older recording in `build/recordings` |
| T | Switch between top-down (the default, FTC +y up the screen, like the Swing viewer) and angled 3D |
| F | Follow the robot |
| Right-drag, scroll (over the field) | Turn the camera (switches to 3D), zoom |

The slider jumps anywhere in the run; the Speed box picks 0.25x to 4x. The Driver Station card
shows the screen **as it was at that moment**, like the Swing viewer. The colors and fonts are in
`HudTheme.cs`; keep them in step with the Swing viewer's `Theme.java`.

To pick a fixed recording instead of the newest, select **Harness Replay** in the scene and set
**Recording Path** in the Inspector.

## Drive live (Stage U2)

Watch the real TeamCode run **while you drive**, with your game controller:

1. In `Harness\`: `.\gradlew.bat unityBridge` (leave it running).
2. Plug in the controller (Xbox, PlayStation or Logitech in X mode; USB or Bluetooth).
3. In Unity, press Play, open the **Replay** chooser and pick **Live: Team A TeleOp** (or another).
   The header shows **LIVE**; the line under the chooser says what to press and which controller
   drives each gamepad.
4. **Enter** = PLAY, **Esc** = STOP (or the buttons in the bottom bar). The run also stops by
   itself at the end of the match.
5. **Save** keeps the run in `build\recordings` (as `unity-live-...json`) to replay later, here or
   in the Swing viewer.

The harness reads the controller (the same code as the Swing viewer's live drive); Unity only
asks for one OpMode call every 20 ms and draws it. With one controller, its D-pad also works the
operator's face buttons. If a controller disconnects, every input is released at once and the
moment shows under Events. **Harness → Live Smoke Test** checks the connection without a
controller (needs `unityBridge` running) and writes `Harness/build/unity-live-smoke.txt`.

## Season fields

The **Field:** button (right of the Replay chooser) or **G** switches between the plain tiled field
and each season in `Harness/fields/` (BIOBUZZ 2026–2027, DECODE 2025–2026). Unity remembers the
choice. `-field biobuzz-2026` on the app's command line picks one at start, and a recording whose
header has `"field"` opens that one. BIOBUZZ shows:
- the HIVE frame, with both HIVES in their starting tilt
- the four FLOWERS
- the loading zones, gardens and alliance areas
- all 16 AprilTags under the cells

DECODE shows only its two goal AprilTags (its field parts aren't in the file yet). The file format and
where every number came from: `Harness/README.md`, "Season field files". The code is `FieldLayout.cs`
(reading and picking) and `FieldBuilder.BuildSeason` (drawing).

## Open it without the Editor (the Windows app)

The replay also runs as a normal Windows app, `Build/HarnessReplay.exe`, so anyone can watch a
recording without opening Unity:

- **Double-click `Harness\unity-view.bat`**: opens the newest recording in `build\recordings`.
- **Drag a recording (`.json`) onto `unity-view.bat`**: opens that one.
- Live play works the same as in the Editor: start `.\gradlew.bat unityBridge` first.

The app has everything above except **Replays...** (that file box only exists in the Editor): pick
recordings from the **Replay** chooser or with N / P instead.

**Building it** (once, and again after any change to the Unity scripts): close the Unity Editor
and double-click `Harness\buildUnity.bat`, or in the Editor use **Harness → Build Windows App**.
It takes a few minutes and needs Unity installed; running the app needs nothing installed.
`unity-view.bat` offers to build it if it's missing. `buildUnity.bat` looks for Unity in
`C:\Program Files\Unity\Hub\Editor\6000.3.14f1\`; set `UNITY_EXE` if it is somewhere else. The
build's log is `Harness/build/unity-build.log`.

How the app finds the recordings (`RecordingLoader.ResolveFolder`, tested in
`RecordingFolderTests`), first match wins:

1. `-recordings <folder>` on the command line (`unity-view.bat` always passes `Harness\build\recordings`).
2. The folder of a `.json` file on the command line.
3. Next to the app: `Harness/unity/Build/` → `Harness/build/recordings`. In the Editor:
   `Harness/unity/Assets` → `Harness/build/recordings`.

`Build/` is about 90 MB and ignored by git; never commit it. The build adds four shaders to
**Project Settings → Graphics → Always Included Shaders**, because the field is built from code and
a build otherwise leaves them out (the field turns magenta).

## Tests

**Harness → Run Replay Tests** (or Window → General → Test Runner → EditMode → Run All). The menu
item also writes a summary to `Harness/build/unity-test-results.txt`. One test checks Team A's
autonomous against `build/recordings/viewer-teamA-autonomous.json`; it is skipped until the
harness tests have made that file.

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

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
| 1–7 (or the View buttons above the field) | Preset viewpoints, with a smooth move: 1 Audience (behind the audience wall), 2 Red drive team, 3 Blue drive team (eye height, from each alliance area), 4 Top-down, 5 Corner (high, from the red/audience corner), 6 HIVE close-up (low, looking up at the cells and their AprilTags), 7 Robot chase (behind the robot, turning with it). Turning or zooming afterwards starts from that view |
| F | Follow the robot |
| Right-drag, scroll (over the field) | Turn the camera (switches to 3D), zoom |

The slider jumps anywhere in the run; the Speed box picks 0.25x to 4x. The Driver Station card
shows the screen **as it was at that moment**, like the Swing viewer. The colors and fonts are in
`HudTheme.cs`; keep them in step with the Swing viewer's `Theme.java`.

To pick a fixed recording instead of the newest, select **Harness Replay** in the scene and set
**Recording Path** in the Inspector.

## Drive live (Stage U2)

Watch the real TeamCode run **while you drive**, with your game controller:

1. Plug in the controller (Xbox, PlayStation or Logitech in X mode; USB or Bluetooth).
2. Open the replay (the Windows app via `unity-view.bat`, or press Play in the Editor) and click
   **Live mode** (or press **L**), right of the chooser. It starts the harness side for you in the
   background: the lamp goes amber while it starts (a few seconds; up to a minute the first time),
   then green.
3. Open the **Replay** chooser and pick **Live: Team A TeleOp** (or another).
   The header shows **LIVE**; the line under the chooser says what to press and which controller
   drives each gamepad.
4. **Enter** = PLAY, **Esc** = STOP (or the buttons in the bottom bar). The run also stops by
   itself at the end of the match.
5. **Save** keeps the run in `build\recordings` (as `unity-live-...json`) to replay later, here or
   in the Swing viewer.

The harness reads the controller (the same code as the Swing viewer's live drive); Unity only
asks for one OpMode call every 20 ms and draws it. With one controller, its D-pad also works the
operator's face buttons. If a controller disconnects, every input is released at once and the
moment shows under Events.

Clicking **Live mode** again (or closing the app) ends the run and the harness side it started.
If you started `.\gradlew.bat unityBridge` yourself, Live mode just uses it and leaves it running.
If it can't start, the button says **Failed** and the message names the reason and the log,
`Harness/build/unity-bridge.log`. It needs the whole `Harness` folder (it runs `gradlew.bat`) and
Java: `JAVA_HOME`, or Android Studio's Java, which it finds by itself.

**Harness → Live Smoke Test** checks the connection without a controller (needs `unityBridge`
running). **Harness → Live Mode Smoke Test** does the same, but lets the Live mode launcher start and
stop the harness side. Both write `Harness/build/unity-live-smoke.txt`.

### Unity's physics (Stage U3)

Set **Physics:** (right of the chooser, or press **U**) to **Unity** before you pick a live OpMode,
and Unity's physics engine moves the robot instead of the harness's own physics. The chooser
title then ends in "Unity physics".
- The robot speeds up, slows down and coasts the same way as with the harness's physics
  (`DriveLaw.cs`, a copy of the harness's `MomentumPhysics`).
- The walls and the solid parts of the field shown now (BIOBUZZ's HIVE frame and FLOWERS) really
  stop the robot, and it slides along them. CELLS high above the floor don't block it.
- The robot stays flat on the floor, and game pieces aren't simulated yet.
- The choice is remembered. It can change only between live runs, and the field used for blocking
  is the one shown when the run starts.

The code is `UnityRobotPhysics.cs` (invisible PhysX colliders and a Rigidbody robot, stepped one
20 ms loop at a time) and `DriveLaw.cs`. The tests are in `UnityRobotPhysicsTests`.

## Visuals: arena and setting

The **Visuals** button (right of the Replay chooser) or **V** opens a screen over the field with
two choices. Both apply at once, are remembered, and V, Esc, **Done** or a click outside closes it.

- **Arena:** the plain tiled field, or a season field (below).
- **Setting**, the place around the arena:
  - **Mission control:** the dark, plain look of the Swing viewer. Nothing around the field.
  - **Competition hall:** a school gym on event day, with a wooden floor, bleachers full of fans,
    banners, drive-team stations at the red and blue walls, a scoreboard and ceiling lights.
  - **Night stadium:** a football field under four floodlight towers, with a running track,
    packed stands, goal posts, stars and the Moon.
  - **Moon base:** a landing pad on grey moon dust, with craters, rocks, habitat domes joined by
    tubes, an antenna dish, a rover, and Earth in a black sky.

The settings show best in the 3D view: press **T**, right-drag to turn, and scroll out (the zoom now
goes far enough to see the whole place). In the top-down view you see the ground around the field.
No setting ever puts anything over the field or gives it a collider, so the robot, the field parts,
the trail and Unity's physics are never affected. That's checked in `SettingBuilderTests`. The code is
`SettingBuilder.cs`: every shape, size and color is in plain code, and new settings go in
`SettingBuilder.All`.

**The settings' art** (added 2026-10-25, all free to use; see `Assets/HarnessReplay/Art/CREDITS.md`):

| Setting | 360° sky (3D view) | Textured surfaces | Detailed models |
|---|---|---|---|
| Competition hall | Poly Haven "Gym 01" (seen above the walls) | wood floor, cinder-block walls | laptops and gamepads on the drive-team tables, tool cart, tool chest |
| Night stadium | Poly Haven night sky | grass, red rubber track, concrete stands | (none yet) |
| Moon base | NASA's Deep Star Maps (the real sky with the Milky Way), NASA's Blue Marble Earth | moon dust, metal landing pad | NASA's Apollo Lunar Module, astronaut, Habitat Demonstration Unit and Space Exploration Vehicle (rover); scanned moon rocks |

- **Where it lives:** `Resources/Settings/<setting id>/`: `sky`, `textures/<surface>_color|_normal|_ao`,
  `models/<name>/<name>` (+ `color`, `normal`).
- **The code:** `SettingAssets.cs` loads it, puts textures on surfaces and sizes each model to its real size.
- **People:** real people from Mixamo (`Resources/People/`, 59 MB): four characters and five animations
  (sit, sit_clap, stand_clap, stand_idle, stand_idle_2). `People.cs` places them:
  - **Fans** (hall bleachers, and the lower 5 rows of each stadium stand) are frozen in a sitting or clapping
    pose. Each pose is captured once per character and shared, so a full stand stays fast.
  - **Drive teams** (hall) stand and play an idle animation, in their alliance's shirt color.
  - Every person gets a random character, shirt, trouser, shoe and hair color, a slightly different height and
    turn, so neighbours never look the same.
  - To add characters or animations: put the Mixamo FBX files in `Assets/HarnessReplay/PeopleSource/`
    (git-ignored), the 2K textures in `Resources/People/<chNN>/textures/`, and run **Harness → Build People**
    (`Editor/PeopleBuilder.cs`). Without the people files the settings go back to simple capsules.
- **Without the files** a setting simply keeps its plain shapes. `SettingAssetsTests` checks every file is
  there.
- **Import settings:** in `Editor/SettingArtImport.cs`. The top-down view keeps its plain dark background.

Start-up options (handy for demos): `-preset 2` (a viewpoint, 1–7), `-setting moon-base`, `-field biobuzz-2026`, `-view 3d`,
`-zoom 3` (times the normal distance), `-yaw 30` (degrees).

## The official field CAD

With BIOBUZZ chosen, the 3D view shows FIRST's official field CAD instead of simple shapes: the
foam tiles, the perimeter rails and glass, the HIVE A-frame with both HIVEs and their cells, the
four FLOWERs, the tape and the AprilTag plates, in the CAD's own colours. It replaces the plain dark
floor and low walls. Without the CAD files (for example in a fresh clone without them), the
simple shapes from `Harness/fields/biobuzz-2026.json` are drawn as before.

- **Files:** `Assets/HarnessReplay/Resources/Field/biobuzz-2026/*.obj` (12 parts, 1.2 M triangles,
  39 MB). The game pieces (POLLEN, red and blue NECTAR) are in `Resources/GamePieces/biobuzz-2026/`;
  they aren't placed on the field yet.
- **Where they came from:** the user's export of the official CAD (15.2 M triangles). WorkerBot split
  it into parts, slimmed it and turned it into the FTC field frame (inches, z up). How it did that is in
  `Assets/HarnessReplay/Art/biobuzz-2026-cad-manifest.json`; credits are in `Art/CREDITS.md`.
- **How it's drawn:** `FieldBuilder.BuildSeason` places every model in `Resources/Field/<field id>`
  with `FtcToUnity.FtcModelRotation` and inch-to-meter scaling. Import settings (calculated normals at
  40°, no compression) are in `Editor/FieldModelImport.cs`.
- **See-through walls and HIVEs:** the clear panels of the perimeter and both HIVEs (the CAD's light
  grey `E6E6E6` pieces) are drawn like clear plastic (`FieldBuilder.SeeThroughParts`, `PanelOpacity`
  0.11), so the robot and the pieces behind or inside them stay visible. The rails, the HIVE frame and
  the colored trim stay solid. They use `Legacy Shaders/Transparent/Specular` (`FieldBuilder.PanelShader`),
  which the build always includes: the Standard shader's Fade mode looked clear in the Editor but solid
  in the app, because the build left that shader version out.
- **Physics:** Unity's physics still uses the simple shapes from the field file, which now use the
  CAD's measurements.

## Season fields

**G** steps through the arenas without opening the Visuals screen: the plain tiled field
and each season in `Harness/fields/` (now only BIOBUZZ 2026–2027; DECODE 2025–2026 is switched off in
`Harness/fields/disabled/`, which isn't read). Unity remembers the
choice. `-field biobuzz-2026` on the app's command line picks one at start, and a recording whose
header has `"field"` opens that one. BIOBUZZ shows:
- the HIVE frame, with both HIVES in their starting tilt
- the four FLOWERS
- the loading zones, gardens and alliance areas
- all 16 AprilTags under the cells

The file format and
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

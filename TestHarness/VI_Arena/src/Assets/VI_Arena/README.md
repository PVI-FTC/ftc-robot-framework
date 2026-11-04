# VI Arena (Unity code)

Replays harness run recordings (`TestHarness/th_src/build/recordings/*.json`) in 3D (called
HarnessReplay until 2026-11-02). The full guide is `TestHarness/VI_Arena/README.md`; the stages are in
`TestHarness/docs/PROGRESS.md`. Open `Replay.unity` and press Play.

| Folder / file | What it does |
|---|---|
| `Replay.unity` | The scene: one object, **Harness Replay**, with `ReplayController`. Everything else is built from code when Play starts. |
| `Runtime/RecordingData.cs` | C# classes matching the recording JSON (format v2) field for field. |
| `Runtime/RecordingLoader.cs` | Reads and checks a recording (version 1 or 2), finds the newest one. Uses Newtonsoft JSON so `null` stays `null`. |
| `Runtime/FtcToUnity.cs` | **The only place** that converts FTC units (inches, heading counter-clockwise from +x) to Unity units (meters, yaw clockwise from +z). |
| `Runtime/ReplayTimeline.cs` | Play/pause/speed/seek, the same rules as the Swing viewer's `Playback`, plus smooth blending between the 20 ms loops. No Unity objects, so it is easy to test. |
| `Runtime/ReplayPanelText.cs` | The panel facts as plain text for one moment (same content as the Swing viewer's `SidePanelText`). |
| `Runtime/HudTheme.cs` | Colors, fonts and drawing helpers, copied from the Swing viewer's `Theme` (dark "mission control" style). |
| `Runtime/HudText.cs` | The words and numbers the panels show (clock, stage line, captions, gauges, Driver Station, events), matching the Swing viewer. |
| `Runtime/ReplayHud.cs` | Draws the panels around the field in the Swing layout: header, chooser, cards, gamepads, playback bar. |
| `Runtime/FieldBuilder.cs` | Builds the dark field, tiles, walls, axis labels, robot (outline, cyan front, white arrow, wheels lit by power, intake) and trail. |
| `Runtime/OrbitCamera.cs` | Camera: top-down by default (like the Swing field), T for angled 3D, right-drag to turn, scroll to zoom, F follow. |
| `Runtime/TestHarnessFolder.cs` | Finds the TestHarness folder (next to the app, above it, or the one picked with "Where is your TestHarness folder?"), and the th_src folders read from it. |
| `Runtime/WindowsDialogs.cs` | Windows' own Open and folder boxes for the built app (Unity's file picker only exists in the Editor). |
| `Runtime/ReplayController.cs` | Ties it together: opens a recording, plays it, keeps the 3D view inside the field area, draws the panels. |
| `Editor/ReplayTestMenu.cs` | **VI Arena → Run Replay Tests** menu; writes `TestHarness/th_src/build/unity-test-results.txt`. |
| `Tests/` | Unity tests (Window → General → Test Runner, EditMode), and `Samples/teamA-teleop-turn.json`, a real harness recording the tests read. |

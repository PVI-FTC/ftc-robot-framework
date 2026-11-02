# Plan: new folder structure (TestHarness / th_src / VI_Arena)

Agreed with you on 2026-11-02 (Q1–Q9). Nothing is moved yet. Edit this file or tell the manager
what to change, then say "go".

## The new layout

```
ftc-robot-framework/
├── TeamCode/                          (unchanged)
└── TestHarness/                       ← today's Harness/            (Q1: no space)
    ├── AGENTS.md, CLAUDE.md           (must stay at the top so AI tools find them;
    │                                   they point into docs/ai-instructions/)
    ├── .gitignore
    ├── view.bat                       (opens the Java viewer: runs th_src\gradlew.bat)
    ├── VI_Arena.bat                   (today's unity-view.bat: opens VI_Arena\dist\VI_Arena.exe)
    ├── docs/                          (Q4)
    │   ├── README.md, GETTING_STARTED.md, BRING_YOUR_OWN_ROBOT.md, CALIBRATION.md
    │   ├── PROGRESS.md, MANAGER_DECISIONS.md, RESTRUCTURE_PLAN.md
    │   └── ai-instructions/
    ├── th_src/                        ← the Java test harness
    │   ├── build.gradle, settings.gradle, gradle.properties, gradlew, gradlew.bat, gradle/
    │   ├── harness.properties
    │   ├── src/                       (agent, main, test, viewer, bridge, pvi extras)
    │   ├── fields/                    (Q3: field files, used by Java and Unity)
    │   ├── robots/                    (Q3: robot setups, used by Java and Unity)
    │   ├── robot-recorder/
    │   └── build/                     (test output and recordings; git-ignored)
    └── VI_Arena/                      ← the Unity application (renamed from HarnessReplay)
        ├── README.md                  (today's unity/README.md)
        ├── src/                       ← today's unity/ project (Assets, Packages,
        │                                 ProjectSettings) + buildUnity.bat
        ├── package.bat                (zips everything in dist/ for Google Drive or a thumb drive)
        └── dist/                      ← the built app: VI_Arena.exe + VI_Arena_Data/, for people
                                          without Unity. Local only, never in git (see "dist")
```

Outside the repo (Q5, Q6): `C:\Dev\VI_Arena-art\`, today's `Harness/art-inbox` (your original
downloads, 2.4 GB). Left off the USB stick (Q9).

## Names (Q7, Q8)

- **Unity: everything renamed**, internal names included: the app is `VI_Arena.exe`, the window
  and in-app title say "VI Arena", and the Unity project's `HarnessReplay` folders, assemblies
  (`.asmdef`), C# namespaces and test names become `VI_Arena`.
- **Java: package names stay** `pvi.harness.*`. Only the folders move.
- Git keeps each file's history through the moves (`git mv`).

## What has to change besides moving files

| Area | Change | Who |
|---|---|---|
| Gradle | `teamCode.sourceDir` and `host.dependenciesFile` become `../../TeamCode/...` and `../../build.dependencies.gradle` (one level deeper); `viewRecording`, `unityBridge` and `makeTest` keep working from `th_src/` | WorkerBot |
| Java tests and code that read files | field files and robots now in `th_src/`; docs now in `../docs/` (e.g. `GuideFileListTest` reads the guide) | WorkerBot, TestBot |
| Unity | the internal rename; the launcher finds `th_src\gradlew.bat`, `th_src\fields`, `th_src\robots` and `th_src\build\recordings` by the new relative paths; the build goes to `VI_Arena/dist/` | UnityBot |
| Launchers | `view.bat` and `VI_Arena.bat` at the top of `TestHarness/`; `buildUnity.bat` in `VI_Arena/src/` | WorkerBot (view.bat), UnityBot (the rest) |
| Docs | moved into `docs/`; every path and link updated (README, guides, the drop-in guide's copy step) | TestBot |
| Rules | the bots' scope becomes "only `TestHarness/`"; `06-bot-team.md`, `01-scope-and-boundaries.md`, `CLAUDE.md`/`AGENTS.md` | Manager |
| `.gitignore` | `th_src/build`, `th_src/.gradle`, `VI_Arena/src/Library` (and Temp, Logs, UserSettings), `VI_Arena/dist`, `VI_Arena/*.zip` | Manager |
| USB sync | new paths; the art no longer goes on the stick | Manager |

## Order

1. **All bots idle; commit** whatever is finished. Close the Unity Editor and the app.
2. **Manager moves the folders** with `git mv` in one step (and moves the Unity `Library` cache
   along, so Unity doesn't have to re-import everything). `art-inbox` moves to
   `C:\Dev\VI_Arena-art`.
3. **WorkerBot** fixes Gradle and Java paths until `gradlew test` runs from `th_src/`.
4. **UnityBot** does the Unity rename and paths, runs the Unity tests and builds into
   `VI_Arena/dist/`. **TestBot** updates the docs and links at the same time (no Gradle needed).
5. **Manager**: rules, `.gitignore`, USB script; full Java suite and Unity tests; checks that
   `view.bat`, `VI_Arena.bat` and Live mode work; commit.
6. Then the **open items** below, then the **GitHub push**.

Expected outcome: the same 390 Java tests and about 125 Unity tests pass, in the new places.

## dist: local only, shared as a zip (decided 2026-11-02)

- **`VI_Arena/dist/` is never committed.** It's git-ignored; builds go there on the PC that
  builds them.
- **To share it:** zip the **entire contents** of `dist/` and put the zip on Google Drive or a
  thumb drive. People without Unity unzip it anywhere and run `VI_Arena.exe`.
- **A one-click zip script** (UnityBot): `VI_Arena/package.bat` builds a dated zip, e.g.
  `VI_Arena/VI_Arena-2026-11-02.zip` (git-ignored), from everything in `dist/`, and says how big it
  is. It never uploads anything by itself.
- **The unzipped app must work on its own:** it's opened from a folder anywhere, with no Unity and
  no repo. Replay of recordings works from it. Live mode needs the Java harness (`th_src`) and Java
  17+, so for live play the app must be able to find a `TestHarness` folder; it says plainly when
  it can't. UnityBot checks this.
- **Size test (UnityBot, still worth it for the upload):** today's build is 461 MB (339 MB of it
  mostly uncompressed textures, HDR skies, the moon-base scene and the crowd). Texture compression
  (BC7, BC6H for HDR skies, max 2048 px), smaller skies, LZ4HC build compression and mesh
  compression, built into a scratch folder, with the real size and pictures before and after.
  You pick whether to use it.

## Open: the GitHub push

Stopped on 2026-11-01 (too slow with the art). The plan: after the restructure, merge the team's
2 new `Pedro-3-Implementation` commits (KimCode `SimpleLeave`, `TeleopV1` changes), fix the 7
tests they break (skip lists and expected OpMode lists, harness side only), and push the
**code-only** branch `VI_Arena-Harness` (about 1 MB, art left out). Confirm when the time comes.

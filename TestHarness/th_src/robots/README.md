# Robots

One folder per robot. A folder can say how a robot **looks** in the Unity replay, how it **drives**,
or both:

| File | What |
|---|---|
| `drive.properties` | A robot profile: the fake robot some OpModes run on (see "drive.properties" below). |
| `camera.properties` | The robot's cameras (see "camera.properties" below). The only place they are set. |
| `robot.properties` | The robot's name, where the model came from, which Unity model to draw, the model's units and axes, its center, and which parts are the drive wheels. |
| anything else | How the model was made (for example `slim_model.py`). |

The model itself lives in Unity, under
`VI_Arena/src/Assets/VI_Arena/Resources/Robots/<id>/`, because Unity can only load models from its
own `Assets` folder.

`robot.properties` is only the picture. The robot's size for physics and the harness, and how it
drives, come from `harness.properties` or the robot's `drive.properties`. Pick the look on the Unity replay's **Visuals** screen (V), under
**Robot**: the plain **Box** (the recording's robot size) or a model (its true size). The choice is
remembered; `-robot <id>` or `-robot box` on the command line picks one for that run only.

## robot.properties

Plain `key=value` lines; `#` starts a comment.

| Key | Meaning |
|---|---|
| `name` | Shown on the Visuals screen. |
| `source` | Where the model came from (also listed in `VI_Arena/src/Assets/VI_Arena/Art/CREDITS.md`). |
| `model` | **Required.** The model's path under `Resources`, without the extension. |
| `model.metersPerUnit` | Meters per model unit: `0.001` for millimeters, `0.0254` for inches. Default `1`. |
| `model.forward` | The model's axis that points to the robot's front: `+x`, `-x`, `+y`, `-y`, `+z` or `-z`. Default `+z`. |
| `model.up` | The model's axis that points up. Default `+y`. Must differ from `model.forward`. |
| `model.centerX`, `model.centerY`, `model.centerZ` | The robot's center in model units (usually the middle of the drive wheels). It is put on the robot's position. Default `0`. Height doesn't matter: the model's lowest point is set on the floor. |
| `wheel.frontLeft`, `wheel.frontRight`, `wheel.rearLeft`, `wheel.rearRight` | The model's parts (by object name) that spin with that motor's power. Several parts may be listed with commas. Leave out for no spinning. |

If the front or sides come out wrong, change `model.forward` (and `model.up`); no code change is
needed. Unity's OBJ importer mirrors x into its own axes, but that doesn't change which way is
front, up or left.

## drive.properties

The robot in `harness.properties` runs every OpMode, except the ones a profile lists. Use a profile
for an OpMode written for a different robot, for example with other motor names. Nothing else is
needed: the harness finds it by looking in this folder, and tests, the viewer and Live mode all use
it. Recordings show the folder's name as the robot.

It has the same keys as `harness.properties`, only the ones that differ, plus:

| Key | Meaning |
|---|---|
| `forOpModes` | **Required.** The OpModes that run on this robot, comma-separated: Driver Station name or class name. |

- **A profile may only change the robot:** `robot.*` (not `robot.id`: its id is the folder's name),
  `drive.*`, `pinpoint`, `hardware.*` and `devices.*`. The field, the viewer and the build settings are one setting for the whole harness.
- **Cameras** aren't set here. See "camera.properties" below.
- **Numbers it doesn't list come from `harness.properties` as they are, marked measured or not.** So
  copy any number you borrow into the profile and mark it `placeholder`: a speed measured on one robot
  isn't measured on another.
- **It fails early, with one message:** a misspelled key, a key it can't change, no `forOpModes`, or
  an OpMode two profiles list.

Copying the harness to another team's branch? Delete the profiles that aren't yours. One that lists
OpModes you don't have does no harm, but it's clutter.

## camera.properties

A robot's webcams, for the main robot (the folder `robot.id` in `harness.properties` names) and for
any profile alike. A robot never gets another robot's cameras.

- **No webcam:** leave the file out, or write an empty `devices.webcams=`.
- **To add one:** copy `team-a/camera.properties` into the robot's folder, change the name in
  `devices.webcams`, and change each `webcam.<name>.*` line to that webcam. Mark what you didn't measure
  `placeholder`.
- **It fails early, naming the file:** a misspelled key, a key for a webcam that isn't listed, or a listed
  webcam missing one of its lines. `docs/README.md` lists the keys.

The main robot's folder has no `drive.properties`: its other settings are in `harness.properties`.

## Robots here

| Folder | Robot | Model |
|---|---|---|
| `team-a` | Team A, the main robot: `camera.properties` only (its logitechVisionWebcam). | None; Unity draws whichever look is picked. |
| `kimcode` | KimCode's robot: `drive.properties` only, for TeleopV1 and SimpleLeave. No webcam. | None; Unity draws whichever look is picked. |
| `gobilda-starter-bot` | goBILDA FTC Starter Bot (2026-2027) | goBILDA's STEP, converted to OBJ by the user, slimmed from 3.4 million to about 140,000 triangles by `slim_model.py` (screws and nuts dropped, wheels split out). |

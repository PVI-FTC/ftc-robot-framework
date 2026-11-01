# Robots

How each robot **looks** in the Unity replay. One folder per robot:

| File | What |
|---|---|
| `robot.properties` | The robot's name, where the model came from, which Unity model to draw, the model's units and axes, its center, and which parts are the drive wheels. |
| anything else | How the model was made (for example `slim_model.py`). |

The model itself lives in Unity, under
`unity/Assets/HarnessReplay/Resources/Robots/<id>/`, because Unity can only load models from its
own `Assets` folder.

This is only the picture. The robot's size for physics and the harness, and how it drives, still
come from `harness.properties`. Pick the look on the Unity replay's **Visuals** screen (V), under
**Robot**: the plain **Box** (the recording's robot size) or a model (its true size). The choice is
remembered; `-robot <id>` or `-robot box` on the command line picks one for that run only.

## robot.properties

Plain `key=value` lines; `#` starts a comment.

| Key | Meaning |
|---|---|
| `name` | Shown on the Visuals screen. |
| `source` | Where the model came from (also listed in `unity/Assets/HarnessReplay/Art/CREDITS.md`). |
| `model` | **Required.** The model's path under `Resources`, without the extension. |
| `model.metersPerUnit` | Meters per model unit: `0.001` for millimeters, `0.0254` for inches. Default `1`. |
| `model.forward` | The model's axis that points to the robot's front: `+x`, `-x`, `+y`, `-y`, `+z` or `-z`. Default `+z`. |
| `model.up` | The model's axis that points up. Default `+y`. Must differ from `model.forward`. |
| `model.centerX`, `model.centerY`, `model.centerZ` | The robot's center in model units (usually the middle of the drive wheels). It is put on the robot's position. Default `0`. Height doesn't matter: the model's lowest point is set on the floor. |
| `wheel.frontLeft`, `wheel.frontRight`, `wheel.rearLeft`, `wheel.rearRight` | The model's parts (by object name) that spin with that motor's power. Several parts may be listed with commas. Leave out for no spinning. |

If the front or sides come out wrong, change `model.forward` (and `model.up`); no code change is
needed. Unity's OBJ importer mirrors x into its own axes, but that doesn't change which way is
front, up or left.

## Robots here

| Folder | Robot | Model |
|---|---|---|
| `gobilda-starter-bot` | goBILDA FTC Starter Bot (2026-2027) | goBILDA's STEP, converted to OBJ by the user, slimmed from 3.4 million to about 140,000 triangles by `slim_model.py` (screws and nuts dropped, wheels split out). |

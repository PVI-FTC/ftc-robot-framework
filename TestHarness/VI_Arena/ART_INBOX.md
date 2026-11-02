# Art inbox

The art inbox is **`C:\Dev\VI_Arena-art\`**, outside the repository (moved out of `Harness/art-inbox`
on 2026-11-02, so the 2.4 GB of originals never go to GitHub or the USB stick). Drop downloaded
models, textures and skies there, **any file names, any sub-folders**. Claude sorts them into
`VI_Arena/src/Assets/VI_Arena/Resources/...`, renames them and wires them in. Only those sorted,
slimmed copies are in git.

**Licences:** for each download, also drop a note (e.g. `links.txt`) with the page you got it from.
Then every file can be credited, and its licence checked (CC0 and public domain are easiest; CC-BY
needs a credit, which is fine).

**Formats:** models as FBX (best) or OBJ; ask first if you only have `.glb`/`.gltf`, which needs an
extra Unity package. Textures as JPG/PNG, 2K–4K, with a Normal map in the **OpenGL** style ("NormalGL")
when there is a choice. Skies as an equirectangular `.hdr` (4K+) or a big `.jpg` panorama.

Tick things off as you go. **Must** = needed for that part; **nice** = adds detail.

---

## 1. Field and robot (seen all the time)

| ✓ | What | Where to get it | Priority |
|---|---|---|---|
| ☐ | BIOBUZZ field elements as FBX/OBJ: HIVE frame, both HIVEs with CELLs, a FLOWER, the perimeter wall, a foam tile. One file for the whole field is also fine | FIRST's official BIOBUZZ field CAD (Onshape; export as OBJ or FBX) | must |
| ☐ | The AprilTag cluster images (printable PDFs/PNGs) | FIRST Playing Field Resources page | nice |
| ☐ | Team A's robot as FBX/OBJ | your team's CAD (Onshape: export as OBJ or FBX) | must |
| ☐ | Foam field tile texture (grey, with the bumpy top) | Poly Haven / ambientCG, or a photo | nice |

## 2. Competition hall

| ✓ | What | Look for | Priority |
|---|---|---|---|
| ☐ | Sky / backdrop | an **indoor HDRI**: a sports hall, gym or large indoor arena | must |
| ☐ | Floor texture set | light wood gym floorboards (Color, Normal, Roughness, AO) | must |
| ☐ | Wall texture set | painted cinder block or painted concrete | nice |
| ☐ | Bleacher model | one section of bleachers / grandstand seating | must |
| ☐ | Table model | a folding table (drive-team station) | must |
| ☐ | Laptop model | for the drive-team table | nice |
| ☐ | Game controller model | Xbox or PlayStation style gamepad | nice |
| ☐ | Phone / tablet model | the Driver Station | nice |
| ☐ | Tool cart model | a rolling tool cart or toolbox | nice |
| ☐ | People models (2–4) | standing people / spectators, any style | nice |

## 3. Night stadium

| ✓ | What | Look for | Priority |
|---|---|---|---|
| ☐ | Sky / backdrop | a **night HDRI**: a stadium at night, or open night sky with stars | must |
| ☐ | Grass texture set | short sports-field grass | must |
| ☐ | Running track texture set | red rubber athletics track | must |
| ☐ | Concrete texture set | rough concrete (stands) | nice |
| ☐ | Floodlight tower model | stadium light pole with a light bank | must |
| ☐ | Goal post model | American football goal post | nice |
| ☐ | Bench model | player bench / dugout bench | nice |
| ☐ | Hurdle model | athletics hurdle | nice |

## 4. Moon base

| ✓ | What | Look for | Priority |
|---|---|---|---|
| ☐ | Sky / backdrop | a **space HDRI**: starfield or Milky Way | must |
| ☐ | Earth picture | equirectangular (map-shaped) Earth, e.g. NASA "Blue Marble" (public domain) | must |
| ☐ | Ground texture set | lunar regolith, or grey volcanic ash / sand with pebbles | must |
| ☐ | Metal plate texture set | worn diamond plate or metal floor panels (landing pad) | must |
| ☐ | Lander model | lunar lander (NASA 3D Resources has the Apollo LM) | must |
| ☐ | Rover model | lunar rover (NASA 3D Resources) | must |
| ☐ | Habitat module model | space habitat / base module | nice |
| ☐ | Astronaut model | spacesuit astronaut (NASA 3D Resources) | nice |
| ☐ | Antenna / dish model | communications dish | nice |

## 5. People (done 2026-10-25: four Mixamo characters and five animations are in `Resources/People`)

No free CC0 people exist on Poly Haven, ambientCG or NASA, so these come from you. Drop the FBX files
in `C:\Dev\VI_Arena-art\people\`.

| ✓ | What | Where to get it | Priority |
|---|---|---|---|
| ✓ | 4–8 different people, standing, as FBX, **with skin**, in a T-pose or idle pose | **Mixamo** (mixamo.com, free with an Adobe account). Download each character as FBX for Unity with *Skin*, no animation, or with one idle animation | must |
| ✓ | A **sitting** animation (e.g. "Sitting Idle") for the fans in the bleachers | Mixamo: pick a character, search "sitting", download FBX for Unity **without skin** (just the animation) | must |
| ✓ | A **standing idle** animation for the drive teams | Mixamo: "Idle" or "Standing", FBX for Unity, without skin | nice |

Mixed clothing colours look best in a crowd: Claude repeats and recolours the people across the
bleachers and puts them at the drive-team stations.

---

## Good free sources

- **NASA 3D Resources** (nasa3d.arc.nasa.gov / science.nasa.gov 3D resources): public domain;
  landers, rovers, astronauts, Earth images.
- **Poly Haven** (polyhaven.com): CC0; HDRIs, texture sets, detailed props.
- **ambientCG** (ambientcg.com): CC0; texture sets.
- **Sketchfab** (sketchfab.com): filter "Downloadable" and a CC licence; check each model's licence.
- **Onshape**: FIRST's official field CAD and your team's robot.

When the files are in, tell Claude "the art inbox is ready" (or name a section to start with).

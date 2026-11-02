using System.Collections.Generic;
using UnityEngine;

namespace VI_Arena
{
    /// <summary>One place the arena can stand in: its name, a line about it, and its sky and light.</summary>
    public sealed class SettingInfo
    {
        public string id;
        public string name;
        public string description;
        /// <summary>What the camera shows where nothing is drawn (the sky).</summary>
        public Color sky;
        /// <summary>Even fill light on everything that is lit.</summary>
        public Color ambient;
        public Color sunColor;
        public float sunIntensity;
        /// <summary>The sun's direction as (down angle, compass angle) in degrees.</summary>
        public Vector2 sunAngles;
    }

    /// <summary>
    /// The surroundings around the field: a school competition hall, a night stadium, a moon base,
    /// or none (the dark "mission control" look). Everything is built from plain shapes in code, like
    /// the field, so every size and color is here to read and change.
    ///
    /// <para>This is scenery, not field data: it is laid out directly in Unity meters (x toward the
    /// audience, z toward the blue alliance, y up) and never enters the field itself (a 4.2 m square
    /// around the center stays empty), so it can't hide the robot or the field parts. Nothing has a
    /// collider: Unity's physics only sees the field. Random-looking things (crowd, craters, stars)
    /// use a fixed seed, so a setting looks the same every time.</para>
    /// </summary>
    public static class SettingBuilder
    {
        /// <summary>The setting that adds nothing: the dark look matching the Swing viewer.</summary>
        public const string MissionControlId = "mission-control";
        /// <summary>Half the size of the square kept empty around the field center (meters): the field plus a margin.</summary>
        public const float KeepClearMeters = 2.1f;
        /// <summary>
        /// The child object holding a setting's ceiling (the hall's trusses and lights). The replay hides it
        /// while the camera is higher than <see cref="CeilingHideAboveMeters"/>, like a dollhouse, so
        /// zooming out never puts the ceiling in front of the field.
        /// </summary>
        public const string CeilingName = "Ceiling";
        public const float CeilingHideAboveMeters = 7.2f;

        /// <summary>Every setting, in the order the Visuals screen shows them.</summary>
        public static readonly IReadOnlyList<SettingInfo> All = new List<SettingInfo>
        {
            new SettingInfo
            {
                id = MissionControlId, name = "Mission control",
                description = "The dark, plain look of the Swing viewer. Nothing around the field.",
                sky = HudTheme.Background, ambient = new Color(0.55f, 0.57f, 0.60f),
                sunColor = Color.white, sunIntensity = 0.9f, sunAngles = new Vector2(60f, -30f),
            },
            new SettingInfo
            {
                id = "competition-hall", name = "Competition hall",
                description = "A school gym on event day: bleachers full of fans, banners, drive-team stations, scoreboard.",
                sky = Hex(0x1A1D24), ambient = new Color(0.62f, 0.58f, 0.52f),
                sunColor = new Color(1f, 0.95f, 0.86f), sunIntensity = 0.8f, sunAngles = new Vector2(75f, 20f),
            },
            new SettingInfo
            {
                id = "night-stadium", name = "Night stadium",
                description = "Under the floodlights on a football field: running track, packed stands, stars overhead.",
                sky = Hex(0x050A18), ambient = new Color(0.30f, 0.33f, 0.42f),
                sunColor = new Color(0.75f, 0.82f, 1f), sunIntensity = 0.45f, sunAngles = new Vector2(55f, 140f),
            },
            new SettingInfo
            {
                id = "moon-base", name = "Moon base",
                description = "A landing pad on the Moon: grey dust, craters, habitat domes, and Earth in a black sky.",
                sky = Hex(0x000000), ambient = new Color(0.20f, 0.20f, 0.22f),
                sunColor = Color.white, sunIntensity = 1.35f, sunAngles = new Vector2(28f, -60f),
            },
        };

        /// <summary>The setting with this id, or Mission control if there is none.</summary>
        public static SettingInfo Find(string id)
        {
            foreach (SettingInfo setting in All)
            {
                if (setting.id == id)
                {
                    return setting;
                }
            }
            return All[0];
        }

        /// <summary>The setting id after <c>-setting</c> on the command line ("-setting moon-base"), or null.</summary>
        public static string SettingFromCommandLine(string[] args)
        {
            for (int i = 0; i < args.Length - 1; i++)
            {
                if (string.Equals(args[i], "-setting", System.StringComparison.OrdinalIgnoreCase))
                {
                    return args[i + 1];
                }
            }
            return null;
        }

        /// <summary>Sets the sky, fill light and sun for a setting.</summary>
        public static void ApplyLighting(SettingInfo setting, Camera camera, Light sun)
        {
            if (camera != null)
            {
                camera.backgroundColor = setting.sky;
            }
            RenderSettings.ambientMode = UnityEngine.Rendering.AmbientMode.Flat;
            RenderSettings.ambientLight = setting.ambient;
            // The 360° sky (if the setting's art is in the project); the replay shows it in the 3D view.
            RenderSettings.skybox = SettingAssets.Sky(setting.id);
            if (sun != null)
            {
                sun.color = setting.sunColor;
                sun.intensity = setting.sunIntensity;
                sun.transform.rotation = Quaternion.Euler(setting.sunAngles.x, setting.sunAngles.y, 0f);
            }
        }

        /// <summary>Builds a setting's surroundings under a new object and returns it (null for Mission control).</summary>
        public static Transform Build(Transform parent, SettingInfo setting)
        {
            if (setting.id == MissionControlId)
            {
                return null;
            }
            var root = new GameObject("Setting " + setting.id).transform;
            root.SetParent(parent, false);
            var random = new System.Random(2026); // a fixed seed: the same crowd and craters every time
            switch (setting.id)
            {
                case "competition-hall":
                    BuildHall(root, random);
                    break;
                case "night-stadium":
                    BuildStadium(root, random);
                    break;
                case "moon-base":
                    BuildMoonBase(root, random);
                    break;
            }
            return root;
        }

        // ---- Competition hall ---------------------------------------------------------------------

        private static void BuildHall(Transform root, System.Random random)
        {
            // Wooden gym floor with court lines, then walls and a dark ceiling.
            const string hall = "competition-hall";
            GameObject floor = Part(root, "Gym floor", new Vector3(0f, -0.04f, 0f), new Vector3(36f, 0.02f, 26f), Hex(0xB98552));
            SettingAssets.ApplySurface(floor, hall, "floor", new Vector2(36f, 26f), 1.6f, Hex(0xF2DDBF), 0.45f);
            Glow(root, "Court line far", new Vector3(-6f, -0.028f, 0f), new Vector3(0.06f, 0.004f, 18f), Hex(0xF2F2F2));
            Glow(root, "Court line near", new Vector3(5.2f, -0.028f, 0f), new Vector3(0.06f, 0.004f, 18f), Hex(0xF2F2F2));
            Glow(root, "Court line red", new Vector3(-0.4f, -0.028f, -9f), new Vector3(11.2f, 0.004f, 0.06f), Hex(0xF2F2F2));
            Glow(root, "Court line blue", new Vector3(-0.4f, -0.028f, 9f), new Vector3(11.2f, 0.004f, 0.06f), Hex(0xF2F2F2));
            Color wall = Hex(0x2C3442);
            foreach (GameObject side in new[]
            {
                Part(root, "Wall far", new Vector3(-18f, 4f, 0f), new Vector3(0.3f, 8f, 26f), wall),
                Part(root, "Wall audience", new Vector3(18f, 4f, 0f), new Vector3(0.3f, 8f, 26f), wall),
                Part(root, "Wall red", new Vector3(0f, 4f, -13f), new Vector3(36f, 8f, 0.3f), wall),
                Part(root, "Wall blue", new Vector3(0f, 4f, 13f), new Vector3(36f, 8f, 0.3f), wall),
            })
            {
                Vector3 s = side.transform.localScale;
                SettingAssets.ApplySurface(side, hall, "wall", new Vector2(Mathf.Max(s.x, s.z), s.y), 1.6f, Hex(0x8C96A8));
            }

            // Bleachers full of fans on the audience side, a smaller stand behind the far side.
            Bleachers(root, random, "Bleachers audience", new Vector3(6.5f, 0f, 0f), 7, 22f, +1, 0.82f);
            Bleachers(root, random, "Bleachers far", new Vector3(-9.5f, 0f, 0f), 4, 16f, -1, 0.55f);

            // Drive-team stations: alliance-colored tables, monitors and drivers at the red and blue walls.
            for (int side = -1; side <= 1; side += 2)
            {
                Color alliance = side < 0 ? Hex(0xD8352C) : Hex(0x2F62E0);
                float z = side * 3.4f;
                for (int team = 0; team < 2; team++)
                {
                    float x = -0.9f + team * 1.8f;
                    Part(root, "Station table", new Vector3(x, 0.38f, z), new Vector3(1.2f, 0.76f, 0.6f), alliance);
                    Glow(root, "Station screen", new Vector3(x, 0.95f, z + side * 0.15f), new Vector3(0.5f, 0.32f, 0.03f), Hex(0x7FD3FF));
                    // Two drivers behind each table, facing the field: real animated people when the files are there.
                    float facing = side < 0 ? 0f : 180f;
                    foreach (float dx in new[] { -0.3f, 0.3f })
                    {
                        var feet = new Vector3(x + dx, 0f, z + side * 0.55f);
                        if (People.PlaceStanding(root, random, feet, facing, alliance) == null)
                        {
                            Person(root, random, feet, alliance);
                        }
                    }
                    // A laptop and a gamepad on the table (detailed models, when the art is there).
                    SettingAssets.PlaceModel(root, hall, "classic_laptop", new Vector3(x - 0.2f, 0.76f, z), side < 0 ? 0f : 180f, 0.34f);
                    SettingAssets.PlaceModel(root, hall, "gamepad", new Vector3(x + 0.32f, 0.76f, z - side * 0.05f), side < 0 ? 10f : 190f, 0.16f);
                }
            }

            // A pit corner behind each alliance: tool cart and tool chest.
            SettingAssets.PlaceModel(root, hall, "tool_cart", new Vector3(-4.6f, 0f, -4.2f), 30f, 1.0f);
            SettingAssets.PlaceModel(root, hall, "metal_tool_chest", new Vector3(4.8f, 0f, 4.4f), 200f, 1.1f);

            // Hanging banners on the side walls, in FIRST-ish colors.
            Color[] bannerColors = { Hex(0xD8352C), Hex(0x2F62E0), Hex(0xF2F2F2), Hex(0xF29A2E), Hex(0x3BAA5C) };
            for (int i = 0; i < 9; i++)
            {
                float x = -14f + i * 3.5f;
                Color color = bannerColors[i % bannerColors.Length];
                Part(root, "Banner red wall", new Vector3(x, 5.6f, -12.8f), new Vector3(1.4f, 2.6f, 0.05f), color);
                Part(root, "Banner blue wall", new Vector3(x, 5.6f, 12.8f), new Vector3(1.4f, 2.6f, 0.05f), bannerColors[(i + 2) % bannerColors.Length]);
            }

            // Scoreboard on the far wall.
            Part(root, "Scoreboard", new Vector3(-17.7f, 5f, 0f), new Vector3(0.3f, 2.4f, 6.4f), Hex(0x111318));
            Glow(root, "Score red", new Vector3(-17.5f, 5f, -1.9f), new Vector3(0.05f, 1.6f, 2.2f), Hex(0xB3261E));
            Glow(root, "Score blue", new Vector3(-17.5f, 5f, 1.9f), new Vector3(0.05f, 1.6f, 2.2f), Hex(0x1F4FC2));
            Sign(root, "FTC HARNESS", new Vector3(-17.45f, 6.55f, 0f), Quaternion.Euler(0f, 90f, 0f), 0.06f, Color.white);
            Sign(root, "2:30", new Vector3(-17.45f, 5f, 0f), Quaternion.Euler(0f, 90f, 0f), 0.09f, Hex(0xFFD24A));

            // Ceiling trusses with bright light panels, none straight over the field. They sit in the
            // "Ceiling" group, which the replay hides while the camera is above them.
            Transform ceiling = new GameObject(CeilingName).transform;
            ceiling.SetParent(root, false);
            foreach (float x in new[] { -14f, -8.5f, -3f, 3f, 8.5f, 14f })
            {
                Part(ceiling, "Truss", new Vector3(x, 7.6f, 0f), new Vector3(0.25f, 0.25f, 26f), Hex(0x4A5160));
                for (int j = 0; j < 4; j++)
                {
                    Glow(ceiling, "Ceiling light", new Vector3(x, 7.4f, -9f + j * 6f), new Vector3(1.4f, 0.08f, 0.7f), Hex(0xFFF4DC));
                }
            }
        }

        /// <summary>Stepped bleachers along z, rising away from the field (direction ±1 along x), filled with fans.</summary>
        private static void Bleachers(Transform root, System.Random random, string name, Vector3 front, int rows, float length,
            int direction, float fullness)
        {
            const float step = 0.75f, rise = 0.45f;
            for (int row = 0; row < rows; row++)
            {
                float x = front.x + direction * (row * step + step / 2f);
                float height = (row + 1) * rise;
                Part(root, name + " row " + row, new Vector3(x, height / 2f, front.z), new Vector3(step, height, length), Hex(0x394252));
                for (float z = -length / 2f + 0.4f; z < length / 2f - 0.3f; z += 0.62f)
                {
                    if (random.NextDouble() < fullness)
                    {
                        // A seated fan facing the field (real people when the files are there), else a capsule.
                        var seat = new Vector3(x - direction * 0.12f, height, front.z + z);
                        if (People.PlaceFan(root, random, seat, direction > 0 ? -90f : 90f) == null)
                        {
                            Person(root, random, new Vector3(x, height, front.z + z), FanColor(random));
                        }
                    }
                }
            }
        }

        // ---- Night stadium ------------------------------------------------------------------------

        private static void BuildStadium(Transform root, System.Random random)
        {
            const string stadium = "night-stadium";
            GameObject grass = Part(root, "Grass", new Vector3(0f, -0.05f, 0f), new Vector3(150f, 0.02f, 150f), Hex(0x2F6A2C));
            SettingAssets.ApplySurface(grass, stadium, "grass", new Vector2(150f, 150f), 2.5f, Hex(0xC8D8B8), 0.05f);
            // Mowing stripes, then a dark mat under the FTC field.
            for (int i = -6; i <= 6; i += 2)
            {
                GameObject stripe = Part(root, "Grass stripe", new Vector3(i * 4f, -0.038f, 0f), new Vector3(4f, 0.004f, 34f), Hex(0x367833));
                SettingAssets.ApplySurface(stripe, stadium, "grass", new Vector2(4f, 34f), 2.5f, Hex(0xE4F0D2), 0.05f);
            }
            Part(root, "Field mat", new Vector3(0f, -0.032f, 0f), new Vector3(7f, 0.01f, 7f), Hex(0x1C1F24));
            // Yard lines inside the track's inner edge.
            Glow(root, "Yard line", new Vector3(-9f, -0.03f, 0f), new Vector3(0.12f, 0.004f, 12f), Hex(0xEDEDED));
            Glow(root, "Yard line", new Vector3(9f, -0.03f, 0f), new Vector3(0.12f, 0.004f, 12f), Hex(0xEDEDED));

            // Running track: a rounded oval of rust-red segments with white lane lines.
            Transform track = Oval(root, "Track", 30f, 19f, 5f, Hex(0xA4442F), 180);
            TextureAll(track, stadium, "track", new Vector2(5f, 0.6f), 1.2f, Hex(0xB2442E), useColorMap: false);
            Oval(root, "Lane line", 30f, 19f, 0.07f, Hex(0xF0F0F0), 180, 1.6f);
            Oval(root, "Lane line", 30f, 19f, 0.07f, Hex(0xF0F0F0), 180, -1.6f);

            // Stands along both long sides, packed with fans, and a press box.
            Stand(root, random, "Stand blue side", 25f, +1);
            Stand(root, random, "Stand red side", -25f, -1);
            Part(root, "Press box", new Vector3(31.5f, 8.5f, 0f), new Vector3(3f, 2.6f, 14f), Hex(0x30353F));
            Glow(root, "Press box windows", new Vector3(30f, 8.7f, 0f), new Vector3(0.05f, 1.1f, 12.5f), Hex(0xFFE7A8));

            // Four floodlight towers with real lights aimed at the field.
            for (int sx = -1; sx <= 1; sx += 2)
            {
                for (int sz = -1; sz <= 1; sz += 2)
                {
                    var base_ = new Vector3(sx * 29f, 0f, sz * 26f);
                    Part(root, "Tower pole", base_ + new Vector3(0f, 12f, 0f), new Vector3(0.7f, 12f, 0.7f), Hex(0x8E949C), PrimitiveType.Cylinder);
                    Quaternion facing = Quaternion.LookRotation(new Vector3(-base_.x, -24f, -base_.z));
                    GameObject head = Glow(root, "Floodlights", base_ + new Vector3(0f, 24.5f, 0f), new Vector3(4.5f, 2.6f, 0.35f), Hex(0xFFF7DE));
                    head.transform.rotation = facing;
                    var flood = new GameObject("Floodlight").AddComponent<Light>();
                    flood.transform.SetParent(root, false);
                    flood.type = LightType.Spot;
                    flood.transform.position = base_ + new Vector3(0f, 24f, 0f);
                    flood.transform.rotation = facing;
                    flood.spotAngle = 38f;
                    flood.range = 80f;
                    flood.intensity = 2.2f;
                    flood.color = new Color(1f, 0.97f, 0.9f);
                }
            }

            // Goal posts at both ends of the football field.
            for (int side = -1; side <= 1; side += 2)
            {
                float x = side * 22f;
                Color yellow = Hex(0xF2C230);
                Part(root, "Goal post", new Vector3(x, 1.5f, 0f), new Vector3(0.2f, 1.5f, 0.2f), yellow, PrimitiveType.Cylinder);
                Part(root, "Crossbar", new Vector3(x, 3f, 0f), new Vector3(0.18f, 0.18f, 5.6f), yellow);
                Part(root, "Upright", new Vector3(x, 5.5f, -2.8f), new Vector3(0.16f, 2.5f, 0.16f), yellow, PrimitiveType.Cylinder);
                Part(root, "Upright", new Vector3(x, 5.5f, 2.8f), new Vector3(0.16f, 2.5f, 0.16f), yellow, PrimitiveType.Cylinder);
            }

            if (SettingAssets.Sky(stadium) == null)
            {
                Stars(root, random, 350, 120f); // the 360° night sky, when it's there, has real stars
            }
            Glow(root, "Moon", new Vector3(-90f, 70f, -60f), new Vector3(9f, 9f, 9f), Hex(0xF4F1E4), PrimitiveType.Sphere);
        }

        /// <summary>How many rows of each stadium stand get real people (the rest stay empty, to keep it fast).</summary>
        public const int HumanRows = 5;

        /// <summary>A long stand on one side (z sign <paramref name="direction"/>), rows rising away from the field.</summary>
        private static void Stand(Transform root, System.Random random, string name, float frontZ, int direction)
        {
            const float step = 0.85f, rise = 0.5f, length = 44f;
            for (int row = 0; row < 12; row++)
            {
                float z = frontZ + direction * (row * step + step / 2f);
                float height = (row + 1) * rise;
                GameObject seats = Part(root, name + " row " + row, new Vector3(0f, height / 2f, z), new Vector3(length, height, step), Hex(0x5A616B));
                SettingAssets.ApplySurface(seats, "night-stadium", "concrete", new Vector2(length, height), 2f, Hex(0xA0A4AA));
                for (float x = -length / 2f + 0.5f; x < length / 2f - 0.4f; x += 0.7f)
                {
                    if (random.NextDouble() < 0.7)
                    {
                        // Real people fill the lower rows (a full stand of them would be slow); without the
                        // people files, capsules fill every row as before.
                        if (People.Available)
                        {
                            if (row < HumanRows)
                            {
                                People.PlaceFan(root, random, new Vector3(x, height, z - direction * 0.12f), direction > 0 ? 180f : 0f);
                            }
                        }
                        else
                        {
                            Person(root, random, new Vector3(x, height, z), FanColor(random));
                        }
                    }
                }
            }
        }

        /// <summary>
        /// A rounded oval (two straights joined by half circles) made of short boxes: <paramref name="length"/>
        /// along x, <paramref name="width"/> along z, measured to the band's middle plus <paramref name="offset"/>.
        /// </summary>
        private static Transform Oval(Transform root, string name, float length, float width, float band, Color color, int segments,
            float offset = 0f)
        {
            var group = new GameObject(name).transform;
            group.SetParent(root, false);
            root = group;
            float radius = width / 2f + offset;
            float straight = length - width;
            float perimeter = 2f * straight + 2f * Mathf.PI * radius;
            float piece = perimeter / segments;
            for (int i = 0; i < segments; i++)
            {
                float d = (i + 0.5f) * piece;
                Vector3 point;
                float angle; // the track's direction there, degrees
                if (d < straight)
                {
                    point = new Vector3(-straight / 2f + d, 0f, radius);
                    angle = 90f;
                }
                else if (d < straight + Mathf.PI * radius)
                {
                    float a = (d - straight) / radius; // around the +x end
                    point = new Vector3(straight / 2f + radius * Mathf.Sin(a), 0f, radius * Mathf.Cos(a));
                    angle = 90f + a * Mathf.Rad2Deg;
                }
                else if (d < 2f * straight + Mathf.PI * radius)
                {
                    point = new Vector3(straight / 2f - (d - straight - Mathf.PI * radius), 0f, -radius);
                    angle = 270f;
                }
                else
                {
                    float a = (d - 2f * straight - Mathf.PI * radius) / radius; // around the -x end
                    point = new Vector3(-straight / 2f - radius * Mathf.Sin(a), 0f, -radius * Mathf.Cos(a));
                    angle = 270f + a * Mathf.Rad2Deg;
                }
                point.y = offset == 0f ? -0.034f : -0.03f;
                GameObject part = offset == 0f
                    // Pieces overlap a little so the outer edge of each curve has no gaps.
                    ? Part(root, name, point, new Vector3(band, 0.006f, piece * 1.6f), color)
                    : Glow(root, name, point, new Vector3(band, 0.004f, piece * 1.15f), color);
                part.transform.rotation = Quaternion.Euler(0f, angle, 0f);
            }
            return group;
        }

        /// <summary>One shared textured surface for every piece under <paramref name="group"/> (the track's 180 pieces).</summary>
        private static void TextureAll(Transform group, string settingId, string surface, Vector2 pieceMeters, float tileMeters, Color tint,
            bool useColorMap = true)
        {
            if (group.childCount == 0)
            {
                return;
            }
            GameObject first = group.GetChild(0).gameObject;
            SettingAssets.ApplySurface(first, settingId, surface, pieceMeters, tileMeters, tint, 0.1f, useColorMap);
            Material shared = first.GetComponent<Renderer>().sharedMaterial;
            foreach (Renderer piece in group.GetComponentsInChildren<Renderer>())
            {
                piece.sharedMaterial = shared;
            }
        }

        // ---- Moon base ----------------------------------------------------------------------------

        private static void BuildMoonBase(Transform root, System.Random random)
        {
            const string moon = "moon-base";
            GameObject regolith = Part(root, "Regolith", new Vector3(0f, -0.06f, 0f), new Vector3(220f, 0.02f, 220f), Hex(0x8D8B86));
            SettingAssets.ApplySurface(regolith, moon, "ground", new Vector2(220f, 220f), 3f, Hex(0xD8D6D2), 0.05f);
            // A metal landing pad under the field, with hazard stripes around its edge.
            GameObject pad = Part(root, "Landing pad", new Vector3(0f, -0.035f, 0f), new Vector3(6.4f, 0.03f, 6.4f), Hex(0x3C424B));
            SettingAssets.ApplySurface(pad, moon, "pad", new Vector2(6.4f, 6.4f), 1.6f, Hex(0x9AA0A8), 0.35f);
            for (int i = 0; i < 16; i++)
            {
                float t = -3f + i * 0.4f;
                Color stripe = i % 2 == 0 ? Hex(0xF2C230) : Hex(0x15171A);
                Glow(root, "Hazard", new Vector3(t, -0.018f, 3.05f), new Vector3(0.4f, 0.004f, 0.25f), stripe);
                Glow(root, "Hazard", new Vector3(t, -0.018f, -3.05f), new Vector3(0.4f, 0.004f, 0.25f), stripe);
                Glow(root, "Hazard", new Vector3(3.05f, -0.018f, t), new Vector3(0.25f, 0.004f, 0.4f), stripe);
                Glow(root, "Hazard", new Vector3(-3.05f, -0.018f, t), new Vector3(0.25f, 0.004f, 0.4f), stripe);
            }

            // Craters: a light rim with a darker floor, scattered away from the pad.
            for (int i = 0; i < 28; i++)
            {
                Vector3 at = AwayFromField(random, 7f, 70f);
                float size = 2f + (float)random.NextDouble() * 9f;
                GameObject rim = Part(root, "Crater rim", at + new Vector3(0f, -0.05f, 0f), new Vector3(size * 1.25f, 0.12f, size * 1.25f), Hex(0xA19E98), PrimitiveType.Cylinder);
                GameObject crater = Part(root, "Crater floor", at + new Vector3(0f, -0.03f, 0f), new Vector3(size, 0.12f, size), Hex(0x6E6C68), PrimitiveType.Cylinder);
                SettingAssets.ApplySurface(rim, moon, "ground", new Vector2(size * 1.25f, size * 1.25f), 3f, Hex(0xE8E6E2), 0.05f);
                SettingAssets.ApplySurface(crater, moon, "ground", new Vector2(size, size), 3f, Hex(0x9A9894), 0.05f);
            }
            // Rocks: detailed scanned moon rocks when the art is there, else simple stones.
            string[] rockModels = { "moon_rock_01", "moon_rock_03", "moon_rock_05" };
            for (int i = 0; i < 60; i++)
            {
                Vector3 at = AwayFromField(random, 4.5f, 40f);
                float s = 0.15f + (float)random.NextDouble() * 0.6f;
                float yaw = (float)random.NextDouble() * 360f;
                if (i < 18 && SettingAssets.PlaceModel(root, moon, rockModels[i % 3], at, yaw, s * 2.2f) != null)
                {
                    continue;
                }
                GameObject rock = Part(root, "Rock", at, new Vector3(s * 1.3f, s * 0.7f, s), Hex(0x5E5C58), PrimitiveType.Sphere);
                rock.transform.rotation = Quaternion.Euler(0f, yaw, 0f);
            }

            // NASA's Apollo Lunar Module and an astronaut by the pad (when the art is there).
            SettingAssets.PlaceModel(root, moon, "lunar_module", new Vector3(11f, 0f, 9f), 200f, 7f);
            SettingAssets.PlaceModel(root, moon, "astronaut", new Vector3(3.8f, 0f, -3.4f), -120f, 1.85f);
            // Habitat: NASA's Habitat Demonstration Unit when the art is there, else three domes and tubes.
            bool nasaHabitat = SettingAssets.PlaceModel(root, moon, "habitat_part1", new Vector3(-14f, 0f, -6f), 90f, 8f) != null;
            if (nasaHabitat)
            {
                SettingAssets.PlaceModel(root, moon, "habitat_part2", new Vector3(-15f, 0f, 7f), 90f, 10f);
            }
            // Habitat: three domes joined by tubes, with lit windows.
            Vector3[] domes = { new Vector3(-12f, 0f, -9f), new Vector3(-16f, 0f, 1f), new Vector3(-11f, 0f, 9f) };
            float[] radii = { 3.2f, 4.2f, 2.8f };
            for (int i = 0; i < domes.Length && !nasaHabitat; i++)
            {
                Part(root, "Dome", domes[i], Vector3.one * radii[i] * 2f, Hex(0xDCE0E5), PrimitiveType.Sphere);
                Glow(root, "Dome window", domes[i] + new Vector3(radii[i] * 0.93f, radii[i] * 0.35f, 0f),
                    new Vector3(0.08f, 0.5f, 1.6f), Hex(0x8FE3FF));
                if (i > 0)
                {
                    Vector3 from = domes[i - 1], to = domes[i];
                    GameObject tube = Part(root, "Tube", (from + to) / 2f + new Vector3(0f, 0.8f, 0f),
                        new Vector3(1.4f, Vector3.Distance(from, to) / 2f, 1.4f), Hex(0xB9BEC6), PrimitiveType.Cylinder);
                    tube.transform.rotation = Quaternion.FromToRotation(Vector3.up, to - from);
                }
            }
            // Antenna mast with a dish and a blinking-red tip.
            Part(root, "Antenna mast", new Vector3(-7f, 4f, -15f), new Vector3(0.3f, 4f, 0.3f), Hex(0xA8ADB5), PrimitiveType.Cylinder);
            GameObject dish = Part(root, "Dish", new Vector3(-7f, 6.5f, -15f), new Vector3(3f, 0.5f, 3f), Hex(0xE6E8EB), PrimitiveType.Sphere);
            dish.transform.rotation = Quaternion.Euler(-35f, 40f, 0f);
            Glow(root, "Antenna light", new Vector3(-7f, 8.15f, -15f), Vector3.one * 0.3f, Hex(0xFF3B30), PrimitiveType.Sphere);
            // A parked rover: NASA's Space Exploration Vehicle (a pressurized lunar rover) when the art is
            // there, else a simple box rover.
            bool nasaRover = SettingAssets.PlaceModel(root, moon, "lunar_rover", new Vector3(9f, 0f, -11f), 30f, 6f) != null;
            if (!nasaRover)
            {
                Part(root, "Rover body", new Vector3(9f, 0.7f, -11f), new Vector3(2.6f, 0.6f, 1.6f), Hex(0xC9CDD3));
            }
            for (int w = 0; w < 4 && !nasaRover; w++)
            {
                GameObject wheel = Part(root, "Rover wheel", new Vector3(9f + (w < 2 ? -0.9f : 0.9f), 0.35f, -11f + (w % 2 == 0 ? -0.85f : 0.85f)),
                    new Vector3(0.7f, 0.12f, 0.7f), Hex(0x2A2C30), PrimitiveType.Cylinder);
                wheel.transform.rotation = Quaternion.Euler(90f, 0f, 0f);
            }

            if (SettingAssets.Sky(moon) == null)
            {
                Stars(root, random, 600, 150f); // NASA's star map, when it's there, is the real sky
            }
            // Earth, hanging over the horizon: oceans, land and clouds.
            var earth = new Vector3(-110f, 45f, 70f);
            GameObject globe = Glow(root, "Earth", earth, Vector3.one * 22f, Hex(0x2C6BD2), PrimitiveType.Sphere);
            Texture2D earthMap = SettingAssets.Texture(moon, "earth");
            if (earthMap != null)
            {
                // NASA's Blue Marble wrapped round the globe, turned so the Atlantic faces the base.
                globe.GetComponent<Renderer>().material = new Material(Shader.Find("Unlit/Texture")) { name = "Earth", mainTexture = earthMap };
                globe.transform.rotation = Quaternion.LookRotation(-earth) * Quaternion.Euler(0f, 110f, 0f);
                return;
            }
            // Land and clouds sit just out of the globe on the side facing the base, so they show.
            Vector3 toBase = (-earth).normalized;
            Vector3 side = Vector3.Cross(Vector3.up, toBase).normalized;
            Vector3 up = Vector3.Cross(toBase, side).normalized;
            (Vector3 offset, Vector3 size, Color color)[] patches =
            {
                (side * 3f + up * 3f, new Vector3(9f, 7f, 2f), Hex(0x4C9A52)),
                (-side * 4f - up * 2f, new Vector3(6f, 8f, 2f), Hex(0x5BA35A)),
                (-side * 1f + up * 6.5f, new Vector3(9f, 2.2f, 2f), Hex(0xF4F6F8)),
                (side * 4.5f - up * 4.5f, new Vector3(7f, 2f, 2f), Hex(0xF4F6F8)),
            };
            foreach (var patch in patches)
            {
                Vector3 onSurface = earth + (patch.offset + toBase * 11f).normalized * 10.6f;
                GameObject piece = Glow(root, "Earth surface", onSurface, patch.size, patch.color, PrimitiveType.Sphere);
                piece.transform.rotation = Quaternion.LookRotation(onSurface - earth, up);
            }
        }

        // ---- Shared pieces ------------------------------------------------------------------------

        /// <summary>A seated or standing fan: a body in a shirt color and a head.</summary>
        private static void Person(Transform root, System.Random random, Vector3 feet, Color shirt)
        {
            float height = 0.95f + (float)random.NextDouble() * 0.25f;
            Part(root, "Fan", feet + new Vector3(0f, height * 0.42f, 0f), new Vector3(0.36f, height * 0.42f, 0.3f), shirt, PrimitiveType.Capsule);
            Color[] skin = { Hex(0xF1C9A5), Hex(0xD9A57A), Hex(0xA8714A), Hex(0x6E4A2F) };
            Part(root, "Head", feet + new Vector3(0f, height * 0.92f, 0f), Vector3.one * 0.22f, skin[random.Next(skin.Length)], PrimitiveType.Sphere);
        }

        private static Color FanColor(System.Random random)
        {
            Color[] shirts =
            {
                Hex(0xD8352C), Hex(0x2F62E0), Hex(0xF2F2F2), Hex(0xF29A2E), Hex(0x3BAA5C), Hex(0xF2C230), Hex(0x7A4BC4), Hex(0x2A2D33),
            };
            return shirts[random.Next(shirts.Length)];
        }

        /// <summary>Small bright dots spread over the upper sky.</summary>
        private static void Stars(Transform root, System.Random random, int count, float distance)
        {
            for (int i = 0; i < count; i++)
            {
                float yaw = (float)random.NextDouble() * Mathf.PI * 2f;
                float up = Mathf.Asin(0.08f + (float)random.NextDouble() * 0.92f);
                var at = new Vector3(Mathf.Cos(yaw) * Mathf.Cos(up), Mathf.Sin(up), Mathf.Sin(yaw) * Mathf.Cos(up)) * distance;
                float size = 0.15f + (float)random.NextDouble() * 0.35f;
                Glow(root, "Star", at, Vector3.one * size, Color.Lerp(Hex(0xBFD4FF), Color.white, (float)random.NextDouble()));
            }
        }

        /// <summary>A random spot on the ground between two distances from the center (outside the field).</summary>
        private static Vector3 AwayFromField(System.Random random, float near, float far)
        {
            float angle = (float)random.NextDouble() * Mathf.PI * 2f;
            float distance = near + (float)random.NextDouble() * (far - near);
            return new Vector3(Mathf.Cos(angle) * distance, 0f, Mathf.Sin(angle) * distance);
        }

        private static void Sign(Transform root, string text, Vector3 position, Quaternion rotation, float size, Color color)
        {
            var label = new GameObject("Sign " + text).AddComponent<TextMesh>();
            label.transform.SetParent(root, false);
            label.transform.SetPositionAndRotation(position, rotation);
            label.text = text;
            label.font = Resources.GetBuiltinResource<Font>("LegacyRuntime.ttf");
            label.GetComponent<MeshRenderer>().material = label.font.material;
            label.fontSize = 64;
            label.characterSize = size;
            label.anchor = TextAnchor.MiddleCenter;
            label.color = color;
        }

        private static readonly Dictionary<(Color, bool), Material> Materials = new Dictionary<(Color, bool), Material>();

        /// <summary>A shaded (lit) shape with no collider.</summary>
        private static GameObject Part(Transform root, string name, Vector3 position, Vector3 size, Color color,
            PrimitiveType type = PrimitiveType.Cube)
        {
            return Shape(root, name, position, size, color, type, false);
        }

        /// <summary>A shape that shows its exact color whatever the light: lamps, screens, stars, painted lines.</summary>
        private static GameObject Glow(Transform root, string name, Vector3 position, Vector3 size, Color color,
            PrimitiveType type = PrimitiveType.Cube)
        {
            return Shape(root, name, position, size, color, type, true);
        }

        private static GameObject Shape(Transform root, string name, Vector3 position, Vector3 size, Color color, PrimitiveType type, bool glow)
        {
            GameObject shape = GameObject.CreatePrimitive(type);
            shape.name = name;
            Object.DestroyImmediate(shape.GetComponent<Collider>()); // scenery: Unity's physics only sees the field
            shape.transform.SetParent(root, false);
            shape.transform.localPosition = position;
            shape.transform.localScale = size;
            Renderer renderer = shape.GetComponent<Renderer>();
            if (!Materials.TryGetValue((color, glow), out Material material) || material == null)
            {
                material = new Material(Shader.Find(glow ? "Unlit/Color" : "Standard")) { color = color };
                if (!glow)
                {
                    material.SetFloat("_Glossiness", 0.1f);
                }
                Materials[(color, glow)] = material;
            }
            renderer.sharedMaterial = material;
            if (glow)
            {
                renderer.shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.Off;
            }
            return shape;
        }

        private static Color Hex(int rgb)
        {
            return new Color(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f);
        }
    }
}

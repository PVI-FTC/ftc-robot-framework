using System.Collections.Generic;
using UnityEngine;
using UnityEngine.Animations;
using UnityEngine.Playables;

namespace PviHarness.Replay
{
    /// <summary>
    /// Real people for the settings, from the Mixamo characters in Resources/People (built by
    /// <b>Harness → Build People</b>). Every copy looks different: a random character, its own shirt,
    /// trouser, shoe and hair colors, a slightly different height, and its own pose or animation timing.
    ///
    /// <para><b>Fans</b> are frozen in a seated pose (sitting, or a moment of clapping), captured once per
    /// character and pose and shared by every fan who uses it, so a full stand stays fast.
    /// <b>Drive team</b> members are animated (standing idle). Without the people files the settings
    /// fall back to their simple capsule figures.</para>
    /// </summary>
    public static class People
    {
        /// <summary>How far a seated pose's hips are above its feet (m): the seat height it expects.</summary>
        public const float SeatHeightMeters = 0.45f;

        private static List<GameObject> characters;
        private static readonly Dictionary<(int, string, float), List<Part>> Poses = new Dictionary<(int, string, float), List<Part>>();
        private static readonly Dictionary<(Material, Color), Material> Tinted = new Dictionary<(Material, Color), Material>();

        /// <summary>One frozen body part: its shape in the pose, materials, and place relative to the feet.</summary>
        private sealed class Part
        {
            public string name;
            public Mesh mesh;
            public Material[] materials;
            public Vector3 position;
            public Quaternion rotation;
        }

        /// <summary>The fans' poses: (animation, moment as a fraction of its length).</summary>
        private static readonly (string clip, float at)[] SeatedPoses =
        {
            ("sit", 0.5f), ("sit_clap", 0.15f), ("sit_clap", 0.55f), ("sit_clap", 0.85f),
        };

        private static readonly Color[] ShirtColors =
        {
            Hex(0xD8352C), Hex(0x2F62E0), Hex(0xF2F2F2), Hex(0xF29A2E), Hex(0x3BAA5C), Hex(0xF2C230), Hex(0x7A4BC4), Hex(0x2A2D33),
            Hex(0x1FA3A3), Hex(0xE36FA8),
        };
        private static readonly Color[] TrouserColors = { Hex(0x3B5A8C), Hex(0x2A2C30), Hex(0x77787C), Hex(0xB8A27A), Hex(0x4E3B2C) };
        private static readonly Color[] ShoeColors = { Color.white, Hex(0x2A2C30), Hex(0x8C8F94) };
        // Hair textures are light, so these tints give natural dark brown, black, brown, auburn, blonde and grey.
        private static readonly Color[] HairColors =
        {
            Hex(0x6B4A30), Hex(0x2E2622), Hex(0x8C5A32), Hex(0xA0522D), Hex(0xE0C080), Hex(0xBEBEBE),
        };

        /// <summary>True if the people files are in the project.</summary>
        public static bool Available
        {
            get
            {
                Load();
                return characters.Count > 0 && Resources.Load<AnimationClip>("People/animations/sit") != null;
            }
        }

        /// <summary>How many different characters there are.</summary>
        public static int CharacterCount
        {
            get
            {
                Load();
                return characters.Count;
            }
        }

        private static void Load()
        {
            if (characters != null && characters.TrueForAll(c => c != null))
            {
                return;
            }
            characters = new List<GameObject>();
            foreach (GameObject prefab in Resources.LoadAll<GameObject>("People"))
            {
                if (prefab.GetComponentInChildren<SkinnedMeshRenderer>() != null && prefab.GetComponent<Animator>() != null)
                {
                    characters.Add(prefab);
                }
            }
            characters.Sort((a, b) => string.CompareOrdinal(a.name, b.name)); // a fixed order, so a seed gives the same crowd
        }

        /// <summary>
        /// A seated fan whose seat is at <paramref name="seat"/> (feet hang <see cref="SeatHeightMeters"/>
        /// lower), facing <paramref name="yawDegrees"/> (Unity yaw). Returns null without the people files.
        /// </summary>
        public static GameObject PlaceFan(Transform parent, System.Random random, Vector3 seat, float yawDegrees)
        {
            if (!Available)
            {
                return null;
            }
            int character = random.Next(characters.Count);
            (string clip, float at) pose = SeatedPoses[random.Next(SeatedPoses.Length)];
            List<Part> parts = Pose(character, pose.clip, pose.at);
            if (parts == null)
            {
                return null;
            }
            var fan = new GameObject("Fan " + characters[character].name).transform;
            fan.SetParent(parent, false);
            fan.localPosition = seat - new Vector3(0f, SeatHeightMeters, 0f);
            fan.localRotation = Quaternion.Euler(0f, yawDegrees + (float)(random.NextDouble() - 0.5) * 20f, 0f);
            fan.localScale = Vector3.one * (0.94f + (float)random.NextDouble() * 0.12f);
            Outfit outfit = Outfit.Random(random);
            foreach (Part part in parts)
            {
                var piece = new GameObject(part.name);
                piece.transform.SetParent(fan, false);
                piece.transform.localPosition = part.position;
                piece.transform.localRotation = part.rotation;
                piece.AddComponent<MeshFilter>().sharedMesh = part.mesh;
                var renderer = piece.AddComponent<MeshRenderer>();
                renderer.sharedMaterials = Dress(part.name, part.materials, outfit);
                renderer.shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.Off; // a crowd's shadows cost a lot for little
            }
            return fan.gameObject;
        }

        /// <summary>
        /// A standing, animated person (a drive-team member) at <paramref name="feet"/>, facing
        /// <paramref name="yawDegrees"/>, in a shirt of <paramref name="shirt"/>. Returns null without the files.
        /// </summary>
        public static GameObject PlaceStanding(Transform parent, System.Random random, Vector3 feet, float yawDegrees, Color shirt)
        {
            if (!Available)
            {
                return null;
            }
            GameObject prefab = characters[random.Next(characters.Count)];
            var person = Object.Instantiate(prefab, parent, false);
            person.name = "Driver " + prefab.name;
            person.transform.localPosition = feet;
            person.transform.localRotation = Quaternion.Euler(0f, yawDegrees, 0f);
            person.transform.localScale = Vector3.one * (0.95f + (float)random.NextDouble() * 0.1f);
            Outfit outfit = Outfit.Random(random);
            outfit.shirt = shirt;
            foreach (SkinnedMeshRenderer smr in person.GetComponentsInChildren<SkinnedMeshRenderer>())
            {
                smr.sharedMaterials = Dress(smr.name, smr.sharedMaterials, outfit);
            }
            string clipName = random.NextDouble() < 0.5 ? "stand_idle" : "stand_idle_2";
            AnimationClip clip = Resources.Load<AnimationClip>("People/animations/" + clipName)
                ?? Resources.Load<AnimationClip>("People/animations/stand_idle");
            Animator animator = person.GetComponent<Animator>();
            if (clip != null && animator != null)
            {
                person.AddComponent<PersonAnimation>().Play(animator, clip, (float)random.NextDouble() * clip.length);
            }
            return person;
        }

        /// <summary>The colors one person wears.</summary>
        private struct Outfit
        {
            public Color shirt, trousers, shoes, hair;

            public static Outfit Random(System.Random random)
            {
                return new Outfit
                {
                    shirt = ShirtColors[random.Next(ShirtColors.Length)],
                    trousers = TrouserColors[random.Next(TrouserColors.Length)],
                    shoes = ShoeColors[random.Next(ShoeColors.Length)],
                    hair = HairColors[random.Next(HairColors.Length)],
                };
            }
        }

        /// <summary>The part's materials, tinted for this outfit (skin and eyes stay as they are).</summary>
        private static Material[] Dress(string partName, Material[] materials, Outfit outfit)
        {
            string n = partName.ToLowerInvariant();
            Color? tint = n.Contains("shirt") || n.Contains("hoodie") || n.Contains("coat") || n.Contains("top") ? outfit.shirt
                : n.Contains("pants") || n.Contains("shorts") || n.Contains("trouser") ? outfit.trousers
                : n.Contains("shoe") || n.Contains("sneaker") || n.Contains("boot") ? outfit.shoes
                : n.Contains("hair") || n.Contains("beard") ? outfit.hair
                : (Color?)null;
            if (tint == null)
            {
                return materials;
            }
            var dressed = new Material[materials.Length];
            for (int i = 0; i < materials.Length; i++)
            {
                dressed[i] = TintedCopy(materials[i], tint.Value);
            }
            return dressed;
        }

        /// <summary>One shared copy per (material, color), so fans in the same colors draw together.</summary>
        private static Material TintedCopy(Material original, Color tint)
        {
            if (!Tinted.TryGetValue((original, tint), out Material copy) || copy == null)
            {
                copy = new Material(original) { name = original.name + " " + ColorUtility.ToHtmlStringRGB(tint), color = tint, enableInstancing = true };
                Tinted[(original, tint)] = copy;
            }
            return copy;
        }

        /// <summary>A character frozen in one moment of an animation, captured once and reused.</summary>
        private static List<Part> Pose(int character, string clipName, float at)
        {
            if (Poses.TryGetValue((character, clipName, at), out List<Part> cached) && cached.TrueForAll(p => p.mesh != null))
            {
                return cached;
            }
            AnimationClip clip = Resources.Load<AnimationClip>("People/animations/" + clipName);
            if (clip == null)
            {
                return null;
            }
            GameObject posed = Object.Instantiate(characters[character]);
            posed.hideFlags = HideFlags.HideAndDontSave;
            clip.SampleAnimation(posed, clip.length * at);
            posed.transform.SetPositionAndRotation(Vector3.zero, Quaternion.identity);
            var parts = new List<Part>();
            foreach (SkinnedMeshRenderer smr in posed.GetComponentsInChildren<SkinnedMeshRenderer>())
            {
                var mesh = new Mesh { name = smr.name + " " + clipName };
                smr.BakeMesh(mesh, true);
                mesh.RecalculateBounds();
                parts.Add(new Part
                {
                    name = smr.name,
                    mesh = mesh,
                    materials = smr.sharedMaterials,
                    position = posed.transform.InverseTransformPoint(smr.transform.position),
                    rotation = Quaternion.Inverse(posed.transform.rotation) * smr.transform.rotation,
                });
            }
            if (Application.isPlaying)
            {
                Object.Destroy(posed);
            }
            else
            {
                Object.DestroyImmediate(posed);
            }
            Poses[(character, clipName, at)] = parts;
            return parts;
        }

        private static Color Hex(int rgb)
        {
            return new Color(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f);
        }
    }

    /// <summary>Plays one looping animation on a person, starting part-way in so a group isn't in step.</summary>
    [ExecuteAlways]
    public sealed class PersonAnimation : MonoBehaviour
    {
        private PlayableGraph graph;

        /// <summary>Starts <paramref name="clip"/> on <paramref name="animator"/>, <paramref name="startSeconds"/> in.</summary>
        public void Play(Animator animator, AnimationClip clip, float startSeconds)
        {
            AnimationClipPlayable playable = AnimationPlayableUtilities.PlayClip(animator, clip, out graph);
            playable.SetTime(startSeconds);
            graph.SetTimeUpdateMode(DirectorUpdateMode.UnscaledGameTime);
        }

        /// <summary>True while the animation is set up.</summary>
        public bool Playing => graph.IsValid();

        private void OnDestroy()
        {
            if (graph.IsValid())
            {
                graph.Destroy();
            }
        }
    }
}

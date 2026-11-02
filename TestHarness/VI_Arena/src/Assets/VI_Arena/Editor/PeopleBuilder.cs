using System.Collections.Generic;
using System.IO;
using System.Linq;
using UnityEditor;
using UnityEngine;

namespace VI_Arena.Editor
{
    /// <summary>
    /// <b>VI Arena → Build People</b>: turns the raw Mixamo downloads into light, reusable people for the
    /// crowds and drive teams (Resources/People). Mixamo puts the whole character, with 4K textures, into
    /// every download (50–90 MB each), so this keeps one copy of each character and each animation:
    /// <list type="number">
    ///   <item>Copy the .fbx files into Assets/VI_Arena/PeopleSource (git-ignored); they import as
    ///   humanoids (<see cref="PeopleImport"/>).</item>
    ///   <item>Put each character's 2K textures in Resources/People/&lt;chNN&gt;/textures
    ///   (<c>_Diffuse</c> .jpg, or .png for see-through hair, and <c>_Normal</c> .jpg). The originals are 4K
    ///   PNGs inside the FBX; extract them with the model importer and shrink them.</item>
    ///   <item>Run this. Per character it saves the meshes, an avatar, materials and a prefab; every
    ///   animation is saved once under Resources/People/animations, named by what it is
    ///   (sit, sit_clap, stand_clap, stand_idle, ...). Humanoid animations play on any character.</item>
    ///   <item>Delete PeopleSource (the raw files stay in C:\Dev\VI_Arena-art).</item>
    /// </list>
    /// </summary>
    public static class PeopleBuilder
    {
        public const string Source = "Assets/VI_Arena/PeopleSource";
        public const string OutRoot = "Assets/VI_Arena/Resources/People";

        [MenuItem("VI Arena/Build People")]
        public static void Build()
        {
            Debug.Log("[VI_Arena] " + BuildAll());
        }

        /// <summary>Builds every character and animation found in <see cref="Source"/>; returns a summary.</summary>
        public static string BuildAll()
        {
            Directory.CreateDirectory(OutRoot + "/animations");
            var lengths = new Dictionary<string, float>();
            var characters = new Dictionary<string, string>();
            var log = new List<string>();
            foreach (string guid in AssetDatabase.FindAssets("t:Model", new[] { Source }))
            {
                string path = AssetDatabase.GUIDToAssetPath(guid);
                string file = Path.GetFileNameWithoutExtension(path);
                var model = AssetDatabase.LoadAssetAtPath<GameObject>(path);
                SkinnedMeshRenderer first = model.GetComponentsInChildren<SkinnedMeshRenderer>().FirstOrDefault();
                if (first != null)
                {
                    string id = first.name.Split('_')[0].ToLowerInvariant();
                    if (!characters.ContainsKey(id))
                    {
                        characters[id] = path;
                    }
                }
                AnimationClip clip = AssetDatabase.LoadAllAssetsAtPath(path).OfType<AnimationClip>()
                    .FirstOrDefault(c => !c.name.StartsWith("__preview"));
                if (clip == null || !clip.isHumanMotion)
                {
                    continue;
                }
                string kind = KindOf(file);
                // The same animation downloaded on another character has the same length: keep one.
                if (lengths.Any(s => s.Key.StartsWith(kind) && Mathf.Abs(s.Value - clip.length) < 0.01f))
                {
                    continue;
                }
                int count = lengths.Keys.Count(k => k.StartsWith(kind));
                string name = count == 0 ? kind : kind + "_" + (count + 1);
                AnimationClip copy = Object.Instantiate(clip);
                copy.name = name;
                AssetDatabase.CreateAsset(copy, OutRoot + "/animations/" + name + ".anim");
                lengths[name] = clip.length;
                log.Add(name);
            }
            foreach (var pair in characters)
            {
                BuildCharacter(pair.Key, pair.Value);
            }
            AssetDatabase.SaveAssets();
            return "Built people " + string.Join(", ", characters.Keys) + "; animations " + string.Join(", ", log);
        }

        /// <summary>What a Mixamo download's file name says it is: sit_clap, sit, stand_clap or stand_idle.</summary>
        public static string KindOf(string fileName)
        {
            string f = fileName.ToLowerInvariant().Replace(" ", "_");
            if (f.StartsWith("sitting_clap")) return "sit_clap";
            if (f.StartsWith("sitting") || f.StartsWith("sit")) return "sit";
            if (f.StartsWith("clapping") || f.Contains("cheer")) return "stand_clap";
            return "stand_idle";
        }

        private static void BuildCharacter(string id, string fbxPath)
        {
            string folder = OutRoot + "/" + id;
            Directory.CreateDirectory(folder + "/meshes");
            Directory.CreateDirectory(folder + "/materials");
            var model = AssetDatabase.LoadAssetAtPath<GameObject>(fbxPath);
            var instance = (GameObject)Object.Instantiate(model);
            instance.name = id;
            string prefix = model.GetComponentsInChildren<SkinnedMeshRenderer>()[0].name.Split('_')[0];
            var materials = new Dictionary<Material, Material>();
            foreach (SkinnedMeshRenderer smr in instance.GetComponentsInChildren<SkinnedMeshRenderer>())
            {
                Mesh mesh = Object.Instantiate(smr.sharedMesh);
                mesh.name = smr.name;
                AssetDatabase.CreateAsset(mesh, folder + "/meshes/" + smr.name + ".asset");
                smr.sharedMesh = mesh;
                var replaced = new Material[smr.sharedMaterials.Length];
                for (int i = 0; i < replaced.Length; i++)
                {
                    Material old = smr.sharedMaterials[i];
                    if (!materials.TryGetValue(old, out Material made))
                    {
                        made = MakeMaterial(folder, prefix, old);
                        AssetDatabase.CreateAsset(made, folder + "/materials/" + old.name + ".mat");
                        materials[old] = made;
                    }
                    replaced[i] = made;
                }
                smr.sharedMaterials = replaced;
            }
            var animator = instance.GetComponent<Animator>();
            if (animator != null && animator.avatar != null)
            {
                Avatar avatar = Object.Instantiate(animator.avatar);
                avatar.name = id + "_avatar";
                AssetDatabase.CreateAsset(avatar, folder + "/" + id + "_avatar.asset");
                animator.avatar = avatar;
                animator.runtimeAnimatorController = null;
            }
            PrefabUtility.SaveAsPrefabAsset(instance, folder + "/" + id + ".prefab");
            Object.DestroyImmediate(instance);
        }

        /// <summary>A Standard material from the character's 2K textures (texture set from the original material).</summary>
        private static Material MakeMaterial(string folder, string prefix, Material old)
        {
            string set = old.mainTexture != null && old.mainTexture.name.Split('_').Length > 1 ? old.mainTexture.name.Split('_')[1] : "1001";
            string baseName = folder + "/textures/" + prefix + "_" + set;
            var made = new Material(Shader.Find("Standard")) { name = old.name, enableInstancing = true };
            Texture2D albedo = AssetDatabase.LoadAssetAtPath<Texture2D>(baseName + "_Diffuse.jpg") ?? AssetDatabase.LoadAssetAtPath<Texture2D>(baseName + "_Diffuse.png");
            Texture2D normal = AssetDatabase.LoadAssetAtPath<Texture2D>(baseName + "_Normal.jpg");
            made.mainTexture = albedo;
            if (normal != null)
            {
                made.SetTexture("_BumpMap", normal);
                made.EnableKeyword("_NORMALMAP");
            }
            made.SetFloat("_Glossiness", 0.25f);
            if (albedo != null && AssetDatabase.GetAssetPath(albedo).EndsWith(".png"))
            {
                // Hair and lashes: cut out the see-through parts.
                made.SetFloat("_Mode", 1f);
                made.SetFloat("_Cutoff", 0.4f);
                made.EnableKeyword("_ALPHATEST_ON");
                made.SetOverrideTag("RenderType", "TransparentCutout");
                made.renderQueue = 2450;
            }
            return made;
        }
    }
}

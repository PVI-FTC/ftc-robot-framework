using System.IO;
using UnityEditor;
using UnityEditor.Build.Reporting;
using UnityEngine;

namespace PviHarness.Replay.Editor
{
    /// <summary>
    /// <b>Harness → Build Windows App</b>: builds the replay as a stand-alone Windows app,
    /// <c>Harness/unity/Build/HarnessReplay.exe</c> (plus its <c>HarnessReplay_Data</c> folder), so it
    /// opens with a double-click (<c>Harness/unity-view.bat</c>) without the Unity Editor.
    ///
    /// <para>The Build folder is ignored by git (it is 100+ MB and rebuilt from the scripts). Rebuild
    /// after changing any Unity script. Headless, with the Editor closed: <c>Harness/buildUnity.bat</c>,
    /// which calls <see cref="BuildFromCommandLine"/>.</para>
    /// </summary>
    public static class BuildWindowsApp
    {
        /// <summary>The scene the app opens.</summary>
        public const string Scene = "Assets/HarnessReplay/Replay.unity";

        /// <summary>Where the app goes: Harness/unity/Build/HarnessReplay.exe.</summary>
        public static string ExePath => Path.GetFullPath(Path.Combine(Application.dataPath, "..", "Build", "HarnessReplay.exe"));

        [MenuItem("Harness/Build Windows App")]
        public static void Build()
        {
            BuildReport report = BuildApp();
            Debug.Log("[HarnessReplay] Build " + report.summary.result + ": " + ExePath);
        }

        /// <summary>For -executeMethod in batch mode: builds, then quits with 0 (succeeded) or 1.</summary>
        public static void BuildFromCommandLine()
        {
            BuildReport report = BuildApp();
            System.Console.WriteLine("[HarnessReplay] Build " + report.summary.result + ": " + ExePath);
            EditorApplication.Exit(report.summary.result == BuildResult.Succeeded ? 0 : 1);
        }

        private static BuildReport BuildApp()
        {
            // A normal resizable window (not full screen), sized like the Swing viewer.
            PlayerSettings.productName = "HarnessReplay";
            PlayerSettings.companyName = "PVI FTC";
            PlayerSettings.fullScreenMode = FullScreenMode.Windowed;
            PlayerSettings.defaultScreenWidth = 1600;
            PlayerSettings.defaultScreenHeight = 1000;
            PlayerSettings.resizableWindow = true;
            PlayerSettings.runInBackground = true; // keep live play stepping when the window isn't focused
            IncludeShaders();
            KeepNormalMappedVariant();

            var options = new BuildPlayerOptions
            {
                scenes = new[] { Scene },
                locationPathName = ExePath,
                target = BuildTarget.StandaloneWindows64,
                options = BuildOptions.None,
            };
            return BuildPipeline.BuildPlayer(options);
        }

        /// <summary>
        /// The shaders FieldBuilder uses: the ones looked up by name with Shader.Find (including the
        /// see-through panels' <see cref="FieldBuilder.PanelShader"/>), and Standard, which the default
        /// material of its CreatePrimitive boxes (walls, wheels) needs.
        /// </summary>
        public static readonly string[] ShadersFoundByName =
        {
            "Sprites/Default", "Unlit/Color", "Unlit/Texture", "Standard", "Skybox/Cubemap", FieldBuilder.PanelShader,
        };

        /// <summary>
        /// A material saved in the project with the Standard shader's normal-map version switched on.
        /// WHY: the settings make their textured materials in code; a build only keeps the shader versions
        /// that saved materials use, so without this the app would drop the surfaces' bumps.
        /// </summary>
        public const string NormalMappedVariantPath = "Assets/HarnessReplay/Resources/ShaderVariants/StandardNormalMapped.mat";

        private static void KeepNormalMappedVariant()
        {
            if (AssetDatabase.LoadAssetAtPath<Material>(NormalMappedVariantPath) != null)
            {
                return;
            }
            System.IO.Directory.CreateDirectory(System.IO.Path.GetDirectoryName(NormalMappedVariantPath));
            var material = new Material(Shader.Find("Standard"));
            material.EnableKeyword("_NORMALMAP");
            material.SetTexture("_BumpMap", Texture2D.normalTexture);
            AssetDatabase.CreateAsset(material, NormalMappedVariantPath);
            AssetDatabase.SaveAssets();
        }

        /// <summary>
        /// Adds <see cref="ShadersFoundByName"/> to Graphics Settings → Always Included Shaders.
        /// WHY: a build leaves out every shader no scene or asset refers to, and the field is built from
        /// code, so Shader.Find would return null in the app (a magenta field and no recording).
        /// </summary>
        private static void IncludeShaders()
        {
            var settings = new SerializedObject(AssetDatabase.LoadAssetAtPath<Object>("ProjectSettings/GraphicsSettings.asset"));
            SerializedProperty list = settings.FindProperty("m_AlwaysIncludedShaders");
            foreach (string name in ShadersFoundByName)
            {
                Shader shader = Shader.Find(name);
                bool present = false;
                for (int i = 0; i < list.arraySize; i++)
                {
                    present |= list.GetArrayElementAtIndex(i).objectReferenceValue == shader;
                }
                if (!present)
                {
                    list.arraySize++;
                    list.GetArrayElementAtIndex(list.arraySize - 1).objectReferenceValue = shader;
                }
            }
            settings.ApplyModifiedPropertiesWithoutUndo();
            AssetDatabase.SaveAssets();
        }
    }
}

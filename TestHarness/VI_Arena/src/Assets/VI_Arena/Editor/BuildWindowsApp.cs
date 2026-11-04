using System.IO;
using UnityEditor;
using UnityEditor.Build.Reporting;
using UnityEngine;

namespace VI_Arena.Editor
{
    /// <summary>
    /// <b>VI Arena → Build Windows App</b>: builds VI Arena as a stand-alone Windows app,
    /// <c>TestHarness/VI_Arena/dist/VI_Arena.exe</c> (plus its <c>VI_Arena_Data</c> folder), so it
    /// opens with a double-click (<c>TestHarness/VI_Arena.bat</c>) without the Unity Editor.
    ///
    /// <para>The build also gets its own copy of the field files and robot looks
    /// (<c>VI_Arena_Data/harness-files</c>, from th_src/fields and th_src/robots), so a copy of dist
    /// unzipped anywhere still draws the season field and the robot.</para>
    ///
    /// <para>The dist folder is ignored by git (it is 100+ MB and rebuilt from the scripts); share it
    /// as a zip with <c>VI_Arena/package.bat</c>. Rebuild after changing any Unity script. Headless,
    /// with the Editor closed: <c>VI_Arena/src/buildUnity.bat</c>, which calls
    /// <see cref="BuildFromCommandLine"/>.</para>
    /// </summary>
    public static class BuildWindowsApp
    {
        /// <summary>The scene the app opens.</summary>
        public const string Scene = "Assets/VI_Arena/Replay.unity";

        /// <summary>Where the app goes: TestHarness/VI_Arena/dist/VI_Arena.exe.</summary>
        public static string ExePath => Path.GetFullPath(Path.Combine(Application.dataPath, "..", "..", "dist", "VI_Arena.exe"));

        [MenuItem("VI Arena/Build Windows App")]
        public static void Build()
        {
            BuildReport report = BuildApp(ExePath);
            Debug.Log("[VI_Arena] Build " + report.summary.result + ": " + ExePath);
        }

        /// <summary>
        /// For -executeMethod in batch mode: builds, then quits with 0 (succeeded) or 1. "-buildPath
        /// &lt;exe&gt;" builds a copy somewhere else instead (for checking while someone has the app open,
        /// e.g. TestHarness/th_src/build/unity-check/VI_Arena.exe; anywhere inside TestHarness still
        /// finds th_src/build/recordings, fields and robots).
        /// </summary>
        public static void BuildFromCommandLine()
        {
            string path = ExePath;
            string[] args = System.Environment.GetCommandLineArgs();
            for (int i = 0; i < args.Length - 1; i++)
            {
                if (string.Equals(args[i], "-buildPath", System.StringComparison.OrdinalIgnoreCase))
                {
                    path = Path.GetFullPath(args[i + 1]);
                }
            }
            BuildReport report = BuildApp(path);
            System.Console.WriteLine("[VI_Arena] Build " + report.summary.result + ": " + path);
            EditorApplication.Exit(report.summary.result == BuildResult.Succeeded ? 0 : 1);
        }

        private static BuildReport BuildApp(string exePath)
        {
            // A normal resizable window (not full screen), sized like the Swing viewer.
            PlayerSettings.productName = "VI Arena";
            PlayerSettings.companyName = "PVI FTC";
            PlayerSettings.fullScreenMode = FullScreenMode.Windowed;
            PlayerSettings.defaultScreenWidth = 1600;
            PlayerSettings.defaultScreenHeight = 1000;
            PlayerSettings.resizableWindow = true;
            PlayerSettings.runInBackground = true; // keep live play stepping when the window isn't focused
            // No "Made with Unity" screen (optional since Unity 6); the app shows its own logo (SplashOverlay).
            PlayerSettings.SplashScreen.show = false;
            PlayerSettings.SplashScreen.showUnityLogo = false;
            IncludeShaders();
            KeepNormalMappedVariant();

            var options = new BuildPlayerOptions
            {
                scenes = new[] { Scene },
                locationPathName = exePath,
                target = BuildTarget.StandaloneWindows64,
                options = BuildOptions.None,
            };
            BuildReport report = BuildPipeline.BuildPlayer(options);
            if (report.summary.result == BuildResult.Succeeded)
            {
                CopyHarnessFiles(exePath);
            }
            return report;
        }

        /// <summary>
        /// Copies th_src/fields (the field files, not their sources) and th_src/robots (robot.properties)
        /// into the app's VI_Arena_Data/harness-files, which the app reads when it can't find a
        /// TestHarness folder (<see cref="TestHarnessFolder.HarnessFiles"/>).
        /// </summary>
        private static void CopyHarnessFiles(string exePath)
        {
            string javaHarness = TestHarnessFolder.JavaHarness(TestHarnessFolder.Search(Application.dataPath));
            string data = Path.Combine(Path.GetDirectoryName(exePath),
                Path.GetFileNameWithoutExtension(exePath) + "_Data", TestHarnessFolder.BundledFolderName);
            if (Directory.Exists(data))
            {
                Directory.Delete(data, true);
            }
            foreach (string field in Directory.GetFiles(Path.Combine(javaHarness, "fields"), "*.json"))
            {
                Directory.CreateDirectory(Path.Combine(data, "fields"));
                File.Copy(field, Path.Combine(data, "fields", Path.GetFileName(field)));
            }
            foreach (string robot in Directory.GetFiles(Path.Combine(javaHarness, "robots"), "robot.properties", SearchOption.AllDirectories))
            {
                string id = Path.GetFileName(Path.GetDirectoryName(robot));
                Directory.CreateDirectory(Path.Combine(data, "robots", id));
                File.Copy(robot, Path.Combine(data, "robots", id, "robot.properties"));
            }
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
        public const string NormalMappedVariantPath = "Assets/VI_Arena/Resources/ShaderVariants/StandardNormalMapped.mat";

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

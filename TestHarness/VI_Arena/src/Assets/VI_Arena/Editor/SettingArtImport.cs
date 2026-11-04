using UnityEditor;

namespace VI_Arena.Editor
{
    /// <summary>
    /// Import settings for the settings' art (Resources/Settings/&lt;setting id&gt;/...): skies, surface
    /// textures and models from Poly Haven, ambientCG and NASA (see Art/CREDITS.md).
    /// <list type="bullet">
    ///   <item><c>sky.hdr</c> / <c>sky.exr</c>: a 360° picture, turned into a cubemap for the skybox.</item>
    ///   <item><c>*_normal.*</c> and models' <c>normal.*</c>: normal maps (bumps).</item>
    ///   <item><c>earth.jpg</c>: kept large so Earth looks sharp in the moon sky.</item>
    ///   <item>models: imported at their own scale; the code sizes each one to its real size.</item>
    /// </list>
    /// </summary>
    public sealed class SettingArtImport : AssetPostprocessor
    {
        private bool IsSettingArt => assetPath.Replace('\\', '/').Contains("/Resources/Settings/");

        private void OnPreprocessTexture()
        {
            if (!IsSettingArt)
            {
                return;
            }
            var importer = (TextureImporter)assetImporter;
            string file = System.IO.Path.GetFileNameWithoutExtension(assetPath).ToLowerInvariant();
            if (file == "sky")
            {
                importer.textureShape = TextureImporterShape.TextureCube;
                importer.generateCubemap = TextureImporterGenerateCubemap.AutoCubemap;
                importer.maxTextureSize = 4096;
                importer.mipmapEnabled = false;
            }
            else if (file == "normal" || file.EndsWith("_normal"))
            {
                importer.textureType = TextureImporterType.NormalMap;
                importer.maxTextureSize = 2048;
            }
            else if (file == "earth")
            {
                importer.maxTextureSize = 8192;
            }
            else
            {
                importer.maxTextureSize = 2048;
            }
        }

        private void OnPreprocessModel()
        {
            if (!IsSettingArt)
            {
                return;
            }
            var importer = (ModelImporter)assetImporter;
            importer.importCameras = false;
            importer.importLights = false;
            importer.importAnimation = false;
            importer.animationType = ModelImporterAnimationType.None;
            importer.indexFormat = ModelImporterIndexFormat.UInt32;
            importer.materialImportMode = ModelImporterMaterialImportMode.ImportStandard;
            importer.isReadable = false;
        }
    }
}

using UnityEditor;

namespace VI_Arena.Editor
{
    /// <summary>
    /// Import settings for the people (Resources/People/&lt;name&gt;/...): Mixamo characters and animations.
    /// They import as Unity humanoids, so any animation (sitting, clapping, idle) plays on any character.
    /// Animations loop, so a fan can clap for the whole match.
    /// </summary>
    public sealed class PeopleImport : AssetPostprocessor
    {
        private bool IsPerson
        {
            get
            {
                string path = assetPath.Replace('\\', '/');
                return path.Contains("/Resources/People/") || path.Contains("/PeopleSource/");
            }
        }

        private void OnPreprocessModel()
        {
            if (!IsPerson)
            {
                return;
            }
            var importer = (ModelImporter)assetImporter;
            importer.animationType = ModelImporterAnimationType.Human;
            importer.avatarSetup = ModelImporterAvatarSetup.CreateFromThisModel;
            importer.importAnimation = true;
            importer.importCameras = false;
            importer.importLights = false;
            importer.materialImportMode = ModelImporterMaterialImportMode.ImportStandard;
            importer.isReadable = false;
        }

        private void OnPreprocessTexture()
        {
            if (!IsPerson)
            {
                return;
            }
            var importer = (TextureImporter)assetImporter;
            importer.maxTextureSize = 2048;
            if (assetPath.Contains("_Normal"))
            {
                importer.textureType = TextureImporterType.NormalMap;
            }
            else
            {
                importer.alphaIsTransparency = assetPath.EndsWith(".png"); // hair and lashes have see-through edges
            }
        }

        private void OnPreprocessAnimation()
        {
            if (!IsPerson)
            {
                return;
            }
            var importer = (ModelImporter)assetImporter;
            ModelImporterClipAnimation[] clips = importer.defaultClipAnimations;
            foreach (ModelImporterClipAnimation clip in clips)
            {
                clip.loopTime = true;
                clip.lockRootRotation = true;
                clip.lockRootHeightY = true;
                clip.lockRootPositionXZ = true;
            }
            importer.clipAnimations = clips;
        }
    }
}

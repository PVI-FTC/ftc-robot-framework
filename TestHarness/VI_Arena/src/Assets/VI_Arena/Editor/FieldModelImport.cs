using UnityEditor;

namespace VI_Arena.Editor
{
    /// <summary>
    /// Import settings for the season field CAD (Resources/Field/&lt;field id&gt;/*.obj) and game pieces
    /// (Resources/GamePieces/...). These files come from FIRST's official field CAD, already slimmed
    /// and turned into the FTC field frame (inches, z up; see TestHarness/VI_Arena/ART_INBOX.md).
    ///
    /// <para>The OBJs carry no normals, so Unity calculates them: a 40° smoothing angle keeps the CAD's
    /// sharp edges sharp and its curves smooth. Every triangle is kept (no compression), with 32-bit
    /// indices because some parts have over 65,000 points. The scale stays 1: the code shrinks inches
    /// to meters when it places the model (<see cref="FtcToUnity.FtcModelRotation"/>).</para>
    /// </summary>
    public sealed class FieldModelImport : AssetPostprocessor
    {
        private void OnPreprocessModel()
        {
            string path = assetPath.Replace('\\', '/');
            if (!path.Contains("/Resources/Field/") && !path.Contains("/Resources/GamePieces/"))
            {
                return;
            }
            var importer = (ModelImporter)assetImporter;
            importer.globalScale = 1f;
            importer.importNormals = ModelImporterNormals.Calculate;
            importer.normalSmoothingAngle = 40f;
            importer.meshCompression = ModelImporterMeshCompression.Off;
            importer.indexFormat = ModelImporterIndexFormat.UInt32;
            importer.importBlendShapes = false;
            importer.importCameras = false;
            importer.importLights = false;
            importer.importAnimation = false;
            importer.animationType = ModelImporterAnimationType.None;
            importer.materialImportMode = ModelImporterMaterialImportMode.ImportStandard;
            importer.isReadable = false;
        }
    }
}

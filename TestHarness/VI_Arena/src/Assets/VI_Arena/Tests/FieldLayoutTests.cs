using System.Collections.Generic;
using System.IO;
using System.Linq;
using NUnit.Framework;
using UnityEngine;
using UnityEngine.TestTools;

namespace VI_Arena.Tests
{
    /// <summary>
    /// The season field files in TestHarness/th_src/fields and the code that reads, picks and places them.
    /// </summary>
    public class FieldLayoutTests
    {
        private static string FieldsFolder => FieldLayouts.ResolveFolder(new string[0], TestHarnessFolder.Search(Application.dataPath), Application.dataPath);

        /// <summary>A field file: in th_src/fields, or switched off in th_src/fields/disabled.</summary>
        private static FieldLayoutData LoadField(string id)
        {
            string active = Path.Combine(FieldsFolder, id + ".json");
            return FieldLayouts.Load(File.Exists(active) ? active : Path.Combine(FieldsFolder, "disabled", id + ".json"));
        }

        /// <summary>
        /// WHAT: Every file in th_src/fields loads, and has an id matching its file name. Files moved
        /// into th_src/fields/disabled (DECODE 2025, switched off by the user on 2026-10-24) are not offered.
        /// WHY: The picker silently skips a broken file, so a typo would only show as a missing field.
        /// </summary>
        [Test]
        public void EveryFieldFileLoads()
        {
            var problems = new List<string>();
            List<FieldLayoutData> fields = FieldLayouts.LoadAll(FieldsFolder, problems);

            CollectionAssert.IsEmpty(problems);
            CollectionAssert.AreEquivalent(new[] { "biobuzz-2026" }, fields.Select(f => f.id));
            CollectionAssert.IsEmpty(FieldLayouts.LoadAll(Path.Combine(FieldsFolder, "disabled"), problems)
                .Select(f => f.id).Intersect(fields.Select(f => f.id)), "a disabled field isn't also active");
        }

        /// <summary>
        /// WHAT: BIOBUZZ has the SDK's 16 tags (getBioBuzzTagLibrary): 3.25 in, four clusters, and each
        /// tag's position inside its cluster copied exactly (-6.5, -2.75, 2.75, 6.5 across; 7.1874; -5.622).
        /// </summary>
        [Test]
        public void BioBuzzTagsMatchTheSdk()
        {
            FieldLayoutData field = LoadField("biobuzz-2026");
            string[] clusters = { "RED SCORING", "RED AUDIENCE", "BLUE AUDIENCE", "BLUE SCORING" };
            double[] across = { -6.5, -2.75, 2.75, 6.5 };

            CollectionAssert.AreEqual(Enumerable.Range(30, 16), field.aprilTags.Select(t => t.id));
            foreach (FieldTagData tag in field.aprilTags)
            {
                int index = tag.id - 30;
                Assert.AreEqual(clusters[index / 4], tag.cluster, "tag " + tag.id);
                Assert.AreEqual(3.25, tag.sizeInches, 1e-9, "tag " + tag.id);
                Assert.AreEqual(across[index % 4], tag.clusterOffsetInches.x, 1e-9, "tag " + tag.id);
                Assert.AreEqual(7.1874, tag.clusterOffsetInches.y, 1e-9, "tag " + tag.id);
                Assert.AreEqual(-5.622, tag.clusterOffsetInches.z, 1e-9, "tag " + tag.id);
            }
        }

        /// <summary>
        /// WHAT: Seen from above (audience at +x, blue at +y), the tags run left to right as in the
        /// manual's Figure 9-17: 33 32 31 30 and 45 44 43 42 on the far cells, 34 35 36 37 and 38 39 40 41
        /// on the audience cells; red cells on the red (-y) side, far cells at -x.
        /// </summary>
        [Test]
        public void BioBuzzTagsAreWhereTheManualShowsThem()
        {
            Dictionary<int, FieldTagData> tags = LoadField("biobuzz-2026").aprilTags.ToDictionary(t => t.id);
            int[][] leftToRight = { new[] { 33, 32, 31, 30 }, new[] { 45, 44, 43, 42 }, new[] { 34, 35, 36, 37 }, new[] { 38, 39, 40, 41 } };

            foreach (int[] row in leftToRight)
            {
                for (int i = 1; i < row.Length; i++)
                {
                    Assert.Greater(tags[row[i]].yInches.Value, tags[row[i - 1]].yInches.Value, row[i] + " is right of " + row[i - 1]);
                }
            }
            Assert.Less(tags[30].yInches.Value, 0, "red is on the -y side");
            Assert.Greater(tags[42].yInches.Value, 0, "blue is on the +y side");
            Assert.Less(tags[30].xInches.Value, 0, "scoring cells are on the far (-x) side");
            Assert.Greater(tags[34].xInches.Value, 0, "audience cells are on the +x side");
        }

        /// <summary>
        /// WHAT: Every tag with a field position faces along a unit vector, and BIOBUZZ's tags (under
        /// the cells) face down at the tiles.
        /// </summary>
        [Test]
        public void TagsFaceAlongUnitVectors()
        {
            foreach (string id in new[] { "biobuzz-2026", "decode-2025" })
            {
                foreach (FieldTagData tag in LoadField(id).aprilTags.Where(t => t.HasFieldPose))
                {
                    var facing = new Vector3((float)tag.facing.x, (float)tag.facing.y, (float)tag.facing.z);
                    Assert.AreEqual(1f, facing.magnitude, 0.01f, id + " tag " + tag.id);
                    if (id == "biobuzz-2026")
                    {
                        Assert.Less(tag.facing.z, -0.5, "tag " + tag.id + " faces down");
                    }
                }
            }
        }

        /// <summary>
        /// WHAT: DECODE's goal tags (fields/disabled/decode-2025.json, kept correct in case it is switched
        /// back on) are the SDK's (getDecodeTagLibrary): 20 BlueTarget and 24 RedTarget at
        /// (-58.3727, -/+55.6425, 29.5) in, 6.5 in; the obelisk tags 21-23 have no field position, so
        /// they aren't drawn.
        /// </summary>
        [Test]
        public void DecodeTagsMatchTheSdk()
        {
            Dictionary<int, FieldTagData> tags = LoadField("decode-2025").aprilTags.ToDictionary(t => t.id);

            Assert.AreEqual("BlueTarget", tags[20].name);
            Assert.AreEqual(-58.3727, tags[20].xInches.Value, 1e-9);
            Assert.AreEqual(-55.6425, tags[20].yInches.Value, 1e-9);
            Assert.AreEqual(29.5, tags[20].zInches.Value, 1e-9);
            Assert.AreEqual(55.6425, tags[24].yInches.Value, 1e-9);
            Assert.AreEqual(6.5, tags[24].sizeInches, 1e-9);
            foreach (int obelisk in new[] { 21, 22, 23 })
            {
                Assert.IsFalse(tags[obelisk].HasFieldPose, "tag " + obelisk);
            }
        }

        /// <summary>
        /// WHAT: Every part is on the field (alliance areas just outside it), above the tiles, has a
        /// real size and a readable color.
        /// </summary>
        [Test]
        public void PartsAreOnTheFieldWithRealSizesAndColors()
        {
            foreach (FieldElementData part in LoadField("biobuzz-2026").elements)
            {
                double limit = part.id.EndsWith("alliance-area") ? 72 + 54 : 72;
                Assert.LessOrEqual(Mathf.Abs((float)part.xInches), limit, part.id);
                Assert.LessOrEqual(Mathf.Abs((float)part.yInches), limit, part.id);
                Assert.Greater(part.zInches, 0, part.id);
                Assert.Greater(part.heightInches, 0, part.id);
                Assert.IsTrue(ColorUtility.TryParseHtmlString(part.color, out _), part.id + " color " + part.color);
            }
        }

        /// <summary>
        /// WHAT: A field part's tilt raises its +x end: a box with heading 0 and tilt 30 points its
        /// length along Unity +x (FTC +x) and 30 degrees up.
        /// WHY: Getting this sign wrong would show both HIVES tipped the wrong way.
        /// </summary>
        [Test]
        public void TiltRaisesThePlusXEnd()
        {
            Vector3 length = FtcToUnity.Rotation(0, 30) * Vector3.forward;

            Assert.AreEqual(0.866f, length.x, 0.001f);
            Assert.AreEqual(0.5f, length.y, 0.001f);
            Assert.AreEqual(0f, length.z, 0.001f);
            Assert.AreEqual(new Vector3(0.5f, -0.866f, 0f), FtcToUnity.Direction(0.5, 0, -0.866), "FTC z is Unity y");
        }

        /// <summary>
        /// WHAT: A CAD point in FTC inches (x toward the audience, y toward blue, z up), read by Unity's
        /// OBJ importer as (-x, y, z), lands where <see cref="FtcToUnity.Position"/> puts it.
        /// WHY: Getting this wrong would show the whole CAD field mirrored or on its side.
        /// </summary>
        [Test]
        public void CadModelsLandInTheFtcFrame()
        {
            var ftc = new Vector3(30f, -20f, 44f);                 // a point on the red side, 44 in up
            var asImported = new Vector3(-ftc.x, ftc.y, ftc.z);    // what Unity's OBJ importer makes of it
            Vector3 placed = FtcToUnity.FtcModelRotation * asImported * FtcToUnity.MetersPerInch;

            Vector3 expected = FtcToUnity.Position(ftc.x, ftc.y, FtcToUnity.Meters(ftc.z));
            Assert.AreEqual(expected.x, placed.x, 1e-4f);
            Assert.AreEqual(expected.y, placed.y, 1e-4f);
            Assert.AreEqual(expected.z, placed.z, 1e-4f);
        }

        /// <summary>
        /// WHAT: The official BIOBUZZ CAD (Resources/Field/biobuzz-2026) has all 12 parts the field is drawn from.
        /// </summary>
        [Test]
        public void BioBuzzCadPartsAreAllThere()
        {
            string[] parts = FieldBuilder.CadModels("biobuzz-2026").Select(p => p.name).ToArray();
            CollectionAssert.AreEquivalent(new[]
            {
                "tiles", "perimeter", "hive-frame", "red-hive", "blue-hive", "flower-far", "flower-blue-wall",
                "flower-red-wall", "flower-audience", "tape", "apriltags", "other",
            }, parts);
        }

        /// <summary>
        /// WHAT: In the drawn BIOBUZZ field the wall and HIVE panels are see-through, while their trim and
        /// the HIVE frame stay solid.
        /// WHY: The user asked to see through the walls and HIVEs (to the robot and the pieces inside).
        /// </summary>
        [Test]
        public void WallAndHivePanelsAreSeeThrough()
        {
            // The alliance-area boxes recolor their material, which Unity warns about outside Play mode only.
            LogAssert.ignoreFailingMessages = true;
            var parent = new GameObject("Test field").transform;
            try
            {
                FieldBuilder.BuildSeason(parent, LoadField("biobuzz-2026"));
                Renderer[] renderers = parent.GetComponentsInChildren<Renderer>();
                foreach (string panel in new[] { "perimeter-E6E6E6", "red-hive-E6E6E6", "blue-hive-E6E6E6" })
                {
                    Material material = renderers.Single(r => r.name == panel).sharedMaterial;
                    Assert.Less(material.color.a, 0.5f, panel + " lets most light through");
                    Assert.GreaterOrEqual(material.renderQueue, 3000, panel + " is drawn as transparent");
                }
                foreach (string solid in new[] { "red-hive-FF0000", "blue-hive-0000FF", "hive-frame-E6E6E6", "perimeter-B3B3B3" })
                {
                    Assert.Less(renderers.Single(r => r.name == solid).sharedMaterial.renderQueue, 3000, solid + " stays solid");
                }
            }
            finally
            {
                Object.DestroyImmediate(parent.gameObject);
            }
        }

        /// <summary>
        /// WHAT: The field shown is the recording's own (header "field"), else the one picked last
        /// time, else the plain field; ids without a file are ignored.
        /// </summary>
        [Test]
        public void PickPrefersTheRecordingThenTheLastChoice()
        {
            string[] available = { "biobuzz-2026", "decode-2025" };

            Assert.AreEqual("decode-2025", FieldLayouts.Pick("decode-2025", "biobuzz-2026", available));
            Assert.AreEqual("biobuzz-2026", FieldLayouts.Pick("missing-1999", "biobuzz-2026", available));
            Assert.AreEqual("biobuzz-2026", FieldLayouts.Pick(null, "biobuzz-2026", available));
            Assert.AreEqual(FieldLayouts.PlainFieldId, FieldLayouts.Pick(null, "", available));
            Assert.AreEqual("decode-2025", FieldLayouts.FieldFromCommandLine(new[] { "VI_Arena.exe", "-field", "decode-2025" }));
            Assert.IsNull(FieldLayouts.FieldFromCommandLine(new[] { "VI_Arena.exe", "-field" }));
        }

        /// <summary>
        /// WHAT: "-fields &lt;folder&gt;" on the command line wins over TestHarness/th_src/fields and over
        /// the app's own copy. (Finding th_src/fields itself: <see cref="TestHarnessFolderTests"/>.)
        /// </summary>
        [Test]
        public void FieldsFolderOnTheCommandLineWins()
        {
            string testHarness = Path.GetFullPath(Path.Combine(Path.GetTempPath(), "pvi-test", "TestHarness"));
            string other = Path.Combine(Path.GetTempPath(), "pvi-test", "my-fields");

            Assert.AreEqual(Path.GetFullPath(other), FieldLayouts.ResolveFolder(new[] { "VI_Arena.exe", "-fields", other }, testHarness, "x"));
            Assert.AreEqual(Path.GetFullPath(other), FieldLayouts.ResolveFolder(new[] { "VI_Arena.exe", "-fields", other }, null, "x"));
        }

        /// <summary>
        /// WHAT: A file that isn't a field file, or has an unknown shape, is refused with a message
        /// that says what is wrong.
        /// </summary>
        [Test]
        public void BadFieldFilesAreRefusedWithAReason()
        {
            string folder = Path.Combine(Path.GetTempPath(), "pvi-test", "bad-fields");
            Directory.CreateDirectory(folder);
            string notField = Path.Combine(folder, "recording.json");
            File.WriteAllText(notField, "{\"header\": {\"version\": 2}}");
            string badShape = Path.Combine(folder, "bad-shape.json");
            File.WriteAllText(badShape, "{\"format\": \"pvi-harness-field\", \"version\": 1, \"id\": \"x\", \"name\": \"X\","
                + " \"elements\": [{\"id\": \"cone\", \"shape\": \"cone\"}]}");

            StringAssert.Contains("not a field file", Assert.Throws<RecordingFormatException>(() => FieldLayouts.Load(notField)).Message);
            StringAssert.Contains("\"cone\"", Assert.Throws<RecordingFormatException>(() => FieldLayouts.Load(badShape)).Message);
            var problems = new List<string>();
            CollectionAssert.IsEmpty(FieldLayouts.LoadAll(folder, problems));
            Assert.AreEqual(2, problems.Count);
        }
    }
}

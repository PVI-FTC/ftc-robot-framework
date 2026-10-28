using System.Collections.Generic;
using System.IO;
using System.Linq;
using NUnit.Framework;
using UnityEngine;

namespace PviHarness.Replay.Tests
{
    /// <summary>
    /// The field file's "pieceObstacles" (field format version 2): the simple shapes the harness's
    /// ball physics uses for the HIVE frame and the FLOWERs line up with FIRST's official field CAD.
    /// </summary>
    public class PieceObstacleTests
    {
        // Rays run level at this height: between a POLLEN's center (1.4 in) and a NECTAR's (1.8 in).
        private const double BallHeightInches = 1.6;
        // How far the CAD may be from a shape's side.
        private const double ToleranceInches = 0.3;
        // Rays start this far outside a side.
        private const double StartOutsideInches = 2.5;

        private GameObject parent;
        private Transform cad;

        [SetUp]
        public void LoadCad()
        {
            parent = new GameObject("Test CAD");
            cad = new GameObject("CAD parts").transform;
            cad.SetParent(parent.transform, false);
            cad.localRotation = FtcToUnity.FtcModelRotation;
            cad.localScale = Vector3.one * FtcToUnity.MetersPerInch;
            foreach (string part in new[] { "hive-frame", "flower-far", "flower-blue-wall", "flower-red-wall", "flower-audience" })
            {
                GameObject source = Resources.Load<GameObject>(FieldBuilder.CadFolder("biobuzz-2026") + "/" + part);
                Assert.IsNotNull(source, "the CAD part " + part);
                GameObject copy = Object.Instantiate(source, cad, false);
                foreach (MeshFilter mesh in copy.GetComponentsInChildren<MeshFilter>())
                {
                    mesh.gameObject.AddComponent<MeshCollider>().sharedMesh = mesh.sharedMesh;
                }
            }
            Physics.SyncTransforms();
        }

        [TearDown]
        public void RemoveCad()
        {
            Object.DestroyImmediate(parent);
        }

        private static List<PieceObstacleData> Obstacles()
        {
            string folder = FieldLayouts.ResolveFolder(new string[0], Application.dataPath, true);
            return FieldLayouts.Load(Path.Combine(folder, "biobuzz-2026.json")).pieceObstacles;
        }

        /// <summary>
        /// WHAT: BIOBUZZ lists 6 HIVE frame boxes, 8 FLOWER wall brackets and 4 FLOWER heads; every side
        /// of a box that a ball can reach from the field is where the CAD is (within 0.3 in) at ball height.
        /// WHY: The Java ball physics bounces balls off these shapes, so they must match the real field.
        /// </summary>
        [Test]
        public void BoxSidesLineUpWithTheCad()
        {
            List<PieceObstacleData> obstacles = Obstacles();
            Assert.AreEqual(18, obstacles.Count);
            Assert.AreEqual(6, obstacles.Count(o => o.id.StartsWith("frame-") && o.shape == "box"));
            Assert.AreEqual(8, obstacles.Count(o => o.id.Contains("-bracket-") && o.shape == "box"));
            Assert.AreEqual(4, obstacles.Count(o => o.id.EndsWith("-head") && o.shape == "cylinder"));

            int checkedSides = 0;
            foreach (PieceObstacleData box in obstacles.Where(o => o.shape == "box"))
            {
                double midX = (box.minXInches + box.maxXInches) / 2;
                double midY = (box.minYInches + box.maxYInches) / 2;
                // The long thin lip: its two long sides, at the middle and part-way along.
                bool lip = box.id.EndsWith("-lip");
                foreach (double x in lip ? new[] { -10.0, 0.0, 10.0 } : new[] { midX })
                {
                    checkedSides += CheckSide(obstacles, box.id + " -y side", x, box.minYInches, 0, 1, box.minYInches);
                    checkedSides += CheckSide(obstacles, box.id + " +y side", x, box.maxYInches, 0, -1, box.maxYInches);
                }
                if (!lip)
                {
                    checkedSides += CheckSide(obstacles, box.id + " -x side", box.minXInches, midY, 1, 0, box.minXInches);
                    checkedSides += CheckSide(obstacles, box.id + " +x side", box.maxXInches, midY, -1, 0, box.maxXInches);
                }
            }
            // 2 lips x 3 places x 2 sides, 4 feet x 4 sides, 8 brackets x 2 sides (the field side and
            // the outer end; the wall side and the side facing the other bracket can't be reached).
            Assert.AreEqual(12 + 16 + 16, checkedSides, "sides checked");
        }

        /// <summary>WHAT: The frame's foot-bar lip is no taller than the file says: a level ray just above it hits nothing.</summary>
        [Test]
        public void LipIsAsTallAsItsTop()
        {
            foreach (PieceObstacleData lip in Obstacles().Where(o => o.id.EndsWith("-lip")))
            {
                double outside = lip.minYInches < 0 ? lip.minYInches - StartOutsideInches : lip.maxYInches + StartOutsideInches;
                double toward = lip.minYInches < 0 ? 1 : -1;
                Assert.IsNull(FirstHitInches(0, outside, lip.topInches + 0.3, 0, toward, 2 * StartOutsideInches),
                    lip.id + ": something in the CAD 0.3 in above its top " + lip.topInches);
                Assert.IsNotNull(FirstHitInches(0, outside, lip.topInches - 0.3, 0, toward, 2 * StartOutsideInches),
                    lip.id + ": nothing in the CAD 0.3 in below its top " + lip.topInches);
            }
        }

        /// <summary>
        /// WHAT: Each FLOWER head's cylinder matches the CAD just above its bottom (seen from the field,
        /// the CAD's widest point is the radius, within 0.3 in), and under the head, at ball height, a
        /// ray from the field reaches the FLOWER's middle without hitting anything.
        /// WHY: The heads start above the balls, so balls roll under them and only meet the brackets.
        /// </summary>
        [Test]
        public void BallsRollUnderTheFlowerHeads()
        {
            foreach (PieceObstacleData head in Obstacles().Where(o => o.id.EndsWith("-head")))
            {
                double reach = head.radiusInches + StartOutsideInches;
                var radii = new List<double>();
                for (int k = 0; k < 16; k++)
                {
                    double angle = k * Mathf.PI / 8;
                    double dx = System.Math.Cos(angle), dy = System.Math.Sin(angle);
                    double startX = head.xInches + dx * reach;
                    double startY = head.yInches + dy * reach;
                    if (System.Math.Abs(startX) > 70 || System.Math.Abs(startY) > 70)
                    {
                        continue; // behind the FLOWER, against the wall
                    }
                    double? side = FirstHitInches(startX, startY, head.bottomInches + 0.5, -dx, -dy, reach);
                    radii.Add(side.HasValue ? reach - side.Value : 0);
                    Assert.IsNull(FirstHitInches(startX, startY, BallHeightInches, -dx, -dy, reach),
                        head.id + ": something under the head at ball height, from angle " + k);
                }
                Assert.GreaterOrEqual(radii.Count, 7, head.id + ": directions from the field");
                Assert.AreEqual(head.radiusInches, radii.Max(), ToleranceInches, head.id + ": widest point of the CAD");
            }
        }

        /// <summary>
        /// WHAT: The O key's drawing has one shape per obstacle, each the size and place the file says
        /// (a lip box and a FLOWER head checked), and no colliders.
        /// </summary>
        [Test]
        public void ObstacleDrawingMatchesTheFile()
        {
            string folder = FieldLayouts.ResolveFolder(new string[0], Application.dataPath, true);
            FieldLayoutData field = FieldLayouts.Load(Path.Combine(folder, "biobuzz-2026.json"));

            Transform drawn = FieldBuilder.BuildObstacles(parent.transform, field);

            Assert.AreEqual(field.pieceObstacles.Count, drawn.childCount);
            Assert.IsEmpty(drawn.GetComponentsInChildren<Collider>());
            PieceObstacleData lip = field.pieceObstacles.First(o => o.id == "frame-red-lip");
            Bounds lipBounds = drawn.Find(lip.id).GetComponent<Renderer>().bounds;
            AssertNear(FtcToUnity.Position(lip.minXInches, lip.minYInches, FtcToUnity.Meters(lip.bottomInches)), lipBounds.min, "lip corner");
            AssertNear(FtcToUnity.Position(lip.maxXInches, lip.maxYInches, FtcToUnity.Meters(lip.topInches)), lipBounds.max, "lip corner");
            PieceObstacleData head = field.pieceObstacles.First(o => o.id == "flower-far-head");
            Bounds headBounds = drawn.Find(head.id).GetComponent<Renderer>().bounds;
            AssertNear(FtcToUnity.Position(head.xInches - head.radiusInches, head.yInches - head.radiusInches,
                FtcToUnity.Meters(head.bottomInches)), headBounds.min, "head bottom corner");
            AssertNear(FtcToUnity.Position(head.xInches + head.radiusInches, head.yInches + head.radiusInches,
                FtcToUnity.Meters(head.topInches)), headBounds.max, "head top corner");
        }

        private static void AssertNear(Vector3 expected, Vector3 actual, string what)
        {
            Assert.Less(Vector3.Distance(expected, actual), FtcToUnity.Meters(0.05), what + ": expected " + expected + ", was " + actual);
        }

        /// <summary>
        /// A level ray at ball height from <see cref="StartOutsideInches"/> outside a side, pointing in
        /// (dx, dy), first meets the CAD within <see cref="ToleranceInches"/> of where the side is.
        /// Returns 1 if checked, 0 if skipped: a side no ball can reach (the ray would start outside the
        /// field, or inside another shape, as between a FLOWER's two brackets) isn't checked.
        /// </summary>
        private int CheckSide(List<PieceObstacleData> all, string what, double x, double y, double dx, double dy, double sideInches)
        {
            double startX = x - dx * StartOutsideInches;
            double startY = y - dy * StartOutsideInches;
            bool insideOther = all.Any(o => o.shape == "box" && startX >= o.minXInches && startX <= o.maxXInches
                && startY >= o.minYInches && startY <= o.maxYInches);
            if (System.Math.Abs(startX) > 70.5 || System.Math.Abs(startY) > 70.5 || insideOther)
            {
                return 0;
            }
            double? hit = FirstHitInches(startX, startY, BallHeightInches, dx, dy, 2 * StartOutsideInches);
            Assert.IsNotNull(hit, what + ": no CAD surface found");
            double hitAt = dx != 0 ? startX + dx * hit.Value : startY + dy * hit.Value;
            Assert.AreEqual(sideInches, hitAt, ToleranceInches, what + " (CAD surface at " + hitAt.ToString("0.00") + " in)");
            return 1;
        }

        /// <summary>How far (inches) a level ray from (x, y, height) along (dx, dy) goes before it meets the CAD, or null.</summary>
        private double? FirstHitInches(double x, double y, double heightInches, double dx, double dy, double maxInches)
        {
            Vector3 start = FtcToUnity.Position(x, y, FtcToUnity.Meters(heightInches));
            Vector3 direction = FtcToUnity.Direction(dx, dy, 0).normalized;
            bool backfaces = Physics.queriesHitBackfaces;
            Physics.queriesHitBackfaces = true;
            RaycastHit[] hits = Physics.RaycastAll(start, direction, FtcToUnity.Meters(maxInches));
            Physics.queriesHitBackfaces = backfaces;
            var mine = hits.Where(h => h.collider.transform.IsChildOf(cad)).ToList();
            return mine.Count == 0 ? (double?)null : FtcToUnity.Inches(mine.Min(h => h.distance));
        }
    }
}

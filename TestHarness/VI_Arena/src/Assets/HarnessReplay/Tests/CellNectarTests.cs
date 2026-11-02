using System.Collections.Generic;
using System.IO;
using System.Linq;
using NUnit.Framework;
using UnityEngine;

namespace PviHarness.Replay.Tests
{
    /// <summary>
    /// The 6 NECTAR that start in the upward HIVE CELLs sit where the official field CAD and the game
    /// manual put them.
    /// </summary>
    public class CellNectarTests
    {
        // NECTAR is 3.6 in across.
        private const double RadiusInches = 1.8;
        // How far a ball may be from touching a CAD surface (the CAD ball is 3.62 in, the panels are thin).
        private const double ToleranceInches = 0.15;
        // Each HIVE starts tipped 30 degrees.
        private const double TiltDegrees = 30;

        private GameObject parent;

        [SetUp]
        public void MakeParent()
        {
            parent = new GameObject("Test HIVEs");
        }

        [TearDown]
        public void RemoveParent()
        {
            Object.DestroyImmediate(parent);
        }

        /// <summary>
        /// WHAT: In a harness recording, each CELL NECTAR rests on the CAD CELL's floor and against the
        /// side wall nearest its ALLIANCE AREA; the lowest one touches the back wall, and the other two
        /// touch the ball below them (manual 10.3.1: "contacting the back wall of the CELL and in a
        /// line against the side closest to the ALLIANCE AREA").
        /// WHY: The CELLs are tipped 30 degrees, so a level row of balls floats above or sinks into
        /// the floor.
        /// HOW: Uses Harness/build/recordings/viewer-teamA-autonomous.json, which the harness tests
        /// write (skipped if it isn't there yet), and rays against the CAD HIVE models.
        /// </summary>
        [Test]
        public void CellNectarRestOnTheCadCellFloorAgainstTheBackAndSideWalls()
        {
            string path = Path.Combine(RecordingLoader.DefaultFolder, "viewer-teamA-autonomous.json");
            if (!File.Exists(path))
            {
                Assert.Ignore("Run the harness tests first (.\\gradlew.bat test) to create " + path);
            }
            List<PieceData> cell = RecordingLoader.Load(path).header.pieces.Where(p => p.state == "cell").ToList();
            Assert.AreEqual(6, cell.Count, "3 NECTAR in each upward CELL");
            Transform hives = LoadHives();

            foreach (string color in new[] { "red", "blue" })
            {
                // Lowest first: the one nearest the pivot (x = 0) is against the back wall.
                List<PieceData> balls = cell.Where(p => p.kind == color + "-nectar")
                    .OrderBy(p => System.Math.Abs(p.xInches)).ToList();
                Assert.AreEqual(3, balls.Count, color);
                // The red up CELL is on the audience (+x) side, the blue one on the far (-x) side.
                double outward = color == "red" ? 1 : -1;
                double tilt = TiltDegrees * Mathf.Deg2Rad;
                // Along the CELL away from the pivot, and the CELL floor's "up", in FTC (x, y, z).
                Vector3 alongCell = FtcToUnity.Direction(outward * System.Math.Cos(tilt), 0, System.Math.Sin(tilt));
                Vector3 floorUp = FtcToUnity.Direction(-outward * System.Math.Sin(tilt), 0, System.Math.Cos(tilt));
                Vector3 towardAlliance = FtcToUnity.Direction(0, color == "red" ? -1 : 1, 0);

                for (int i = 0; i < balls.Count; i++)
                {
                    PieceData ball = balls[i];
                    Vector3 center = FtcToUnity.Position(ball.xInches, ball.yInches, FtcToUnity.Meters(ball.zInches));
                    string name = ball.id;
                    AssertSurfaceAtRadius(hives, center, -floorUp, name + ": on the CELL floor");
                    AssertSurfaceAtRadius(hives, center, towardAlliance, name + ": against the side wall nearest its alliance");
                    if (i == 0)
                    {
                        AssertSurfaceAtRadius(hives, center, -alongCell, name + ": against the CELL's back wall");
                    }
                    else
                    {
                        PieceData below = balls[i - 1];
                        Vector3 belowCenter = FtcToUnity.Position(below.xInches, below.yInches, FtcToUnity.Meters(below.zInches));
                        Assert.AreEqual(2 * RadiusInches, FtcToUnity.Inches(Vector3.Distance(center, belowCenter)),
                            ToleranceInches, name + ": touching the ball below it");
                    }
                }
            }
        }

        /// <summary>The CAD HIVEs placed the way FieldBuilder.BuildSeason places them, with mesh colliders for the rays.</summary>
        private Transform LoadHives()
        {
            var model = new GameObject("CAD HIVEs").transform;
            model.SetParent(parent.transform, false);
            model.localRotation = FtcToUnity.FtcModelRotation;
            model.localScale = Vector3.one * FtcToUnity.MetersPerInch;
            foreach (string part in new[] { "red-hive", "blue-hive" })
            {
                GameObject source = Resources.Load<GameObject>(FieldBuilder.CadFolder("biobuzz-2026") + "/" + part);
                Assert.IsNotNull(source, "the CAD part " + part);
                GameObject copy = Object.Instantiate(source, model, false);
                foreach (MeshFilter mesh in copy.GetComponentsInChildren<MeshFilter>())
                {
                    mesh.gameObject.AddComponent<MeshCollider>().sharedMesh = mesh.sharedMesh;
                }
            }
            Physics.SyncTransforms();
            return model;
        }

        /// <summary>The nearest HIVE surface from the ball's center in that direction is one ball radius away.</summary>
        private static void AssertSurfaceAtRadius(Transform hives, Vector3 center, Vector3 direction, string what)
        {
            bool backfaces = Physics.queriesHitBackfaces;
            Physics.queriesHitBackfaces = true;
            RaycastHit[] hits = Physics.RaycastAll(center, direction.normalized, FtcToUnity.Meters(12));
            Physics.queriesHitBackfaces = backfaces;
            var mine = hits.Where(h => h.collider.transform.IsChildOf(hives)).ToList();
            Assert.IsNotEmpty(mine, what + " (no CAD surface found)");
            double nearestInches = FtcToUnity.Inches(mine.Min(h => h.distance));
            Assert.AreEqual(RadiusInches, nearestInches, ToleranceInches, what);
        }
    }
}

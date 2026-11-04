using System.Collections.Generic;
using NUnit.Framework;
using UnityEngine;

namespace VI_Arena.Tests
{
    /// <summary>
    /// The field's AprilTags drawn with their real 36h11 patterns (<see cref="AprilTag36h11"/>,
    /// <see cref="FieldBuilder.AddTag"/>).
    /// </summary>
    public class AprilTagPatternTests
    {
        /// <summary>
        /// Tag 0's black square, row by row from the top ('#' black, '.' white), worked out separately
        /// from the older AprilTag 2 code for tag 0 (0xd5d628584, bits laid out row by row).
        /// </summary>
        private static readonly string[] TagZero =
        {
            "########",
            "#..#.#.#",
            "##...#.#",
            "##..####",
            "#.#.####",
            "##.#..##",
            "####.###",
            "########",
        };

        /// <summary>WHAT: The family has 587 codes, starting with the AprilTag 3 library's own first ones.</summary>
        [Test]
        public void TheCodesAreTheLibrarysOwn()
        {
            Assert.AreEqual(587, AprilTag36h11.Codes.Length);
            Assert.AreEqual(0xD7E00984BL, AprilTag36h11.Codes[0]);
            Assert.AreEqual(0xDDA664CA7L, AprilTag36h11.Codes[1]);
            Assert.AreEqual(587, new HashSet<long>(AprilTag36h11.Codes).Count, "all different");
        }

        /// <summary>
        /// WHAT: Tag 0 is drawn exactly like the published tag 0: a white edge, then this black square.
        /// WHY: A wrong bit order or a mirror would still look like a tag but be a different id (or none).
        /// </summary>
        [Test]
        public void TagZeroLooksLikeThePublishedTag()
        {
            bool[,] white = AprilTag36h11.Pattern(0);
            for (int i = 0; i < AprilTag36h11.Cells; i++)
            {
                Assert.IsTrue(white[0, i] && white[9, i] && white[i, 0] && white[i, 9], "white edge at " + i);
            }
            for (int row = 0; row < 8; row++)
            {
                for (int column = 0; column < 8; column++)
                {
                    Assert.AreEqual(TagZero[row][column] == '.', white[row + 1, column + 1], "row " + row + ", column " + column);
                }
            }
        }

        /// <summary>
        /// WHAT: Reading a drawn BIOBUZZ tag's data cells back gives its own code, and no other id has it.
        /// </summary>
        [Test]
        public void EachBioBuzzTagReadsBackAsItsOwnId()
        {
            for (int id = 30; id <= 45; id++)
            {
                bool[,] white = AprilTag36h11.Pattern(id);
                long read = 0;
                foreach (Vector2Int cell in AprilTag36h11.BitCells)
                {
                    read = (read << 1) | (white[cell.y + 1, cell.x + 1] ? 1L : 0L);
                }
                Assert.AreEqual(id, System.Array.IndexOf(AprilTag36h11.Codes, read), "tag " + id);
            }
        }

        /// <summary>
        /// WHAT: Every BIOBUZZ tag in the field file is drawn at its place, its picture facing the way the
        /// file says with its top toward the sky, 10/8 of its size across (the white edge around the
        /// black square), with its own id's pattern.
        /// </summary>
        [Test]
        public void EveryBioBuzzTagIsPlacedFacingOutWithItsOwnPattern()
        {
            var problems = new List<string>();
            FieldLayoutData field = FieldLayouts.LoadAll(FieldLayouts.DefaultFolder, problems).Find(f => f.id == "biobuzz-2026");
            Assert.IsNotNull(field, string.Join("; ", problems));
            var parent = new GameObject("test tags");
            try
            {
                int drawn = 0;
                foreach (FieldTagData tag in field.aprilTags)
                {
                    if (!tag.HasFieldPose)
                    {
                        continue;
                    }
                    Transform holder = FieldBuilder.AddTag(parent.transform, tag, onCad: false);
                    Transform pattern = holder.Find("Pattern");
                    Vector3 facing = FtcToUnity.Direction(tag.facing.x, tag.facing.y, tag.facing.z).normalized;

                    Vector3 at = FtcToUnity.Position(tag.xInches.Value, tag.yInches.Value, FtcToUnity.Meters(tag.zInches.Value));
                    Assert.Less(Vector3.Distance(at, pattern.position), 1e-4f, "tag " + tag.id + " place");
                    // A quad's visible side faces its -z.
                    Assert.Greater(Vector3.Dot(-pattern.forward, facing), 0.9999f, "tag " + tag.id + " faces out");
                    Assert.Greater(pattern.up.y, 0f, "tag " + tag.id + " top toward the sky");
                    Assert.AreEqual(FtcToUnity.Meters(tag.sizeInches) * 10f / 8f, pattern.lossyScale.x, 1e-5f, "tag " + tag.id + " size");
                    Assert.AreEqual("AprilTag 36h11 id " + tag.id, pattern.GetComponent<Renderer>().sharedMaterial.mainTexture.name);
                    drawn++;
                }
                Assert.AreEqual(16, drawn, "all 16 BIOBUZZ tags");
            }
            finally
            {
                Object.DestroyImmediate(parent);
            }
        }
    }
}

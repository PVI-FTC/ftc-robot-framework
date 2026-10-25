using System.Linq;
using NUnit.Framework;
using UnityEngine;

namespace PviHarness.Replay.Tests
{
    /// <summary>The Mixamo people for the crowds and drive teams (Resources/People).</summary>
    public class PeopleTests
    {
        private GameObject parent;

        [SetUp]
        public void MakeParent()
        {
            parent = new GameObject("Test people");
        }

        [TearDown]
        public void RemoveParent()
        {
            Object.DestroyImmediate(parent);
        }

        /// <summary>WHAT: The people files are there: at least four characters and the sitting and standing animations.</summary>
        [Test]
        public void PeopleAreInTheProject()
        {
            Assert.IsTrue(People.Available);
            Assert.GreaterOrEqual(People.CharacterCount, 4);
            foreach (string clip in new[] { "sit", "sit_clap", "stand_idle" })
            {
                AnimationClip found = Resources.Load<AnimationClip>("People/animations/" + clip);
                Assert.IsNotNull(found, clip);
                Assert.IsTrue(found.isHumanMotion, clip + " plays on any character");
            }
        }

        /// <summary>
        /// WHAT: No two fans next to each other look the same (character, pose or any clothing color differs).
        /// WHY: The user asked for people who look unique, not a copied crowd.
        /// </summary>
        [Test]
        public void NeighbouringFansLookDifferent()
        {
            var random = new System.Random(7);
            string[] looks = Enumerable.Range(0, 30)
                .Select(i => People.PlaceFan(parent.transform, random, new Vector3(i * 0.6f, 1f, 5f), 90f))
                .Select(Look).ToArray();

            for (int i = 1; i < looks.Length; i++)
            {
                Assert.AreNotEqual(looks[i - 1], looks[i], "fans " + (i - 1) + " and " + i);
            }
            Assert.Greater(looks.Distinct().Count(), 25, "almost every fan is different");
        }

        /// <summary>WHAT: A fan sits on the seat (feet the seat height lower, hips at the seat) and has no colliders.</summary>
        [Test]
        public void FansSitOnTheirSeats()
        {
            GameObject fan = People.PlaceFan(parent.transform, new System.Random(3), new Vector3(0f, 2f, 6f), 90f);
            Bounds b = fan.GetComponentsInChildren<Renderer>()[0].bounds;
            foreach (Renderer r in fan.GetComponentsInChildren<Renderer>())
            {
                b.Encapsulate(r.bounds);
            }

            Assert.AreEqual(2f - People.SeatHeightMeters, b.min.y, 0.12f, "feet rest on the step below the seat");
            Assert.Less(b.max.y, 2f + 1.0f, "a seated person is under a meter above the seat");
            Assert.IsEmpty(fan.GetComponentsInChildren<Collider>());
        }

        /// <summary>WHAT: A drive-team member stands at the spot, wears the shirt color asked for, and is animated.</summary>
        [Test]
        public void DriversStandAnimatedInTheirShirtColor()
        {
            Color red = new Color(0.85f, 0.2f, 0.17f);
            GameObject driver = People.PlaceStanding(parent.transform, new System.Random(5), new Vector3(1f, 0f, -3f), 0f, red);

            Assert.IsTrue(driver.GetComponent<PersonAnimation>().Playing);
            Assert.AreEqual(new Vector3(1f, 0f, -3f), driver.transform.localPosition);
            bool redShirt = driver.GetComponentsInChildren<SkinnedMeshRenderer>()
                .Where(r => r.name.ToLowerInvariant().Contains("shirt") || r.name.ToLowerInvariant().Contains("hoodie") || r.name.ToLowerInvariant().Contains("coat"))
                .All(r => r.sharedMaterials.All(m => m.color == red));
            Assert.IsTrue(redShirt, "alliance-colored shirt");
        }

        private static string Look(GameObject fan)
        {
            return fan.name + "|" + string.Join(",", fan.GetComponentsInChildren<MeshRenderer>()
                .Select(r => r.GetComponent<MeshFilter>().sharedMesh.name + ":" + string.Join("/", r.sharedMaterials.Select(m => m.name))));
        }
    }
}

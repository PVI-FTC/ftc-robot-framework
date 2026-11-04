using NUnit.Framework;
using UnityEngine;

namespace VI_Arena.Tests
{
    /// <summary>
    /// Telling a key press from a held key's repeats (<see cref="HeldKeys"/>). Unity can't send a real key
    /// to ReplayController in a test, so this checks the logic; the key itself was checked in the built app.
    /// </summary>
    public class HeldKeysTests
    {
        /// <summary>
        /// WHAT: The first key-down is a press; more key-downs before the key-up are repeats.
        /// WHY: A press a little over half a second flipped the A key's switch twice, so nothing seemed to happen.
        /// </summary>
        [Test]
        public void AHeldKeyIsOnePress()
        {
            var keys = new HeldKeys();

            Assert.IsFalse(keys.IsRepeat(KeyCode.A), "the press");
            Assert.IsTrue(keys.IsRepeat(KeyCode.A), "first repeat");
            Assert.IsTrue(keys.IsRepeat(KeyCode.A), "second repeat");
        }

        /// <summary>WHAT: After the key-up, the next key-down is a new press.</summary>
        [Test]
        public void ReleasingAKeyMakesTheNextPressNew()
        {
            var keys = new HeldKeys();
            keys.IsRepeat(KeyCode.A);

            keys.Released(KeyCode.A);

            Assert.IsFalse(keys.IsRepeat(KeyCode.A));
        }

        /// <summary>WHAT: Holding one key doesn't make another key a repeat, and releasing one doesn't release the other.</summary>
        [Test]
        public void KeysAreTrackedSeparately()
        {
            var keys = new HeldKeys();
            keys.IsRepeat(KeyCode.A);

            Assert.IsFalse(keys.IsRepeat(KeyCode.C), "C pressed while A is held");
            keys.Released(KeyCode.C);
            Assert.IsTrue(keys.IsRepeat(KeyCode.A), "A is still held");
        }

        /// <summary>
        /// WHAT: Clearing forgets held keys.
        /// WHY: With Alt-Tab during a press the window never hears the key-up, and the next press would be ignored.
        /// </summary>
        [Test]
        public void ClearingForgetsHeldKeys()
        {
            var keys = new HeldKeys();
            keys.IsRepeat(KeyCode.A);

            keys.Clear();

            Assert.IsFalse(keys.IsRepeat(KeyCode.A));
        }

        /// <summary>WHAT: Stepping and speed keys keep acting while held; the switches, including A, don't.</summary>
        [Test]
        public void OnlySteppingAndSpeedKeysActWhileHeld()
        {
            foreach (KeyCode key in new[] { KeyCode.LeftArrow, KeyCode.RightArrow, KeyCode.LeftBracket, KeyCode.RightBracket, KeyCode.Minus, KeyCode.Equals })
            {
                Assert.IsTrue(HeldKeys.ActsWhileHeld(key), key.ToString());
            }
            foreach (KeyCode key in new[] { KeyCode.A, KeyCode.C, KeyCode.O, KeyCode.V, KeyCode.G, KeyCode.U, KeyCode.L, KeyCode.N, KeyCode.P, KeyCode.Space, KeyCode.Return, KeyCode.Escape, KeyCode.Home, KeyCode.End })
            {
                Assert.IsFalse(HeldKeys.ActsWhileHeld(key), key.ToString());
            }
        }
    }
}

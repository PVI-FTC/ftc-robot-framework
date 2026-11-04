using System.Collections.Generic;
using UnityEngine;

namespace VI_Arena
{
    /// <summary>
    /// Tells a real key press from Windows repeating a held key. Unity's GUI events look the same for
    /// both, so without this a switch key flips again with every repeat: a press a little over the
    /// keyboard's repeat delay (about half a second) flips it twice and looks like nothing happened.
    /// </summary>
    public sealed class HeldKeys
    {
        private readonly HashSet<KeyCode> held = new HashSet<KeyCode>();

        /// <summary>Call on each key-down event. True if the key was already down, so this is only a repeat.</summary>
        public bool IsRepeat(KeyCode key)
        {
            return !held.Add(key);
        }

        /// <summary>Call on each key-up event.</summary>
        public void Released(KeyCode key)
        {
            held.Remove(key);
        }

        /// <summary>Forgets every key: when the window loses focus it never hears the key-up.</summary>
        public void Clear()
        {
            held.Clear();
        }

        /// <summary>True for keys that should keep acting while held: stepping along the timeline and changing the speed. All others are switches.</summary>
        public static bool ActsWhileHeld(KeyCode key)
        {
            switch (key)
            {
                case KeyCode.LeftArrow:
                case KeyCode.RightArrow:
                case KeyCode.LeftBracket:
                case KeyCode.RightBracket:
                case KeyCode.Minus:
                case KeyCode.Equals:
                    return true;
                default:
                    return false;
            }
        }
    }
}

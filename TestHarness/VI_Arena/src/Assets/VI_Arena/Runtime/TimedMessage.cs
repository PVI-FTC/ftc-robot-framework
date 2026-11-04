namespace VI_Arena
{
    /// <summary>
    /// A short message that goes away by itself, like "AprilTag sight lines: Off" after pressing A. The
    /// time is passed in (seconds, real time), so it also runs out while a replay is paused.
    /// </summary>
    public sealed class TimedMessage
    {
        /// <summary>How long a message stays, in seconds.</summary>
        public const float ShownSeconds = 2f;

        private string text;
        private float untilSeconds;

        /// <summary>Shows <paramref name="message"/> from <paramref name="nowSeconds"/>, replacing any message before it.</summary>
        public void Show(string message, float nowSeconds)
        {
            text = message;
            untilSeconds = nowSeconds + ShownSeconds;
        }

        /// <summary>The message to show at <paramref name="nowSeconds"/>, or null when there is none or its time is up.</summary>
        public string TextAt(float nowSeconds)
        {
            return nowSeconds < untilSeconds ? text : null;
        }
    }
}

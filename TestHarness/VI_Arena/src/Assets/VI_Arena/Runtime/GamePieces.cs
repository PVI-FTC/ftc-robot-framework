using System.Collections.Generic;
using UnityEngine;

namespace VI_Arena
{
    /// <summary>
    /// Where every game piece (POLLEN, NECTAR) is at each moment of a run. A recording lists every piece
    /// once at INIT (<see cref="HeaderData.pieces"/>) and then, per loop, only the pieces that changed;
    /// this adds the changes up, so any loop can be shown, forwards or backwards.
    ///
    /// <para>The harness works out how the pieces move (the robot pushes them), so a replay always shows
    /// the same pushes. A live run's recording keeps growing; <see cref="At"/> catches up as it does.</para>
    /// </summary>
    public sealed class GamePieceTrack
    {
        private readonly RunRecordingData recording;
        private readonly Dictionary<string, int> indexById = new Dictionary<string, int>();
        // One array per loop: every piece's state after that loop. Unchanged pieces share the same
        // PieceData object, so this stays small.
        private readonly List<PieceData[]> statesByLoop = new List<PieceData[]>();
        private readonly PieceData[] start;

        /// <summary>Reads the pieces of <paramref name="recording"/> (none for recordings older than version 4).</summary>
        public GamePieceTrack(RunRecordingData recording)
        {
            this.recording = recording;
            List<PieceData> pieces = recording.header.pieces ?? new List<PieceData>();
            start = pieces.ToArray();
            for (int i = 0; i < start.Length; i++)
            {
                indexById[start[i].id] = i;
            }
        }

        /// <summary>Every piece at INIT, in the recording's order.</summary>
        public IReadOnlyList<PieceData> Start => start;

        /// <summary>
        /// Every piece's state right after loop <paramref name="loopIndex"/>. Changes for a piece the
        /// header didn't list are ignored.
        /// </summary>
        public PieceData[] At(int loopIndex)
        {
            int last = Mathf.Min(loopIndex, recording.loops.Count - 1);
            while (statesByLoop.Count <= last)
            {
                PieceData[] before = statesByLoop.Count == 0 ? start : statesByLoop[statesByLoop.Count - 1];
                List<PieceData> changes = recording.loops[statesByLoop.Count].pieces;
                PieceData[] after = before;
                if (changes != null && changes.Count > 0)
                {
                    after = (PieceData[])before.Clone();
                    foreach (PieceData change in changes)
                    {
                        if (change != null && change.id != null && indexById.TryGetValue(change.id, out int i))
                        {
                            after[i] = change;
                        }
                    }
                }
                statesByLoop.Add(after);
            }
            return last < 0 ? start : statesByLoop[last];
        }
    }

    /// <summary>
    /// Draws the game pieces: the CAD balls from Resources/GamePieces/biobuzz-2026 (pollen, red-nectar,
    /// blue-nectar), or plain colored spheres without them. Balls that move roll as they go.
    /// </summary>
    public sealed class GamePieceView
    {
        /// <summary>Where the piece models live in Resources (one per kind, named like the kind).</summary>
        public const string ModelFolder = "GamePieces/biobuzz-2026/";

        private readonly Transform root;
        private readonly Dictionary<string, Transform> balls = new Dictionary<string, Transform>();
        private readonly Dictionary<string, Vector3> lastPosition = new Dictionary<string, Vector3>();

        /// <summary>Makes one ball per piece under a new "Game pieces" object in <paramref name="parent"/>.</summary>
        public GamePieceView(Transform parent, IReadOnlyList<PieceData> pieces)
        {
            root = new GameObject("Game pieces").transform;
            root.SetParent(parent, false);
            foreach (PieceData piece in pieces)
            {
                if (piece == null || piece.id == null || balls.ContainsKey(piece.id))
                {
                    continue;
                }
                Transform ball = MakeBall(piece.kind);
                ball.name = piece.id;
                ball.SetParent(root, false);
                balls[piece.id] = ball;
            }
        }

        /// <summary>The object holding every ball.</summary>
        public Transform Root => root;

        /// <summary>The ball drawn for a piece id, or null.</summary>
        public Transform Ball(string id) => balls.TryGetValue(id, out Transform ball) ? ball : null;

        /// <summary>Puts every ball where <paramref name="states"/> says, rolling the ones that moved.</summary>
        public void Show(PieceData[] states)
        {
            foreach (PieceData piece in states)
            {
                if (piece == null || piece.id == null || !balls.TryGetValue(piece.id, out Transform ball))
                {
                    continue;
                }
                Vector3 position = FtcToUnity.Position(piece.xInches, piece.yInches, FtcToUnity.Meters(piece.zInches));
                if (lastPosition.TryGetValue(piece.id, out Vector3 before))
                {
                    // Rolling without slipping: a ball turns (distance / radius) radians about the axis
                    // across its path.
                    Vector3 moved = position - before;
                    moved.y = 0f;
                    float radius = FtcToUnity.Meters(DiameterInches(piece.kind) / 2.0);
                    if (moved.sqrMagnitude > 1e-10f)
                    {
                        float degrees = moved.magnitude / radius * Mathf.Rad2Deg;
                        ball.rotation = Quaternion.AngleAxis(degrees, Vector3.Cross(Vector3.up, moved.normalized)) * ball.rotation;
                    }
                }
                ball.localPosition = position;
                lastPosition[piece.id] = position;
            }
        }

        /// <summary>Removes every ball.</summary>
        public void Destroy()
        {
            if (Application.isPlaying)
            {
                Object.Destroy(root.gameObject);
            }
            else
            {
                Object.DestroyImmediate(root.gameObject);
            }
        }

        /// <summary>A ball's size from the game manual: POLLEN 2.8 in, NECTAR 3.6 in.</summary>
        public static double DiameterInches(string kind) => kind == "pollen" ? 2.8 : 3.6;

        /// <summary>A ball's color when there is no model: POLLEN yellow, NECTAR red or blue.</summary>
        public static Color ColorOf(string kind)
        {
            switch (kind)
            {
                case "pollen": return new Color(1f, 0.94f, 0.25f);
                case "red-nectar": return new Color(0.9f, 0.1f, 0.1f);
                case "blue-nectar": return new Color(0.15f, 0.3f, 0.95f);
                default: return Color.gray;
            }
        }

        private static Transform MakeBall(string kind)
        {
            GameObject model = kind == null ? null : Resources.Load<GameObject>(ModelFolder + kind);
            GameObject ball;
            if (model != null)
            {
                // The models are in inches, centered on the ball.
                ball = Object.Instantiate(model);
                ball.transform.localScale = Vector3.one * FtcToUnity.MetersPerInch;
            }
            else
            {
                ball = GameObject.CreatePrimitive(PrimitiveType.Sphere);
                if (Application.isPlaying)
                {
                    Object.Destroy(ball.GetComponent<Collider>());
                }
                else
                {
                    Object.DestroyImmediate(ball.GetComponent<Collider>());
                }
                ball.transform.localScale = Vector3.one * FtcToUnity.Meters(DiameterInches(kind));
                var material = new Material(Shader.Find("Standard")) { color = ColorOf(kind) };
                material.SetFloat("_Glossiness", 0.2f);
                ball.GetComponent<Renderer>().sharedMaterial = material;
            }
            return ball.transform;
        }
    }
}

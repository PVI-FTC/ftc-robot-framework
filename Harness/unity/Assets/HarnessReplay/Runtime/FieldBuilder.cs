using UnityEngine;

namespace PviHarness.Replay
{
    /// <summary>
    /// Builds the replay scene from code when Play starts: light, the 12 x 12 ft field with its
    /// 2 ft tiles, low walls, axis labels, the robot (body, outline, front edge, heading arrow,
    /// four wheels and the intake) and its trail. Nothing is hand-placed in the scene file, so
    /// every size and color is right here to read and change.
    ///
    /// <para>Colors match the Swing viewer's dark "mission control" field (FieldPainter): a dark
    /// field with faint grid lines, a cyan trail, a dark robot with a light outline, a cyan front
    /// edge and a white heading arrow. Wheels light cyan when driving forward and amber in
    /// reverse. All positions come from <see cref="FtcToUnity"/>.</para>
    /// </summary>
    public static class FieldBuilder
    {
        /// <summary>FTC field tiles are 2 ft (24 in) square: 6 x 6 tiles.</summary>
        public const float TileInches = 24f;
        /// <summary>Perimeter wall height. Kept low so the top-down view looks like the Swing field.</summary>
        public const float WallHeightMeters = 0.10f;
        /// <summary>Robot body height. Not recorded anywhere, so just a guess for drawing.</summary>
        public const float RobotHeightMeters = 0.10f;
        /// <summary>How high above the floor the robot body's bottom sits.</summary>
        public const float RobotClearanceMeters = 0.02f;
        /// <summary>Wheel size seen from above, as in the Swing viewer (4 x 1.6 in).</summary>
        public const float WheelLengthInches = 4f;
        public const float WheelWidthInches = 1.6f;

        // Colors, the same values as the Swing FieldPainter.
        public static readonly Color FieldColor = Hex(0x0A0D11);
        public static readonly Color FootLineColor = Hex(0x12171D);
        public static readonly Color TileLineColor = Hex(0x1D242C);
        public static readonly Color BorderColor = Hex(0x3A444F);
        public static readonly Color RobotFillColor = Hex(0x0E1F2E);
        public static readonly Color RobotOutlineColor = Hex(0xA9BCCC);
        public static readonly Color WheelColor = Hex(0x1A2A38);

        /// <summary>The objects the controller moves or recolors during replay.</summary>
        public sealed class Built
        {
            public Transform robot;
            public LineRenderer trail;
            /// <summary>Front-left, front-right, rear-left, rear-right.</summary>
            public Renderer[] wheels;
            public Renderer intake;
        }

        /// <summary>Creates everything under one parent object and returns the parts that change.</summary>
        public static Built Build(Transform parent, float robotLengthInches, float robotWidthInches)
        {
            AddLight(parent);
            AddFloor(parent);
            AddWalls(parent);
            AddAxisLabels(parent);
            var built = new Built { trail = AddTrail(parent) };
            AddRobot(parent, built, robotLengthInches, robotWidthInches);
            return built;
        }

        /// <summary>Remakes the robot at a new size (for example when another recording is opened).</summary>
        public static void RebuildRobot(Transform parent, Built built, float lengthInches, float widthInches)
        {
            if (built.robot != null)
            {
                Object.Destroy(built.robot.gameObject);
            }
            AddRobot(parent, built, lengthInches, widthInches);
        }

        /// <summary>
        /// Colors the wheels and intake for one loop's motor powers: cyan forward, amber reverse,
        /// brighter the harder they push; dark when stopped. Like the Swing robot drawing.
        /// </summary>
        public static void ShowPowers(Built built, LoopData loop)
        {
            string[] wheelNames = { "frontLeft", "frontRight", "rearLeft", "rearRight" };
            for (int i = 0; i < built.wheels.Length; i++)
            {
                built.wheels[i].material.color = PowerColor(MotorPower(loop, wheelNames[i]), WheelColor);
            }
            if (built.intake != null)
            {
                built.intake.material.color = PowerColor(MotorPower(loop, "intake"), WheelColor);
            }
        }

        /// <summary>Dark when stopped, then toward cyan (forward) or amber (reverse) as the power grows.</summary>
        public static Color PowerColor(double power, Color stopped)
        {
            float amount = Mathf.Clamp01(Mathf.Abs((float)power));
            if (amount < 0.005f)
            {
                return stopped;
            }
            return Color.Lerp(stopped, power > 0 ? HudTheme.Accent : HudTheme.Amber, 0.35f + 0.65f * amount);
        }

        private static double MotorPower(LoopData loop, string name)
        {
            foreach (MotorData motor in loop.motors)
            {
                if (motor.name == name)
                {
                    return motor.power;
                }
            }
            return 0;
        }

        // ---- The parts -----------------------------------------------------------------------------

        private static void AddLight(Transform parent)
        {
            var light = new GameObject("Sun").AddComponent<Light>();
            light.type = LightType.Directional;
            light.intensity = 0.9f;
            light.shadows = LightShadows.Soft;
            light.shadowStrength = 0.5f;
            light.transform.SetParent(parent, false);
            light.transform.rotation = Quaternion.Euler(60f, -30f, 0f);
            // Plain, even fill light. Unity's default lights the scene from a (blue) sky, which
            // tints the colors even though no sky is shown.
            RenderSettings.ambientMode = UnityEngine.Rendering.AmbientMode.Flat;
            RenderSettings.ambientLight = new Color(0.55f, 0.57f, 0.60f);
            RenderSettings.skybox = null;
        }

        private static void AddFloor(Transform parent)
        {
            // The field is unlit, so its colors are exactly the Swing field's in every view.
            GameObject field = Box(parent, "Field", new Vector3(0f, -0.01f, 0f),
                new Vector3(FtcToUnity.FieldSizeMeters, 0.02f, FtcToUnity.FieldSizeMeters), Color.white, unlit: true);
            Renderer renderer = field.GetComponent<Renderer>();
            renderer.material.mainTexture = TileTexture();
            renderer.shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.Off;
        }

        /// <summary>The field's pattern: faint lines every foot, stronger ones every 2 ft tile.</summary>
        private static Texture2D TileTexture()
        {
            const int size = 1152;               // 8 pixels per inch
            const int pixelsPerFoot = size / 12;
            var texture = new Texture2D(size, size, TextureFormat.RGB24, true) { name = "Field tiles" };
            var pixels = new Color32[size * size];
            for (int y = 0; y < size; y++)
            {
                for (int x = 0; x < size; x++)
                {
                    Color color = FieldColor;
                    bool tileLine = x % (2 * pixelsPerFoot) < 3 || y % (2 * pixelsPerFoot) < 3;
                    bool footLine = x % pixelsPerFoot < 2 || y % pixelsPerFoot < 2;
                    if (tileLine)
                    {
                        color = TileLineColor;
                    }
                    else if (footLine)
                    {
                        color = FootLineColor;
                    }
                    pixels[y * size + x] = color;
                }
            }
            texture.SetPixels32(pixels);
            texture.wrapMode = TextureWrapMode.Clamp;
            texture.anisoLevel = 8;
            texture.Apply();
            return texture;
        }

        private static void AddWalls(Transform parent)
        {
            float half = FtcToUnity.FieldSizeMeters / 2f;
            const float thickness = 0.03f;
            float length = FtcToUnity.FieldSizeMeters + 2f * thickness;
            float y = WallHeightMeters / 2f;
            Box(parent, "Wall +x", new Vector3(half + thickness / 2f, y, 0f), new Vector3(thickness, WallHeightMeters, length), BorderColor);
            Box(parent, "Wall -x", new Vector3(-half - thickness / 2f, y, 0f), new Vector3(thickness, WallHeightMeters, length), BorderColor);
            Box(parent, "Wall +y", new Vector3(0f, y, half + thickness / 2f), new Vector3(length, WallHeightMeters, thickness), BorderColor);
            Box(parent, "Wall -y", new Vector3(0f, y, -half - thickness / 2f), new Vector3(length, WallHeightMeters, thickness), BorderColor);
        }

        /// <summary>"+X" and "+Y" labels just outside the field, where the Swing viewer puts them.</summary>
        private static void AddAxisLabels(Transform parent)
        {
            Label(parent, "+X", FtcToUnity.Position(FtcToUnity.FieldHalfWidthInches + 5, 0, 0.02f));
            Label(parent, "+Y", FtcToUnity.Position(FtcToUnity.FieldHalfWidthInches - 4, FtcToUnity.FieldHalfWidthInches + 5, 0.02f));
        }

        private static void AddRobot(Transform parent, Built built, float lengthInches, float widthInches)
        {
            float length = FtcToUnity.Meters(lengthInches);
            float width = FtcToUnity.Meters(widthInches);
            var robot = new GameObject("Robot").transform;
            robot.SetParent(parent, false);
            built.robot = robot;

            // The robot's local +z is its front (FtcToUnity.Rotation turns +z to face the heading).
            float bodyY = RobotClearanceMeters + RobotHeightMeters / 2f;
            float top = RobotClearanceMeters + RobotHeightMeters;
            // Flat dark navy like the Swing robot, so it reads the same in the top-down view.
            Box(robot, "Body", new Vector3(0f, bodyY, 0f), new Vector3(width, RobotHeightMeters, length), RobotFillColor, unlit: true);

            // Light outline around the top, like the Swing robot's outline.
            const float rim = 0.006f;
            Box(robot, "Outline left", new Vector3(-width / 2f + rim / 2f, top, 0f), new Vector3(rim, 0.004f, length), RobotOutlineColor, unlit: true);
            Box(robot, "Outline right", new Vector3(width / 2f - rim / 2f, top, 0f), new Vector3(rim, 0.004f, length), RobotOutlineColor, unlit: true);
            Box(robot, "Outline back", new Vector3(0f, top, -length / 2f + rim / 2f), new Vector3(width, 0.004f, rim), RobotOutlineColor, unlit: true);
            Box(robot, "Outline front", new Vector3(0f, top, length / 2f - rim / 2f), new Vector3(width, 0.004f, rim), RobotOutlineColor, unlit: true);
            // Front edge in cyan, so the front shows even without the arrow.
            Box(robot, "Front edge", new Vector3(0f, top, length / 2f - 0.008f), new Vector3(width, 0.005f, 0.016f), HudTheme.Accent, unlit: true);

            // Four wheels in the corners, on top so they show from above, and the intake across the front.
            float wheelForward = FtcToUnity.Meters(lengthInches / 2f - WheelLengthInches / 2f - 1f);
            float wheelLeft = FtcToUnity.Meters(widthInches / 2f - WheelWidthInches / 2f - 0.6f);
            Vector3 wheelSize = new Vector3(FtcToUnity.Meters(WheelWidthInches), 0.006f, FtcToUnity.Meters(WheelLengthInches));
            built.wheels = new[]
            {
                // Unity +x is the robot's right, so "left" wheels sit at -x.
                Box(robot, "Wheel front left", new Vector3(-wheelLeft, top + 0.002f, wheelForward), wheelSize, WheelColor, unlit: true).GetComponent<Renderer>(),
                Box(robot, "Wheel front right", new Vector3(wheelLeft, top + 0.002f, wheelForward), wheelSize, WheelColor, unlit: true).GetComponent<Renderer>(),
                Box(robot, "Wheel rear left", new Vector3(-wheelLeft, top + 0.002f, -wheelForward), wheelSize, WheelColor, unlit: true).GetComponent<Renderer>(),
                Box(robot, "Wheel rear right", new Vector3(wheelLeft, top + 0.002f, -wheelForward), wheelSize, WheelColor, unlit: true).GetComponent<Renderer>(),
            };
            built.intake = Box(robot, "Intake", new Vector3(0f, top + 0.002f, length / 2f - FtcToUnity.Meters(2.2f)),
                new Vector3(width * 0.55f, 0.006f, FtcToUnity.Meters(1.2f)), WheelColor, unlit: true).GetComponent<Renderer>();

            // White heading arrow: a shaft from the center to past the front, and a wedge tip.
            float arrowY = top + 0.012f;
            float shaftLength = length / 2f + FtcToUnity.Meters(4f);
            Box(robot, "Heading arrow", new Vector3(0f, arrowY, shaftLength / 2f), new Vector3(0.012f, 0.004f, shaftLength), HudTheme.Text, unlit: true);
            GameObject tip = Box(robot, "Heading arrow tip", new Vector3(0f, arrowY, shaftLength + 0.01f),
                new Vector3(0.06f, 0.004f, 0.06f), HudTheme.Text, unlit: true);
            tip.transform.localRotation = Quaternion.Euler(0f, 45f, 0f);
        }

        private static LineRenderer AddTrail(Transform parent)
        {
            var line = new GameObject("Trail").AddComponent<LineRenderer>();
            line.transform.SetParent(parent, false);
            line.useWorldSpace = true;
            line.widthMultiplier = 0.014f;
            line.numCapVertices = 2;
            line.material = new Material(Shader.Find("Sprites/Default"));
            line.startColor = HudTheme.Accent;
            line.endColor = HudTheme.Accent;
            line.shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.Off;
            line.positionCount = 0;
            return line;
        }

        /// <summary>
        /// A colored box (a scaled cube) without a collider: nothing in Stage U1 collides. Unlit
        /// boxes show their exact color from any angle (for flat markings); lit ones get shading.
        /// </summary>
        private static GameObject Box(Transform parent, string name, Vector3 localPosition, Vector3 size, Color color, bool unlit = false)
        {
            GameObject box = GameObject.CreatePrimitive(PrimitiveType.Cube);
            box.name = name;
            Object.Destroy(box.GetComponent<Collider>());
            box.transform.SetParent(parent, false);
            box.transform.localPosition = localPosition;
            box.transform.localScale = size;
            Renderer renderer = box.GetComponent<Renderer>();
            if (unlit)
            {
                renderer.material = new Material(Shader.Find(name == "Field" ? "Unlit/Texture" : "Unlit/Color")) { color = color };
                renderer.shadowCastingMode = UnityEngine.Rendering.ShadowCastingMode.Off;
            }
            else
            {
                Material material = renderer.material;
                material.color = color;
                // Matte, not shiny: Unity's default material is semi-glossy, which shows bright glare.
                material.SetFloat("_Glossiness", 0.05f);
                material.SetFloat("_Metallic", 0f);
            }
            return box;
        }

        private static void Label(Transform parent, string text, Vector3 position)
        {
            var label = new GameObject("Label " + text).AddComponent<TextMesh>();
            label.transform.SetParent(parent, false);
            label.transform.position = position;
            label.transform.rotation = Quaternion.Euler(90f, 0f, 0f); // lying flat, readable from above
            label.text = text;
            label.font = Resources.GetBuiltinResource<Font>("LegacyRuntime.ttf");
            label.GetComponent<MeshRenderer>().material = label.font.material;
            label.fontSize = 48;
            label.characterSize = 0.012f;
            label.anchor = TextAnchor.MiddleLeft;
            label.color = HudTheme.Muted;
        }

        private static Color Hex(int rgb)
        {
            return new Color(((rgb >> 16) & 0xFF) / 255f, ((rgb >> 8) & 0xFF) / 255f, (rgb & 0xFF) / 255f);
        }
    }
}

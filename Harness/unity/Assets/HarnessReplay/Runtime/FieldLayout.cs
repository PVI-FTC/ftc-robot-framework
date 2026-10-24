using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using Newtonsoft.Json;
using UnityEngine;

namespace PviHarness.Replay
{
    /// <summary>
    /// One season's field, read from <c>Harness/fields/&lt;id&gt;.json</c> (for example
    /// <c>biobuzz-2026.json</c>): its fixed parts (boxes and cylinders in FTC inches), its AprilTags
    /// and where the numbers came from. The format is documented in Harness/README.md ("Season field
    /// files"). It is plain JSON with no Unity in it, so the harness's Java can read the same files.
    /// </summary>
    public sealed class FieldLayoutData
    {
        public string format;
        public int version;
        /// <summary>Short id, the file name without .json: "biobuzz-2026".</summary>
        public string id;
        /// <summary>The game's name, shown in the field picker: "BIOBUZZ".</summary>
        public string name;
        public string season;
        public List<string> sources = new List<string>();
        public string frame;
        public string accuracyNote;
        public string startingNote;
        public List<FieldElementData> elements = new List<FieldElementData>();
        public List<FieldTagData> aprilTags = new List<FieldTagData>();
    }

    /// <summary>
    /// A fixed part of the field: a box or an upright cylinder. Position is the part's center, in
    /// FTC inches (z = height of the center above the tiles). A box's length runs along +x and its
    /// width along +y before it is turned: first tilted (<see cref="tiltDegrees"/>, positive raises
    /// its +x end), then turned to <see cref="headingDegrees"/> (counter-clockwise from +x).
    /// </summary>
    public sealed class FieldElementData
    {
        public string id;
        public string name;
        /// <summary>"box" or "cylinder".</summary>
        public string shape;
        public double xInches;
        public double yInches;
        public double zInches;
        public double lengthInches;
        public double widthInches;
        public double heightInches;
        /// <summary>Cylinders only.</summary>
        public double diameterInches;
        public double headingDegrees;
        public double tiltDegrees;
        /// <summary>"#RRGGBB".</summary>
        public string color;
        /// <summary>"nominal" (the manual's numbers), "approximate" (worked out from its figures) or "sdk".</summary>
        public string accuracy;
    }

    /// <summary>
    /// One AprilTag. Position (its center) and <see cref="facing"/> (the way its printed side
    /// points, a unit vector) are in the FTC field frame, or null when the field position isn't
    /// known (such tags aren't drawn).
    /// </summary>
    public sealed class FieldTagData
    {
        public int id;
        public string name;
        /// <summary>The SDK's cluster name for tags printed together (BIOBUZZ), or null.</summary>
        public string cluster;
        public double sizeInches;
        /// <summary>The SDK's position of this tag inside its cluster, copied exactly; null without a cluster.</summary>
        public FieldVectorData clusterOffsetInches;
        public double? xInches;
        public double? yInches;
        public double? zInches;
        public FieldVectorData facing;
        /// <summary>True if the tag moves during a match (BIOBUZZ HIVE tags); its pose here is the starting one.</summary>
        public bool moves;
        public string accuracy;

        /// <summary>True when the file says where the tag is and which way it faces.</summary>
        public bool HasFieldPose => xInches.HasValue && yInches.HasValue && zInches.HasValue && facing != null;
    }

    /// <summary>A direction or offset with x, y and z (FTC frame unless the field says otherwise).</summary>
    public sealed class FieldVectorData
    {
        public double x;
        public double y;
        public double z;
    }

    /// <summary>
    /// Finds and reads the season field files, and picks which one to show.
    /// </summary>
    public static class FieldLayouts
    {
        /// <summary>The "no season" choice: only the plain tiled field.</summary>
        public const string PlainFieldId = "plain";
        /// <summary>The format versions this project can read.</summary>
        public static readonly int[] SupportedVersions = { 1 };

        /// <summary>Harness/fields, found from wherever this app runs (see <see cref="ResolveFolder"/>).</summary>
        public static string DefaultFolder => ResolveFolder(Environment.GetCommandLineArgs(), Application.dataPath, Application.isEditor);

        /// <summary>
        /// The field files folder: <c>-fields &lt;folder&gt;</c> on the command line, else Harness/fields
        /// (from Harness/unity/Assets in the Editor, or Harness/unity/Build/HarnessReplay_Data in the built app).
        /// </summary>
        public static string ResolveFolder(string[] args, string dataPath, bool isEditor)
        {
            for (int i = 0; i < args.Length - 1; i++)
            {
                if (string.Equals(args[i], "-fields", StringComparison.OrdinalIgnoreCase))
                {
                    return Path.GetFullPath(args[i + 1]);
                }
            }
            return isEditor
                ? Path.GetFullPath(Path.Combine(dataPath, "..", "..", "fields"))
                : Path.GetFullPath(Path.Combine(dataPath, "..", "..", "..", "fields"));
        }

        /// <summary>Reads one field file. Throws <see cref="RecordingFormatException"/> saying what is wrong.</summary>
        public static FieldLayoutData Load(string path)
        {
            FieldLayoutData field;
            try
            {
                field = JsonConvert.DeserializeObject<FieldLayoutData>(File.ReadAllText(path));
            }
            catch (Exception e) when (e is IOException || e is JsonException || e is UnauthorizedAccessException)
            {
                throw new RecordingFormatException("Can't read field file " + path + ": " + e.Message, e);
            }
            string file = Path.GetFileName(path);
            if (field == null || field.format != "pvi-harness-field")
            {
                throw new RecordingFormatException(file + " is not a field file (expected \"format\": \"pvi-harness-field\").");
            }
            if (!SupportedVersions.Contains(field.version))
            {
                throw new RecordingFormatException(file + " is field format version " + field.version
                    + "; this project reads version " + string.Join(", ", SupportedVersions) + ".");
            }
            if (string.IsNullOrWhiteSpace(field.id) || string.IsNullOrWhiteSpace(field.name))
            {
                throw new RecordingFormatException(file + " needs an \"id\" and a \"name\".");
            }
            foreach (FieldElementData element in field.elements)
            {
                if (element.shape != "box" && element.shape != "cylinder")
                {
                    throw new RecordingFormatException(file + ": element \"" + element.id + "\" has shape \""
                        + element.shape + "\"; expected \"box\" or \"cylinder\".");
                }
            }
            return field;
        }

        /// <summary>
        /// Every readable field file in a folder, sorted by id. Files that can't be read are skipped
        /// and reported in <paramref name="problems"/>, so one bad file doesn't hide the others.
        /// </summary>
        public static List<FieldLayoutData> LoadAll(string folder, List<string> problems)
        {
            var fields = new List<FieldLayoutData>();
            if (!Directory.Exists(folder))
            {
                return fields;
            }
            foreach (string path in Directory.GetFiles(folder, "*.json").OrderBy(p => p, StringComparer.OrdinalIgnoreCase))
            {
                try
                {
                    fields.Add(Load(path));
                }
                catch (RecordingFormatException e)
                {
                    problems?.Add(e.Message);
                }
            }
            return fields;
        }

        /// <summary>
        /// Which field to show: the one a recording asks for (its header's optional "field"), else
        /// the one picked last time, else the plain field. Ids that aren't available are ignored.
        /// </summary>
        public static string Pick(string recordingField, string remembered, IEnumerable<string> availableIds)
        {
            var available = new HashSet<string>(availableIds, StringComparer.OrdinalIgnoreCase);
            if (!string.IsNullOrEmpty(recordingField) && available.Contains(recordingField))
            {
                return recordingField;
            }
            if (!string.IsNullOrEmpty(remembered) && available.Contains(remembered))
            {
                return remembered;
            }
            return PlainFieldId;
        }

        /// <summary>The field id after <c>-field</c> on the command line ("-field biobuzz-2026"), or null.</summary>
        public static string FieldFromCommandLine(string[] args)
        {
            for (int i = 0; i < args.Length - 1; i++)
            {
                if (string.Equals(args[i], "-field", StringComparison.OrdinalIgnoreCase))
                {
                    return args[i + 1];
                }
            }
            return null;
        }

        /// <summary>A "#RRGGBB" color, or grey if it can't be read.</summary>
        public static Color ParseColor(string hex)
        {
            return ColorUtility.TryParseHtmlString(hex ?? "", out Color color) ? color : new Color(0.5f, 0.5f, 0.5f);
        }
    }
}

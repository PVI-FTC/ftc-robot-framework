package pvi.harness.hardware;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The AprilTags printed on a season's field, read from its field file ({@code fields/*.json},
 * the {@code "aprilTags"} list): where each tag is, which way it faces, and how big it is.
 *
 * <p>The fake webcam uses them to work out by itself which tags are in view (see
 * {@link CameraView}), instead of a test scripting them. Tags without a field position (for
 * example DECODE's OBELISK tags) are left out. Tags marked {@code moves} (BIOBUZZ's tags on the
 * turning HIVE) are used where the file puts them: the harness doesn't move them yet.</p>
 */
public final class FieldAprilTags {

    /**
     * One tag on the field, in the FTC field frame (inches, origin at the field centre on the
     * tiles, z up).
     *
     * @param id          the tag's number
     * @param name        what the field file calls it, e.g. "RED SCORING"
     * @param sizeInches  the black square's width
     * @param xInches     the tag centre's position
     * @param yInches     the tag centre's position
     * @param zInches     the tag centre's height above the tiles
     * @param facingX     the direction the printed side faces (a unit vector)
     * @param facingY     the direction the printed side faces
     * @param facingZ     the direction the printed side faces
     * @param moves       true if the tag moves during a match (it is still used where the file puts it)
     */
    public record Tag(int id, String name, double sizeInches, double xInches, double yInches, double zInches,
                      double facingX, double facingY, double facingZ, boolean moves) {
        public Tag {
            double length = Math.sqrt(facingX * facingX + facingY * facingY + facingZ * facingZ);
            if (!(length > 0.5 && length < 1.5)) {
                throw new IllegalArgumentException("Tag " + id + "'s facing must be a unit vector, but its length is " + length);
            }
            if (!(sizeInches > 0)) {
                throw new IllegalArgumentException("Tag " + id + "'s size must be more than 0 inches, but was " + sizeInches);
            }
            // Store it exactly unit length, so the angle maths stays exact.
            facingX /= length;
            facingY /= length;
            facingZ /= length;
        }
    }

    /** Only static methods here; nothing to create. */
    private FieldAprilTags() {
    }

    /**
     * The tags of a season field, by its id (the file name without {@code .json}), looked for in
     * {@code fields/} and then {@code fields/disabled/} (relative to the Harness folder).
     */
    public static List<Tag> forField(String fieldId) {
        for (Path file : new Path[] {Path.of("fields", fieldId + ".json"), Path.of("fields", "disabled", fieldId + ".json")}) {
            if (Files.isRegularFile(file)) {
                return read(file);
            }
        }
        throw new IllegalArgumentException("No field file for \"" + fieldId + "\" in fields/ or fields/disabled/.");
    }

    /** The tags in one field file, in the file's order. */
    public static List<Tag> read(Path fieldFile) {
        try {
            JSONObject field = new JSONObject(Files.readString(fieldFile, StandardCharsets.UTF_8));
            JSONArray items = field.optJSONArray("aprilTags");
            List<Tag> tags = new ArrayList<>();
            for (int i = 0; items != null && i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);
                if (item.isNull("xInches") || item.isNull("facing")) {
                    continue;   // no field position (for example DECODE's OBELISK tags)
                }
                JSONObject facing = item.getJSONObject("facing");
                tags.add(new Tag(item.getInt("id"), item.optString("name", ""), item.getDouble("sizeInches"),
                        item.getDouble("xInches"), item.getDouble("yInches"), item.getDouble("zInches"),
                        facing.getDouble("x"), facing.getDouble("y"), facing.getDouble("z"),
                        item.optBoolean("moves", false)));
            }
            return Collections.unmodifiableList(tags);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read the field file " + fieldFile.toAbsolutePath(), e);
        } catch (JSONException | IllegalArgumentException e) {
            throw new IllegalStateException("The field file " + fieldFile + " has a bad \"aprilTags\" entry: " + e.getMessage(), e);
        }
    }
}

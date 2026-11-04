package pvi.harness.hardware;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import pvi.harness.physics.ModelValue;

/**
 * Where a webcam sits on the robot and what it can see: its mount, field of view, picture size
 * and how far away it can still read an AprilTag. Plain data, read from the robot's
 * {@code robots/<id>/camera.properties} ({@code webcam.<name>.<fact>}), so the fake webcam can work out which tags are in view and Unity
 * or the viewer can show what the camera sees.
 *
 * <p>The mount is measured from the robot's center on the floor (the point the drive pose is
 * for), in the robot's frame: forward, left and up. All three angles 0 means the camera is level
 * and looks straight forward. Yaw turns the lens left, pitch tilts it up, and roll turns the
 * picture clockwise as seen from behind the camera; they are applied in that order (yaw, then
 * pitch, then roll), each about the camera's own axes.</p>
 *
 * @param name                 the webcam's configuration name, e.g. "logitechVisionWebcam"
 * @param forwardInches        how far in front of the robot center the lens is
 * @param leftInches           how far left of the robot center (negative: right)
 * @param upInches             how high above the floor
 * @param yawDegrees           turned left (negative: right)
 * @param pitchDegrees         tilted up (negative: down)
 * @param rollDegrees          the picture turned clockwise, seen from behind the camera
 * @param horizontalFovDegrees how wide the picture is, edge to edge
 * @param verticalFovDegrees   how tall the picture is, edge to edge
 * @param imageWidthPixels     picture width
 * @param imageHeightPixels    picture height
 * @param maxRangeInches       farthest distance at which a tag is still read
 * @param maxViewAngleDegrees  how far a tag may be turned away from the camera and still be read
 * @param aprilTags            where the tags it sees come from: {@link #FIELD_TAGS} (worked out
 *                             from the field's tags and the robot's pose) or {@link #SCRIPTED_TAGS}
 *                             (only an {@code AprilTagScript}, as before)
 */
public record CameraSetup(String name, ModelValue forwardInches, ModelValue leftInches, ModelValue upInches,
                          ModelValue yawDegrees, ModelValue pitchDegrees, ModelValue rollDegrees,
                          ModelValue horizontalFovDegrees, ModelValue verticalFovDegrees,
                          ModelValue imageWidthPixels, ModelValue imageHeightPixels, ModelValue maxRangeInches,
                          ModelValue maxViewAngleDegrees, String aprilTags) {

    /** Unit label for angles. */
    public static final String DEGREES = "deg";
    /** Unit label for picture sizes. */
    public static final String PIXELS = "px";
    /** {@link #aprilTags()}: the camera sees the field's tags that are in view. */
    public static final String FIELD_TAGS = "field";
    /** {@link #aprilTags()}: the camera sees only what a test's {@code AprilTagScript} says. */
    public static final String SCRIPTED_TAGS = "script";

    /** The fact names, in a fixed order: the {@code harness.properties} key endings and the recording's field names. */
    public static final List<String> FACT_NAMES = List.of("forwardInches", "leftInches", "upInches",
            "yawDegrees", "pitchDegrees", "rollDegrees", "horizontalFovDegrees", "verticalFovDegrees",
            "imageWidthPixels", "imageHeightPixels", "maxRangeInches", "maxViewAngleDegrees");

    /** Refuses a missing name or value, a field of view outside 0 to 180 degrees, and sizes or ranges that make no sense. */
    public CameraSetup {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("A camera setup needs the webcam's configuration name.");
        }
        for (ModelValue v : new ModelValue[] {forwardInches, leftInches, upInches, yawDegrees, pitchDegrees,
                rollDegrees, horizontalFovDegrees, verticalFovDegrees, imageWidthPixels, imageHeightPixels,
                maxRangeInches, maxViewAngleDegrees}) {
            if (v == null) {
                throw new IllegalArgumentException("The camera setup for " + name + " needs every value.");
            }
        }
        for (ModelValue fov : new ModelValue[] {horizontalFovDegrees, verticalFovDegrees}) {
            if (!(fov.value() > 0 && fov.value() < 180)) {
                throw new IllegalArgumentException(name + ": a field of view must be between 0 and 180 degrees, got "
                        + fov.value());
            }
        }
        for (ModelValue size : new ModelValue[] {imageWidthPixels, imageHeightPixels}) {
            if (!(size.value() >= 1) || size.value() != Math.rint(size.value())) {
                throw new IllegalArgumentException(name + ": a picture size must be a whole number of pixels, got "
                        + size.value());
            }
        }
        if (!(maxRangeInches.value() > 0)) {
            throw new IllegalArgumentException(name + ": the range must be above zero, got " + maxRangeInches.value());
        }
        if (!(maxViewAngleDegrees.value() > 0 && maxViewAngleDegrees.value() <= 90)) {
            throw new IllegalArgumentException(name + ": the view angle must be above 0 and at most 90 degrees, got "
                    + maxViewAngleDegrees.value());
        }
        if (!FIELD_TAGS.equals(aprilTags) && !SCRIPTED_TAGS.equals(aprilTags)) {
            throw new IllegalArgumentException(name + ": aprilTags must be " + FIELD_TAGS + " or " + SCRIPTED_TAGS
                    + ", got " + aprilTags);
        }
    }

    /** Every fact by its name ({@link #FACT_NAMES}), in that order. */
    public Map<String, ModelValue> facts() {
        Map<String, ModelValue> facts = new LinkedHashMap<>();
        ModelValue[] values = {forwardInches, leftInches, upInches, yawDegrees, pitchDegrees, rollDegrees,
                horizontalFovDegrees, verticalFovDegrees, imageWidthPixels, imageHeightPixels, maxRangeInches,
                maxViewAngleDegrees};
        for (int i = 0; i < values.length; i++) {
            facts.put(FACT_NAMES.get(i), values[i]);
        }
        return facts;
    }

    /** This setup with only {@link #aprilTags()} changed, e.g. after a test scripted the webcam. */
    public CameraSetup withAprilTags(String mode) {
        return new CameraSetup(name, forwardInches, leftInches, upInches, yawDegrees, pitchDegrees, rollDegrees,
                horizontalFovDegrees, verticalFovDegrees, imageWidthPixels, imageHeightPixels, maxRangeInches,
                maxViewAngleDegrees, mode);
    }

    /** The unit of each fact, by its name. */
    public static String unitOf(String factName) {
        if (factName.endsWith("Inches")) {
            return ModelValue.INCHES;
        }
        return factName.endsWith("Degrees") ? DEGREES : PIXELS;
    }

    /** The names of the facts that are still guesses, for reports and displays. */
    public List<String> placeholders() {
        List<String> names = new ArrayList<>();
        facts().forEach((factName, value) -> {
            if (!value.measured()) {
                names.add(factName);
            }
        });
        return names;
    }
}

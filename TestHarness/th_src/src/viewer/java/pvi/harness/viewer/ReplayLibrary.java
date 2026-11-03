package pvi.harness.viewer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The saved recordings the replay picker lists: what each file is, when it was saved and how
 * big it is. Worked out from the file name and the file system only, without reading the
 * recordings, so even a long list opens at once. Plain data, so tests can check it.
 */
public final class ReplayLibrary {

    /** What made a recording, from its file name. */
    public enum Kind {
        /** A live drive saved from the viewer: {@code drive-<teleop>-<date>-<time>.json}. */
        DRIVE("Drive"),
        /** A ready-made scenario run from the viewer: {@code viewer-<id>.json} or {@code scenario-<id>.json}. */
        SCENARIO("Scenario"),
        /** Saved by a test, or anything else. */
        TEST("Test");

        private final String title;

        Kind(String title) {
            this.title = title;
        }

        /** "Drive", "Scenario" or "Test". */
        public String title() {
            return title;
        }

        /** The kind for a file name. */
        public static Kind of(String fileName) {
            String name = fileName.toLowerCase(Locale.ROOT);
            if (name.startsWith("drive-")) {
                return DRIVE;
            }
            if (name.startsWith("viewer-") || name.startsWith("scenario-")) {
                return SCENARIO;
            }
            return TEST;
        }
    }

    /**
     * One saved recording.
     *
     * @param file     where it is
     * @param name     the file name without ".json"
     * @param kind     what made it
     * @param saved    when the file was last written
     * @param sizeBytes the file's size
     */
    public record Entry(Path file, String name, Kind kind, Instant saved, long sizeBytes) {

        /** The size for people: "812 KB", "1.4 MB". */
        public String sizeText() {
            if (sizeBytes < 1024 * 1024) {
                return Math.max(1, Math.round(sizeBytes / 1024.0)) + " KB";
            }
            return String.format(Locale.ROOT, "%.1f MB", sizeBytes / (1024.0 * 1024.0));
        }

        /** True if {@code query} (ignoring case) appears in the name or the kind. Blank matches everything. */
        public boolean matches(String query) {
            if (query == null || query.isBlank()) {
                return true;
            }
            String q = query.trim().toLowerCase(Locale.ROOT);
            return name.toLowerCase(Locale.ROOT).contains(q) || kind.title().toLowerCase(Locale.ROOT).contains(q);
        }
    }

    /** Only static methods here; nothing to create. */
    private ReplayLibrary() {
    }

    /** Every {@code .json} recording in {@code folder}, newest first; empty if there is no folder. */
    public static List<Entry> list(Path folder) {
        List<Entry> entries = new ArrayList<>();
        for (Path file : ViewerChoices.recordingsNewestFirst(folder)) {
            String fileName = file.getFileName().toString();
            try {
                entries.add(new Entry(file, fileName.substring(0, fileName.length() - ".json".length()),
                        Kind.of(fileName), Files.getLastModifiedTime(file).toInstant(), Files.size(file)));
            } catch (IOException e) {
                throw new UncheckedIOException("Could not read the details of " + file, e);
            }
        }
        return entries;
    }
}

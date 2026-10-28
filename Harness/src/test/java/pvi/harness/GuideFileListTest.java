package pvi.harness;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import pvi.harness.runtime.HarnessConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Keeps {@code BRING_YOUR_OWN_ROBOT.md} honest: its list of PVI test files to delete in another
 * repository must be exactly the test files that use PVI's own TeamCode classes, or the PVI
 * extras that are only built with {@code pvi.extras=true} (the classes in {@code src/pvi*}).
 *
 * <p>Another team's repository doesn't have PVI's OpModes, so those tests can't compile there.
 * When someone adds or removes such a test, this fails and says which lines of the guide's list
 * to change. It only runs in PVI's own repository ({@code pvi.extras=true}); elsewhere the files
 * are meant to be gone.</p>
 */
class GuideFileListTest {
    /** The guide, relative to the Harness folder (the tests' working folder). */
    private static final Path GUIDE = Path.of("BRING_YOUR_OWN_ROBOT.md");
    /** Where the tests are. */
    private static final Path TESTS = Path.of("src/test/java/pvi/harness");
    /** An import of PVI's TeamCode (not the harness's own test samples in TeamCode packages). */
    private static final Pattern USES_PVI_TEAMCODE =
            Pattern.compile("org\\.firstinspires\\.ftc\\.teamcode\\.(opmodes|robots|common|subsystems|pedro|KimCode)\\b");
    /** Folders of PVI's extras, built only with pvi.extras=true. */
    private static final List<Path> PVI_EXTRAS = List.of(Path.of("src/pvi/java"), Path.of("src/pviTest/java"),
            Path.of("src/pviViewer/java"));
    /** A file name in the guide's list, such as {@code physics/PedroRouteTest.java}. */
    private static final Pattern LISTED_FILE = Pattern.compile("([A-Za-z0-9_/]+\\.java)");

    @Test
    void theGuideListsExactlyThePviTestFiles() throws IOException {
        assumeTrue(HarnessConfig.load().pviExtras(), "only in PVI's repository (pvi.extras=true)");

        Pattern usesPviExtras = pviExtrasClassNames();
        TreeSet<String> usingPvi = new TreeSet<>();
        try (Stream<Path> files = Files.walk(TESTS)) {
            for (Path file : (Iterable<Path>) files.filter(f -> f.toString().endsWith(".java"))::iterator) {
                String code = Files.readString(file);
                if (USES_PVI_TEAMCODE.matcher(code).find() || usesPviExtras.matcher(code).find()) {
                    usingPvi.add(TESTS.relativize(file).toString().replace('\\', '/'));
                }
            }
        }

        assertEquals(usingPvi, listedInGuide(),
                "The list of PVI test files in " + GUIDE + " (Step 2, the code block) is out of date."
                        + " Make it exactly the files on the left (the test files that use PVI's TeamCode or extras).");
    }

    /** Matches any class name from PVI's extras folders (for example the viewer's PVI scenarios). */
    private static Pattern pviExtrasClassNames() throws IOException {
        List<String> names = new ArrayList<>();
        for (Path folder : PVI_EXTRAS) {
            if (Files.isDirectory(folder)) {
                try (Stream<Path> files = Files.walk(folder)) {
                    files.map(f -> f.getFileName().toString()).filter(n -> n.endsWith(".java"))
                            .forEach(n -> names.add(Pattern.quote(n.substring(0, n.length() - ".java".length()))));
                }
            }
        }
        return Pattern.compile(names.isEmpty() ? "(?!)" : "\\b(" + String.join("|", names) + ")\\b");
    }

    /** The file names in the guide's code block that follows "Delete these test files". */
    private static TreeSet<String> listedInGuide() throws IOException {
        String guide = Files.readString(GUIDE);
        int start = guide.indexOf("Delete these test files");
        int blockStart = guide.indexOf("```", start);
        int blockEnd = guide.indexOf("```", blockStart + 3);
        if (start < 0 || blockStart < 0 || blockEnd < 0) {
            throw new IllegalStateException(GUIDE + " has no code block after \"Delete these test files\".");
        }
        TreeSet<String> listed = new TreeSet<>();
        Matcher file = LISTED_FILE.matcher(guide.substring(blockStart + 3, blockEnd));
        while (file.find()) {
            listed.add(file.group(1));
        }
        return listed;
    }
}

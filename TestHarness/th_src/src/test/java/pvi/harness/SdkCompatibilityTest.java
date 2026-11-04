package pvi.harness;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.firstinspires.ftc.robotcore.internal.opmode.OpModeServices;
import org.firstinspires.ftc.vision.VisionPortal;
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessorImpl;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import pvi.harness.runtime.HarnessConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Safety net for FTC SDK upgrades (RP9): fails with a plain message when the SDK changes under
 * the harness.
 *
 * <p>The harness leans on the SDK in three ways that the compiler can't check for us:</p>
 * <ol>
 *   <li>It has its own copies of {@code VisionPortal} and {@code AprilTagProcessorImpl}, which
 *       must keep every public constructor, method and field of the SDK's versions, or TeamCode
 *       that compiles on the robot won't compile (or run) here.</li>
 *   <li>{@code OpModeRunner} reaches into non-public SDK fields by name. A renamed field only
 *       shows up when a test runs.</li>
 *   <li>{@code build.gradle} takes the robot library versions from
 *       {@code ../build.dependencies.gradle}, leaving out the Android-only ones listed in
 *       {@code harness.properties} ({@code host.skipLibraries}). If that goes wrong, or the skip
 *       list goes stale after an SDK upgrade, the harness would quietly test against the wrong
 *       libraries.</li>
 * </ol>
 *
 * <p>If one of these tests fails after an SDK upgrade, the message says what to update.</p>
 */
class SdkCompatibilityTest {
    /** The harness's own copies of SDK classes. Each must match the SDK class with the same name. */
    private static final List<Class<?>> HARNESS_COPIES = List.of(VisionPortal.class, AprilTagProcessorImpl.class);

    /** System property the Gradle test task sets: the robot libraries the harness really uses. */
    private static final String ROBOT_LIBRARIES_PROPERTY = "harness.robotLibraries";
    /** Matches lines such as {@code implementation 'org.firstinspires.ftc:Vision:12.0.0'}. */
    private static final Pattern ROBOT_LIB = Pattern.compile("^\\s*(?:implementation|api)\\s+'([^:']+):([^:']+):([^:']+)'", Pattern.MULTILINE);

    /**
     * WHAT: The harness's {@code VisionPortal} and {@code AprilTagProcessorImpl} have every public
     * constructor, method and field of the real SDK classes (nested classes too, such as
     * {@code VisionPortal.Builder}).
     *
     * <p>HOW: Loads the SDK's own versions from the Vision jar in a separate class loader (the
     * harness's copies come first on the normal class path, so they hide the SDK's), writes every
     * public member of both as text such as {@code Builder setCamera(CameraName)}, and lists the
     * ones the harness copy is missing.</p>
     *
     * <p>WHY: After an SDK upgrade, a robot program might call a new VisionPortal method. It
     * would build for the robot but not for the harness, and the error wouldn't say why.</p>
     */
    @Test
    void harnessCopiesOfSdkClassesHaveEverySdkMember() throws Exception {
        String visionVersion = harnessLibraryVersions().get("org.firstinspires.ftc:Vision");
        assertNotNull(visionVersion, "The harness doesn't use org.firstinspires.ftc:Vision, so there is"
                + " nothing to compare its VisionPortal copy with. Is it in host.skipLibraries by mistake?");
        String visionJarName = "Vision-" + visionVersion + ".jar";
        try (URLClassLoader sdkLoader = sdkOnlyClassLoader()) {
            List<String> problems = new ArrayList<>();
            for (Class<?> harnessCopy : HARNESS_COPIES) {
                Class<?> sdkClass = Class.forName(harnessCopy.getName(), false, sdkLoader);
                // Make sure we really compare against the SDK, not against the harness copy again.
                String sdkSource = sdkClass.getProtectionDomain().getCodeSource().getLocation().getPath();
                assertTrue(sdkSource.endsWith(visionJarName),
                        "Expected the SDK's " + sdkClass.getName() + " to come from " + visionJarName
                                + " but it came from " + sdkSource);
                compareClass(sdkClass, harnessCopy, problems);
            }
            if (!problems.isEmpty()) {
                fail("The harness copies of SDK classes no longer match the SDK in " + visionJarName + ".\n"
                        + "Add these to the harness copy (in src/main/java), doing nothing or the simplest"
                        + " safe thing:\n  " + String.join("\n  ", problems));
            }
        }
    }

    /**
     * WHAT: The non-public SDK fields that {@code OpModeRunner} sets and reads by name still exist,
     * with a type the harness can use.
     *
     * <p>HOW: Looks for each field in {@code OpMode} and its SDK superclasses, the same way
     * {@code OpModeRunner} does, and checks its type.</p>
     *
     * <p>WHY: {@code OpModeRunner} finds these fields by name. If the SDK renames one, STOP or
     * {@code requestOpModeStop()} would stop working, and the failure would show up far from the
     * cause. The names here must match {@code OpModeRunner}'s.</p>
     */
    @Test
    void sdkFieldsTheRunnerReachesIntoStillExist() {
        List<String> problems = new ArrayList<>();
        // "has a stop been requested" flag: OpModeRunner reads it as a boolean.
        checkOpModeField("stopRequested", boolean.class, problems);
        // Link to the Robot Controller's OpMode manager: OpModeRunner stores its HarnessOpModeServices
        // (an OpModeServices) there.
        checkOpModeField("internalOpModeServices", OpModeServices.class, problems);
        if (!problems.isEmpty()) {
            fail("The FTC SDK changed a non-public field that OpModeRunner uses. Update OpModeRunner"
                    + " (and this test) for the new SDK:\n  " + String.join("\n  ", problems));
        }
    }

    /**
     * WHAT: The harness uses exactly the robot build's libraries and versions, minus the
     * Android-only ones in {@code host.skipLibraries}, and every skipped name is really in the
     * robot build.
     *
     * <p>HOW: Reads the robot build's library file as text (it is only read, never changed),
     * takes out the skipped names from {@code harness.properties}, and compares the rest with the
     * list {@code build.gradle} actually used (the {@code harness.robotLibraries} property).</p>
     *
     * <p>WHY: After a team upgrades the SDK, the harness must test against the new libraries. A
     * skipped name that is no longer in the robot build means the skip list is stale (for example
     * a library was renamed), and a new Android-only library would need adding to it.</p>
     */
    @Test
    void harnessLibraryVersionsMatchTheRobotBuild() throws IOException {
        HarnessConfig config = HarnessConfig.load();
        Path robotFile = Path.of(config.hostDependenciesFile());
        Map<String, String> robot = versions(Files.readString(robotFile), ROBOT_LIB);
        Map<String, String> harness = harnessLibraryVersions();

        List<String> problems = new ArrayList<>();
        Map<String, String> expected = new TreeMap<>(robot);
        for (String skipped : config.skipLibraries()) {
            if (expected.remove(skipped) == null) {
                problems.add(skipped + ": in host.skipLibraries but not in " + robotFile
                        + " (remove it from harness.properties, or fix the spelling)");
            }
        }
        expected.forEach((library, robotVersion) -> {
            String harnessVersion = harness.get(library);
            if (harnessVersion == null) {
                problems.add(library + ": robot build uses " + robotVersion + ", harness leaves it out");
            } else if (!harnessVersion.equals(robotVersion)) {
                problems.add(library + ": robot build uses " + robotVersion + ", harness uses " + harnessVersion);
            }
        });
        harness.forEach((library, harnessVersion) -> {
            if (!expected.containsKey(library)) {
                problems.add(library + ": harness uses " + harnessVersion + ", but it is skipped or not in "
                        + robotFile);
            }
        });
        assertEquals(List.of(), problems,
                "The harness's robot libraries don't match the robot build. Check host.skipLibraries in"
                        + " harness.properties and how build.gradle reads " + robotFile + ".");
    }

    // ------------------------------------------------------------------------------------------

    /**
     * The robot libraries build.gradle really used, keyed by "group:name", from the
     * {@code harness.robotLibraries} property ("group:name:version" items, comma-separated).
     */
    private static Map<String, String> harnessLibraryVersions() {
        String list = System.getProperty(ROBOT_LIBRARIES_PROPERTY);
        assertNotNull(list, "The " + ROBOT_LIBRARIES_PROPERTY + " system property is not set. Run the"
                + " tests through Gradle (gradlew test), which sets it from build.gradle.");
        Map<String, String> found = new TreeMap<>();
        for (String item : list.split(",")) {
            String[] parts = item.trim().split(":");
            assertEquals(3, parts.length, "Expected group:name:version in " + ROBOT_LIBRARIES_PROPERTY
                    + ", got \"" + item + "\"");
            found.put(parts[0] + ":" + parts[1], parts[2]);
        }
        assertFalse(found.isEmpty(), ROBOT_LIBRARIES_PROPERTY + " lists no libraries");
        return found;
    }

    /** Every "group:name" to "version" a pattern finds in a Gradle file, in name order. */
    private static Map<String, String> versions(String gradleFile, Pattern line) {
        Map<String, String> found = new TreeMap<>();
        Matcher matcher = line.matcher(gradleFile);
        while (matcher.find()) {
            found.put(matcher.group(1) + ":" + matcher.group(2), matcher.group(3));
        }
        return found;
    }

    /**
     * A class loader that sees only the library jars on the test class path, not the harness's or
     * TeamCode's compiled folders, so it loads the SDK's own VisionPortal instead of the harness's.
     */
    private static URLClassLoader sdkOnlyClassLoader() throws IOException {
        List<URL> jars = new ArrayList<>();
        for (String entry : System.getProperty("java.class.path").split(File.pathSeparator)) {
            File file = new File(entry);
            if (file.isFile() && file.getName().endsWith(".jar")) {
                jars.add(file.toURI().toURL());
            }
        }
        // Parent = Java's own classes only, so nothing from the normal class path leaks in.
        return new URLClassLoader(jars.toArray(new URL[0]), ClassLoader.getPlatformClassLoader());
    }

    /** Adds a line to {@code problems} for each public SDK member the harness copy lacks. */
    private static void compareClass(Class<?> sdkClass, Class<?> harnessClass, List<String> problems) {
        TreeSet<String> missing = publicMembers(sdkClass);
        missing.removeAll(publicMembers(harnessClass));
        for (String member : missing) {
            problems.add(displayName(sdkClass) + ": " + member);
        }
        // Nested public classes (for example VisionPortal.Builder) must match too.
        for (Class<?> sdkNested : sdkClass.getDeclaredClasses()) {
            if (!Modifier.isPublic(sdkNested.getModifiers())) {
                continue;
            }
            Class<?> harnessNested = Arrays.stream(harnessClass.getDeclaredClasses())
                    .filter(c -> c.getSimpleName().equals(sdkNested.getSimpleName()))
                    .findFirst().orElse(null);
            if (harnessNested == null) {
                problems.add(displayName(sdkClass) + ": nested class " + sdkNested.getSimpleName() + " is missing");
            } else {
                compareClass(sdkNested, harnessNested, problems);
            }
        }
    }

    /**
     * Every public constructor, method and field a program can use on {@code type}, as text with
     * plain type names, so classes from two different class loaders can be compared.
     */
    private static TreeSet<String> publicMembers(Class<?> type) {
        TreeSet<String> members = new TreeSet<>();
        for (Constructor<?> constructor : type.getConstructors()) {
            members.add("new " + type.getSimpleName() + parameters(constructor.getParameterTypes()));
        }
        // getMethods() includes public methods inherited from superclasses and interfaces, so a
        // method the harness copy inherits counts as present.
        for (Method method : type.getMethods()) {
            if (!method.isSynthetic() && !method.isBridge() && method.getDeclaringClass() != Object.class) {
                members.add((Modifier.isStatic(method.getModifiers()) ? "static " : "")
                        + typeName(method.getReturnType()) + " " + method.getName()
                        + parameters(method.getParameterTypes()));
            }
        }
        // Public fields too (such as AprilTagProcessorImpl.TAG and enum constants), with whether
        // they are static or final, since TeamCode could read any of them.
        for (Field field : type.getFields()) {
            members.add((Modifier.isStatic(field.getModifiers()) ? "static " : "")
                    + (Modifier.isFinal(field.getModifiers()) ? "final " : "")
                    + "field " + typeName(field.getType()) + " " + field.getName());
        }
        return members;
    }

    /** "(int, android.util.Size)" for a parameter list. */
    private static String parameters(Class<?>[] types) {
        return Arrays.stream(types).map(SdkCompatibilityTest::typeName).collect(Collectors.joining(", ", "(", ")"));
    }

    /** A type's full name, written the way it looks in Java source ("int[]", "VisionPortal.Builder"). */
    private static String typeName(Class<?> type) {
        return type.getTypeName().replace('$', '.');
    }

    /** "VisionPortal.Builder" rather than "org.firstinspires.ftc.vision.VisionPortal$Builder". */
    private static String displayName(Class<?> type) {
        return type.getName().substring(type.getPackageName().length() + 1).replace('$', '.');
    }

    /**
     * Looks for a non-public field in OpMode and its SDK superclasses (as OpModeRunner does) and
     * checks that a value of {@code harnessValueType} fits in it.
     */
    private static void checkOpModeField(String name, Class<?> harnessValueType, List<String> problems) {
        for (Class<?> type = OpMode.class; type != null; type = type.getSuperclass()) {
            try {
                Field field = type.getDeclaredField(name);
                boolean fits = harnessValueType.isPrimitive()
                        ? field.getType() == harnessValueType
                        : field.getType().isAssignableFrom(harnessValueType);
                if (!fits) {
                    problems.add(type.getSimpleName() + "." + name + " is now a " + field.getType().getName()
                            + "; the harness uses it as a " + harnessValueType.getName());
                }
                return;
            } catch (NoSuchFieldException e) {
                // keep looking in the superclass
            }
        }
        problems.add("OpMode (or a superclass) no longer has a field named \"" + name + "\"");
    }
}

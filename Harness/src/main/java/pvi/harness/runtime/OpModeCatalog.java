package pvi.harness.runtime;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.Disabled;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Modifier;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Finds the robot's OpModes by itself, the way the Robot Controller app builds its menu: every
 * class marked {@code @TeleOp} or {@code @Autonomous}, leaving out the ones marked
 * {@code @Disabled}.
 *
 * <p>Without this, every OpMode the viewer, the Unity bridge or a test should offer has to be
 * typed into a list by hand, and a new OpMode is easy to forget. With it, a team adds an OpMode
 * to TeamCode and the harness sees it.</p>
 *
 * <p>How: it looks through the compiled classes of the given packages (and their sub-packages)
 * on the class path, and reads each class's annotations. Classes are loaded without running
 * their {@code static} code, so nothing in TeamCode runs just because it was listed. Abstract
 * classes and classes that aren't OpModes are skipped even if annotated.</p>
 *
 * <p>The result is plain data ({@link Entry}), so a menu, a test or Unity can use it. To run an
 * entry, make the OpMode with {@link Entry#newOpMode()}.</p>
 */
public final class OpModeCatalog {

    /**
     * One OpMode found in the robot code, as the Driver Station would list it.
     *
     * @param className       full class name, for example
     *                        {@code org.firstinspires.ftc.teamcode.opmodes.teleop.TeamATeleOp}
     * @param name            the name shown on the Driver Station: the annotation's {@code name},
     *                        or the class's simple name when that is empty (as the SDK does)
     * @param group           the annotation's {@code group} (empty if none)
     * @param period          {@code TELEOP} for {@code @TeleOp}, {@code AUTONOMOUS} for
     *                        {@code @Autonomous}
     * @param preselectTeleOp for an autonomous: the TeleOp the Driver Station picks next (empty if
     *                        none, and always empty for a TeleOp)
     */
    public record Entry(String className, String name, String group, MatchPeriod period, String preselectTeleOp) {

        /** The class's simple name, for example {@code TeamATeleOp}. */
        public String simpleName() {
            return className.substring(className.lastIndexOf('.') + 1).replace('$', '.');
        }

        /** Makes a new OpMode of this class, the way the Robot Controller does when you press INIT. */
        public OpMode newOpMode() {
            try {
                return (OpMode) Class.forName(className).getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("Could not create " + className
                        + ". An OpMode needs a public constructor with no arguments.", e);
            }
        }
    }

    /**
     * A class marked {@code @TeleOp} or {@code @Autonomous} that couldn't be loaded on this
     * computer (for example because a library it needs is missing), so it can't be offered. It is
     * reported, never quietly dropped: a team would otherwise wonder where its OpMode went.
     *
     * @param className full class name
     * @param reason    why it couldn't be loaded (the Java error)
     */
    public record LeftOut(String className, String reason) {
    }

    /**
     * What a search found: the OpModes that can run, and the OpModes that had to be left out.
     *
     * @param opModes every enabled OpMode, sorted by Driver Station name
     * @param leftOut every annotated OpMode class that couldn't be loaded, by class name
     */
    public record Scan(List<Entry> opModes, List<LeftOut> leftOut) {
    }

    /** Only static methods here; nothing to create. */
    private OpModeCatalog() {
    }

    /**
     * Every enabled OpMode in the packages the settings file names ({@code teamCode.opModePackages}
     * in {@code harness.properties}), sorted by Driver Station name.
     */
    public static List<Entry> fromConfig(HarnessConfig config) {
        return find(config.opModePackages());
    }

    /**
     * Every enabled {@code @TeleOp} and {@code @Autonomous} OpMode in the given packages and their
     * sub-packages, sorted by Driver Station name.
     *
     * @param packages package names, for example {@code org.firstinspires.ftc.teamcode}
     * @throws IllegalArgumentException if a package has no compiled classes on the class path
     *                                  (usually a spelling mistake in the package name)
     */
    public static List<Entry> find(List<String> packages) {
        Scan scan = scan(packages);
        for (LeftOut l : scan.leftOut()) {
            System.err.println("Harness: " + l.className() + " is marked as an OpMode but couldn't be loaded, so it"
                    + " is left out of the OpMode list: " + l.reason());
        }
        return scan.opModes();
    }

    /**
     * Like {@link #find(List)}, but also says which annotated OpModes couldn't be loaded
     * ({@link Scan#leftOut()}), so a menu can show them instead of a warning in the output.
     */
    public static Scan scan(List<String> packages) {
        return scan(packages, OpModeCatalog.class.getClassLoader());
    }

    /** {@link #scan(List)} with a chosen class loader (tests use one that hides a class). */
    static Scan scan(List<String> packages, ClassLoader loader) {
        // Class names in name order, so the result never depends on the order files are listed in.
        TreeSet<String> classNames = new TreeSet<>();
        for (String packageName : packages) {
            List<String> found = classesIn(packageName, loader);
            if (found.isEmpty()) {
                throw new IllegalArgumentException("Found no compiled classes in package \"" + packageName
                        + "\". Check the package name (for example in teamCode.opModePackages in harness.properties).");
            }
            classNames.addAll(found);
        }

        List<Entry> entries = new ArrayList<>();
        List<LeftOut> leftOut = new ArrayList<>();
        for (String className : classNames) {
            Entry entry = entryFor(className, loader, leftOut);
            if (entry != null) {
                entries.add(entry);
            }
        }
        entries.sort(Comparator.comparing(Entry::name).thenComparing(Entry::className));
        return new Scan(Collections.unmodifiableList(entries), Collections.unmodifiableList(leftOut));
    }

    /**
     * The OpMode entry for one class, or null if it isn't an enabled, annotated OpMode. A class
     * that can't be loaded is skipped, but if its class file is marked as an OpMode it goes into
     * {@code leftOut}.
     */
    private static Entry entryFor(String className, ClassLoader loader, List<LeftOut> leftOut) {
        Class<?> type;
        try {
            // false = don't run the class's static code; reading annotations doesn't need it.
            type = Class.forName(className, false, loader);
        } catch (ClassNotFoundException | LinkageError e) {
            // Usually a helper class that can't load on a laptop, which is fine to skip. An OpMode
            // that can't load is reported instead.
            if (markedAsOpMode(className, loader)) {
                leftOut.add(new LeftOut(className, e.toString()));
            }
            return null;
        }
        TeleOp teleOp = type.getAnnotation(TeleOp.class);
        Autonomous autonomous = type.getAnnotation(Autonomous.class);
        if (teleOp == null && autonomous == null) {
            return null;
        }
        if (type.isAnnotationPresent(Disabled.class)
                || !OpMode.class.isAssignableFrom(type)
                || Modifier.isAbstract(type.getModifiers())) {
            return null;
        }
        if (teleOp != null && autonomous != null) {
            throw new IllegalStateException(className + " is marked both @TeleOp and @Autonomous; an OpMode can only be one.");
        }
        String simpleName = className.substring(className.lastIndexOf('.') + 1).replace('$', '.');
        if (teleOp != null) {
            return new Entry(className, nameOr(teleOp.name(), simpleName), teleOp.group(), MatchPeriod.TELEOP, "");
        }
        return new Entry(className, nameOr(autonomous.name(), simpleName), autonomous.group(),
                MatchPeriod.AUTONOMOUS, autonomous.preselectTeleOp());
    }

    /**
     * True if the class file mentions {@code @TeleOp} or {@code @Autonomous}. Used only for a class
     * that can't be loaded: its annotations can't be read the normal way, but their names are
     * written in the class file as plain text.
     */
    private static boolean markedAsOpMode(String className, ClassLoader loader) {
        try (java.io.InputStream in = loader.getResourceAsStream(className.replace('.', '/') + ".class")) {
            if (in == null) {
                return false;
            }
            String text = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.ISO_8859_1);
            return text.contains("L" + TeleOp.class.getName().replace('.', '/') + ";")
                    || text.contains("L" + Autonomous.class.getName().replace('.', '/') + ";");
        } catch (IOException e) {
            return false;
        }
    }

    /** The annotation's name, or the class's simple name when the annotation leaves it empty. */
    private static String nameOr(String annotationName, String simpleName) {
        return annotationName.trim().isEmpty() ? simpleName : annotationName;
    }

    /**
     * Full names of every compiled class in a package and its sub-packages, from every class path
     * folder or jar that has that package.
     */
    private static List<String> classesIn(String packageName, ClassLoader loader) {
        String folder = packageName.replace('.', '/');
        List<String> classNames = new ArrayList<>();
        try {
            for (URL url : Collections.list(loader.getResources(folder))) {
                if (url.getProtocol().equals("file")) {
                    addFromFolder(Path.of(url.toURI()), packageName, classNames);
                } else if (url.getProtocol().equals("jar")) {
                    addFromJar(((JarURLConnection) url.openConnection()).getJarFileURL(), folder, classNames);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not list the classes in " + packageName, e);
        } catch (URISyntaxException e) {
            throw new IllegalStateException("Could not list the classes in " + packageName, e);
        }
        return classNames;
    }

    /** Adds every .class file under a class path folder, as a class name. */
    private static void addFromFolder(Path packageFolder, String packageName, List<String> classNames) throws IOException {
        try (Stream<Path> files = Files.walk(packageFolder)) {
            files.filter(f -> f.getFileName().toString().endsWith(".class")).forEach(f -> {
                String relative = packageFolder.relativize(f).toString().replace(File.separatorChar, '.');
                classNames.add(packageName + "." + relative.substring(0, relative.length() - ".class".length()));
            });
        }
    }

    /** Adds every .class entry under a folder inside a jar, as a class name. */
    private static void addFromJar(URL jarUrl, String folder, List<String> classNames) throws IOException {
        File jar;
        try {
            jar = new File(jarUrl.toURI());
        } catch (URISyntaxException e) {
            throw new IOException("Bad jar location " + jarUrl, e);
        }
        try (JarFile file = new JarFile(jar)) {
            file.stream()
                    .map(entry -> entry.getName())
                    .filter(name -> name.startsWith(folder + "/") && name.endsWith(".class"))
                    .forEach(name -> classNames.add(name.substring(0, name.length() - ".class".length()).replace('/', '.')));
        }
    }
}

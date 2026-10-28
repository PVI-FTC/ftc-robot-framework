package pvi.harness.runtime;

import com.qualcomm.robotcore.eventloop.opmode.OpMode;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import pvi.harness.runtime.catalogsample.SampleTeleOp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Checks that {@link OpModeCatalog} finds the robot's OpModes by itself (RP8): the same list the
 * Driver Station would show, without anyone typing it in.
 *
 * <p>The {@code catalogsample} package (test code only) holds small sample classes, one for each
 * case: a normal TeleOp, an autonomous, one with no name, a LinearOpMode, a {@code @Disabled}
 * one, an abstract one, and an annotated class that isn't an OpMode.</p>
 */
class OpModeCatalogTest {
    /** Where TeamCode's OpModes live (the robot code compiled into the harness). */
    private static final String TEAM_CODE = "org.firstinspires.ftc.teamcode";
    /** The test-only sample OpModes. */
    private static final String SAMPLES = "pvi.harness.runtime.catalogsample";
    private static final String TEAM_CODE_OPMODES = TEAM_CODE + ".opmodes.";

    /**
     * WHAT: Every annotated OpMode in TeamCode is found, with the name, group and period written
     * in its annotation.
     *
     * <p>HOW: Lists TeamCode's package and looks for each of the robot's OpModes by class name.</p>
     *
     * <p>WHY: The viewer, the Unity bridge and Save as test can then offer every OpMode, and a
     * name typo in a hand-written list can't hide one. Other OpModes added to TeamCode later
     * are fine: this only checks that these are there. Update the list when one is renamed or
     * removed (Pedro 3 removed {@code TeamAPedroTuning} and added {@code KimCode/TeleopV1}).</p>
     */
    @Test
    void findsEveryTeamCodeOpModeWithItsNameGroupAndPeriod() {
        assumePviRepository();
        List<OpModeCatalog.Entry> found = OpModeCatalog.find(List.of(TEAM_CODE));

        List<OpModeCatalog.Entry> expected = List.of(
                teleOp("teleop.TeamATeleOp", "Team A Mecanum Drive", "Team A"),
                teleOp("teleop.TeamBTeleOp", "Team B TeleOp", "Team B"),
                teleOp("teleop.TeamCTeleOp", "Team C TeleOp", "Team C"),
                teleOp("teleop.RumbleDemoTeleOp", "Rumble Demo (Template)", "Testing"),
                teleOp("testing.TeamAAprilTagVisionTestOpMode", "Team A AprilTag Vision Test", "Testing"),
                teleOp("testing.TeamACameraCalibrationCaptureOpMode", "Team A Camera Calibration Capture", "Testing"),
                teleOp("testing.TeamAPedroDiagnostic", "Team A Pedro Diagnostic", "Testing"),
                new OpModeCatalog.Entry(TEAM_CODE + ".KimCode.TeleopV1", "KimCode TeleopV1", "KimCode",
                        MatchPeriod.TELEOP, ""),
                autonomous("autonomous.TeamAAutoOpMode", "Team A Autonomous Demo", "Team A"),
                autonomous("autonomous.TeamAPedroPilotAutoOpMode", "Team A Pedro Paths", "Team A Testing"));
        List<OpModeCatalog.Entry> missing = expected.stream().filter(e -> !found.contains(e)).toList();
        assertEquals(List.of(), missing, "OpModes missing or listed differently. Found:\n  "
                + found.stream().map(OpModeCatalog.Entry::toString).collect(Collectors.joining("\n  ")));
    }

    /**
     * WHAT: With the harness's settings file, the catalog lists TeamCode's OpModes.
     *
     * <p>HOW: Reads {@code harness.properties} ({@code teamCode.opModePackages}) and looks for
     * Team A's TeleOp.</p>
     *
     * <p>WHY: In another team's repository, only that setting changes; no code does.</p>
     */
    @Test
    void theSettingsFileSaysWhichPackagesToSearch() {
        assumePviRepository();
        List<OpModeCatalog.Entry> found = OpModeCatalog.fromConfig(HarnessConfig.load());

        assertTrue(found.contains(teleOp("teleop.TeamATeleOp", "Team A Mecanum Drive", "Team A")),
                "Found: " + found);
    }

    /**
     * WHAT: An OpMode added to the code shows up without editing any list.
     *
     * <p>HOW: Lists the test-only sample package, which no list anywhere mentions.</p>
     *
     * <p>WHY: This is the point of RP8: a team adds an OpMode and the harness sees it.</p>
     */
    @Test
    void aNewOpModeShowsUpWithoutEditingAnyList() {
        List<OpModeCatalog.Entry> found = OpModeCatalog.find(List.of(SAMPLES));

        assertTrue(found.contains(new OpModeCatalog.Entry(SampleTeleOp.class.getName(),
                "Sample TeleOp", "Samples", MatchPeriod.TELEOP, "")), "Found: " + found);
    }

    /**
     * WHAT: {@code @Disabled} OpModes are left out, as on the Driver Station.
     *
     * <p>WHY: Teams disable unfinished or old OpModes; offering them would be confusing.</p>
     */
    @Test
    void disabledOpModesAreLeftOut() {
        assertEquals(List.of(), names(SAMPLES).stream().filter(n -> n.contains("Disabled")).toList());
    }

    /**
     * WHAT: Abstract classes and annotated classes that aren't OpModes are left out.
     *
     * <p>WHY: Neither can be run, so listing them would only lead to a crash later.</p>
     */
    @Test
    void abstractClassesAndNonOpModesAreLeftOut() {
        List<String> names = names(SAMPLES);
        assertFalse(names.contains("Sample Abstract"), "Found: " + names);
        assertFalse(names.contains("Sample Not An OpMode"), "Found: " + names);
    }

    /**
     * WHAT: The full sample list is exactly the four runnable, enabled OpModes, sorted by name.
     *
     * <p>WHY: Checks nothing extra sneaks in, and that the order is the same every run.</p>
     */
    @Test
    void listsExactlyTheEnabledOpModesSortedByName() {
        assertEquals(List.of("Sample Autonomous", "Sample Linear", "Sample TeleOp", "SampleUnnamedTeleOp"),
                names(SAMPLES));
    }

    /**
     * WHAT: An OpMode whose annotation has no name gets its class name, as the SDK does.
     */
    @Test
    void anOpModeWithNoNameIsListedByItsClassName() {
        OpModeCatalog.Entry entry = entry(SAMPLES, "SampleUnnamedTeleOp");

        assertEquals("", entry.group());
        assertEquals(MatchPeriod.TELEOP, entry.period());
    }

    /**
     * WHAT: An autonomous keeps its period and the TeleOp it preselects.
     *
     * <p>WHY: Running an autonomous for the TeleOp's 2 minutes (or the other way round) would
     * give a wrong test.</p>
     */
    @Test
    void anAutonomousKeepsItsPeriodAndPreselectedTeleOp() {
        OpModeCatalog.Entry entry = entry(SAMPLES, "Sample Autonomous");

        assertEquals(MatchPeriod.AUTONOMOUS, entry.period());
        assertEquals("Sample TeleOp", entry.preselectTeleOp());
    }

    /**
     * WHAT: A listed OpMode can be created and run, like pressing INIT on the Driver Station.
     */
    @Test
    void aListedOpModeCanBeCreated() {
        assumePviRepository();
        OpMode opMode = entry(TEAM_CODE, "Team A Mecanum Drive").newOpMode();

        assertEquals(TEAM_CODE_OPMODES + "teleop.TeamATeleOp", opMode.getClass().getName());
    }

    /**
     * WHAT: A package with no classes (usually a typo) fails with a message naming it, instead of
     * quietly finding nothing.
     */
    @Test
    void aMistypedPackageFailsWithItsName() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
                () -> OpModeCatalog.find(List.of("org.firstinspires.ftc.teamcod")));

        assertTrue(error.getMessage().contains("\"org.firstinspires.ftc.teamcod\""), error.getMessage());
    }

    // ------------------------------------------------------------------------------------------

    /** These checks name PVI's own OpModes, so they only run in PVI's repository (pvi.extras=true). */
    private static void assumePviRepository() {
        assumeTrue(HarnessConfig.load().pviExtras(), "checks PVI's own OpModes; only with pvi.extras=true");
    }

    private static OpModeCatalog.Entry teleOp(String classInOpModes, String name, String group) {
        return new OpModeCatalog.Entry(TEAM_CODE_OPMODES + classInOpModes, name, group, MatchPeriod.TELEOP, "");
    }

    private static OpModeCatalog.Entry autonomous(String classInOpModes, String name, String group) {
        return new OpModeCatalog.Entry(TEAM_CODE_OPMODES + classInOpModes, name, group, MatchPeriod.AUTONOMOUS, "");
    }

    /** Driver Station names of everything found in a package, in the catalog's order. */
    private static List<String> names(String packageName) {
        return OpModeCatalog.find(List.of(packageName)).stream().map(OpModeCatalog.Entry::name).toList();
    }

    /** The one entry with this Driver Station name. */
    private static OpModeCatalog.Entry entry(String packageName, String name) {
        return OpModeCatalog.find(List.of(packageName)).stream()
                .filter(e -> e.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No OpMode named \"" + name + "\" in " + packageName));
    }
}

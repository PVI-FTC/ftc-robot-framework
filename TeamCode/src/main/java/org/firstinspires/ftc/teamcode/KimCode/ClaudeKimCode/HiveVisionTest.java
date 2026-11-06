package org.firstinspires.ftc.teamcode.KimCode.ClaudeKimCode;

import com.pedropathing.math.Pose;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.core.input.InputManager;

/**
 * Checks what the Limelight can see of the HIVE. The robot does not drive.
 *
 * <p>Put the robot by hand on the spot set by TEST_X_INCHES and TEST_Y_INCHES, facing the HIVE, and
 * read the telemetry. Press A for Red or B for Blue. Use this to find a viewing spot where the
 * tags can be read, then copy that spot into {@link HiveField}.</p>
 */
@TeleOp(name = "Hive Vision Test", group = "KimCode Testing")
public final class HiveVisionTest extends OpMode {
    /** Where you put the robot, written for Red. Change these to try another spot. */
    private static final double TEST_X_INCHES = HiveField.VIEW_X_INCHES;
    private static final double TEST_Y_INCHES = HiveField.VIEW_Y_INCHES;

    private HiveField.Alliance alliance = HiveField.Alliance.RED;
    private KimHiveVision vision;
    private InputManager input;

    @Override
    public void init() {
        vision = new KimHiveVision(hardwareMap, alliance);
        input = new InputManager(gamepad1);
        vision.start();
    }

    @Override
    public void loop() {
        input.update();
        if (input.wasAJustPressed()) {
            selectAlliance(HiveField.Alliance.RED);
        } else if (input.wasBJustPressed()) {
            selectAlliance(HiveField.Alliance.BLUE);
        }

        Pose robot = HiveField.pose(alliance, TEST_X_INCHES, TEST_Y_INCHES,
                HiveField.FACING_HIVE_DEGREES);
        vision.update(robot);

        telemetry.addData("Limelight found", vision.isAvailable());
        telemetry.addData("Alliance (A red, B blue)", alliance);
        addCell("Start-raised CELL", true, vision.startRaisedCell(), robot);
        addCell("Start-lowered CELL", false, vision.startLoweredCell(), robot);
        telemetry.addData("Answer", vision.finder().answer());
        telemetry.addData("Votes (start tilt / tipped)", "%d / %d",
                vision.finder().startTiltVotes(), vision.finder().tippedVotes());
        telemetry.update();
    }

    @Override
    public void stop() {
        if (vision != null) {
            vision.stop();
        }
    }

    private void selectAlliance(HiveField.Alliance newAlliance) {
        alliance = newAlliance;
        vision.setAlliance(newAlliance);
    }

    private void addCell(String name, boolean isStartRaisedCell, KimHiveVision.CellView cell,
            Pose robot) {
        if (!cell.isVisible()) {
            telemetry.addData(name, "no tags");
            return;
        }
        double distance = vision.distanceToCellInches(isStartRaisedCell, robot);
        telemetry.addData(name, "%d tags, tx %.1f, ty %.1f, height %.1f in", cell.tagCount(),
                cell.targetXDegrees(), cell.targetYDegrees(),
                HiveCellFinder.tagHeightInches(distance, cell.targetYDegrees()));
    }
}

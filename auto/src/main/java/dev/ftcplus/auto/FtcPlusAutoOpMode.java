package dev.ftcplus.auto;

import com.pedropathing.follower.Follower;
import com.pedropathing.follower.FollowerConstants;
import com.pedropathing.localization.Localizer;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;

import com.qualcomm.robotcore.hardware.HardwareMap;
import dev.ftcplus.auto.pedro.FtcPlusMecanumDrivetrain;
import dev.ftcplus.core.Component;
import dev.ftcplus.core.Robot;
import dev.ftcplus.core.Runtime;
import dev.ftcplus.drivetrains.MecanumDrive;
import dev.ftcplus.ftcruntime.FtcDeviceFactory;
import dev.ftcplus.ftcruntime.FtcTelemetryProvider;
import dev.ftcplus.ftcruntime.RobotResolver;
import dev.ftcplus.ftcruntime.menu.GamepadMenuInputSource;
import dev.ftcplus.ftcruntime.menu.TelemetryMenu;
import dev.ftcplus.core.menu.MenuHost;
import dev.ftcplus.core.menu.MenuItem;
import dev.ftcplus.limelight.Limelight;

import java.util.List;

public abstract class FtcPlusAutoOpMode extends LinearOpMode {

    protected abstract List<AutoPath> paths();
    protected abstract Localizer localizer();
    protected abstract FollowerConstants followerConstants();

    @Override
    public final void runOpMode() {
        List<AutoPath> availablePaths = paths();

        if (availablePaths == null || availablePaths.isEmpty()) {
            telemetry.addLine("No auto paths defined.");
            telemetry.update();
            waitForStart();
            return;
        }

        AutoPath selected = availablePaths.size() == 1
                ? availablePaths.get(0)
                : pickPath(availablePaths);

        if (selected == null) return;

        Robot<?, ?, ?> robot = RobotResolver.resolve(RobotResolver.findRobotClasses());
        Runtime runtime = new Runtime(robot, new FtcDeviceFactory(hardwareMap), new FtcTelemetryProvider(telemetry));
        runtime.initialize();
        injectLimelights(robot, hardwareMap);

        MecanumDrive drive = findMecanumDrive(robot);
        if (drive == null) throw new IllegalStateException(
            "No MecanumDrive registered on robot — FtcPlusAutoOpMode requires a MecanumDrive subsystem"
        );

        FtcPlusMecanumDrivetrain drivetrain = new FtcPlusMecanumDrivetrain(
            drive.frontLeft(), drive.frontRight(),
            drive.backLeft(),  drive.backRight()
        );

        Follower follower = new Follower(followerConstants(), localizer(), drivetrain);

        AutoSession session = new AutoSession(robot, runtime, follower, selected);

        telemetry.addLine("Ready — " + selected.name());
        telemetry.update();

        waitForStart();
        runtime.start();

        while (opModeIsActive() && !session.isFinished()) {
            session.update();
        }

        runtime.stop();
    }

    private MecanumDrive findMecanumDrive(Robot<?, ?, ?> robot) {
        for (dev.ftcplus.core.Component child : robot.children()) {
            if (child instanceof MecanumDrive) return (MecanumDrive) child;
        }
        return null;
    }

    private AutoPath pickPath(List<AutoPath> paths) {
        MenuHost host = new MenuHost();
        TelemetryMenu menu = new TelemetryMenu("Select Auto", telemetry);
        final AutoPath[] selected = {null};

        for (AutoPath path : paths) {
            menu.addItem(MenuItem.action(path.name(), () -> {
                selected[0] = path;
                host.back();
            }));
        }

        host.setRoot(menu);
        GamepadMenuInputSource input = new GamepadMenuInputSource(gamepad1, gamepad2);

        while (!isStarted() && !isStopRequested() && host.isActive()) {
            input.update();
            host.update(input);
        }

        return selected[0];
    }

    private void injectLimelights(Component component, HardwareMap hwMap) {
        if (component instanceof Limelight) {
            Limelight ll = (Limelight) component;
            try {
                Limelight3A sdk = hwMap.get(Limelight3A.class, ll.entry().hardwareName());
                ll.attachLimelight(sdk);
            } catch (Exception e) {
                telemetry.addLine("Warning: Limelight '" + ll.entry().hardwareName() + "' not found in config.");
            }
            for (Component child : component.children()) {
                injectLimelights(child, hwMap);
            }
        }
    }
}
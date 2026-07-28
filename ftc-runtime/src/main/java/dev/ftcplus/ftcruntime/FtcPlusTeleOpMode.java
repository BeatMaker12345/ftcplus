package dev.ftcplus.ftcruntime;

import android.content.Context;
import android.content.SharedPreferences;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.HardwareMap;
import dev.ftcplus.core.Component;
import dev.ftcplus.core.Robot;
import dev.ftcplus.core.Runtime;
import dev.ftcplus.ftcruntime.controls.Controls;
import dev.ftcplus.limelight.Limelight;

import java.util.List;

public abstract class FtcPlusTeleOpMode extends LinearOpMode {
    private Runtime runtime;
    private Controls controls;

    @Override
    public final void runOpMode() {
        Robot robot = resolveRobot();
        runtime = new Runtime(robot, new FtcDeviceFactory(hardwareMap), new FtcTelemetryProvider(telemetry));
        controls = new Controls(gamepad1, gamepad2, runtime.signalBus());
        runtime.robot().attachGamepadFeedback(controls);

        configure();

        runtime.initialize();
        injectLimelights(robot, hardwareMap);
        waitForStart();

        if (isStopRequested()) return;

        runtime.start();

        while (opModeIsActive()) {
            controls.update();
            runtime.update();
        }

        runtime.stop();
    }

    protected void configure() {}

    protected final Runtime runtime() {
        return runtime;
    }

    protected final Object controls() {
        return controls;
    }


    private Robot<?, ?, ?> resolveRobot() {
        List<Class<?>> classes = RobotResolver.findRobotClasses();
        SharedPreferences prefs = hardwareMap.appContext
                .getSharedPreferences("ftcplus_settings", Context.MODE_PRIVATE);
        return RobotResolver.resolveFromPrefs(classes, prefs, getClass());
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
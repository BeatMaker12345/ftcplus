package dev.ftcplus.ftcruntime;

import android.content.Context;
import android.content.SharedPreferences;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.HardwareMap;
import dev.ftcplus.core.Component;
import dev.ftcplus.core.Robot;
import dev.ftcplus.core.Runtime;
import dev.ftcplus.limelight.Limelight;
import dev.ftcplus.runtime.AdvancedOpMode;
import dev.ftcplus.runtime.OpMode;
import dev.ftcplus.runtime.controls.OpModeControls;

import java.util.List;

public abstract class FtcPlusTeleOpMode extends LinearOpMode {
    private final OpMode opMode;

    protected FtcPlusTeleOpMode(OpMode opMode) {
        this.opMode = opMode;
    }

    @Override
    public void runOpMode() {
        Robot<?, ?, ?> robot = resolveRobot();
        FtcGamepadState gamepadState = new FtcGamepadState(gamepad1, gamepad2);
        Runtime runtime = new Runtime(robot, new FtcDeviceFactory(hardwareMap), new FtcTelemetryProvider(telemetry));
        OpModeControls controls = new OpModeControls(runtime.signalBus(), gamepadState, robot);
        robot.attachGamepadFeedback(controls);

        opMode.attachRuntime(runtime, controls);

        if (opMode instanceof AdvancedOpMode adv) adv.onInit();

        runtime.initialize();
        injectLimelights(robot, hardwareMap);

        while (!isStarted() && !isStopRequested()) {
            if (opMode instanceof AdvancedOpMode adv) adv.initLoop();
        }
        if (isStopRequested()) return;

        runtime.start();
        if (opMode instanceof AdvancedOpMode adv) adv.onRun();

        while (opModeIsActive()) {
            controls.update();
            runtime.update();
            if (opMode instanceof AdvancedOpMode adv) adv.runLoop();
        }

        if (opMode instanceof AdvancedOpMode adv) adv.onStop();
        runtime.stop();
    }

    private Robot<?, ?, ?> resolveRobot() {
        List<Class<?>> classes = dev.ftcplus.runtime.RobotResolver.findRobotClasses();
        SharedPreferences prefs = hardwareMap.appContext
                .getSharedPreferences("ftcplus_settings", Context.MODE_PRIVATE);
        return FtcRobotResolver.resolveFromPrefs(classes, prefs, opMode.getClass());
    }

    private void injectLimelights(Component component, HardwareMap hwMap) {
        if (component instanceof Limelight ll) {
            try {
                Limelight3A sdk = hwMap.get(Limelight3A.class, ll.entry().hardwareName());
                ll.attachLimelight(sdk);
            } catch (Exception e) {
                telemetry.addLine("Warning: Limelight '" + ll.entry().hardwareName() + "' not found.");
            }
        }
        for (Component child : component.children()) {
            injectLimelights(child, hwMap);
        }
    }
}
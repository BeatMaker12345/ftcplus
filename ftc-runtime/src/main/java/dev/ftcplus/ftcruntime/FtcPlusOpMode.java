package dev.ftcplus.ftcruntime;

import android.content.Context;
import android.content.SharedPreferences;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import dev.ftcplus.core.Robot;
import dev.ftcplus.core.Runtime;
import dev.ftcplus.core.UseRobot;
import dev.ftcplus.ftcruntime.controls.Controls;

import java.util.ArrayList;
import java.util.List;

public abstract class FtcPlusOpMode extends LinearOpMode {
    private Runtime runtime;
    private Controls controls;

    @Override
    public final void runOpMode() {
        Robot robot = resolveRobot();
        runtime = new Runtime(robot, new FtcDeviceFactory(hardwareMap));
        controls = new Controls(gamepad1, gamepad2, runtime.signalBus());
        runtime.robot().attachGamepadFeedback(controls);

        configure();

        runtime.initialize();
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


    private Robot<?, ?> resolveRobot() {
        List<Class<?>> classes = RobotResolver.findRobotClasses();
        SharedPreferences prefs = hardwareMap.appContext
                .getSharedPreferences("ftcplus_settings", Context.MODE_PRIVATE);
        return RobotResolver.resolveFromPrefs(classes, prefs, getClass());
    }
}

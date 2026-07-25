package dev.ftcplus.ftcruntime;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import dev.ftcplus.core.Robot;
import dev.ftcplus.core.Runtime;

public abstract class FtcPlusAdvancedOpMode extends LinearOpMode {
    private Runtime runtime;

    protected final Runtime createRuntime(Robot robot) {
        runtime = new Runtime(robot, new FtcDeviceFactory(hardwareMap));
        return runtime;
    }

    protected final Runtime runtime() {
        return runtime;
    }
}
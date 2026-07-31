package dev.ftcplus.sim.runner;

import dev.ftcplus.core.Robot;
import dev.ftcplus.core.Runtime;
import dev.ftcplus.runtime.AdvancedOpMode;
import dev.ftcplus.runtime.OpMode;
import dev.ftcplus.runtime.RobotResolver;
import dev.ftcplus.runtime.controls.OpModeControls;
import dev.ftcplus.sim.devices.SimDeviceFactory;
import dev.ftcplus.sim.gamepad.SimGamepadState;
import dev.ftcplus.sim.telemetry.SimTelemetryProvider;

import java.util.List;

public final class SimOpModeRunner {

    private final OpMode opMode;
    private final SimDeviceFactory deviceFactory;
    private final SimGamepadState  gamepadState;
    private final SimTelemetryProvider telemetry;

    private Robot<?, ?, ?> robot;
    private Runtime runtime;
    private OpModeControls controls;

    private volatile boolean running = false;
    private int loopHz = 50;

    public SimOpModeRunner(OpMode opMode) {
        this.opMode        = opMode;
        this.deviceFactory = new SimDeviceFactory();
        this.gamepadState  = new SimGamepadState();
        this.telemetry     = new SimTelemetryProvider();
    }

    public SimOpModeRunner(OpMode opMode, Robot<?, ?, ?> robot) {
        this.opMode        = opMode;
        this.robot         = robot;
        this.deviceFactory = new SimDeviceFactory();
        this.gamepadState  = new SimGamepadState();
        this.telemetry     = new SimTelemetryProvider();
    }

    public SimOpModeRunner(OpMode opMode, boolean verboseTelemetry) {
        this.opMode        = opMode;
        this.deviceFactory = new SimDeviceFactory();
        this.gamepadState  = new SimGamepadState();
        this.telemetry     = new SimTelemetryProvider(verboseTelemetry);
    }


    public void init() {
        if (robot == null) {
            List<Class<?>> classes = RobotResolver.findRobotClasses();
            robot = RobotResolver.resolve(classes, opMode.getClass());
        }
        runtime  = new Runtime(robot, deviceFactory, telemetry);
        controls = new OpModeControls(runtime.signalBus(), gamepadState, robot);
        robot.attachGamepadFeedback(controls);

        opMode.attachRuntime(runtime, controls);

        if (opMode instanceof AdvancedOpMode adv) adv.onInit();

        runtime.initialize();
    }

    public void start() {
        runtime.start();
        if (opMode instanceof AdvancedOpMode adv) adv.onRun();
    }

    public void loop(int loops) {
        for (int i = 0; i < loops; i++) {
            tick();
        }
    }

    public void tick() {
        deviceFactory.update();
        controls.update();
        runtime.update();
        if (opMode instanceof AdvancedOpMode adv) adv.runLoop();
    }

    public void run() {
        running = true;
        long periodNs = 1_000_000_000L / loopHz;
        while (running) {
            long start = System.nanoTime();
            tick();
            long elapsed = System.nanoTime() - start;
            long sleep = (periodNs - elapsed) / 1_000_000;
            if (sleep > 0) {
                try { Thread.sleep(sleep); } catch (InterruptedException e) { break; }
            }
        }
    }

    public void stop() {
        running = false;
        if (opMode instanceof AdvancedOpMode adv) adv.onStop();
        runtime.stop();
    }


    public Robot<?, ?, ?>    robot()         { return robot; }
    public Runtime           runtime()       { return runtime; }
    public SimGamepadState   gamepad()       { return gamepadState; }
    public SimDeviceFactory  deviceFactory() { return deviceFactory; }

    public void setLoopHz(int hz) { this.loopHz = hz; }
}

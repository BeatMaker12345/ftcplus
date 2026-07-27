package dev.ftcplus.core;

import dev.ftcplus.core.signal.SignalBus;
import dev.ftcplus.core.telemetry.TelemetryProvider;

import java.util.Objects;

public final class Runtime {
    private final Robot<?, ?> robot;
    private final TelemetryProvider telemetry;

    private LifecycleState state = LifecycleState.CREATED;

    private final SignalBus signalBus = new SignalBus();

    public Runtime(Robot<?, ?> robot, DeviceFactory deviceFactory, TelemetryProvider telemetry) {
        this.robot = Objects.requireNonNull(robot, "robot");
        this.telemetry = Objects.requireNonNull(telemetry, "telemetry");
        this.robot.resolveIdentityInternal();
        this.robot.attachBus(signalBus);
        this.robot.attachDeviceFactory(
                Objects.requireNonNull(deviceFactory, "deviceFactory")
        );
        this.robot.attachTelemetry(telemetry);
    }

    public Robot<?, ?> robot() {
        return robot;
    }

    public LifecycleState state() {
        return state;
    }

    public void initialize() {
        requireState(LifecycleState.CREATED);

        robot.initializeInternal();
        robot.defineTelemetry();
        state = LifecycleState.INITIALIZED;
    }

    public void start() {
        requireState(LifecycleState.INITIALIZED);

        robot.startInternal();
        state = LifecycleState.STARTED;
    }

    public void update() {
        requireState(LifecycleState.STARTED);

        robot.updateInternal();
        signalBus.flush();
        telemetry.update();
    }

    public void stop() {
        requireState(LifecycleState.STARTED);

        robot.stopInternal();
        state = LifecycleState.STOPPED;
    }

    private void requireState(LifecycleState expected) {
        if (state != expected) {
            throw new IllegalStateException(
                    "Expected " + expected + ", but runtime was " + state
            );
        }
    }

    public SignalBus signalBus() {
        return signalBus;
    }
}
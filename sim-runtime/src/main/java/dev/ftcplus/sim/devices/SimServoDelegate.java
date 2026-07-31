package dev.ftcplus.sim.devices;

import dev.ftcplus.core.servo.ServoDelegate;
import dev.ftcplus.core.servo.ServoSpec;
import dev.ftcplus.sim.physics.ServoPhysics;

public final class SimServoDelegate implements ServoDelegate {

    private final ServoPhysics physics;
    private dev.ftcplus.core.Direction direction = dev.ftcplus.core.Direction.FORWARD;
    private double rangeMin = 0.0, rangeMax = 1.0;

    public SimServoDelegate(ServoSpec spec) {
        this.physics = new ServoPhysics(spec);
    }

    @Override
    public void setPosition(double pos) {
        double scaled = rangeMin + pos * (rangeMax - rangeMin);
        if (direction == dev.ftcplus.core.Direction.REVERSE) scaled = rangeMax - pos * (rangeMax - rangeMin);
        physics.setPosition(scaled);
    }
    @Override public double getPosition()            { return physics.position(); }
    @Override public void setDirection(dev.ftcplus.core.Direction direction) { this.direction = direction; }
    @Override public dev.ftcplus.core.Direction getDirection() { return direction; }
    @Override
    public void scaleRange(double min, double max) {
        this.rangeMin = min;
        this.rangeMax = max;
    }

    @Override public String getDeviceName() { return "SimServo"; }

    public void update() { physics.update(); }
}

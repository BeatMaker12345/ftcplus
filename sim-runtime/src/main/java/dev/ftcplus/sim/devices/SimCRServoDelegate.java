package dev.ftcplus.sim.devices;

import dev.ftcplus.core.servo.CRServoDelegate;
import dev.ftcplus.core.servo.CRServoSpec;
import dev.ftcplus.sim.physics.CRServoPhysics;

public final class SimCRServoDelegate implements CRServoDelegate {

    private final CRServoPhysics physics;
    private dev.ftcplus.core.Direction direction = dev.ftcplus.core.Direction.FORWARD;

    public SimCRServoDelegate(CRServoSpec spec) {
        this.physics = new CRServoPhysics();
    }

    @Override public void   setPower(double power) { physics.setPower(direction == dev.ftcplus.core.Direction.REVERSE ? -power : power); }
    @Override public double getPower()             { return physics.power(); }
    @Override public void setDirection(dev.ftcplus.core.Direction direction) { this.direction = direction; }
    @Override public dev.ftcplus.core.Direction getDirection() { return direction; }
    @Override public String getDeviceName() { return "SimCRServo"; }

    public void update() { physics.update(); }
}
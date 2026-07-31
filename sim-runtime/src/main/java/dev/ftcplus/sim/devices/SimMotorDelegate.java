package dev.ftcplus.sim.devices;

import dev.ftcplus.core.Direction;
import dev.ftcplus.core.motor.MotorDelegate;
import dev.ftcplus.core.motor.RunMode;
import dev.ftcplus.core.motor.ZeroPowerBehavior;
import dev.ftcplus.core.motor.MotorSpec;
import dev.ftcplus.sim.physics.MotorPhysics;

public final class SimMotorDelegate implements MotorDelegate {

    private final MotorPhysics physics;
    private RunMode mode = RunMode.RUN_WITHOUT_ENCODER;
    private ZeroPowerBehavior zeroPowerBehavior = ZeroPowerBehavior.FLOAT;
    private Direction direction = Direction.FORWARD;
    private int targetPosition = 0;
    private double power = 0;

    public SimMotorDelegate(MotorSpec spec) {
        this.physics = new MotorPhysics(spec);
    }

    @Override
    public void setPower(double power) {
        this.power = direction == Direction.REVERSE ? -power : power;
        if (mode == RunMode.RUN_TO_POSITION) {
            double error = targetPosition - physics.position();
            this.power = Math.max(-1, Math.min(1, error / 100.0)) * Math.abs(power);
        }
        physics.setPower(this.power);
    }

    @Override public double getPower()           { return power; }
    @Override public int getCurrentPosition()    { return (int) physics.position(); }
    public double getVelocity()        { return physics.velocity(); }
    @Override public void setTargetPosition(int pos) { this.targetPosition = pos; }
    @Override public int getTargetPosition()     { return targetPosition; }
    @Override public boolean isBusy()            { return Math.abs(physics.position() - targetPosition) >= 5; }
    @Override public void setMode(RunMode mode)  {
        this.mode = mode;
        if (mode == RunMode.STOP_AND_RESET_ENCODER) physics.setPosition(0);
    }
    @Override public RunMode getMode()           { return mode; }
    @Override public void setZeroPowerBehavior(ZeroPowerBehavior b) { this.zeroPowerBehavior = b; }
    @Override public ZeroPowerBehavior getZeroPowerBehavior()       { return zeroPowerBehavior; }

    @Override
    public boolean getPowerFloat() {
        return zeroPowerBehavior == ZeroPowerBehavior.FLOAT && power == 0;
    }

    @Override
    public String getDeviceName() {
        return "SimMotor";
    }

    @Override public void setDirection(Direction d) { this.direction = d; }
    @Override public Direction getDirection()    { return direction; }
    public double estimateCurrentDraw() { return physics.estimatedCurrentAmps(); }

    public void update() { physics.update(); }
}

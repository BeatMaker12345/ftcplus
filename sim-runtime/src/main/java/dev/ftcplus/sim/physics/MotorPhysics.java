package dev.ftcplus.sim.physics;

import dev.ftcplus.core.motor.MotorSpec;

public final class MotorPhysics {
    private final MotorSpec spec;
    private double power = 0;
    private double velocity = 0;
    private double position = 0;
    private double load = 0;
    private long lastUpdateNs = System.nanoTime();

    public MotorPhysics(MotorSpec spec) { this.spec = spec; }

    public void setPower(double power) { this.power = Math.max(-1, Math.min(1, power)); }
    public void setLoad(double load)   { this.load  = Math.max(0, Math.min(1, load)); }

    public void update() {
        long now = System.nanoTime();
        double dt = (now - lastUpdateNs) / 1e9;
        lastUpdateNs = now;

        if (spec != null) {
            double maxV = spec.freeSpeedRpm * spec.ticksPerRevolution / 60.0;
            double targetV = power * maxV * (1 - load * 0.8);
            double tau = 0.1;
            velocity += (targetV - velocity) * (dt / tau);
        } else {
            velocity = power * 1000;
        }
        position += velocity * dt;
    }

    public double velocity()  { return velocity; }
    public double position()  { return position; }
    public void setPosition(double pos) { this.position = pos; }

    public double estimatedCurrentAmps() {
        if (spec == null) return Math.abs(power) * 3.0;
        return spec.freeCurrentAmps + (spec.stallCurrentAmps - spec.freeCurrentAmps) * Math.abs(power);
    }
}

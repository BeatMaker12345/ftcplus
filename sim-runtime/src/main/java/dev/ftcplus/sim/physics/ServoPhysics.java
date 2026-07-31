package dev.ftcplus.sim.physics;

import dev.ftcplus.core.servo.ServoSpec;

public final class ServoPhysics {
    private final ServoSpec spec;
    private double position = 0.5;
    private double targetPosition = 0.5;
    private long lastUpdateNs = System.nanoTime();

    public ServoPhysics(ServoSpec spec) { this.spec = spec; }

    public void setPosition(double pos) { this.targetPosition = Math.max(0, Math.min(1, pos)); }

    public void update() {
        long now = System.nanoTime();
        double dt = (now - lastUpdateNs) / 1e9;
        lastUpdateNs = now;
        double travelDeg = spec != null ? spec.travelDegrees : 180.0;
        double speed = 600.0 / travelDeg;
        double delta = targetPosition - position;
        double maxDelta = speed * dt;
        if (Math.abs(delta) <= maxDelta) position = targetPosition;
        else position += Math.signum(delta) * maxDelta;
    }

    public double position() { return position; }
}

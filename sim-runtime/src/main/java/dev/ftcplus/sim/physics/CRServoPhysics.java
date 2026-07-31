package dev.ftcplus.sim.physics;

public final class CRServoPhysics {
    private double power = 0;
    private double position = 0;
    private long lastUpdateNs = System.nanoTime();

    public void setPower(double power) { this.power = Math.max(-1, Math.min(1, power)); }

    public void update() {
        long now = System.nanoTime();
        double dt = (now - lastUpdateNs) / 1e9;
        lastUpdateNs = now;
        position += power * 360.0 * dt;
    }

    public double power()    { return power; }
    public double position() { return position; }
}

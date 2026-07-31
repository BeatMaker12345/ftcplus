package dev.ftcplus.sim.devices;

import dev.ftcplus.core.sensor.ImuDelegate;

public final class SimImuDelegate implements ImuDelegate {

    private double yawDegrees   = 0;
    private double pitchDegrees = 0;
    private double rollDegrees  = 0;

    @Override public double getYaw()   { return yawDegrees; }
    @Override public double getPitch() { return pitchDegrees; }
    @Override public double getRoll()  { return rollDegrees; }
    @Override public void resetYaw()   { yawDegrees = 0; }

    public void addYaw(double degrees) { yawDegrees += degrees; }
    public void setYaw(double degrees) { yawDegrees = degrees; }
}

package dev.ftcplus.core.sensor;

public interface ImuDelegate {
    double getYaw();
    double getPitch();
    double getRoll();
    void resetYaw();
}
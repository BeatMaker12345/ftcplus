package dev.ftcplus.core.motor;

import dev.ftcplus.core.Direction;

public interface MotorDelegate {
    void setPower(double power);
    double getPower();
    void setDirection(Direction direction);
    Direction getDirection();
    void setMode(RunMode mode);
    RunMode getMode();
    void setTargetPosition(int ticks);
    int getTargetPosition();
    int getCurrentPosition();
    boolean isBusy();
    void setZeroPowerBehavior(ZeroPowerBehavior behavior);
    ZeroPowerBehavior getZeroPowerBehavior();
    boolean getPowerFloat();
    String getDeviceName();
}
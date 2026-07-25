package dev.ftcplus.core.servo;

import dev.ftcplus.core.Direction;

public interface CRServoDelegate {
    void setPower(double power);
    double getPower();
    void setDirection(Direction direction);
    Direction getDirection();
    String getDeviceName();
}
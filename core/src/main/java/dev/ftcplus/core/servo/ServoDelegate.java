package dev.ftcplus.core.servo;

import dev.ftcplus.core.Direction;

public interface ServoDelegate {
    void setPosition(double position);
    double getPosition();
    void setDirection(Direction direction);
    Direction getDirection();
    void scaleRange(double min, double max);
    String getDeviceName();
}
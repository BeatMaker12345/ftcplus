package dev.ftcplus.core;

public interface GamepadFeedback {
    void vibrate(GamepadSide side, int milliseconds);
    void setLed(GamepadSide side, double r, double g, double b, int durationMs);
    double axisValue(GamepadAxis axis);
}
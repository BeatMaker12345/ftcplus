package dev.ftcplus.runtime.controls;

import dev.ftcplus.core.GamepadAxis;
import dev.ftcplus.core.GamepadButton;
import dev.ftcplus.core.GamepadSide;

public interface GamepadState {
    double  axisValue(GamepadAxis axis);
    boolean buttonValue(GamepadButton button);
    void    vibrate(GamepadSide side, int durationMs);
    void    setLed(GamepadSide side, double r, double g, double b, int durationMs);
}

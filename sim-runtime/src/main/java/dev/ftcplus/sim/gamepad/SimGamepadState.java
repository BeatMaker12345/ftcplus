package dev.ftcplus.sim.gamepad;

import dev.ftcplus.core.GamepadAxis;
import dev.ftcplus.core.GamepadButton;
import dev.ftcplus.runtime.controls.GamepadState;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class SimGamepadState implements GamepadState {

    private final Map<GamepadAxis, Double>   axes    = new ConcurrentHashMap<>();
    private final Map<GamepadButton, Boolean> buttons = new ConcurrentHashMap<>();


    @Override
    public double axisValue(GamepadAxis axis) {
        return axes.getOrDefault(axis, 0.0);
    }

    @Override
    public boolean buttonValue(GamepadButton button) {
        return buttons.getOrDefault(button.resolve(), false);
    }

    @Override
    public void vibrate(dev.ftcplus.core.GamepadSide side, int durationMs) {
        // no-op in sim
    }

    @Override
    public void setLed(dev.ftcplus.core.GamepadSide side, double r, double g, double b, int durationMs) {
        // no-op in sim
    }


    public void setAxis(GamepadAxis axis, double value) {
        axes.put(axis, value);
    }

    public void pressButton(GamepadButton button) {
        buttons.put(button.resolve(), true);
    }

    public void releaseButton(GamepadButton button) {
        buttons.put(button.resolve(), false);
    }

    public void releaseAll() {
        axes.clear();
        buttons.clear();
    }
}

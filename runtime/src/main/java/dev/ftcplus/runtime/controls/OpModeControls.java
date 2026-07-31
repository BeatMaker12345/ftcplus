package dev.ftcplus.runtime.controls;

import dev.ftcplus.core.GamepadAxis;
import dev.ftcplus.core.GamepadButton;
import dev.ftcplus.core.GamepadFeedback;
import dev.ftcplus.core.GamepadSide;
import dev.ftcplus.core.signal.SignalBus;
import dev.ftcplus.core.Drive;

import java.util.ArrayList;
import java.util.List;

public final class OpModeControls implements GamepadFeedback {

    private final SignalBus bus;
    private final GamepadState gamepadState;
    private final dev.ftcplus.core.Robot<?, ?, ?> robot;
    private final List<ControlBinding> bindings = new ArrayList<>();
    private final List<DriveBinding> driveBindings = new ArrayList<>();

    public OpModeControls(SignalBus bus, GamepadState gamepadState, dev.ftcplus.core.Robot<?, ?, ?> robot) {
        this.bus          = bus;
        this.gamepadState = gamepadState;
        this.robot        = robot;
    }


    public ButtonBindingBuilder pressed(GamepadButton button) {
        return new ButtonBindingBuilder(button, ButtonBindingBuilder.Trigger.WHEN_PRESSED, bus, gamepadState, bindings);
    }

    public ButtonBindingBuilder released(GamepadButton button) {
        return new ButtonBindingBuilder(button, ButtonBindingBuilder.Trigger.WHEN_RELEASED, bus, gamepadState, bindings);
    }

    public ButtonBindingBuilder held(GamepadButton button) {
        return new ButtonBindingBuilder(button, ButtonBindingBuilder.Trigger.WHILE_HELD, bus, gamepadState, bindings);
    }


    public AxisBindingBuilder onAxis(GamepadAxis axis) {
        return new AxisBindingBuilder(axis, bus, gamepadState, bindings);
    }


    public <T extends Drive> DriveBindingBuilder<T> drive(Class<T> driveClass) {
        T drive = findSubsystem(driveClass);
        if (drive == null) throw new IllegalStateException(
            "No " + driveClass.getSimpleName() + " registered on robot"
        );
        DriveBindingBuilder<T> builder = new DriveBindingBuilder<>(drive, gamepadState);
        driveBindings.add(builder.binding());
        return builder;
    }


    public void update() {
        for (ControlBinding binding : bindings) {
            binding.update();
        }
        for (DriveBinding binding : driveBindings) {
            binding.update();
        }
    }


    @Override
    public void vibrate(GamepadSide side, int milliseconds) {
        gamepadState.vibrate(side, milliseconds);
    }

    @Override
    public void setLed(GamepadSide side, double r, double g, double b, int durationMs) {
        gamepadState.setLed(side, r, g, b, durationMs);
    }

    public double axisValue(GamepadAxis axis) {
        return gamepadState.axisValue(axis);
    }

    public boolean buttonValue(GamepadButton button) {
        return gamepadState.buttonValue(button);
    }


    @SuppressWarnings("unchecked")
    private <T> T findSubsystem(Class<T> type) {
        for (dev.ftcplus.core.Component child : robot.children()) {
            if (type.isInstance(child)) return (T) child;
        }
        return null;
    }
}

package dev.ftcplus.runtime.controls;

import dev.ftcplus.core.GamepadAxis;
import dev.ftcplus.core.signal.Event;
import dev.ftcplus.core.signal.SignalBus;

import java.util.List;
import java.util.function.DoubleFunction;

public final class AxisBindingBuilder {

    private final GamepadAxis axis;
    private final SignalBus bus;
    private final GamepadState gamepadState;
    private final List<ControlBinding> bindings;

    AxisBindingBuilder(GamepadAxis axis, SignalBus bus, GamepadState gamepadState,
                       List<ControlBinding> bindings) {
        this.axis         = axis;
        this.bus          = bus;
        this.gamepadState = gamepadState;
        this.bindings     = bindings;
    }

    public void send(DoubleFunction<? extends Event> eventFactory) {
        bindings.add(() -> {
            double value = gamepadState.axisValue(axis);
            if (Math.abs(value) > 0.05) { // deadband
                bus.send(eventFactory.apply(value));
            }
        });
    }
}

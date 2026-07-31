package dev.ftcplus.runtime.controls;

import dev.ftcplus.core.GamepadButton;
import dev.ftcplus.core.signal.Event;
import dev.ftcplus.core.signal.SignalBus;

import java.util.List;
import java.util.function.Supplier;

public final class ButtonBindingBuilder {

    public enum Trigger { WHEN_PRESSED, WHEN_RELEASED, WHILE_HELD }

    private final GamepadButton button;
    private final Trigger trigger;
    private final SignalBus bus;
    private final GamepadState gamepadState;
    private final List<ControlBinding> bindings;

    ButtonBindingBuilder(GamepadButton button, Trigger trigger, SignalBus bus,
                          GamepadState gamepadState, List<ControlBinding> bindings) {
        this.button      = button;
        this.trigger     = trigger;
        this.bus         = bus;
        this.gamepadState = gamepadState;
        this.bindings    = bindings;
    }

    public void send(Supplier<? extends Event> eventFactory) {
        boolean[] lastState = {false};

        bindings.add(() -> {
            boolean current = gamepadState.buttonValue(button);
            boolean shouldFire = switch (trigger) {
                case WHEN_PRESSED  -> current && !lastState[0];
                case WHEN_RELEASED -> !current && lastState[0];
                case WHILE_HELD    -> current;
            };
            if (shouldFire) bus.send(eventFactory.get());
            lastState[0] = current;
        });
    }
}

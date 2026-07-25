package dev.ftcplus.ftcruntime.controls;

import dev.ftcplus.core.signal.Event;
import dev.ftcplus.core.signal.SignalBus;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class ControlBinding {
    public enum Trigger { PRESSED, HELD, RELEASED, WHILE_HELD }

    private final BooleanSupplier condition;
    private final Trigger trigger;
    private final Supplier<? extends Event> eventSupplier;
    private final SignalBus bus;

    private boolean previousState = false;

    ControlBinding(
            BooleanSupplier condition,
            Trigger trigger,
            Supplier<? extends Event> eventSupplier,
            SignalBus bus
    ) {
        this.condition = condition;
        this.trigger = trigger;
        this.eventSupplier = eventSupplier;
        this.bus = bus;
    }

    void update() {
        boolean current = condition.getAsBoolean();

        switch (trigger) {
            case PRESSED:
                if (current && !previousState) fire();
                break;
            case RELEASED:
                if (!current && previousState) fire();
                break;
            case HELD:
                if (current && !previousState) fire();
                break;
            case WHILE_HELD:
                if (current) fire();
                break;
        }

        previousState = current;
    }

    private void fire() {
        bus.send(eventSupplier.get());
    }
}
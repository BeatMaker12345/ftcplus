package dev.ftcplus.ftcruntime.controls;

import dev.ftcplus.core.signal.Event;
import dev.ftcplus.core.signal.SignalBus;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

public final class ControlBindingBuilder {
    private final BooleanSupplier condition;
    private final List<ControlBinding> bindings;
    private final SignalBus bus;

    ControlBindingBuilder(
            BooleanSupplier condition,
            List<ControlBinding> bindings,
            SignalBus bus
    ) {
        this.condition = condition;
        this.bindings = bindings;
        this.bus = bus;
    }

    public void send(Supplier<? extends Event> event) {
        bindings.add(new ControlBinding(condition, ControlBinding.Trigger.PRESSED, event, bus));
    }

    public void sendWhileHeld(Supplier<? extends Event> event) {
        bindings.add(new ControlBinding(condition, ControlBinding.Trigger.WHILE_HELD,  event, bus));
    }

    public void sendOnRelease(Supplier<? extends Event> event) {
        bindings.add(new ControlBinding(condition, ControlBinding.Trigger.RELEASED, event, bus));
    }
}
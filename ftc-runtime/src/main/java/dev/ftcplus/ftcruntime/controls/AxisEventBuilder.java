package dev.ftcplus.ftcruntime.controls;

import dev.ftcplus.core.signal.Event;
import dev.ftcplus.core.signal.SignalBus;

import java.util.List;
import java.util.function.DoubleFunction;
import java.util.function.Function;

public final class AxisEventBuilder {
    private final GamepadAxis axis;
    private final Function<GamepadAxis, Double> axisReader;
    private final List<ControlBinding> bindings;
    private final SignalBus bus;

    AxisEventBuilder(
            GamepadAxis axis,
            Function<GamepadAxis, Double> axisReader,
            List<ControlBinding> bindings,
            SignalBus bus
    ) {
        this.axis = axis;
        this.axisReader = axisReader;
        this.bindings = bindings;
        this.bus = bus;
    }

    public void send(DoubleFunction<? extends Event> eventConstructor) {
        bindings.add(new ControlBinding(
                () -> true,
                ControlBinding.Trigger.WHILE_HELD,
                () -> eventConstructor.apply(axisReader.apply(axis)),
                bus
        ));
    }
}
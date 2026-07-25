package dev.ftcplus.ftcruntime.controls;

import dev.ftcplus.core.signal.SignalBus;

import java.util.List;
import java.util.function.Function;

public final class AxisBindingBuilder {
    private final GamepadAxis axis;
    private final Function<GamepadAxis, Double> axisReader;
    private final List<ControlBinding> bindings;
    private final SignalBus bus;

    AxisBindingBuilder(
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

    public ControlBindingBuilder exceeds(double threshold) {
        return new ControlBindingBuilder(
                () -> Math.abs(axisReader.apply(axis)) > threshold,
                bindings,
                bus
        );
    }

    public ControlBindingBuilder exceeds(double min, double max) {
        return new ControlBindingBuilder(
                () -> {
                    double v = axisReader.apply(axis);
                    return v > min && v < max;
                },
                bindings,
                bus
        );
    }
}
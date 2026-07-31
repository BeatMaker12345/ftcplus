package dev.ftcplus.core.signal;

import java.util.*;
import java.util.function.Consumer;

public final class SignalBus {
    private final Map<Class<? extends Signal>, List<Consumer<Signal>>> listeners = new HashMap<>();
    private final List<Signal> queue = new ArrayList<>();

    public <T extends Signal> Subscription subscribe(
            Class<T> type,
            Consumer<T> listener
    ) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(listener, "listener");

        Consumer<Signal> wrapped = signal -> listener.accept(type.cast(signal));

        listeners
                .computeIfAbsent(type, k -> new ArrayList<>())
                .add(wrapped);

        return new Subscription(() -> {
            List<Consumer<Signal>> list = listeners.get(type);
            if (list != null) list.remove(wrapped);
        });
    }

    public void send(Signal signal) {
        Objects.requireNonNull(signal, "signal");

        if (signal.getClass().isAnnotationPresent(Immediate.class)) {
            dispatch(signal);
        } else {
            queue.add(signal);
        }
    }

    public void flush() {
        for (Signal signal : queue) {
            dispatch(signal);
        }
        queue.clear();
    }

    private void dispatch(Signal signal) {
        List<Consumer<Signal>> list = listeners.get(signal.getClass());
        if (list == null) return;
        new ArrayList<>(list).forEach(l -> l.accept(signal));

        for (Consumer<Signal> listener : list) {
            listener.accept(signal);
        }
    }
}

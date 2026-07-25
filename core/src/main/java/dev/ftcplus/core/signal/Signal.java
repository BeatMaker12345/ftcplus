package dev.ftcplus.core.signal;

public abstract class Signal {
    private final long timestamp = System.nanoTime();

    public final long timestamp() {
        return timestamp;
    }
}
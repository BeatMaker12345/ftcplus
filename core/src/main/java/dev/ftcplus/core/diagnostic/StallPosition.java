package dev.ftcplus.core.diagnostic;

public final class StallPosition {
    public final int ticks;
    public final boolean timedOut;

    public StallPosition(int ticks, boolean timedOut) {
        this.ticks    = ticks;
        this.timedOut = timedOut;
    }

    public int ticks() { return ticks; }
    public boolean timedOut() { return timedOut; }
}
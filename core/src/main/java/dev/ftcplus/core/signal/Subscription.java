package dev.ftcplus.core.signal;

public final class Subscription {
    private final Runnable canceller;
    private boolean cancelled = false;

    Subscription(Runnable canceller) {
        this.canceller = canceller;
    }

    public void cancel() {
        if (!cancelled) {
            cancelled = true;
            canceller.run();
        }
    }

    public boolean isCancelled() {
        return cancelled;
    }
}

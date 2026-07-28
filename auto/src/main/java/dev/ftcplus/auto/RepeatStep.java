package dev.ftcplus.auto;

import java.util.List;
import java.util.function.IntFunction;

public final class RepeatStep implements AutoStep {
    public final int count;
    public final long durationMs;
    public final boolean incrementPerAction;
    public final List<IntFunction<AutoAction>> actionFactories;

    RepeatStep(int count, long durationMs, boolean incrementPerAction, List<IntFunction<AutoAction>> actionFactories) {
        this.count = count;
        this.durationMs = durationMs;
        this.incrementPerAction = incrementPerAction;
        this.actionFactories = actionFactories;
    }

    public boolean isTimeBased() { return durationMs >= 0; }
}
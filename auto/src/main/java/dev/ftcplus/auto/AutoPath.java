package dev.ftcplus.auto;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public abstract class AutoPath {
    private final List<AutoStep> steps = new ArrayList<>();

    public abstract String name();
    public abstract void define();

    protected final void action(AutoAction action) {
        steps.add(new ActionStep(action));
    }

    protected final BranchBuilder branch() {
        return new BranchBuilder(this);
    }

    void addBranch(BranchStep step) {
        steps.add(step);
    }


    protected final RepeatBuilder repeat(int count) {
        return new RepeatBuilder(this, count, -1, false);
    }

    protected final RepeatBuilder repeatFor(long durationMs) {
        return new RepeatBuilder(this, -1, durationMs, false);
    }

    void addRepeat(RepeatStep step) {
        steps.add(step);
    }

    final List<AutoStep> steps() { return steps; }


    public static final class BranchBuilder {
        private final AutoPath path;
        private final List<BranchStep.Case> cases = new ArrayList<>();

        BranchBuilder(AutoPath path) { this.path = path; }

        public BranchBuilder when(Supplier<Boolean> condition, AutoAction action) {
            cases.add(new BranchStep.Case(condition, action));
            return this;
        }

        public BranchBuilder when(Supplier<Boolean> condition, AutoPath subPath) {
            cases.add(new BranchStep.Case(condition, subPath));
            return this;
        }

        public void otherwise(AutoAction action) {
            cases.add(new BranchStep.Case(null, action));
            path.addBranch(new BranchStep(new ArrayList<>(cases)));
        }

        public void otherwise(AutoPath subPath) {
            cases.add(new BranchStep.Case(null, subPath));
            path.addBranch(new BranchStep(new ArrayList<>(cases)));
        }

        public void build() {
            path.addBranch(new BranchStep(new ArrayList<>(cases)));
        }
    }

    public static class RepeatBuilder {
        private final AutoPath path;
        private final int count;
        private final long durationMs;
        private final boolean incrementEachAction;
        private final List<IntFunction<AutoAction>> actions = new ArrayList<>();

        RepeatBuilder(AutoPath path, int count, long durationMs, boolean incrementEachAction) {
            this.path                = path;
            this.count               = count;
            this.durationMs          = durationMs;
            this.incrementEachAction = incrementEachAction;
        }

        public RepeatBuilder action(IntFunction<AutoAction> factory) {
            actions.add(factory);
            return this;
        }

        public RepeatBuilder action(AutoAction action) {
            actions.add(i -> action);
            return this;
        }

        public RepeatBuilder incrementPerAction() {
            RepeatBuilder newBuilder = new RepeatBuilder(path, count, durationMs, true);
            newBuilder.actions.addAll(this.actions);
            return newBuilder;
        }

        public void build() {
            path.addRepeat(new RepeatStep(count, durationMs, incrementEachAction, new ArrayList<>(actions)));
        }
    }
}
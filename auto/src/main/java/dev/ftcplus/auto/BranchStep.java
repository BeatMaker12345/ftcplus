package dev.ftcplus.auto;

import java.util.List;
import java.util.function.Supplier;

public final class BranchStep implements AutoStep {
    public static final class Case {
        public final Supplier<Boolean> condition;
        public final AutoAction action;
        public final AutoPath subPath;

        Case(Supplier<Boolean> condition, AutoAction action) {
            this.condition = condition;
            this.action    = action;
            this.subPath   = null;
        }

        Case(Supplier<Boolean> condition, AutoPath subPath) {
            this.condition = condition;
            this.action    = null;
            this.subPath   = subPath;
        }

        public boolean isOtherwise() { return condition == null; }
        public boolean evaluate() { return condition == null || condition.get(); }
    }

    public final List<Case> cases;
    BranchStep(List<Case> cases) { this.cases = cases; }
}
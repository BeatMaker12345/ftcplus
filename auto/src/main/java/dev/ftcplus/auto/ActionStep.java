package dev.ftcplus.auto;

public final class ActionStep implements AutoStep {
    public final AutoAction action;
    ActionStep(AutoAction action) { this.action = action; }
}
package dev.ftcplus.auto;

import java.util.ArrayList;
import java.util.List;

public abstract class AutoPath {
    private final List<AutoAction> actions = new ArrayList<>();

    public abstract String name();
    public abstract void define();

    protected final void action(AutoAction action) { actions.add(action); }
    final List<AutoAction> actions() { return actions; }
}
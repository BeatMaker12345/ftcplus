package dev.ftcplus.core.power;

import dev.ftcplus.core.signal.Event;

public abstract class PowerConstraint {
    public abstract boolean check(PowerBudget budget);
    public boolean retryUntilPassed() { return true; }
    public Event onBlocked() { return null; }
}
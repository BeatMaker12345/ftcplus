package dev.ftcplus.core;

import dev.ftcplus.core.signal.Signal;

public interface DashboardListener {
    void onUpdate();
    void onSignal(Signal signal);
}

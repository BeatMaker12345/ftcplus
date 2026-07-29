package dev.ftcplus.dashboard;

public final class DashboardMessage {
    public enum Type {
        COMPONENT_TREE,
        TELEMETRY,
        SIGNAL,
        POWER,
        SETTINGS,
        DIAGNOSTIC_RESULT,
        CALIBRATION_RESULT,

        SET_SETTING,
        RUN_DIAGNOSTIC,
        RUN_CALIBRATION
    }

    private DashboardMessage() {}
}
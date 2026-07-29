package dev.ftcplus.core;

/**
 * Allows Runtime to pass itself to a DashboardListener without knowing its concrete type.
 */
public interface DashboardAttachable {
    void attach(Robot<?, ?, ?> robot, Runtime runtime);
}

package dev.ftcplus.core.telemetry;

public interface TelemetryProvider {
    void update();

    LineBuilder line(java.util.function.Supplier<String> value);
    LineBuilder line(String value);
    void divider();

    PanelBuilder panel(String name);
    GraphBuilder graph(String key);
    TableBuilder table(String name);
    FieldBuilder field();
}
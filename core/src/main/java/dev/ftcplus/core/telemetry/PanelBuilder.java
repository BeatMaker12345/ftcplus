package dev.ftcplus.core.telemetry;

import java.util.function.Supplier;

public interface PanelBuilder {
    PanelBuilder line(Supplier<String> value);
    PanelBuilder line(String value);
    PanelBuilder divider();
    PanelBuilder graph(String key);
    PanelBuilder table(String name);
}
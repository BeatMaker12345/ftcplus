package dev.ftcplus.core.telemetry;

import java.util.function.Supplier;

public interface RowBuilder {
    RowBuilder item(String label, Supplier<String> value);
    RowBuilder item(String label, String value);
    TableBuilder end();
}
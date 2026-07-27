package dev.ftcplus.core.telemetry;

import java.util.function.DoubleSupplier;

public interface GraphBuilder {
    GraphBuilder source(DoubleSupplier supplier);
    GraphBuilder color(String hex);
    GraphBuilder range(double min, double max);
    GraphBuilder unit(String unit);
}
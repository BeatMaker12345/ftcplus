package dev.ftcplus.sim.telemetry;

import dev.ftcplus.core.telemetry.FieldAxis;
import dev.ftcplus.core.telemetry.FieldBuilder;
import dev.ftcplus.core.telemetry.GraphBuilder;
import dev.ftcplus.core.telemetry.LineBuilder;
import dev.ftcplus.core.telemetry.PanelBuilder;
import dev.ftcplus.core.telemetry.RowBuilder;
import dev.ftcplus.core.telemetry.TableBuilder;
import dev.ftcplus.core.telemetry.TelemetryProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

public final class SimTelemetryProvider implements TelemetryProvider {

    private final List<Supplier<String>> lines = new ArrayList<>();
    private boolean verbose = false;

    public SimTelemetryProvider() {}
    public SimTelemetryProvider(boolean verbose) { this.verbose = verbose; }

    @Override
    public void update() {
        if (!verbose) return;
        System.out.println("--- Telemetry ---");
        for (Supplier<String> line : lines) {
            System.out.println(line.get());
        }
    }

    @Override
    public LineBuilder line(Supplier<String> value) {
        lines.add(value);
        return new NoOpLineBuilder();
    }

    @Override public LineBuilder line(String value) { return line(() -> value); }
    @Override public void divider()                 { lines.add(() -> "---"); }
    @Override public PanelBuilder panel(String name) { return new NoOpPanelBuilder(name, this); }
    @Override public GraphBuilder graph(String key)  { return new NoOpGraphBuilder(key, this); }
    @Override public TableBuilder table(String name) { return new NoOpTableBuilder(this); }
    @Override public FieldBuilder field()            { return new NoOpFieldBuilder(); }


    private static class NoOpLineBuilder implements LineBuilder {
        @Override public LineBuilder color(String hex)  { return this; }
        @Override public LineBuilder bold()             { return this; }
        @Override public LineBuilder italic()           { return this; }
        @Override public LineBuilder size(float scale)  { return this; }
    }

    private static class NoOpPanelBuilder implements PanelBuilder {
        private final SimTelemetryProvider provider;
        NoOpPanelBuilder(String name, SimTelemetryProvider p) { this.provider = p; }
        @Override public PanelBuilder line(Supplier<String> v)  { provider.lines.add(v); return this; }
        @Override public PanelBuilder line(String v)            { return line(() -> v); }
        @Override public PanelBuilder divider()                 { return this; }
        @Override public PanelBuilder graph(String key)         { return this; }
        @Override public PanelBuilder table(String name)        { return this; }
    }

    private static class NoOpGraphBuilder implements GraphBuilder {
        private final SimTelemetryProvider provider;
        private final String key;
        private DoubleSupplier source;
        NoOpGraphBuilder(String key, SimTelemetryProvider p) { this.key = key; this.provider = p; }
        @Override public GraphBuilder source(DoubleSupplier s)  {
            this.source = s;
            provider.lines.add(() -> key + ": " + String.format("%.2f", s.getAsDouble()));
            return this;
        }
        @Override public GraphBuilder color(String hex)             { return this; }
        @Override public GraphBuilder range(double min, double max) { return this; }
        @Override public GraphBuilder unit(String unit)             { return this; }
    }

    private static class NoOpTableBuilder implements TableBuilder {
        private final SimTelemetryProvider provider;
        NoOpTableBuilder(SimTelemetryProvider p) { this.provider = p; }
        @Override public RowBuilder row() { return new NoOpRowBuilder(this, provider); }
    }

    private static class NoOpRowBuilder implements RowBuilder {
        private final TableBuilder parent;
        private final SimTelemetryProvider provider;
        NoOpRowBuilder(TableBuilder parent, SimTelemetryProvider p) { this.parent = parent; this.provider = p; }
        @Override public RowBuilder item(String label, Supplier<String> value) {
            provider.lines.add(() -> label + ": " + value.get());
            return this;
        }
        @Override public RowBuilder item(String label, String value) { return item(label, () -> value); }
        @Override public TableBuilder end() { return parent; }
    }

    private static class NoOpFieldBuilder implements FieldBuilder {
        @Override public FieldBuilder robot(Supplier<double[]> p)                              { return this; }
        @Override public FieldBuilder robot(String name, Supplier<double[]> p)                 { return this; }
        @Override public FieldBuilder point(String name, Supplier<double[]> xyz)               { return this; }
        @Override public FieldBuilder point(String name, double x, double y, double z)         { return this; }
        @Override public FieldBuilder axis(String name, DoubleSupplier value, FieldAxis axis)  { return this; }
        @Override public FieldBuilder line(String name, Supplier<double[]> f, Supplier<double[]> t) { return this; }
    }
}

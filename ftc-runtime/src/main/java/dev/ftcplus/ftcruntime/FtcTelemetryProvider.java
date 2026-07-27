package dev.ftcplus.ftcruntime;

import dev.ftcplus.core.telemetry.*;
import org.firstinspires.ftc.robotcore.external.Telemetry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

public class FtcTelemetryProvider implements TelemetryProvider {
    private final Telemetry telemetry;
    private final List<Runnable> entries = new ArrayList<>();

    public FtcTelemetryProvider(Telemetry telemetry) {
        this.telemetry = telemetry;
    }

    @Override
    public void update() {
        telemetry.clearAll();
        for (Runnable entry : entries) {
            entry.run();
        }
        telemetry.update();
    }

    @Override
    public LineBuilder line(Supplier<String> value) {
        entries.add(() -> telemetry.addLine(value.get()));
        return new NoOpLineBuilder();
    }

    @Override
    public LineBuilder line(String value) {
        entries.add(() -> telemetry.addLine(value));
        return new NoOpLineBuilder();
    }

    @Override
    public void divider() {
        entries.add(() -> telemetry.addLine("---------------------------------"));
    }

    @Override
    public PanelBuilder panel(String name) {
        entries.add(() -> telemetry.addLine("=== " + name + " ==="));
        return new FtcPanelBuilder(name, telemetry, entries);
    }

    @Override
    public GraphBuilder graph(String key) {
        return new NoOpGraphBuilder(key, entries, telemetry);
    }

    @Override
    public TableBuilder table(String name) {
        entries.add(() -> telemetry.addLine("[ " + name + " ]"));
        return new FtcTableBuilder(telemetry, entries);
    }

    @Override
    public FieldBuilder field() {
        return new NoOpFieldBuilder();
    }


    private static final class NoOpLineBuilder implements LineBuilder {
        @Override public LineBuilder color(String hex)    { return this; }
        @Override public LineBuilder bold()               { return this; }
        @Override public LineBuilder italic()             { return this; }
        @Override public LineBuilder size(float scale)    { return this; }
    }

    private static final class FtcPanelBuilder implements PanelBuilder {
        private final String name;
        private final Telemetry telemetry;
        private final List<Runnable> entries;

        FtcPanelBuilder(String name, Telemetry telemetry, List<Runnable> entries) {
            this.name = name;
            this.telemetry = telemetry;
            this.entries = entries;
        }

        @Override
        public PanelBuilder line(Supplier<String> value) {
            entries.add(() -> telemetry.addLine("  " + value.get()));
            return this;
        }

        @Override
        public PanelBuilder line(String value) {
            entries.add(() -> telemetry.addLine("  " + value));
            return this;
        }

        @Override
        public PanelBuilder divider() {
            entries.add(() -> telemetry.addLine("  --------"));
            return this;
        }

        @Override
        public PanelBuilder graph(String key) { return this; }

        @Override
        public PanelBuilder table(String name) {
            entries.add(() -> telemetry.addLine("  [ " + name + "  ]"));
            return this;
        }
    }

    private static final class NoOpGraphBuilder implements GraphBuilder {
        private final String key;
        private final List<Runnable> entries;
        private final Telemetry telemetry;
        private DoubleSupplier source;

        NoOpGraphBuilder(String key, List<Runnable> entries, Telemetry telemetry) {
            this.key = key;
            this.entries = entries;
            this.telemetry = telemetry;
        }

        @Override
        public GraphBuilder source(DoubleSupplier supplier) {
            this.source = supplier;

            entries.add(() -> telemetry.addLine(key + ": " + (source != null ? source.getAsDouble() : "?")));
            return this;
        }

        @Override public GraphBuilder color(String hex)             { return this; }
        @Override public GraphBuilder range(double min, double max) { return this; }
        @Override public GraphBuilder unit(String unit)             { return this; }
    }

    private static final class FtcTableBuilder implements TableBuilder {
        private final Telemetry telemetry;
        private final List<Runnable> entries;

        FtcTableBuilder(Telemetry telemetry, List<Runnable> entries) {
            this.telemetry = telemetry;
            this.entries = entries;
        }

        @Override
        public RowBuilder row() {
            return new FtcRowBuilder(telemetry, entries, this);
        }
    }

    private static final class FtcRowBuilder implements RowBuilder {
        private final Telemetry telemetry;
        private final List<Runnable> entries;
        private final TableBuilder parent;
        private final List<Runnable> items = new ArrayList<>();

        FtcRowBuilder(Telemetry telemetry,  List<Runnable> entries, TableBuilder parent) {
            this.telemetry = telemetry;
            this.entries = entries;
            this.parent = parent;
        }

        @Override
        public RowBuilder item(String label, Supplier<String> value) {
            items.add(() -> telemetry.addLine("  " + label + ": " + value.get()));
            return this;
        }

        @Override
        public RowBuilder item(String label, String value) {
            items.add(() -> telemetry.addLine("  " + label + ": " + value));
            return this;
        }

        @Override
        public TableBuilder end() {
            entries.addAll(items);
            return parent;
        }
    }

    private static final class NoOpFieldBuilder implements FieldBuilder {
        @Override public FieldBuilder robot(Supplier<double[]> p)                                       { return this; }
        @Override public FieldBuilder robot(String name, Supplier<double[]> p)                          { return this; }
        @Override public FieldBuilder point(String name, Supplier<double[]> xyz)                        { return this; }
        @Override public FieldBuilder point(String name, double x, double y, double z)                  { return this; }
        @Override public FieldBuilder axis(String name, DoubleSupplier v, FieldAxis axis)               { return this; }
        @Override public FieldBuilder line(String name, Supplier<double[]> from, Supplier<double[]> to) { return this; }
    }
}
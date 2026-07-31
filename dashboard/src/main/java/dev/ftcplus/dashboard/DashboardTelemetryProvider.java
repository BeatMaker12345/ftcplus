// dashboard/src/main/java/dev/ftcplus/dashboard/DashboardTelemetryProvider.java
package dev.ftcplus.dashboard;

import dev.ftcplus.core.telemetry.FieldAxis;
import dev.ftcplus.core.telemetry.FieldBuilder;
import dev.ftcplus.core.telemetry.GraphBuilder;
import dev.ftcplus.core.telemetry.LineBuilder;
import dev.ftcplus.core.telemetry.PanelBuilder;
import dev.ftcplus.core.telemetry.RowBuilder;
import dev.ftcplus.core.telemetry.TableBuilder;
import dev.ftcplus.core.telemetry.TelemetryProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

public final class DashboardTelemetryProvider implements TelemetryProvider {

    private final List<TelemetryEntry> entries = new ArrayList<>();
    private final FtcDashboardServer server;

    public DashboardTelemetryProvider(FtcDashboardServer server) {
        this.server = server;
    }

    @Override
    public void update() {
        try {
            JSONArray arr = new JSONArray();
            for (TelemetryEntry entry : entries) {
                arr.put(entry.toJSON());
            }
            server.broadcast(DashboardSerializer.serializeTelemetry(this).toString());
        } catch (Exception e) {
            java.util.logging.Logger.getLogger("FtcDashboard").warning("Telemetry update failed: " + e.getMessage());
        }
    }

    public JSONArray toJSON() throws JSONException {
        JSONArray arr = new JSONArray();
        for (TelemetryEntry entry : entries) {
            arr.put(entry.toJSON());
        }
        return arr;
    }


    @Override
    public LineBuilder line(Supplier<String> value) {
        LineEntry e = new LineEntry(value);
        entries.add(e);
        return e;
    }

    @Override
    public LineBuilder line(String value) {
        return line(() -> value);
    }

    @Override
    public void divider() {
        entries.add(new DividerEntry());
    }

    @Override
    public PanelBuilder panel(String name) {
        PanelEntry e = new PanelEntry(name);
        entries.add(e);
        return e;
    }

    @Override
    public GraphBuilder graph(String key) {
        GraphEntry e = new GraphEntry(key);
        entries.add(e);
        return e;
    }

    @Override
    public TableBuilder table(String name) {
        TableEntry e = new TableEntry(name);
        entries.add(e);
        return e;
    }

    @Override
    public FieldBuilder field() {
        FieldEntry e = new FieldEntry();
        entries.add(e);
        return e;
    }


    interface TelemetryEntry {
        JSONObject toJSON() throws JSONException;
    }

    static final class LineEntry implements TelemetryEntry, LineBuilder {
        private final Supplier<String> value;
        private String color;
        private boolean bold, italic;
        private float size = 1.0f;

        LineEntry(Supplier<String> value) { this.value = value; }

        @Override public LineBuilder color(String hex)  { this.color = hex; return this; }
        @Override public LineBuilder bold()             { this.bold = true; return this; }
        @Override public LineBuilder italic()           { this.italic = true; return this; }
        @Override public LineBuilder size(float scale)  { this.size = scale; return this; }

        @Override
        public JSONObject toJSON() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("kind",   "line");
            o.put("value",  value.get());
            if (color  != null) o.put("color",  color);
            if (bold)           o.put("bold",   true);
            if (italic)         o.put("italic", true);
            if (size != 1.0f)   o.put("size",   size);
            return o;
        }
    }

    static final class DividerEntry implements TelemetryEntry {
        @Override
        public JSONObject toJSON() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("kind", "divider");
            return o;
        }
    }

    static final class PanelEntry implements TelemetryEntry, PanelBuilder {
        private final String name;
        private final List<TelemetryEntry> children = new ArrayList<>();

        PanelEntry(String name) { this.name = name; }

        @Override
        public PanelBuilder line(Supplier<String> value) {
            children.add(new LineEntry(value)); return this;
        }

        @Override
        public PanelBuilder line(String value) {
            return line(() -> value);
        }

        @Override
        public PanelBuilder divider() {
            children.add(new DividerEntry()); return this;
        }

        @Override
        public PanelBuilder graph(String key) {
            children.add(new GraphEntry(key)); return this;
        }

        @Override
        public PanelBuilder table(String name) {
            children.add(new TableEntry(name)); return this;
        }

        @Override
        public JSONObject toJSON() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("kind", "panel");
            o.put("name", name);
            JSONArray arr = new JSONArray();
            for (TelemetryEntry e : children) arr.put(e.toJSON());
            o.put("children", arr);
            return o;
        }
    }

    static final class GraphEntry implements TelemetryEntry, GraphBuilder {
        private final String key;
        private DoubleSupplier source;
        private String color;
        private double min = Double.NaN, max = Double.NaN;
        private String unit;

        GraphEntry(String key) { this.key = key; }

        @Override public GraphBuilder source(DoubleSupplier s)          { this.source = s; return this; }
        @Override public GraphBuilder color(String hex)                  { this.color = hex; return this; }
        @Override public GraphBuilder range(double min, double max)      { this.min = min; this.max = max; return this; }
        @Override public GraphBuilder unit(String unit)                  { this.unit = unit; return this; }

        @Override
        public JSONObject toJSON() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("kind",  "graph");
            o.put("key",   key);
            o.put("value", source != null ? source.getAsDouble() : 0);
            if (color != null)          o.put("color", color);
            if (!Double.isNaN(min))     o.put("min",   min);
            if (!Double.isNaN(max))     o.put("max",   max);
            if (unit != null)           o.put("unit",  unit);
            return o;
        }
    }

    static final class TableEntry implements TelemetryEntry, TableBuilder {
        private final String name;
        private final List<RowEntry> rows = new ArrayList<>();

        TableEntry(String name) { this.name = name; }

        @Override
        public RowBuilder row() {
            RowEntry r = new RowEntry(this);
            rows.add(r);
            return r;
        }

        @Override
        public JSONObject toJSON() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("kind", "table");
            o.put("name", name);
            JSONArray arr = new JSONArray();
            for (RowEntry r : rows) arr.put(r.toJSON());
            o.put("rows", arr);
            return o;
        }
    }

    static final class RowEntry implements RowBuilder {
        private final TableEntry parent;
        private final List<ItemEntry> items = new ArrayList<>();

        RowEntry(TableEntry parent) { this.parent = parent; }

        @Override
        public RowBuilder item(String label, Supplier<String> value) {
            items.add(new ItemEntry(label, value)); return this;
        }

        @Override
        public RowBuilder item(String label, String value) {
            return item(label, () -> value);
        }

        @Override
        public TableBuilder end() { return parent; }

        public JSONObject toJSON() throws JSONException {
            JSONObject o = new JSONObject();
            JSONArray arr = new JSONArray();
            for (ItemEntry i : items) arr.put(i.toJSON());
            o.put("items", arr);
            return o;
        }
    }

    static final class ItemEntry {
        final String label;
        final Supplier<String> value;

        ItemEntry(String label, Supplier<String> value) {
            this.label = label;
            this.value = value;
        }

        public JSONObject toJSON() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("label", label);
            o.put("value", value.get());
            return o;
        }
    }

    static final class FieldEntry implements TelemetryEntry, FieldBuilder {
        private final List<FieldObject> objects = new ArrayList<>();

        @Override
        public FieldBuilder robot(Supplier<double[]> pose) {
            objects.add(new FieldObject("robot", null, pose, null, null)); return this;
        }

        @Override
        public FieldBuilder robot(String name, Supplier<double[]> pose) {
            objects.add(new FieldObject("robot", name, pose, null, null)); return this;
        }

        @Override
        public FieldBuilder point(String name, Supplier<double[]> xyz) {
            objects.add(new FieldObject("point", name, xyz, null, null)); return this;
        }

        @Override
        public FieldBuilder point(String name, double x, double y, double z) {
            return point(name, () -> new double[]{x, y, z});
        }

        @Override
        public FieldBuilder axis(String name, DoubleSupplier value, FieldAxis axis) {
            objects.add(new FieldObject("axis", name, null, value, axis)); return this;
        }

        @Override
        public FieldBuilder line(String name, Supplier<double[]> from, Supplier<double[]> to) {
            objects.add(new FieldObject("line", name, from, null, null) {
                final Supplier<double[]> toSupplier = to;
            }); return this;
        }

        @Override
        public JSONObject toJSON() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("kind", "field");
            JSONArray arr = new JSONArray();
            for (FieldObject obj : objects) arr.put(obj.toJSON());
            o.put("objects", arr);
            return o;
        }
    }

    static class FieldObject {
        final String kind;
        final String name;
        final Supplier<double[]> poseSupplier;
        final DoubleSupplier valueSupplier;
        final FieldAxis axis;

        FieldObject(String kind, String name, Supplier<double[]> poseSupplier,
                    DoubleSupplier valueSupplier, FieldAxis axis) {
            this.kind          = kind;
            this.name          = name;
            this.poseSupplier  = poseSupplier;
            this.valueSupplier = valueSupplier;
            this.axis          = axis;
        }

        public JSONObject toJSON() throws JSONException {
            JSONObject o = new JSONObject();
            o.put("kind", kind);
            if (name != null) o.put("name", name);

            if (poseSupplier != null) {
                double[] pose = poseSupplier.get();
                if (pose != null) {
                    o.put("x", pose.length > 0 ? pose[0] : 0);
                    o.put("y", pose.length > 1 ? pose[1] : 0);
                    o.put("z", pose.length > 2 ? pose[2] : 0);
                }
            }

            if (valueSupplier != null) {
                o.put("value", valueSupplier.getAsDouble());
                if (axis != null) o.put("axis", axis.name());
            }

            return o;
        }
    }
}
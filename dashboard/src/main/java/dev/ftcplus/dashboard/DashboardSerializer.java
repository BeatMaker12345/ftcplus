// dashboard/src/main/java/dev/ftcplus/dashboard/DashboardSerializer.java
package dev.ftcplus.dashboard;

import dev.ftcplus.core.Component;
import dev.ftcplus.core.HardwareDevice;
import dev.ftcplus.core.Robot;
import dev.ftcplus.core.sensor.Sensor;
import dev.ftcplus.core.Subsystem;
import dev.ftcplus.core.power.PowerBudget;
import dev.ftcplus.core.telemetry.TelemetryProvider;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class DashboardSerializer {

    private DashboardSerializer() {}


    public static JSONObject serializeTree(Component root) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("type", "COMPONENT_TREE");
        obj.put("root", serializeComponent(root));
        return obj;
    }

    private static JSONObject serializeComponent(Component component) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("path",  component.path() != null ? component.path() : "robot");
        obj.put("name",  component.getClass().getSimpleName());
        obj.put("kind",  componentKind(component));

        if (component instanceof Subsystem) {
            Subsystem<?> sub = (Subsystem<?>) component;
            if (sub.currentState() != null) {
                obj.put("state", sub.currentState().toString());
            }
        }

        JSONArray children = new JSONArray();
        for (Component child : component.children()) {
            children.put(serializeComponent(child));
        }
        obj.put("children", children);

        return obj;
    }

    private static String componentKind(Component component) {
        if (component instanceof Robot)         return "robot";
        if (component instanceof Subsystem)     return "subsystem";
        if (component instanceof Sensor)        return "sensor";
        if (component instanceof HardwareDevice) return "hardware";
        return "component";
    }


    public static JSONObject serializeTelemetry(DashboardTelemetryProvider telemetry) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("type",      "TELEMETRY");
        obj.put("timestamp", System.currentTimeMillis());
        obj.put("entries",   telemetry.toJSON());
        return obj;
    }


    public static JSONObject serializeSignal(String signalClass, Object payload) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("type",        "SIGNAL");
        obj.put("timestamp",   System.currentTimeMillis());
        obj.put("signalClass", signalClass);
        obj.put("payload",     payload != null ? payload.toString() : null);
        return obj;
    }


    public static JSONObject serializePower(PowerBudget budget, Component root) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("type",               "POWER");
        obj.put("timestamp",          System.currentTimeMillis());
        obj.put("totalAmps",          budget.totalCurrentAmps());
        obj.put("maxAmps",            budget.maxCurrentAmps());
        obj.put("utilizationPercent", budget.utilizationPercent());
        obj.put("isOverBudget",       budget.isOverBudget());
        obj.put("components",         serializeComponentPower(root));
        return obj;
    }

    private static JSONArray serializeComponentPower(Component component) throws JSONException {
        JSONArray arr = new JSONArray();

        if (component instanceof HardwareDevice) {
            JSONObject entry = new JSONObject();
            entry.put("path",  component.path());
            entry.put("amps",  ((HardwareDevice) component).estimatedCurrentDraw());
            arr.put(entry);
        }

        for (Component child : component.children()) {
            JSONArray childArr = serializeComponentPower(child);
            for (int i = 0; i < childArr.length(); i++) {
                arr.put(childArr.get(i));
            }
        }

        return arr;
    }


    public static JSONObject serializeSettings(java.util.Map<String, Object> settings) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put("type",     "SETTINGS");
        obj.put("settings", new JSONObject(settings));
        return obj;
    }
}
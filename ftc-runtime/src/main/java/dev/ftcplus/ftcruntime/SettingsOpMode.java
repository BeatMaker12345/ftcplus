package dev.ftcplus.ftcruntime;

import android.content.Context;
import android.content.SharedPreferences;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.ftcplus.core.Component;
import dev.ftcplus.core.Robot;
import dev.ftcplus.core.Runtime;
import dev.ftcplus.core.Setting;
import dev.ftcplus.core.SettingMenu;
import dev.ftcplus.core.menu.MenuItem;
import dev.ftcplus.core.menu.MenuHost;
import dev.ftcplus.ftcruntime.menu.GamepadMenuInputSource;
import dev.ftcplus.ftcruntime.menu.TelemetryMenu;

@TeleOp(name = "Settings", group = "FTC+")
public final class SettingsOpMode extends FtcPlusAdvancedOpMode {

    @Override
    public void runOpMode() {
        SharedPreferences prefs = hardwareMap.appContext
                .getSharedPreferences("ftcplus_settings", Context.MODE_PRIVATE);

        List<Class<?>> robotClasses = RobotResolver.findRobotClasses();

        // robot picker
        if (robotClasses.size() > 1) {
            showRobotPicker(robotClasses, prefs);
            if (!opModeIsActive()) return;
        }

        Robot<?, ?, ?> robot = RobotResolver.resolveFromPrefs(robotClasses, prefs, null);
        loadSettings(robot, prefs);

        MenuHost host = new MenuHost();
        TelemetryMenu root;

        try {
            Runtime runtime = createRuntime(robot);
            runtime.initialize();
            root = buildMenu(robot, prefs, host);
        } catch (Exception e) {
            root = new TelemetryMenu("Settings", telemetry);
            root.addItem(MenuItem.info("Initialization failed:"));
            root.addItem(MenuItem.info(e.getMessage() != null
                    ? e.getMessage() : e.getClass().getSimpleName()));
        }

        host.setRoot(root);

        GamepadMenuInputSource input = new GamepadMenuInputSource(gamepad1, gamepad2);

        waitForStart();

        while (opModeIsActive() && host.isActive()) {
            input.update();
            host.update(input);
        }

        try {
            saveSettings(robot, prefs);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    private void showRobotPicker(List<Class<?>> robotClasses, SharedPreferences prefs) {
        GamepadMenuInputSource input = new GamepadMenuInputSource(gamepad1, gamepad2);
        MenuHost host = new MenuHost();
        TelemetryMenu picker = new TelemetryMenu("Select Robot", telemetry);

        for (Class<?> c : robotClasses) {
            dev.ftcplus.core.TeamRobot annotation = c.getAnnotation(dev.ftcplus.core.TeamRobot.class);
            String name = annotation != null && !annotation.name().isEmpty()
                    ? annotation.name()
                    : c.getSimpleName();

            picker.addItem(MenuItem.action(name, () -> {
                RobotResolver.saveSelectedRobotName(prefs, name);
                host.back(); // exits the picker
            }));
        }

        host.setRoot(picker);

        while (opModeIsActive() && host.isActive()) {
            input.update();
            host.update(input);
        }
    }


    private TelemetryMenu buildMenu(Robot<?, ?, ?> robot, SharedPreferences prefs, MenuHost host) {
        TelemetryMenu root = new TelemetryMenu("Settings", telemetry);

        Map<String, List<SettingEntry>> grouped = collectSettings(robot);

        if (grouped.isEmpty()) {
            root.addItem(MenuItem.info("No @Setting fields found."));
            return root;
        }

        for (Map.Entry<String, List<SettingEntry>> entry : grouped.entrySet()) {
            String componentPath = entry.getKey();
            List<SettingEntry> settings = entry.getValue();

            if (settings.size() == 1 && grouped.size() == 1) {
                addSettingItems(root, settings.get(0), host, prefs);
            } else {
                TelemetryMenu sub = new TelemetryMenu(componentPath, telemetry);
                for (SettingEntry s : settings) {
                    addSettingItems(sub, s, host, prefs);
                }
                root.addItem(MenuItem.action(componentPath, () -> host.push(sub)));
            }
        }

        return root;
    }

    @SuppressWarnings("unchecked")
    private void addSettingItems(TelemetryMenu menu, SettingEntry entry, MenuHost host, SharedPreferences prefs) {
        Field field = entry.field;
        Object instance = entry.instance;
        Setting annotation = field.getAnnotation(Setting.class);
        String name = annotation.name();
        Class<?> type = field.getType();

        try {
            if (type == boolean.class || type == Boolean.class) {
                menu.addItem(MenuItem.toggle(
                        () -> {
                            try { return name + ": " + field.getBoolean(instance); }
                            catch (Exception e) { return name + ": ?"; }
                        },
                        () -> {
                            try {
                                field.setBoolean(instance, !field.getBoolean(instance));
                                if (annotation.persist()) saveField(field, instance, prefs);
                            } catch (Exception ignored) {}
                        }
                ));

            } else if (type == double.class || type == Double.class
                    || type == float.class || type == Float.class) {
                double step = annotation.step();
                double min  = annotation.min();
                double max  = annotation.max();

                menu.addItem(MenuItem.numeric(
                        () -> {
                            try { return name + ": " + field.getDouble(instance); }
                            catch (Exception e) { return name + ": ?"; }
                        },
                        () -> {
                            try {
                                double v = Math.max(min, field.getDouble(instance) - step);
                                field.setDouble(instance, v);
                                if (annotation.persist()) saveField(field, instance, prefs);
                            } catch (Exception ignored) {}
                        },
                        () -> {
                            try {
                                double v = Math.min(max, field.getDouble(instance) + step);
                                field.setDouble(instance, v);
                                if (annotation.persist()) saveField(field, instance, prefs);
                            } catch (Exception ignored) {}
                        }
                ));

            } else if (type == int.class || type == Integer.class
                    || type == long.class || type == Long.class) {
                double step = annotation.step();
                double min  = annotation.min();
                double max  = annotation.max();

                menu.addItem(MenuItem.numeric(
                        () -> {
                            try { return name + ": " + field.getLong(instance); }
                            catch (Exception e) { return name + ": ?"; }
                        },
                        () -> {
                            try {
                                long v = (long) Math.max(min, field.getLong(instance) - step);
                                field.setLong(instance, v);
                                if (annotation.persist()) saveField(field, instance, prefs);
                            } catch (Exception ignored) {}
                        },
                        () -> {
                            try {
                                long v = (long) Math.min(max, field.getLong(instance) + step);
                                field.setLong(instance, v);
                                if (annotation.persist()) saveField(field, instance, prefs);
                            } catch (Exception ignored) {}
                        }
                ));

            } else if (type.isEnum()) {
                Class rawType = type;
                menu.addItem(MenuItem.enumCycle(
                        name + ": ",
                        rawType,
                        () -> {
                            try { return (Enum) field.get(instance); }
                            catch (Exception e) { return null; }
                        },
                        val -> {
                            try {
                                field.set(instance, val);
                                if (annotation.persist()) saveField(field, instance, prefs);
                            } catch (Exception ignored) {}
                        }
                ));

            } else {
                menu.addItem(MenuItem.info(name + ": (unsupported type)"));
            }

        } catch (Exception e) {
            menu.addItem(MenuItem.info(name + ": (error)"));
        }
    }

    private Map<String, List<SettingEntry>> collectSettings(Component root) {
        Map<String, List<SettingEntry>> grouped = new LinkedHashMap<>();
        walkComponent(root, grouped);
        return grouped;
    }

    private void walkComponent(Component component, Map<String, List<SettingEntry>> grouped) {
        List<SettingEntry> entries = new ArrayList<>();

        for (Class<?> type = component.getClass();
             type != null && Component.class.isAssignableFrom(type);
             type = type.getSuperclass()) {

            for (Field field : type.getDeclaredFields()) {
                if (!field.isAnnotationPresent(Setting.class)) continue;
                field.trySetAccessible();
                entries.add(new SettingEntry(field, component));
            }
        }

        if (!entries.isEmpty()) {
            String path = resolveMenuName(component);
            grouped.put(path, entries);
        }

        for (Component child : component.children()) {
            walkComponent(child, grouped);
        }
    }

    private String resolveMenuName(Component component) {
        SettingMenu annotation = component.getClass().getAnnotation(SettingMenu.class);
        if (annotation != null) return annotation.name();
        if (component.parent() == null) return "Robot";
        return component.path();
    }

    private boolean isRoot(Component component) {
        return component.parent() == null;
    }


    private void loadSettings(Component root, SharedPreferences prefs) {
        Map<String, List<SettingEntry>> grouped = collectSettings(root);
        for (List<SettingEntry> entries : grouped.values()) {
            for (SettingEntry entry : entries) {
                Setting annotation = entry.field.getAnnotation(Setting.class);
                if (!annotation.persist()) continue;
                loadField(entry.field, entry.instance, prefs);
            }
        }
    }

    private void saveSettings(Component root, SharedPreferences prefs) throws IllegalAccessException {
        Map<String, List<SettingEntry>> grouped = collectSettings(root);
        for (List<SettingEntry> entries : grouped.values()) {
            for (SettingEntry entry : entries) {
                Setting annotation = entry.field.getAnnotation(Setting.class);
                if (!annotation.persist()) continue;
                saveField(entry.field, entry.instance, prefs);
            }
        }
    }

    private void saveField(Field field, Object instance, SharedPreferences prefs) throws IllegalAccessException {
        String key = field.getDeclaringClass().getName() + "." + field.getName();
        SharedPreferences.Editor editor = prefs.edit();
        try {
            Class<?> type = field.getType();
            if (type == boolean.class || type == Boolean.class)
                editor.putBoolean(key, field.getBoolean(instance));
            else if (type == double.class || type == Double.class)
                editor.putFloat(key, (float) field.getDouble(instance));
            else if (type == float.class || type == Float.class)
                editor.putFloat(key, field.getFloat(instance));
            else if (type == int.class || type == Integer.class)
                editor.putInt(key, field.getInt(instance));
            else if (type == long.class || type == Long.class)
                editor.putLong(key, field.getLong(instance));
            else if (type.isEnum())
                editor.putString(key, ((Enum<?>) field.get(instance)).name());
            editor.apply();
            CalibrationOpMode.streamSettingChange(
                    field.getDeclaringClass().getName(),
                    field.getName(),
                    field.get(instance)
            );
        } catch (Exception ignored) {}
    }

    private void loadField(Field field, Object instance, SharedPreferences prefs) {
        String key = field.getDeclaringClass().getName() + "." + field.getName();
        if (!prefs.contains(key)) return;
        try {
            Class<?> type = field.getType();
            if (type == boolean.class || type == Boolean.class)
                field.setBoolean(instance, prefs.getBoolean(key, false));
            else if (type == double.class || type == Double.class)
                field.setDouble(instance, prefs.getFloat(key, 0f));
            else if (type == float.class || type == Float.class)
                field.setFloat(instance, prefs.getFloat(key, 0f));
            else if (type == int.class || type == Integer.class)
                field.setInt(instance, prefs.getInt(key, 0));
            else if (type == long.class || type == Long.class)
                field.setLong(instance, prefs.getLong(key, 0L));
            else if (type.isEnum()) {
                String name = prefs.getString(key, null);
                if (name != null) {
                    for (Object constant : type.getEnumConstants()) {
                        if (((Enum<?>) constant).name().equals(name)) {
                            field.set(instance, constant);
                            break;
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }


    private Robot<?, ?, ?> resolveRobot() {
        List<Class<?>> classes = RobotResolver.findRobotClasses();
        // TODO: pick robot from Settings
        return RobotResolver.resolve(classes);
    }

    private static final class SettingEntry {
        final Field field;
        final Object instance;

        SettingEntry(Field field, Object instance) {
            this.field = field;
            this.instance = instance;
        }
    }
}
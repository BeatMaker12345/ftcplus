package dev.ftcplus.ftcruntime;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import dev.ftcplus.core.*;
import dev.ftcplus.core.Runtime;
import dev.ftcplus.core.menu.MenuHost;
import dev.ftcplus.core.menu.MenuItem;
import dev.ftcplus.ftcruntime.menu.GamepadMenuInputSource;
import dev.ftcplus.ftcruntime.menu.TelemetryMenu;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@TeleOp(name = "Calibration", group = "FTC+")
public class CalibrationOpMode extends FtcPlusAdvancedOpMode {

    @Override
    public void runOpMode() {
        Robot<?, ?, ?> robot = RobotResolver.resolve(RobotResolver.findRobotClasses());
        Runtime runtime = createRuntime(robot);
        runtime.initialize();

        MenuHost host = new MenuHost();
        TelemetryMenu root = buildMenu(robot, host);
        host.setRoot(root);

        GamepadMenuInputSource input = new GamepadMenuInputSource(gamepad1, gamepad2);

        waitForStart();

        while (opModeIsActive() && host.isActive()) {
            input.update();
            host.update(input);
        }
    }

    private TelemetryMenu buildMenu(Robot<?, ?, ?> robot, MenuHost host) {
        TelemetryMenu root = new TelemetryMenu("Calibration", telemetry);

        Map<String, List<CalibrationEntry>> grouped = collectCalibrations(robot);

        if (grouped.isEmpty()) {
            root.addItem(MenuItem.info("No @Calibration methods found."));
            return root;
        }

        root.addItem(MenuItem.action("Run All", () -> runAll(grouped, host)));
        root.addItem(MenuItem.info(""));

        for (Map.Entry<String, List<CalibrationEntry>> entry : grouped.entrySet()) {
            String path = entry.getKey();
            List<CalibrationEntry> calibrations = entry.getValue();

            TelemetryMenu sub = new TelemetryMenu(path, telemetry);
            sub.addItem(MenuItem.action("Run All in " + path, () -> runGroup(calibrations, host)));
            sub.addItem(MenuItem.info(""));

            for (CalibrationEntry c : calibrations) {
                sub.addItem(MenuItem.action(c.annotation.value(), () -> runSingle(c, host)));
            }

            root.addItem(MenuItem.action(path, () -> host.push(sub)));
        }

        return root;
    }


    private void runSingle(CalibrationEntry entry, MenuHost host) {
        CalibrationResult result = invoke(entry);
        showResult(entry.annotation.value(), result, host);
    }

    private void runGroup(List<CalibrationEntry> entries, MenuHost host) {
        for (CalibrationEntry entry : entries) {
            CalibrationResult result = invoke(entry);
            streamResult(entry, result);
        }
        telemetry.addLine("Group calibration complete.");
        telemetry.update();
    }

    private void runAll(Map<String, List<CalibrationEntry>> grouped, MenuHost host) {
        TelemetryMenu resultMenu = new TelemetryMenu("Results", telemetry);
        int success = 0, failed = 0, warned = 0;

        for (List<CalibrationEntry> entries : grouped.values()) {
            for (CalibrationEntry entry : entries) {
                CalibrationResult result = invoke(entry);
                streamResult(entry, result);

                String icon = resultIcon(result);
                resultMenu.addItem(MenuItem.info(icon + " " + entry.annotation.value()
                    + ": " + result.message));

                switch (result.status) {
                    case SUCCESS: success++; break;
                    case FAILED:  failed++;  break;
                    case WARN:    warned++;  break;
                }
            }
        }

        resultMenu.addItem(MenuItem.info(""));
        resultMenu.addItem(MenuItem.info("✓ " + success + "  ✗ " + failed + "  ⚠ " + warned));
        resultMenu.addItem(MenuItem.action("Back", host::back));
        host.push(resultMenu);
    }

    private void showResult(String name, CalibrationResult result, MenuHost host) {
        TelemetryMenu resultMenu = new TelemetryMenu(name, telemetry);
        resultMenu.addItem(MenuItem.info(resultIcon(result) + " " + result.message));

        for (CalibrationResult.Value v : result.values) {
            resultMenu.addItem(MenuItem.info(
                    v.fieldName + " = " + v.value
                    + (v.comment.isEmpty() ? "" : " (" + v.comment + ")")
            ));
        }

        resultMenu.addItem(MenuItem.info(""));
        resultMenu.addItem(MenuItem.action("Back", host::back));
        host.push(resultMenu);
    }

    private void streamResult(CalibrationEntry entry, CalibrationResult result) {
        if (!result.isSuccess() || result.values.isEmpty()) return;

        java.util.Map<String, Object> values = new java.util.LinkedHashMap<>();
        for (CalibrationResult.Value v : result.values) {
            values.put(v.fieldName, v.value);
        }

        StreamingLog.calibration(entry.instance.getClass().getName(), values);
    }


    private Map<String, List<CalibrationEntry>> collectCalibrations(Component root) {
        Map<String, List<CalibrationEntry>> grouped = new LinkedHashMap<>();
        walkComponent(root, grouped);
        return grouped;
    }

    private void walkComponent(Component component, Map<String, List<CalibrationEntry>> grouped) {
        if (!(component instanceof HardwareDevice)) {
            for (Component child : component.children()) {
                walkComponent(child, grouped);
            }
            return;
        }

        List<CalibrationEntry> entries = new ArrayList<>();

        for (Class<?> type = component.getClass();
             type != null && Component.class.isAssignableFrom(type);
             type = type.getSuperclass()) {
            for (Method method : type.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(Calibration.class)) continue;
                if (!method.getReturnType().equals(CalibrationResult.class)) continue;
                method.trySetAccessible();
                entries.add(new CalibrationEntry(method, component,
                        method.getAnnotation(Calibration.class)));
            }
        }

        if (!entries.isEmpty()) {
            String path = component.parent() == null ? "Robot" : component.path();
            grouped.put(path, entries);
        }

        for (Component child : component.children()) {
            walkComponent(child, grouped);
        }
    }

    private CalibrationResult invoke(CalibrationEntry entry) {
        try {
            Object result = entry.method.invoke(entry.instance);
            if (result instanceof CalibrationResult) return (CalibrationResult) result;
            return CalibrationResult.failed("Method did not return CalibrationResult");
        } catch (Exception e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return CalibrationResult.failed("Exception: " + cause.getMessage());
        }
    }

    private static String resultIcon(CalibrationResult result) {
        switch (result.status) {
            case SUCCESS: return "✓";
            case FAILED:  return "✗";
            case WARN:    return "⚠";
            default:      return "?";
        }
    }

    private static final class CalibrationEntry {
        final Method method;
        final Object instance;
        final Calibration annotation;

        CalibrationEntry(Method method, Object instance, Calibration annotation) {
            this.method     = method;
            this.instance   = instance;
            this.annotation = annotation;
        }
    }
}
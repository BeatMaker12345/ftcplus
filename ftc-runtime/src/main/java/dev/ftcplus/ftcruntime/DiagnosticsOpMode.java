package dev.ftcplus.ftcruntime;

import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import dev.ftcplus.core.Component;
import dev.ftcplus.core.Diagnostic;
import dev.ftcplus.core.DiagnosticResult;
import dev.ftcplus.core.Robot;
import dev.ftcplus.core.Runtime;
import dev.ftcplus.core.menu.MenuItem;
import dev.ftcplus.core.menu.MenuHost;
import dev.ftcplus.ftcruntime.menu.GamepadMenuInputSource;
import dev.ftcplus.ftcruntime.menu.TelemetryMenu;

@TeleOp(name = "Diagnostics", group = "FTC+")
public final class DiagnosticsOpMode extends FtcPlusAdvancedOpMode {

    @Override
    public void runOpMode() {
        Robot<?, ?, ?> robot = RobotResolver.resolve(RobotResolver.findRobotClasses());
        Runtime runtime = createRuntime(robot);

        MenuHost host = new MenuHost();

        TelemetryMenu root;

        try {
            runtime.initialize();
            root = buildMenu(robot, host);
        } catch (Exception e) {
            root = new TelemetryMenu("Diagnostics", telemetry);
            root.addItem(MenuItem.info("Initialization failed:"));
            root.addItem(MenuItem.info(e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName()));
            root.addItem(MenuItem.info(""));
            root.addItem(MenuItem.info("Check hardware connections and config names."));
        }

        host.setRoot(root);

        GamepadMenuInputSource input = new GamepadMenuInputSource(gamepad1, gamepad2);

        waitForStart();

        while (opModeIsActive() && host.isActive()) {
            input.update();
            host.update(input);
        }
    }


    private TelemetryMenu buildMenu(Robot<?, ?, ?> robot, MenuHost host) {
        TelemetryMenu root = new TelemetryMenu("Diagnostics", telemetry);

        Map<String, List<DiagnosticEntry>> grouped = collectDiagnostics(robot);

        if (grouped.isEmpty()) {
            root.addItem(MenuItem.info("No @Diagnostic methods found."));
            return root;
        }

        // top-level: run all
        root.addItem(MenuItem.action("Run All", () -> runAll(grouped, host)));
        root.addItem(MenuItem.info(""));

        for (Map.Entry<String, List<DiagnosticEntry>> entry : grouped.entrySet()) {
            String componentPath = entry.getKey();
            List<DiagnosticEntry> diagnostics = entry.getValue();

            TelemetryMenu sub = new TelemetryMenu(componentPath, telemetry);

            sub.addItem(MenuItem.action("Run All in " + componentPath,
                    () -> runGroup(diagnostics)));
            sub.addItem(MenuItem.info(""));

            for (DiagnosticEntry d : diagnostics) {
                sub.addItem(MenuItem.action(
                        d.annotation.name(),
                        () -> runSingle(d, host)
                ));
            }

            root.addItem(MenuItem.action(componentPath, () -> host.push(sub)));
        }

        return root;
    }


    private void runSingle(DiagnosticEntry entry, MenuHost host) {
        DiagnosticResult result = invoke(entry);
        TelemetryMenu resultMenu = new TelemetryMenu(entry.annotation.name(), telemetry);
        resultMenu.addItem(MenuItem.info(resultIcon(result) + " " + result.toString()));
        if (!result.message.isEmpty()) {
            resultMenu.addItem(MenuItem.info(result.message));
        }
        resultMenu.addItem(MenuItem.action("Back", host::back));
        host.push(resultMenu);
    }

    private void runGroup(List<DiagnosticEntry> entries) {
        telemetry.clearAll();
        telemetry.addLine("Running diagnostics...");
        telemetry.update();

        for (DiagnosticEntry entry : entries) {
            DiagnosticResult result = invoke(entry);
            telemetry.addLine(resultIcon(result) + " " + entry.annotation.name()
                    + (result.message.isEmpty() ? "" : ": " + result.message));
        }
        telemetry.update();
    }

    private void runAll(Map<String, List<DiagnosticEntry>> grouped, MenuHost host) {
        List<DiagnosticEntry> all = new ArrayList<>();
        for (List<DiagnosticEntry> entries : grouped.values()) all.addAll(entries);

        TelemetryMenu resultMenu = new TelemetryMenu("Results", telemetry);
        int passed = 0, failed = 0, warned = 0;

        for (DiagnosticEntry entry : all) {
            DiagnosticResult result = invoke(entry);
            resultMenu.addItem(MenuItem.info(
                    resultIcon(result) + " " + entry.annotation.name()
                    + (result.message.isEmpty() ? "" : ": " + result.message)
            ));
            switch (result.status) {
                case PASS: passed++; break;
                case FAIL: failed++; break;
                case WARN: warned++; break;
            }
        }

        resultMenu.addItem(MenuItem.info(""));
        resultMenu.addItem(MenuItem.info(
                "✓ " + passed + "  ✗ " + failed + "  ⚠ " + warned
        ));
        resultMenu.addItem(MenuItem.action("Back", host::back));
        host.push(resultMenu);
    }

    private DiagnosticResult invoke(DiagnosticEntry entry) {
        try {
            Object result = entry.method.invoke(entry.instance);
            if (result instanceof DiagnosticResult) return (DiagnosticResult) result;
            return DiagnosticResult.fail("Method did not return DiagnosticResult");
        } catch (Exception e) {
            return DiagnosticResult.fail("Exception: " + e.getMessage());
        }
    }

    private static String resultIcon(DiagnosticResult result) {
        switch (result.status) {
            case PASS: return "✓";
            case FAIL: return "✗";
            case WARN: return "⚠";
            default:   return "?";
        }
    }


    private Map<String, List<DiagnosticEntry>> collectDiagnostics(Component root) {
        Map<String, List<DiagnosticEntry>> grouped = new LinkedHashMap<>();
        walkComponent(root, grouped);
        return grouped;
    }

    private void walkComponent(Component component, Map<String, List<DiagnosticEntry>> grouped) {
        List<DiagnosticEntry> entries = new ArrayList<>();

        for (Class<?> type = component.getClass();
             type != null && Component.class.isAssignableFrom(type);
             type = type.getSuperclass()) {

            for (Method method : type.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(Diagnostic.class)) continue;
                if (!method.getReturnType().equals(DiagnosticResult.class)) continue;
                method.trySetAccessible();
                entries.add(new DiagnosticEntry(method, component,
                        method.getAnnotation(Diagnostic.class)));
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


    private static final class DiagnosticEntry {
        final Method method;
        final Object instance;
        final Diagnostic annotation;

        DiagnosticEntry(Method method, Object instance, Diagnostic annotation) {
            this.method = method;
            this.instance = instance;
            this.annotation = annotation;
        }
    }
}
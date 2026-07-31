package dev.ftcplus.sim;

import dev.ftcplus.runtime.OpMode;
import dev.ftcplus.sim.runner.SimOpModeRunner;

import java.util.List;

public final class SimLauncher {

    public static void main(String[] args) throws Exception {
        System.out.println("FTC+ Simulator");
        System.out.println("==============");

        List<Class<?>> opModes = discoverOpModes();

        if (opModes.isEmpty()) {
            System.out.println("No @OpMode.Register classes found.");
            System.out.println("Make sure the annotation processor has run (./gradlew :TeamCode:compileJava)");
            return;
        }

        Class<?> selected;
        if (opModes.size() == 1) {
            selected = opModes.get(0);
        } else {
            System.out.println("Available opmodes:");
            for (int i = 0; i < opModes.size(); i++) {
                OpMode.Register reg = opModes.get(i).getAnnotation(OpMode.Register.class);
                String name = reg != null ? reg.value() : opModes.get(i).getSimpleName();
                System.out.printf("  %d. %s%n", i + 1, name);
            }
            System.out.print("Select: ");
            int choice = new java.util.Scanner(System.in).nextInt() - 1;
            selected = opModes.get(Math.max(0, Math.min(opModes.size() - 1, choice)));
        }

        OpMode opMode = (OpMode) selected.getDeclaredConstructor().newInstance();
        System.out.println("Running: " + selected.getSimpleName());
        System.out.println("Dashboard: http://localhost:7273");
        System.out.println("Press Ctrl+C to stop.");

        SimOpModeRunner runner = new SimOpModeRunner(opMode, true);
        runner.init();
        runner.start();

        Runtime.getRuntime().addShutdownHook(new Thread(runner::stop));
        runner.run();
    }

    private static List<Class<?>> discoverOpModes() {
        try {
            Class<?> registry = Class.forName("dev.ftcplus.generated.FtcPlusRegistry");
            Class<?>[] opmodes = (Class<?>[]) registry.getMethod("getOpModes").invoke(null);
            return List.of(opmodes);
        } catch (Exception e) {
            System.err.println("No opmode registry found: " + e.getMessage());
            return List.of();
        }
    }
}
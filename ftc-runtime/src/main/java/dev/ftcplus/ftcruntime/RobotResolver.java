package dev.ftcplus.ftcruntime;

import dev.ftcplus.core.Robot;
import dev.ftcplus.core.UseRobot;

import java.util.ArrayList;
import java.util.List;

public final class RobotResolver {

    private RobotResolver() {}

    static String getSelectedRobotName(android.content.SharedPreferences prefs) {
        return prefs.getString("ftcplus_selected_robot", null);
    }

    static void saveSelectedRobotName(android.content.SharedPreferences prefs, String name) {
        prefs.edit().putString("ftcplus_selected_robot", name).apply();
    }

    static Robot<?, ?> resolveFromPrefs(
            List<Class<?>> robotClasses,
            android.content.SharedPreferences prefs,
            Class<?> requiredBy
    ) {
        if (robotClasses.isEmpty()) {
            throw new IllegalStateException("No @Robot class found.");
        }

        Class<?> selected = robotClasses.get(0);

        if (robotClasses.size() > 1) {
            String savedName = getSelectedRobotName(prefs);
            if (savedName != null) {
                for (Class<?> c : robotClasses) {
                    dev.ftcplus.core.TeamRobot annotation =
                            c.getAnnotation(dev.ftcplus.core.TeamRobot.class);
                    String name = annotation != null && !annotation.name().isEmpty()
                            ? annotation.name()
                            : c.getSimpleName();
                    if (name.equals(savedName)) {
                        selected = c;
                        break;
                    }
                }
            }
        }

        if (requiredBy != null) {
            UseRobot useRobot = requiredBy.getAnnotation(UseRobot.class);
            if (useRobot != null && !useRobot.value().isAssignableFrom(selected)) {
                throw new IllegalStateException(
                        requiredBy.getSimpleName() + " requires robot " +
                                useRobot.value().getSimpleName() +
                                " but active robot is " + selected.getSimpleName());
            }
        }

        try {
            return (Robot<?, ?>) selected.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to instantiate robot " + selected.getSimpleName(), e);
        }
    }

    public static List<Class<?>> findRobotClasses() {
        List<Class<?>> results = new ArrayList<>();
        try {
            Class<?> registry = Class.forName("dev.ftcplus.generated.FtcPlusRegistry");
            Class<?>[] robots = (Class<?>[]) registry.getMethod("getRobots").invoke(null);
            for (Class<?> c : robots) results.add(c);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "FtcPlusRegistry not found. Did the gradle plugin run?", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load robot registry", e);
        }
        return results;
    }

    static Robot<?, ?> resolve(List<Class<?>> robotClasses, Class<?> requiredBy) {
        if (robotClasses.isEmpty()) {
            throw new IllegalStateException(
                    "No @Robot class found. Annotate your Robot class with @Robot.");
        }

        Class<?> selected = robotClasses.get(0);

        // enforce @UseRobot if present
        if (requiredBy != null) {
            UseRobot useRobot = requiredBy.getAnnotation(UseRobot.class);
            if (useRobot != null && !useRobot.value().isAssignableFrom(selected)) {
                throw new IllegalStateException(
                        requiredBy.getSimpleName() + " requires robot " +
                                useRobot.value().getSimpleName() +
                                " but active robot is " + selected.getSimpleName());
            }
        }

        try {
            return (Robot<?, ?>) selected.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to instantiate robot " + selected.getSimpleName() +
                            ". Does it have a no-arg constructor?", e);
        }
    }

    public static Robot<?, ?> resolve(List<Class<?>> robotClasses) {
        return resolve(robotClasses, null);
    }
}
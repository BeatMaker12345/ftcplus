package dev.ftcplus.runtime;

import dev.ftcplus.core.Robot;
import dev.ftcplus.core.UseRobot;

import java.util.ArrayList;
import java.util.List;

public class RobotResolver {

    private RobotResolver() {}

    public static List<Class<?>> findRobotClasses() {
        List<Class<?>> results = new ArrayList<>();
        try {
            Class<?> registry = Class.forName("dev.ftcplus.generated.FtcPlusRegistry");
            Class<?>[] robots = (Class<?>[]) registry.getMethod("getRobots").invoke(null);
            for (Class<?> c : robots) results.add(c);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                "FtcPlusRegistry not found — did the annotation processor run?", e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load robot registry", e);
        }
        return results;
    }

    public static Robot<?, ?, ?> resolve(List<Class<?>> robotClasses) {
        return resolve(robotClasses, null);
    }

    public static Robot<?, ?, ?> resolve(List<Class<?>> robotClasses, Class<?> requiredBy) {
        if (robotClasses.isEmpty()) {
            throw new IllegalStateException(
                "No @TeamRobot class found — annotate your Robot class with @TeamRobot.");
        }

        Class<?> selected = selectRobot(robotClasses, requiredBy);

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
            return (Robot<?, ?, ?>) selected.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to instantiate robot " + selected.getSimpleName(), e);
        }
    }

    protected static Class<?> selectRobot(List<Class<?>> robotClasses, Class<?> requiredBy) {
        return robotClasses.get(0);
    }
}

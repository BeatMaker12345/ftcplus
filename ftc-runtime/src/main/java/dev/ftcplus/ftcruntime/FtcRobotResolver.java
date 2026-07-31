package dev.ftcplus.ftcruntime;

import android.content.SharedPreferences;
import dev.ftcplus.core.Robot;
import dev.ftcplus.core.TeamRobot;

import java.util.List;

final class FtcRobotResolver {
    private FtcRobotResolver() {}

    static Robot<?, ?, ?> resolveFromPrefs(
            List<Class<?>> robotClasses,
            SharedPreferences prefs,
            Class<?> requiredBy
    ) {
        if (robotClasses.isEmpty()) throw new IllegalStateException("No @TeamRobot class found.");

        Class<?> selected = robotClasses.get(0);

        if (robotClasses.size() > 1) {
            String savedName = prefs.getString("ftcplus_selected_robot", null);
            if (savedName != null) {
                for (Class<?> c : robotClasses) {
                    TeamRobot ann = c.getAnnotation(TeamRobot.class);
                    String name = ann != null && !ann.name().isEmpty() ? ann.name() : c.getSimpleName();
                    if (name.equals(savedName)) { selected = c; break; }
                }
            }
        }

        return RobotResolver.resolve(List.of(selected), requiredBy);
    }
}
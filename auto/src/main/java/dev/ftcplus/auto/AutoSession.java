package dev.ftcplus.auto;

import com.pedropathing.follower.Follower;
import dev.ftcplus.core.Robot;
import dev.ftcplus.core.Runtime;

import java.util.List;

public class AutoSession {
    private final Robot<?, ?> robot;
    private final Runtime runtime;
    private final Follower follower;
    private final List<AutoAction> actions;

    private int currentIndex = 0;
    private boolean started = false;

    public AutoSession(Robot<?, ?> robot, Runtime runtime, Follower follower, AutoPath path) {
        this.robot = robot;
        this.runtime = runtime;
        this.follower = follower;

        path.define();
        this.actions = path.actions();

        for (AutoAction action : actions) {
            action.attach(robot, follower);
        }
    }

    public void update() {
        if (isFinished()) return;

        runtime.update();
        follower.update();

        AutoAction current = actions.get(currentIndex);

        if (!started) {
            current.onStart();
            started = true;
        }

        current.onUpdate();

        if (current.isFinished()) {
            current.onEnd();
            currentIndex++;
            started = false;
        }
    }

    public boolean isFinished() { return currentIndex >= actions.size(); }
}
package dev.ftcplus.auto;

import dev.ftcplus.core.Robot;

import com.pedropathing.follower.Follower;
import com.pedropathing.paths.Path;

public abstract class AutoAction {
    private Robot<?, ?, ?> robot;
    private Follower follower;
    private boolean followingPath = false;

    final void attach(Robot<?, ?, ?> robot, Follower follower) {
        this.robot    = robot;
        this.follower = follower;
    }

    public abstract void onStart();
    public abstract boolean isFinished();
    public void onEnd() {}
    public void onUpdate() {}

    protected final void followPath(Path path) {

    }
}
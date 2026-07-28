package dev.ftcplus.limelight;

import dev.ftcplus.limelight.signal.AprilTagDetected;

import java.util.function.Function;

public final class AprilTagBinding {
    public enum Mode { EDGE, CONTINUOUS }

    public final int tagId;
    public final Mode mode;
    public final Function<AprilTagDetected, dev.ftcplus.core.signal.Event> signalFactory;

    private boolean wasVisible = false;

    AprilTagBinding(int tagId, Mode mode, Function<AprilTagDetected, dev.ftcplus.core.signal.Event> signalFactory) {
        this.tagId         = tagId;
        this.mode          = mode;
        this.signalFactory = signalFactory;
    }

    public boolean shouldFire(boolean currentlyVisible) {
        boolean fire = switch (mode) {
            case EDGE       -> currentlyVisible && !wasVisible;
            case CONTINUOUS -> currentlyVisible;
        };
        wasVisible = currentlyVisible;
        return fire;
    }
}
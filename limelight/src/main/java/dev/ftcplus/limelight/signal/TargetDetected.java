package dev.ftcplus.limelight.signal;

import dev.ftcplus.core.signal.Event;

public class TargetDetected extends Event {
    public final double yawDegrees;
    public final double pitchDegrees;

    public TargetDetected(double yawDegrees, double pitchDegrees) {
        this.yawDegrees = yawDegrees;
        this.pitchDegrees = pitchDegrees;
    }
}
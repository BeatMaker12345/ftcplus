package dev.ftcplus.limelight.signal;

import dev.ftcplus.core.signal.Event;

public class AprilTagDetected extends Event {
    public final int    tagId;
    public final double x;
    public final double y;
    public final double z;
    public final double yawDegrees;
    public final double confidence;
    public final double distanceInches;

    public AprilTagDetected(int tagId, double x, double y, double z, double yawDegrees, double confidence, double distanceInches) {
        this.tagId = tagId;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yawDegrees = yawDegrees;
        this.confidence = confidence;
        this.distanceInches = distanceInches;
    }
}
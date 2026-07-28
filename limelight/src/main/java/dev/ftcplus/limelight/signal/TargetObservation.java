package dev.ftcplus.limelight.signal;

import dev.ftcplus.core.signal.Message;

public class TargetObservation extends Message {
    public final double yawDegrees;
    public final double pitchDegrees;
    public final double areaPercent;
    public final double confidence;
    public final int    pipeline;

    public TargetObservation(double yawDegrees, double pitchDegrees, double areaPercent, double confidence, int pipeline) {
        this.yawDegrees = yawDegrees;
        this.pitchDegrees = pitchDegrees;
        this.areaPercent = areaPercent;
        this.confidence = confidence;
        this.pipeline = pipeline;
    }
}
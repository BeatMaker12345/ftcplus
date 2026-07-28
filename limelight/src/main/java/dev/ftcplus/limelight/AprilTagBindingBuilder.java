package dev.ftcplus.limelight;

import dev.ftcplus.core.signal.Event;
import dev.ftcplus.limelight.signal.AprilTagDetected;

import java.util.function.Function;
import java.util.function.Supplier;

public final class AprilTagBindingBuilder {
    private final int tagId;
    private final Limelight limelight;
    private AprilTagBinding.Mode mode = AprilTagBinding.Mode.EDGE;

    AprilTagBindingBuilder(int tagId, Limelight limelight) {
        this.tagId     = tagId;
        this.limelight = limelight;
    }

    public AprilTagBindingBuilder edge() {
        this.mode = AprilTagBinding.Mode.EDGE;
        return this;
    }

    public AprilTagBindingBuilder continuous() {
        this.mode = AprilTagBinding.Mode.CONTINUOUS;
        return this;
    }

    public void send(Function<AprilTagDetected, Event> factory) {
        limelight.addAprilTagBinding(new AprilTagBinding(tagId, mode, factory));
    }

    public void send(Supplier<Event> factory) {
        limelight.addAprilTagBinding(new AprilTagBinding(tagId, mode, detected -> factory.get()));
    }
}
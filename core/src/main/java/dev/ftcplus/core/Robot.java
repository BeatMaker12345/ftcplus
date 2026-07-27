package dev.ftcplus.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public abstract class Robot<H extends Enum<H> & HardwareEntry, G> extends Component {
    private final RobotConfiguration<H, G> config;

    protected Robot(RobotConfiguration<H, G> config) {
        this.config = config;
    }

    public final RobotConfiguration<H, G> config() {
        return config;
    }

    protected final <T extends Component> T register(T component) {
        return registerChild(component);
    }
    protected void defineTelemetry() {}
}
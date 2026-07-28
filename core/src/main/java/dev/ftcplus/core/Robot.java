package dev.ftcplus.core;

import dev.ftcplus.core.power.PowerBudget;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public abstract class Robot<
        H extends Enum<H> & HardwareEntry,
        G,
        P extends RobotProperties> extends Component {

    public final Class<H> hardware;
    public final G globals;
    public final P properties;
    private PowerBudget powerBudget;

    protected Robot(Class<H> hardware, G globals, P properties) {
        this.hardware   = hardware;
        this.globals    = globals;
        this.properties = properties;
    }

    final void initPowerBudget() {
        powerBudget = new PowerBudget(this,
                properties.maxCurrentAmps(),
                properties.nominalVoltage()
        );
    }

    public H hardware(String name) {
        for (H entry : hardware.getEnumConstants()) {
            if (entry.name().equalsIgnoreCase(name)) {
                return entry;
            }
        }
        return null;
    }

    protected final <T extends Component> T register(T component) {
        return registerChild(component);
    }
    protected void defineTelemetry() {}

    public final PowerBudget powerBudget() { return powerBudget; }
}
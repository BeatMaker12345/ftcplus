package dev.ftcplus.core;

public abstract class RobotConfiguration<H extends Enum<H> & HardwareEntry, G> {
    public final Class<H> hardware;
    public final G globals;

    protected RobotConfiguration(Class<H> hardware, G globals) {
        this.hardware = hardware;
        this.globals = globals;
    }

    public H hardware(String name) {
        for (H entry : hardware.getEnumConstants()) {
            if (entry.name().equalsIgnoreCase(name)) {
                return entry;
            }
        }
        return null;
    }
}

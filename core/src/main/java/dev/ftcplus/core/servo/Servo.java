package dev.ftcplus.core.servo;

import dev.ftcplus.core.Direction;
import dev.ftcplus.core.HardwareDevice;
import dev.ftcplus.core.HardwareEntry;

public abstract class Servo extends HardwareDevice {

    public static final double MIN_POSITION = 0.0;
    public static final double MAX_POSITION = 1.0;

    private final HardwareEntry entry;
    private final ServoSpec spec;

    private ServoDelegate delegate;

    protected Servo(HardwareEntry entry, ServoSpec spec) {
        this.entry = entry;
        this.spec = spec;
    }

    protected Servo(HardwareEntry entry) {
        this.entry = entry;
        this.spec = null;
    }

    @Override
    protected final void onInitialize() {
        delegate = deviceFactory().createServoDelegate(entry, spec);
        onServoInitialize();
    }

    protected void onServoInitialize() {}

    public final void setPosition(double position) { delegate.setPosition(position); }
    public final double getPosition()              { return delegate.getPosition(); }
    public final void setDirection(Direction d)    { delegate.setDirection(d); }
    public final Direction getDirection()          { return delegate.getDirection(); }
    public final void scaleRange(double min, double max) { delegate.scaleRange(min, max); }
    public final String getDeviceName()            { return delegate.getDeviceName(); }

    public final boolean hasSpec() { return spec != null; }

    public final ServoSpec spec() {
        requireSpec();
        return spec;
    }

    public final void moveToDegrees(double degrees) {
        requireSpec();
        double position = degrees / spec.travelDegrees;
        setPosition(Math.max(MIN_POSITION, Math.min(MAX_POSITION, position)));
    }

    public final double getAngle() {
        requireSpec();
        return getPosition() * spec.travelDegrees;
    }

    public final void togglePosition(double a, double b) {
        togglePosition(a, b, 0.02);
    }

    public final void togglePosition(double a, double b, double tolerance) {
        if (isAtPosition(a, tolerance)) setPosition(b);
        else setPosition(a);
    }

    public final boolean isAtPosition(double position, double tolerance) {
        return Math.abs(getPosition() - position) <= tolerance;
    }

    public final boolean isAtPosition(double position) {
        return isAtPosition(position, 0.02);
    }

    private void requireSpec() {
        if (spec == null) {
            throw new IllegalStateException(
                    "This servo was instantiated without a ServoSpec. " +
                            "Extended Methods are unavailable."
            );
        }
    }
}
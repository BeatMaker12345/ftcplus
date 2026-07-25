package dev.ftcplus.core.servo;

import dev.ftcplus.core.Direction;
import dev.ftcplus.core.HardwareDevice;
import dev.ftcplus.core.HardwareEntry;

public abstract class CRServo extends HardwareDevice {

    private final HardwareEntry entry;
    private final CRServoSpec spec;

    private CRServoDelegate delegate;

    protected CRServo(HardwareEntry entry, CRServoSpec spec) {
        this.entry = entry;
        this.spec = spec;
    }

    protected CRServo(HardwareEntry entry) {
        this.entry = entry;
        this.spec = null;
    }

    @Override
    protected final void onInitialize() {
        delegate = deviceFactory().createCRServoDelegate(entry, spec);
        onCRServoInitialize();
    }

    protected void onCRServoInitialize() {}

    public final void setPower(double power)    { delegate.setPower(power); }
    public final double getPower()              { return delegate.getPower(); }
    public final void setDirection(Direction d) { delegate.setDirection(d); }
    public final Direction getDirection()       { return delegate.getDirection(); }
    public final String getDeviceName()         { return delegate.getDeviceName(); }

    public final boolean hasSpec() { return spec != null; }

    public final CRServoSpec spec() {
        requireSpec();
        return spec;
    }

    public final double estimateCurrentDraw() {
        requireSpec();
        double power = Math.abs(getPower());
        return spec.freeCurrentAmps + power * (spec.stallCurrentAmps - spec.freeCurrentAmps);
    }

    private void requireSpec() {
        if (spec == null) {
            throw new IllegalStateException(
                    "This CRServo was instantiated without a CRServoSpec. " +
                            "Extended Methods are unavailable."
            );
        }
    }
}
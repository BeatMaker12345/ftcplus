package dev.ftcplus.core.motor;

import dev.ftcplus.core.Direction;
import dev.ftcplus.core.HardwareDevice;
import dev.ftcplus.core.HardwareEntry;

public abstract class Motor extends HardwareDevice {

    private final HardwareEntry entry;
    private final MotorSpec spec;

    private MotorDelegate delegate;

    protected Motor(HardwareEntry entry, MotorSpec spec) {
        this.entry = entry;
        this.spec = spec;
    }

    protected Motor(HardwareEntry entry) {
        this.entry = entry;
        this.spec = null;
    }

    @Override
    protected final void onInitialize() {
        delegate = deviceFactory().createMotorDelegate(entry, spec);
        onMotorInitialize();
    }

    protected void onMotorInitialize() {}


    public final void setPower(double power) {
        delegate.setPower(power);
    }

    public final double getPower() {
        return delegate.getPower();
    }

    public final void setDirection(Direction direction) {
        delegate.setDirection(direction);
    }

    public final Direction getDirection() {
        return delegate.getDirection();
    }

    public final void setMode(RunMode mode) {
        delegate.setMode(mode);
    }

    public final RunMode getMode() {
        return delegate.getMode();
    }

    public final void setTargetPosition(int ticks) {
        delegate.setTargetPosition(ticks);
    }

    public final int getTargetPosition() {
        return delegate.getTargetPosition();
    }

    public final int getCurrentPosition() {
        return delegate.getCurrentPosition();
    }

    public final boolean isBusy() {
        return delegate.isBusy();
    }

    public final void setZeroPowerBehavior(ZeroPowerBehavior behavior) {
        delegate.setZeroPowerBehavior(behavior);
    }

    public final ZeroPowerBehavior getZeroPowerBehavior() {
        return delegate.getZeroPowerBehavior();
    }

    public final boolean getPowerFloat() {
        return delegate.getPowerFloat();
    }

    public final String getDeviceName() {
        return delegate.getDeviceName();
    }


    public final boolean hasSpec() {
        return spec != null;
    }

    public final MotorSpec spec() {
        requireSpec();
        return spec;
    }

    public final void rotateDegrees(double degrees) {
        requireSpec();
        int ticks = (int) Math.round((degrees / 360.0) * spec.ticksPerRevolution);
        setMode(RunMode.STOP_AND_RESET_ENCODER);
        setTargetPosition(ticks);
        setMode(RunMode.RUN_TO_POSITION);
    }

    public final double estimateCurrentDraw() {
        requireSpec();
        double power = Math.abs(getPower());
        return spec.freeCurrentAmps + power * (spec.stallCurrentAmps - spec.freeCurrentAmps);
    }

    public final double getTicksPerRevolution() {
        requireSpec();
        return spec.ticksPerRevolution;
    }

    private void requireSpec() {
        if (spec == null) {
            throw new IllegalStateException(
                    "This motor was instantiated without a MotorSpec. " +
                            "Extended Methods are unavailable."
            );
        }
    }

    @Override
    public double estimatedCurrentDraw() {
        return hasSpec() ? estimateCurrentDraw() : 0;
    }
}
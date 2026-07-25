package dev.ftcplus.core.sensor;

import dev.ftcplus.core.HardwareDevice;
import dev.ftcplus.core.HardwareEntry;

public class Imu extends HardwareDevice {

    private final HardwareEntry entry;
    private final ImuOrientation orientation;
    private ImuDelegate delegate;

    public Imu(HardwareEntry entry, ImuOrientation orientation) {
        this.entry = entry;
        this.orientation = orientation;
    }

    public Imu(HardwareEntry entry) {
        this.entry = entry;
        this.orientation = ImuOrientation.LOGO_FACING_UP;
    }

    @Override
    protected final void onInitialize() {
        delegate = deviceFactory().createImuDelegate(entry, orientation);
        onImuInitialize();
    }

    protected void onImuInitialize() {}

    public final double getYaw()   { return delegate.getYaw(); }
    public final double getPitch() { return delegate.getPitch(); }
    public final double getRoll()  { return delegate.getRoll(); }
    public final void resetYaw()   { delegate.resetYaw(); }
}
package dev.ftcplus.sim.devices;

import dev.ftcplus.core.DeviceFactory;
import dev.ftcplus.core.HardwareEntry;
import dev.ftcplus.core.motor.MotorDelegate;
import dev.ftcplus.core.motor.MotorSpec;
import dev.ftcplus.core.sensor.ImuDelegate;
import dev.ftcplus.core.sensor.ImuOrientation;
import dev.ftcplus.core.servo.CRServoDelegate;
import dev.ftcplus.core.servo.CRServoSpec;
import dev.ftcplus.core.servo.ServoDelegate;
import dev.ftcplus.core.servo.ServoSpec;

import java.util.ArrayList;
import java.util.List;

public final class SimDeviceFactory implements DeviceFactory {

    private final List<SimMotorDelegate>   motors   = new ArrayList<>();
    private final List<SimServoDelegate>   servos   = new ArrayList<>();
    private final List<SimCRServoDelegate> crServos = new ArrayList<>();
    private final List<SimImuDelegate>     imus     = new ArrayList<>();

    @Override
    public MotorDelegate createMotorDelegate(HardwareEntry entry, MotorSpec spec) {
        SimMotorDelegate d = new SimMotorDelegate(spec);
        motors.add(d);
        return d;
    }

    @Override
    public MotorDelegate createMotorDelegate(HardwareEntry entry) {
        return createMotorDelegate(entry, null);
    }

    @Override
    public ServoDelegate createServoDelegate(HardwareEntry entry, ServoSpec spec) {
        SimServoDelegate d = new SimServoDelegate(spec);
        servos.add(d);
        return d;
    }

    @Override
    public ServoDelegate createServoDelegate(HardwareEntry entry) {
        return createServoDelegate(entry, null);
    }

    @Override
    public CRServoDelegate createCRServoDelegate(HardwareEntry entry, CRServoSpec spec) {
        SimCRServoDelegate d = new SimCRServoDelegate(spec);
        crServos.add(d);
        return d;
    }

    @Override
    public CRServoDelegate createCRServoDelegate(HardwareEntry entry) {
        return createCRServoDelegate(entry, null);
    }

    @Override
    public ImuDelegate createImuDelegate(HardwareEntry entry, ImuOrientation orientation) {
        SimImuDelegate d = new SimImuDelegate();
        imus.add(d);
        return d;
    }

    public void update() {
        motors.forEach(SimMotorDelegate::update);
        servos.forEach(SimServoDelegate::update);
        crServos.forEach(SimCRServoDelegate::update);
    }

    public List<SimMotorDelegate>   motors()   { return motors; }
    public List<SimServoDelegate>   servos()   { return servos; }
    public List<SimCRServoDelegate> crServos() { return crServos; }
    public List<SimImuDelegate>     imus()     { return imus; }
}

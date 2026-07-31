package dev.ftcplus.core;

public abstract class Drive<S extends Enum<S>> extends Subsystem<S> {
    public abstract void setInputs(double forward, double strafe, double turn);
}
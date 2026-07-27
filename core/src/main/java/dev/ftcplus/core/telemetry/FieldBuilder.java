package dev.ftcplus.core.telemetry;

import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

public interface FieldBuilder {
    FieldBuilder robot(Supplier<double[]> poseXYHeading);
    FieldBuilder robot(String name, Supplier<double[]> poseXYHeading);

    FieldBuilder point(String name, Supplier<double[]> xyzSupplier);
    FieldBuilder point(String name, double x, double y, double z);

    FieldBuilder axis(String name, DoubleSupplier value, FieldAxis axis);

    FieldBuilder line(String name, Supplier<double[]> from, Supplier<double[]> to);
}
package dev.ftcplus.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CalibrationResult {
    public enum Status { SUCCESS, FAILED, WARN }

    public static final class Value {
        public final String fieldName;
        public final Object value;
        public final String comment;

        Value(String fieldName, Object value, String comment) {
            this.fieldName = fieldName;
            this.value     = value;
            this.comment   = comment;
        }
    }

    public final Status status;
    public final String message;
    public final List<Value> values;

    private CalibrationResult(Status status, String message, List<Value> values) {
        this.status  = status;
        this.message = message;
        this.values  = Collections.unmodifiableList(values);
    }


    public static CalibrationResult offset(String fieldName, int ticks) {
        return new CalibrationResult(
                Status.SUCCESS,
                fieldName + " = " + ticks,
                List.of(new Value(fieldName, ticks, "encoder offset (ticks"))
        );
    }

    public static CalibrationResult offset(String fieldName, double value) {
        return new CalibrationResult(
                Status.SUCCESS,
                fieldName + " = " + value,
                List.of(new Value(fieldName, value, "calibrated value"))
        );
    }

    public static CalibrationResult values(Value... vals) {
        List<Value> list = new ArrayList<>();
        Collections.addAll(list, vals);
        return new CalibrationResult(Status.SUCCESS, "Calibration complete", list);
    }

    public static CalibrationResult failed(String reason) {
        return new CalibrationResult(Status.FAILED, reason, List.of());
    }

    public static CalibrationResult warn(String message, Value... vals) {
        List<Value> list = new ArrayList<>();
        Collections.addAll(list, vals);
        return new CalibrationResult(Status.WARN, message, list);
    }

    public static Value value(String fieldName, int v)                    { return new Value(fieldName, v, null); }
    public static Value value(String fieldName, double v)                 { return new Value(fieldName, v, null); }
    public static Value value(String fieldName, boolean v)                { return new Value(fieldName, v, null); }
    public static Value value(String fieldName, Object v, String comment) { return new Value(fieldName, v, comment); }

    public boolean isSuccess() { return status == Status.SUCCESS; }
}
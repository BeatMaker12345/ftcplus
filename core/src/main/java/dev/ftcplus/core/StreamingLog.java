package dev.ftcplus.core;

public final class StreamingLog {

    public static final String SETTINGS_TAG     = "FTCPLUS_SETTINGS";
    public static final String CALIBRATION_TAG  = "FTCPLUS_CALIBRATION";

    private StreamingLog() {}

    public static void setting(String className, String fieldName, Object value) {
        String json = "{\"class\":\"" + className + "\","
                + "\"field\":\"" + fieldName + "\","
                + "\"value\":" + value + "}";
        log(SETTINGS_TAG, json);
    }

    public static void calibration(String className, java.util.Map<String, Object> values) {
        StringBuilder json = new StringBuilder();
        json.append("{\"class\":\"").append(className).append("\",\"values\":{");
        boolean first = true;
        for (java.util.Map.Entry<String, Object> e : values.entrySet()) {
            if (!first) json.append(",");
            json.append("\"").append(e.getKey()).append("\":").append(e.getValue());
            first = false;
        }
        json.append("}}");
        log(CALIBRATION_TAG, json.toString());
    }

    private static void log(String tag, String message) {
        try {
            Class<?> logClass = Class.forName("android.util.Log");
            logClass.getMethod("i", String.class, String.class).invoke(null, tag, message);
        } catch (Exception e) {
            System.out.println(tag + ": " + message);
        }
    }
}
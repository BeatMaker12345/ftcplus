package dev.ftcplus.core;

public final class DiagnosticResult {
    public enum Status { PASS, FAIL, WARN }

    public final Status status;
    public final String message;

    private DiagnosticResult(Status status, String message) {
        this.status = status;
        this.message = message;
    }

    public static DiagnosticResult pass() {
        return new DiagnosticResult(Status.PASS, "");
    }

    public static DiagnosticResult pass(String message) {
        return new DiagnosticResult(Status.PASS, message);
    }

    public static DiagnosticResult fail(String message) {
        return new DiagnosticResult(Status.FAIL, message);
    }

    public static DiagnosticResult warn(String message) {
        return new DiagnosticResult(Status.WARN, message);
    }

    public boolean isPassing() { return status == Status.PASS; }

    @Override
    public String toString() {
        return status + (message.isEmpty() ? "" : ": " + message);
    }
}
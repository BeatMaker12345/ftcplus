package dev.ftcplus.core.diagnostic;

import dev.ftcplus.core.DiagnosticResult;

public final class AssertionBuilder {
    private final double actual;
    private String failureMessage = null;
    private boolean warnOnly = false;

    public AssertionBuilder(double actual) {
        this.actual = actual;
    }


    public TerminalAssertion toBe(double expected) {
        return new TerminalAssertion(
                Math.abs(actual - expected) < 1e-9,
                "Expected " + expected + " but got " + actual,
                failureMessage, warnOnly
        );
    }

    public WithinBuilder within(double tolerance) {
        return new WithinBuilder(actual, tolerance, failureMessage, warnOnly);
    }

    public TerminalAssertion greaterThan(double min) {
        return new TerminalAssertion(
                actual > min,
                "Expected > " + min + " but got " + actual,
                failureMessage, warnOnly
        );
    }

    public TerminalAssertion lessThan(double max) {
        return new TerminalAssertion(
                actual < max,
                "Expected < " + max + " but got " + actual,
                failureMessage, warnOnly
        );
    }

    public TerminalAssertion between(double min, double max) {
        return new TerminalAssertion(
                actual >= min && actual <= max,
                "Expected between " + min + " and " + max + " but got " + actual,
                failureMessage, warnOnly
        );
    }


    public AssertionBuilder otherwise(String message) {
        this.failureMessage = message;
        return this;
    }

    public AssertionBuilder warn() {
        this.warnOnly = true;
        return this;
    }


    public static final class WithinBuilder {
        private final double actual;
        private final double tolerance;
        private final String customMessage;
        private final boolean warnOnly;

        WithinBuilder(double actual, double tolerance, String customMessage, boolean warnOnly) {
            this.actual = actual;
            this.tolerance = tolerance;
            this.customMessage = customMessage;
            this.warnOnly = warnOnly;
        }

        public TerminalAssertion of(double expected) {
            boolean pass = Math.abs(actual - expected) <= tolerance;
            String defaultMsg = "Expected " + actual + " within " + tolerance + " of " + expected;
            return new TerminalAssertion(pass, defaultMsg, customMessage, warnOnly);
        }
    }

    public static final class TerminalAssertion {
        private final boolean passed;
        private final String defaultMessage;
        private String customMessage;
        private boolean warnOnly;

        TerminalAssertion(boolean passed, String defaultMessage, String customMessage, boolean warnOnly) {
            this.passed = passed;
            this.defaultMessage = defaultMessage;
            this.customMessage = customMessage;
            this.warnOnly = warnOnly;
        }

        public TerminalAssertion otherwise(String message) {
            this.customMessage = message;
            return this;
        }

        public TerminalAssertion warn() {
            this.warnOnly = true;
            return this;
        }

        public DiagnosticResult result() {
            if (passed) return DiagnosticResult.pass(defaultMessage);
            String msg = customMessage != null ? customMessage : defaultMessage;
            return warnOnly ? DiagnosticResult.warn(msg) : DiagnosticResult.fail(msg);
        }
    }
}
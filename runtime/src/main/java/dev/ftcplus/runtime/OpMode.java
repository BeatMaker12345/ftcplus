package dev.ftcplus.runtime;

import dev.ftcplus.core.Runtime;
import dev.ftcplus.runtime.controls.OpModeControls;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public abstract class OpMode {

    private OpModeControls controls;
    private Runtime runtime;

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface Register {
        String value();
        String group() default "FTC+";
    }

    protected abstract void configure();

    protected final OpModeControls controls() {
        return controls;
    }

    protected final Runtime runtime() {
        return runtime;
    }

    public final void attachRuntime(Runtime runtime, OpModeControls controls) {
        this.runtime  = runtime;
        this.controls = controls;
        configure();
    }
}

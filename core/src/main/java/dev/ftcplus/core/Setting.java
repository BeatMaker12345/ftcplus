package dev.ftcplus.core;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Setting {
    String name();
    double step()     default 0.05;
    double min()      default -Double.MAX_VALUE;
    double max()      default Double.MAX_VALUE;
    boolean persist() default true;
}
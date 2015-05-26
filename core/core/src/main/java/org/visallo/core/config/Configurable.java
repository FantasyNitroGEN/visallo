package org.visallo.core.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD})
public @interface Configurable {
    static final String DEFAULT_NAME = "__DEFAULT__";
    static final String DEFAULT_VALUE = "__FAIL__";

    String name() default DEFAULT_NAME;

    String defaultValue() default DEFAULT_VALUE;

    boolean required() default true;
}

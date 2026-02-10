package com.neo4j.loopy.cli;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark fields in LoopyConfig that should generate CLI options.
 * This annotation is processed at compile-time to generate Picocli @Option annotations.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.SOURCE)
public @interface CliOption {
    
    /**
     * The option names, including short and long forms.
     * Example: {"--neo4j-uri", "-u"}
     */
    String[] names();
    
    /**
     * Description of the option shown in help text.
     */
    String description() default "";
    
    /**
     * Whether this option is required.
     */
    boolean required() default false;
    
    /**
     * Whether this option is hidden from help output.
     */
    boolean hidden() default false;
    
    /**
     * Regular expression pattern for validation.
     */
    String pattern() default "";
    
    /**
     * Minimum value for numeric options.
     */
    double min() default Double.NEGATIVE_INFINITY;
    
    /**
     * Maximum value for numeric options.
     */
    double max() default Double.POSITIVE_INFINITY;
    
    /**
     * Environment variable name that can override this option.
     */
    String envVar() default "";
}
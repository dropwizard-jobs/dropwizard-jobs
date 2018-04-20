package io.dropwizard.jobs.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Delay the start of a job which has the {@link Every @Every} annotation. Without a delay the job will
 * start immediately.
 * 
 * @author Martin Charlesworth
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DelayStart {
    String value();
}

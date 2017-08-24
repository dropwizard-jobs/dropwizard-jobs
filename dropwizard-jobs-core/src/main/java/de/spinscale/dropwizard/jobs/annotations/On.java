package de.spinscale.dropwizard.jobs.annotations;

import org.quartz.Trigger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface On {

    enum MisfirePolicy {
        SMART,
        IGNORE_MISFIRES,
        DO_NOTHING,
        FIRE_AND_PROCEED
    }

    String value() default "";

    /**
     * The name of this job. If not specified, the name of the job will default to the canonical name of the annotated
     * class
     */
    String jobName() default "";

    boolean requestRecovery() default false;

    boolean storeDurably() default false;

    int priority() default Trigger.DEFAULT_PRIORITY;

    MisfirePolicy misfirePolicy() default MisfirePolicy.SMART;

}

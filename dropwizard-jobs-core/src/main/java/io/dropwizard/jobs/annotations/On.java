package io.dropwizard.jobs.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.quartz.Trigger;

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
     * @return the name of the job
     */
    String jobName() default "";

    String timeZone() default "";

    boolean requestRecovery() default false;

    boolean storeDurably() default false;

    int priority() default Trigger.DEFAULT_PRIORITY;

    MisfirePolicy misfirePolicy() default MisfirePolicy.SMART;

}

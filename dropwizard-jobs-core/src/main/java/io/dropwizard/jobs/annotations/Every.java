package io.dropwizard.jobs.annotations;

import org.quartz.Trigger;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Every {

    enum MisfirePolicy {
        SMART,
        IGNORE_MISFIRES,
        FIRE_NOW,
        NOW_WITH_EXISTING_COUNT,
        NOW_WITH_REMAINING_COUNT,
        NEXT_WITH_EXISTING_COUNT,
        NEXT_WITH_REMAINING_COUNT,
    }

    String value() default "";

    /**
     * The name of this job. If not specified, the name of the job will default to the canonical name of the annotated
     * class
     * @return the name of the job
     */
    String jobName() default "";

    int repeatCount() default -1;

    boolean requestRecovery() default false;

    boolean storeDurably() default false;

    int priority() default Trigger.DEFAULT_PRIORITY;

    MisfirePolicy misfirePolicy() default MisfirePolicy.SMART;

}

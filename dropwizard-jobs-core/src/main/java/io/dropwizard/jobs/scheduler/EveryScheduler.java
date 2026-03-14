package io.dropwizard.jobs.scheduler;

import io.dropwizard.jobs.Job;
import io.dropwizard.jobs.JobMediator;
import io.dropwizard.jobs.ScheduledJob;
import io.dropwizard.jobs.annotations.DelayStart;
import io.dropwizard.jobs.annotations.Every;
import io.dropwizard.jobs.parser.TimeParserUtil;
import org.quartz.JobKey;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import java.time.Instant;
import java.util.Date;
import java.util.stream.Stream;

import static io.dropwizard.jobs.scheduler.AnnotationReader.resolveDurationExpression;

/**
 * Scheduler strategy for jobs annotated with {@link Every @Every}.
 * <p>
 * This scheduler handles jobs that need to run repeatedly at fixed intervals.
 * It creates Quartz triggers with simple schedules based on the annotation values.
 * </p>
 * <p>
 * <strong>Features supported:</strong>
 * </p>
 * <ul>
 *   <li>Configurable interval via annotation value or configuration override</li>
 *   <li>Optional start delay via {@link DelayStart @DelayStart}</li>
 *   <li>Configurable repeat count (default: repeat forever)</li>
 *   <li>Configurable misfire policy</li>
 *   <li>Job priority</li>
 *   <li>Job recovery and durability settings</li>
 * </ul>
 *
 * @see Every
 * @see DelayStart
 * @see JobScheduler
 */
public class EveryScheduler extends JobScheduler {

    /**
     * Creates a new EveryScheduler.
     *
     * @param mediator the job mediator providing access to configuration and scheduling operations
     */
    public EveryScheduler(JobMediator mediator) {
        super(mediator);
    }

    @Override
    public void schedule() {
        this.scheduledJobs()
                .forEach(mediator::scheduleOrRescheduleJob);
    }

    // ========================================================================
    // JOB STREAM CREATION
    // Converts @Every jobs to ScheduledJob instances with triggers
    // ========================================================================

    /**
     * Creates a stream of scheduled jobs from all jobs annotated with {@code @Every}.
     *
     * @return stream of scheduled jobs ready for Quartz scheduling
     */
    protected Stream<ScheduledJob> scheduledJobs() {
        return mediator.getJobs()
                .allEvery()
                .map(job -> {
                    Class<? extends Job> clazz = job.getJobClass();
                    Every everyAnnotation = clazz.getAnnotation(Every.class);

                    long interval = getInterval(everyAnnotation, clazz);
                    SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInMilliseconds(interval);

                    int repeatCount = everyAnnotation.repeatCount();
                    applyRepeatCount(repeatCount, scheduleBuilder);

                    Every.MisfirePolicy misfirePolicy = everyAnnotation.misfirePolicy();
                    applyMisfirePolicy(misfirePolicy, scheduleBuilder);

                    Instant start = extractStart(clazz);
                    int priority = everyAnnotation.priority();
                    Trigger trigger = TriggerBuilder.newTrigger().withSchedule(scheduleBuilder)
                            .startAt(Date.from(start))
                            .withPriority(priority)
                            .build();

                    // ensure that only one instance of each job is scheduled
                    JobKey jobKey = createJobKey(everyAnnotation.jobName(), job);
                    String message = extractMessage(clazz, jobKey);
                    boolean requestRecovery = everyAnnotation.requestRecovery();
                    boolean storeDurably = everyAnnotation.storeDurably();
                    return new ScheduledJob(jobKey, clazz, trigger, requestRecovery, storeDurably, message);
                });
    }

    // ========================================================================
    // INTERVAL RESOLUTION
    // Parses interval from annotation or configuration
    // ========================================================================

    /**
     * Determines the interval in milliseconds from the annotation value.
     * <p>
     * If the value is empty or a placeholder expression, it will be resolved
     * from the configuration.
     * </p>
     *
     * @param everyAnnotation the annotation containing the interval value
     * @param clazz the job class (for configuration lookup)
     * @return the interval in milliseconds
     */
    private long getInterval(Every everyAnnotation, Class<? extends Job> clazz) {
        String value = everyAnnotation.value();
        String expression = resolveDurationExpression(value, clazz, mediator.getConfiguration());
        return TimeParserUtil.parseDuration(expression);
    }

    // ========================================================================
    // SCHEDULE CONFIGURATION
    // Applies repeat count and misfire policy to schedule builder
    // ========================================================================

    /**
     * Applies the repeat count to the schedule builder.
     *
     * @param repeatCount the number of times to repeat (-1 for forever)
     * @param scheduleBuilder the schedule builder to configure
     */
    private void applyRepeatCount(int repeatCount, SimpleScheduleBuilder scheduleBuilder) {
        if (repeatCount > -1)
            scheduleBuilder.withRepeatCount(repeatCount);
        else
            scheduleBuilder.repeatForever();
    }

    /**
     * Applies the misfire policy to the schedule builder.
     *
     * @param misfirePolicy the misfire policy from the annotation
     * @param scheduleBuilder the schedule builder to configure
     */
    private void applyMisfirePolicy(Every.MisfirePolicy misfirePolicy, SimpleScheduleBuilder scheduleBuilder) {
        switch (misfirePolicy) {
            case IGNORE_MISFIRES:
                scheduleBuilder.withMisfireHandlingInstructionIgnoreMisfires();
                break;
            case FIRE_NOW:
                scheduleBuilder.withMisfireHandlingInstructionFireNow();
                break;
            case NOW_WITH_EXISTING_COUNT:
                scheduleBuilder.withMisfireHandlingInstructionNowWithExistingCount();
                break;
            case NOW_WITH_REMAINING_COUNT:
                scheduleBuilder.withMisfireHandlingInstructionNowWithRemainingCount();
                break;
            case NEXT_WITH_EXISTING_COUNT:
                scheduleBuilder.withMisfireHandlingInstructionNextWithExistingCount();
                break;
            case NEXT_WITH_REMAINING_COUNT:
                scheduleBuilder.withMisfireHandlingInstructionNextWithRemainingCount();
                break;
            case SMART:
                break;
            default:
                log.warn("Nothing to do for the misfire policy: {}", misfirePolicy);
                break;
        }
    }

    // ========================================================================
    // TRIGGER CONFIGURATION
    // Extracts start time and builds log messages
    // ========================================================================

    /**
     * Extracts the start time for a job, accounting for any delay.
     *
     * @param clazz the job class to check for {@code @DelayStart}
     * @return the instant when the job should start
     */
    private Instant extractStart(Class<? extends Job> clazz) {
        Instant start = Instant.now();
        DelayStart delayAnnotation = clazz.getAnnotation(DelayStart.class);
        if (delayAnnotation != null) {
            long milliSecondDelay = TimeParserUtil.parseDuration(delayAnnotation.value());
            start = start.plusMillis(milliSecondDelay);
        }
        return start;
    }

    /**
     * Builds a log message for the scheduled job.
     *
     * @param clazz the job class
     * @param jobKey the job key
     * @return a formatted message for logging
     */
    private String extractMessage(Class<? extends Job> clazz, JobKey jobKey) {
        DelayStart delayAnnotation = clazz.getAnnotation(DelayStart.class);
        Every everyAnnotation = clazz.getAnnotation(Every.class);
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("    %-7s %s", everyAnnotation.value(), jobKey.toString()));
        if (delayAnnotation != null) {
            sb.append(" (").append(delayAnnotation.value()).append(" delay)");
        }
        return sb.toString();
    }
}

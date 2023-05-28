package io.dropwizard.jobs.scheduler;

import io.dropwizard.jobs.Job;
import io.dropwizard.jobs.JobConfiguration;
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

import static io.dropwizard.jobs.scheduler.AnnotationReader.readDurationFromConfig;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class EveryScheduler extends JobScheduler {
    public EveryScheduler(JobMediator mediator) {
        super(mediator);
    }

    @Override
    public void schedule() {
        this.scheduledJobs()
                .forEach(mediator::scheduleOrRescheduleJob);
    }

    protected Stream<ScheduledJob> scheduledJobs() {
        return mediator.getJobs()
                .allEvery()
                .map(job -> {
                    Class<? extends Job> clazz = job.getClass();
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

    private long getInterval(Every everyAnnotation, Class<? extends Job> clazz) {
        String value = everyAnnotation.value();
        String expression = durationOrPlainExpression(value, clazz);
        return TimeParserUtil.parseDuration(expression);
    }

    protected String durationOrPlainExpression(String expression, Class<? extends Job> clazz) {
        if (expression.isEmpty() || expression.matches("\\$\\{.*\\}")) {
            JobConfiguration configuration = mediator.getConfiguration();
            String fromConfig = readDurationFromConfig(expression, clazz, configuration);
            expression = !isEmpty(fromConfig) ? fromConfig : expression;
            log.info(clazz + " is configured in the config file to run every " + expression);
        }
        return expression;
    }

    private void applyRepeatCount(int repeatCount, SimpleScheduleBuilder scheduleBuilder) {
        if (repeatCount > -1)
            scheduleBuilder.withRepeatCount(repeatCount);
        else
            scheduleBuilder.repeatForever();
    }

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
            default:
                log.warn("Nothing to do for the misfire policy: {}", misfirePolicy);
                break;
        }
    }

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

    private Instant extractStart(Class<? extends Job> clazz) {
        Instant start = Instant.now();
        DelayStart delayAnnotation = clazz.getAnnotation(DelayStart.class);
        if (delayAnnotation != null) {
            long milliSecondDelay = TimeParserUtil.parseDuration(delayAnnotation.value());
            start = start.plusMillis(milliSecondDelay);
        }
        return start;
    }

}

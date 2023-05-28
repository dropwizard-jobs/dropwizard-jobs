package io.dropwizard.jobs.scheduler;

import io.dropwizard.jobs.Job;
import io.dropwizard.jobs.JobConfiguration;
import io.dropwizard.jobs.JobMediator;
import io.dropwizard.jobs.ScheduledJob;
import io.dropwizard.jobs.annotations.On;
import org.apache.commons.lang3.StringUtils;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import java.time.ZoneId;
import java.util.TimeZone;
import java.util.stream.Stream;

public class OnCronScheduler extends EveryScheduler {

    private final CronExpressionParser cronExpressionParser;

    public OnCronScheduler(JobMediator mediator) {
        super(mediator);
        JobConfiguration configuration = mediator.getConfiguration();
        this.cronExpressionParser = new CronExpressionParser(configuration);
    }

    protected Stream<ScheduledJob> scheduledJobs() {
        return mediator.getJobs()
                .allOnCron()
                .map(job -> {

                    Class<? extends Job> clazz = job.getClass();
                    On onAnnotation = clazz.getAnnotation(On.class);
                    String value = onAnnotation.value();
                    String cronExpression = durationOrPlainExpression(value, clazz);

                    boolean requestRecovery = onAnnotation.requestRecovery();
                    boolean storeDurably = onAnnotation.storeDurably();

                    CronScheduleBuilder scheduleBuilder = cronExpressionParser.parse(cronExpression);

                    String timeZoneStr = onAnnotation.timeZone();
                    applyTimezone(timeZoneStr, scheduleBuilder);

                    On.MisfirePolicy misfirePolicy = onAnnotation.misfirePolicy();
                    applyMisfirePolicy(misfirePolicy, scheduleBuilder);

                    int priority = onAnnotation.priority();
                    Trigger trigger = TriggerBuilder.newTrigger()
                            .withSchedule(scheduleBuilder)
                            .withPriority(priority)
                            .build();

                    // ensure that only one instance of each job is scheduled
                    JobKey jobKey = createJobKey(onAnnotation.jobName(), job);

                    String message = String.format("    %-21s %s", cronExpression, jobKey.toString());
                    return new ScheduledJob(jobKey, clazz, trigger, requestRecovery, storeDurably, message);
                });
    }

    private void applyTimezone(String timeZoneStr, CronScheduleBuilder scheduleBuilder) {
        if (StringUtils.isNotBlank(timeZoneStr)) {
            TimeZone timeZone = TimeZone.getTimeZone(ZoneId.of(timeZoneStr));
            scheduleBuilder.inTimeZone(timeZone);
        }
    }

    private void applyMisfirePolicy(On.MisfirePolicy misfirePolicy, CronScheduleBuilder scheduleBuilder) {
        if (misfirePolicy == On.MisfirePolicy.IGNORE_MISFIRES)
            scheduleBuilder.withMisfireHandlingInstructionIgnoreMisfires();
        else if (misfirePolicy == On.MisfirePolicy.DO_NOTHING)
            scheduleBuilder.withMisfireHandlingInstructionDoNothing();
        else if (misfirePolicy == On.MisfirePolicy.FIRE_AND_PROCEED)
            scheduleBuilder.withMisfireHandlingInstructionFireAndProceed();
    }


}

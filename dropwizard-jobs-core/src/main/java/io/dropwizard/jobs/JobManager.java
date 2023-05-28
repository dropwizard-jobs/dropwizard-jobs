package io.dropwizard.jobs;

import io.dropwizard.jobs.annotations.*;
import io.dropwizard.jobs.annotations.Every.MisfirePolicy;
import io.dropwizard.jobs.parser.TimeParserUtil;
import io.dropwizard.lifecycle.Managed;
import org.apache.commons.lang3.StringUtils;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.spi.JobFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.dropwizard.jobs.AnnotationReader.readDurationFromConfig;

public class JobManager implements Managed {

    protected static final Logger log = LoggerFactory.getLogger(JobManager.class);

    protected Job[] jobs;
    protected Scheduler scheduler;
    protected JobConfiguration configuration;

    protected TimeZone defaultTimezone;

    public JobManager(JobConfiguration configuration, Job... jobs) {
        this.configuration = configuration;
        this.jobs = jobs;
        if (configuration != null && configuration.getQuartzConfiguration().containsKey("de.spinscale.dropwizard.jobs.timezone")) {
            defaultTimezone = TimeZone.getTimeZone(configuration.getQuartzConfiguration().get("de.spinscale.dropwizard.jobs.timezone"));
        } else {
            defaultTimezone = TimeZone.getDefault();
        }
    }

    private static JobDetail build(Job job) {
        Class<? extends Job> jobClass = job.getClass();
        String jobClassName = jobClass.getName();
        String jobGroupName = job.getGroupName();
        return JobBuilder
                .newJob(jobClass)
                .withIdentity(jobClassName, jobGroupName)
                .build();
    }

    private static JobDetail build(ScheduledJob job) {
        JobKey jobKey = job.getJobKey();
        return JobBuilder.newJob(job.getClazz())
                .withIdentity(jobKey)
                .requestRecovery(job.isRequestsRecovery())
                .storeDurably(job.isStoreDurably())
                .build();
    }


    @Override
    public void start() throws Exception {
        scheduler = createScheduler();
        scheduler.setJobFactory(getJobFactory());
        scheduler.start();
        scheduleAllJobs();
        logAllOnApplicationStopJobs();
    }

    private Scheduler createScheduler() throws SchedulerException {
        if (configuration.getQuartzConfiguration().isEmpty()) {
            return StdSchedulerFactory.getDefaultScheduler();
        }

        StdSchedulerFactory factory = new StdSchedulerFactory();
        factory.initialize(createProperties());
        return factory.getScheduler();
    }

    @Override
    public void stop() throws Exception {
        scheduleAllJobsOnApplicationStop();

        // this is enough to put the job into the queue, otherwise the jobs wont
        // be executed, anyone got a better solution?
        Thread.sleep(100);

        scheduler.shutdown(true);
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    protected JobFactory getJobFactory() {
        return new DropwizardJobFactory(jobs);
    }

    protected void scheduleAllJobs() throws SchedulerException {
        scheduleAllJobsOnApplicationStart();
        allJobsWithEveryAnnotation()
                .forEach(this::scheduleOrRescheduleJob);
        allJobsWithOnAnnotation()
                .forEach(this::scheduleOrRescheduleJob);
    }

    protected void scheduleAllJobsOnApplicationStop() throws SchedulerException {
        List<JobDetail> jobDetails = Arrays.stream(jobs)
                .filter(job -> job.getClass().isAnnotationPresent(OnApplicationStop.class))
                .map(JobManager::build)
                .collect(Collectors.toList());
        for (JobDetail jobDetail : jobDetails) {
            scheduleNow(jobDetail);
        }
    }

    private void scheduleNow(JobDetail jobDetail) throws SchedulerException {
        Trigger nowTrigger = nowTrigger();
        scheduler.scheduleJob(jobDetail, nowTrigger);
    }

    /**
     * Allow timezone to be configured on a per-cron basis with [timezoneName] appended to the cron format
     *
     * @param cronExpr the modified cron format
     * @return the cron schedule with the timezone applied to it if needed
     */
    protected CronScheduleBuilder createCronScheduleBuilder(String cronExpr) {
        int i = cronExpr.indexOf("[");
        int j = cronExpr.indexOf("]");
        TimeZone timezone = defaultTimezone;
        if (i > -1 && j > -1) {
            timezone = TimeZone.getTimeZone(cronExpr.substring(i + 1, j));
            cronExpr = cronExpr.substring(0, i).trim();
        }
        return CronScheduleBuilder.cronSchedule(cronExpr).inTimeZone(timezone);
    }

    protected Stream<ScheduledJob> allJobsWithOnAnnotation() {
        return Arrays.stream(this.jobs)
                .filter(job -> job.getClass().isAnnotationPresent(On.class))
                .map(job -> {

                    Class<? extends Job> clazz = job.getClass();
                    On onAnnotation = clazz.getAnnotation(On.class);
                    String cron = onAnnotation.value();

                    if (cron.isEmpty() || cron.matches("\\$\\{.*\\}")) {
                        cron = readDurationFromConfig(onAnnotation, clazz, configuration);
                        log.info(clazz + " is configured in the config file to run every " + cron);
                    }

                    boolean requestRecovery = onAnnotation.requestRecovery();
                    boolean storeDurably = onAnnotation.storeDurably();

                    CronScheduleBuilder scheduleBuilder = createCronScheduleBuilder(cron);

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

                    String message = String.format("    %-21s %s", cron, jobKey.toString());
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

    private JobKey createJobKey(final String annotationJobName, final Job job) {
        String key = StringUtils.isNotBlank(annotationJobName) ? annotationJobName : job.getClass().getCanonicalName();
        return JobKey.jobKey(key, job.getGroupName());
    }

    protected Stream<ScheduledJob> allJobsWithEveryAnnotation() {
        return Arrays.stream(this.jobs)
                .filter(job -> job.getClass().isAnnotationPresent(Every.class))
                .map(job -> {
                    Class<? extends Job> clazz = job.getClass();
                    Every everyAnnotation = clazz.getAnnotation(Every.class);

                    long interval = getInterval(clazz, everyAnnotation);
                    SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                            .withIntervalInMilliseconds(interval);

                    int repeatCount = everyAnnotation.repeatCount();
                    applyRepeatCount(repeatCount, scheduleBuilder);

                    MisfirePolicy misfirePolicy = everyAnnotation.misfirePolicy();
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

    private long getInterval(Class<? extends Job> clazz, Every everyAnnotation) {
        String value = everyAnnotation.value();
        if (value.isEmpty() || value.matches("\\$\\{.*\\}")) {
            value = readDurationFromConfig(everyAnnotation, clazz, configuration);
            log.info(clazz + " is configured in the config file to run every " + value);
        }
        return TimeParserUtil.parseDuration(value);
    }

    private void applyRepeatCount(int repeatCount, SimpleScheduleBuilder scheduleBuilder) {
        if (repeatCount > -1)
            scheduleBuilder.withRepeatCount(repeatCount);
        else
            scheduleBuilder.repeatForever();
    }

    private void applyMisfirePolicy(MisfirePolicy misfirePolicy, SimpleScheduleBuilder scheduleBuilder) {
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

    protected void scheduleAllJobsOnApplicationStart() throws SchedulerException {
        List<JobDetail> jobDetails = Arrays.stream(this.jobs)
                .filter(job -> job.getClass().isAnnotationPresent(OnApplicationStart.class))
                .map(JobManager::build)
                .collect(Collectors.toList());

        if (!jobDetails.isEmpty()) {
            log.info("Jobs to run on application start:");
            for (JobDetail jobDetail : jobDetails) {
                scheduler.scheduleJob(jobDetail, Set.of(nowTrigger()), true);
                log.info("   " + jobDetail.getJobClass().getCanonicalName());
            }
        }
    }

    protected Trigger nowTrigger() {
        return TriggerBuilder.newTrigger().startNow().build();
    }


    private Properties createProperties() {
        Properties properties = new Properties();
        properties.putAll(configuration.getQuartzConfiguration());
        return properties;
    }

    private void logAllOnApplicationStopJobs() {
        log.info("Jobs to run on application stop:");

        Arrays.stream(this.jobs)
                .filter(job -> job.getClass().isAnnotationPresent(OnApplicationStop.class))
                .map(Job::getClass)
                .forEach(clazz -> log.info("   " + clazz.getCanonicalName()));
    }

    private void scheduleOrRescheduleJob(ScheduledJob job) {
        JobKey jobKey = job.getJobKey();
        Trigger trigger = job.getTrigger();
        JobDetail jobDetail = build(job);

        try {
            if (scheduler.checkExists(jobKey)) {
                // if the job has exactly one trigger, we can just reschedule it, which allows us to update the schedule for
                // that trigger.
                List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
                if (triggers.size() == 1) {
                    scheduler.rescheduleJob(triggers.get(0).getKey(), trigger);
                    log.info(job.getMessage());
                } else {
                    // if for some reason the job has multiple triggers, it's easiest to just delete and re-create the job,
                    // since we want to enforce a one-to-one relationship between jobs and triggers
                    scheduler.deleteJob(jobKey);
                    scheduler.scheduleJob(jobDetail, trigger);
                    log.info(job.getMessage());
                }
            } else {
                // if the job doesn't already exist, we can create it, along with its trigger. this prevents us
                // from creating multiple instances of the same job when running in a clustered environment
                scheduler.scheduleJob(jobDetail, trigger);
                log.info("scheduled job with key {}", jobKey.toString());
                log.info(job.getMessage());
            }
        } catch (SchedulerException e) {
            log.error(String.format("error occurred scheduling the job %s", jobKey), e);
        }

    }
}

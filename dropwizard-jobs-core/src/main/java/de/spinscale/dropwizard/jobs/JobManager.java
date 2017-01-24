package de.spinscale.dropwizard.jobs;

import de.spinscale.dropwizard.jobs.annotations.DelayStart;
import de.spinscale.dropwizard.jobs.annotations.Every;
import de.spinscale.dropwizard.jobs.annotations.On;
import de.spinscale.dropwizard.jobs.annotations.OnApplicationStart;
import de.spinscale.dropwizard.jobs.annotations.OnApplicationStop;
import de.spinscale.dropwizard.jobs.parser.TimeParserUtil;
import io.dropwizard.lifecycle.Managed;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.spi.JobFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class JobManager implements Managed {

    protected static final Logger log = LoggerFactory.getLogger(JobManager.class);

    protected Job[] jobs;
    protected Scheduler scheduler;
    protected JobConfiguration configuration;

    public JobManager(Job ... jobs) {
        this.jobs = jobs;
    }

    public void configure(JobConfiguration configuration) {
        this.configuration = configuration;
    }

    @Override
    public void start() throws Exception {
        scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.setJobFactory(getJobFactory());
        scheduler.start();
        scheduleAllJobs();
        logAllOnApplicationStopJobs();
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
        scheduleAllJobsWithEveryAnnotation();
        scheduleAllJobsWithOnAnnotation();
    }

    protected void scheduleAllJobsOnApplicationStop() throws SchedulerException {
        List<JobDetail> jobDetails = Arrays.stream(jobs)
                .filter(job -> job.getClass().isAnnotationPresent(OnApplicationStop.class))
                .map(job -> JobBuilder.newJob(job.getClass()).build())
                .collect(Collectors.toList());
        for (JobDetail jobDetail : jobDetails) {
            scheduler.scheduleJob(jobDetail, executeNowTrigger());
        }
    }

    protected void scheduleAllJobsWithOnAnnotation() throws SchedulerException {
        List<Class<? extends Job>> onJobClasses = Arrays.stream(this.jobs)
                .filter(job -> job.getClass().isAnnotationPresent(On.class))
                .map(job -> job.getClass())
                .collect(Collectors.toList());

        if (onJobClasses.isEmpty()) {
            return;
        }

        log.info("Jobs with @On annotation:");
        for (Class<? extends org.quartz.Job> clazz : onJobClasses) {
            On onAnnotation = clazz.getAnnotation(On.class);
            String cron = onAnnotation.value();
            String key = StringUtils.isNotBlank(onAnnotation.jobName()) ? onAnnotation.jobName() : clazz.getCanonicalName();
            int priority = onAnnotation.priority();
            On.MisfirePolicy misfirePolicy = onAnnotation.misfirePolicy();
            boolean requestRecovery = onAnnotation.requestRecovery();
            boolean storeDurably = onAnnotation.storeDurably();

            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cron);
            if (misfirePolicy == On.MisfirePolicy.IGNORE_MISFIRES) scheduleBuilder.withMisfireHandlingInstructionIgnoreMisfires();
            else if (misfirePolicy == On.MisfirePolicy.DO_NOTHING) scheduleBuilder.withMisfireHandlingInstructionDoNothing();
            else if (misfirePolicy == On.MisfirePolicy.FIRE_AND_PROCEED) scheduleBuilder.withMisfireHandlingInstructionFireAndProceed();

            Trigger trigger = TriggerBuilder.newTrigger().withSchedule(scheduleBuilder).withPriority(priority).build();

            // ensure that only one instance of each job is scheduled
            JobKey jobKey = JobKey.jobKey(key);
            createOrUpdateJob(jobKey, clazz, trigger, requestRecovery, storeDurably);
            log.info(String.format("    %-21s %s", cron, jobKey.toString()));
        }
    }

    protected void scheduleAllJobsWithEveryAnnotation() throws SchedulerException {
        List<Class<? extends Job>> everyJobClasses = Arrays.stream(this.jobs)
                .filter(job -> job.getClass().isAnnotationPresent(Every.class))
                .map(job -> job.getClass())
                .collect(Collectors.toList());

        if (everyJobClasses.isEmpty()) {
            return;
        }

        log.info("Jobs with @Every annotation:");
        for (Class<? extends org.quartz.Job> clazz : everyJobClasses) {
            Every everyAnnotation = clazz.getAnnotation(Every.class);
            int priority = everyAnnotation.priority();
            Every.MisfirePolicy misfirePolicy = everyAnnotation.misfirePolicy();
            boolean requestRecovery = everyAnnotation.requestRecovery();
            boolean storeDurably = everyAnnotation.storeDurably();
            int repeatCount = everyAnnotation.repeatCount();

            String value = everyAnnotation.value();
            if (value.isEmpty() || value.matches("\\$\\{.*\\}")) {
                value = readDurationFromConfig(everyAnnotation, clazz);
                log.info(clazz + " is configured in the config file to run every " + value);
            }
            long milliSeconds = TimeParserUtil.parseDuration(value);

            SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInMilliseconds(milliSeconds);

            if (repeatCount > -1) scheduleBuilder.withRepeatCount(repeatCount);
            else scheduleBuilder.repeatForever();

            if (misfirePolicy == Every.MisfirePolicy.IGNORE_MISFIRES) scheduleBuilder.withMisfireHandlingInstructionIgnoreMisfires();
            else if (misfirePolicy == Every.MisfirePolicy.FIRE_NOW) scheduleBuilder.withMisfireHandlingInstructionFireNow();
            else if (misfirePolicy == Every.MisfirePolicy.NOW_WITH_EXISTING_COUNT) scheduleBuilder.withMisfireHandlingInstructionNowWithExistingCount();
            else if (misfirePolicy == Every.MisfirePolicy.NOW_WITH_REMAINING_COUNT) scheduleBuilder.withMisfireHandlingInstructionNowWithRemainingCount();
            else if (misfirePolicy == Every.MisfirePolicy.NEXT_WITH_EXISTING_COUNT) scheduleBuilder.withMisfireHandlingInstructionNextWithExistingCount();
            else if (misfirePolicy == Every.MisfirePolicy.NEXT_WITH_REMAINING_COUNT) scheduleBuilder.withMisfireHandlingInstructionNextWithRemainingCount();

            DateTime start = new DateTime();
            DelayStart delayAnnotation = clazz.getAnnotation(DelayStart.class);
            if (delayAnnotation != null) {
                long milliSecondDelay = TimeParserUtil.parseDuration(delayAnnotation.value());
                start = start.plus(Duration.millis(milliSecondDelay));
            }

            Trigger trigger = TriggerBuilder.newTrigger().withSchedule(scheduleBuilder)
                    .startAt(start.toDate())
                    .withPriority(priority)
                    .build();

            // ensure that only one instance of each job is scheduled
            String key = StringUtils.isNotBlank(everyAnnotation.jobName()) ? everyAnnotation.jobName() : clazz.getCanonicalName();
            JobKey jobKey = JobKey.jobKey(key);
            createOrUpdateJob(jobKey, clazz, trigger, requestRecovery, storeDurably);

            String logMessage = String.format("    %-7s %s", everyAnnotation.value(), jobKey.toString());
            if (delayAnnotation != null) {
                logMessage += " (" + delayAnnotation.value() + " delay)";
            }
            log.info(logMessage);
        }
    }

    @SuppressWarnings("unchecked")
    private String readDurationFromConfig(Every annotation, Class<? extends org.quartz.Job> clazz) {
        if (configuration == null) {
            return null;
        }
        String property = WordUtils.uncapitalize(clazz.getSimpleName());
        if (!annotation.value().isEmpty()) {
            property = annotation.value().substring(2, annotation.value().length() - 1);
        }
        return configuration.getJobs().getOrDefault(property, null);
    }

    protected void scheduleAllJobsOnApplicationStart() throws SchedulerException {
        List<JobDetail> jobDetails = Arrays.stream(this.jobs)
                .filter(job -> job.getClass().isAnnotationPresent(OnApplicationStart.class))
                .map(job -> JobBuilder.newJob(job.getClass()).build())
                .collect(Collectors.toList());

        if (!jobDetails.isEmpty()) {
            log.info("Jobs to run on application start:");
            for (JobDetail jobDetail : jobDetails) {
                scheduler.scheduleJob(jobDetail, executeNowTrigger());
                log.info("   " + jobDetail.getJobClass().getCanonicalName());
            }
        }
    }

    protected Trigger executeNowTrigger() {
        return TriggerBuilder.newTrigger().startNow().build();
    }

    private void logAllOnApplicationStopJobs() {
        log.info("Jobs to run on application stop:");

        Arrays.stream(this.jobs)
                .filter(job -> job.getClass().isAnnotationPresent(OnApplicationStop.class))
                .map(job -> job.getClass())
                .forEach(clazz -> log.info("   " + clazz.getCanonicalName()));
    }

    private void createOrUpdateJob(JobKey jobKey, Class<? extends org.quartz.Job> clazz, Trigger trigger,
                                   boolean requestsRecovery, boolean storeDurably) throws SchedulerException {
        JobBuilder jobBuilder = JobBuilder.newJob(clazz).withIdentity(jobKey).requestRecovery(requestsRecovery)
                .storeDurably(storeDurably);

        if (!scheduler.checkExists(jobKey)) {
            // if the job doesn't already exist, we can create it, along with its trigger. this prevents us
            // from creating multiple instances of the same job when running in a clustered environment
            scheduler.scheduleJob(jobBuilder.build(), trigger);
            log.info("scheduled job with key " + jobKey.toString());
        } else {
            // if the job has exactly one trigger, we can just reschedule it, which allows us to update the schedule for
            // that trigger.
            List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
            if (triggers.size() == 1) {
                scheduler.rescheduleJob(triggers.get(0).getKey(), trigger);
            } else {
                // if for some reason the job has multiple triggers, it's easiest to just delete and re-create the job,
                // since we want to enforce a one-to-one relationship between jobs and triggers
                scheduler.deleteJob(jobKey);
                scheduler.scheduleJob(jobBuilder.build(), trigger);
            }
        }
    }
}

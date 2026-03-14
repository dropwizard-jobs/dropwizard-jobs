package io.dropwizard.jobs;

import io.dropwizard.jobs.scheduler.EveryScheduler;
import io.dropwizard.jobs.scheduler.OnApplicationStartScheduler;
import io.dropwizard.jobs.scheduler.OnApplicationStopScheduler;
import io.dropwizard.jobs.scheduler.OnCronScheduler;
import io.dropwizard.lifecycle.Managed;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.spi.JobFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;
import java.util.Set;

public class JobManager implements Managed, JobMediator {

    protected static final Logger log = LoggerFactory.getLogger(JobManager.class);

    /**
     * Grace period in milliseconds to allow in-flight job executions to begin completion
     * before initiating scheduler shutdown. This delay ensures that stop jobs scheduled
     * via {@code @OnApplicationStop} have time to be queued for execution.
     */
    private static final long SHUTDOWN_GRACE_PERIOD_MS = 100;

    protected final JobConfiguration configuration;
    protected final JobFilters jobs;

    private final OnApplicationStartScheduler onApplicationStartScheduler;
    private final OnApplicationStopScheduler onApplicationStopScheduler;
    private final EveryScheduler everyScheduler;
    private final OnCronScheduler onCronScheduler;

    protected Scheduler scheduler;

    public JobManager(JobConfiguration configuration, List<Job> jobs) {
        this.configuration = configuration;
        this.jobs = new JobFilters(jobs);

        this.onApplicationStartScheduler = new OnApplicationStartScheduler(this);
        this.onApplicationStopScheduler = new OnApplicationStopScheduler(this);
        this.everyScheduler = new EveryScheduler(this);
        this.onCronScheduler = new OnCronScheduler(this);

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

        onApplicationStartScheduler.schedule();
        everyScheduler.schedule();
        onCronScheduler.schedule();

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
        onApplicationStopScheduler.schedule();

        // Allow a brief grace period for @OnApplicationStop jobs to be queued before shutdown.
        // Without this delay, the scheduler may shut down before the jobs are executed.
        Thread.sleep(SHUTDOWN_GRACE_PERIOD_MS);

        scheduler.shutdown(true);
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public JobFilters getJobs() {
        return jobs;
    }

    @Override
    public JobConfiguration getConfiguration() {
        return configuration;
    }

    protected JobFactory getJobFactory() {
        return new DropwizardJobFactory(jobs);
    }

    public void scheduleNow(JobDetail jobDetail) throws SchedulerException {
        Trigger nowTrigger = nowTrigger();
        scheduler.scheduleJob(jobDetail, Set.of(nowTrigger), true);
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

        jobs.allOnApplicationStop()
                .map(Job::getClass)
                .forEach(clazz -> log.info("   {}", clazz.getCanonicalName()));
    }

    /**
     * Schedules or reschedules a job with Quartz. This method handles three scenarios:
     * <ul>
     *   <li>Job exists with one trigger: reschedules the existing trigger</li>
     *   <li>Job exists with multiple triggers: deletes and recreates the job (enforces 1:1 job-to-trigger relationship)</li>
     *   <li>Job doesn't exist: creates a new job with its trigger</li>
     * </ul>
     * <p>
     * <strong>Note:</strong> {@link SchedulerException} is caught and logged at WARN level but not rethrown.
     * This is intentional for cluster resilience — if this node fails to schedule the job, another node
     * in the cluster may succeed. This prevents a single scheduling failure from cascading into a
     * broader application failure.
     *
     * @param job the scheduled job to schedule or reschedule
     */
    public void scheduleOrRescheduleJob(ScheduledJob job) {
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
                    log.info("Rescheduled job: {}", job.getMessage());
                } else {
                    // if for some reason the job has multiple triggers, it's easiest to just delete and re-create the job,
                    // since we want to enforce a one-to-one relationship between jobs and triggers
                    scheduler.deleteJob(jobKey);
                    scheduler.scheduleJob(jobDetail, trigger);
                    log.info("Scheduled job: {}", job.getMessage());
                }
            } else {
                // if the job doesn't already exist, we can create it, along with its trigger. this prevents us
                // from creating multiple instances of the same job when running in a clustered environment
                scheduler.scheduleJob(jobDetail, trigger);
                log.info("Scheduled job: {}", job.getMessage());
            }
        } catch (SchedulerException e) {
            log.warn("Failed to schedule job with key '{}': {}", jobKey, e.getMessage(), e);
        }

    }
}

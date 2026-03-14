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

/**
 * Manages the lifecycle and scheduling of Quartz jobs in a Dropwizard application.
 * <p>
 * This class implements the Dropwizard {@link Managed} interface, allowing it to be
 * managed by the Dropwizard lifecycle. It creates and configures a Quartz scheduler,
 * schedules jobs based on their annotations, and handles graceful shutdown.
 * </p>
 * <p>
 * The job manager supports four types of job scheduling via annotations:
 * </p>
 * <ul>
 *   <li>{@code @OnApplicationStart} - Jobs that run once when the application starts</li>
 *   <li>{@code @OnApplicationStop} - Jobs that run once when the application stops</li>
 *   <li>{@code @Every} - Jobs that run repeatedly at a fixed interval</li>
 *   <li>{@code @On} - Jobs that run based on cron expressions</li>
 * </ul>
 * <p>
 * <strong>Extension for Dependency Injection:</strong>
 * </p>
 * <p>
 * For dependency injection frameworks (Guice, Spring, HK2), extend this class and
 * override {@link #getJobFactory()} to provide a custom job factory that supports
 * dependency injection. See {@code GuiceJobManager} and {@code SpringJobManager} for examples.
 * </p>
 * <p>
 * <strong>Architecture Notes:</strong>
 * </p>
 * <ul>
 *   <li>This class follows the Mediator pattern via {@link JobMediator} to coordinate with
 *       scheduler strategy classes in the {@code scheduler/} sub-package</li>
 *   <li>Each scheduler type ({@link EveryScheduler}, {@link OnCronScheduler}, etc.) handles
 *       one annotation type following the Strategy pattern</li>
 *   <li>The class is organized into sections: Lifecycle, Configuration, Scheduling, and Utilities</li>
 * </ul>
 *
 * @see JobsBundle
 * @see JobConfiguration
 * @see Job
 * @see io.dropwizard.jobs.scheduler.JobScheduler
 */
public class JobManager implements Managed, JobMediator {

    protected static final Logger log = LoggerFactory.getLogger(JobManager.class);

    /**
     * Grace period in milliseconds to allow in-flight job executions to begin completion
     * before initiating scheduler shutdown. This delay ensures that stop jobs scheduled
     * via {@code @OnApplicationStop} have time to be queued for execution.
     */
    private static final long SHUTDOWN_GRACE_PERIOD_MS = 100;

    // ========================================================================
    // FIELDS
    // ========================================================================

    protected final JobConfiguration configuration;
    protected final JobFilters jobs;

    private final OnApplicationStartScheduler onApplicationStartScheduler;
    private final OnApplicationStopScheduler onApplicationStopScheduler;
    private final EveryScheduler everyScheduler;
    private final OnCronScheduler onCronScheduler;

    protected Scheduler scheduler;

    // ========================================================================
    // CONSTRUCTION
    // ========================================================================

    /**
     * Creates a new JobManager with the specified configuration and jobs.
     *
     * @param configuration the application configuration containing job and Quartz settings
     * @param jobs the list of jobs to be scheduled
     */
    public JobManager(JobConfiguration configuration, List<Job> jobs) {
        this.configuration = configuration;
        this.jobs = new JobFilters(jobs);

        this.onApplicationStartScheduler = new OnApplicationStartScheduler(this);
        this.onApplicationStopScheduler = new OnApplicationStopScheduler(this);
        this.everyScheduler = new EveryScheduler(this);
        this.onCronScheduler = new OnCronScheduler(this);
    }

    // ========================================================================
    // LIFECYCLE MANAGEMENT
    // Handles the Dropwizard Managed interface: start() and stop()
    // ========================================================================

    /**
     * Starts the Quartz scheduler and schedules all configured jobs.
     * <p>
     * This method is called by Dropwizard during application startup. It:
     * </p>
     * <ol>
     *   <li>Creates and configures the Quartz scheduler</li>
     *   <li>Sets the job factory for dependency injection</li>
     *   <li>Starts the scheduler</li>
     *   <li>Schedules all {@code @OnApplicationStart}, {@code @Every}, and {@code @On} jobs</li>
     *   <li>Logs all registered {@code @OnApplicationStop} jobs</li>
     * </ol>
     *
     * @throws Exception if the scheduler fails to start
     */
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

    /**
     * Stops the Quartz scheduler after running all {@code @OnApplicationStop} jobs.
     * <p>
     * This method is called by Dropwizard during application shutdown. It:
     * </p>
     * <ol>
     *   <li>Schedules all {@code @OnApplicationStop} jobs</li>
     *   <li>Waits for a brief grace period to allow stop jobs to be queued</li>
     *   <li>Shuts down the scheduler, waiting for running jobs to complete</li>
     * </ol>
     *
     * @throws Exception if the scheduler fails to stop
     */
    @Override
    public void stop() throws Exception {
        onApplicationStopScheduler.schedule();

        // Allow a brief grace period for @OnApplicationStop jobs to be queued before shutdown.
        // Without this delay, the scheduler may shut down before the jobs are executed.
        Thread.sleep(SHUTDOWN_GRACE_PERIOD_MS);

        scheduler.shutdown(true);
    }

    // ========================================================================
    // CONFIGURATION
    // Scheduler creation, properties, and job factory
    // ========================================================================

    /**
     * Creates and configures the Quartz scheduler.
     * <p>
     * If custom Quartz configuration is provided via {@link JobConfiguration#getQuartzConfiguration()},
     * it will be used to initialize the scheduler. Otherwise, the default scheduler configuration
     * (RAMJobStore) is used.
     * </p>
     *
     * @return a configured Quartz scheduler
     * @throws SchedulerException if the scheduler cannot be created
     */
    private Scheduler createScheduler() throws SchedulerException {
        if (configuration.getQuartzConfiguration().isEmpty()) {
            return StdSchedulerFactory.getDefaultScheduler();
        }

        StdSchedulerFactory factory = new StdSchedulerFactory();
        factory.initialize(createProperties());
        return factory.getScheduler();
    }

    /**
     * Creates Properties from the Quartz configuration map.
     *
     * @return Properties containing all Quartz configuration entries
     */
    private Properties createProperties() {
        Properties properties = new Properties();
        properties.putAll(configuration.getQuartzConfiguration());
        return properties;
    }

    /**
     * Returns the job factory to use for creating job instances.
     * <p>
     * Subclasses should override this method to provide a custom job factory
     * that supports dependency injection.
     * </p>
     *
     * @return the job factory for creating job instances
     */
    protected JobFactory getJobFactory() {
        return new DropwizardJobFactory(jobs);
    }

    // ========================================================================
    // SCHEDULING OPERATIONS
    // Core scheduling methods used by scheduler strategy classes
    // ========================================================================

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
    @Override
    public void scheduleOrRescheduleJob(ScheduledJob job) {
        JobKey jobKey = job.getJobKey();
        Trigger trigger = job.getTrigger();
        JobDetail jobDetail = build(job);

        try {
            if (scheduler.checkExists(jobKey)) {
                List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
                if (triggers.size() == 1) {
                    rescheduleSingleTrigger(triggers.get(0), trigger, job.getMessage());
                } else {
                    replaceMultiTriggerJob(jobKey, jobDetail, trigger, job.getMessage());
                }
            } else {
                createNewJob(jobDetail, trigger, job.getMessage());
            }
        } catch (SchedulerException e) {
            log.warn("Failed to schedule job with key '{}': {}", jobKey, e.getMessage(), e);
        }
    }

    /**
     * Reschedules an existing job that has exactly one trigger.
     *
     * @param existingTrigger the current trigger to replace
     * @param newTrigger the new trigger to use
     * @param message the log message for the job
     * @throws SchedulerException if rescheduling fails
     */
    private void rescheduleSingleTrigger(Trigger existingTrigger, Trigger newTrigger, String message)
            throws SchedulerException {
        scheduler.rescheduleJob(existingTrigger.getKey(), newTrigger);
        log.info("Rescheduled job: {}", message);
    }

    /**
     * Replaces a job that has multiple triggers by deleting and recreating it.
     * This enforces the one-to-one relationship between jobs and triggers.
     *
     * @param jobKey the key of the job to replace
     * @param jobDetail the new job detail
     * @param trigger the trigger for the new job
     * @param message the log message for the job
     * @throws SchedulerException if the operation fails
     */
    private void replaceMultiTriggerJob(JobKey jobKey, JobDetail jobDetail, Trigger trigger, String message)
            throws SchedulerException {
        scheduler.deleteJob(jobKey);
        scheduler.scheduleJob(jobDetail, trigger);
        log.info("Scheduled job: {}", message);
    }

    /**
     * Creates a new job with its trigger.
     *
     * @param jobDetail the job detail for the new job
     * @param trigger the trigger for the new job
     * @param message the log message for the job
     * @throws SchedulerException if scheduling fails
     */
    private void createNewJob(JobDetail jobDetail, Trigger trigger, String message) throws SchedulerException {
        scheduler.scheduleJob(jobDetail, trigger);
        log.info("Scheduled job: {}", message);
    }

    /**

     * Schedules a job to run immediately.
     * <p>
     * This method creates a trigger that fires immediately and schedules the job
     * with the Quartz scheduler. If the job already exists, it will be replaced.
     * </p>
     *
     * @param jobDetail the job detail describing the job to schedule
     * @throws SchedulerException if the job cannot be scheduled
     */
    public void scheduleNow(JobDetail jobDetail) throws SchedulerException {
        Trigger nowTrigger = nowTrigger();
        scheduler.scheduleJob(jobDetail, Set.of(nowTrigger), true);
    }

    /**
     * Creates a trigger that fires immediately.
     *
     * @return a trigger that starts now
     */
    protected Trigger nowTrigger() {
        return TriggerBuilder.newTrigger().startNow().build();
    }

    // ========================================================================
    // ACCESSORS
    // Getters for scheduler state and configuration
    // ========================================================================

    /**
     * Returns the Quartz scheduler managed by this job manager.
     * <p>
     * This provides direct access to the underlying scheduler for advanced operations.
     * </p>
     *
     * @return the Quartz scheduler
     */
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

    // ========================================================================
    // UTILITY METHODS
    // Private helpers for job building and logging
    // ========================================================================

    /**
     * Builds a Quartz JobDetail from a ScheduledJob.
     *
     * @param job the scheduled job containing job metadata
     * @return a configured JobDetail ready for scheduling
     */
    private static JobDetail build(ScheduledJob job) {
        JobKey jobKey = job.getJobKey();
        return JobBuilder.newJob(job.getClazz())
                .withIdentity(jobKey)
                .requestRecovery(job.isRequestsRecovery())
                .storeDurably(job.isStoreDurably())
                .build();
    }

    /**
     * Logs all jobs annotated with {@code @OnApplicationStop}.
     * <p>
     * This is called during startup to provide visibility into which jobs
     * will run when the application shuts down.
     * </p>
     */
    private void logAllOnApplicationStopJobs() {
        log.info("Jobs to run on application stop:");

        jobs.allOnApplicationStop()
                .map(Job::getClass)
                .forEach(clazz -> log.info("   {}", clazz.getCanonicalName()));
    }
}

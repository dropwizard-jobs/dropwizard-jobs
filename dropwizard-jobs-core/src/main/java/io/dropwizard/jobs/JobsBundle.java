package io.dropwizard.jobs;

import com.codahale.metrics.SharedMetricRegistries;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.quartz.Scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * A Dropwizard bundle that integrates the Quartz scheduler for background job execution.
 * <p>
 * This bundle manages the lifecycle of scheduled jobs, starting the Quartz scheduler
 * when the application starts and shutting it down gracefully when the application stops.
 * Jobs can be scheduled using annotations like {@code @Every}, {@code @On},
 * {@code @OnApplicationStart}, and {@code @OnApplicationStop}.
 * </p>
 * <p>
 * Example usage in your Dropwizard application:
 * </p>
 * <pre>{@code
 * @Override
 * public void initialize(Bootstrap<MyConfiguration> bootstrap) {
 *     bootstrap.addBundle(new JobsBundle(Arrays.asList(
 *         new MyEveryJob(),
 *         new MyCronJob()
 *     )));
 * }
 * }</pre>
 *
 * @see JobConfiguration
 * @see JobManager
 * @see Job
 */
public class JobsBundle implements ConfiguredBundle<JobConfiguration> {

    private final List<Job> jobs;
    protected JobManager jobManager;

    /**
     * Creates a new JobsBundle with the specified jobs.
     * <p>
     * Each job in the list should be annotated with one of the scheduling annotations:
     * {@code @Every}, {@code @On}, {@code @OnApplicationStart}, or {@code @OnApplicationStop}.
     * </p>
     *
     * @param jobs the list of jobs to be scheduled and managed by this bundle
     */
    public JobsBundle(List<Job> jobs) {
        Objects.requireNonNull(jobs, "jobs must not be null");
        this.jobs = new ArrayList<>(jobs);
    }

    /**
     * Starts the job manager and registers it with the Dropwizard lifecycle.
     * <p>
     * This method is called by Dropwizard during application startup. It creates
     * a {@link JobManager} with the provided configuration and jobs, then registers
     * it as a managed object so it will be started and stopped with the application.
     * </p>
     *
     * @param configuration the application configuration containing job and Quartz settings
     * @param environment the Dropwizard environment
     * @throws Exception if the job manager fails to start
     */
    @Override
    public void run(JobConfiguration configuration, Environment environment) throws Exception {
        jobManager = new JobManager(configuration, jobs);
        environment.lifecycle().manage(jobManager);
    }

    /**
     * Initializes the bundle by registering a shared metrics registry.
     * <p>
     * This registry is used by jobs to record execution timing metrics.
     * The metrics can be accessed via the "dropwizard-jobs" key.
     * </p>
     * <p>
     * This method removes any registry that may have been eagerly created by Job constructors
     * before registering the bootstrap's MetricRegistry. This ensures all jobs use the
     * correct Dropwizard-managed registry, fixing a race condition where jobs instantiated
     * before this method runs would otherwise use a disconnected registry.
     * </p>
     *
     * @param bootstrap the Dropwizard bootstrap object
     */
    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        // Remove any registry that may have been eagerly created by Job constructors,
        // then register the bootstrap's MetricRegistry to ensure all jobs use the
        // correct Dropwizard-managed registry.
        SharedMetricRegistries.remove(Job.DROPWIZARD_JOBS_KEY);
        SharedMetricRegistries.add(Job.DROPWIZARD_JOBS_KEY, bootstrap.getMetricRegistry());
    }

    /**
     * Returns the Quartz scheduler used by this bundle.
     * <p>
     * This provides direct access to the underlying Quartz scheduler for advanced
     * operations like manually triggering jobs or querying job status.
     * </p>
     *
     * @return the Quartz scheduler, or null if the bundle has not been run yet
     */
    public Scheduler getScheduler() {
        return jobManager.getScheduler();
    }

}

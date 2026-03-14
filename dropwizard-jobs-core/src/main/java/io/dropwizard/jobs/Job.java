package io.dropwizard.jobs;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * Abstract base class for all scheduled jobs in dropwizard-jobs.
 * <p>
 * To create a scheduled job, extend this class and implement the {@link #doJob(JobExecutionContext)}
 * method. Then annotate your class with one of the scheduling annotations:
 * </p>
 * <ul>
 *   <li>{@code @Every("5s")} - Run every 5 seconds</li>
 *   <li>{@code @On("0 0 12 * * ?")} - Run at noon daily (cron expression)</li>
 *   <li>{@code @OnApplicationStart} - Run once when the application starts</li>
 *   <li>{@code @OnApplicationStop} - Run once when the application stops</li>
 * </ul>
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * @Every("1m")
 * public class MyScheduledJob extends Job {
 *     @Override
 *     public void doJob(JobExecutionContext context) throws JobExecutionException {
 *         // Your job logic here
 *         System.out.println("Job executed at: " + new Date());
 *     }
 * }
 * }</pre>
 * <p>
 * <strong>Important:</strong> Always implement {@link #doJob(JobExecutionContext)}, never override
 * {@link #execute(JobExecutionContext)} directly. The base class handles metrics timing around
 * job execution.
 * </p>
 * <p>
 * Job execution timing is automatically recorded in the metrics registry under the job's
 * class name.
 * </p>
 *
 * @see JobsBundle
 * @see JobConfiguration
 */
public abstract class Job implements org.quartz.Job {

    /**
     * The key used to register the shared metrics registry for job metrics.
     */
    public static final String DROPWIZARD_JOBS_KEY = "dropwizard-jobs";
    private final String groupName;

    private volatile Timer timer;
    private volatile MetricRegistry metricRegistry;
    private final boolean lazyMetrics;

    /**
     * Creates a new job with the default metrics registry and no group name.
     * <p>
     * The metrics registry is obtained lazily from the shared registries using
     * {@link #DROPWIZARD_JOBS_KEY} on first execution. This ensures that the
     * correct Dropwizard-managed registry is used even if the job is instantiated
     * before {@link JobsBundle#initialize(io.dropwizard.core.setup.Bootstrap)} is called.
     * </p>
     */
    public Job() {
        this((String) null);
    }

    /**
     * Creates a new job with the specified group name.
     * <p>
     * Group names allow multiple instances of the same job class to be scheduled
     * independently. Each instance with a different group name will have its own
     * schedule.
     * </p>
     * <p>
     * The metrics registry is obtained lazily from the shared registries using
     * {@link #DROPWIZARD_JOBS_KEY} on first execution. This ensures that the
     * correct Dropwizard-managed registry is used even if the job is instantiated
     * before {@link JobsBundle#initialize(io.dropwizard.core.setup.Bootstrap)} is called.
     * </p>
     *
     * @param groupName the group name for this job instance
     */
    public Job(String groupName) {
        this.groupName = groupName;
        this.lazyMetrics = true;
        // timer and metricRegistry will be initialized lazily
    }

    /**
     * Creates a new job with the specified metrics registry.
     * <p>
     * The metrics registry is eagerly initialized at construction time since
     * the caller has explicitly provided a registry.
     * </p>
     *
     * @param metricRegistry the metrics registry to use for timing job executions
     */
    public Job(MetricRegistry metricRegistry) {
        this(metricRegistry, null);
    }

    /**
     * Creates a new job with the specified metrics registry and group name.
     * <p>
     * The metrics registry is eagerly initialized at construction time since
     * the caller has explicitly provided a registry.
     * </p>
     *
     * @param metricRegistry the metrics registry to use for timing job executions
     * @param groupName the group name for this job instance, or null for no grouping
     */
    public Job(MetricRegistry metricRegistry, String groupName) {
        this.metricRegistry = metricRegistry;
        this.timer = metricRegistry.timer(name(getClass()));
        this.groupName = groupName;
        this.lazyMetrics = false;
    }

    /**
     * Ensures the metrics registry and timer are initialized.
     * <p>
     * For jobs using lazy metrics resolution (default constructors), this method
     * obtains the shared metrics registry on first call, by which time
     * {@link JobsBundle#initialize(io.dropwizard.core.setup.Bootstrap)} has already
     * registered the correct Dropwizard-managed registry.
     * </p>
     * <p>
     * This method uses double-checked locking for thread-safety.
     * </p>
     */
    private void ensureMetricsInitialized() {
        if (lazyMetrics && timer == null) {
            synchronized (this) {
                if (timer == null) {
                    metricRegistry = SharedMetricRegistries.getOrCreate(DROPWIZARD_JOBS_KEY);
                    timer = metricRegistry.timer(name(getClass()));
                }
            }
        }
    }

    /**
     * Executes the job with automatic timing metrics.
     * <p>
     * <strong>Do not override this method.</strong> Instead, implement
     * {@link #doJob(JobExecutionContext)} to provide your job logic.
     * </p>
     *
     * @param context the Quartz job execution context
     * @throws JobExecutionException if the job fails to execute
     */
    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        ensureMetricsInitialized();
        try (Context ignored = timer.time()) {
            doJob(context);
        }
    }

    /**
     * The method to implement for job execution logic.
     * <p>
     * This method is called by the Quartz scheduler when the job's trigger fires.
     * Implement this method to perform the actual work of your job.
     * </p>
     * <p>
     * The {@code context} parameter provides access to job details, trigger information,
     * and the scheduler instance.
     * </p>
     *
     * @param context the Quartz job execution context providing job and trigger details
     * @throws JobExecutionException if the job encounters an error during execution
     */
    public abstract void doJob(JobExecutionContext context) throws JobExecutionException;

    /**
     * Returns the metrics registry used by this job.
     * <p>
     * For jobs using lazy metrics resolution, this will trigger initialization
     * if the registry has not yet been obtained.
     * </p>
     *
     * @return the metrics registry
     */
    protected MetricRegistry getMetricRegistry() {
        ensureMetricsInitialized();
        return metricRegistry;
    }

    /**
     * Returns the group name for this job instance.
     * <p>
     * Group names allow multiple instances of the same job class to be scheduled
     * with different schedules.
     * </p>
     *
     * @return the group name, or null if no group name was set
     */
    public String getGroupName() {
        return groupName;
    }
}

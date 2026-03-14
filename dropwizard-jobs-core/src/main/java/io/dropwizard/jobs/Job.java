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

    private final Timer timer;
    private final MetricRegistry metricRegistry;

    /**
     * Creates a new job with the default metrics registry and no group name.
     * <p>
     * The metrics registry is obtained from the shared registries using
     * {@link #DROPWIZARD_JOBS_KEY}.
     * </p>
     */
    public Job() {
        // get the metrics registry which was shared during bundle instantiation
        this(SharedMetricRegistries.getOrCreate(DROPWIZARD_JOBS_KEY), null);
    }

    /**
     * Creates a new job with the specified group name.
     * <p>
     * Group names allow multiple instances of the same job class to be scheduled
     * independently. Each instance with a different group name will have its own
     * schedule.
     * </p>
     *
     * @param groupName the group name for this job instance
     */
    public Job(String groupName) {
        this(SharedMetricRegistries.getOrCreate(DROPWIZARD_JOBS_KEY), groupName);
    }

    /**
     * Creates a new job with the specified metrics registry.
     *
     * @param metricRegistry the metrics registry to use for timing job executions
     */
    public Job(MetricRegistry metricRegistry) {
        this(metricRegistry, null);
    }

    /**
     * Creates a new job with the specified metrics registry and group name.
     *
     * @param metricRegistry the metrics registry to use for timing job executions
     * @param groupName the group name for this job instance, or null for no grouping
     */
    public Job(MetricRegistry metricRegistry, String groupName) {
        this.timer = metricRegistry.timer(name(getClass()));
        this.metricRegistry = metricRegistry;
        this.groupName = groupName;
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
     *
     * @return the metrics registry
     */
    protected MetricRegistry getMetricRegistry() {
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

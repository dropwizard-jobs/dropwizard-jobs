package io.dropwizard.jobs;

import org.quartz.JobListener;
import org.quartz.spi.JobFactory;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link JobManager} implementation that integrates with Spring for dependency injection.
 * <p>
 * This class enables jobs to have dependencies injected by Spring. Jobs are discovered by
 * scanning the Spring application context for beans that extend {@link Job}. Job listeners
 * are discovered by scanning for beans that implement {@link JobListener}.
 * </p>
 *
 * @see SpringJobFactory
 * @see SpringJobsBundle
 */
public class SpringJobManager extends JobManager {

    protected SpringJobFactory jobFactory;

    /**
     * Creates a new SpringJobManager with the specified configuration and application context.
     * <p>
     * This constructor eagerly retrieves all Job beans from the Spring application context.
     * This is a necessary trade-off for job discovery:
     * </p>
     * <ul>
     *   <li>The Spring context is the source of truth for which jobs exist</li>
     *   <li>{@code context.getBeansOfType(Job.class)} requires instantiating the beans</li>
     *   <li>The instances are used to read class-level annotations and instance configuration</li>
     * </ul>
     * <p>
     * The actual job execution uses NEW instances created by {@link SpringJobFactory}, which
     * properly respects Spring bean scopes (e.g., {@code @Singleton}, {@code @Prototype}).
     * The instances created here are used only for job registration and then discarded.
     * </p>
     *
     * @param config the application configuration containing job and Quartz settings
     * @param context the Spring application context used to discover and instantiate jobs and job listeners
     * @see Hk2JobsBundle#run(JobConfiguration, io.dropwizard.core.setup.Environment)
     * @see GuiceJobManager#getJobs(com.google.inject.Injector)
     */
    public SpringJobManager(JobConfiguration config, ApplicationContext context) {
        super(config, toJobMetadata(new ArrayList<>(context.getBeansOfType(Job.class).values())),
              getJobListeners(context));
        jobFactory = new SpringJobFactory(context);
    }

    /**
     * Converts Job instances to JobMetadata.
     *
     * @param jobs the list of job instances
     * @return a list of job metadata
     */
    private static List<JobMetadata> toJobMetadata(List<Job> jobs) {
        return jobs.stream()
                .map(JobMetadata::new)
                .toList();
    }

    /**
     * Discovers all JobListener instances from the Spring application context.
     * <p>
     * This method retrieves all beans that implement {@link JobListener} from the
     * Spring context. Unlike job discovery, this uses Spring's built-in
     * {@code getBeansOfType()} method which handles the scanning efficiently.
     * </p>
     *
     * @param context the Spring application context to scan for job listener beans
     * @return a list of all discovered JobListener instances
     */
    static List<JobListener> getJobListeners(ApplicationContext context) {
        return new ArrayList<>(context.getBeansOfType(JobListener.class).values());
    }

    @Override
    protected JobFactory getJobFactory() {
        return jobFactory;
    }
}

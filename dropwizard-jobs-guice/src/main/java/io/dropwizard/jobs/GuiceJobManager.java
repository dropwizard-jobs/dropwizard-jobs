package io.dropwizard.jobs;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.quartz.spi.JobFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A {@link JobManager} implementation that integrates with Google Guice for dependency injection.
 * <p>
 * This class enables jobs to have dependencies injected by Guice. Jobs are discovered by scanning
 * the Guice bindings for types that extend {@link Job}.
 * </p>
 *
 * @see GuiceJobFactory
 * @see GuiceJobsBundle
 */
public class GuiceJobManager extends JobManager {

    private final GuiceJobFactory jobFactory;

    /**
     * Creates a new GuiceJobManager with the specified configuration and injector.
     *
     * @param config the application configuration containing job and Quartz settings
     * @param injector the Guice injector used to discover and instantiate jobs
     */
    public GuiceJobManager(JobConfiguration config, Injector injector) {
        super(config, getJobs(injector));
        jobFactory = new GuiceJobFactory(injector);
    }

    /**
     * Discovers all Job instances from the Guice injector bindings.
     * <p>
     * This method eagerly instantiates all jobs bound in the Guice container. This is a
     * necessary trade-off for job discovery:
     * </p>
     * <ul>
     *   <li>Guice bindings are the source of truth for which jobs exist</li>
     *   <li>There is no API to enumerate bound types without instantiating them</li>
     *   <li>The instances are used to read class-level annotations and instance configuration</li>
     * </ul>
     * <p>
     * The actual job execution uses NEW instances created by {@link GuiceJobFactory}, which
     * properly respects Guice scopes (e.g., {@code @Singleton}, {@code @PerLookup}). The
     * instances created here are used only for job registration and then discarded.
     * </p>
     *
     * @param injector the Guice injector to scan for job bindings
     * @return a list of all discovered Job instances
     * @see Hk2JobsBundle#run(JobConfiguration, io.dropwizard.core.setup.Environment)
     * @see SpringJobManager#SpringJobManager(JobConfiguration, org.springframework.context.ApplicationContext)
     */
    static List<Job> getJobs(Injector injector) {
        List<Job> jobs = new ArrayList<>();
        Map<Key<?>, Binding<?>> bindings = injector.getBindings();
        for (Key<?> key : bindings.keySet()) {
            TypeLiteral<?> typeLiteral = key.getTypeLiteral();
            Class<?> clazz = typeLiteral.getRawType();
            if (Job.class.isAssignableFrom(clazz)) {
                jobs.add((Job) injector.getInstance(clazz));
            }
        }
        return jobs;
    }


    @Override
    protected JobFactory getJobFactory() {
        return jobFactory;
    }
}

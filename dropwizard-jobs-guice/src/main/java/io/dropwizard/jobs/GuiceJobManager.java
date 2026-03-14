package io.dropwizard.jobs;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.quartz.JobListener;
import org.quartz.spi.JobFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A {@link JobManager} implementation that integrates with Google Guice for dependency injection.
 * <p>
 * This class enables jobs to have dependencies injected by Guice. Jobs are discovered by scanning
 * the Guice bindings for types that extend {@link Job}. Job listeners are discovered by scanning
 * for types that implement {@link JobListener}.
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
     * @param injector the Guice injector used to discover and instantiate jobs and job listeners
     */
    public GuiceJobManager(JobConfiguration config, Injector injector) {
        super(config, getJobs(injector), getJobListeners(injector));
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

    /**
     * Discovers all JobListener instances from the Guice injector bindings.
     * <p>
     * This method eagerly instantiates all job listeners bound in the Guice container.
     * Similar to job discovery, this is necessary because Guice does not provide an API
     * to enumerate bound types without instantiating them.
     * </p>
     *
     * @param injector the Guice injector to scan for job listener bindings
     * @return a list of all discovered JobListener instances
     */
    static List<JobListener> getJobListeners(Injector injector) {
        List<JobListener> listeners = new ArrayList<>();
        Map<Key<?>, Binding<?>> bindings = injector.getBindings();
        for (Key<?> key : bindings.keySet()) {
            TypeLiteral<?> typeLiteral = key.getTypeLiteral();
            Class<?> clazz = typeLiteral.getRawType();
            if (JobListener.class.isAssignableFrom(clazz) && clazz != JobListener.class) {
                listeners.add((JobListener) injector.getInstance(clazz));
            }
        }
        return listeners;
    }

    @Override
    protected JobFactory getJobFactory() {
        return jobFactory;
    }
}

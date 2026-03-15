package io.dropwizard.jobs;

import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.spi.AbstractContainerLifecycleListener;
import org.glassfish.jersey.server.spi.Container;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.spi.JobFactory;

import io.dropwizard.core.setup.Environment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * A {@link JobsBundle} implementation that uses HK2 to instantiate a {@link Job}.
 * 
 * <p>
 * Example of usage:
 * </p>
 * 
 * <pre>{@code
 * public class MyDropwizardApplication extends Application<MyConfiguration> {
 *     ...
 *
 *     @Override
 *     public void initialize(Bootstrap<MyConfiguration> bootstrap) {
 *         bootstrap.addBundle(new Hk2JobsBundle(BuilderHelper.createContractFilter(Job.class.getName())));
 *     }
 *
 *     @Override
 *     public void run(MyConfiguration configuration, Environment environment) {
 *         environment.jersey().register(new AbstractBinder() {
 *             @Override
 *             protected void configure() {
 *                 // Register jobs with contract type `io.dropwizard.jobs.Job`.
 *                 bind(MyJobWithPerLookup.class).to(Job.class);
 *                 bind(MyJobWithPerThread.class).to(Job.class).in(PerThread.class);
 *                 bind(MyJobWithSingleton.class).to(Job.class).in(Singleton.class);
 *             }
 *         });
 *     }
 * }
 * }</pre>
 */
public class Hk2JobsBundle extends JobsBundle {

    private final Filter searchCriteria;
    private final Filter listenerSearchCriteria;

    /**
     * Create a new instance with given HK2 search criteria.
     * 
     * <p>
     * You can implement your own {@code Filter} or you can use one of the {@code Filter} implementations provided by
     * {@code BuilderHelper}. The most common case is to use an {@code IndexedFilter} provided by {@code BuilderHelper},
     * like this:
     * </p>
     * 
     * <pre>
     * IndexedFilter jobFilter = BuilderHelper.createContractFilter(Job.class.getName());
     * bootstrap.addBundle(new Hk2JobsBundle(jobFilter));
     * </pre>
     * 
     * @param searchCriteria the returned {@link Job} service will match the Filter (in other words,
     *            searchCriteria.matches returns true). Should not be null.
     */
    public Hk2JobsBundle(final Filter searchCriteria) {
        this(searchCriteria, null);
    }

    /**
     * Create a new instance with given HK2 search criteria for jobs and listeners.
     * 
     * <p>
     * You can implement your own {@code Filter} or you can use one of the {@code Filter} implementations provided by
     * {@code BuilderHelper}. The most common case is to use an {@code IndexedFilter} provided by {@code BuilderHelper},
     * like this:
     * </p>
     * 
     * <pre>
     * IndexedFilter jobFilter = BuilderHelper.createContractFilter(Job.class.getName());
     * IndexedFilter listenerFilter = BuilderHelper.createContractFilter(JobListener.class.getName());
     * bootstrap.addBundle(new Hk2JobsBundle(jobFilter, listenerFilter));
     * </pre>
     * 
     * @param searchCriteria the returned {@link Job} service will match the Filter (in other words,
     *            searchCriteria.matches returns true). Should not be null.
     * @param listenerSearchCriteria the returned {@link JobListener} service will match the Filter.
     *            May be null if no listeners should be discovered.
     */
    public Hk2JobsBundle(final Filter searchCriteria, final Filter listenerSearchCriteria) {
        super(new ArrayList<>());
        Objects.requireNonNull(searchCriteria);
        this.searchCriteria = searchCriteria;
        this.listenerSearchCriteria = listenerSearchCriteria;
    }

    @Override
    public void run(final JobConfiguration configuration, final Environment environment) {
        environment.jersey().register(new AbstractContainerLifecycleListener() {
            @Override
            public void onStartup(Container container) {
                final InjectionManager im = container.getApplicationHandler().getInjectionManager();
                final ServiceLocator locator = im.getInstance(ServiceLocator.class);
                /*
                 * Eagerly resolve all Job instances from the HK2 container to discover available job classes.
                 * <p>
                 * This is a necessary trade-off: the DI container is the source of truth for which jobs
                 * exist, and there is no API to enumerate job classes without instantiating them. The
                 * instances created here are used for:
                 * <ul>
                 *   <li>Reading class-level annotations (@Every, @On, etc.) to determine scheduling</li>
                 *   <li>Accessing instance-specific configuration (e.g., groupName)</li>
                 *   <li>Registering jobs with the Quartz scheduler</li>
                 * </ul>
                 * <p>
                 * The actual job execution uses NEW instances created by the JobFactory below, which
                 * properly respects DI scopes (e.g., @Singleton, @PerLookup). The instances created
                 * here are discarded after job registration.
                 *
                 * @see GuiceJobManager#getJobs(com.google.inject.Injector)
                 * @see SpringJobManager#SpringJobManager(JobConfiguration, org.springframework.context.ApplicationContext)
                 */
                @SuppressWarnings("unchecked")
                final List<Job> jobs = (List<Job>) locator.getAllServices(searchCriteria);

                // Discover listeners if filter provided
                final List<JobListener> listeners;
                if (listenerSearchCriteria != null) {
                    @SuppressWarnings("unchecked")
                    List<JobListener> discovered = (List<JobListener>) locator.getAllServices(listenerSearchCriteria);
                    listeners = discovered;
                } else {
                    listeners = Collections.emptyList();
                }

                // Convert Job instances to JobMetadata for the constructor
                List<JobMetadata> jobMetadata = jobs.stream()
                        .map(job -> new JobMetadata(job.getClass(), job.getGroupName()))
                        .toList();

                jobManager = new JobManager(configuration, jobMetadata, listeners) {
                    @Override
                    protected JobFactory getJobFactory() {
                        return (bundle,
                                scheduler) -> (org.quartz.Job) locator.getAllServiceHandles(searchCriteria).stream()
                                        .filter(sh -> sh.getActiveDescriptor().getImplementationClass()
                                                .equals(bundle.getJobDetail().getJobClass()))
                                        .findFirst().orElseThrow(IllegalStateException::new).getService();
                    }
                };
                try {
                    jobManager.start();
                } catch (Exception ex) {
                    throw new IllegalStateException(ex);
                }
            }

            @Override
            public void onShutdown(Container container) {
                if (jobManager != null) {
                    try {
                        jobManager.stop();
                    } catch (Exception ex) {
                        throw new IllegalStateException(ex);
                    }
                }
            }
        });
    }

    /**
     * Returns the Quartz {@link Scheduler} managed by this bundle.
     * <p>
     * This method returns {@code null} if the bundle has not been started yet, which can happen
     * if called before the Jersey container has completed startup (i.e., before
     * {@code onStartup} has been invoked).
     * </p>
     *
     * @return the scheduler, or {@code null} if the bundle has not been started
     */
    @Override
    public Scheduler getScheduler() {
        return (jobManager != null) ? super.getScheduler() : null;
    }

}

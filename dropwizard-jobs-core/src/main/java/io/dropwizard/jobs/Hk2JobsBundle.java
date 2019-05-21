package io.dropwizard.jobs;

import io.dropwizard.setup.Environment;
import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.spi.AbstractContainerLifecycleListener;
import org.glassfish.jersey.server.spi.Container;
import org.quartz.Scheduler;
import org.quartz.spi.JobFactory;

import java.util.List;
import java.util.Objects;

/**
 * A {@link JobsBundle} implementation that uses HK2 to instantiate a {@link Job}.
 * 
 * <p>
 * Example of usage:
 * </p>
 * 
 * <pre>
 * public class MyDropwizardApplication extends Application&lt;MyConfiguration&gt; {
 *     ...
 * 
 *     &#64;Override
 *     public void initialize(Bootstrap&lt;MyConfiguration&gt; bootstrap) {
 *         bootstrap.addBundle(new Hk2JobsBundle(BuilderHelper.createContractFilter(Job.class.getName())));
 *     }
 * 
 *     &#64;Override
 *     public void run(MyConfiguration configuration, Environment environment) {
 *         environment.jersey().register(new AbstractBinder() {
 *             &#64;Override
 *             protected void configure() {
 *                 // Register jobs with contract type `io.dropwizard.jobs.Job`.
 *                 bind(MyJobWithPerLookup.class).to(Job.class);
 *                 bind(MyJobWithPerThread.class).to(Job.class).in(PerThread.class);
 *                 bind(MyJobWithSingleton.class).to(Job.class).in(Singleton.class);
 *             }
 *         });
 *     }
 * }
 * </pre>
 */
public class Hk2JobsBundle extends JobsBundle {

    private final Filter searchCriteria;

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
        Objects.requireNonNull(searchCriteria);
        this.searchCriteria = searchCriteria;
    }

    @Override
    public void run(final JobConfiguration configuration, final Environment environment) {
        environment.jersey().register(new AbstractContainerLifecycleListener() {
            @Override
            public void onStartup(Container container) {
                final InjectionManager im = container.getApplicationHandler().getInjectionManager();
                final ServiceLocator locator = im.getInstance(ServiceLocator.class);
                // TODO: Avoid useless Job instantiation. Note: Same as GuiceJobManager and SpringJobManager.
                @SuppressWarnings("unchecked")
                final List<Job> jobs = (List<Job>) locator.getAllServices(searchCriteria);
                jobManager = new JobManager(configuration, jobs.toArray(new Job[jobs.size()])) {
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

    @Override
    public Scheduler getScheduler() {
        return (jobManager != null) ? super.getScheduler() : null;
    }

}

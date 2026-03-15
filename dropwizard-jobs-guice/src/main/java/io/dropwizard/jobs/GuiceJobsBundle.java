package io.dropwizard.jobs;

import com.google.inject.Injector;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.lifecycle.Managed;
import org.quartz.Scheduler;

import java.util.ArrayList;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A {@link JobsBundle} implementation that uses Guice to instantiate a {@link Job}.
 * 
 * <p>
 * This bundle supports two usage modes:
 * </p>
 * 
 * <h2>Eager Mode</h2>
 * <p>
 * Use {@link #GuiceJobsBundle(Injector)} when the Guice Injector is available at bundle
 * construction time (init phase). This is the traditional usage pattern:
 * </p>
 *
 * <pre>{@code
 * @Override
 * public void initialize(Bootstrap<MyConfig> bootstrap) {
 *     Injector injector = Guice.createInjector(new MyModule());
 *     bootstrap.addBundle(new GuiceJobsBundle(injector));
 * }
 * }</pre>
 *
 * <h2>Deferred Mode</h2>
 * <p>
 * Use {@link #GuiceJobsBundle(Supplier)} when the Guice Injector is not available until
 * the run phase (e.g., when using dropwizard-guicey). The supplier is called during
 * {@link Managed#start()}, after all bundles have completed their run() methods:
 * </p>
 *
 * <pre>{@code
 * @Override
 * public void initialize(Bootstrap<MyConfig> bootstrap) {
 *     guiceBundle = GuiceBundle.builder()...build();
 *     bootstrap.addBundle(guiceBundle);
 *     bootstrap.addBundle(new GuiceJobsBundle(() -> guiceBundle.getInjector()));
 * }
 * }</pre>
 */
public class GuiceJobsBundle extends JobsBundle {

    private final Injector injector;
    private final Supplier<Injector> injectorSupplier;

    /**
     * Creates a GuiceJobsBundle with an eagerly-provided Injector.
     * Use this constructor when the Guice Injector is available at
     * bundle construction time (init phase).
     *
     * @param injector the Guice Injector to use for job instantiation.
     *            Must not be null.
     */
    public GuiceJobsBundle(Injector injector) {
        super(new ArrayList<>());
        this.injector = Objects.requireNonNull(injector);
        this.injectorSupplier = null;
    }

    /**
     * Creates a GuiceJobsBundle with a deferred Injector supplier.
     * Use this constructor when the Guice Injector is not available until
     * the run phase (e.g., when using dropwizard-guicey).
     * The supplier is called during Managed.start(), after all bundles
     * have completed their run() methods.
     *
     * @param injectorSupplier a Supplier that provides the Guice Injector.
     *            Must not be null. The supplier will be called exactly once
     *            during the managed lifecycle start phase.
     */
    public GuiceJobsBundle(Supplier<Injector> injectorSupplier) {
        super(new ArrayList<>());
        this.injector = null;
        this.injectorSupplier = Objects.requireNonNull(injectorSupplier);
    }

    @Override
    public void run(JobConfiguration configuration, Environment environment) throws Exception {
        if (injector != null) {
            // Eager mode: existing behavior, unchanged
            jobManager = new GuiceJobManager(configuration, injector);
            environment.lifecycle().manage(jobManager);
        } else {
            // Deferred mode: wrap in Managed to defer creation + start
            environment.lifecycle().manage(new Managed() {
                @Override
                public void start() throws Exception {
                    Injector resolved = injectorSupplier.get();
                    jobManager = new GuiceJobManager(configuration, resolved);
                    jobManager.start();
                }

                @Override
                public void stop() throws Exception {
                    if (jobManager != null) {
                        jobManager.stop();
                    }
                }
            });
        }
    }

    /**
     * Returns the Quartz {@link Scheduler} managed by this bundle.
     * <p>
     * This method returns {@code null} if the bundle has not been started yet, which can happen
     * if using the deferred constructor and called before the Dropwizard lifecycle has completed
     * startup (i.e., before {@code Managed.start()} has been invoked).
     * </p>
     *
     * @return the scheduler, or {@code null} if the bundle has not been started
     */
    @Override
    public Scheduler getScheduler() {
        return (jobManager != null) ? super.getScheduler() : null;
    }
}

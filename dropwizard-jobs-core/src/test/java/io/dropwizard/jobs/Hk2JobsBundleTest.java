package io.dropwizard.jobs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Singleton;

import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.Binder;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.spi.AbstractContainerLifecycleListener;
import org.glassfish.jersey.server.spi.Container;
import org.junit.Test;

import io.dropwizard.jersey.DropwizardResourceConfig;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.setup.Environment;

public class Hk2JobsBundleTest {

    private final JobConfiguration configuration = mock(JobConfiguration.class);
    private final Environment environment = mock(Environment.class);

    /**
     * A test case for the {@link Hk2JobsBundle#run(JobConfiguration, Environment)} and
     * {@link Hk2JobsBundle#getScheduler()}.
     */
    @Test
    public void registerToContainer() throws Exception {
        final Filter searchCriteria = BuilderHelper.createContractFilter(Job.class.getName());
        final Hk2JobsBundle jobsBundle = new Hk2JobsBundle(searchCriteria);

        // initialization
        final DropwizardResourceConfig resourceConfig = DropwizardResourceConfig.forTesting();
        when(environment.jersey()).thenReturn(new JerseyEnvironment(null, resourceConfig));
        jobsBundle.run(configuration, environment);
        final ApplicationHandler applicationHandler = new ApplicationHandler(resourceConfig);
        final InjectionManager im = applicationHandler.getInjectionManager();
        final ServiceLocator serviceLocator = im.getInstance(ServiceLocator.class);
        ServiceLocatorUtilities.bind(serviceLocator, new AbstractBinder() {
            @Override
            protected void configure() {
                bind(ApplicationStartTestJob.class).to(Job.class).in(Singleton.class);
                bind(ApplicationStopTestJob.class).to(Job.class).in(Singleton.class);
                bind(EveryTestJob.class).to(EveryTestJob.class);
            }
        });
        final Container container = mock(Container.class);
        when(container.getApplicationHandler()).thenReturn(applicationHandler);
        when(container.getConfiguration()).thenReturn(resourceConfig);

        // verify precondition
        assertThat(jobsBundle.getScheduler()).isNull();

        // startup container
        applicationHandler.onStartup(container);
        Thread.sleep(1000);

        // verify at startup
        @SuppressWarnings("unchecked")
        final List<AbstractJob> jobs = (List<AbstractJob>) serviceLocator.getAllServices(searchCriteria);
        AbstractJob applicationStartTestJob = getJob(jobs, ApplicationStartTestJob.class);
        AbstractJob applicationStopTestJob = getJob(jobs, ApplicationStopTestJob.class);
        assertThat(jobs).hasSize(2);
        assertThat(applicationStartTestJob.latch().getCount()).isEqualTo(0);
        assertThat(applicationStopTestJob.latch().getCount()).isEqualTo(1);
        assertThat(jobsBundle.getScheduler().isStarted()).isTrue();

        // shutdown container
        applicationHandler.onShutdown(container);
        Thread.sleep(1000);

        // verify at shutdown
        assertThat(applicationStopTestJob.latch().getCount()).isEqualTo(0);
        assertThat(jobsBundle.getScheduler().isShutdown()).isTrue();
    }

    /**
     * A test case for the {@link JobManager#start()} and {@link JobManager#stop()} that raise exception.
     */
    @Test
    public void testIllegalState() {
        final AtomicReference<AbstractContainerLifecycleListener> listener = new AtomicReference<>();
        final JerseyEnvironment jersey = mock(JerseyEnvironment.class);
        when(environment.jersey()).thenReturn(jersey);
        doAnswer(invocation -> {
            listener.set(invocation.getArgument(0));
            return null;
        }).when(jersey).register((Object) any());

        final Hk2JobsBundle instance = new Hk2JobsBundle(BuilderHelper.createContractFilter(""));
        instance.run(null, environment);

        final Container container = mock(Container.class);
        final ApplicationHandler applicationHandler = new ApplicationHandler();
        when(container.getApplicationHandler()).thenReturn(applicationHandler);
        try {
            listener.get().onStartup(container);
            fail();
        } catch (IllegalStateException ignore) {
            // OK: fall through
        }
        try {
            listener.get().onShutdown(container);
            fail();
        } catch (IllegalStateException ignore) {
            // OK: fall through
        }
    }

    private AbstractJob getJob(final List<AbstractJob> jobs, final Class<?> implementationClass) {
        return jobs.stream().filter(job -> job.getClass().equals(implementationClass)).findFirst()
                .orElseThrow(IllegalStateException::new);
    }
    
}

package io.dropwizard.jobs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.spi.AbstractContainerLifecycleListener;
import org.glassfish.jersey.server.spi.Container;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;
import org.quartz.SchedulerException;
import jakarta.inject.Singleton;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import io.dropwizard.core.setup.Environment;
import io.dropwizard.jersey.setup.JerseyEnvironment;
import io.dropwizard.jersey.DropwizardResourceConfig;

@Timeout(15)
public class Hk2JobsBundleTest {

    private final Environment environment = mock(Environment.class);
    private final JerseyEnvironment jerseyEnvironment = mock(JerseyEnvironment.class);
    private Hk2JobsBundle startedBundle;
    private ServiceLocator locator;

    @AfterEach
    public void closeResources() throws Exception {
        try {
            if (startedBundle != null && startedBundle.getScheduler() != null && !startedBundle.getScheduler().isShutdown()) {
                startedBundle.getScheduler().shutdown(true);
            }
        } finally {
            if (locator != null) {
                ServiceLocatorFactory.getInstance().destroy(locator);
            }
        }
    }

    @Test
    public void testNullFilterThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            new Hk2JobsBundle(null);
        });
    }

    @Test
    public void testGetSchedulerReturnsNullBeforeStartup() throws Exception {
        Filter jobFilter = BuilderHelper.createContractFilter(Job.class.getName());
        Hk2JobsBundle bundle = new Hk2JobsBundle(jobFilter);

        when(environment.jersey()).thenReturn(jerseyEnvironment);
        bundle.run(new TestConfig(), environment);

        // getScheduler() should return null before container startup
        assertThat(bundle.getScheduler(), is(nullValue()));
    }

    @Test
    public void testBundleConstructionWithOneArg() {
        Filter jobFilter = BuilderHelper.createContractFilter(Job.class.getName());
        Hk2JobsBundle bundle = new Hk2JobsBundle(jobFilter);
        assertThat(bundle, is(notNullValue()));
    }

    @Test
    public void testBundleConstructionWithTwoArgs() {
        Filter jobFilter = BuilderHelper.createContractFilter(Job.class.getName());
        Filter listenerFilter = BuilderHelper.createContractFilter(org.quartz.JobListener.class.getName());
        Hk2JobsBundle bundle = new Hk2JobsBundle(jobFilter, listenerFilter);
        assertThat(bundle, is(notNullValue()));
    }

    @Test
    public void testBundleConstructionWithNullListenerFilter() {
        Filter jobFilter = BuilderHelper.createContractFilter(Job.class.getName());
        Hk2JobsBundle bundle = new Hk2JobsBundle(jobFilter, null);
        assertThat(bundle, is(notNullValue()));
    }

    @Test
    public void executesJobsBoundAfterBundleRegistrationAndStopsWithJersey() throws Exception {
        DropwizardResourceConfig resources = DropwizardResourceConfig.forTesting();
        when(environment.jersey()).thenReturn(new JerseyEnvironment(null, resources));
        startedBundle = new Hk2JobsBundle(BuilderHelper.createContractFilter(Job.class.getName()));
        startedBundle.run(new TestConfig(), environment);
        assertNull(startedBundle.getScheduler());

        ApplicationHandler handler = new ApplicationHandler(resources);
        locator = handler.getInjectionManager().getInstance(ServiceLocator.class);
        ServiceLocatorUtilities.bind(locator, new AbstractBinder() {
            @Override
            protected void configure() {
                bind(ApplicationStartTestJob.class).to(Job.class).in(Singleton.class);
                bind(ApplicationStopTestJob.class).to(Job.class).in(Singleton.class);
            }
        });
        Container container = mock(Container.class);
        when(container.getApplicationHandler()).thenReturn(handler);
        when(container.getConfiguration()).thenReturn(resources);

        handler.onStartup(container);
        assertTrue(startedBundle.getScheduler().isStarted());
        assertTrue(findJob(ApplicationStartTestJob.class).latch.await(2, TimeUnit.SECONDS));
        ApplicationStopTestJob stopJob = findJob(ApplicationStopTestJob.class);
        assertEquals(1, stopJob.latch.getCount());

        handler.onShutdown(container);

        assertEquals(0, stopJob.latch.getCount());
        assertTrue(startedBundle.getScheduler().isShutdown());
    }

    @Test
    public void startupFailureRetainsSchedulerCause() {
        startedBundle = new Hk2JobsBundle(BuilderHelper.createContractFilter(Job.class.getName()));
        TestConfig config = new TestConfig();
        config.withQuartzProperty("org.quartz.threadPool.threadCount", "0");
        AbstractContainerLifecycleListener listener = registerListener(startedBundle, config);
        ApplicationHandler handler = new ApplicationHandler(DropwizardResourceConfig.forTesting());
        locator = handler.getInjectionManager().getInstance(ServiceLocator.class);
        Container container = mock(Container.class);
        when(container.getApplicationHandler()).thenReturn(handler);

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> listener.onStartup(container));
        assertInstanceOf(SchedulerException.class, failure.getCause());
    }

    @Test
    public void shutdownFailureRetainsOriginalCause() throws Exception {
        Hk2JobsBundle bundle = new Hk2JobsBundle(BuilderHelper.createContractFilter(Job.class.getName()));
        AbstractContainerLifecycleListener listener = registerListener(bundle, new TestConfig());
        bundle.jobManager = mock(JobManager.class);
        SchedulerException expected = new SchedulerException("shutdown failed");
        doThrow(expected).when(bundle.jobManager).stop();

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> listener.onShutdown(null));
        assertSame(expected, failure.getCause());
    }

    @Test
    public void shutdownBeforeStartupDoesNotCreateAManager() {
        Hk2JobsBundle bundle = new Hk2JobsBundle(BuilderHelper.createContractFilter(Job.class.getName()));
        AbstractContainerLifecycleListener listener = registerListener(bundle, new TestConfig());
        assertDoesNotThrow(() -> listener.onShutdown(null));
        assertNull(bundle.getScheduler());
    }

    private <JobType extends Job> JobType findJob(Class<JobType> jobClass) {
        return locator.getAllServices(Job.class).stream().filter(jobClass::isInstance).map(jobClass::cast).findFirst().orElseThrow();
    }

    private AbstractContainerLifecycleListener registerListener(Hk2JobsBundle bundle, JobConfiguration config) {
        when(environment.jersey()).thenReturn(jerseyEnvironment);
        bundle.run(config, environment);
        ArgumentCaptor<AbstractContainerLifecycleListener> listener = ArgumentCaptor.forClass(AbstractContainerLifecycleListener.class);
        verify(jerseyEnvironment).register(listener.capture());
        return listener.getValue();
    }

    public static class TestConfig extends MutableJobConfiguration {
        public TestConfig() {
            withQuartzProperties(DefaultQuartzConfiguration.get());
            withQuartzProperty("org.quartz.scheduler.instanceName", "hk2-bundle-" + UUID.randomUUID());
        }
    }
}

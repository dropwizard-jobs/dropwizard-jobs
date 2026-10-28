package io.dropwizard.jobs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.concurrent.TimeUnit;
import java.util.UUID;
import java.util.Set;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import io.dropwizard.jobs.annotations.ListeningFor;
import org.quartz.JobKey;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.listeners.JobListenerSupport;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.inject.Singleton;

import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

@Timeout(15)
public class Hk2JobManagerTest {

    private JobManager jobManager;
    private ServiceLocator locator;

    @AfterEach
    public void closeResources() throws Exception {
        try {
            if (jobManager != null && jobManager.getScheduler() != null && !jobManager.getScheduler().isShutdown()) {
                jobManager.getScheduler().shutdown(true);
            }
        } finally {
            if (locator != null) {
                ServiceLocatorFactory.getInstance().destroy(locator);
            }
        }
    }

    @BeforeEach
    public void setUp() {
        // Create a service locator with a unique name
        locator = ServiceLocatorUtilities.createAndPopulateServiceLocator("testLocator" + System.nanoTime());
        
        // Bind jobs with both their concrete class AND Job.class as contract
        // Use .in(Singleton.class) so the same instance is used by both Quartz and the test
        ServiceLocatorUtilities.bind(locator, new AbstractBinder() {
            @Override
            protected void configure() {
                bindAsContract(Dependency.class);
                // Bind as singleton so test can access the same instance that Quartz executes
                bind(ApplicationStartTestJob.class).to(Job.class).to(ApplicationStartTestJob.class).in(Singleton.class);
                bind(OnTestJob.class).to(Job.class).to(OnTestJob.class).in(Singleton.class);
                bind(EveryTestJob.class).to(Job.class).to(EveryTestJob.class).in(Singleton.class);
                bind(DependencyTestJob.class).to(Job.class).to(DependencyTestJob.class).in(Singleton.class);
                bind(ApplicationStopTestJob.class).to(Job.class).to(ApplicationStopTestJob.class).in(Singleton.class);
                bind(RecordingListener.class).to(JobListener.class).to(RecordingListener.class).in(Singleton.class);
            }
        });
        
        // Force reification by getting all services handles - this initializes the descriptors properly
        locator.getAllServiceHandles(Job.class);
        
        Filter jobFilter = BuilderHelper.createContractFilter(Job.class.getName());
        jobManager = new Hk2JobManager(new TestConfig(), locator, jobFilter, null);
    }

    @Test
    public void testAllHk2Jobs() throws Exception {
        assertTrue(jobManager.getJobListeners().isEmpty());
        jobManager.start();
        // Get the singleton instance from the locator and check its latch
        ApplicationStartTestJob startJob = locator.getService(ApplicationStartTestJob.class);
        assertThat(startJob.latch.await(1, TimeUnit.SECONDS), is(true));
        
        OnTestJob onJob = locator.getService(OnTestJob.class);
        assertThat(onJob.latch.await(2, TimeUnit.SECONDS), is(true));
        
        EveryTestJob everyJob = locator.getService(EveryTestJob.class);
        assertThat(everyJob.latch.await(2, TimeUnit.SECONDS), is(true));
        
        DependencyTestJob depJob = locator.getService(DependencyTestJob.class);
        assertThat(depJob.latch.await(2, TimeUnit.SECONDS), is(true));

        jobManager.stop();
        
        ApplicationStopTestJob stopJob = locator.getService(ApplicationStopTestJob.class);
        assertThat(stopJob.latch.await(1, TimeUnit.SECONDS), is(true));
    }

    @Test
    public void discoversInjectedListenerAndFiltersExecutions() throws Exception {
        jobManager = new Hk2JobManager(new TestConfig(), locator,
                BuilderHelper.createContractFilter(Job.class.getName()),
                BuilderHelper.createContractFilter(JobListener.class.getName()));
        RecordingListener listener = locator.getService(RecordingListener.class);
        assertEquals(java.util.List.of(listener), jobManager.getJobListeners());
        assertNotNull(listener.dependency);
        jobManager.start();
        assertTrue(locator.getService(ApplicationStartTestJob.class).latch.await(2, TimeUnit.SECONDS));
        assertTrue(listener.executed.await(2, TimeUnit.SECONDS));
        jobManager.stop();
        assertEquals(Set.of(JobKey.jobKey(EveryTestJob.class.getCanonicalName())), Set.copyOf(listener.jobs));
    }

    @ListeningFor(matcher = ListeningFor.MatcherType.JOB_NAME_EQUALS, value = "io.dropwizard.jobs.EveryTestJob")
    public static class RecordingListener extends JobListenerSupport {
        final Dependency dependency;
        final CountDownLatch executed = new CountDownLatch(1);
        final Queue<JobKey> jobs = new ConcurrentLinkedQueue<>();

        @jakarta.inject.Inject
        public RecordingListener(Dependency dependency) {
            this.dependency = dependency;
        }

        @Override
        public String getName() {
            return getClass().getName();
        }

        @Override
        public void jobWasExecuted(JobExecutionContext context, JobExecutionException failure) {
            jobs.add(context.getJobDetail().getKey());
            executed.countDown();
        }
    }

    public static class TestConfig extends MutableJobConfiguration {
        public TestConfig() {
            withQuartzProperties(DefaultQuartzConfiguration.get());
            withQuartzProperty("org.quartz.scheduler.instanceName", "hk2-manager-" + UUID.randomUUID());
        }
    }
}

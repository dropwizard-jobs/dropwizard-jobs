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
import org.quartz.listeners.JobListenerSupport;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

@Timeout(15)
public class GuiceJobManagerTest {

    private JobManager jobManager;
    private Injector injector;

    @AfterEach
    public void shutdownScheduler() throws Exception {
        if (jobManager != null && jobManager.getScheduler() != null && !jobManager.getScheduler().isShutdown()) {
            jobManager.getScheduler().shutdown(true);
        }
    }

    @BeforeEach
    public void ensureLatchesAreZero() {
        injector = Guice.createInjector((Module) binder -> {
            binder.bind(ApplicationStartTestJob.class).asEagerSingleton();
            binder.bind(OnTestJob.class).asEagerSingleton();
            binder.bind(EveryTestJob.class).asEagerSingleton();
            binder.bind(DependencyTestJob.class).asEagerSingleton();
            binder.bind(ApplicationStopTestJob.class).asEagerSingleton();
            binder.bind(RecordingListener.class).asEagerSingleton();
        });
        jobManager = new GuiceJobManager(new TestConfig(), injector);
    }

    @Test
    public void testAllGuiceJobs() throws Exception {
        jobManager.start();
        assertThat(injector.getInstance(ApplicationStartTestJob.class).latch.await(1, TimeUnit.SECONDS), is(true));

        assertThat(injector.getInstance(OnTestJob.class).latch.await(2, TimeUnit.SECONDS), is(true));
        assertThat(injector.getInstance(EveryTestJob.class).latch.await(2, TimeUnit.SECONDS), is(true));
        assertThat(injector.getInstance(DependencyTestJob.class).latch.await(2, TimeUnit.SECONDS), is(true));

        jobManager.stop();
        assertThat(injector.getInstance(ApplicationStopTestJob.class).latch.await(1, TimeUnit.SECONDS), is(true));
    }
    
    @Test
    public void discoversInjectedListenerAndFiltersExecutions() throws Exception {
        RecordingListener listener = injector.getInstance(RecordingListener.class);
        assertEquals(java.util.List.of(listener), jobManager.getJobListeners());
        assertNotNull(listener.dependency);
        jobManager.start();
        assertTrue(injector.getInstance(ApplicationStartTestJob.class).latch.await(2, TimeUnit.SECONDS));
        assertTrue(listener.executed.await(2, TimeUnit.SECONDS));
        jobManager.stop();
        assertEquals(Set.of(JobKey.jobKey(EveryTestJob.class.getCanonicalName())), Set.copyOf(listener.jobs));
    }

    @ListeningFor(matcher = ListeningFor.MatcherType.JOB_NAME_EQUALS, value = "io.dropwizard.jobs.EveryTestJob")
    public static class RecordingListener extends JobListenerSupport {
        final Dependency dependency;
        final CountDownLatch executed = new CountDownLatch(1);
        final Queue<JobKey> jobs = new ConcurrentLinkedQueue<>();

        @com.google.inject.Inject
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
            withQuartzProperty("org.quartz.scheduler.instanceName", "guice-manager-" + UUID.randomUUID());
        }
    }
}

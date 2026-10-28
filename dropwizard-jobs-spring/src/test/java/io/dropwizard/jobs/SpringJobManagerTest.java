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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@Timeout(15)
public class SpringJobManagerTest {

    private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ApplicationStartTestJob.class,
            ApplicationStopTestJob.class, EveryTestJob.class, OnTestJob.class, DependencyTestJob.class,
            Dependency.class, RecordingListener.class);
    private final JobManager jobManager = new SpringJobManager(new TestConfig(), context);

    @AfterEach
    public void closeResources() throws Exception {
        try {
            if (jobManager.getScheduler() != null && !jobManager.getScheduler().isShutdown()) {
                jobManager.getScheduler().shutdown(true);
            }
        } finally {
            context.close();
        }
    }

    @Test
    public void testThatJobsAreExecuted() throws Exception {
        jobManager.start();
        assertThat(context.getBean(ApplicationStartTestJob.class).latch.await(1, TimeUnit.SECONDS), is(true));

        assertThat(context.getBean(OnTestJob.class).latch.await(2, TimeUnit.SECONDS), is(true));
        assertThat(context.getBean(DependencyTestJob.class).latch.await(2, TimeUnit.SECONDS), is(true));
        assertThat(context.getBean(EveryTestJob.class).latch.await(2, TimeUnit.SECONDS), is(true));

        jobManager.stop();
        assertThat(context.getBean(ApplicationStopTestJob.class).latch.await(1, TimeUnit.SECONDS), is(true));
    }
    
    @Test
    public void discoversInjectedListenerAndFiltersExecutions() throws Exception {
        RecordingListener listener = context.getBean(RecordingListener.class);
        assertEquals(java.util.List.of(listener), jobManager.getJobListeners());
        assertSame(context.getBean(Dependency.class), listener.dependency);
        jobManager.start();
        assertTrue(context.getBean(ApplicationStartTestJob.class).latch.await(2, TimeUnit.SECONDS));
        assertTrue(listener.executed.await(2, TimeUnit.SECONDS));
        jobManager.stop();
        assertEquals(Set.of(JobKey.jobKey(EveryTestJob.class.getCanonicalName())), Set.copyOf(listener.jobs));
    }

    @ListeningFor(matcher = ListeningFor.MatcherType.JOB_NAME_EQUALS, value = "io.dropwizard.jobs.EveryTestJob")
    public static class RecordingListener extends JobListenerSupport {
        final Dependency dependency;
        final CountDownLatch executed = new CountDownLatch(1);
        final Queue<JobKey> jobs = new ConcurrentLinkedQueue<>();

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
            withQuartzProperty("org.quartz.scheduler.instanceName", "spring-manager-" + UUID.randomUUID());
        }
    }
}
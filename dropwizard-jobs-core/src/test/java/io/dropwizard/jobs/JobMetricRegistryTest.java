package io.dropwizard.jobs;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.dropwizard.jobs.Job.DROPWIZARD_JOBS_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@Timeout(10)
@ResourceLock(DROPWIZARD_JOBS_KEY)
public class JobMetricRegistryTest {

    @BeforeEach
    @AfterEach
    public void cleanupSharedMetricRegistries() {
        SharedMetricRegistries.remove(DROPWIZARD_JOBS_KEY);
    }

    @Test
    public void jobUsesBootstrapRegistryWhenCreatedBeforeBundleInitialize() throws JobExecutionException {
        // Step 1: Create a job via default constructor (lazy path) BEFORE JobsBundle.initialize()
        Job job = new TestJob();

        // Step 2: Simulate what JobsBundle.initialize() does - remove any pre-existing registry
        // and add the bootstrap registry
        MetricRegistry expectedRegistry = new MetricRegistry();
        SharedMetricRegistries.remove(DROPWIZARD_JOBS_KEY);
        SharedMetricRegistries.add(DROPWIZARD_JOBS_KEY, expectedRegistry);

        // Step 3: Execute the job (or call getMetricRegistry) to trigger lazy resolution
        JobExecutionContext mockContext = mock(JobExecutionContext.class);
        job.execute(mockContext);

        // Step 4: Verify the job uses the bootstrap registry (same instance)
        assertThat(job.getMetricRegistry(), is(sameInstance(expectedRegistry)));
    }

    @Test
    public void jobWithExplicitRegistryUsesProvidedRegistry() {
        // Create a MetricRegistry explicitly
        MetricRegistry explicitRegistry = new MetricRegistry();

        // Create a Job via the explicit constructor
        Job job = new TestJob(explicitRegistry);

        // Assert getMetricRegistry() returns the same instance
        assertThat(job.getMetricRegistry(), is(sameInstance(explicitRegistry)));
    }

    @Test
    public void lazyInitializationIsThreadSafe() throws Exception {
        // Create a job with lazy initialization
        Job job = new TestJob();

        // Set up the shared registry
        MetricRegistry expectedRegistry = new MetricRegistry();
        SharedMetricRegistries.add(DROPWIZARD_JOBS_KEY, expectedRegistry);

        // Create a latch to synchronize all threads
        int numThreads = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(numThreads);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        List<Future<MetricRegistry>> futures = new ArrayList<>();

        try {
            for (int threadIndex = 0; threadIndex < numThreads; threadIndex++) {
                futures.add(executor.submit(() -> {
                    try {
                        assertTrue(startLatch.await(5, TimeUnit.SECONDS));
                        job.execute(mock(JobExecutionContext.class));
                        return job.getMetricRegistry();
                    } finally {
                        doneLatch.countDown();
                    }
                }));
            }

            startLatch.countDown();
            assertTrue(doneLatch.await(5, TimeUnit.SECONDS));

            for (Future<MetricRegistry> future : futures) {
                assertThat(future.get(5, TimeUnit.SECONDS), is(sameInstance(expectedRegistry)));
            }
            assertEquals(numThreads, expectedRegistry.timer(MetricRegistry.name(job.getClass())).getCount());
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void successfulExecutionReceivesOriginalContextAndRecordsTiming() throws Exception {
        MetricRegistry registry = new MetricRegistry();
        AtomicReference<JobExecutionContext> received = new AtomicReference<>();
        Job job = new TestJob(registry) {
            @Override
            public void doJob(JobExecutionContext context) {
                received.set(context);
            }
        };
        JobExecutionContext context = mock(JobExecutionContext.class);

        job.execute(context);

        assertSame(context, received.get());
        assertEquals(1, registry.timer(MetricRegistry.name(job.getClass())).getCount());
    }

    @Test
    public void failedExecutionRecordsTimingAndPreservesQuartzExceptionFlags() {
        MetricRegistry registry = new MetricRegistry();
        IllegalStateException cause = new IllegalStateException("dependency failed");
        JobExecutionException expected = new JobExecutionException(cause);
        expected.setRefireImmediately(true);
        expected.setUnscheduleFiringTrigger(true);
        expected.setUnscheduleAllTriggers(true);
        Job job = new TestJob(registry) {
            @Override
            public void doJob(JobExecutionContext context) throws JobExecutionException {
                throw expected;
            }
        };

        JobExecutionException actual = assertThrows(JobExecutionException.class, () -> job.execute(mock(JobExecutionContext.class)));

        assertSame(expected, actual);
        assertSame(cause, actual.getCause());
        assertTrue(actual.refireImmediately());
        assertTrue(actual.unscheduleFiringTrigger());
        assertTrue(actual.unscheduleAllTriggers());
        assertEquals(1, registry.timer(MetricRegistry.name(job.getClass())).getCount());
    }

    @Test
    public void uncheckedFailureAlsoRecordsTimingAndPreservesException() {
        MetricRegistry registry = new MetricRegistry();
        IllegalStateException expected = new IllegalStateException("execution failed");
        Job job = new TestJob(registry) {
            @Override
            public void doJob(JobExecutionContext context) {
                throw expected;
            }
        };

        assertSame(expected, assertThrows(IllegalStateException.class, () -> job.execute(mock(JobExecutionContext.class))));
        assertEquals(1, registry.timer(MetricRegistry.name(job.getClass())).getCount());
    }

    private static class TestJob extends Job {

        public TestJob() {
            super();
        }

        public TestJob(MetricRegistry metricRegistry) {
            super(metricRegistry);
        }

        @Override
        public void doJob(JobExecutionContext context) throws JobExecutionException {
            // No-op for testing
        }
    }
}

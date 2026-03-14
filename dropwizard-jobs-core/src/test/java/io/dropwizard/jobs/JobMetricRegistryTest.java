package io.dropwizard.jobs;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static io.dropwizard.jobs.Job.DROPWIZARD_JOBS_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;

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

        for (int i = 0; i < numThreads; i++) {
            futures.add(executor.submit(() -> {
                startLatch.await();
                try {
                    JobExecutionContext mockContext = mock(JobExecutionContext.class);
                    job.execute(mockContext);
                } finally {
                    doneLatch.countDown();
                }
                return job.getMetricRegistry();
            }));
        }

        // Release all threads simultaneously
        startLatch.countDown();

        // Wait for all threads to complete
        doneLatch.await();

        // Verify all threads got the same registry instance without exceptions
        for (Future<MetricRegistry> future : futures) {
            MetricRegistry registry = future.get();
            assertThat(registry, is(sameInstance(expectedRegistry)));
        }

        executor.shutdown();
    }

    /**
     * Simple test job implementation for testing metric registry behavior.
     */
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

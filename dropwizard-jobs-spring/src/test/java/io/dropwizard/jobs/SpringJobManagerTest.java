package io.dropwizard.jobs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.concurrent.TimeUnit;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

@Timeout(15)
public class SpringJobManagerTest {

    private final AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(ApplicationStartTestJob.class,
            ApplicationStopTestJob.class, EveryTestJob.class, OnTestJob.class, DependencyTestJob.class,
            Dependency.class);
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
    
    public static class TestConfig extends MutableJobConfiguration {
        public TestConfig() {
            withQuartzProperties(DefaultQuartzConfiguration.get());
            withQuartzProperty("org.quartz.scheduler.instanceName", "spring-manager-" + UUID.randomUUID());
        }
    }
}
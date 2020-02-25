package io.dropwizard.jobs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class SpringJobManagerTest {

    private final ApplicationContext context = new AnnotationConfigApplicationContext(ApplicationStartTestJob.class,
            ApplicationStopTestJob.class, EveryTestJob.class, OnTestJob.class, DependencyTestJob.class,
            Dependency.class);
    private final JobManager jobManager = new SpringJobManager(new TestConfig(), context);

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
    
    public static class TestConfig extends JobConfiguration {
        
    }
}
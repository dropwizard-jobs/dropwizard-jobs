package de.spinscale.dropwizard.jobs;

import org.junit.Before;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class SpringJobManagerTest {

    private ApplicationContext context = new AnnotationConfigApplicationContext(ApplicationStartTestJob.class,
            ApplicationStopTestJob.class, EveryTestJob.class, OnTestJob.class, DependencyTestJob.class,
            Dependency.class);
    private JobManager jobManager = new SpringJobManager(context);

    @Before
    public void ensureLatchesAreZero() {
        assertThat(ApplicationStartTestJob.latch.getCount(), is(1L));
        assertThat(OnTestJob.latch.getCount(), is(2L));
        assertThat(EveryTestJob.latch.getCount(), is(5L));
        assertThat(DependencyTestJob.latch.getCount(), is(5L));
        assertThat(ApplicationStopTestJob.latch.getCount(), is(1L));
    }

    @Test
    public void testThatJobsAreExecuted() throws Exception {
        jobManager.start();
        assertThat(ApplicationStartTestJob.latch.await(1, TimeUnit.SECONDS), is(true));

        assertThat(OnTestJob.latch.await(2, TimeUnit.SECONDS), is(true));
        assertThat(DependencyTestJob.latch.await(2, TimeUnit.SECONDS), is(true));
        assertThat(EveryTestJob.latch.await(2, TimeUnit.SECONDS), is(true));

        jobManager.stop();
        assertThat(ApplicationStopTestJob.latch.await(1, TimeUnit.SECONDS), is(true));
    }
}

package de.spinscale.dropwizard.jobs;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;

public class JobManagerTest {

    JobManager jobManager = new JobManager();

    @Test
    public void jobsOnStartupShouldBeExecuted() throws Exception {
        jobManager.start();
        Thread.sleep(1000);
        assertThat(ApplicationStartTestJob.results, hasSize(1));
    }

    @Test
    public void jobsOnStoppingShouldBeExecuted() throws Exception {
        jobManager.start();
        jobManager.stop();
        assertThat(ApplicationStopTestJob.results, hasSize(1));
    }

    @Test
    public void jobsWithOnAnnotationShouldBeExecuted() throws Exception {
        OnTestJob.results.clear();
        jobManager.start();
        Thread.sleep(5000);
        assertThat(OnTestJob.results, hasSize(greaterThan(5)));
    }

    @Test
    public void jobsWithEveryAnnotationShouldBeExecuted() throws Exception {
        EveryTestJob.results.clear();
        jobManager.start();
        Thread.sleep(5000);
        assertThat(EveryTestJob.results, hasSize(greaterThan(5)));
    }
}

package de.spinscale.dropwizard.jobs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.hamcrest.core.IsEqual;
import org.junit.Before;
import org.junit.Test;
import org.quartz.JobKey;

import de.spinscale.dropwizard.jobs.annotations.Every;
import de.spinscale.dropwizard.jobs.annotations.On;
import io.dropwizard.Configuration;

public class JobManagerTest {

    private JobManager jobManager = new JobManager();

    @Before
    public void ensureLatchesAreUntouched() {
        assertThat(ApplicationStartTestJob.latch.getCount(), is(1L));
        assertThat(OnTestJob.latch.getCount(), is(2L));
        assertThat(EveryTestJob.latch.getCount(), is(5L));
        assertThat(EveryTestJobWithDelay.latch.getCount(), is(5L));
        assertThat(EveryTestJobAlternativeConfiguration.latch.getCount(), is(5L));
        assertThat(ApplicationStopTestJob.latch.getCount(), is(1L));
    }

    @Test
    public void testThatJobsAreExecuted() throws Exception {
        TestConfig config = new TestConfig();
        config.getJobs().put("everyTestJobDefaultConfiguration", "50ms");
        config.getJobs().put("testJob", "50ms");
        jobManager.configure(config);

        jobManager.start();

        // a job with an @Every annotation that doesn't specify a job name should be assigned the canonical class name
        String jobName = EveryTestJob.class.getCanonicalName();
        assertTrue(jobManager.scheduler.checkExists(JobKey.jobKey(jobName)));

        // a job with an @Every annotation that specifies a job name should get that name, not the canonical class name
        jobName = EveryTestJobWithJobName.class.getAnnotation(Every.class).jobName();
        assertTrue(jobManager.scheduler.checkExists(JobKey.jobKey(jobName)));

        // a job with an @On annotation that doesn't specify a job name should be assigned the canonical class name
        jobName = OnTestJob.class.getCanonicalName();
        assertTrue(jobManager.scheduler.checkExists(JobKey.jobKey(jobName)));

        // a job with an @On annotation that specifies a job name should get that name, not the canonical class name
        jobName = OnTestJobWithJobName.class.getAnnotation(On.class).jobName();
        assertTrue(jobManager.scheduler.checkExists(JobKey.jobKey(jobName)));

        // if there are two jobs that have the same name, only one job and trigger will be created with that job name
        // this simulates running in clustered environments where two or more nodes have the same set of jobs
        jobName = EveryTestJobWithJobName.class.getAnnotation(Every.class).jobName();
        assertThat(jobName, IsEqual.equalTo(EveryTestJobWithSameJobName.class.getAnnotation(Every.class).jobName()));
        assertTrue(jobManager.scheduler.checkExists(JobKey.jobKey(jobName)));
        assertThat(jobManager.scheduler.getTriggersOfJob(JobKey.jobKey(jobName)).size(), IsEqual.equalTo(1));

        assertThat(EveryTestJobWithDelay.latch.await(800, TimeUnit.MILLISECONDS), is(false));
        assertThat(ApplicationStartTestJob.latch.await(1, TimeUnit.SECONDS), is(true));
        assertThat(EveryTestJobAlternativeConfiguration.latch.await(1, TimeUnit.SECONDS), is(true));
        assertThat(EveryTestJobWithDelay.latch.await(2, TimeUnit.SECONDS), is(true));
        assertThat(EveryTestJob.latch.await(1, TimeUnit.SECONDS), is(true));
        assertThat(OnTestJob.latch.await(5, TimeUnit.SECONDS), is(true));

        jobManager.stop();

        assertThat(ApplicationStopTestJob.latch.await(2, TimeUnit.SECONDS), is(true));
    }

    private static class TestConfig extends Configuration {
        private Map<String, String> jobs = new HashMap<>();

        public Map<String, String> getJobs() {
            return jobs;
        }
    }
}
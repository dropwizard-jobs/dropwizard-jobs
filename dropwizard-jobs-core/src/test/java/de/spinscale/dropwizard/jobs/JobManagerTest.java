package de.spinscale.dropwizard.jobs;

import de.spinscale.dropwizard.jobs.annotations.Every;
import de.spinscale.dropwizard.jobs.annotations.On;
import io.dropwizard.Configuration;
import org.hamcrest.core.IsEqual;
import org.junit.Before;
import org.junit.Test;
import org.quartz.JobKey;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;

public class JobManagerTest {

    private JobManager jobManager = new JobManager();
    private ApplicationStartTestJob startTestJob = new ApplicationStartTestJob();
    private OnTestJob onTestJob = new OnTestJob();
    private OnTestJobWithJobName onTestJobWithJobName = new OnTestJobWithJobName();
    private EveryTestJob everyTestJob = new EveryTestJob();
    private EveryTestJobWithDelay everyTestJobWithDelay = new EveryTestJobWithDelay();
    private EveryTestJobAlternativeConfiguration everyTestJobAlternativeConfiguration = new EveryTestJobAlternativeConfiguration();
    private EveryTestJobWithJobName everyTestJobWithJobName = new EveryTestJobWithJobName();
    private ApplicationStopTestJob applicationStopTestJob = new ApplicationStopTestJob();

    @Before
    public void ensureLatchesAreUntouched() {
        jobManager = new JobManager(startTestJob, onTestJob, onTestJobWithJobName, everyTestJob, everyTestJobWithJobName,
                everyTestJobWithDelay, everyTestJobAlternativeConfiguration, applicationStopTestJob);
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

        assertThat(everyTestJobWithDelay.latch().await(100, TimeUnit.MILLISECONDS), is(false));
        assertThat(everyTestJobWithJobName.latch().await(1, TimeUnit.SECONDS), is(true));
        assertThat(everyTestJobAlternativeConfiguration.latch().await(1, TimeUnit.SECONDS), is(true));
        assertThat(everyTestJob.latch().await(1, TimeUnit.SECONDS), is(true));

        assertThat(everyTestJobWithDelay.latch().await(2, TimeUnit.SECONDS), is(true));
        assertThat(onTestJob.latch().await(2, TimeUnit.SECONDS), is(true));

        jobManager.stop();

        assertThat(applicationStopTestJob.latch().await(2, TimeUnit.SECONDS), is(true));
    }

    private static class TestConfig extends Configuration implements JobConfiguration {
        private Map<String, String> jobs = new HashMap<>();

        public Map<String, String> getJobs() {
            return jobs;
        }
    }
}
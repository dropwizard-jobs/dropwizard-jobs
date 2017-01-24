package de.spinscale.dropwizard.jobs;

import de.spinscale.dropwizard.jobs.annotations.Every;
import de.spinscale.dropwizard.jobs.annotations.On;

import org.hamcrest.core.IsEqual;
import org.junit.Before;
import org.junit.Test;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Trigger;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.dropwizard.Configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

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

    @Test
    public void testJobsWithDefaultConfiguration() throws Exception {
        jobManager = new JobManager(new EveryTestJobWithDefaultConfiguration(), new OnTestJobWithDefaultConfiguration());

        jobManager.start();

        String jobName = EveryTestJobWithDefaultConfiguration.class.getCanonicalName();
        JobDetail jobDetail = jobManager.scheduler.getJobDetail(JobKey.jobKey(jobName));
        Trigger trigger = jobManager.scheduler.getTriggersOfJob(JobKey.jobKey(jobName)).get(0);

        assertEquals(false, jobDetail.requestsRecovery());
        assertEquals(false, jobDetail.isDurable());
        assertEquals(Trigger.DEFAULT_PRIORITY, trigger.getPriority());
        assertEquals(Trigger.MISFIRE_INSTRUCTION_SMART_POLICY, trigger.getMisfireInstruction());

        jobName = OnTestJobWithDefaultConfiguration.class.getCanonicalName();
        jobDetail = jobManager.scheduler.getJobDetail(JobKey.jobKey(jobName));
        trigger = jobManager.scheduler.getTriggersOfJob(JobKey.jobKey(jobName)).get(0);

        assertEquals(false, jobDetail.requestsRecovery());
        assertEquals(false, jobDetail.isDurable());
        assertEquals(Trigger.DEFAULT_PRIORITY, trigger.getPriority());
        assertEquals(Trigger.MISFIRE_INSTRUCTION_SMART_POLICY, trigger.getMisfireInstruction());

        jobManager.stop();
    }

    @Test
    public void testJobsWithNonDefaultConfiguration() throws Exception {
        jobManager = new JobManager(new EveryTestJobWithNonDefaultConfiguration(), new OnTestJobWithNonDefaultConfiguration());

        jobManager.start();

        String jobName = EveryTestJobWithNonDefaultConfiguration.class.getCanonicalName();
        JobDetail jobDetail = jobManager.scheduler.getJobDetail(JobKey.jobKey(jobName));
        Trigger trigger = jobManager.scheduler.getTriggersOfJob(JobKey.jobKey(jobName)).get(0);

        assertEquals(true, jobDetail.requestsRecovery());
        assertEquals(true, jobDetail.isDurable());
        assertEquals(20, trigger.getPriority());
        assertEquals(Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY, trigger.getMisfireInstruction());

        jobName = OnTestJobWithNonDefaultConfiguration.class.getCanonicalName();
        jobDetail = jobManager.scheduler.getJobDetail(JobKey.jobKey(jobName));
        trigger = jobManager.scheduler.getTriggersOfJob(JobKey.jobKey(jobName)).get(0);

        assertEquals(true, jobDetail.requestsRecovery());
        assertEquals(true, jobDetail.isDurable());
        assertEquals(20, trigger.getPriority());
        assertEquals(Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY, trigger.getMisfireInstruction());

        jobManager.stop();
    }

    private static class TestConfig extends Configuration implements JobConfiguration {
        private Map<String, String> jobs = new HashMap<>();

        public Map<String, String> getJobs() {
            return jobs;
        }
    }

    @Every("10ms")
    class EveryTestJobWithDefaultConfiguration extends AbstractJob {
        public EveryTestJobWithDefaultConfiguration() { super(1); }
    }

    @On("0/1 * * * * ?")
    class OnTestJobWithDefaultConfiguration extends AbstractJob {
        public OnTestJobWithDefaultConfiguration() { super(1); }
    }

    @Every(value = "10ms", requestRecovery = true, storeDurably = true, priority = 20, misfirePolicy = Every.MisfirePolicy.IGNORE_MISFIRES)
    class EveryTestJobWithNonDefaultConfiguration extends AbstractJob {
        public EveryTestJobWithNonDefaultConfiguration() { super(1); }
    }

    @On(value = "0/1 * * * * ?", requestRecovery = true, storeDurably = true, priority = 20, misfirePolicy = On.MisfirePolicy.IGNORE_MISFIRES)
    class OnTestJobWithNonDefaultConfiguration extends AbstractJob {
        public OnTestJobWithNonDefaultConfiguration() { super(1); }
    }

}

package io.dropwizard.jobs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.Test;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerConfigException;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.triggers.CronTriggerImpl;

import io.dropwizard.jobs.annotations.Every;
import io.dropwizard.jobs.annotations.On;

public class JobManagerTest {

    private JobManager jobManager = new JobManager(new TestConfig());
    private ApplicationStartTestJob startTestJob = new ApplicationStartTestJob();
    private OnTestJob onTestJob = new OnTestJob();
    private OnTestJobWithJobName onTestJobWithJobName = new OnTestJobWithJobName();
    private EveryTestJob everyTestJob = new EveryTestJob();
    private EveryTestJobWithDelay everyTestJobWithDelay = new EveryTestJobWithDelay();
    private EveryTestJobAlternativeConfiguration everyTestJobAlternativeConfiguration = new EveryTestJobAlternativeConfiguration();
    private EveryTestJobWithJobName everyTestJobWithJobName = new EveryTestJobWithJobName();
    private ApplicationStopTestJob applicationStopTestJob = new ApplicationStopTestJob();
    private OnTestJobAlternativeConfiguration onTestJobAlternativeConfiguration = new OnTestJobAlternativeConfiguration();

    @Test
    public void testThatJobsAreExecuted() throws Exception {
        TestConfig config = new TestConfig();
        config.getJobs().put("everyTestJobDefaultConfiguration", "50ms");
        config.getJobs().put("testJob", "50ms");
        config.getJobs().put("onTestJob", "0/1 * * * * ?");
        jobManager = new JobManager(config, startTestJob, onTestJob, onTestJobWithJobName, everyTestJob,
                everyTestJobWithJobName, everyTestJobWithDelay, everyTestJobAlternativeConfiguration,
                applicationStopTestJob, onTestJobAlternativeConfiguration);
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

        // test an @On job which reads the cron expression from the config file
        assertThat(onTestJobAlternativeConfiguration.latch().await(2, TimeUnit.SECONDS), is(true));

        jobManager.stop();

        assertThat(applicationStopTestJob.latch().await(2, TimeUnit.SECONDS), is(true));
    }

    @Test
    public void testJobsWithDefaultConfiguration() throws Exception {
        jobManager = new JobManager(new TestConfig(), new EveryTestJobWithDefaultConfiguration(),
                new OnTestJobWithDefaultConfiguration());

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
    public void testJobsWithMultipleInstances() throws Exception {
        jobManager = new JobManager(
            new TestConfig(),
            new OnTestJobWithVariableGroupName("group_one"),
            new OnTestJobWithVariableGroupName("group_two"),
            new EveryTestJobWithDefaultConfiguration()
        );

        jobManager.start();
        List<String> jobGroupNames = jobManager.scheduler.getJobGroupNames();

        String jobName = EveryTestJobWithDefaultConfiguration.class.getCanonicalName();
        JobDetail jobDetail = jobManager.scheduler.getJobDetail(JobKey.jobKey(jobName));
        Trigger trigger = jobManager.scheduler.getTriggersOfJob(JobKey.jobKey(jobName)).get(0);

        assertNotNull(jobDetail);
        assertNotNull(trigger);
        assertTrue(jobGroupNames.containsAll(Arrays.asList("group_one", "group_two")));
        assertThat(jobManager.scheduler.getJobKeys(GroupMatcher.anyGroup()).size(), equalTo(3));

        jobManager.stop();
    }

    @Test
    public void testJobsWithoutGroupShouldOnlyHaveOneInstance() throws Exception {
        jobManager = new JobManager(
            new TestConfig(),
            new EveryTestJobWithDefaultConfiguration(),
            new EveryTestJobWithDefaultConfiguration()
        );

        jobManager.start();

        String jobName = EveryTestJobWithDefaultConfiguration.class.getCanonicalName();
        JobDetail jobDetail = jobManager.scheduler.getJobDetail(JobKey.jobKey(jobName));
        Trigger trigger = jobManager.scheduler.getTriggersOfJob(JobKey.jobKey(jobName)).get(0);

        assertNotNull(jobDetail);
        assertNotNull(trigger);
        assertThat(jobManager.scheduler.getJobKeys(GroupMatcher.anyGroup()).size(), equalTo(1));

        jobManager.stop();
    }

    @Test
    public void testJobsWithTimeZoneInOnAnnotation() throws Exception {
        jobManager = new JobManager(new TestConfig(), new OnTestJobWithTimeZoneConfiguration(),
            new OnTestJobWithDefaultConfiguration());
        jobManager.start();

        String jobName = OnTestJobWithTimeZoneConfiguration.class.getCanonicalName();
        CronTrigger trigger = (CronTrigger) jobManager.scheduler.getTriggersOfJob(JobKey.jobKey(jobName)).get(0);

        assertEquals("Europe/Stockholm", trigger.getTimeZone().getID());

        jobManager.stop();
    }

    @Test
    public void testJobsWithNonDefaultConfiguration() throws Exception {
        jobManager = new JobManager(new TestConfig(), new EveryTestJobWithNonDefaultConfiguration(),
                new OnTestJobWithNonDefaultConfiguration());

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

    @Test
    public void shouldFallbackToNormalBehaviourWhenNoPropertiesAreDefined() throws Exception {
        TestConfig config = new TestConfig();
        config.getQuartzConfiguration().clear();

        jobManager = new JobManager(config, startTestJob, onTestJob, onTestJobWithJobName, everyTestJob,
                everyTestJobWithJobName);
        jobManager.start();

        assertThat(jobManager.getScheduler().getMetaData().getThreadPoolSize(), Matchers.is(10));
        jobManager.stop();
    }

    @Test(expected = SchedulerConfigException.class)
    public void throwsExceptionWhenNoPropertiesAreDefined() throws Exception {
        TestConfig config = new TestConfig();
        config.getQuartzConfiguration().clear();
        config.getQuartzConfiguration().put("some", "property");

        jobManager = new JobManager(config, startTestJob, onTestJob, onTestJobWithJobName, everyTestJob,
                everyTestJobWithJobName);
        jobManager.start();
    }

    @Test
    public void shouldSetDropwizardPropertiesOnSchedulerFactory() throws Exception {
        TestConfig config = new TestConfig();
        // change configuration
        config.getQuartzConfiguration().put("org.quartz.threadPool.threadCount", "15");
        jobManager = new JobManager(config, startTestJob, onTestJob, onTestJobWithJobName, everyTestJob,
                everyTestJobWithJobName);
        jobManager.start();
        assertThat(jobManager.getScheduler().getMetaData().getThreadPoolSize(), Matchers.is(15));
        jobManager.stop();
    }

    @Test
    public void allowTimezoneConfiguration() throws Exception {
        TestConfig config = new TestConfig();
        config.getQuartzConfiguration().put("de.spinscale.dropwizard.jobs.timezone", "Europe/London");

        jobManager = new JobManager(config, startTestJob, onTestJob, onTestJobWithJobName, everyTestJob,
                everyTestJobWithJobName);

        // by default, crons should use the configuration setting's timezone
        CronTriggerImpl trigger1 = (CronTriggerImpl)(jobManager.createCronScheduleBuilder("0 15 10 ? * *").build());
        assertEquals(TimeZone.getTimeZone("Europe/London"), trigger1.getTimeZone());
        assertEquals("0 15 10 ? * *", trigger1.getCronExpression());

        // can use [timezone] syntax to set a specific cron to a specific timezone
        CronTriggerImpl trigger2 = (CronTriggerImpl)(jobManager.createCronScheduleBuilder("0 15 10 ? * * [America/Los_Angeles]").build());
        assertEquals(TimeZone.getTimeZone("America/Los_Angeles"), trigger2.getTimeZone());
        assertEquals("0 15 10 ? * *", trigger2.getCronExpression());
    }

    private static class TestConfig extends JobConfiguration {
        private Map<String, String> quartzConfiguration;

        @SuppressWarnings("unchecked")
        private TestConfig() {
            quartzConfiguration = (Map<String, String>) ((HashMap<String, String>) DefaultQuartzConfiguration.get()).clone();
        }

        private Map<String, String> jobs = new HashMap<>();

        public Map<String, String> getJobs() {
            return jobs;
        }

        public Map<String, String> getQuartzConfiguration() {
            return quartzConfiguration;
        }
    }

    @Every("10ms")
    class EveryTestJobWithDefaultConfiguration extends AbstractJob {
        public EveryTestJobWithDefaultConfiguration() {
            super(1);
        }
    }

    @On("0/1 * * * * ?")
    class OnTestJobWithDefaultConfiguration extends AbstractJob {
        public OnTestJobWithDefaultConfiguration() {
            super(1);
        }
    }

    @On("0/1 * * * * ?")
    class OnTestJobWithVariableGroupName extends AbstractJob {
        public OnTestJobWithVariableGroupName(String groupName) {
            super(1, groupName);
        }
    }

    @On(value = "0 0 13 ? * MON", timeZone = "Europe/Stockholm")
    class OnTestJobWithTimeZoneConfiguration extends AbstractJob {
        public OnTestJobWithTimeZoneConfiguration() {
            super(1);
        }
    }

    @Every(value = "10ms", requestRecovery = true, storeDurably = true, priority = 20, misfirePolicy = Every.MisfirePolicy.IGNORE_MISFIRES)
    class EveryTestJobWithNonDefaultConfiguration extends AbstractJob {
        public EveryTestJobWithNonDefaultConfiguration() {
            super(1);
        }
    }

    @On(value = "0/1 * * * * ?", requestRecovery = true, storeDurably = true, priority = 20, misfirePolicy = On.MisfirePolicy.IGNORE_MISFIRES)
    class OnTestJobWithNonDefaultConfiguration extends AbstractJob {
        public OnTestJobWithNonDefaultConfiguration() {
            super(1);
        }
    }

}

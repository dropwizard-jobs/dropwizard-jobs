package io.dropwizard.jobs;

import io.dropwizard.jobs.annotations.Every;
import io.dropwizard.jobs.annotations.On;
import io.dropwizard.jobs.scheduler.CronExpressionParser;
import org.hamcrest.Matchers;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.triggers.CronTriggerImpl;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class JobManagerTest {

    private JobManager jobManager;
    private final ApplicationStartTestJob startTestJob = new ApplicationStartTestJob();
    private final OnTestJob onTestJob = new OnTestJob();
    private final OnTestJobWithJobName onTestJobWithJobName = new OnTestJobWithJobName();
    private final EveryTestJob everyTestJob = new EveryTestJob();

    @BeforeEach
    public void setUp() {
        jobManager = JobManager.fromJobs(new TestConfig(), new ArrayList<>());
    }
    private final EveryTestJobWithDelay everyTestJobWithDelay = new EveryTestJobWithDelay();
    private final EveryTestJobAlternativeConfiguration everyTestJobAlternativeConfiguration = new EveryTestJobAlternativeConfiguration();
    private final EveryTestJobWithJobName everyTestJobWithJobName = new EveryTestJobWithJobName();
    private final ApplicationStopTestJob applicationStopTestJob = new ApplicationStopTestJob();
    private final OnTestJobAlternativeConfiguration onTestJobAlternativeConfiguration = new OnTestJobAlternativeConfiguration();

    @Test
    public void testThatJobsAreExecuted() throws Exception {
        TestConfig config = new TestConfig();
        config.getJobs().put("everyTestJobDefaultConfiguration", "50ms");
        config.getJobs().put("testJob", "50ms");
        config.getJobs().put("onTestJob", "0/1 * * * * ?");
        jobManager = JobManager.fromJobs(config, List.of(startTestJob, onTestJob, onTestJobWithJobName, everyTestJob,
                everyTestJobWithJobName, everyTestJobWithDelay, everyTestJobAlternativeConfiguration,
                applicationStopTestJob, onTestJobAlternativeConfiguration));
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
        assertThat(startTestJob.latch().await(1, TimeUnit.SECONDS), is(true));

        assertThat(everyTestJobWithDelay.latch().await(2, TimeUnit.SECONDS), is(true));
        assertThat(onTestJob.latch().await(2, TimeUnit.SECONDS), is(true));

        // test an @On job which reads the cron expression from the config file
        assertThat(onTestJobAlternativeConfiguration.latch().await(2, TimeUnit.SECONDS), is(true));

        jobManager.stop();

        assertThat(applicationStopTestJob.latch().await(2, TimeUnit.SECONDS), is(true));
    }

    @Test
    public void testJobsWithDefaultConfiguration() throws Exception {
        jobManager = JobManager.fromJobs(new TestConfig(), List.of(new EveryTestJobWithDefaultConfiguration(),
                new OnTestJobWithDefaultConfiguration()));

        jobManager.start();

        String jobName = EveryTestJobWithDefaultConfiguration.class.getCanonicalName();
        JobDetail jobDetail = jobManager.scheduler.getJobDetail(JobKey.jobKey(jobName));
        Trigger trigger = jobManager.scheduler.getTriggersOfJob(JobKey.jobKey(jobName)).get(0);

        assertFalse(jobDetail.requestsRecovery());
        assertFalse(jobDetail.isDurable());
        assertEquals(Trigger.DEFAULT_PRIORITY, trigger.getPriority());
        assertEquals(Trigger.MISFIRE_INSTRUCTION_SMART_POLICY, trigger.getMisfireInstruction());

        jobName = OnTestJobWithDefaultConfiguration.class.getCanonicalName();
        jobDetail = jobManager.scheduler.getJobDetail(JobKey.jobKey(jobName));
        trigger = jobManager.scheduler.getTriggersOfJob(JobKey.jobKey(jobName)).get(0);

        assertFalse(jobDetail.requestsRecovery());
        assertFalse(jobDetail.isDurable());
        assertEquals(Trigger.DEFAULT_PRIORITY, trigger.getPriority());
        assertEquals(Trigger.MISFIRE_INSTRUCTION_SMART_POLICY, trigger.getMisfireInstruction());

        jobManager.stop();
    }

    @Test
    public void testJobsWithMultipleInstances() throws Exception {
        jobManager = JobManager.fromJobs(new TestConfig(), List.of(new OnTestJobWithVariableGroupName("group_one"),
                new OnTestJobWithVariableGroupName("group_two"), new EveryTestJobWithDefaultConfiguration()));

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
        jobManager = JobManager.fromJobs(new TestConfig(), List.of(new EveryTestJobWithDefaultConfiguration(),
                new EveryTestJobWithDefaultConfiguration()));

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
        jobManager = JobManager.fromJobs(new TestConfig(), List.of(new OnTestJobWithTimeZoneConfiguration(),
                new OnTestJobWithDefaultConfiguration()));
        jobManager.start();

        String jobName = OnTestJobWithTimeZoneConfiguration.class.getCanonicalName();
        CronTrigger trigger = (CronTrigger) jobManager.scheduler.getTriggersOfJob(JobKey.jobKey(jobName)).get(0);

        assertEquals("Europe/Stockholm", trigger.getTimeZone().getID());

        jobManager.stop();
    }

    @Test
    public void testInvalidTimezoneInCronExpressionThrowsException() {
        TestConfig config = new TestConfig();
        CronExpressionParser cronExpressionParser = new CronExpressionParser(config);

        String invalidTimezone = "Invalid/Timezone";
        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> cronExpressionParser.parse("0 15 10 ? * * [" + invalidTimezone + "]")
        );

        assertTrue(exception.getMessage().contains("Invalid timezone ID: '" + invalidTimezone + "'"));
    }

    @Test
    public void testInvalidTimezoneInConfigurationThrowsException() {
        TestConfig config = new TestConfig();
        config.getQuartzConfiguration().put("io.github.dropwizard-jobs.timezone", "Foo/Bar");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> new CronExpressionParser(config)
        );

        assertTrue(exception.getMessage().contains("Invalid timezone ID: 'Foo/Bar'"));
    }

    @Test
    public void testInvalidTimezoneInOnAnnotationThrowsException() {
        jobManager = JobManager.fromJobs(new TestConfig(), List.of(new OnTestJobWithInvalidTimeZone()));

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> jobManager.start()
        );

        assertTrue(exception.getMessage().contains("Invalid timezone ID: 'Invalid/Timezone'"));
    }

    @Test
    public void testJobsWithNonDefaultConfiguration() throws Exception {
        jobManager = JobManager.fromJobs(new TestConfig(), List.of(new EveryTestJobWithNonDefaultConfiguration(),
                new OnTestJobWithNonDefaultConfiguration()));

        jobManager.start();

        String jobName = EveryTestJobWithNonDefaultConfiguration.class.getCanonicalName();
        JobDetail jobDetail = jobManager.scheduler.getJobDetail(JobKey.jobKey(jobName));
        Trigger trigger = jobManager.scheduler.getTriggersOfJob(JobKey.jobKey(jobName)).get(0);

        assertTrue(jobDetail.requestsRecovery());
        assertTrue(jobDetail.isDurable());
        assertEquals(20, trigger.getPriority());
        assertEquals(Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY, trigger.getMisfireInstruction());

        jobName = OnTestJobWithNonDefaultConfiguration.class.getCanonicalName();
        jobDetail = jobManager.scheduler.getJobDetail(JobKey.jobKey(jobName));
        trigger = jobManager.scheduler.getTriggersOfJob(JobKey.jobKey(jobName)).get(0);

        assertTrue(jobDetail.requestsRecovery());
        assertTrue(jobDetail.isDurable());
        assertEquals(20, trigger.getPriority());
        assertEquals(Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY, trigger.getMisfireInstruction());

        jobManager.stop();
    }

    @Test
    public void shouldFallbackToNormalBehaviourWhenNoPropertiesAreDefined() throws Exception {
        TestConfig config = new TestConfig();
        config.getQuartzConfiguration().clear();

        jobManager = JobManager.fromJobs(config, List.of(startTestJob, onTestJob, onTestJobWithJobName, everyTestJob,
                everyTestJobWithJobName));
        jobManager.start();

        assertThat(jobManager.getScheduler().getMetaData().getThreadPoolSize(), Matchers.is(10));
        jobManager.stop();
    }

    @Test
    public void throwsExceptionWhenNoPropertiesAreDefined() {
        assertThrows(SchedulerConfigException.class, () -> {
            TestConfig config = new TestConfig();
            config.getQuartzConfiguration().clear();
            config.getQuartzConfiguration().put("some", "property");

            jobManager = JobManager.fromJobs(config, List.of(startTestJob, onTestJob, onTestJobWithJobName, everyTestJob,
                    everyTestJobWithJobName));
            jobManager.start();
        });
    }

    @Test
    public void shouldSetDropwizardPropertiesOnSchedulerFactory() throws Exception {
        TestConfig config = new TestConfig();
        // change configuration - use unique instance name to avoid conflicts with parallel tests
        config.getQuartzConfiguration().put("org.quartz.scheduler.instanceName", "testScheduler-" + UUID.randomUUID());
        config.getQuartzConfiguration().put("org.quartz.threadPool.threadCount", "15");
        jobManager = JobManager.fromJobs(config, List.of(startTestJob, onTestJob, onTestJobWithJobName, everyTestJob,
                everyTestJobWithJobName));
        jobManager.start();
        assertThat(jobManager.getScheduler().getMetaData().getThreadPoolSize(), Matchers.is(15));
        jobManager.stop();
    }

    @Test
    public void allowTimezoneConfiguration() {
        TestConfig config = new TestConfig();
        config.getQuartzConfiguration().put("io.github.dropwizard-jobs.timezone", "Europe/London");
        CronExpressionParser cronExpressionParser = new CronExpressionParser(config);

        jobManager = JobManager.fromJobs(config, List.of(startTestJob, onTestJob, onTestJobWithJobName, everyTestJob,
                everyTestJobWithJobName));

        // by default, crons should use the configuration setting's timezone
        CronTriggerImpl trigger1 = (CronTriggerImpl) (cronExpressionParser.parse("0 15 10 ? * *").build());
        assertEquals(TimeZone.getTimeZone("Europe/London"), trigger1.getTimeZone());
        assertEquals("0 15 10 ? * *", trigger1.getCronExpression());

        // can use [timezone] syntax to set a specific cron to a specific timezone
        CronTriggerImpl trigger2 = (CronTriggerImpl) (cronExpressionParser.parse("0 15 10 ? * * [America/Los_Angeles]").build());
        assertEquals(TimeZone.getTimeZone("America/Los_Angeles"), trigger2.getTimeZone());
        assertEquals("0 15 10 ? * *", trigger2.getCronExpression());
    }

    @Test
    public void shouldCatchAndLogSchedulerExceptionWithoutThrowing() throws Exception {
        // Create a mock scheduler that throws SchedulerException
        Scheduler mockScheduler = mock(Scheduler.class);
        SchedulerException testException = new SchedulerException("Test scheduler failure");
        when(mockScheduler.checkExists(any(JobKey.class))).thenThrow(testException);

        // Create a JobManager and inject the mock scheduler
        jobManager = JobManager.fromJobs(new TestConfig(), List.of(new EveryTestJobWithDefaultConfiguration()));
        jobManager.scheduler = mockScheduler;

        // Create a scheduled job to use for testing
        JobKey jobKey = JobKey.jobKey("test-job");
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("test-trigger")
                .startNow()
                .build();
        ScheduledJob scheduledJob = new ScheduledJob(jobKey, EveryTestJobWithDefaultConfiguration.class, trigger,
                false, false, "test message");

        // The method should not throw an exception - it should catch and log it
        // This verifies the error handling behavior in scheduleOrRescheduleJob()
        assertDoesNotThrow(() -> jobManager.scheduleOrRescheduleJob(scheduledJob));

        // Verify the scheduler was called (even though it threw an exception)
        verify(mockScheduler).checkExists(jobKey);
    }

    @Test
    public void testJobListenersAreRegisteredDuringStart() throws Exception {
        // Create a counting listener that tracks job executions
        CountingTestJobListener countingListener = new CountingTestJobListener("CountingTestJobListener");

        jobManager = JobManager.fromJobs(new TestConfig(),
                List.of(new EveryTestJobWithDefaultConfiguration()),
                List.of(countingListener));

        jobManager.start();

        // Verify the listener is registered with the scheduler
        assertThat(jobManager.getJobListeners().size(), is(1));
        assertThat(jobManager.getJobListeners().get(0), is(countingListener));

        // Wait for the job to execute at least once
        assertThat(countingListener.latch().await(2, TimeUnit.SECONDS), is(true));

        // Verify the listener received the job execution event
        assertThat(countingListener.executionCount(), Matchers.greaterThanOrEqualTo(1));

        jobManager.stop();
    }

    @Test
    public void testMultipleJobListenersReceiveEvents() throws Exception {
        CountingTestJobListener listener1 = new CountingTestJobListener("Listener1");
        CountingTestJobListener listener2 = new CountingTestJobListener("Listener2");

        jobManager = JobManager.fromJobs(new TestConfig(),
                List.of(new EveryTestJobWithDefaultConfiguration()),
                List.of(listener1, listener2));

        jobManager.start();

        // Wait for jobs to execute
        assertThat(listener1.latch().await(2, TimeUnit.SECONDS), is(true));
        assertThat(listener2.latch().await(2, TimeUnit.SECONDS), is(true));

        // Both listeners should have received at least one event
        assertThat(listener1.executionCount(), Matchers.greaterThanOrEqualTo(1));
        assertThat(listener2.executionCount(), Matchers.greaterThanOrEqualTo(1));

        jobManager.stop();
    }

    /**
     * A test JobListener that counts job executions using a CountDownLatch.
     */
    static class CountingTestJobListener implements JobListener {
        private final String name;
        private final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        private int executionCount = 0;

        public CountingTestJobListener(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public void jobToBeExecuted(JobExecutionContext context) {
            // No-op
        }

        @Override
        public void jobExecutionVetoed(JobExecutionContext context) {
            // No-op
        }

        @Override
        public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
            executionCount++;
            latch.countDown();
        }

        public java.util.concurrent.CountDownLatch latch() {
            return latch;
        }

        public int executionCount() {
            return executionCount;
        }
    }

    private static class TestConfig extends JobConfiguration {
        private final Map<String, String> quartzConfiguration;

        private TestConfig() {
            quartzConfiguration = new HashMap<>(DefaultQuartzConfiguration.get());
        }

        private final Map<String, String> jobs = new HashMap<>();

        @Override
        public Map<String, String> getJobs() {
            return jobs;
        }

        @Override
        public Map<String, String> getQuartzConfiguration() {
            return quartzConfiguration;
        }
    }

    @Every("10ms")
    static class EveryTestJobWithDefaultConfiguration extends AbstractJob {
        public EveryTestJobWithDefaultConfiguration() {
            super(1);
        }
    }

    @On("0/1 * * * * ?")
    static class OnTestJobWithDefaultConfiguration extends AbstractJob {
        public OnTestJobWithDefaultConfiguration() {
            super(1);
        }
    }

    @On("0/1 * * * * ?")
    static class OnTestJobWithVariableGroupName extends AbstractJob {
        public OnTestJobWithVariableGroupName(String groupName) {
            super(1, groupName);
        }
    }

    @On(value = "0 0 13 ? * MON", timeZone = "Europe/Stockholm")
    static class OnTestJobWithTimeZoneConfiguration extends AbstractJob {
        public OnTestJobWithTimeZoneConfiguration() {
            super(1);
        }
    }

    @Every(value = "10ms", requestRecovery = true, storeDurably = true, priority = 20,
            misfirePolicy = Every.MisfirePolicy.IGNORE_MISFIRES)
    static class EveryTestJobWithNonDefaultConfiguration extends AbstractJob {
        public EveryTestJobWithNonDefaultConfiguration() {
            super(1);
        }
    }

    @On(value = "0/1 * * * * ?", requestRecovery = true, storeDurably = true, priority = 20,
            misfirePolicy = On.MisfirePolicy.IGNORE_MISFIRES)
    static class OnTestJobWithNonDefaultConfiguration extends AbstractJob {
        public OnTestJobWithNonDefaultConfiguration() {
            super(1);
        }
    }

    @On(value = "0/1 * * * * ?", timeZone = "Invalid/Timezone")
    static class OnTestJobWithInvalidTimeZone extends AbstractJob {
        public OnTestJobWithInvalidTimeZone() {
            super(1);
        }
    }

}

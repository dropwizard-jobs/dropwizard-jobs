package io.dropwizard.jobs;

import org.junit.jupiter.api.Test;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.TriggerFiredBundle;

import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DropwizardJobFactoryTest {

    @Test
    public void testNewJobReturnsPreRegisteredInstanceWhenClassAndGroupMatch() throws SchedulerException {
        // Setup: create a pre-registered job
        EveryTestJob preRegisteredJob = new EveryTestJob();
        JobFilters jobFilters = new JobFilters(Collections.singletonList(preRegisteredJob));
        DropwizardJobFactory factory = new DropwizardJobFactory(jobFilters);

        // Mock the bundle with matching job class and default group
        TriggerFiredBundle bundle = mock(TriggerFiredBundle.class);
        JobDetail jobDetail = mock(JobDetail.class);
        JobKey jobKey = new JobKey("testJob", org.quartz.utils.Key.DEFAULT_GROUP);

        when(bundle.getJobDetail()).thenReturn(jobDetail);
        when(jobDetail.getJobClass()).thenReturn((Class) EveryTestJob.class);
        when(jobDetail.getKey()).thenReturn(jobKey);

        Scheduler scheduler = mock(Scheduler.class);

        // Execute
        Job result = factory.newJob(bundle, scheduler);

        // Verify: should return the pre-registered instance
        assertThat(result, is(notNullValue()));
        assertThat(result, is(sameInstance(preRegisteredJob)));
    }

    @Test
    public void testNewJobCreatesNewInstanceViaReflectionWhenJobClassNotInPreRegisteredList()
            throws SchedulerException {
        // Setup: empty job filters (no pre-registered jobs)
        JobFilters jobFilters = new JobFilters(Collections.emptyList());
        DropwizardJobFactory factory = new DropwizardJobFactory(jobFilters);

        // Mock the bundle with a job class not in the pre-registered list
        TriggerFiredBundle bundle = mock(TriggerFiredBundle.class);
        JobDetail jobDetail = mock(JobDetail.class);
        JobKey jobKey = new JobKey("testJob", org.quartz.utils.Key.DEFAULT_GROUP);

        when(bundle.getJobDetail()).thenReturn(jobDetail);
        when(jobDetail.getJobClass()).thenReturn((Class) EveryTestJob.class);
        when(jobDetail.getKey()).thenReturn(jobKey);

        Scheduler scheduler = mock(Scheduler.class);

        // Execute
        Job result = factory.newJob(bundle, scheduler);

        // Verify: should create a new instance via reflection
        assertThat(result, is(notNullValue()));
        assertThat(result, is(instanceOf(EveryTestJob.class)));
    }

    @Test
    public void testNewJobCreatesNewInstanceViaReflectionWhenJobClassExistsButWithDifferentGroupName()
            throws SchedulerException {
        // Setup: create a pre-registered job with default group
        EveryTestJob preRegisteredJob = new EveryTestJob();
        JobFilters jobFilters = new JobFilters(Collections.singletonList(preRegisteredJob));
        DropwizardJobFactory factory = new DropwizardJobFactory(jobFilters);

        // Mock the bundle with same job class but different group name
        TriggerFiredBundle bundle = mock(TriggerFiredBundle.class);
        JobDetail jobDetail = mock(JobDetail.class);
        JobKey jobKey = new JobKey("testJob", "differentGroupName");

        when(bundle.getJobDetail()).thenReturn(jobDetail);
        when(jobDetail.getJobClass()).thenReturn((Class) EveryTestJob.class);
        when(jobDetail.getKey()).thenReturn(jobKey);

        Scheduler scheduler = mock(Scheduler.class);

        // Execute
        Job result = factory.newJob(bundle, scheduler);

        // Verify: should create a new instance via reflection (not the pre-registered one)
        assertThat(result, is(notNullValue()));
        assertThat(result, is(instanceOf(EveryTestJob.class)));
        assertThat(result, is(not(sameInstance(preRegisteredJob))));
    }

    @Test
    public void testNewJobThrowsSchedulerExceptionWhenJobClassCannotBeInstantiated() {
        // Setup: empty job filters
        JobFilters jobFilters = new JobFilters(Collections.emptyList());
        DropwizardJobFactory factory = new DropwizardJobFactory(jobFilters);

        // Mock the bundle with a job class that has no no-arg constructor
        TriggerFiredBundle bundle = mock(TriggerFiredBundle.class);
        JobDetail jobDetail = mock(JobDetail.class);
        JobKey jobKey = new JobKey("testJob", org.quartz.utils.Key.DEFAULT_GROUP);

        when(bundle.getJobDetail()).thenReturn(jobDetail);
        when(jobDetail.getJobClass()).thenReturn((Class) JobWithoutNoArgConstructor.class);
        when(jobDetail.getKey()).thenReturn(jobKey);

        Scheduler scheduler = mock(Scheduler.class);

        // Execute & Verify: should throw SchedulerException
        SchedulerException exception = assertThrows(SchedulerException.class, () -> {
            factory.newJob(bundle, scheduler);
        });

        assertThat(exception.getMessage(), containsString("no no-arg constructor found"));
        assertThat(exception.getMessage(), containsString(JobWithoutNoArgConstructor.class.getName()));
    }

    /**
     * Test job class without a no-arg constructor for testing the error case.
     */
    private static class JobWithoutNoArgConstructor extends Job {
        @SuppressWarnings("unused")
        public JobWithoutNoArgConstructor(String requiredArg) {
            // This constructor requires an argument, so no no-arg constructor exists
        }

        @Override
        public void doJob(org.quartz.JobExecutionContext context) {
            // No-op
        }
    }
}

package io.dropwizard.jobs;

import org.junit.jupiter.api.Test;
import org.quartz.JobDetail;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.TriggerFiredBundle;

import java.util.Collections;
import java.util.Map;
import java.lang.reflect.InvocationTargetException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DropwizardJobFactoryTest {

    @Test
    public void testNewJobReturnsPreRegisteredInstanceWhenClassAndGroupMatch() throws SchedulerException {
        // Setup: create a pre-registered job metadata
        EveryTestJob preRegisteredJob = new EveryTestJob();
        JobMetadata metadata = new JobMetadata(preRegisteredJob);
        JobFilters jobFilters = new JobFilters(Collections.singletonList(metadata));
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

        assertSame(preRegisteredJob, result);
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
        // Setup: create a pre-registered job metadata with default group
        EveryTestJob preRegisteredJob = new EveryTestJob();
        JobMetadata metadata = new JobMetadata(preRegisteredJob.getClass(), preRegisteredJob.getGroupName());
        JobFilters jobFilters = new JobFilters(Collections.singletonList(metadata));
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

        // Verify: should create a new instance via reflection
        assertThat(result, is(notNullValue()));
        assertThat(result, is(instanceOf(EveryTestJob.class)));
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

    @Test
    public void constructsRegisteredMetadataWithoutAnInstance() throws Exception {
        JobFilters filters = new JobFilters(java.util.List.of(new JobMetadata(ReflectiveTestJob.class, null)));
        assertInstanceOf(ReflectiveTestJob.class, new DropwizardJobFactory(filters)
                .newJob(bundleFor(ReflectiveTestJob.class), null));
    }

    @Test
    public void resolvesProxySuperclassForRuntimeScheduledJobs() throws Exception {
        DropwizardJobFactory factory = new DropwizardJobFactory(new JobFilters(Collections.emptyList()));
        assertEquals(ReflectiveTestJob.class, factory.newJob(bundleFor(ReflectiveTestJob$$Proxy.class), null).getClass());
    }

    @Test
    public void reflectionFailuresRetainTheirCauses() {
        Map<Class<? extends Job>, Class<? extends Throwable>> cases = Map.of(
                AbstractTestJob.class, InstantiationException.class,
                PrivateConstructorTestJob.class, IllegalAccessException.class,
                ThrowingConstructorTestJob.class, InvocationTargetException.class);
        DropwizardJobFactory factory = new DropwizardJobFactory(new JobFilters(Collections.emptyList()));
        cases.forEach((jobClass, causeClass) -> {
            SchedulerException failure = assertThrows(SchedulerException.class, () -> factory.newJob(bundleFor(jobClass), null));
            assertInstanceOf(causeClass, failure.getCause());
            assertTrue(failure.getMessage().contains(jobClass.getName()));
        });
    }

    private TriggerFiredBundle bundleFor(Class<? extends Job> jobClass) {
        TriggerFiredBundle bundle = mock(TriggerFiredBundle.class);
        when(bundle.getJobDetail()).thenReturn(JobBuilder.newJob(jobClass).withIdentity("factory-test").build());
        return bundle;
    }

    public static class ReflectiveTestJob extends Job {
        @Override
        public void doJob(JobExecutionContext context) {
        }
    }

    public static class ReflectiveTestJob$$Proxy extends ReflectiveTestJob {
    }

    public abstract static class AbstractTestJob extends ReflectiveTestJob {
    }

    public static class PrivateConstructorTestJob extends ReflectiveTestJob {
        private PrivateConstructorTestJob() {
        }
    }

    public static class ThrowingConstructorTestJob extends ReflectiveTestJob {
        public ThrowingConstructorTestJob() {
            throw new IllegalStateException("constructor failure");
        }
    }

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

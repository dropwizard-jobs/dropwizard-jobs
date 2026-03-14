package io.dropwizard.jobs.scheduler;

import io.dropwizard.jobs.AllJobsTestJobListener;
import io.dropwizard.jobs.NameMatchTestJobListener;
import io.dropwizard.jobs.UnannotatedTestJobListener;
import io.dropwizard.jobs.annotations.ListeningFor;
import io.dropwizard.jobs.annotations.ListeningFor.MatcherType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.Matcher;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.EverythingMatcher;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.matchers.KeyMatcher;
import org.quartz.impl.matchers.NameMatcher;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class JobListenerRegistrarTest {

    // ========================================================================
    // Tests for resolveMatcherFrom() - testing matcher resolution from annotations
    // ========================================================================

    @Test
    public void testUnannotatedListenerDefaultsToAllJobs() {
        Matcher<JobKey> matcher = JobListenerRegistrar.resolveMatcherFrom(UnannotatedTestJobListener.class);

        assertThat(matcher, instanceOf(EverythingMatcher.class));
    }

    @Test
    public void testListeningForAllJobsProducesEverythingMatcher() {
        Matcher<JobKey> matcher = JobListenerRegistrar.resolveMatcherFrom(AllJobsTestJobListener.class);

        assertThat(matcher, instanceOf(EverythingMatcher.class));
    }

    @Test
    public void testJobNameEqualsProducesKeyMatcher() {
        Matcher<JobKey> matcher = JobListenerRegistrar.resolveMatcherFrom(NameMatchTestJobListener.class);

        assertThat(matcher, instanceOf(KeyMatcher.class));
    }

    @Test
    public void testJobGroupEqualsProducesGroupMatcher() {
        @ListeningFor(matcher = MatcherType.JOB_GROUP_EQUALS, value = "myGroup")
        class GroupMatchListener implements JobListener {
            @Override
            public String getName() {
                return "GroupMatchListener";
            }

            @Override
            public void jobToBeExecuted(org.quartz.JobExecutionContext context) {
            }

            @Override
            public void jobExecutionVetoed(org.quartz.JobExecutionContext context) {
            }

            @Override
            public void jobWasExecuted(org.quartz.JobExecutionContext context,
                    org.quartz.JobExecutionException jobException) {
            }
        }

        Matcher<JobKey> matcher = JobListenerRegistrar.resolveMatcherFrom(GroupMatchListener.class);

        assertThat(matcher, instanceOf(GroupMatcher.class));
    }

    @Test
    public void testJobGroupStartsWithProducesGroupMatcher() {
        @ListeningFor(matcher = MatcherType.JOB_GROUP_STARTS_WITH, value = "prefix")
        class GroupPrefixListener implements JobListener {
            @Override
            public String getName() {
                return "GroupPrefixListener";
            }

            @Override
            public void jobToBeExecuted(org.quartz.JobExecutionContext context) {
            }

            @Override
            public void jobExecutionVetoed(org.quartz.JobExecutionContext context) {
            }

            @Override
            public void jobWasExecuted(org.quartz.JobExecutionContext context,
                    org.quartz.JobExecutionException jobException) {
            }
        }

        Matcher<JobKey> matcher = JobListenerRegistrar.resolveMatcherFrom(GroupPrefixListener.class);

        assertThat(matcher, instanceOf(GroupMatcher.class));
    }

    // ========================================================================
    // Tests for toMatcher() - testing all MatcherType conversions
    // ========================================================================

    @Test
    public void testToMatcherAllJobs() {
        Matcher<JobKey> matcher = JobListenerRegistrar.toMatcher(MatcherType.ALL_JOBS, "");
        assertThat(matcher, instanceOf(EverythingMatcher.class));
    }

    @Test
    public void testToMatcherJobNameEquals() {
        Matcher<JobKey> matcher = JobListenerRegistrar.toMatcher(MatcherType.JOB_NAME_EQUALS, "myJob");
        assertThat(matcher, instanceOf(KeyMatcher.class));
    }

    @Test
    public void testToMatcherJobGroupEquals() {
        Matcher<JobKey> matcher = JobListenerRegistrar.toMatcher(MatcherType.JOB_GROUP_EQUALS, "myGroup");
        assertThat(matcher, instanceOf(GroupMatcher.class));
    }

    @Test
    public void testToMatcherJobNameStartsWith() {
        Matcher<JobKey> matcher = JobListenerRegistrar.toMatcher(MatcherType.JOB_NAME_STARTS_WITH, "prefix");
        assertThat(matcher, instanceOf(NameMatcher.class));
    }

    @Test
    public void testToMatcherJobNameEndsWith() {
        Matcher<JobKey> matcher = JobListenerRegistrar.toMatcher(MatcherType.JOB_NAME_ENDS_WITH, "suffix");
        assertThat(matcher, instanceOf(NameMatcher.class));
    }

    @Test
    public void testToMatcherJobNameContains() {
        Matcher<JobKey> matcher = JobListenerRegistrar.toMatcher(MatcherType.JOB_NAME_CONTAINS, "middle");
        assertThat(matcher, instanceOf(NameMatcher.class));
    }

    @Test
    public void testToMatcherJobGroupStartsWith() {
        Matcher<JobKey> matcher = JobListenerRegistrar.toMatcher(MatcherType.JOB_GROUP_STARTS_WITH, "groupPrefix");
        assertThat(matcher, instanceOf(GroupMatcher.class));
    }

    @Test
    public void testToMatcherJobGroupEndsWith() {
        Matcher<JobKey> matcher = JobListenerRegistrar.toMatcher(MatcherType.JOB_GROUP_ENDS_WITH, "groupSuffix");
        assertThat(matcher, instanceOf(GroupMatcher.class));
    }

    @Test
    public void testToMatcherJobGroupContains() {
        Matcher<JobKey> matcher = JobListenerRegistrar.toMatcher(MatcherType.JOB_GROUP_CONTAINS, "groupMiddle");
        assertThat(matcher, instanceOf(GroupMatcher.class));
    }

    // ========================================================================
    // Tests for register() method - integration with Scheduler
    // ========================================================================

    @Test
    public void testRegisterCallsSchedulerListenerManager() throws SchedulerException {
        Scheduler mockScheduler = mock(Scheduler.class);
        org.quartz.ListenerManager mockListenerManager = mock(org.quartz.ListenerManager.class);
        when(mockScheduler.getListenerManager()).thenReturn(mockListenerManager);

        JobListener testListener = new UnannotatedTestJobListener();
        JobListenerRegistrar registrar = new JobListenerRegistrar(mockScheduler, List.of(testListener));

        registrar.register();

        verify(mockListenerManager).addJobListener(eq(testListener), any(Matcher.class));
    }

    @Test
    public void testRegisterWithMultipleListeners() throws SchedulerException {
        Scheduler mockScheduler = mock(Scheduler.class);
        org.quartz.ListenerManager mockListenerManager = mock(org.quartz.ListenerManager.class);
        when(mockScheduler.getListenerManager()).thenReturn(mockListenerManager);

        JobListener listener1 = new UnannotatedTestJobListener();
        JobListener listener2 = new AllJobsTestJobListener();
        JobListener listener3 = new NameMatchTestJobListener();

        JobListenerRegistrar registrar = new JobListenerRegistrar(mockScheduler,
                List.of(listener1, listener2, listener3));

        registrar.register();

        verify(mockListenerManager, times(3)).addJobListener(any(JobListener.class), any(Matcher.class));
    }

    @Test
    public void testRegisterWithCorrectMatcherForAnnotatedListener() throws SchedulerException {
        Scheduler mockScheduler = mock(Scheduler.class);
        org.quartz.ListenerManager mockListenerManager = mock(org.quartz.ListenerManager.class);
        when(mockScheduler.getListenerManager()).thenReturn(mockListenerManager);

        JobListener testListener = new NameMatchTestJobListener();
        JobListenerRegistrar registrar = new JobListenerRegistrar(mockScheduler, List.of(testListener));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Matcher<JobKey>> matcherCaptor = ArgumentCaptor.forClass(Matcher.class);

        registrar.register();

        verify(mockListenerManager).addJobListener(eq(testListener), matcherCaptor.capture());

        Matcher<JobKey> capturedMatcher = matcherCaptor.getValue();
        assertThat(capturedMatcher, instanceOf(KeyMatcher.class));
    }

    @Test
    public void testRegisterWithEmptyListenerList() throws SchedulerException {
        Scheduler mockScheduler = mock(Scheduler.class);
        org.quartz.ListenerManager mockListenerManager = mock(org.quartz.ListenerManager.class);
        when(mockScheduler.getListenerManager()).thenReturn(mockListenerManager);

        JobListenerRegistrar registrar = new JobListenerRegistrar(mockScheduler, List.of());

        registrar.register();

        verify(mockListenerManager, never()).addJobListener(any(JobListener.class), any(Matcher.class));
    }
}

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
import org.quartz.impl.matchers.NameMatcher;
import org.quartz.listeners.JobListenerSupport;

import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

public class JobListenerRegistrarTest {

    private static final Map<MatcherType, String> NAME_MATCHES = Map.of(
            MatcherType.JOB_NAME_EQUALS, "report",
            MatcherType.JOB_NAME_STARTS_WITH, "reportJob",
            MatcherType.JOB_NAME_ENDS_WITH, "dailyreport",
            MatcherType.JOB_NAME_CONTAINS, "dailyreportJob");

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
    public void testJobNameEqualsMatchesAcrossGroups() {
        Matcher<JobKey> matcher = JobListenerRegistrar.resolveMatcherFrom(NameMatchTestJobListener.class);

        assertTrue(matcher.isMatch(JobKey.jobKey("testJob")));
        assertTrue(matcher.isMatch(JobKey.jobKey("testJob", "reporting")));
        assertFalse(matcher.isMatch(JobKey.jobKey("otherJob", "reporting")));
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
        assertTrue(matcher.isMatch(JobKey.jobKey("myJob")));
        assertTrue(matcher.isMatch(JobKey.jobKey("myJob", "batch")));
        assertFalse(matcher.isMatch(JobKey.jobKey("myJobExtra", "batch")));
        assertFalse(matcher.isMatch(JobKey.jobKey("prefixmyJob", "batch")));
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

    @Test
    public void testNameMatchersWithoutGroupMatchAcrossGroups() {
        for (Map.Entry<MatcherType, String> entry : NAME_MATCHES.entrySet()) {
            Matcher<JobKey> matcher = JobListenerRegistrar.toMatcher(entry.getKey(), "report", "");
            for (String group : List.of(Scheduler.DEFAULT_GROUP, "reporting", "batch")) {
                assertTrue(matcher.isMatch(JobKey.jobKey(entry.getValue(), group)), entry.getKey().name());
                assertFalse(matcher.isMatch(JobKey.jobKey("otherJob", group)), entry.getKey().name());
            }
        }
    }

    @Test
    public void testNameMatchersCanBeRestrictedToGroup() {
        for (Map.Entry<MatcherType, String> entry : NAME_MATCHES.entrySet()) {
            Matcher<JobKey> matcher = JobListenerRegistrar.toMatcher(entry.getKey(), "report", "reporting");
            assertTrue(matcher.isMatch(JobKey.jobKey(entry.getValue(), "reporting")), entry.getKey().name());
            assertFalse(matcher.isMatch(JobKey.jobKey(entry.getValue())), entry.getKey().name());
            assertFalse(matcher.isMatch(JobKey.jobKey(entry.getValue(), "batch")), entry.getKey().name());
            assertFalse(matcher.isMatch(JobKey.jobKey("otherJob", "reporting")), entry.getKey().name());
        }
    }

    @Test
    public void testExplicitDefaultGroupPreservesExactNameScope() {
        Matcher<JobKey> matcher = JobListenerRegistrar.toMatcher(MatcherType.JOB_NAME_EQUALS, "report", Scheduler.DEFAULT_GROUP);

        assertTrue(matcher.isMatch(JobKey.jobKey("report")));
        assertFalse(matcher.isMatch(JobKey.jobKey("report", "reporting")));
        assertFalse(matcher.isMatch(JobKey.jobKey("reportJob")));
        assertFalse(matcher.isMatch(JobKey.jobKey("dailyreport")));
    }

    @Test
    public void testGroupNamesAreMatchedLiterally() {
        Matcher<JobKey> matcher = JobListenerRegistrar.toMatcher(MatcherType.JOB_NAME_EQUALS, "report", " reporting ");

        assertTrue(matcher.isMatch(JobKey.jobKey("report", " reporting ")));
        assertFalse(matcher.isMatch(JobKey.jobKey("report", "reporting")));
    }

    @Test
    public void testGroupRestrictionRejectsNonNameMatchers() {
        for (MatcherType type : MatcherType.values()) {
            if (!NAME_MATCHES.containsKey(type)) {
                IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                        () -> JobListenerRegistrar.toMatcher(type, "report", "reporting"));
                assertEquals("@ListeningFor group is only supported for JOB_NAME_* matchers, not " + type,
                        exception.getMessage());
            }
        }
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
        assertTrue(capturedMatcher.isMatch(JobKey.jobKey("testJob")));
        assertTrue(capturedMatcher.isMatch(JobKey.jobKey("testJob", "reporting")));
        assertFalse(capturedMatcher.isMatch(JobKey.jobKey("otherJob", "reporting")));
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

    @Test
    public void testRegisterWithGroupRestrictedListener() throws SchedulerException {
        Scheduler mockScheduler = mock(Scheduler.class);
        org.quartz.ListenerManager mockListenerManager = mock(org.quartz.ListenerManager.class);
        when(mockScheduler.getListenerManager()).thenReturn(mockListenerManager);

        JobListener listener = new ScopedNameTestJobListener();
        JobListenerRegistrar registrar = new JobListenerRegistrar(mockScheduler, List.of(listener));
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Matcher<JobKey>> matcherCaptor = ArgumentCaptor.forClass(Matcher.class);

        registrar.register();

        verify(mockListenerManager).addJobListener(eq(listener), matcherCaptor.capture());
        Matcher<JobKey> matcher = matcherCaptor.getValue();
        assertTrue(matcher.isMatch(JobKey.jobKey("testJob", "reporting")));
        assertFalse(matcher.isMatch(JobKey.jobKey("testJob")));
        assertFalse(matcher.isMatch(JobKey.jobKey("testJob", "batch")));
        assertFalse(matcher.isMatch(JobKey.jobKey("otherJob", "reporting")));
    }

    @Test
    public void testRegisterRejectsInvalidGroupBeforeAccessingScheduler() {
        Scheduler mockScheduler = mock(Scheduler.class);
        JobListenerRegistrar registrar = new JobListenerRegistrar(mockScheduler, List.of(new InvalidGroupTestJobListener()));

        assertThrows(IllegalArgumentException.class, registrar::register);
        verifyNoInteractions(mockScheduler);
    }

    @ListeningFor(matcher = MatcherType.JOB_NAME_EQUALS, value = "testJob", group = "reporting")
    static class ScopedNameTestJobListener extends JobListenerSupport {
        @Override
        public String getName() {
            return getClass().getSimpleName();
        }
    }

    @ListeningFor(group = "reporting")
    static class InvalidGroupTestJobListener extends JobListenerSupport {
        @Override
        public String getName() {
            return getClass().getSimpleName();
        }
    }
}

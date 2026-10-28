package io.dropwizard.jobs.scheduler;

import com.codahale.metrics.MetricRegistry;
import io.dropwizard.jobs.*;
import io.dropwizard.jobs.annotations.DelayStart;
import io.dropwizard.jobs.annotations.Every;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.mockito.ArgumentCaptor;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@Timeout(10)
class EverySchedulerTest {

    @Test
    void mapsEveryMisfirePolicyToItsQuartzInstruction() {
        Map<Class<? extends Job>, Integer> cases = Map.of(
                SmartTestJob.class, Trigger.MISFIRE_INSTRUCTION_SMART_POLICY,
                IgnoreTestJob.class, Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY,
                FireNowTestJob.class, SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW,
                NowExistingTestJob.class, SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_EXISTING_REPEAT_COUNT,
                NowRemainingTestJob.class, SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NOW_WITH_REMAINING_REPEAT_COUNT,
                NextExistingTestJob.class, SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_EXISTING_COUNT,
                NextRemainingTestJob.class, SimpleTrigger.MISFIRE_INSTRUCTION_RESCHEDULE_NEXT_WITH_REMAINING_COUNT);
        assertEquals(Every.MisfirePolicy.values().length, cases.size());
        cases.forEach((jobClass, instruction) -> assertEquals(instruction,
                capture(jobClass).getTrigger().getMisfireInstruction(), jobClass.getSimpleName()));
    }

    @Test
    void preservesFiniteAndInfiniteRepeatCounts() {
        Map<Class<? extends Job>, Integer> cases = Map.of(
                SmartTestJob.class, SimpleTrigger.REPEAT_INDEFINITELY,
                CountZeroTestJob.class, 0,
                CountTwoTestJob.class, 2);
        cases.forEach((jobClass, count) -> {
            SimpleTrigger trigger = assertInstanceOf(SimpleTrigger.class, capture(jobClass).getTrigger());
            assertEquals(count, trigger.getRepeatCount());
            assertEquals(1_000L, trigger.getRepeatInterval());
        });
    }

    @Test
    void preservesJobIdentityAndSchedulingMetadata() {
        ScheduledJob scheduled = capture(CountTwoTestJob.class);
        assertEquals(JobKey.jobKey("bounded", "strategy-tests"), scheduled.getJobKey());
        assertEquals(CountTwoTestJob.class, scheduled.getClazz());
        assertTrue(scheduled.isRequestsRecovery());
        assertTrue(scheduled.isStoreDurably());
        assertEquals(9, scheduled.getTrigger().getPriority());
    }

    @Test
    void delayChangesStartTimeWithoutDependingOnExecutionSpeed() {
        long before = System.currentTimeMillis();
        Trigger delayed = capture(DelayedTestJob.class).getTrigger();
        long after = System.currentTimeMillis();
        assertTrue(delayed.getStartTime().getTime() >= before + 2_000);
        assertTrue(delayed.getStartTime().getTime() <= after + 2_000);

        before = System.currentTimeMillis();
        Trigger immediate = capture(SmartTestJob.class).getTrigger();
        after = System.currentTimeMillis();
        assertTrue(immediate.getStartTime().getTime() >= before);
        assertTrue(immediate.getStartTime().getTime() <= after);
    }

    @Test
    void repeatCountTwoExecutesExactlyThreeTimes() throws Exception {
        FiniteTestJob job = new FiniteTestJob();
        MutableJobConfiguration config = new MutableJobConfiguration().withQuartzProperties(DefaultQuartzConfiguration.get())
                .withQuartzProperty("org.quartz.scheduler.instanceName", "finite-" + UUID.randomUUID());
        JobManager manager = JobManager.fromJobs(config, List.of(job));
        try {
            manager.start();
            assertTrue(job.finished.await(2, TimeUnit.SECONDS));
            await().atMost(2, TimeUnit.SECONDS).until(() -> !manager.getScheduler()
                    .checkExists(JobKey.jobKey(FiniteTestJob.class.getCanonicalName())));
            assertEquals(3, job.executions.get());
        } finally {
            if (manager.getScheduler() != null) {
                manager.getScheduler().shutdown(true);
            }
        }
    }

    private ScheduledJob capture(Class<? extends Job> jobClass) {
        JobMediator mediator = mock(JobMediator.class);
        when(mediator.getConfiguration()).thenReturn(new MutableJobConfiguration());
        when(mediator.getJobs()).thenReturn(new JobFilters(List.of(new JobMetadata(jobClass, "strategy-tests"))));
        new EveryScheduler(mediator).schedule();
        ArgumentCaptor<ScheduledJob> scheduled = ArgumentCaptor.forClass(ScheduledJob.class);
        verify(mediator).scheduleOrRescheduleJob(scheduled.capture());
        return scheduled.getValue();
    }

    public static class NoOpTestJob extends Job {
        public NoOpTestJob() {
            super(new MetricRegistry());
        }

        @Override
        public void doJob(JobExecutionContext context) {
        }
    }

    @Every("1s")
    public static class SmartTestJob extends NoOpTestJob {
    }

    @Every(value = "1s", misfirePolicy = Every.MisfirePolicy.IGNORE_MISFIRES)
    public static class IgnoreTestJob extends NoOpTestJob {
    }

    @Every(value = "1s", misfirePolicy = Every.MisfirePolicy.FIRE_NOW)
    public static class FireNowTestJob extends NoOpTestJob {
    }

    @Every(value = "1s", misfirePolicy = Every.MisfirePolicy.NOW_WITH_EXISTING_COUNT)
    public static class NowExistingTestJob extends NoOpTestJob {
    }

    @Every(value = "1s", misfirePolicy = Every.MisfirePolicy.NOW_WITH_REMAINING_COUNT)
    public static class NowRemainingTestJob extends NoOpTestJob {
    }

    @Every(value = "1s", misfirePolicy = Every.MisfirePolicy.NEXT_WITH_EXISTING_COUNT)
    public static class NextExistingTestJob extends NoOpTestJob {
    }

    @Every(value = "1s", misfirePolicy = Every.MisfirePolicy.NEXT_WITH_REMAINING_COUNT)
    public static class NextRemainingTestJob extends NoOpTestJob {
    }

    @Every(value = "1s", repeatCount = 0)
    public static class CountZeroTestJob extends NoOpTestJob {
    }

    @Every(value = "1s", repeatCount = 2, priority = 9, jobName = "bounded", requestRecovery = true, storeDurably = true)
    public static class CountTwoTestJob extends NoOpTestJob {
    }

    @Every("1s")
    @DelayStart("2s")
    public static class DelayedTestJob extends NoOpTestJob {
    }

    @Every(value = "10ms", repeatCount = 2)
    public static class FiniteTestJob extends NoOpTestJob {
        final AtomicInteger executions = new AtomicInteger();
        final CountDownLatch finished = new CountDownLatch(3);

        @Override
        public void doJob(JobExecutionContext context) {
            executions.incrementAndGet();
            finished.countDown();
        }
    }
}
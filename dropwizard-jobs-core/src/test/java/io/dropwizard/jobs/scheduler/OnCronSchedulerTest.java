package io.dropwizard.jobs.scheduler;

import io.dropwizard.jobs.*;
import io.dropwizard.jobs.annotations.DelayStart;
import io.dropwizard.jobs.annotations.On;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.quartz.CronTrigger;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;

import java.util.List;
import java.util.Map;
import java.util.Date;
import java.util.TimeZone;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OnCronSchedulerTest {

    @Test
    void mapsEveryCronMisfirePolicyToItsQuartzInstruction() {
        Map<Class<? extends Job>, Integer> cases = Map.of(
                SmartTestJob.class, Trigger.MISFIRE_INSTRUCTION_SMART_POLICY,
                IgnoreTestJob.class, Trigger.MISFIRE_INSTRUCTION_IGNORE_MISFIRE_POLICY,
                SkipTestJob.class, CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING,
                FireTestJob.class, CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
        assertEquals(On.MisfirePolicy.values().length, cases.size());
        cases.forEach((jobClass, instruction) -> assertEquals(instruction,
                capture(jobClass).getTrigger().getMisfireInstruction(), jobClass.getSimpleName()));
    }

    @Test
    void preservesIdentityRecoveryDurabilityAndPriority() {
        ScheduledJob scheduled = capture(FireTestJob.class);
        assertEquals(JobKey.jobKey("cron-name", "strategy-tests"), scheduled.getJobKey());
        assertTrue(scheduled.isRequestsRecovery());
        assertTrue(scheduled.isStoreDurably());
        assertEquals(12, scheduled.getTrigger().getPriority());
        assertEquals("0 15 10 ? * *", assertInstanceOf(CronTrigger.class, scheduled.getTrigger()).getCronExpression());
    }

    @Test
    void annotationTimezoneOverridesInlineAndConfigurationTimezones() {
        CronTrigger trigger = assertInstanceOf(CronTrigger.class, capture(TimezoneTestJob.class).getTrigger());
        assertEquals("Europe/Stockholm", trigger.getTimeZone().getID());
    }

    @Test
    void inlineTimezoneOverridesConfigurationTimezone() {
        CronTrigger trigger = assertInstanceOf(CronTrigger.class, capture(InlineTimezoneTestJob.class).getTrigger());
        assertEquals("America/Los_Angeles", trigger.getTimeZone().getID());
    }

    @Test
    void delayStartDoesNotDelayCronJobs() {
        long before = System.currentTimeMillis();
        Trigger trigger = capture(DelayedTestJob.class).getTrigger();
        long after = System.currentTimeMillis();
        assertTrue(trigger.getStartTime().getTime() >= Instant.ofEpochMilli(before).truncatedTo(ChronoUnit.SECONDS).toEpochMilli());
        assertTrue(trigger.getStartTime().getTime() <= after);
    }

    @Test
    void rejectsMalformedTimezoneSuffixesAndMissingCronExpressions() {
        CronExpressionParser parser = new CronExpressionParser(new MutableJobConfiguration());
        for (String expression : List.of("0 15 10 ? * * ]UTC[", "0 15 10 ? * * [UTC] trailing",
                "0 15 10 ? * * [UTC][UTC]", "0 15 10 ? * * [UTC", "0 15 10 ? * * UTC]",
                "0 15 10 ? * * []", "", " ")) {
            assertThrows(IllegalArgumentException.class, () -> parser.parse(expression), expression);
        }
        assertThrows(IllegalArgumentException.class, () -> parser.parse(null));
        RuntimeException invalidCron = assertThrows(RuntimeException.class, () -> parser.parse("not a cron"));
        assertInstanceOf(java.text.ParseException.class, invalidCron.getCause());
    }

    @Test
    void missingConfigurationUsesJvmTimezone() {
        CronTrigger trigger = (CronTrigger) new CronExpressionParser(null).parse("0 15 10 ? * *").build();
        assertEquals(TimeZone.getDefault(), trigger.getTimeZone());
    }

    @Test
    void daylightSavingChangesUtcFireTimeWithoutChangingLocalSchedule() {
        CronExpressionParser parser = new CronExpressionParser(new MutableJobConfiguration());
        CronTrigger trigger = (CronTrigger) TriggerBuilder.newTrigger()
                .startAt(Date.from(Instant.parse("2026-01-01T00:00:00Z")))
                .withSchedule(parser.parse("0 0 12 ? * * [Europe/Stockholm]")).build();
        assertEquals(Instant.parse("2026-03-28T11:00:00Z"),
                trigger.getFireTimeAfter(Date.from(Instant.parse("2026-03-28T00:00:00Z"))).toInstant());
        assertEquals(Instant.parse("2026-03-29T10:00:00Z"),
                trigger.getFireTimeAfter(Date.from(Instant.parse("2026-03-29T00:00:00Z"))).toInstant());
    }

    private ScheduledJob capture(Class<? extends Job> jobClass) {
        JobMediator mediator = mock(JobMediator.class);
        MutableJobConfiguration config = new MutableJobConfiguration()
                .withQuartzProperty("io.github.dropwizard-jobs.timezone", "Europe/London");
        when(mediator.getConfiguration()).thenReturn(config);
        when(mediator.getJobs()).thenReturn(new JobFilters(List.of(new JobMetadata(jobClass, "strategy-tests"))));
        new OnCronScheduler(mediator).schedule();
        ArgumentCaptor<ScheduledJob> scheduled = ArgumentCaptor.forClass(ScheduledJob.class);
        verify(mediator).scheduleOrRescheduleJob(scheduled.capture());
        return scheduled.getValue();
    }

    public static class NoOpTestJob extends Job {
        @Override
        public void doJob(JobExecutionContext context) {
        }
    }

    @On("0 15 10 ? * *")
    public static class SmartTestJob extends NoOpTestJob {
    }

    @On(value = "0 15 10 ? * *", misfirePolicy = On.MisfirePolicy.IGNORE_MISFIRES)
    public static class IgnoreTestJob extends NoOpTestJob {
    }

    @On(value = "0 15 10 ? * *", misfirePolicy = On.MisfirePolicy.DO_NOTHING)
    public static class SkipTestJob extends NoOpTestJob {
    }

    @On(value = "0 15 10 ? * *", misfirePolicy = On.MisfirePolicy.FIRE_AND_PROCEED,
            jobName = "cron-name", requestRecovery = true, storeDurably = true, priority = 12)
    public static class FireTestJob extends NoOpTestJob {
    }

    @On(value = "0 15 10 ? * * [America/Los_Angeles]", timeZone = "Europe/Stockholm")
    public static class TimezoneTestJob extends NoOpTestJob {
    }

    @On("0 15 10 ? * * [America/Los_Angeles]")
    public static class InlineTimezoneTestJob extends NoOpTestJob {
    }

    @On("0 15 10 ? * *")
    @DelayStart("1h")
    public static class DelayedTestJob extends NoOpTestJob {
    }
}
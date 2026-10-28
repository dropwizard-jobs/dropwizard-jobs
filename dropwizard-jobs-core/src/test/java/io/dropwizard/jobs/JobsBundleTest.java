package io.dropwizard.jobs;

import com.codahale.metrics.SharedMetricRegistries;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;
import io.dropwizard.jobs.annotations.Every;
import io.dropwizard.jobs.annotations.On;
import io.dropwizard.jobs.annotations.OnApplicationStop;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.SimpleTrigger;
import org.quartz.CronTrigger;
import org.quartz.listeners.JobListenerSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

import static io.dropwizard.jobs.Job.DROPWIZARD_JOBS_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

@ResourceLock(DROPWIZARD_JOBS_KEY)
@Timeout(15)
public class JobsBundleTest {

    private final Environment environment = mock(Environment.class);
    private final LifecycleEnvironment applicationContext = mock(LifecycleEnvironment.class);
    private JobManager startedManager;

    @AfterEach
    public void removeSharedRegistry() throws Exception {
        try {
            if (startedManager != null && startedManager.getScheduler() != null && !startedManager.getScheduler().isShutdown()) {
                startedManager.getScheduler().shutdown(true);
            }
        } finally {
            SharedMetricRegistries.remove(DROPWIZARD_JOBS_KEY);
        }
    }

    @Test
    public void assertJobsBundleIsWorking() throws Exception {
        when(environment.lifecycle()).thenReturn(applicationContext);
        final JobsBundle jobsBundle = new JobsBundle(new ArrayList<>());
        jobsBundle.run(new MyConfiguration(), environment);

        final ArgumentCaptor<JobManager> captor = ArgumentCaptor.forClass(JobManager.class);
        verify(applicationContext).manage(captor.capture());

        JobManager jobManager = captor.getValue();
        assertThat(jobManager, is(notNullValue()));
        assertThat(jobsBundle.getScheduler(), is(jobManager.getScheduler()));
    }

    @Test
    public void shouldAddDropwizardJobsToMetricsRegistry() {
        new JobsBundle(new ArrayList<>()).initialize(new Bootstrap<>(new Application<MyConfiguration>() {
            @Override
            public void run(MyConfiguration myConfiguration, Environment environment) {

            }
        }));

        assertThat(SharedMetricRegistries.names(), hasItem(DROPWIZARD_JOBS_KEY));
    }

    @Test
    public void yamlConfigurationDrivesBundleExecutionMetricsAndShutdown() throws Exception {
        String yaml = """
                jobs:
                  interval: 25ms
                  cron: "0 0 12 ? * * [Europe/Stockholm]"
                quartz:
                  org.quartz.scheduler.instanceName: yaml-%s
                  org.quartz.scheduler.skipUpdateCheck: "true"
                  org.quartz.threadPool.class: org.quartz.simpl.SimpleThreadPool
                  org.quartz.threadPool.threadCount: "2"
                  io.github.dropwizard-jobs.timezone: Europe/London
                """.formatted(UUID.randomUUID());
        YamlConfigurationFactory<MutableJobConfiguration> factory = new YamlConfigurationFactory<>(
                MutableJobConfiguration.class, Validators.newValidator(), Jackson.newObjectMapper(), "dw");
        MutableJobConfiguration config = factory.build(
                path -> new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), "jobs.yml");
        assertEquals("2", config.getQuartzConfiguration().get("org.quartz.threadPool.threadCount"));

        ConfiguredIntervalTestJob intervalJob = new ConfiguredIntervalTestJob();
        ConfiguredStopTestJob stopJob = new ConfiguredStopTestJob();
        CountDownLatch listenerCalled = new CountDownLatch(1);
        JobListenerSupport listener = new JobListenerSupport() {
            @Override
            public String getName() {
                return "yaml-listener";
            }

            @Override
            public void jobWasExecuted(JobExecutionContext context, JobExecutionException failure) {
                if (context.getJobDetail().getKey().equals(JobKey.jobKey("yaml-interval"))) {
                    listenerCalled.countDown();
                }
            }
        };
        JobsBundle bundle = new JobsBundle(List.of(intervalJob, new ConfiguredCronTestJob(), stopJob), List.of(listener));
        Bootstrap<MyConfiguration> bootstrap = new Bootstrap<>(new Application<MyConfiguration>() {
            @Override
            public void run(MyConfiguration configuration, Environment environment) {
            }
        });
        bundle.initialize(bootstrap);
        when(environment.lifecycle()).thenReturn(applicationContext);
        bundle.run(config, environment);
        ArgumentCaptor<JobManager> captor = ArgumentCaptor.forClass(JobManager.class);
        verify(applicationContext).manage(captor.capture());
        startedManager = captor.getValue();
        startedManager.start();

        assertTrue(intervalJob.executed.await(2, TimeUnit.SECONDS));
        assertTrue(listenerCalled.await(2, TimeUnit.SECONDS));
        assertSame(bootstrap.getMetricRegistry(), intervalJob.getMetricRegistry());
        assertEquals(2, bundle.getScheduler().getMetaData().getThreadPoolSize());
        SimpleTrigger interval = (SimpleTrigger) bundle.getScheduler().getTriggersOfJob(JobKey.jobKey("yaml-interval")).get(0);
        assertEquals(25, interval.getRepeatInterval());
        CronTrigger cron = (CronTrigger) bundle.getScheduler().getTriggersOfJob(JobKey.jobKey("yaml-cron")).get(0);
        assertEquals("0 0 12 ? * *", cron.getCronExpression());
        assertEquals("Europe/Stockholm", cron.getTimeZone().getID());

        startedManager.stop();

        assertEquals(0, stopJob.executed.getCount());
        assertTrue(bundle.getScheduler().isShutdown());
        assertTrue(bootstrap.getMetricRegistry().timer(com.codahale.metrics.MetricRegistry.name(intervalJob.getClass())).getCount() > 0);
    }

    @Every(value = "${interval}", jobName = "yaml-interval")
    public static class ConfiguredIntervalTestJob extends Job {
        final CountDownLatch executed = new CountDownLatch(1);

        @Override
        public void doJob(JobExecutionContext context) {
            executed.countDown();
        }
    }

    @On(value = "${cron}", jobName = "yaml-cron")
    public static class ConfiguredCronTestJob extends Job {
        @Override
        public void doJob(JobExecutionContext context) {
        }
    }

    @OnApplicationStop
    public static class ConfiguredStopTestJob extends Job {
        final CountDownLatch executed = new CountDownLatch(1);

        @Override
        public void doJob(JobExecutionContext context) {
            executed.countDown();
        }
    }

    private static class MyConfiguration extends JobConfiguration {}
}

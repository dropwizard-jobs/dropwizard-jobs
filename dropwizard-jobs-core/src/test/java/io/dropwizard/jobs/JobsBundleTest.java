package io.dropwizard.jobs;

import com.codahale.metrics.SharedMetricRegistries;
import io.dropwizard.core.Application;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;

import static io.dropwizard.jobs.Job.DROPWIZARD_JOBS_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

public class JobsBundleTest {

    private final Environment environment = mock(Environment.class);
    private final LifecycleEnvironment applicationContext = mock(LifecycleEnvironment.class);

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

    private static class MyConfiguration extends JobConfiguration {}
}

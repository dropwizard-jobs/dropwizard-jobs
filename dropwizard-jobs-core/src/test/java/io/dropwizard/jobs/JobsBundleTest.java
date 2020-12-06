package io.dropwizard.jobs;

import static io.dropwizard.jobs.Job.DROPWIZARD_JOBS_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.codahale.metrics.SharedMetricRegistries;

import io.dropwizard.Application;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class JobsBundleTest {

    private final Environment environment = mock(Environment.class);
    private final LifecycleEnvironment applicationContext = mock(LifecycleEnvironment.class);

    @Test
    public void assertJobsBundleIsWorking() throws Exception {
        when(environment.lifecycle()).thenReturn(applicationContext);
        final JobsBundle jobsBundle = new JobsBundle();
        jobsBundle.run(new MyConfiguration(), environment);

        final ArgumentCaptor<JobManager> captor = ArgumentCaptor.forClass(JobManager.class);
        verify(applicationContext).manage(captor.capture());

        JobManager jobManager = captor.getValue();
        assertThat(jobManager, is(notNullValue()));
        assertThat(jobsBundle.getScheduler(), is(jobManager.getScheduler()));
    }

    @Test
    public void should_add_dropwizard_jobs_to_metrics_registry() throws Exception {
        new JobsBundle().initialize(new Bootstrap<>(new Application<MyConfiguration>() {
            @Override
            public void run(MyConfiguration myConfiguration, Environment environment) throws Exception {

            }
        }));

        assertThat(SharedMetricRegistries.names(), hasItem(DROPWIZARD_JOBS_KEY));
    }

    private static class MyConfiguration extends JobConfiguration {}
}

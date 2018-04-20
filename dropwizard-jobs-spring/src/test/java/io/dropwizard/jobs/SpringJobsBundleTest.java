package io.dropwizard.jobs;

import io.dropwizard.Configuration;
import io.dropwizard.jobs.ApplicationStartTestJob;
import io.dropwizard.jobs.ApplicationStopTestJob;
import io.dropwizard.jobs.EveryTestJob;
import io.dropwizard.jobs.JobConfiguration;
import io.dropwizard.jobs.JobManager;
import io.dropwizard.jobs.OnTestJob;
import io.dropwizard.jobs.SpringJobsBundle;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.setup.Environment;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SpringJobsBundleTest {

    private final Environment environment = mock(Environment.class);
    private final LifecycleEnvironment applicationContext = mock(LifecycleEnvironment.class);

    @Test
    public void assertJobsBundleIsWorking() throws Exception {
        ApplicationContext context = new AnnotationConfigApplicationContext(ApplicationStartTestJob.class,
                ApplicationStopTestJob.class, EveryTestJob.class, OnTestJob.class);

        when(environment.lifecycle()).thenReturn(applicationContext);
        new SpringJobsBundle(context).run(new MyConfiguration(), environment);

        final ArgumentCaptor<JobManager> jobManagerCaptor = ArgumentCaptor.forClass(JobManager.class);
        verify(applicationContext).manage(jobManagerCaptor.capture());

        JobManager jobManager = jobManagerCaptor.getValue();
        assertThat(jobManager, is(notNullValue()));
    }

    private static class MyConfiguration extends Configuration implements JobConfiguration {}
}

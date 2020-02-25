package io.dropwizard.jobs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.google.inject.Guice;
import com.google.inject.Injector;

import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.setup.Environment;

public class GuiceJobsBundleTest {

    private final Environment environment = mock(Environment.class);
    private final LifecycleEnvironment applicationContext = mock(LifecycleEnvironment.class);

    @Test
    public void assertJobsBundleIsWorking() throws Exception {
        Injector injector = Guice.createInjector();

        when(environment.lifecycle()).thenReturn(applicationContext);
        new GuiceJobsBundle(injector).run(new MyConfiguration(), environment);

        final ArgumentCaptor<JobManager> jobManagerCaptor = ArgumentCaptor.forClass(JobManager.class);
        verify(applicationContext).manage(jobManagerCaptor.capture());

        JobManager jobManager = jobManagerCaptor.getValue();
        assertThat(jobManager, is(notNullValue()));
    }

    private static class MyConfiguration extends JobConfiguration {}
}

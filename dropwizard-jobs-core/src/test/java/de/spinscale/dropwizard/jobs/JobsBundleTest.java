package de.spinscale.dropwizard.jobs;

import io.dropwizard.Configuration;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;
import io.dropwizard.setup.Environment;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JobsBundleTest {

    private final Environment environment = mock(Environment.class);
    private final LifecycleEnvironment applicationContext = mock(LifecycleEnvironment.class);

    @Test
    public void assertJobsBundleIsWorking() throws Exception {
        when(environment.lifecycle()).thenReturn(applicationContext);
        new JobsBundle().run(new MyConfiguration(), environment);

        final ArgumentCaptor<JobManager> captor = ArgumentCaptor.forClass(JobManager.class);
        verify(applicationContext).manage(captor.capture());

        JobManager jobManager = captor.getValue();
        assertThat(jobManager, is(notNullValue()));
    }

    private static class MyConfiguration extends Configuration implements JobConfiguration {}
}

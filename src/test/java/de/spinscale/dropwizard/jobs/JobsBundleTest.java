package de.spinscale.dropwizard.jobs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.*;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.codahale.dropwizard.jetty.MutableServletContextHandler;
import com.codahale.dropwizard.setup.Environment;

public class JobsBundleTest {

    private final Environment environment = mock(Environment.class);
	private final MutableServletContextHandler applicationContext = mock(MutableServletContextHandler.class);

    @Test
    public void assertJobsBundleIsWorking() {
    	when(environment.getApplicationContext()).thenReturn(applicationContext);
        new JobsBundle().run(environment);

        final ArgumentCaptor<JobManager> jobManagerCaptor = ArgumentCaptor.forClass(JobManager.class);
        verify(applicationContext).manage(jobManagerCaptor.capture());

        JobManager jobManager = jobManagerCaptor.getValue();
        assertThat(jobManager, is(notNullValue()));
    }
}

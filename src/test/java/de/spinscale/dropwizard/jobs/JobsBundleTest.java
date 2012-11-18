package de.spinscale.dropwizard.jobs;

import com.yammer.dropwizard.config.Environment;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class JobsBundleTest {

    private final Environment environment = mock(Environment.class);

    @Test
    public void assertJobsBundleIsWorking() {
        new JobsBundle().run(environment);

        final ArgumentCaptor<JobManager> jobManagerCaptor = ArgumentCaptor.forClass(JobManager.class);
        verify(environment).manage(jobManagerCaptor.capture());

        JobManager jobManager = jobManagerCaptor.getValue();
        assertThat(jobManager, is(notNullValue()));
    }
}

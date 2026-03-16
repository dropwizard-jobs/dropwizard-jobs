package io.dropwizard.jobs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.junit.jupiter.api.Test;

import io.dropwizard.core.setup.Environment;
import io.dropwizard.jersey.setup.JerseyEnvironment;

public class Hk2JobsBundleTest {

    private final Environment environment = mock(Environment.class);
    private final JerseyEnvironment jerseyEnvironment = mock(JerseyEnvironment.class);

    @Test
    public void testNullFilterThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            new Hk2JobsBundle(null);
        });
    }

    @Test
    public void testGetSchedulerReturnsNullBeforeStartup() throws Exception {
        Filter jobFilter = BuilderHelper.createContractFilter(Job.class.getName());
        Hk2JobsBundle bundle = new Hk2JobsBundle(jobFilter);

        when(environment.jersey()).thenReturn(jerseyEnvironment);
        bundle.run(new TestConfig(), environment);

        // getScheduler() should return null before container startup
        assertThat(bundle.getScheduler(), is(nullValue()));
    }

    @Test
    public void testBundleConstructionWithOneArg() {
        Filter jobFilter = BuilderHelper.createContractFilter(Job.class.getName());
        Hk2JobsBundle bundle = new Hk2JobsBundle(jobFilter);
        assertThat(bundle, is(notNullValue()));
    }

    @Test
    public void testBundleConstructionWithTwoArgs() {
        Filter jobFilter = BuilderHelper.createContractFilter(Job.class.getName());
        Filter listenerFilter = BuilderHelper.createContractFilter(org.quartz.JobListener.class.getName());
        Hk2JobsBundle bundle = new Hk2JobsBundle(jobFilter, listenerFilter);
        assertThat(bundle, is(notNullValue()));
    }

    @Test
    public void testBundleConstructionWithNullListenerFilter() {
        Filter jobFilter = BuilderHelper.createContractFilter(Job.class.getName());
        Hk2JobsBundle bundle = new Hk2JobsBundle(jobFilter, null);
        assertThat(bundle, is(notNullValue()));
    }

    public static class TestConfig extends JobConfiguration {
    }
}

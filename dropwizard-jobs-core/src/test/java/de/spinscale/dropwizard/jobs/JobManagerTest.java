package de.spinscale.dropwizard.jobs;

import io.dropwizard.Configuration;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class JobManagerTest {

    private JobManager jobManager = new JobManager();

    @Before
    public void ensureLatchesAreUntouched() {
        assertThat(ApplicationStartTestJob.latch.getCount(), is(1L));
        assertThat(OnTestJob.latch.getCount(), is(2L));
        assertThat(EveryTestJob.latch.getCount(), is(5L));
        assertThat(EveryTestJobWithDelay.latch.getCount(), is(5L));
        assertThat(EveryTestJobAlternativeConfiguration.latch.getCount(), is(5L));
        assertThat(ApplicationStopTestJob.latch.getCount(), is(1L));
    }

    @Test
    public void testThatJobsAreExecuted() throws Exception {
        TestConfig config = new TestConfig();
        config.getJobs().put("everyTestJobDefaultConfiguration", "50ms");
        config.getJobs().put("testJob", "50ms");
        jobManager.configure(config);

        jobManager.start();
        assertThat(ApplicationStartTestJob.latch.await(2, TimeUnit.SECONDS), is(true));
        assertThat(EveryTestJobWithDelay.latch.await(1, TimeUnit.SECONDS), is(false));

        assertThat(EveryTestJobAlternativeConfiguration.latch.await(1, TimeUnit.SECONDS), is(true));
        assertThat(EveryTestJobWithDelay.latch.await(2, TimeUnit.SECONDS), is(true));
        assertThat(EveryTestJob.latch.await(1, TimeUnit.SECONDS), is(true));

        assertThat(OnTestJob.latch.await(5, TimeUnit.SECONDS), is(true));

        jobManager.stop();
        assertThat(ApplicationStopTestJob.latch.await(2, TimeUnit.SECONDS), is(true));
    }

    private static class TestConfig extends Configuration {
        private Map<String,String> jobs = new HashMap<>();
        public Map<String, String> getJobs() {
            return jobs;
        }
        public void setJobs(Map<String, String> jobs) {
            this.jobs = jobs;
        }
    }
}

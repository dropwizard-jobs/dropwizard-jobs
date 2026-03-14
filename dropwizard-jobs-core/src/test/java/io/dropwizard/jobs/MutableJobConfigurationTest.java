package io.dropwizard.jobs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MutableJobConfiguration}.
 * <p>
 * Demonstrates how to use MutableJobConfiguration to configure Quartz properties
 * with periods in their keys, which is incompatible with Dropwizard's ConfigOverride.
 * </p>
 */
class MutableJobConfigurationTest {

    @Test
    void testEmptyConfiguration() {
        MutableJobConfiguration config = new MutableJobConfiguration();

        assertThat(config.getJobs(), is(anEmptyMap()));
        assertThat(config.getQuartzConfiguration(), is(anEmptyMap()));
        assertFalse(config.hasJobs());
        assertFalse(config.hasQuartzConfiguration());
    }

    @Test
    void testWithQuartzProperty() {
        MutableJobConfiguration config = new MutableJobConfiguration()
            .withQuartzProperty("org.quartz.scheduler.instanceName", "testScheduler")
            .withQuartzProperty("org.quartz.threadPool.threadCount", "5");

        assertThat(config.getQuartzConfiguration(), hasEntry("org.quartz.scheduler.instanceName", "testScheduler"));
        assertThat(config.getQuartzConfiguration(), hasEntry("org.quartz.threadPool.threadCount", "5"));
        assertTrue(config.hasQuartzConfiguration());
    }

    @Test
    void testWithQuartzPropertyWithPeriodsInKey() {
        // This is the key test case - Quartz property keys with periods
        // that would be misinterpreted by ConfigOverride.config()
        MutableJobConfiguration config = new MutableJobConfiguration()
            .withQuartzProperty("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX")
            .withQuartzProperty("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate")
            .withQuartzProperty("org.quartz.jobStore.dataSource", "myDS")
            .withQuartzProperty("org.quartz.dataSource.myDS.driver", "org.postgresql.Driver")
            .withQuartzProperty("org.quartz.dataSource.myDS.URL", "jdbc:postgresql://localhost:5432/test")
            .withQuartzProperty("org.quartz.dataSource.myDS.user", "testuser")
            .withQuartzProperty("org.quartz.dataSource.myDS.password", "testpass");

        Map<String, String> quartzConfig = config.getQuartzConfiguration();
        assertThat(quartzConfig, hasEntry("org.quartz.dataSource.myDS.URL", "jdbc:postgresql://localhost:5432/test"));
        assertThat(quartzConfig, hasEntry("org.quartz.dataSource.myDS.user", "testuser"));
        assertThat(quartzConfig, hasEntry("org.quartz.dataSource.myDS.password", "testpass"));
        assertThat(quartzConfig.size(), is(7));
    }

    @Test
    void testWithJob() {
        MutableJobConfiguration config = new MutableJobConfiguration()
            .withJob("myScheduledJob", "5s")
            .withJob("myCronJob", "0 0 12 * * ?");

        assertThat(config.getJobs(), hasEntry("myScheduledJob", "5s"));
        assertThat(config.getJobs(), hasEntry("myCronJob", "0 0 12 * * ?"));
        assertTrue(config.hasJobs());
    }

    @Test
    void testSetQuartzConfiguration() {
        Map<String, String> quartzProps = new HashMap<>();
        quartzProps.put("org.quartz.scheduler.instanceName", "myScheduler");
        quartzProps.put("org.quartz.threadPool.threadCount", "10");

        MutableJobConfiguration config = new MutableJobConfiguration()
            .setQuartzConfiguration(quartzProps);

        assertThat(config.getQuartzConfiguration(), hasEntry("org.quartz.scheduler.instanceName", "myScheduler"));
        assertThat(config.getQuartzConfiguration(), hasEntry("org.quartz.threadPool.threadCount", "10"));
    }

    @Test
    void testSetJobs() {
        Map<String, String> jobs = new HashMap<>();
        jobs.put("job1", "1m");
        jobs.put("job2", "0 0/5 * * * ?");

        MutableJobConfiguration config = new MutableJobConfiguration()
            .setJobs(jobs);

        assertThat(config.getJobs(), hasEntry("job1", "1m"));
        assertThat(config.getJobs(), hasEntry("job2", "0 0/5 * * * ?"));
    }

    @Test
    void testWithQuartzProperties() {
        Map<String, String> additionalProps = new HashMap<>();
        additionalProps.put("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX");
        additionalProps.put("org.quartz.jobStore.tablePrefix", "QRTZ_");

        MutableJobConfiguration config = new MutableJobConfiguration()
            .withQuartzProperty("org.quartz.scheduler.instanceName", "testScheduler")
            .withQuartzProperties(additionalProps);

        assertThat(config.getQuartzConfiguration(), hasEntry("org.quartz.scheduler.instanceName", "testScheduler"));
        assertThat(config.getQuartzConfiguration(), hasEntry("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX"));
        assertThat(config.getQuartzConfiguration(), hasEntry("org.quartz.jobStore.tablePrefix", "QRTZ_"));
        assertThat(config.getQuartzConfiguration().size(), is(3));
    }

    @Test
    void testClearJobs() {
        MutableJobConfiguration config = new MutableJobConfiguration()
            .withJob("job1", "5s")
            .withJob("job2", "10s");

        assertTrue(config.hasJobs());

        config.clearJobs();

        assertFalse(config.hasJobs());
        assertThat(config.getJobs(), is(anEmptyMap()));
    }

    @Test
    void testClearQuartzConfiguration() {
        MutableJobConfiguration config = new MutableJobConfiguration()
            .withQuartzProperty("org.quartz.scheduler.instanceName", "testScheduler");

        assertTrue(config.hasQuartzConfiguration());

        config.clearQuartzConfiguration();

        assertFalse(config.hasQuartzConfiguration());
        assertThat(config.getQuartzConfiguration(), is(anEmptyMap()));
    }

    @Test
    void testCopyFromExistingConfiguration() {
        // Create a source configuration
        MutableJobConfiguration source = new MutableJobConfiguration()
            .withJob("sourceJob", "30s")
            .withQuartzProperty("org.quartz.scheduler.instanceName", "sourceScheduler");

        // Copy to new configuration
        MutableJobConfiguration copy = new MutableJobConfiguration(source);

        assertThat(copy.getJobs(), hasEntry("sourceJob", "30s"));
        assertThat(copy.getQuartzConfiguration(), hasEntry("org.quartz.scheduler.instanceName", "sourceScheduler"));

        // Verify they are independent copies
        copy.withJob("newJob", "1m");
        assertThat(source.getJobs(), not(hasKey("newJob")));
    }

    @Test
    void testNullSafety() {
        MutableJobConfiguration config = new MutableJobConfiguration();

        // These should not throw
        config.setJobs(null);
        config.setQuartzConfiguration(null);
        config.withQuartzProperties(null);

        assertThat(config.getJobs(), is(anEmptyMap()));
        assertThat(config.getQuartzConfiguration(), is(anEmptyMap()));
    }

    @Test
    void testMethodChaining() {
        // Verify that all methods return the same instance for chaining
        MutableJobConfiguration config = new MutableJobConfiguration();

        assertSame(config, config.withJob("job1", "5s"));
        assertSame(config, config.withQuartzProperty("key", "value"));
        assertSame(config, config.setJobs(new HashMap<>()));
        assertSame(config, config.setQuartzConfiguration(new HashMap<>()));
        assertSame(config, config.clearJobs());
        assertSame(config, config.clearQuartzConfiguration());
    }
}

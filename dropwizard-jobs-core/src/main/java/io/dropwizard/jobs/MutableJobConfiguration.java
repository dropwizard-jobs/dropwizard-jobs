package io.dropwizard.jobs;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A mutable implementation of {@link JobConfiguration} for programmatic configuration,
 * particularly useful in testing scenarios with Testcontainers or dynamic configuration.
 * <p>
 * This class addresses the incompatibility between Dropwizard's {@code ConfigOverride.config()}
 * and Quartz property keys that contain periods (e.g., {@code org.quartz.dataSource.myDS.URL}).
 * Dropwizard interprets periods as hierarchical separators, which breaks flat Quartz property keys.
 * </p>
 * <p>
 * Example usage with Testcontainers for a JDBC job store:
 * </p>
 * <pre>{@code
 * PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
 * postgres.start();
 * 
 * MutableJobConfiguration config = new MutableJobConfiguration()
 *     .withQuartzProperty("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX")
 *     .withQuartzProperty("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate")
 *     .withQuartzProperty("org.quartz.jobStore.dataSource", "myDS")
 *     .withQuartzProperty("org.quartz.dataSource.myDS.driver", postgres.getDriverClassName())
 *     .withQuartzProperty("org.quartz.dataSource.myDS.URL", postgres.getJdbcUrl())
 *     .withQuartzProperty("org.quartz.dataSource.myDS.user", postgres.getUsername())
 *     .withQuartzProperty("org.quartz.dataSource.myDS.password", postgres.getPassword());
 * 
 * JobManager jobManager = new JobManager(config, jobs);
 * }</pre>
 * <p>
 * This approach bypasses the {@code ConfigOverride} limitation by constructing the
 * configuration programmatically, allowing Quartz property keys with periods to be
 * stored as flat string keys in the map.
 * </p>
 *
 * @see JobConfiguration
 * @see JobManager
 */
public class MutableJobConfiguration extends JobConfiguration {

    private Map<String, String> jobs = new HashMap<>();
    private Map<String, String> quartzConfiguration = new HashMap<>();

    /**
     * Creates a new empty mutable configuration.
     */
    public MutableJobConfiguration() {
        // Default constructor with empty maps
    }

    /**
     * Creates a mutable configuration initialized from an existing configuration.
     *
     * @param source the configuration to copy initial values from
     */
    public MutableJobConfiguration(JobConfiguration source) {
        if (source.getJobs() != null) {
            this.jobs.putAll(source.getJobs());
        }
        if (source.getQuartzConfiguration() != null) {
            this.quartzConfiguration.putAll(source.getQuartzConfiguration());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getJobs() {
        return jobs;
    }

    /**
     * Sets the job schedule overrides map.
     *
     * @param jobs a map of job names to schedule expressions
     * @return this configuration for method chaining
     */
    public MutableJobConfiguration setJobs(Map<String, String> jobs) {
        this.jobs = jobs != null ? new HashMap<>(jobs) : new HashMap<>();
        return this;
    }

    /**
     * Adds a single job schedule override.
     *
     * @param jobName the job name (matching the annotation value)
     * @param schedule the schedule expression (duration or cron)
     * @return this configuration for method chaining
     */
    public MutableJobConfiguration withJob(String jobName, String schedule) {
        this.jobs.put(jobName, schedule);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @JsonProperty("quartz")
    public Map<String, String> getQuartzConfiguration() {
        return quartzConfiguration;
    }

    /**
     * Sets the Quartz configuration properties.
     * <p>
     * This replaces any existing Quartz configuration.
     * </p>
     *
     * @param quartzConfiguration a map of Quartz property names to values
     * @return this configuration for method chaining
     */
    public MutableJobConfiguration setQuartzConfiguration(Map<String, String> quartzConfiguration) {
        this.quartzConfiguration = quartzConfiguration != null ? new HashMap<>(quartzConfiguration) : new HashMap<>();
        return this;
    }

    /**
     * Adds a single Quartz configuration property.
     * <p>
     * This method is the key solution for the {@code ConfigOverride} incompatibility.
     * It allows setting Quartz properties with periods in their keys (e.g.,
     * {@code org.quartz.dataSource.myDS.URL}) without Dropwizard interpreting
     * the periods as hierarchical separators.
     * </p>
     *
     * @param key the Quartz property name (e.g., "org.quartz.dataSource.myDS.URL")
     * @param value the property value
     * @return this configuration for method chaining
     */
    public MutableJobConfiguration withQuartzProperty(String key, String value) {
        this.quartzConfiguration.put(key, value);
        return this;
    }

    /**
     * Adds multiple Quartz configuration properties.
     *
     * @param properties the properties to add
     * @return this configuration for method chaining
     */
    public MutableJobConfiguration withQuartzProperties(Map<String, String> properties) {
        if (properties != null) {
            this.quartzConfiguration.putAll(properties);
        }
        return this;
    }

    /**
     * Clears all job schedule overrides.
     *
     * @return this configuration for method chaining
     */
    public MutableJobConfiguration clearJobs() {
        this.jobs.clear();
        return this;
    }

    /**
     * Clears all Quartz configuration properties.
     *
     * @return this configuration for method chaining
     */
    public MutableJobConfiguration clearQuartzConfiguration() {
        this.quartzConfiguration.clear();
        return this;
    }
}

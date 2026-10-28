package io.dropwizard.jobs;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.core.Configuration;

/**
 * Base configuration class for Dropwizard applications using the jobs bundle.
 * <p>
 * Extend this class in your application to enable job scheduling configuration.
 * Your configuration class can override {@link #getJobs()} to provide runtime
 * overrides for job schedules, and {@link #getQuartzConfiguration()} to customize
 * the Quartz scheduler settings.
 * </p>
 * <p>
 * Example usage in YAML configuration:
 * </p>
 * <pre>{@code
 * jobs:
 *   myJob: "5s"  # Schedule for @Every("${myJob}") or @Every on MyJob
 * quartz:
 *   org.quartz.scheduler.instanceName: "myScheduler"
 * }</pre>
 *
 * @see JobsBundle
 * @see JobManager
 */
public class JobConfiguration extends Configuration {

    /**
     * Returns a map of job names to their schedule values for runtime override.
     * <p>
    * An explicit placeholder such as {@code @Every("${myJob}")} or
    * {@code @On("${myJob}")} selects the key {@code myJob}. When the annotation
    * value is empty, the key is the job's simple class name with its first character
    * lowercased: {@code MyJob} uses {@code myJob}.
    * </p>
    * <p>
    * Literal annotation values such as {@code @Every("5s")} are used directly;
    * they are not looked up in this map.
     * </p>
     * <p>
     * For {@code @Every} jobs, values can be durations like "5s", "1m", "1h".
     * For {@code @On} jobs, values should be valid cron expressions.
     * </p>
     *
     * @return a map of job name to schedule override, empty map by default
     */
    public Map<String, String> getJobs() {
        return Collections.emptyMap();
    }

    /**
     * Returns Quartz scheduler configuration properties.
     * <p>
     * These properties are passed directly to the Quartz {@code StdSchedulerFactory}
     * to configure the scheduler. Common properties include:
     * </p>
     * <ul>
     *   <li>{@code org.quartz.scheduler.instanceName} - Scheduler name</li>
     *   <li>{@code org.quartz.threadPool.threadCount} - Thread pool size</li>
     *   <li>{@code org.quartz.jobStore.class} - Job store implementation</li>
     * </ul>
     * <p>
     * If this method returns an empty map (the default), the scheduler uses
     * {@link DefaultQuartzConfiguration#get()} which provides a RAM-based job store.
     * For clustered environments, override this method to provide JDBC job store
     * configuration.
     * </p>
     *
     * @return a map of Quartz configuration properties, empty map by default
     */
    @JsonProperty("quartz")
    public Map<String, String> getQuartzConfiguration() {
        return Collections.emptyMap();
    }

    /**
     * Checks if any job schedule overrides are defined.
     * <p>
     * Returns {@code true} if {@link #getJobs()} returns a non-empty map,
     * indicating that one or more job schedules may be overridden at runtime.
     * </p>
     *
     * @return {@code true} if job schedule overrides are configured, {@code false} otherwise
     */
    public boolean hasJobs() {
        return !getJobs().isEmpty();
    }

    /**
     * Checks if any Quartz configuration is defined.
     * <p>
     * Returns {@code true} if {@link #getQuartzConfiguration()} returns a non-empty map,
     * indicating that custom Quartz scheduler settings are provided.
     * </p>
     *
     * @return {@code true} if Quartz configuration is provided, {@code false} otherwise
     */
    public boolean hasQuartzConfiguration() {
        return !getQuartzConfiguration().isEmpty();
    }
}

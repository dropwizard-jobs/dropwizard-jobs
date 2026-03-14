package io.dropwizard.jobs.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@link org.quartz.JobListener} implementation for automatic registration
 * with the Quartz scheduler.
 * <p>
 * The annotation specifies which jobs the listener should receive events for,
 * using a {@link MatcherType} to define the matching strategy and an optional
 * {@link #value()} for the match pattern.
 * </p>
 * <p>
 * Example usage:
 * </p>
 * <pre>{@code
 * // Listen to ALL jobs
 * @ListeningFor
 * public class AuditListener implements JobListener { ... }
 *
 * // Listen to a specific job by name
 * @ListeningFor(matcher = MatcherType.JOB_NAME_EQUALS, value = "myJobName")
 * public class SpecificJobListener implements JobListener { ... }
 *
 * // Listen to all jobs in a group
 * @ListeningFor(matcher = MatcherType.JOB_GROUP_EQUALS, value = "reporting")
 * public class GroupListener implements JobListener { ... }
 * }</pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ListeningFor {

    /**
     * The matcher type that determines how jobs are matched for this listener.
     * Defaults to {@link MatcherType#ALL_JOBS}, which matches every job.
     *
     * @return the matcher type
     */
    MatcherType matcher() default MatcherType.ALL_JOBS;

    /**
     * The value used by the matcher for pattern matching.
     * <p>
     * This is required for all matcher types except {@link MatcherType#ALL_JOBS}.
     * The meaning depends on the matcher type:
     * </p>
     * <ul>
     *   <li>{@code JOB_NAME_EQUALS} - the exact job name to match</li>
     *   <li>{@code JOB_GROUP_EQUALS} - the exact group name to match</li>
     *   <li>{@code JOB_NAME_STARTS_WITH} - the job name prefix</li>
     *   <li>{@code JOB_NAME_ENDS_WITH} - the job name suffix</li>
     *   <li>{@code JOB_NAME_CONTAINS} - the job name substring</li>
     *   <li>{@code JOB_GROUP_STARTS_WITH} - the group name prefix</li>
     *   <li>{@code JOB_GROUP_ENDS_WITH} - the group name suffix</li>
     *   <li>{@code JOB_GROUP_CONTAINS} - the group name substring</li>
     * </ul>
     *
     * @return the match pattern value
     */
    String value() default "";

    /**
     * Defines the matching strategy for determining which jobs a listener receives events for.
     * <p>
     * Each enum constant maps directly to a Quartz {@link org.quartz.Matcher} implementation:
     * </p>
     * <ul>
     *   <li>{@link #ALL_JOBS} → {@code EverythingMatcher.allJobs()}</li>
     *   <li>{@link #JOB_NAME_EQUALS} → {@code KeyMatcher.keyEquals(JobKey.jobKey(value))}</li>
     *   <li>{@link #JOB_GROUP_EQUALS} → {@code GroupMatcher.jobGroupEquals(value)}</li>
     *   <li>{@link #JOB_NAME_STARTS_WITH} → {@code NameMatcher.nameStartsWith(value)}</li>
     *   <li>{@link #JOB_NAME_ENDS_WITH} → {@code NameMatcher.nameEndsWith(value)}</li>
     *   <li>{@link #JOB_NAME_CONTAINS} → {@code NameMatcher.nameContains(value)}</li>
     *   <li>{@link #JOB_GROUP_STARTS_WITH} → {@code GroupMatcher.jobGroupStartsWith(value)}</li>
     *   <li>{@link #JOB_GROUP_ENDS_WITH} → {@code GroupMatcher.jobGroupEndsWith(value)}</li>
     *   <li>{@link #JOB_GROUP_CONTAINS} → {@code GroupMatcher.jobGroupContains(value)}</li>
     * </ul>
     */
    enum MatcherType {
        /** Matches all jobs. No value required. */
        ALL_JOBS,
        /** Matches a job with the exact name specified in value. */
        JOB_NAME_EQUALS,
        /** Matches all jobs in the group specified in value. */
        JOB_GROUP_EQUALS,
        /** Matches jobs whose name starts with value. */
        JOB_NAME_STARTS_WITH,
        /** Matches jobs whose name ends with value. */
        JOB_NAME_ENDS_WITH,
        /** Matches jobs whose name contains value. */
        JOB_NAME_CONTAINS,
        /** Matches jobs in groups whose name starts with value. */
        JOB_GROUP_STARTS_WITH,
        /** Matches jobs in groups whose name ends with value. */
        JOB_GROUP_ENDS_WITH,
        /** Matches jobs in groups whose name contains value. */
        JOB_GROUP_CONTAINS,
    }
}

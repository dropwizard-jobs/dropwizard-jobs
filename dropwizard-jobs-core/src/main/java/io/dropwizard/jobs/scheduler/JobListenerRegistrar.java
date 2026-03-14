package io.dropwizard.jobs.scheduler;

import io.dropwizard.jobs.annotations.ListeningFor;
import io.dropwizard.jobs.annotations.ListeningFor.MatcherType;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.Matcher;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.matchers.EverythingMatcher;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.impl.matchers.KeyMatcher;
import org.quartz.impl.matchers.NameMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Registers {@link JobListener} instances with a Quartz {@link Scheduler}.
 * <p>
 * Reads the {@link ListeningFor} annotation from each listener to determine
 * which jobs the listener should observe. If the annotation is absent,
 * the listener is registered for all jobs.
 * </p>
 * <p>
 * This class follows the Strategy pattern used by other classes in the
 * {@code scheduler/} sub-package, but does not extend {@link JobScheduler}
 * since it performs listener registration rather than job scheduling.
 * </p>
 */
public class JobListenerRegistrar {

    private static final Logger log = LoggerFactory.getLogger(JobListenerRegistrar.class);

    private final Scheduler scheduler;
    private final List<JobListener> jobListeners;

    /**
     * Creates a new registrar with the specified scheduler and listeners.
     *
     * @param scheduler the Quartz scheduler to register listeners with
     * @param jobListeners the list of job listeners to register
     */
    public JobListenerRegistrar(Scheduler scheduler, List<JobListener> jobListeners) {
        this.scheduler = scheduler;
        this.jobListeners = jobListeners;
    }

    /**
     * Registers all job listeners with the scheduler's ListenerManager.
     * <p>
     * Each listener's {@link ListeningFor} annotation determines the matcher used.
     * Listeners without the annotation default to ALL_JOBS matching.
     * </p>
     *
     * @throws SchedulerException if listener registration fails
     */
    public void register() throws SchedulerException {
        for (JobListener listener : jobListeners) {
            Matcher<JobKey> matcher = resolveMatcherFrom(listener.getClass());
            scheduler.getListenerManager().addJobListener(listener, matcher);
            log.info("Registered JobListener '{}' with matcher: {}",
                    listener.getName(), describeMatch(listener.getClass()));
        }
    }

    /**
     * Reads the {@link ListeningFor} annotation and converts it to a Quartz Matcher.
     * <p>
     * This method is package-private to allow testing without reflection.
     * </p>
     *
     * @param listenerClass the class of the job listener
     * @return a Quartz matcher for the specified listener
     */
    static Matcher<JobKey> resolveMatcherFrom(Class<?> listenerClass) {
        ListeningFor annotation = listenerClass.getAnnotation(ListeningFor.class);
        if (annotation == null) {
            return EverythingMatcher.allJobs();
        }
        return toMatcher(annotation.matcher(), annotation.value());
    }

    /**
     * Converts a MatcherType enum and value to a Quartz Matcher instance.
     *
     * @param type the matcher type from the annotation
     * @param value the value pattern for matching
     * @return a Quartz matcher
     */
    static Matcher<JobKey> toMatcher(MatcherType type, String value) {
        return switch (type) {
            case ALL_JOBS -> EverythingMatcher.allJobs();
            case JOB_NAME_EQUALS -> KeyMatcher.keyEquals(JobKey.jobKey(value));
            case JOB_GROUP_EQUALS -> GroupMatcher.jobGroupEquals(value);
            case JOB_NAME_STARTS_WITH -> NameMatcher.nameStartsWith(value);
            case JOB_NAME_ENDS_WITH -> NameMatcher.nameEndsWith(value);
            case JOB_NAME_CONTAINS -> NameMatcher.nameContains(value);
            case JOB_GROUP_STARTS_WITH -> GroupMatcher.jobGroupStartsWith(value);
            case JOB_GROUP_ENDS_WITH -> GroupMatcher.jobGroupEndsWith(value);
            case JOB_GROUP_CONTAINS -> GroupMatcher.jobGroupContains(value);
        };
    }

    /**
     * Creates a human-readable description of the matcher for a listener.
     *
     * @param listenerClass the class of the job listener
     * @return a description string for logging
     */
    private static String describeMatch(Class<?> listenerClass) {
        ListeningFor annotation = listenerClass.getAnnotation(ListeningFor.class);
        if (annotation == null) {
            return "ALL_JOBS (default - no @ListeningFor annotation)";
        }
        if (annotation.matcher() == MatcherType.ALL_JOBS) {
            return "ALL_JOBS";
        }
        return annotation.matcher() + "('" + annotation.value() + "')";
    }
}

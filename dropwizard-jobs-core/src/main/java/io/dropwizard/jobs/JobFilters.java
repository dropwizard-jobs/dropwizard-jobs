package io.dropwizard.jobs;

import io.dropwizard.jobs.annotations.Every;
import io.dropwizard.jobs.annotations.On;
import io.dropwizard.jobs.annotations.OnApplicationStart;
import io.dropwizard.jobs.annotations.OnApplicationStop;
import org.quartz.utils.Key;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class JobFilters {
    private final List<JobMetadata> jobs;

    public JobFilters(List<JobMetadata> jobs) {
        this.jobs = jobs;
    }

    public Stream<JobMetadata> allOnApplicationStop() {
        return jobs.stream()
                .filter(job -> job.getJobClass().isAnnotationPresent(OnApplicationStop.class));
    }

    public Stream<JobMetadata> allOnCron() {
        return jobs.stream()
                .filter(job -> job.getJobClass().isAnnotationPresent(On.class));
    }

    public Stream<JobMetadata> allEvery() {
        return jobs.stream()
                .filter(job -> job.getJobClass().isAnnotationPresent(Every.class));
    }

    public Stream<JobMetadata> allOnApplicationStart() {
        return jobs.stream()
                .filter(job -> job.getJobClass().isAnnotationPresent(OnApplicationStart.class));
    }

    public Optional<JobMetadata> findWith(Class<? extends org.quartz.Job> jobClass, String groupName) {
        return jobs.stream()
                .filter(job -> isJobClassMatch(job.getJobClass(), jobClass) && equalGroupName(job, groupName))
                .findFirst();
    }

    /**
     * Checks if the job class matches, accounting for AOP proxy classes.
     * AOP frameworks create proxy subclasses, so we need to check both directions:
     * - jobClass.isAssignableFrom(job.getJobClass()) - when jobClass is the original and job is a proxy
     * - job.getJobClass().isAssignableFrom(jobClass) - when job is the original and jobClass is a proxy
     *
     * @param jobClass the job class from the JobDetail (may be a proxy class)
     * @param actualJobClass the actual job's class (may be a proxy class)
     * @return true if the classes match or are in the same inheritance hierarchy
     */
    private boolean isJobClassMatch(Class<? extends org.quartz.Job> actualJobClass,
            Class<? extends org.quartz.Job> jobClass) {
        return jobClass.isAssignableFrom(actualJobClass) || actualJobClass.isAssignableFrom(jobClass);
    }

    private boolean equalGroupName(final JobMetadata job, final String groupName) {
        String jobGroupName = job.getGroupName();
        return (Objects.equals(groupName, Key.DEFAULT_GROUP) && jobGroupName == null)
                || Objects.equals(jobGroupName, groupName);
    }
}

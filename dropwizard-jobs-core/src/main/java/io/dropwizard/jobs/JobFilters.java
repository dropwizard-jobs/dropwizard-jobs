package io.dropwizard.jobs;

import io.dropwizard.jobs.annotations.Every;
import io.dropwizard.jobs.annotations.On;
import io.dropwizard.jobs.annotations.OnApplicationStart;
import io.dropwizard.jobs.annotations.OnApplicationStop;
import org.quartz.utils.Key;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class JobFilters {
    private final Job[] jobs;

    public JobFilters(Job[] jobs) {

        this.jobs = jobs;
    }

    public Stream<Job> allOnApplicationStop() {
        return Arrays.stream(jobs)
                .filter(job -> job.getClass().isAnnotationPresent(OnApplicationStop.class));
    }

    public Stream<Job> allOnCron() {
        return Arrays.stream(this.jobs)
                .filter(job -> job.getClass().isAnnotationPresent(On.class));
    }

    public Stream<Job> allEvery() {
        return Arrays.stream(this.jobs)
                .filter(job -> job.getClass().isAnnotationPresent(Every.class));
    }

    public Stream<Job> allOnApplicationStart() {
        return Arrays.stream(this.jobs)
                .filter(job -> job.getClass().isAnnotationPresent(OnApplicationStart.class));
    }

    public Optional<Job> findWith(Class<? extends org.quartz.Job> jobClass, String groupName) {
        return Arrays.stream(this.jobs)
                .filter(job -> job.getClass().equals(jobClass) && equalGroupName(job, groupName))
                .findFirst();
    }

    private boolean equalGroupName(final Job job, final String groupName) {
        String jobGroupName = job.getGroupName();
        return (Objects.equals(groupName, Key.DEFAULT_GROUP) && jobGroupName == null)
                || Objects.equals(jobGroupName, groupName);
    }
}

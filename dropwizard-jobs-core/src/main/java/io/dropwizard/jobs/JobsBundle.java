package io.dropwizard.jobs;

import com.codahale.metrics.SharedMetricRegistries;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.quartz.Scheduler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JobsBundle implements ConfiguredBundle<JobConfiguration> {

    private final List<Job> jobs;
    protected JobManager jobManager;

    public JobsBundle(List<Job> jobs) {
        Objects.requireNonNull(jobs, "jobs must not be null");
        this.jobs = new ArrayList<>(jobs);
    }

    @Override
    public void run(JobConfiguration configuration, Environment environment) throws Exception {
        jobManager = new JobManager(configuration, jobs);
        environment.lifecycle().manage(jobManager);
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        // add shared metrics registry to be used by Jobs, since defaultRegistry
        // has been removed. Use idempotent registration pattern to avoid exceptions
        // when the key is already registered (e.g., in test environments).
        if (!SharedMetricRegistries.names().contains(Job.DROPWIZARD_JOBS_KEY)) {
            SharedMetricRegistries.add(Job.DROPWIZARD_JOBS_KEY, bootstrap.getMetricRegistry());
        }
    }

    public Scheduler getScheduler() {
        return jobManager.getScheduler();
    }

}

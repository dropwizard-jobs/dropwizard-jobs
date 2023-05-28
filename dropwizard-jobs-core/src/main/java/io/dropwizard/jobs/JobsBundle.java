package io.dropwizard.jobs;

import com.codahale.metrics.SharedMetricRegistries;
import io.dropwizard.core.ConfiguredBundle;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import org.quartz.Scheduler;

import java.util.List;

public class JobsBundle implements ConfiguredBundle<JobConfiguration> {

    private final List<Job> jobs;
    protected JobManager jobManager;

    public JobsBundle(List<Job> jobs) {
        this.jobs = jobs;
    }

    @Override
    public void run(JobConfiguration configuration, Environment environment) throws Exception {
        jobManager = new JobManager(configuration, jobs);
        environment.lifecycle().manage(jobManager);
    }

    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        // add shared metrics registry to be used by Jobs, since defaultRegistry
        // has been removed
        SharedMetricRegistries.add(Job.DROPWIZARD_JOBS_KEY, bootstrap.getMetricRegistry());
    }

    public Scheduler getScheduler() {
        return jobManager.getScheduler();
    }

}

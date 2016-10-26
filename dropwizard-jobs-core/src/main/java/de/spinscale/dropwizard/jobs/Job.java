package de.spinscale.dropwizard.jobs;

import static com.codahale.metrics.MetricRegistry.name;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

public abstract class Job implements org.quartz.Job {
    public static final String DROPWIZARD_JOBS_KEY = "dropwizard-jobs";

    private final Timer timer;

    public Job() {
        // get the metrics registry which was shared during bundle instantiation
        this(SharedMetricRegistries.getOrCreate(DROPWIZARD_JOBS_KEY));
    }

    public Job(MetricRegistry metricRegistry) {
        timer = metricRegistry.timer(name(getClass(), getClass().getName()));
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try (Context timerContext = timer.time()) {
            doJob();
        }
    }

    public abstract void doJob();
}

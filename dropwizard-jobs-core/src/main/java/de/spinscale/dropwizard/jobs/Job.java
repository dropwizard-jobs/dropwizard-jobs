package de.spinscale.dropwizard.jobs;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import static com.codahale.metrics.MetricRegistry.name;

public abstract class Job implements org.quartz.Job {

    public static final String DROPWIZARD_JOBS_KEY = "dropwizard-jobs";

    private final Timer timer;
    private final MetricRegistry metricRegistry;

    public Job() {
        // get the metrics registry which was shared during bundle instantiation
        this(SharedMetricRegistries.getOrCreate(DROPWIZARD_JOBS_KEY));
    }

    public Job(MetricRegistry metricRegistry) {
        this.timer = metricRegistry.timer(name(getClass()));
        this.metricRegistry = metricRegistry;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try (Context timerContext = timer.time()) {
            doJob(context);
        }
    }

    public abstract void doJob(JobExecutionContext context) throws JobExecutionException;

    protected MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }

}

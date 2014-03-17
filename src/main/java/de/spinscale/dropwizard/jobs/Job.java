package de.spinscale.dropwizard.jobs;

import static com.codahale.metrics.MetricRegistry.name;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

public abstract class Job implements org.quartz.Job {

    private final Timer timer;

    public Job() {
    	MetricRegistry registry = SharedMetricRegistries.getOrCreate("dropwizard-jobs");
        timer = registry.timer(name(getClass(), getClass().getName()));
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Context timerContext = timer.time();
        try {
            doJob();
        } finally {
            timerContext.stop();
        }
    }

    public abstract void doJob();
}

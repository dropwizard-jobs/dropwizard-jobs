package de.spinscale.dropwizard.jobs;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import static com.codahale.metrics.MetricRegistry.name;

public abstract class Job implements org.quartz.Job {

    private final Timer timer;

    public Job () {
        // get the metrics registry which was shared during bundle instantiation
        this(SharedMetricRegistries.getOrCreate("dropwizard-jobs"));
    }

    public Job(MetricRegistry metricRegistry) {
        timer = metricRegistry.timer(name(getClass(), getClass().getName()));
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Timer.Context timerContext = timer.time();
        try {
            doJob();
        } finally {
            timerContext.stop();
        }
    }

    public abstract void doJob();
}

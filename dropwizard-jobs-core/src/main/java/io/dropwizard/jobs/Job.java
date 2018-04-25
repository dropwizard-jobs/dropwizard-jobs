package io.dropwizard.jobs;

import static com.codahale.metrics.MetricRegistry.name;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.SharedMetricRegistries;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public abstract class Job implements org.quartz.Job {

    public static final String DROPWIZARD_JOBS_KEY = "dropwizard-jobs";
    private final String groupName;

    private final Timer timer;
    private final MetricRegistry metricRegistry;

    public Job() {
        // get the metrics registry which was shared during bundle instantiation
        this(SharedMetricRegistries.getOrCreate(DROPWIZARD_JOBS_KEY), null);
    }

    public Job(String groupName) {
        this(SharedMetricRegistries.getOrCreate(DROPWIZARD_JOBS_KEY), groupName);
    }

    public Job(MetricRegistry metricRegistry) {
        this(metricRegistry, null);
    }

    public Job(MetricRegistry metricRegistry, String groupName) {
        this.timer = metricRegistry.timer(name(getClass()));
        this.metricRegistry = metricRegistry;
        this.groupName = groupName;
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

    public String getGroupName() {
        return groupName;
    }
}

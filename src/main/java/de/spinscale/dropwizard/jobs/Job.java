package de.spinscale.dropwizard.jobs;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public abstract class Job implements org.quartz.Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        doJob();
    }

    public abstract void doJob();
}

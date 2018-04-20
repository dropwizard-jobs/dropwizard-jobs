package io.dropwizard.jobs;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import io.dropwizard.jobs.annotations.OnApplicationStop;

@OnApplicationStop
public class ApplicationStopTestJob extends AbstractJob {

    public ApplicationStopTestJob() {
        super(1);
    }

    @Override
    public void doJob(JobExecutionContext context) throws JobExecutionException {
        super.doJob(context);
    }
}

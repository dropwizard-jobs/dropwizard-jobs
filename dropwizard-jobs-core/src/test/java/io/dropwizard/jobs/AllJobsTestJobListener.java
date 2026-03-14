package io.dropwizard.jobs;

import io.dropwizard.jobs.annotations.ListeningFor;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

@ListeningFor
public class AllJobsTestJobListener implements JobListener {

    @Override
    public String getName() {
        return "AllJobsTestJobListener";
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
    }
}

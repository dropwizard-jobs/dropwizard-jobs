package io.dropwizard.jobs;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

// No @ListeningFor annotation — should default to ALL_JOBS
public class UnannotatedTestJobListener implements JobListener {

    @Override
    public String getName() {
        return "UnannotatedTestJobListener";
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

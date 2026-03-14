package io.dropwizard.jobs;

import io.dropwizard.jobs.annotations.ListeningFor;
import io.dropwizard.jobs.annotations.ListeningFor.MatcherType;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;

@ListeningFor(matcher = MatcherType.JOB_NAME_EQUALS, value = "testJob")
public class NameMatchTestJobListener implements JobListener {

    @Override
    public String getName() {
        return "NameMatchTestJobListener";
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

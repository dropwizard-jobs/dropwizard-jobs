package io.dropwizard.jobs;

import io.dropwizard.jobs.Job;
import io.dropwizard.jobs.annotations.OnApplicationStop;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.concurrent.CountDownLatch;

@OnApplicationStop
public class ApplicationStopTestJob extends Job {

    final CountDownLatch latch = new CountDownLatch(1);

    @Override
    public void doJob(JobExecutionContext context) throws JobExecutionException {
        latch.countDown();
    }

}

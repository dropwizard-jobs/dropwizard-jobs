package io.dropwizard.jobs;

import io.dropwizard.jobs.Job;
import io.dropwizard.jobs.annotations.OnApplicationStart;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.concurrent.CountDownLatch;

@OnApplicationStart
public class ApplicationStartTestJob extends Job {

    final CountDownLatch latch = new CountDownLatch(1);

    @Override
    public void doJob(JobExecutionContext context) throws JobExecutionException {
        latch.countDown();
    }
}

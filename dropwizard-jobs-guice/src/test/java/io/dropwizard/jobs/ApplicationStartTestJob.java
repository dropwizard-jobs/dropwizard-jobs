package io.dropwizard.jobs;

import java.util.concurrent.CountDownLatch;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import io.dropwizard.jobs.annotations.OnApplicationStart;

@OnApplicationStart
public class ApplicationStartTestJob extends Job {

    final CountDownLatch latch = new CountDownLatch(1);

    @Override
    public void doJob(JobExecutionContext context) throws JobExecutionException {
        latch.countDown();
    }
}

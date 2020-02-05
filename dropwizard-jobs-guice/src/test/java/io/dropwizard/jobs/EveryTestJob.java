package io.dropwizard.jobs;

import java.util.concurrent.CountDownLatch;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import io.dropwizard.jobs.annotations.Every;

@Every("100ms")
public class EveryTestJob extends Job {

    final CountDownLatch latch = new CountDownLatch(5);

    @Override
    public void doJob(JobExecutionContext context) throws JobExecutionException {
        latch.countDown();
    }
}

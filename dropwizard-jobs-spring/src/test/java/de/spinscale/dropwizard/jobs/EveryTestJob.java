package de.spinscale.dropwizard.jobs;

import de.spinscale.dropwizard.jobs.annotations.Every;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.concurrent.CountDownLatch;

@Every("50ms")
public class EveryTestJob extends Job {

    final CountDownLatch latch = new CountDownLatch(5);

    @Override
    public void doJob(JobExecutionContext context) throws JobExecutionException {
        latch.countDown();
    }
}

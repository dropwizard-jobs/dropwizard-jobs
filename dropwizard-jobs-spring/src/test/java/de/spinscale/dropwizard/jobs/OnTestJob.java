package de.spinscale.dropwizard.jobs;

import de.spinscale.dropwizard.jobs.annotations.On;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import java.util.concurrent.CountDownLatch;

@On("0/1 * * * * ?")
public class OnTestJob extends Job {

    final CountDownLatch latch = new CountDownLatch(2);

    @Override
    public void doJob(JobExecutionContext context) throws JobExecutionException {
        latch.countDown();
    }
}

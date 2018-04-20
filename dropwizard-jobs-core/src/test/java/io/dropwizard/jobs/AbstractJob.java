package io.dropwizard.jobs;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import io.dropwizard.jobs.Job;

import java.util.concurrent.CountDownLatch;

public abstract class AbstractJob extends Job {

    private final CountDownLatch latch;

    public AbstractJob(int count) {
        latch = new CountDownLatch(count);
    }

    public AbstractJob(int count, String groupName) {
        super(groupName);
        latch = new CountDownLatch(count);
    }

    @Override
    public void doJob(JobExecutionContext context) throws JobExecutionException {
        latch.countDown();
    }

    public CountDownLatch latch() {
        return latch;
    }
}

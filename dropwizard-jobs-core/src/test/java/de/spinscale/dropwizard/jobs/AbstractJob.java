package de.spinscale.dropwizard.jobs;

import java.util.concurrent.CountDownLatch;

public abstract class AbstractJob extends Job {

    private final CountDownLatch latch;

    public AbstractJob(int count) {
        latch = new CountDownLatch(count);
    }

    @Override
    public void doJob() {
        latch.countDown();
    }

    public CountDownLatch latch() {
        return latch;
    }
}

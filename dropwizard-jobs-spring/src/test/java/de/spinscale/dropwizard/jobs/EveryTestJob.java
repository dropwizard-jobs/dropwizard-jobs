package de.spinscale.dropwizard.jobs;

import de.spinscale.dropwizard.jobs.annotations.Every;

import java.util.concurrent.CountDownLatch;

@Every("50ms")
public class EveryTestJob extends Job {

    static final CountDownLatch latch = new CountDownLatch(5);

    @Override
    public void doJob() {
        latch.countDown();
    }
}

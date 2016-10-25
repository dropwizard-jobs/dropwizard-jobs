package de.spinscale.dropwizard.jobs;

import de.spinscale.dropwizard.jobs.annotations.DelayStart;
import de.spinscale.dropwizard.jobs.annotations.Every;

import java.util.concurrent.CountDownLatch;

@DelayStart("2s")
@Every("50ms")
public class EveryTestJobWithDelay extends Job {

    static final CountDownLatch latch = new CountDownLatch(5);

    @Override
    public void doJob() {
        latch.countDown();
    }
}

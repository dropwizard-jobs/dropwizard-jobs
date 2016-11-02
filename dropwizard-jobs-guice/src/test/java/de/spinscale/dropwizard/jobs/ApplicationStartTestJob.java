package de.spinscale.dropwizard.jobs;

import de.spinscale.dropwizard.jobs.annotations.OnApplicationStart;

import java.util.concurrent.CountDownLatch;

@OnApplicationStart
public class ApplicationStartTestJob extends Job {

    final CountDownLatch latch = new CountDownLatch(1);

    @Override
    public void doJob() {
        latch.countDown();
    }
}

package de.spinscale.dropwizard.jobs;

import de.spinscale.dropwizard.jobs.annotations.On;

import java.util.concurrent.CountDownLatch;

@On("0/1 * * * * ?")
public class OnTestJob extends Job {

    static final CountDownLatch latch = new CountDownLatch(2);

    @Override
    public void doJob() {
        latch.countDown();
    }
}

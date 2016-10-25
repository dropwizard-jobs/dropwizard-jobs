package de.spinscale.dropwizard.jobs;

import de.spinscale.dropwizard.jobs.annotations.OnApplicationStop;

import java.util.concurrent.CountDownLatch;

@OnApplicationStop
public class ApplicationStopTestJob extends Job {

    static final CountDownLatch latch = new CountDownLatch(1);

    @Override
    public void doJob() {
        latch.countDown();
    }

}

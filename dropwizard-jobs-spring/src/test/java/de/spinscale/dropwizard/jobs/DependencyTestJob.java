package de.spinscale.dropwizard.jobs;

import de.spinscale.dropwizard.jobs.annotations.Every;

import javax.inject.Inject;
import java.util.concurrent.CountDownLatch;

@Every("100ms")
public class DependencyTestJob extends Job {

    @Inject
    private Dependency dependency;

    static final CountDownLatch latch = new CountDownLatch(5);

    @Override
    public void doJob() {
        if (dependency == null)
            throw new IllegalStateException("dependency is null");
        latch.countDown();
    }
}

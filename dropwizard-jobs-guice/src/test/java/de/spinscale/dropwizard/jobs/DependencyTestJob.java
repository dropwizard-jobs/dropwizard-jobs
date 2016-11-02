package de.spinscale.dropwizard.jobs;

import com.google.inject.Inject;
import de.spinscale.dropwizard.jobs.annotations.Every;

import java.util.concurrent.CountDownLatch;

@Every("100ms")
public class DependencyTestJob extends Job {

    final CountDownLatch latch = new CountDownLatch(5);
    private Dependency dependency;

    @Inject
    public DependencyTestJob(Dependency dependency) {
        this.dependency = dependency;
    }

    @Override
    public void doJob() {
        if (dependency == null)
            throw new IllegalStateException("dependency is null");
        latch.countDown();
    }
}

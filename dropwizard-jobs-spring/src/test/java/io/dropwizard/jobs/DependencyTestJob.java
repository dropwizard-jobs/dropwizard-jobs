package io.dropwizard.jobs;

import io.dropwizard.jobs.Job;
import io.dropwizard.jobs.annotations.Every;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

import javax.inject.Inject;
import java.util.concurrent.CountDownLatch;

@Every("100ms")
public class DependencyTestJob extends Job {

    private Dependency dependency;
    final CountDownLatch latch = new CountDownLatch(5);

    @Inject
    public DependencyTestJob(Dependency dependency) {
        this.dependency = dependency;
    }

    @Override
    public void doJob(JobExecutionContext context) throws JobExecutionException {
        if (dependency == null)
            throw new IllegalStateException("dependency is null");
        latch.countDown();
    }
}

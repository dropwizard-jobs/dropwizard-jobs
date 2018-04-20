package io.dropwizard.jobs;

import com.google.inject.Inject;

import io.dropwizard.jobs.Job;
import io.dropwizard.jobs.annotations.Every;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

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
    public void doJob(JobExecutionContext context) throws JobExecutionException {
        if (dependency == null)
            throw new IllegalStateException("dependency is null");
        latch.countDown();
    }
}

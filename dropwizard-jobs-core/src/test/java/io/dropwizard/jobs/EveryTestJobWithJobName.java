package io.dropwizard.jobs;

import io.dropwizard.jobs.annotations.Every;

@Every(value = "10ms", jobName = "FooJob")
public class EveryTestJobWithJobName extends AbstractJob {

    public EveryTestJobWithJobName() {
        super(5);
    }
}

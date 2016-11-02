package de.spinscale.dropwizard.jobs;

import de.spinscale.dropwizard.jobs.annotations.Every;

@Every(value = "10ms", jobName = "FooJob")
public class EveryTestJobWithJobName extends AbstractJob {

    public EveryTestJobWithJobName() {
        super(5);
    }
}

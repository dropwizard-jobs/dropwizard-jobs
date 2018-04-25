package io.dropwizard.jobs;

import io.dropwizard.jobs.annotations.Every;

@Every("10ms")
public class EveryTestJob extends AbstractJob {

    public EveryTestJob() {
        super(5);
    }
}

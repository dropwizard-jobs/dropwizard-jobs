package io.dropwizard.jobs;

import io.dropwizard.jobs.annotations.DelayStart;
import io.dropwizard.jobs.annotations.Every;

@DelayStart("1s")
@Every("10ms")
public class EveryTestJobWithDelay extends AbstractJob {

    public EveryTestJobWithDelay() {
        super(5);
    }
}

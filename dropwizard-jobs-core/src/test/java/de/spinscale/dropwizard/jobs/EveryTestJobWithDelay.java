package de.spinscale.dropwizard.jobs;

import de.spinscale.dropwizard.jobs.annotations.DelayStart;
import de.spinscale.dropwizard.jobs.annotations.Every;

@DelayStart("1s")
@Every("10ms")
public class EveryTestJobWithDelay extends AbstractJob {

    public EveryTestJobWithDelay() {
        super(5);
    }
}

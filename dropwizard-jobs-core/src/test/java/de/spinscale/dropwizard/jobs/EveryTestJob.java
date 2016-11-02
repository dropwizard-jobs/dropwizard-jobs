package de.spinscale.dropwizard.jobs;

import de.spinscale.dropwizard.jobs.annotations.Every;

@Every("10ms")
public class EveryTestJob extends AbstractJob {

    public EveryTestJob() {
        super(5);
    }
}

package de.spinscale.dropwizard.jobs;

import de.spinscale.dropwizard.jobs.annotations.Every;

@Every("${testJob}")
public class EveryTestJobAlternativeConfiguration extends AbstractJob {

    public EveryTestJobAlternativeConfiguration() {
        super(5);
    }
}

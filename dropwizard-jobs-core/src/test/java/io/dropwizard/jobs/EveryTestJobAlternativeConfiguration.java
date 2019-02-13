package io.dropwizard.jobs;

import io.dropwizard.jobs.annotations.Every;

@Every("${testJob}")
public class EveryTestJobAlternativeConfiguration extends AbstractJob {

    public EveryTestJobAlternativeConfiguration() {
        super(5);
    }
}

package de.spinscale.dropwizard.jobs;

import de.spinscale.dropwizard.jobs.annotations.On;

@On("${onTestJob}")
public class OnTestJobAlternativeConfiguration extends AbstractJob {

    public OnTestJobAlternativeConfiguration() {
        super(1);
    }

}

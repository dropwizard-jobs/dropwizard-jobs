package io.dropwizard.jobs;

import io.dropwizard.jobs.annotations.On;

@On("${onTestJob}")
public class OnTestJobAlternativeConfiguration extends AbstractJob {

    public OnTestJobAlternativeConfiguration() {
        super(1);
    }

}

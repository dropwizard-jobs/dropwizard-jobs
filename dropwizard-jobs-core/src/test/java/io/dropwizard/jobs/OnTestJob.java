package io.dropwizard.jobs;

import io.dropwizard.jobs.annotations.On;

@On("0/1 * * * * ?")
public class OnTestJob extends AbstractJob {

    public OnTestJob() {
        super(1);
    }
}

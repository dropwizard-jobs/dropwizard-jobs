package io.dropwizard.jobs;

import io.dropwizard.jobs.annotations.On;

@On(value = "0/1 * * * * ?", jobName = "BarJob")
public class OnTestJobWithJobName extends AbstractJob {

    public OnTestJobWithJobName() {
        super(1);
    }
}

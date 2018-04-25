package io.dropwizard.jobs;

import io.dropwizard.jobs.annotations.OnApplicationStart;

@OnApplicationStart
public class ApplicationStartTestJob extends AbstractJob {

    public ApplicationStartTestJob() {
        super(1);
    }
}

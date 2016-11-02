package de.spinscale.dropwizard.jobs;

import de.spinscale.dropwizard.jobs.annotations.OnApplicationStart;

@OnApplicationStart
public class ApplicationStartTestJob extends AbstractJob {

    public ApplicationStartTestJob() {
        super(1);
    }
}

package de.spinscale.dropwizard.jobs;

import de.spinscale.dropwizard.jobs.annotations.OnApplicationStop;

@OnApplicationStop
public class ApplicationStopTestJob extends AbstractJob {

    public ApplicationStopTestJob() {
        super(1);
    }

    @Override
    public void doJob() {
        super.doJob();
    }
}

package de.spinscale.dropwizard.jobs;

import de.spinscale.dropwizard.jobs.annotations.On;

@On(value = "0/1 * * * * ?", jobName = "BarJob")
public class OnTestJobWithJobName extends AbstractJob {

    public OnTestJobWithJobName() {
        super(1);
    }
}

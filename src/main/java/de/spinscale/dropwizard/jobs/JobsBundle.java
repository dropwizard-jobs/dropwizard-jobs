package de.spinscale.dropwizard.jobs;

import com.yammer.dropwizard.Bundle;
import com.yammer.dropwizard.config.Environment;

public class JobsBundle extends Bundle {

    @Override
    public void run(Environment environment) {
        environment.manage(new JobManager());
    }

}

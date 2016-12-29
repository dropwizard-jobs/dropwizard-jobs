package de.spinscale.dropwizard.jobs;

import com.google.inject.Injector;

import io.dropwizard.setup.Environment;

public class GuiceJobsBundle extends JobsBundle {

    public GuiceJobsBundle(Injector injector) {
        jobManager = new GuiceJobManager(injector);
    }

    @Override
    public void run(JobConfiguration configuration, Environment environment) throws Exception {
        jobManager.configure(configuration);
        environment.lifecycle().manage(jobManager);
    }

}

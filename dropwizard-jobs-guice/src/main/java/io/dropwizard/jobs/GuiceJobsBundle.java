package io.dropwizard.jobs;

import com.google.inject.Injector;

import io.dropwizard.jobs.JobConfiguration;
import io.dropwizard.jobs.JobsBundle;
import io.dropwizard.setup.Environment;

public class GuiceJobsBundle extends JobsBundle {
    
    private Injector injector;

    public GuiceJobsBundle(Injector injector) {
        this.injector = injector;
        
    }

    @Override
    public void run(JobConfiguration configuration, Environment environment) throws Exception {
        jobManager = new GuiceJobManager(configuration, injector);
        environment.lifecycle().manage(jobManager);
    }

}

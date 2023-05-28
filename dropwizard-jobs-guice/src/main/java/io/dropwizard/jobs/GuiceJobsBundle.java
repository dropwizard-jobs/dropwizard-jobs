package io.dropwizard.jobs;

import com.google.inject.Injector;
import io.dropwizard.core.setup.Environment;

import java.util.ArrayList;

public class GuiceJobsBundle extends JobsBundle {
    
    private final Injector injector;

    public GuiceJobsBundle(Injector injector) {
        super(new ArrayList<>());
        this.injector = injector;
        
    }

    @Override
    public void run(JobConfiguration configuration, Environment environment) throws Exception {
        jobManager = new GuiceJobManager(configuration, injector);
        environment.lifecycle().manage(jobManager);
    }

}

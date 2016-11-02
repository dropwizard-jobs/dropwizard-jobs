package de.spinscale.dropwizard.jobs;

import com.google.inject.Injector;
import io.dropwizard.setup.Environment;

public class GuiceJobsBundle extends JobsBundle {

    private GuiceJobManager guiceJobsManager;
    
    public GuiceJobsBundle(Injector injector) {
        guiceJobsManager = new GuiceJobManager(injector);
    }

    @Override
    public void run(JobConfiguration configuration, Environment environment) throws Exception {
        guiceJobsManager.configure(configuration);
        environment.lifecycle().manage(guiceJobsManager);
    }
}

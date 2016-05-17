package de.spinscale.dropwizard.jobs;

import com.google.inject.Injector;
import de.spinscale.dropwizard.jobs.JobsBundle;
import io.dropwizard.Configuration;
import io.dropwizard.setup.Environment;

public class GuiceJobsBundle extends JobsBundle {
    Injector injector;
    private GuiceJobManager guiceJobsManager;
    
    public GuiceJobsBundle(Injector injector) {
        this("", injector);
    }

    public GuiceJobsBundle(String scanUrl, Injector injector) {
        super(scanUrl);
        this.injector = injector;
        guiceJobsManager = new GuiceJobManager(scanURL, injector);
    }

    @Override
    public void run(Environment environment) {
        environment.lifecycle().manage(guiceJobsManager);
    }
    
    public void configure(Configuration config) {
    	guiceJobsManager.configure(config);
    }

}

package de.spinscale.dropwizard.jobs;

import com.google.inject.Injector;
import de.spinscale.dropwizard.jobs.JobsBundle;
import io.dropwizard.setup.Environment;

public class GuiceJobsBundle extends JobsBundle {
    Injector injector;

    public GuiceJobsBundle(Injector injector) {
        this("", injector);
    }

    public GuiceJobsBundle(String scanUrl, Injector injector) {
        super(scanUrl);
        this.injector = injector;
    }

    @Override
    public void run(Environment environment) {
        environment.lifecycle().manage(new GuiceJobManager(scanURL, injector));
    }

}

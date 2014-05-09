package de.spinscale.dropwizard.jobs;

import org.springframework.context.ApplicationContext;

import de.spinscale.dropwizard.jobs.JobsBundle;
import io.dropwizard.setup.Environment;

public class SpringJobsBundle extends JobsBundle {

    private ApplicationContext context;

    public SpringJobsBundle(ApplicationContext context) {
        this("", context);
    }

    public SpringJobsBundle(String scanUrl, ApplicationContext context) {
        super(scanUrl);
        this.context = context;
    }

    @Override
    public void run(Environment environment) {
        environment.lifecycle().manage(new SpringJobManager(scanURL, context));
    }

}

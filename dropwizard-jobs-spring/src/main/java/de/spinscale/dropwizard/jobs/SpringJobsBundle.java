package de.spinscale.dropwizard.jobs;

import io.dropwizard.setup.Environment;
import org.springframework.context.ApplicationContext;

public class SpringJobsBundle extends JobsBundle {

    private ApplicationContext context;

    public SpringJobsBundle(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void run(JobConfiguration configuration, Environment environment) throws Exception {
        SpringJobManager springJobManager = new SpringJobManager(context);
        springJobManager.configure(configuration);
        environment.lifecycle().manage(springJobManager);
    }

}

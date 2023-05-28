package io.dropwizard.jobs;

import io.dropwizard.core.setup.Environment;
import org.springframework.context.ApplicationContext;

public class SpringJobsBundle extends JobsBundle {

    private final ApplicationContext context;

    public SpringJobsBundle(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void run(JobConfiguration configuration, Environment environment) throws Exception {
        jobManager = new SpringJobManager(configuration, context);
        environment.lifecycle().manage(jobManager);
    }

}

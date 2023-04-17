package io.dropwizard.jobs;

import org.springframework.context.ApplicationContext;

import io.dropwizard.core.setup.Environment;

public class SpringJobsBundle extends JobsBundle {

    private ApplicationContext context;

    public SpringJobsBundle(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void run(JobConfiguration configuration, Environment environment) throws Exception {
        jobManager = new SpringJobManager(configuration, context);
        environment.lifecycle().manage(jobManager);
    }

}

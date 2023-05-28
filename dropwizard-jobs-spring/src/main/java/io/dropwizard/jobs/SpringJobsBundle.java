package io.dropwizard.jobs;

import io.dropwizard.core.setup.Environment;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;

public class SpringJobsBundle extends JobsBundle {

    private final ApplicationContext context;

    public SpringJobsBundle(ApplicationContext context) {
        super(new ArrayList<>());
        this.context = context;
    }

    @Override
    public void run(JobConfiguration configuration, Environment environment) {
        jobManager = new SpringJobManager(configuration, context);
        environment.lifecycle().manage(jobManager);
    }

}

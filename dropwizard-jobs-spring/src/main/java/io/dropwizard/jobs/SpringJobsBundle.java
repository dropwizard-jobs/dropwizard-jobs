package io.dropwizard.jobs;

import io.dropwizard.core.setup.Environment;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;

/**
 * A Dropwizard bundle that integrates the jobs library with Spring for dependency injection.
 * <p>
 * This bundle discovers jobs from the Spring application context and registers them with
 * the Quartz scheduler. Jobs are managed by {@link SpringJobManager} and created by
 * {@link SpringJobFactory}.
 * </p>
 *
 * @see SpringJobManager
 * @see SpringJobFactory
 */
public class SpringJobsBundle extends JobsBundle {

    private final ApplicationContext context;

    /**
     * Creates a new SpringJobsBundle with the specified application context.
     *
     * @param context the Spring application context used to discover and manage jobs
     */
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

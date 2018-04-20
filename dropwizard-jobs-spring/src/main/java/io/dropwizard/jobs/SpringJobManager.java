package io.dropwizard.jobs;

import org.quartz.spi.JobFactory;
import org.springframework.context.ApplicationContext;

import io.dropwizard.jobs.Job;
import io.dropwizard.jobs.JobConfiguration;
import io.dropwizard.jobs.JobManager;

public class SpringJobManager extends JobManager {

    protected SpringJobFactory jobFactory;

    public SpringJobManager(JobConfiguration config, ApplicationContext context) {
        super(config, context.getBeansOfType(Job.class).values().toArray(new Job[] {}));
        jobFactory = new SpringJobFactory(context);
    }

    @Override
    protected JobFactory getJobFactory() {
        return jobFactory;
    }
}

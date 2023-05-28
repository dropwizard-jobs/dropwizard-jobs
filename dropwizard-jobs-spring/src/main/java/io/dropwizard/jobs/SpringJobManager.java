package io.dropwizard.jobs;

import org.quartz.spi.JobFactory;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;

public class SpringJobManager extends JobManager {

    protected SpringJobFactory jobFactory;

    public SpringJobManager(JobConfiguration config, ApplicationContext context) {
        super(config, new ArrayList<>(context.getBeansOfType(Job.class).values()));
        jobFactory = new SpringJobFactory(context);
    }

    @Override
    protected JobFactory getJobFactory() {
        return jobFactory;
    }
}

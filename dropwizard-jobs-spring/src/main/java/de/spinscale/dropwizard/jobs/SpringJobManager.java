package de.spinscale.dropwizard.jobs;

import org.quartz.spi.JobFactory;
import org.springframework.context.ApplicationContext;

public class SpringJobManager extends JobManager {

    protected SpringJobFactory jobFactory;

    public SpringJobManager(ApplicationContext context) {
        super(context.getBeansOfType(Job.class).values().toArray(new Job[] {}));
        jobFactory = new SpringJobFactory(context);
    }

    @Override
    protected JobFactory getJobFactory() {
        return jobFactory;
    }
}

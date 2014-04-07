package de.spinscale.dropwizard.jobs;

import org.quartz.impl.StdSchedulerFactory;
import org.reflections.Reflections;
import org.springframework.context.ApplicationContext;

public class SpringJobManager extends JobManager {

    protected SpringJobFactory jobFactory;

    public SpringJobManager(String scanUrl, ApplicationContext context) {
        reflections = new Reflections(scanUrl);
        jobFactory = new SpringJobFactory(context);
    }

    public SpringJobManager(ApplicationContext context) {
        this("", context);
    }

    @Override
    public void start() throws Exception {
        scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.setJobFactory(jobFactory);
        scheduler.start();

        scheduleAllJobs();
    }

}

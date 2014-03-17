package de.spinscale.dropwizard.jobs.guice;

import com.google.inject.Injector;
import de.spinscale.dropwizard.jobs.JobManager;
import org.quartz.impl.StdSchedulerFactory;
import org.reflections.Reflections;

public class GuiceJobManager extends JobManager {

    protected GuiceJobFactory jobFactory;

    public GuiceJobManager(String scanUrl, Injector injector) {
        reflections = new Reflections(scanUrl);
        jobFactory = new GuiceJobFactory(injector);
    }

    @Override
    public void start() throws Exception {
        scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.setJobFactory(jobFactory);
        scheduler.start();

        scheduleAllJobsOnApplicationStart();
        scheduleAllJobsWithEveryAnnotation();
        scheduleAllJobsWithOnAnnotation();
    }

}

package de.spinscale.dropwizard.jobs;

import org.quartz.impl.StdSchedulerFactory;
import org.reflections.Reflections;

import com.google.inject.Injector;

public class GuiceJobManager extends JobManager {

    protected GuiceJobFactory jobFactory;

    public GuiceJobManager(String scanUrl, Injector injector) {
        reflections = new Reflections(scanUrl);
        jobFactory = new GuiceJobFactory(injector);
    }

    public GuiceJobManager(Injector injector) {
        this("", injector);
    }

    @Override
    public void start() throws Exception {
        scheduler = StdSchedulerFactory.getDefaultScheduler();
        scheduler.setJobFactory(jobFactory);
        scheduler.start();

        scheduleAllJobs();
    }

}

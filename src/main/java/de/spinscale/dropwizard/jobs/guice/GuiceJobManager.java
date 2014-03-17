package de.spinscale.dropwizard.jobs.guice;

import com.google.inject.Injector;
import de.spinscale.dropwizard.jobs.JobManager;
import org.quartz.Scheduler;
import org.quartz.impl.StdSchedulerFactory;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GuiceJobManager extends JobManager {

    private static final Logger log = LoggerFactory.getLogger(GuiceJobManager.class);
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

    @Override
    public void stop() throws Exception {
        scheduleAllJobsOnApplicationStop();

        // this is enough to put the job into the queue, otherwise the jobs wont be executed
        // anyone got a better solution?
        Thread.sleep(100);

        scheduler.shutdown(true);
    }

}

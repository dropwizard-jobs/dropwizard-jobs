package de.spinscale.dropwizard.jobs;

import com.google.common.collect.Sets;
import com.yammer.dropwizard.lifecycle.Managed;
import de.spinscale.dropwizard.jobs.annotations.Every;
import de.spinscale.dropwizard.jobs.annotations.On;
import de.spinscale.dropwizard.jobs.annotations.OnApplicationStart;
import de.spinscale.dropwizard.jobs.annotations.OnApplicationStop;
import de.spinscale.dropwizard.jobs.parser.TimeParserUtil;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class JobManager implements Managed {

    private static final Logger log = LoggerFactory.getLogger(JobManager.class);
    private Reflections reflections = null;
    protected Scheduler scheduler;

    public JobManager() {
        this("");
    }
    
    public JobManager(String scanUrl) {
    	reflections = new Reflections(scanUrl);
    }
    
    @Override
    public void start() throws Exception {
        scheduler = StdSchedulerFactory.getDefaultScheduler();
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

    private void scheduleAllJobsOnApplicationStop() throws SchedulerException {
        List<Class<? extends Job>> stopJobClasses = getJobClasses(OnApplicationStop.class);
        for (Class<? extends Job> clazz : stopJobClasses) {
            JobBuilder jobDetail = JobBuilder.newJob(clazz);
            scheduler.scheduleJob(jobDetail.build(), executeNowTrigger());
        }
    }

    private List<Class<? extends Job>> getJobClasses(Class annotation) {
        Set<Class<? extends Job>> jobs = (Set<Class<? extends Job>>) reflections.getSubTypesOf(Job.class);
        Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(annotation);

        return Sets.intersection(new HashSet<Class<? extends Job>>(jobs), annotatedClasses).immutableCopy().asList();
    }

    private void scheduleAllJobsWithOnAnnotation() throws SchedulerException {
        List<Class<? extends Job>> onJobClasses = getJobClasses(On.class);
        log.info("Jobs with @On annotation: " + onJobClasses);

        for (Class<? extends org.quartz.Job> clazz : onJobClasses) {
            On annotation = clazz.getAnnotation(On.class);

            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(annotation.value());
            Trigger trigger = TriggerBuilder.newTrigger().withSchedule(scheduleBuilder).build();
            JobBuilder jobBuilder = JobBuilder.newJob(clazz);
            scheduler.scheduleJob(jobBuilder.build(), trigger);
        }
    }

    private void scheduleAllJobsWithEveryAnnotation() throws SchedulerException {
        List<Class<? extends Job>> everyJobClasses = getJobClasses(Every.class);
        log.info("Jobs with @Every annotation: " + everyJobClasses);

        for (Class<? extends org.quartz.Job> clazz : everyJobClasses) {
            Every annotation = clazz.getAnnotation(Every.class);
            int secondDelay = TimeParserUtil.parseDuration(annotation.value());
            SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(secondDelay).repeatForever();
            Trigger trigger = TriggerBuilder.newTrigger().withSchedule(scheduleBuilder).build();

            JobBuilder jobBuilder = JobBuilder.newJob(clazz);
            scheduler.scheduleJob(jobBuilder.build(), trigger);
        }
    }

    private void scheduleAllJobsOnApplicationStart() throws SchedulerException {
        List<Class<? extends Job>> startJobClasses = getJobClasses(OnApplicationStart.class);
        log.info("Jobs to run on application start: " + startJobClasses);
        for (Class<? extends org.quartz.Job> clazz : startJobClasses) {
            JobBuilder jobBuilder = JobBuilder.newJob(clazz);
            scheduler.scheduleJob(jobBuilder.build(), executeNowTrigger());
        }
    }

    private Trigger executeNowTrigger() {
        return  TriggerBuilder.newTrigger().startNow().build();
    }
}

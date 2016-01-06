package de.spinscale.dropwizard.jobs;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.joda.time.DateTime;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import de.spinscale.dropwizard.jobs.annotations.DelayStart;
import de.spinscale.dropwizard.jobs.annotations.Every;
import de.spinscale.dropwizard.jobs.annotations.On;
import de.spinscale.dropwizard.jobs.annotations.OnApplicationStart;
import de.spinscale.dropwizard.jobs.annotations.OnApplicationStop;
import de.spinscale.dropwizard.jobs.parser.TimeParserUtil;
import io.dropwizard.lifecycle.Managed;

public class JobManager implements Managed {

    private static final Logger log = LoggerFactory.getLogger(JobManager.class);
    protected Reflections reflections = null;
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
        scheduleAllJobs();
        logAllOnApplicationStopJobs();
    }

    @Override
    public void stop() throws Exception {
        scheduleAllJobsOnApplicationStop();

        // this is enough to put the job into the queue, otherwise the jobs wont
        // be executed
        // anyone got a better solution?
        Thread.sleep(100);

        scheduler.shutdown(true);
    }

    protected void scheduleAllJobs() throws SchedulerException {
        scheduleAllJobsOnApplicationStart();
        scheduleAllJobsWithEveryAnnotation();
        scheduleAllJobsWithOnAnnotation();
    }

    protected void scheduleAllJobsOnApplicationStop() throws SchedulerException {
        List<Class<? extends Job>> stopJobClasses = getJobClasses(OnApplicationStop.class);
        for (Class<? extends Job> clazz : stopJobClasses) {
            JobBuilder jobDetail = JobBuilder.newJob(clazz);
            scheduler.scheduleJob(jobDetail.build(), executeNowTrigger());
        }
    }

    protected List<Class<? extends Job>> getJobClasses(Class<? extends Annotation> annotation) {
        Set<Class<? extends Job>> jobs = reflections.getSubTypesOf(Job.class);
        Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(annotation);

        return Sets.intersection(new HashSet<Class<? extends Job>>(jobs), annotatedClasses).immutableCopy().asList();
    }

    protected void scheduleAllJobsWithOnAnnotation() throws SchedulerException {
        List<Class<? extends Job>> onJobClasses = getJobClasses(On.class);

        log.info("Jobs with @On annotation:");
        if (onJobClasses.isEmpty()) {
            log.info("    NONE");
        } else {
            for (Class<? extends org.quartz.Job> clazz : onJobClasses) {
                On annotation = clazz.getAnnotation(On.class);
                String cron = annotation.value();
                CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cron);
                Trigger trigger = TriggerBuilder.newTrigger().withSchedule(scheduleBuilder).build();
                JobBuilder jobBuilder = JobBuilder.newJob(clazz);
                scheduler.scheduleJob(jobBuilder.build(), trigger);

                log.info(String.format("    %-21s %s", cron, clazz.getCanonicalName()));
            }
        }
    }

    protected void scheduleAllJobsWithEveryAnnotation() throws SchedulerException {
        List<Class<? extends Job>> everyJobClasses = getJobClasses(Every.class);

        log.info("Jobs with @Every annotation:");
        if (everyJobClasses.isEmpty()) {
            log.info("    NONE");
        } else {
            for (Class<? extends org.quartz.Job> clazz : everyJobClasses) {
                Every everyAnnotation = clazz.getAnnotation(Every.class);
                int secondInterval = TimeParserUtil.parseDuration(everyAnnotation.value());
                SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInSeconds(secondInterval).repeatForever();

                DateTime start = new DateTime();
                DelayStart delayAnnotation = clazz.getAnnotation(DelayStart.class);
                if (delayAnnotation != null) {
                    int secondDelay = TimeParserUtil.parseDuration(delayAnnotation.value());
                    start = start.plusSeconds(secondDelay);
                }

                Trigger trigger = TriggerBuilder.newTrigger().withSchedule(scheduleBuilder)
                        .startAt(start.toDate())
                        .build();
                JobBuilder jobBuilder = JobBuilder.newJob(clazz);
                scheduler.scheduleJob(jobBuilder.build(), trigger);

                String logMessage = String.format("    %-7s %s", everyAnnotation.value(), clazz.getCanonicalName());
                if (delayAnnotation != null) {
                    logMessage += " (" + delayAnnotation.value() + " delay)";
                }
                log.info(logMessage);
            }
        }
    }

    protected void scheduleAllJobsOnApplicationStart() throws SchedulerException {
        List<Class<? extends Job>> startJobClasses = getJobClasses(OnApplicationStart.class);

        log.info("Jobs to run on application start:");
        if (startJobClasses.isEmpty()) {
            log.info("    NONE");
        } else {
            for (Class<? extends org.quartz.Job> clazz : startJobClasses) {

                JobBuilder jobBuilder = JobBuilder.newJob(clazz);
                scheduler.scheduleJob(jobBuilder.build(), executeNowTrigger());

                log.info("   " + clazz.getCanonicalName());
            }
        }
    }

    protected Trigger executeNowTrigger() {
        return TriggerBuilder.newTrigger().startNow().build();
    }

    private void logAllOnApplicationStopJobs() throws SchedulerException {
        List<Class<? extends Job>> stopJobClasses = getJobClasses(OnApplicationStop.class);
        log.info("Jobs to run on application stop:");
        if (stopJobClasses.isEmpty()) {
            log.info("    NONE");
        } else {
            for (Class<? extends Job> clazz : stopJobClasses) {
                log.info("   " + clazz.getCanonicalName());
            }
        }
    }
}

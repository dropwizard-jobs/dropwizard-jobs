package de.spinscale.dropwizard.jobs;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobKey;
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
import io.dropwizard.Configuration;
import io.dropwizard.lifecycle.Managed;

public class JobManager implements Managed {

    private static final Logger log = LoggerFactory.getLogger(JobManager.class);
    protected Reflections reflections = null;
    protected Scheduler scheduler;
    private Configuration config;

    public JobManager() {
        this("");
    }

    public JobManager(String scanUrl) {
        reflections = new Reflections(scanUrl);
    }

    public void configure(Configuration config) {
        this.config = config;
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
                On onAnnotation = clazz.getAnnotation(On.class);
                String cron = onAnnotation.value();
                CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(cron);
                Trigger trigger = TriggerBuilder.newTrigger().withSchedule(scheduleBuilder).build();

                // ensure that only one instance of each job is scheduled
                JobKey jobKey = JobKey.jobKey(StringUtils.isNotBlank(onAnnotation.jobName()) ? onAnnotation.jobName() : clazz
                        .getCanonicalName());
                createOrUpdateJob(jobKey, clazz, trigger);
                log.info(String.format("    %-21s %s", cron, jobKey.toString()));
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

                String value = everyAnnotation.value();
                if (value.isEmpty() || value.matches("\\$\\{.*\\}")) {
                    value = readDurationFromConfig(everyAnnotation, clazz);
                    log.info(clazz + " is configured in the config file to run every " + value);
                }
                long milliSeconds = TimeParserUtil.parseDuration(value);
                SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                        .withIntervalInMilliseconds(milliSeconds).repeatForever();

                DateTime start = new DateTime();
                DelayStart delayAnnotation = clazz.getAnnotation(DelayStart.class);
                if (delayAnnotation != null) {
                    long milliSecondDelay = TimeParserUtil.parseDuration(delayAnnotation.value());
                    start = start.plus(Duration.millis(milliSecondDelay));
                }

                Trigger trigger = TriggerBuilder.newTrigger().withSchedule(scheduleBuilder)
                        .startAt(start.toDate())
                        .build();

                // ensure that only one instance of each job is scheduled
                JobKey jobKey = JobKey.jobKey(StringUtils.isNotBlank(everyAnnotation.jobName()) ? everyAnnotation.jobName() : clazz
                        .getCanonicalName());
                createOrUpdateJob(jobKey, clazz, trigger);

                String logMessage = String.format("    %-7s %s", everyAnnotation.value(), jobKey.toString());
                if (delayAnnotation != null) {
                    logMessage += " (" + delayAnnotation.value() + " delay)";
                }
                log.info(logMessage);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private String readDurationFromConfig(Every annotation, Class<? extends org.quartz.Job> clazz) {
        if (config == null) {
            return null;
        }
        try {
            String property = WordUtils.uncapitalize(clazz.getSimpleName());
            if (!annotation.value().isEmpty()) {
                property = annotation.value().substring(2, annotation.value().length() - 1);
            }
            Method m = config.getClass().getMethod("getJobs");
            Map<String, String> jobConfig = (Map<String, String>) m.invoke(config);
            if (jobConfig != null && jobConfig.containsKey(property)) {
                return jobConfig.get(property);
            }
        } catch (NoSuchMethodException | SecurityException | InvocationTargetException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return null;
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

    private void logAllOnApplicationStopJobs() {
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

    private void createOrUpdateJob(JobKey jobKey, Class<? extends org.quartz.Job> clazz, Trigger trigger) throws SchedulerException {
        JobBuilder jobBuilder = JobBuilder.newJob(clazz).withIdentity(jobKey);
        if (!scheduler.checkExists(jobKey)) {
            // if the job doesn't already exist, we can create it, along with its trigger. this prevents us
            // from creating multiple instances of the same job when running in a clustered environment
            scheduler.scheduleJob(jobBuilder.build(), trigger);
            log.error("SCHEDULED JOB WITH KEY " + jobKey.toString());
        } else {
            // if the job has exactly one trigger, we can just reschedule it, which allows us to update the schedule for
            // that trigger.
            List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
            if (triggers.size() == 1) {
                scheduler.rescheduleJob(triggers.get(0).getKey(), trigger);
                return;
            }

            // if for some reason the job has multiple triggers, it's easiest to just delete and re-create the job,
            // since we want to enforce a one-to-one relationship between jobs and triggers
            scheduler.deleteJob(jobKey);
            scheduler.scheduleJob(jobBuilder.build(), trigger);
        }
    }
}

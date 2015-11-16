package de.spinscale.dropwizard.jobs;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.LinkedList;
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

        List<OnJobLogLine> logLines = new LinkedList<>();
        for (Class<? extends org.quartz.Job> clazz : onJobClasses) {
            On annotation = clazz.getAnnotation(On.class);

            logLines.add(new OnJobLogLine(clazz, annotation));

            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(annotation.value());
            Trigger trigger = TriggerBuilder.newTrigger().withSchedule(scheduleBuilder).build();
            JobBuilder jobBuilder = JobBuilder.newJob(clazz);
            scheduler.scheduleJob(jobBuilder.build(), trigger);
        }

        logJobs("Jobs with @On annotation:", logLines);
    }

    protected void scheduleAllJobsWithEveryAnnotation() throws SchedulerException {
        List<Class<? extends Job>> everyJobClasses = getJobClasses(Every.class);

        List<EveryJobLogLine> logLines = new LinkedList<>();
        for (Class<? extends org.quartz.Job> clazz : everyJobClasses) {
            Every annotation = clazz.getAnnotation(Every.class);
            int secondInterval = TimeParserUtil.parseDuration(annotation.value());
            SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                    .withIntervalInSeconds(secondInterval).repeatForever();

            DateTime start = new DateTime();
            DelayStart delayAnnotation = clazz.getAnnotation(DelayStart.class);
            if (delayAnnotation != null) {
                int secondDelay = TimeParserUtil.parseDuration(delayAnnotation.value());
                start = start.plusSeconds(secondDelay);
            }

            logLines.add(new EveryJobLogLine(clazz, annotation, delayAnnotation));

            Trigger trigger = TriggerBuilder.newTrigger().withSchedule(scheduleBuilder)
            		.startAt(start.toDate())
            		.build();
            JobBuilder jobBuilder = JobBuilder.newJob(clazz);
            scheduler.scheduleJob(jobBuilder.build(), trigger);
        }

        logJobs("Jobs with @Every annotation:", logLines);
    }

    protected void scheduleAllJobsOnApplicationStart() throws SchedulerException {
        List<Class<? extends Job>> startJobClasses = getJobClasses(OnApplicationStart.class);

        List<JobLogLine> logLines = new LinkedList<>();
        for (Class<? extends org.quartz.Job> clazz : startJobClasses) {
            logLines.add(new OnApplicationStartLogLine(clazz));

            JobBuilder jobBuilder = JobBuilder.newJob(clazz);
            scheduler.scheduleJob(jobBuilder.build(), executeNowTrigger());
        }

        logJobs("Jobs to run on application start:", logLines);
    }

    protected Trigger executeNowTrigger() {
        return TriggerBuilder.newTrigger().startNow().build();
    }

    private void logJobs(String title, List<? extends JobLogLine> logLines) {
        StringBuilder message = new StringBuilder(512);
        message.append(title).append('\n').append('\n');
        if (logLines.isEmpty()) {
            message.append("    NONE").append('\n');
        } else {
            for (JobLogLine line : logLines) {
                message.append("    ").append(line.toString()).append('\n');
            }
        }

        log.info(message.toString());
    }

    private interface JobLogLine {
        String toString();
    }

    private static class OnApplicationStartLogLine implements JobLogLine {
        private final Class<? extends org.quartz.Job> clazz;

        public OnApplicationStartLogLine(Class<? extends org.quartz.Job> clazz) {
            this.clazz = clazz;
        }

        @Override
        public String toString() {
            return clazz.getCanonicalName();
        }
    }

    private static class OnJobLogLine implements JobLogLine {
        private final Class<? extends org.quartz.Job> clazz;
        private final On on;

        public OnJobLogLine(Class<? extends org.quartz.Job> clazz, On on) {
            this.clazz = clazz;
            this.on = on;
        }

        @Override
        public String toString() {
            final String cron = on.value();
            return String.format("%-21s %s", cron, clazz.getCanonicalName());
        }
    }

    private static class EveryJobLogLine implements JobLogLine {
        private final Class<? extends org.quartz.Job> clazz;
        private final Every every;
        private final DelayStart delayStart;

        public EveryJobLogLine(Class<? extends org.quartz.Job> clazz, Every every, DelayStart delayStart) {
            this.clazz = clazz;
            this.every = every;
            this.delayStart = delayStart;
        }

        @Override
        public String toString() {
            final StringBuilder msg = new StringBuilder(128);
            msg.append(String.format("%-7s %s", every.value(), clazz.getCanonicalName()));
            if (delayStart != null) {
                msg.append(String.format(" (%s delay)", delayStart.value()));
            }
            return msg.toString();
        }
    }
}

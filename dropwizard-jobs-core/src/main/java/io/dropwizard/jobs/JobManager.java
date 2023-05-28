package io.dropwizard.jobs;

import io.dropwizard.jobs.scheduler.EveryScheduler;
import io.dropwizard.jobs.scheduler.OnApplicationStartScheduler;
import io.dropwizard.jobs.scheduler.OnApplicationStopScheduler;
import io.dropwizard.jobs.scheduler.OnCronScheduler;
import io.dropwizard.lifecycle.Managed;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.spi.JobFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Properties;
import java.util.Set;

public class JobManager implements Managed, JobMediator {

    protected static final Logger log = LoggerFactory.getLogger(JobManager.class);
    protected final JobConfiguration configuration;
    protected final JobFilters jobs;

    private final OnApplicationStartScheduler onApplicationStartScheduler;
    private final OnApplicationStopScheduler onApplicationStopScheduler;
    private final EveryScheduler everyScheduler;
    private final OnCronScheduler onCronScheduler;

    protected Scheduler scheduler;


    public JobManager(JobConfiguration configuration, Job... jobs) {
        this.configuration = configuration;
        this.jobs = new JobFilters(jobs);

        this.onApplicationStartScheduler = new OnApplicationStartScheduler(this);
        this.onApplicationStopScheduler = new OnApplicationStopScheduler(this);
        this.everyScheduler = new EveryScheduler(this);
        this.onCronScheduler = new OnCronScheduler(this);

    }

    private static JobDetail build(ScheduledJob job) {
        JobKey jobKey = job.getJobKey();
        return JobBuilder.newJob(job.getClazz())
                .withIdentity(jobKey)
                .requestRecovery(job.isRequestsRecovery())
                .storeDurably(job.isStoreDurably())
                .build();
    }

    @Override
    public void start() throws Exception {
        scheduler = createScheduler();
        scheduler.setJobFactory(getJobFactory());
        scheduler.start();

        onApplicationStartScheduler.schedule();
        everyScheduler.schedule();
        onCronScheduler.schedule();

        logAllOnApplicationStopJobs();
    }

    private Scheduler createScheduler() throws SchedulerException {
        if (configuration.getQuartzConfiguration().isEmpty()) {
            return StdSchedulerFactory.getDefaultScheduler();
        }

        StdSchedulerFactory factory = new StdSchedulerFactory();
        factory.initialize(createProperties());
        return factory.getScheduler();
    }

    @Override
    public void stop() throws Exception {
        onApplicationStopScheduler.schedule();

        // this is enough to put the job into the queue, otherwise the jobs wont
        // be executed, anyone got a better solution?
        Thread.sleep(100);

        scheduler.shutdown(true);
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public JobFilters getJobs() {
        return jobs;
    }

    @Override
    public JobConfiguration getConfiguration() {
        return configuration;
    }

    protected JobFactory getJobFactory() {
        return new DropwizardJobFactory(jobs);
    }

    public void scheduleNow(JobDetail jobDetail) throws SchedulerException {
        Trigger nowTrigger = nowTrigger();
        scheduler.scheduleJob(jobDetail, Set.of(nowTrigger), true);
    }

    protected Trigger nowTrigger() {
        return TriggerBuilder.newTrigger().startNow().build();
    }


    private Properties createProperties() {
        Properties properties = new Properties();
        properties.putAll(configuration.getQuartzConfiguration());
        return properties;
    }

    private void logAllOnApplicationStopJobs() {
        log.info("Jobs to run on application stop:");

        jobs.allOnApplicationStop()
                .map(Job::getClass)
                .forEach(clazz -> log.info("   " + clazz.getCanonicalName()));
    }

    public void scheduleOrRescheduleJob(ScheduledJob job) {
        JobKey jobKey = job.getJobKey();
        Trigger trigger = job.getTrigger();
        JobDetail jobDetail = build(job);

        try {
            if (scheduler.checkExists(jobKey)) {
                // if the job has exactly one trigger, we can just reschedule it, which allows us to update the schedule for
                // that trigger.
                List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
                if (triggers.size() == 1) {
                    scheduler.rescheduleJob(triggers.get(0).getKey(), trigger);
                    log.info(job.getMessage());
                } else {
                    // if for some reason the job has multiple triggers, it's easiest to just delete and re-create the job,
                    // since we want to enforce a one-to-one relationship between jobs and triggers
                    scheduler.deleteJob(jobKey);
                    scheduler.scheduleJob(jobDetail, trigger);
                    log.info(job.getMessage());
                }
            } else {
                // if the job doesn't already exist, we can create it, along with its trigger. this prevents us
                // from creating multiple instances of the same job when running in a clustered environment
                scheduler.scheduleJob(jobDetail, trigger);
                log.info("scheduled job with key {}", jobKey.toString());
                log.info(job.getMessage());
            }
        } catch (SchedulerException e) {
            log.error(String.format("error occurred scheduling the job %s", jobKey), e);
        }

    }
}

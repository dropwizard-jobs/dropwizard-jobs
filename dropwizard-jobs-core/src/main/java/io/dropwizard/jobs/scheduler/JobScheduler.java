package io.dropwizard.jobs.scheduler;

import io.dropwizard.jobs.Job;
import io.dropwizard.jobs.JobMediator;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class JobScheduler {
    final Logger log;

    final JobMediator mediator;

    public JobScheduler(JobMediator mediator) {
        this.mediator = mediator;
        this.log = LoggerFactory.getLogger(getClass());
    }

    public abstract void schedule() throws SchedulerException;

    protected JobDetail build(Job job) {
        Class<? extends Job> jobClass = job.getClass();
        String jobClassName = jobClass.getName();
        String jobGroupName = job.getGroupName();
        return JobBuilder
                .newJob(jobClass)
                .withIdentity(jobClassName, jobGroupName)
                .build();
    }


    protected JobKey createJobKey(final String jobName, final Job job) {
        String key = StringUtils.isNotBlank(jobName) ? jobName : job.getClass().getCanonicalName();
        return JobKey.jobKey(key, job.getGroupName());
    }

}

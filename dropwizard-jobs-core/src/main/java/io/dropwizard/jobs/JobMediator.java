package io.dropwizard.jobs;

import org.quartz.JobDetail;
import org.quartz.SchedulerException;

public interface JobMediator {
    JobFilters getJobs();

    JobConfiguration getConfiguration();

    void scheduleNow(JobDetail jobDetail) throws SchedulerException;

    void scheduleOrRescheduleJob(ScheduledJob scheduledJob);

}

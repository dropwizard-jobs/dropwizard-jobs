package io.dropwizard.jobs;

import java.util.Objects;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;
import org.quartz.utils.Key;

class DropwizardJobFactory implements JobFactory {

    private final Job[] jobs;

    DropwizardJobFactory(Job ... jobs) {
        this.jobs = jobs;
    }

    @Override
    public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
        JobDetail jobDetail = bundle.getJobDetail();
        JobKey jobKey = jobDetail.getKey();

        for (Job job : jobs) {
            if (job.getClass().equals(jobDetail.getJobClass()) && equalGroupName(job, jobKey)) {
                return job;
            }
        }
        return null;
    }

    private boolean equalGroupName(final Job job, final JobKey quartzJobKey) {
        return Key.DEFAULT_GROUP.equals(quartzJobKey.getGroup()) && job.getGroupName() == null
            || Objects.equals(job.getGroupName(), quartzJobKey.getGroup());
    }
}

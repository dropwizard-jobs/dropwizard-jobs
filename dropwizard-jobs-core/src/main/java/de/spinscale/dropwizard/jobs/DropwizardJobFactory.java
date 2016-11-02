package de.spinscale.dropwizard.jobs;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

class DropwizardJobFactory implements JobFactory {

    private final Job[] jobs;

    DropwizardJobFactory(Job ... jobs) {
        this.jobs = jobs;
    }

    @Override
    public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
        JobDetail jobDetail = bundle.getJobDetail();
        for (Job job : jobs) {
            if (job.getClass().equals(jobDetail.getJobClass())) {
                return job;
            }
        }
        return null;
    }
}

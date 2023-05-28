package io.dropwizard.jobs;

import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

class DropwizardJobFactory implements JobFactory {

    private final JobFilters jobs;

    DropwizardJobFactory(JobFilters jobs) {
        this.jobs = jobs;
    }

    @Override
    public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) {
        JobDetail jobDetail = bundle.getJobDetail();
        Class<? extends org.quartz.Job> jobClass = jobDetail.getJobClass();
        JobKey jobKey = jobDetail.getKey();
        String groupName = jobKey.getGroup();

        return jobs.findWith(jobClass, groupName).orElse(null);
    }

}

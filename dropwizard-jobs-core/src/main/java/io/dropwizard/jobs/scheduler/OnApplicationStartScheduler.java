package io.dropwizard.jobs.scheduler;

import io.dropwizard.jobs.JobMediator;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;

import java.util.List;
import java.util.stream.Collectors;

public class OnApplicationStartScheduler extends JobScheduler {
    public OnApplicationStartScheduler(JobMediator mediator) {
        super(mediator);
    }

    @Override
    public void schedule() throws SchedulerException {

        List<JobDetail> jobDetails = mediator.getJobs()
                .allOnApplicationStart()
                .map(this::build)
                .collect(Collectors.toList());

        if (!jobDetails.isEmpty()) {
            log.info("Jobs to run on application start:");
            for (JobDetail jobDetail : jobDetails) {
                mediator.scheduleNow(jobDetail);
                log.info("   " + jobDetail.getJobClass().getCanonicalName());
            }
        }
    }
}

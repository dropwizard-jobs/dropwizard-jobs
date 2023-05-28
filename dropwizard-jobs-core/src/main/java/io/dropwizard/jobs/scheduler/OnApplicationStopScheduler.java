package io.dropwizard.jobs.scheduler;

import io.dropwizard.jobs.JobMediator;
import org.quartz.JobDetail;
import org.quartz.SchedulerException;

import java.util.List;
import java.util.stream.Collectors;

public class OnApplicationStopScheduler extends JobScheduler {
    public OnApplicationStopScheduler(JobMediator mediator) {
        super(mediator);
    }

    @Override
    public void schedule() throws SchedulerException {

        List<JobDetail> jobDetails = mediator.getJobs()
                .allOnApplicationStop()
                .map(this::build)
                .collect(Collectors.toList());
        for (JobDetail jobDetail : jobDetails) {
            mediator.scheduleNow(jobDetail);
        }

    }
}

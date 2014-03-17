package de.spinscale.dropwizard.jobs.guice;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

/**
 * Created by yun on 17/03/14.
 */
public class GuiceJobFactory implements JobFactory {

    private Injector injector;

    @Inject
    public GuiceJobFactory (Injector injector) {
        this.injector = injector;
    }


    @Override
    public Job newJob(TriggerFiredBundle triggerFiredBundle, Scheduler scheduler) throws SchedulerException {

        JobDetail jobDetail = triggerFiredBundle.getJobDetail();
        Class<? extends Job> jobClass = jobDetail.getJobClass();
        return injector.getInstance(jobClass);
    }
}

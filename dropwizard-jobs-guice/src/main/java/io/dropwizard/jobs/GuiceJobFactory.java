package io.dropwizard.jobs;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.ConfigurationException;

import java.lang.reflect.InvocationTargetException;

public class GuiceJobFactory implements JobFactory {

    private final Injector injector;

    @Inject
    public GuiceJobFactory(Injector injector) {
        this.injector = injector;
    }

    @Override
    public Job newJob(TriggerFiredBundle triggerFiredBundle, Scheduler scheduler) throws SchedulerException {
        JobDetail jobDetail = triggerFiredBundle.getJobDetail();
        Class<? extends Job> jobClass = jobDetail.getJobClass();

        // First try to get the job instance from the Guice injector
        try {
            return injector.getInstance(jobClass);
        } catch (ConfigurationException e) {
            // Job class is not bound in Guice, fall back to reflection
            return createJobViaReflection(jobClass);
        }
    }

    private Job createJobViaReflection(Class<? extends Job> jobClass) throws SchedulerException {
        try {
            return jobClass.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException e) {
            throw new SchedulerException("Job class '" + jobClass.getName()
                    + "' cannot be instantiated: no no-arg constructor found", e);
        } catch (InstantiationException e) {
            throw new SchedulerException("Job class '" + jobClass.getName()
                    + "' cannot be instantiated: abstract class or interface", e);
        } catch (IllegalAccessException e) {
            throw new SchedulerException("Job class '" + jobClass.getName()
                    + "' cannot be instantiated: constructor is not accessible", e);
        } catch (InvocationTargetException e) {
            throw new SchedulerException("Job class '" + jobClass.getName()
                    + "' cannot be instantiated: constructor threw an exception", e);
        }
    }
}

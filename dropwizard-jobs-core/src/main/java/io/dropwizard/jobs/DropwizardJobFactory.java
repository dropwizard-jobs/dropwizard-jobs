package io.dropwizard.jobs;

import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

import java.lang.reflect.InvocationTargetException;

class DropwizardJobFactory implements JobFactory {

    private final JobFilters jobs;

    DropwizardJobFactory(JobFilters jobs) {
        this.jobs = jobs;
    }

    @Override
    public Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) throws SchedulerException {
        JobDetail jobDetail = bundle.getJobDetail();
        Class<? extends org.quartz.Job> jobClass = jobDetail.getJobClass();
        JobKey jobKey = jobDetail.getKey();
        String groupName = jobKey.getGroup();

        // First try to find the job in the pre-registered list (existing behavior)
        Job job = jobs.findWith(jobClass, groupName).orElse(null);
        if (job != null) {
            return job;
        }

        // Resolve the actual job class if jobClass is an AOP proxy
        Class<? extends org.quartz.Job> resolvedJobClass = resolveJobClass(jobClass);

        // Fallback: create a new instance via reflection for runtime-scheduled jobs
        return createJobViaReflection(resolvedJobClass);
    }

    /**
     * Resolves the original job class from a potential AOP proxy subclass.
     * AOP frameworks like Guice AOP create subclasses with names containing "$$"
     * (e.g., "MyJob$$EnhancerByGuice$$..."). This method walks up the hierarchy
     * to find the first non-proxy class that is still a Job subclass.
     *
     * @param jobClass the potentially proxied job class
     * @return the resolved non-proxy job class, or the original class if no proxy is detected
     */
    @SuppressWarnings("unchecked")
    private Class<? extends org.quartz.Job> resolveJobClass(Class<? extends org.quartz.Job> jobClass) {
        Class<?> current = jobClass;
        while (current != null && current != Job.class && current != Object.class) {
            // AOP proxy classes typically have "$$" in their name (e.g., EnhancerByGuice, CGLIB)
            if (!current.getName().contains("$$")) {
                return (Class<? extends org.quartz.Job>) current;
            }
            current = current.getSuperclass();
        }
        return jobClass; // fallback to original if no non-proxy class found
    }

    private Job createJobViaReflection(Class<? extends org.quartz.Job> jobClass) throws SchedulerException {
        try {
            return (Job) jobClass.getDeclaredConstructor().newInstance();
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

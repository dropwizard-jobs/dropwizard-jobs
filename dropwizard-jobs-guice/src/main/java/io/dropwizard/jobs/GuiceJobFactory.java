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

/**
 * A Quartz {@link JobFactory} implementation that uses Guice to instantiate jobs.
 * <p>
 * This factory allows Guice to manage the dependency injection of job instances,
 * enabling jobs to receive Guice-injected dependencies.
 * </p>
 */
public class GuiceJobFactory implements JobFactory {

    private final Injector injector;

    /**
     * Creates a new GuiceJobFactory with the specified Injector.
     *
     * @param injector the Guice Injector to use for job instantiation
     */
    @Inject
    public GuiceJobFactory(Injector injector) {
        this.injector = injector;
    }

    @Override
    public Job newJob(TriggerFiredBundle triggerFiredBundle, Scheduler scheduler) throws SchedulerException {
        JobDetail jobDetail = triggerFiredBundle.getJobDetail();
        Class<? extends Job> jobClass = jobDetail.getJobClass();

        // Resolve the actual job class if jobClass is an AOP proxy
        Class<? extends Job> resolvedJobClass = resolveJobClass(jobClass);

        // First try to get the job instance from the Guice injector
        try {
            return injector.getInstance(resolvedJobClass);
        } catch (ConfigurationException e) {
            // Job class is not bound in Guice, fall back to reflection
            return createJobViaReflection(resolvedJobClass);
        }
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
    private Class<? extends Job> resolveJobClass(Class<? extends Job> jobClass) {
        Class<?> current = jobClass;
        while (current != null && current != Job.class && current != Object.class) {
            // AOP proxy classes typically have "$$" in their name (e.g., EnhancerByGuice, CGLIB)
            if (!current.getName().contains("$$")) {
                return (Class<? extends Job>) current;
            }
            current = current.getSuperclass();
        }
        return jobClass; // fallback to original if no non-proxy class found
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

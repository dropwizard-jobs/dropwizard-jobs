package io.dropwizard.jobs;

import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.quartz.Scheduler;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;

/**
 * A Quartz {@link JobFactory} that uses HK2 to instantiate jobs.
 * <p>
 * This factory creates new job instances using the HK2 service locator for each job execution,
 * ensuring proper dependency injection.
 * </p>
 */
public class Hk2JobFactory implements JobFactory {

    private final ServiceLocator locator;
    private final Filter jobFilter;

    /**
     * Creates a new Hk2JobFactory.
     *
     * @param locator the HK2 service locator
     * @param jobFilter the filter for job services
     */
    public Hk2JobFactory(ServiceLocator locator, Filter jobFilter) {
        this.locator = locator;
        this.jobFilter = jobFilter;
    }

    @Override
    public org.quartz.Job newJob(TriggerFiredBundle bundle, Scheduler scheduler) {
        Class<? extends org.quartz.Job> jobClass = bundle.getJobDetail().getJobClass();

        // Find the service handle for this job class
        @SuppressWarnings("unchecked")
        ServiceHandle<Job> handle = locator.getAllServiceHandles(jobFilter).stream()
                .filter(sh -> sh.getActiveDescriptor() != null
                        && sh.getActiveDescriptor().getImplementationClass().equals(jobClass))
                .findFirst()
                .map(sh -> (ServiceHandle<Job>) sh)
                .orElseThrow(() -> new IllegalStateException("No HK2 service found for job class: " + jobClass));

        return handle.getService();
    }
}
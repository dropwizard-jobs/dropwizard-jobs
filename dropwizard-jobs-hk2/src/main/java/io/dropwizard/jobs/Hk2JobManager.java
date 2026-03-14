package io.dropwizard.jobs;

import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.quartz.JobListener;
import org.quartz.spi.JobFactory;

import io.dropwizard.core.setup.Environment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A {@link JobManager} implementation that integrates with HK2 for dependency injection.
 * <p>
 * This class enables jobs to have dependencies injected by HK2. Jobs are discovered by scanning
 * the HK2 service locator for types that extend {@link Job}. Job listeners are discovered by scanning
 * for types that implement {@link JobListener}.
 * </p>
 * <p>
 * Unlike the {@link Hk2JobsBundle}, this manager avoids instantiating jobs during discovery by using
 * {@link ServiceHandle} to get the implementation classes without creating instances.
 * </p>
 *
 * @see Hk2JobFactory
 * @see Hk2JobsBundle
 */
public class Hk2JobManager extends JobManager {

    private final Hk2JobFactory jobFactory;

    /**
     * Creates a new Hk2JobManager with the specified configuration and service locator.
     *
     * @param config the application configuration containing job and Quartz settings
     * @param locator the HK2 service locator used to discover and instantiate jobs and job listeners
     * @param jobFilter the filter to select job services
     * @param listenerFilter the filter to select job listener services, may be null
     */
    public Hk2JobManager(JobConfiguration config, ServiceLocator locator, Filter jobFilter, Filter listenerFilter) {
        super(config, getJobMetadata(locator, jobFilter), getJobListeners(locator, listenerFilter));
        jobFactory = new Hk2JobFactory(locator, jobFilter);
    }

    /**
     * Discovers job metadata from the HK2 service locator.
     * <p>
     * This method gets the service handles for jobs without instantiating them, then creates
     * JobMetadata with the implementation class and group name obtained by instantiating one
     * instance per unique class (to get the group name).
     * </p>
     *
     * @param locator the HK2 service locator
     * @param jobFilter the filter for job services
     * @return list of job metadata
     */
    @SuppressWarnings("unchecked")
    private static List<JobMetadata> getJobMetadata(ServiceLocator locator, Filter jobFilter) {
        // Get all service handles for jobs
        List<ServiceHandle<?>> handles = locator.getAllServiceHandles(jobFilter);

        // Group by implementation class to avoid duplicate instantiation
        Map<Class<? extends Job>, List<ServiceHandle<?>>> byClass = handles.stream()
                .collect(Collectors.groupingBy(sh -> (Class<? extends Job>) sh.getActiveDescriptor().getImplementationClass()));

        List<JobMetadata> metadata = new ArrayList<>();
        for (Map.Entry<Class<? extends Job>, List<ServiceHandle<?>>> entry : byClass.entrySet()) {
            Class<? extends Job> clazz = entry.getKey();
            // Instantiate one instance to get the group name
            Job instance = locator.getService(clazz);
            String groupName = instance.getGroupName();
            metadata.add(new JobMetadata(clazz, groupName));
        }

        return metadata;
    }

    /**
     * Discovers all JobListener instances from the HK2 service locator.
     * <p>
     * This method eagerly instantiates all job listeners bound in the HK2 container.
     * </p>
     *
     * @param locator the HK2 service locator
     * @param listenerFilter the filter for job listener services, may be null
     * @return list of job listeners
     */
    @SuppressWarnings("unchecked")
    private static List<JobListener> getJobListeners(ServiceLocator locator, Filter listenerFilter) {
        if (listenerFilter == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>((List<JobListener>) locator.getAllServices(listenerFilter));
    }

    @Override
    protected JobFactory getJobFactory() {
        return jobFactory;
    }
}
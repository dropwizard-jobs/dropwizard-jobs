package io.dropwizard.jobs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.concurrent.TimeUnit;

import jakarta.inject.Singleton;

import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class Hk2JobManagerTest {

    private JobManager jobManager;
    private ServiceLocator locator;

    @BeforeEach
    public void setUp() {
        // Create a service locator with a unique name
        locator = ServiceLocatorUtilities.createAndPopulateServiceLocator("testLocator" + System.nanoTime());
        
        // Bind jobs with both their concrete class AND Job.class as contract
        // Use .in(Singleton.class) so the same instance is used by both Quartz and the test
        ServiceLocatorUtilities.bind(locator, new AbstractBinder() {
            @Override
            protected void configure() {
                bindAsContract(Dependency.class);
                // Bind as singleton so test can access the same instance that Quartz executes
                bind(ApplicationStartTestJob.class).to(Job.class).to(ApplicationStartTestJob.class).in(Singleton.class);
                bind(OnTestJob.class).to(Job.class).to(OnTestJob.class).in(Singleton.class);
                bind(EveryTestJob.class).to(Job.class).to(EveryTestJob.class).in(Singleton.class);
                bind(DependencyTestJob.class).to(Job.class).to(DependencyTestJob.class).in(Singleton.class);
                bind(ApplicationStopTestJob.class).to(Job.class).to(ApplicationStopTestJob.class).in(Singleton.class);
            }
        });
        
        // Force reification by getting all services handles - this initializes the descriptors properly
        locator.getAllServiceHandles(Job.class);
        
        Filter jobFilter = BuilderHelper.createContractFilter(Job.class.getName());
        jobManager = new Hk2JobManager(new TestConfig(), locator, jobFilter, null);
    }

    @Test
    public void testAllHk2Jobs() throws Exception {
        jobManager.start();
        // Get the singleton instance from the locator and check its latch
        ApplicationStartTestJob startJob = locator.getService(ApplicationStartTestJob.class);
        assertThat(startJob.latch.await(1, TimeUnit.SECONDS), is(true));
        
        OnTestJob onJob = locator.getService(OnTestJob.class);
        assertThat(onJob.latch.await(2, TimeUnit.SECONDS), is(true));
        
        EveryTestJob everyJob = locator.getService(EveryTestJob.class);
        assertThat(everyJob.latch.await(2, TimeUnit.SECONDS), is(true));
        
        DependencyTestJob depJob = locator.getService(DependencyTestJob.class);
        assertThat(depJob.latch.await(2, TimeUnit.SECONDS), is(true));

        jobManager.stop();
        
        ApplicationStopTestJob stopJob = locator.getService(ApplicationStopTestJob.class);
        assertThat(stopJob.latch.await(1, TimeUnit.SECONDS), is(true));
    }

    public static class TestConfig extends JobConfiguration {
    }
}

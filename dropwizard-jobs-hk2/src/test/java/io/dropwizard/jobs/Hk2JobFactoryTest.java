package io.dropwizard.jobs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.glassfish.hk2.api.Filter;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.spi.TriggerFiredBundle;

public class Hk2JobFactoryTest {

    private ServiceLocator locator;
    private Filter jobFilter;
    private Hk2JobFactory jobFactory;

    @BeforeEach
    public void setUp() {
        locator = ServiceLocatorUtilities.createAndPopulateServiceLocator("testLocatorFactory" + System.nanoTime());
        
        // Bind jobs with Job.class as contract - this is required for Hk2JobFactory to find them
        ServiceLocatorUtilities.bind(locator, new AbstractBinder() {
            @Override
            protected void configure() {
                bind(ApplicationStartTestJob.class).to(Job.class);
            }
        });
        
        // Force reification by getting all services handles - this initializes the descriptors properly
        locator.getAllServiceHandles(Job.class);
        
        jobFilter = BuilderHelper.createContractFilter(Job.class.getName());
        jobFactory = new Hk2JobFactory(locator, jobFilter);
    }

    @Test
    public void testNewJobReturnsServiceFromLocator() throws Exception {
        TriggerFiredBundle bundle = mock(TriggerFiredBundle.class);
        Scheduler scheduler = mock(Scheduler.class);
        JobDetail jobDetail = mock(JobDetail.class);
        
        when(bundle.getJobDetail()).thenReturn(jobDetail);
        when(jobDetail.getKey()).thenReturn(JobKey.jobKey("testJob"));
        when(jobDetail.getJobClass()).thenReturn((Class) ApplicationStartTestJob.class);
        
        org.quartz.Job job = jobFactory.newJob(bundle, scheduler);
        
        assertThat(job, is(instanceOf(ApplicationStartTestJob.class)));
    }

    @Test
    public void testNewJobThrowsWhenJobNotFound() throws Exception {
        // Create a new factory with an empty locator
        ServiceLocator emptyLocator = ServiceLocatorUtilities.createAndPopulateServiceLocator("emptyLocator" + System.nanoTime());
        Hk2JobFactory emptyFactory = new Hk2JobFactory(emptyLocator, jobFilter);
        
        TriggerFiredBundle bundle = mock(TriggerFiredBundle.class);
        Scheduler scheduler = mock(Scheduler.class);
        JobDetail jobDetail = mock(JobDetail.class);
        
        when(bundle.getJobDetail()).thenReturn(jobDetail);
        when(jobDetail.getKey()).thenReturn(JobKey.jobKey("testJob"));
        when(jobDetail.getJobClass()).thenReturn((Class) ApplicationStartTestJob.class);
        
        assertThrows(IllegalStateException.class, () -> {
            emptyFactory.newJob(bundle, scheduler);
        });
    }
}

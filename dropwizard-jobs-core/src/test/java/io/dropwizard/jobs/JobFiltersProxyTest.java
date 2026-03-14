package io.dropwizard.jobs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

/**
 * Tests that JobFilters correctly handles AOP proxy subclasses.
 * See: https://github.com/dropwizard-jobs/dropwizard-jobs/issues/75
 */
class JobFiltersProxyTest {

    @Test
    void allEveryFiltersSubclasses() {
        // Create a simulated proxy subclass of EveryTestJob
        EveryTestJob proxyJob = new EveryTestJob() {};
        JobMetadata metadata = new JobMetadata(proxyJob.getClass(), proxyJob.getGroupName());
        JobFilters filters = new JobFilters(List.of(metadata));

        List<JobMetadata> everyJobs = filters.allEvery().toList();
        assertThat(everyJobs, hasSize(1));
        assertEquals(proxyJob.getClass(), everyJobs.get(0).getJobClass());
    }

    @Test
    void allOnCronFiltersSubclasses() {
        // Create a simulated proxy subclass of OnTestJob
        OnTestJob proxyJob = new OnTestJob() {};
        JobMetadata metadata = new JobMetadata(proxyJob.getClass(), proxyJob.getGroupName());
        JobFilters filters = new JobFilters(List.of(metadata));

        List<JobMetadata> onJobs = filters.allOnCron().toList();
        assertThat(onJobs, hasSize(1));
        assertEquals(proxyJob.getClass(), onJobs.get(0).getJobClass());
    }

    @Test
    void allOnApplicationStartFiltersSubclasses() {
        // Create a simulated proxy subclass of ApplicationStartTestJob
        ApplicationStartTestJob proxyJob = new ApplicationStartTestJob() {};
        JobMetadata metadata = new JobMetadata(proxyJob.getClass(), proxyJob.getGroupName());
        JobFilters filters = new JobFilters(List.of(metadata));

        List<JobMetadata> startJobs = filters.allOnApplicationStart().toList();
        assertThat(startJobs, hasSize(1));
        assertEquals(proxyJob.getClass(), startJobs.get(0).getJobClass());
    }

    @Test
    void allOnApplicationStopFiltersSubclasses() {
        // Create a simulated proxy subclass of ApplicationStopTestJob
        ApplicationStopTestJob proxyJob = new ApplicationStopTestJob() {};
        JobMetadata metadata = new JobMetadata(proxyJob.getClass(), proxyJob.getGroupName());
        JobFilters filters = new JobFilters(List.of(metadata));

        List<JobMetadata> stopJobs = filters.allOnApplicationStop().toList();
        assertThat(stopJobs, hasSize(1));
        assertEquals(proxyJob.getClass(), stopJobs.get(0).getJobClass());
    }

    @Test
    void findWithMatchesSubclassToSuperclass() {
        EveryTestJob originalJob = new EveryTestJob();
        JobMetadata metadata = new JobMetadata(originalJob.getClass(), originalJob.getGroupName());
        JobFilters filters = new JobFilters(List.of(metadata));

        // The original class should still be found
        Optional<JobMetadata> found = filters.findWith(EveryTestJob.class, null);
        assertTrue(found.isPresent());
        assertEquals(originalJob.getClass(), found.get().getJobClass());
    }

    @Test
    void findWithMatchesProxySubclassToSuperclass() {
        // Create a simulated proxy subclass
        EveryTestJob proxyJob = new EveryTestJob() {};
        JobMetadata metadata = new JobMetadata(proxyJob.getClass(), proxyJob.getGroupName());
        JobFilters filters = new JobFilters(List.of(metadata));

        // Should find the proxy when searching by the parent class
        Optional<JobMetadata> found = filters.findWith(EveryTestJob.class, null);
        assertTrue(found.isPresent());
        assertEquals(proxyJob.getClass(), found.get().getJobClass());
    }

    @Test
    void findWithMatchesSuperclassToProxySubclass() {
        // Create a simulated proxy subclass
        EveryTestJob proxyJob = new EveryTestJob() {};
        Class<? extends EveryTestJob> proxyClass = proxyJob.getClass();
        JobMetadata metadata = new JobMetadata(proxyJob.getClass(), proxyJob.getGroupName());
        JobFilters filters = new JobFilters(List.of(metadata));

        // Should find the proxy when searching by the proxy class
        Optional<JobMetadata> found = filters.findWith(proxyClass, null);
        assertTrue(found.isPresent());
        assertEquals(proxyJob.getClass(), found.get().getJobClass());
    }
}

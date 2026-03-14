package io.dropwizard.jobs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
        JobFilters filters = new JobFilters(List.of(proxyJob));

        List<Job> everyJobs = filters.allEvery().toList();
        assertThat(everyJobs, hasSize(1));
        assertSame(proxyJob, everyJobs.get(0));
    }

    @Test
    void allOnCronFiltersSubclasses() {
        // Create a simulated proxy subclass of OnTestJob
        OnTestJob proxyJob = new OnTestJob() {};
        JobFilters filters = new JobFilters(List.of(proxyJob));

        List<Job> onJobs = filters.allOnCron().toList();
        assertThat(onJobs, hasSize(1));
        assertSame(proxyJob, onJobs.get(0));
    }

    @Test
    void allOnApplicationStartFiltersSubclasses() {
        // Create a simulated proxy subclass of ApplicationStartTestJob
        ApplicationStartTestJob proxyJob = new ApplicationStartTestJob() {};
        JobFilters filters = new JobFilters(List.of(proxyJob));

        List<Job> startJobs = filters.allOnApplicationStart().toList();
        assertThat(startJobs, hasSize(1));
        assertSame(proxyJob, startJobs.get(0));
    }

    @Test
    void allOnApplicationStopFiltersSubclasses() {
        // Create a simulated proxy subclass of ApplicationStopTestJob
        ApplicationStopTestJob proxyJob = new ApplicationStopTestJob() {};
        JobFilters filters = new JobFilters(List.of(proxyJob));

        List<Job> stopJobs = filters.allOnApplicationStop().toList();
        assertThat(stopJobs, hasSize(1));
        assertSame(proxyJob, stopJobs.get(0));
    }

    @Test
    void findWithMatchesSubclassToSuperclass() {
        EveryTestJob originalJob = new EveryTestJob();
        JobFilters filters = new JobFilters(List.of(originalJob));

        // The original class should still be found
        Optional<Job> found = filters.findWith(EveryTestJob.class, null);
        assertTrue(found.isPresent());
        assertSame(originalJob, found.get());
    }

    @Test
    void findWithMatchesProxySubclassToSuperclass() {
        // Create a simulated proxy subclass
        EveryTestJob proxyJob = new EveryTestJob() {};
        JobFilters filters = new JobFilters(List.of(proxyJob));

        // Should find the proxy when searching by the parent class
        Optional<Job> found = filters.findWith(EveryTestJob.class, null);
        assertTrue(found.isPresent());
        assertSame(proxyJob, found.get());
    }

    @Test
    void findWithMatchesSuperclassToProxySubclass() {
        // Create a simulated proxy subclass
        EveryTestJob proxyJob = new EveryTestJob() {};
        Class<? extends EveryTestJob> proxyClass = proxyJob.getClass();
        JobFilters filters = new JobFilters(List.of(proxyJob));

        // Should find the proxy when searching by the proxy class
        Optional<Job> found = filters.findWith(proxyClass, null);
        assertTrue(found.isPresent());
        assertSame(proxyJob, found.get());
    }
}

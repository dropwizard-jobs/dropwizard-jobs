package io.dropwizard.jobs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.google.inject.Guice;
import com.google.inject.Injector;

import io.dropwizard.core.setup.Environment;
import io.dropwizard.lifecycle.Managed;
import io.dropwizard.lifecycle.setup.LifecycleEnvironment;

public class GuiceJobsBundleTest {

    private final Environment environment = mock(Environment.class);
    private final LifecycleEnvironment applicationContext = mock(LifecycleEnvironment.class);

    @Test
    public void assertJobsBundleIsWorking() throws Exception {
        Injector injector = Guice.createInjector();

        when(environment.lifecycle()).thenReturn(applicationContext);
        new GuiceJobsBundle(injector).run(new MyConfiguration(), environment);

        final ArgumentCaptor<JobManager> jobManagerCaptor = ArgumentCaptor.forClass(JobManager.class);
        verify(applicationContext).manage(jobManagerCaptor.capture());

        JobManager jobManager = jobManagerCaptor.getValue();
        assertThat(jobManager, is(notNullValue()));
    }

    // ========== Deferred mode tests ==========

    @Test
    public void testDeferredConstructorRejectsNull() {
        assertThrows(NullPointerException.class, () -> {
            new GuiceJobsBundle((Supplier<Injector>) null);
        });
    }

    @Test
    public void testDeferredRunRegistersManaged() throws Exception {
        Injector injector = Guice.createInjector();
        GuiceJobsBundle bundle = new GuiceJobsBundle(() -> injector);

        when(environment.lifecycle()).thenReturn(applicationContext);
        bundle.run(new MyConfiguration(), environment);

        // Verify a Managed is registered (not a GuiceJobManager directly)
        ArgumentCaptor<Managed> managedCaptor = ArgumentCaptor.forClass(Managed.class);
        verify(applicationContext).manage(managedCaptor.capture());

        Managed managed = managedCaptor.getValue();
        assertThat(managed, is(notNullValue()));
        // Verify it's a Managed wrapper, not a JobManager directly
        assertThat("Expected a Managed wrapper, not a JobManager directly",
            managed instanceof JobManager, is(false));
    }

    @Test
    public void testDeferredGetSchedulerReturnsNullBeforeStart() throws Exception {
        Injector injector = Guice.createInjector();
        GuiceJobsBundle bundle = new GuiceJobsBundle(() -> injector);

        when(environment.lifecycle()).thenReturn(applicationContext);
        bundle.run(new MyConfiguration(), environment);

        // getScheduler() should return null before Managed.start() is called
        assertThat(bundle.getScheduler(), is(nullValue()));
    }

    @Test
    public void testDeferredSupplierCalledDuringManagedStart() throws Exception {
        AtomicBoolean supplierCalled = new AtomicBoolean(false);
        Supplier<Injector> supplier = () -> {
            supplierCalled.set(true);
            return Guice.createInjector();
        };

        GuiceJobsBundle bundle = new GuiceJobsBundle(supplier);

        when(environment.lifecycle()).thenReturn(applicationContext);
        bundle.run(new MyConfiguration(), environment);

        // Supplier should NOT be called yet (deferred until Managed.start())
        assertFalse(supplierCalled.get(), "Supplier should not be called during run()");

        // Capture and invoke the Managed wrapper's start()
        ArgumentCaptor<Managed> managedCaptor = ArgumentCaptor.forClass(Managed.class);
        verify(applicationContext).manage(managedCaptor.capture());
        Managed managed = managedCaptor.getValue();
        managed.start();

        // Supplier SHOULD be called after Managed.start()
        assertTrue(supplierCalled.get(), "Supplier should be called during Managed.start()");
    }

    @Test
    public void testDeferredModeStartsJobManager() throws Exception {
        Injector injector = Guice.createInjector();
        GuiceJobsBundle bundle = new GuiceJobsBundle(() -> injector);

        when(environment.lifecycle()).thenReturn(applicationContext);
        bundle.run(new MyConfiguration(), environment);

        // getScheduler() returns null before start
        assertThat(bundle.getScheduler(), is(nullValue()));

        // Capture and invoke the Managed wrapper's start()
        ArgumentCaptor<Managed> managedCaptor = ArgumentCaptor.forClass(Managed.class);
        verify(applicationContext).manage(managedCaptor.capture());
        Managed managed = managedCaptor.getValue();
        managed.start();

        // After start(), getScheduler() should return a non-null scheduler
        assertThat(bundle.getScheduler(), is(notNullValue()));
    }

    // ========== Backward compatibility tests ==========

    @Test
    public void testEagerConstructorRejectsNull() {
        assertThrows(NullPointerException.class, () -> {
            new GuiceJobsBundle((Injector) null);
        });
    }

    private static class MyConfiguration extends JobConfiguration {}
}

package io.dropwizard.jobs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

public class GuiceJobManagerTest {

    private JobManager jobManager;
    private Injector injector;

    @Before
    public void ensureLatchesAreZero() {
        injector = Guice.createInjector((Module) binder -> {
            binder.bind(ApplicationStartTestJob.class).asEagerSingleton();
            binder.bind(OnTestJob.class).asEagerSingleton();
            binder.bind(EveryTestJob.class).asEagerSingleton();
            binder.bind(DependencyTestJob.class).asEagerSingleton();
            binder.bind(ApplicationStopTestJob.class).asEagerSingleton();
        });
        jobManager = new GuiceJobManager(new TestConfig(), injector);
    }

    @Test
    public void testAllGuiceJobs() throws Exception {
        jobManager.start();
        assertThat(injector.getInstance(ApplicationStartTestJob.class).latch.await(1, TimeUnit.SECONDS), is(true));

        assertThat(injector.getInstance(OnTestJob.class).latch.await(2, TimeUnit.SECONDS), is(true));
        assertThat(injector.getInstance(EveryTestJob.class).latch.await(2, TimeUnit.SECONDS), is(true));
        assertThat(injector.getInstance(DependencyTestJob.class).latch.await(2, TimeUnit.SECONDS), is(true));

        jobManager.stop();
        assertThat(injector.getInstance(ApplicationStopTestJob.class).latch.await(1, TimeUnit.SECONDS), is(true));
    }
    
    public static class TestConfig extends JobConfiguration {
        
    }
}

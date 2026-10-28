package io.dropwizard.jobs;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import com.google.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;
import org.quartz.spi.TriggerFiredBundle;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.inject.matcher.Matchers.only;
import static com.google.inject.matcher.Matchers.subclassesOf;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GuiceJobFactoryTest {

    @Test
    void obtainsDependencyBearingJobFromInjector() throws Exception {
        Injector injector = Guice.createInjector(binder -> {
            binder.bind(String.class).toInstance("injected");
            binder.bind(InjectedTestJob.class).in(Singleton.class);
        });
        InjectedTestJob job = assertInstanceOf(InjectedTestJob.class,
                new GuiceJobFactory(injector).newJob(bundleFor(InjectedTestJob.class), null));
        assertSame(injector.getInstance(InjectedTestJob.class), job);
        assertEquals("injected", job.dependency);
    }

    @Test
    void explicitlyUnboundJobFallsBackToReflection() throws Exception {
        Injector injector = Guice.createInjector(binder -> binder.requireExplicitBindings());
        assertEquals(ReflectiveTestJob.class,
                new GuiceJobFactory(injector).newJob(bundleFor(ReflectiveTestJob.class), null).getClass());
    }

    @Test
    void provisioningFailureIsNotHiddenByReflection() {
        Injector injector = mock(Injector.class);
        ProvisionException expected = new ProvisionException("broken dependency");
        when(injector.getInstance(ReflectiveTestJob.class)).thenThrow(expected);
        assertSame(expected, assertThrows(ProvisionException.class,
                () -> new GuiceJobFactory(injector).newJob(bundleFor(ReflectiveTestJob.class), null)));
    }

    @Test
    void resolvesRealGuiceProxyAndRetainsInterception() throws Exception {
        AtomicInteger interceptions = new AtomicInteger();
        Method jobMethod = ReflectiveTestJob.class.getMethod("doJob", JobExecutionContext.class);
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(ReflectiveTestJob.class).in(Singleton.class);
                bindInterceptor(subclassesOf(ReflectiveTestJob.class), only(jobMethod), invocation -> {
                    interceptions.incrementAndGet();
                    return invocation.proceed();
                });
            }
        });
        ReflectiveTestJob original = injector.getInstance(ReflectiveTestJob.class);
        assertTrue(original.getClass().getName().contains("$$"));
        ReflectiveTestJob resolved = assertInstanceOf(ReflectiveTestJob.class,
                new GuiceJobFactory(injector).newJob(bundleFor(original.getClass()), null));
        assertSame(original, resolved);
        resolved.doJob(mock(JobExecutionContext.class));
        assertEquals(1, interceptions.get());
    }

    @Test
    void unscopedJobsAreNewForEachExecution() throws Exception {
        GuiceJobFactory factory = new GuiceJobFactory(Guice.createInjector());
        assertNotSame(factory.newJob(bundleFor(ReflectiveTestJob.class), null),
                factory.newJob(bundleFor(ReflectiveTestJob.class), null));
    }

    @Test
    void reflectionFailuresRetainTheirCauses() {
        Injector injector = Guice.createInjector(binder -> binder.requireExplicitBindings());
        GuiceJobFactory factory = new GuiceJobFactory(injector);
        Map<Class<? extends Job>, Class<? extends Throwable>> cases = Map.of(
                InjectedTestJob.class, NoSuchMethodException.class,
                AbstractTestJob.class, InstantiationException.class,
                PrivateConstructorTestJob.class, IllegalAccessException.class,
                ThrowingConstructorTestJob.class, InvocationTargetException.class);
        cases.forEach((jobClass, causeClass) -> {
            SchedulerException failure = assertThrows(SchedulerException.class, () -> factory.newJob(bundleFor(jobClass), null));
            assertInstanceOf(causeClass, failure.getCause());
            assertTrue(failure.getMessage().contains(jobClass.getName()));
        });
    }

    private TriggerFiredBundle bundleFor(Class<? extends Job> jobClass) {
        TriggerFiredBundle bundle = mock(TriggerFiredBundle.class);
        when(bundle.getJobDetail()).thenReturn(JobBuilder.newJob(jobClass).withIdentity("factory-test").build());
        return bundle;
    }

    public static class ReflectiveTestJob extends Job {
        @Override
        public void doJob(JobExecutionContext context) {
        }
    }

    public static class InjectedTestJob extends ReflectiveTestJob {
        final String dependency;

        @Inject
        public InjectedTestJob(String dependency) {
            this.dependency = dependency;
        }
    }

    public abstract static class AbstractTestJob extends ReflectiveTestJob {
    }

    public static class PrivateConstructorTestJob extends ReflectiveTestJob {
        private PrivateConstructorTestJob() {
        }
    }

    public static class ThrowingConstructorTestJob extends ReflectiveTestJob {
        public ThrowingConstructorTestJob() {
            throw new IllegalStateException("constructor failure");
        }
    }
}
package io.dropwizard.jobs;

import org.junit.jupiter.api.Test;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SpringJobFactoryTest {

    @Test
    void returnsRegisteredDependencyBearingBean() throws Exception {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(String.class, () -> "injected");
            context.registerBean(InjectedTestJob.class);
            context.refresh();
            InjectedTestJob job = assertInstanceOf(InjectedTestJob.class,
                    new SpringJobFactory(context).newJob(bundleFor(InjectedTestJob.class), null));
            assertSame(context.getBean(InjectedTestJob.class), job);
            assertEquals("injected", job.dependency);
        }
    }

    @Test
    void autowiresDependencyWhenJobIsNotARegisteredBean() throws Exception {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(String.class, () -> "autowired");
            context.refresh();
            assertThrows(NoSuchBeanDefinitionException.class, () -> context.getBean(InjectedTestJob.class));
            InjectedTestJob job = assertInstanceOf(InjectedTestJob.class,
                    new SpringJobFactory(context).newJob(bundleFor(InjectedTestJob.class), null));
            assertEquals("autowired", job.dependency);
        }
    }

    @Test
    void failedAutowiringFallsBackToReflection() throws Exception {
        ApplicationContext context = mock(ApplicationContext.class);
        AutowireCapableBeanFactory beanFactory = mock(AutowireCapableBeanFactory.class);
        when(context.getBean(ReflectiveTestJob.class)).thenThrow(new NoSuchBeanDefinitionException(ReflectiveTestJob.class));
        when(context.getAutowireCapableBeanFactory()).thenReturn(beanFactory);
        when(beanFactory.createBean(ReflectiveTestJob.class)).thenThrow(new BeanCreationException("autowiring failed"));

        assertEquals(ReflectiveTestJob.class,
                new SpringJobFactory(context).newJob(bundleFor(ReflectiveTestJob.class), null).getClass());
        verify(beanFactory).createBean(ReflectiveTestJob.class);
    }

    @Test
    void registeredBeanCreationFailureIsNotHidden() {
        ApplicationContext context = mock(ApplicationContext.class);
        BeanCreationException expected = new BeanCreationException("broken dependency");
        when(context.getBean(InjectedTestJob.class)).thenThrow(expected);
        assertSame(expected, assertThrows(BeanCreationException.class,
                () -> new SpringJobFactory(context).newJob(bundleFor(InjectedTestJob.class), null)));
        verify(context, never()).getAutowireCapableBeanFactory();
    }

    @Test
    void reflectionFailuresRetainTheirCauses() {
        ApplicationContext context = mock(ApplicationContext.class);
        when(context.getAutowireCapableBeanFactory()).thenThrow(new IllegalStateException("autowiring unavailable"));
        SpringJobFactory factory = new SpringJobFactory(context);
        Map<Class<? extends Job>, Class<? extends Throwable>> cases = Map.of(
                InjectedTestJob.class, NoSuchMethodException.class,
                AbstractTestJob.class, InstantiationException.class,
                PrivateConstructorTestJob.class, IllegalAccessException.class,
                ThrowingConstructorTestJob.class, InvocationTargetException.class);
        cases.forEach((jobClass, causeClass) -> {
            when(context.getBean(jobClass)).thenThrow(new NoSuchBeanDefinitionException(jobClass));
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
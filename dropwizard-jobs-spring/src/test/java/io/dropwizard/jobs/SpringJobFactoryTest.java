package io.dropwizard.jobs;

import org.junit.jupiter.api.Test;
import org.quartz.JobBuilder;
import org.quartz.JobExecutionContext;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.TriggerFiredBundle;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SpringJobFactoryTest {

    private final Scheduler scheduler = mock(Scheduler.class);

    @Test
    public void testReturnsRegisteredBean() throws SchedulerException {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(SimpleTestJob.class)) {
            assertSame(context.getBean(SimpleTestJob.class),
                    new SpringJobFactory(context).newJob(bundleFor(SimpleTestJob.class), scheduler));
        }
    }

    @Test
    public void testAutowiresUnregisteredJob() throws SchedulerException {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(Dependency.class)) {
            AutowiredTestJob job = assertInstanceOf(AutowiredTestJob.class,
                    new SpringJobFactory(context).newJob(bundleFor(AutowiredTestJob.class), scheduler));
            assertSame(context.getBean(Dependency.class), job.dependency);
        }
    }

    @Test
    public void testRejectsMissingRequiredDependency() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.refresh();
            SchedulerException failure = assertThrows(SchedulerException.class,
                    () -> new SpringJobFactory(context).newJob(bundleFor(AutowiredTestJob.class), scheduler));
            assertInstanceOf(BeanCreationException.class, failure.getCause());
        }
    }

    @Test
    public void testPreservesPostProcessorFailure() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.refresh();
            BeanCreationException rejection = new BeanCreationException("Job rejected by post-processor");
            context.getBeanFactory().addBeanPostProcessor(new BeanPostProcessor() {
                @Override
                public Object postProcessBeforeInitialization(Object bean, String beanName) {
                    if (bean instanceof SimpleTestJob) {
                        throw rejection;
                    }
                    return bean;
                }
            });
            SchedulerException failure = assertThrows(SchedulerException.class,
                    () -> new SpringJobFactory(context).newJob(bundleFor(SimpleTestJob.class), scheduler));
            BeanCreationException creationFailure = assertInstanceOf(BeanCreationException.class, failure.getCause());
            assertSame(rejection, creationFailure.getMostSpecificCause());
        }
    }

    @Test
    public void testRejectsAmbiguousBeans() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean("firstJob", SimpleTestJob.class);
            context.registerBean("secondJob", SimpleTestJob.class);
            context.refresh();
            SchedulerException failure = assertThrows(SchedulerException.class,
                    () -> new SpringJobFactory(context).newJob(bundleFor(SimpleTestJob.class), scheduler));
            assertInstanceOf(NoUniqueBeanDefinitionException.class, failure.getCause());
        }
    }

    @Test
    public void testPreservesBeanLookupFailure() {
        ApplicationContext context = mock(ApplicationContext.class);
        BeanCreationException rejection = new BeanCreationException("Job bean could not be created");
        when(context.getBean(SimpleTestJob.class)).thenThrow(rejection);
        SchedulerException failure = assertThrows(SchedulerException.class,
                () -> new SpringJobFactory(context).newJob(bundleFor(SimpleTestJob.class), scheduler));
        assertSame(rejection, failure.getCause());
    }

    private TriggerFiredBundle bundleFor(Class<? extends Job> jobClass) {
        TriggerFiredBundle bundle = mock(TriggerFiredBundle.class);
        when(bundle.getJobDetail()).thenReturn(JobBuilder.newJob(jobClass).build());
        return bundle;
    }

    public static class SimpleTestJob extends Job {
        @Override
        public void doJob(JobExecutionContext context) {
        }
    }

    public static class AutowiredTestJob extends Job {
        @Autowired
        private Dependency dependency;

        @Override
        public void doJob(JobExecutionContext context) {
        }
    }
}
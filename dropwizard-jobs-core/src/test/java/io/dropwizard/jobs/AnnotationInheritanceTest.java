package io.dropwizard.jobs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.dropwizard.jobs.annotations.DelayStart;
import io.dropwizard.jobs.annotations.Every;
import io.dropwizard.jobs.annotations.On;
import io.dropwizard.jobs.annotations.OnApplicationStart;
import io.dropwizard.jobs.annotations.OnApplicationStop;
import org.junit.jupiter.api.Test;

/**
 * Tests that job scheduling annotations are inherited by subclasses,
 * which is required for AOP proxy compatibility (e.g., Guice AOP).
 * See: https://github.com/dropwizard-jobs/dropwizard-jobs/issues/75
 */
class AnnotationInheritanceTest {

    @Test
    void everyAnnotationIsInheritedBySubclass() {
        // EveryTestJob is annotated with @Every("10ms")
        Class<?> subclass = new EveryTestJob() {}.getClass();
        assertTrue(subclass.isAnnotationPresent(Every.class),
                "@Every should be inherited by subclasses");
        assertEquals("10ms", subclass.getAnnotation(Every.class).value());
    }

    @Test
    void onAnnotationIsInheritedBySubclass() {
        // OnTestJob is annotated with @On("0/1 * * * * ?")
        Class<?> subclass = new OnTestJob() {}.getClass();
        assertTrue(subclass.isAnnotationPresent(On.class),
                "@On should be inherited by subclasses");
        assertEquals("0/1 * * * * ?", subclass.getAnnotation(On.class).value());
    }

    @Test
    void onApplicationStartAnnotationIsInheritedBySubclass() {
        // ApplicationStartTestJob is annotated with @OnApplicationStart
        Class<?> subclass = new ApplicationStartTestJob() {}.getClass();
        assertTrue(subclass.isAnnotationPresent(OnApplicationStart.class),
                "@OnApplicationStart should be inherited by subclasses");
    }

    @Test
    void onApplicationStopAnnotationIsInheritedBySubclass() {
        // ApplicationStopTestJob is annotated with @OnApplicationStop
        Class<?> subclass = new ApplicationStopTestJob() {}.getClass();
        assertTrue(subclass.isAnnotationPresent(OnApplicationStop.class),
                "@OnApplicationStop should be inherited by subclasses");
    }

    @Test
    void delayStartAnnotationIsInheritedBySubclass() {
        // EveryTestJobWithDelay is annotated with @DelayStart("1s")
        Class<?> subclass = new EveryTestJobWithDelay() {}.getClass();
        assertTrue(subclass.isAnnotationPresent(DelayStart.class),
                "@DelayStart should be inherited by subclasses");
        assertEquals("1s", subclass.getAnnotation(DelayStart.class).value());
    }
}

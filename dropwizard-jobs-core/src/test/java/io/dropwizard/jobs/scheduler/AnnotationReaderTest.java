package io.dropwizard.jobs.scheduler;

import io.dropwizard.jobs.Job;
import io.dropwizard.jobs.MutableJobConfiguration;
import org.junit.jupiter.api.Test;
import org.quartz.JobExecutionContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AnnotationReaderTest {

    @Test
    void literalsAreNotConfigurationKeys() {
        MutableJobConfiguration config = new MutableJobConfiguration().withJob("5s", "1h");
        assertEquals("5s", AnnotationReader.resolveDurationExpression("5s", AnnotatedTestJob.class, config));
        assertEquals("0 15 10 ? * *", AnnotationReader.resolveDurationExpression("0 15 10 ? * *", AnnotatedTestJob.class, config));
    }

    @Test
    void emptyValueUsesUncapitalizedClassName() {
        MutableJobConfiguration config = new MutableJobConfiguration().withJob("annotatedTestJob", "25ms");
        assertEquals("25ms", AnnotationReader.resolveDurationExpression("", AnnotatedTestJob.class, config));
    }

    @Test
    void placeholderUsesExplicitKey() {
        MutableJobConfiguration config = new MutableJobConfiguration().withJob("interval", "5min");
        assertEquals("5min", AnnotationReader.resolveDurationExpression("${interval}", AnnotatedTestJob.class, config));
    }

    @Test
    void missingAndEmptyOverridesLeaveExpressionForDownstreamValidation() {
        MutableJobConfiguration config = new MutableJobConfiguration().withJob("empty", "");
        for (String expression : List.of("", "${missing}", "${empty}")) {
            assertEquals(expression, AnnotationReader.resolveDurationExpression(expression, AnnotatedTestJob.class, config));
        }
        assertEquals("5s", AnnotationReader.resolveDurationExpression("5s", AnnotatedTestJob.class, null));
        assertEquals("${missing}", AnnotationReader.resolveDurationExpression("${missing}", AnnotatedTestJob.class, null));
        assertNull(AnnotationReader.readDurationFromConfig("${missing}", AnnotatedTestJob.class, null));
    }

    @Test
    void configurationLookupRejectsMalformedAndEmptyPlaceholders() {
        MutableJobConfiguration config = new MutableJobConfiguration();
        for (String expression : List.of("plain-key", "${missing", "missing}", "${}")) {
            assertThrows(IllegalArgumentException.class,
                    () -> AnnotationReader.readDurationFromConfig(expression, AnnotatedTestJob.class, config), expression);
        }
        assertThrows(IllegalArgumentException.class,
                () -> AnnotationReader.resolveDurationExpression("${}", AnnotatedTestJob.class, config));
    }

    static class AnnotatedTestJob extends Job {
        @Override
        public void doJob(JobExecutionContext context) {
        }
    }
}
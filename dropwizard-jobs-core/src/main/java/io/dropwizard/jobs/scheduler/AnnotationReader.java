package io.dropwizard.jobs.scheduler;

import io.dropwizard.jobs.JobConfiguration;
import org.apache.commons.text.WordUtils;
import org.quartz.Job;

abstract class AnnotationReader {

    static String readDurationFromConfig(String value, Class<? extends Job> clazz, JobConfiguration configuration) {
        if (configuration == null) {
            return null;
        }
        String property = WordUtils.uncapitalize(clazz.getSimpleName());
        if (!value.isEmpty()) {
            property = value.substring(2, value.length() - 1);
        }
        return configuration.getJobs().getOrDefault(property, null);
    }

}

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
            if (!value.startsWith("${") || !value.endsWith("}")) {
                throw new IllegalArgumentException(
                    "Invalid placeholder format '" + value + "'. Expected format: ${propertyName}");
            }
            property = value.substring(2, value.length() - 1);
            if (property.isEmpty()) {
                throw new IllegalArgumentException(
                    "Empty placeholder key in '" + value + "'. Property name cannot be empty.");
            }
        }
        return configuration.getJobs().getOrDefault(property, null);
    }

}

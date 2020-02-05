package io.dropwizard.jobs;

import org.apache.commons.text.WordUtils;

import io.dropwizard.jobs.annotations.Every;
import io.dropwizard.jobs.annotations.On;

abstract class AnnotationReader {

    static String readDurationFromConfig(On annotation, Class<? extends org.quartz.Job> clazz, JobConfiguration configuration) {
        if (configuration == null) {
            return null;
        } else {
            String property = WordUtils.uncapitalize(clazz.getSimpleName());
            if (!annotation.value().isEmpty()) {
                property = annotation.value().substring(2, annotation.value().length() - 1);
            }
            return configuration.getJobs().getOrDefault(property, null);
        }
    }

    static String readDurationFromConfig(Every annotation, Class<? extends org.quartz.Job> clazz, JobConfiguration configuration) {
        if (configuration == null) {
            return null;
        }
        String property = WordUtils.uncapitalize(clazz.getSimpleName());
        if (!annotation.value().isEmpty()) {
            property = annotation.value().substring(2, annotation.value().length() - 1);
        }
        return configuration.getJobs().getOrDefault(property, null);
    }

}

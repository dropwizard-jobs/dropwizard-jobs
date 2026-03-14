package io.dropwizard.jobs.scheduler;

import io.dropwizard.jobs.Job;
import io.dropwizard.jobs.JobConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.commons.lang3.StringUtils.isEmpty;

abstract class AnnotationReader {

    private static final Logger log = LoggerFactory.getLogger(AnnotationReader.class);

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

    /**
     * Resolves a duration expression, either from configuration or as a plain value.
     * <p>
     * If the expression is empty or a placeholder (${...}), attempts to read the value
     * from the job configuration. Otherwise, returns the expression as-is.
     *
     * @param expression   the expression to resolve (may be empty, a placeholder, or a plain value)
     * @param clazz        the job class being scheduled
     * @param configuration the job configuration
     * @return the resolved expression
     */
    static String resolveDurationExpression(String expression, Class<? extends Job> clazz,
            JobConfiguration configuration) {
        if (expression.isEmpty() || expression.matches("\\$\\{.*\\}")) {
            String fromConfig = readDurationFromConfig(expression, clazz, configuration);
            if (!isEmpty(fromConfig)) {
                log.info("{} is configured in the config file to run every {}", clazz.getSimpleName(), fromConfig);
                return fromConfig;
            }
        }
        return expression;
    }

}

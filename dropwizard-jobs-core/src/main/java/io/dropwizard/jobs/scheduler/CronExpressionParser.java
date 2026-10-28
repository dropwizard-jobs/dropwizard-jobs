package io.dropwizard.jobs.scheduler;

import io.dropwizard.jobs.JobConfiguration;
import org.quartz.CronScheduleBuilder;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.TimeZone;

public class CronExpressionParser {
    private final TimeZone defaultTimezone;

    public CronExpressionParser(JobConfiguration configuration) {
        this.defaultTimezone = readDefaultTimezoneFromConfiguration(configuration);
    }

    private TimeZone readDefaultTimezoneFromConfiguration(JobConfiguration configuration) {
        if (configuration != null && configuration.getQuartzConfiguration().containsKey("io.github.dropwizard-jobs.timezone")) {
            String timezoneId = configuration.getQuartzConfiguration().get("io.github.dropwizard-jobs.timezone");
            return validateAndGetTimeZone(timezoneId);
        } else {
            return TimeZone.getDefault();
        }
    }


    /**
     * Allow timezone to be configured on a per-cron basis with [timezoneName] appended to the cron format
     *
     * @param cronExpression the modified cron format
     * @return the cron schedule with the timezone applied to it if needed
     * @throws IllegalArgumentException if the timezone suffix is malformed
     */
    public CronScheduleBuilder parse(String cronExpression) {
        String expression = cronExpression.trim();
        int timezoneStart = expression.indexOf('[');
        int timezoneEnd = expression.indexOf(']');
        TimeZone timezone = defaultTimezone;
        if (timezoneStart >= 0 || timezoneEnd >= 0) {
            if (timezoneStart < 0 || timezoneEnd <= timezoneStart || timezoneEnd != expression.length() - 1
                    || expression.indexOf('[', timezoneStart + 1) >= 0) {
                throw new IllegalArgumentException("Invalid timezone suffix in cron expression '" + cronExpression
                        + "'. Expected a single [timezone] at the end.");
            }
            String timezoneId = expression.substring(timezoneStart + 1, timezoneEnd);
            timezone = validateAndGetTimeZone(timezoneId);
            expression = expression.substring(0, timezoneStart).trim();
        }
        return CronScheduleBuilder.cronSchedule(expression).inTimeZone(timezone);
    }

    /**
     * Validates the timezone ID and returns the corresponding TimeZone object.
     *
     * @param timezoneId the timezone ID to validate
     * @return the validated TimeZone object
     * @throws IllegalArgumentException if the timezone ID is invalid
     */
    static TimeZone validateAndGetTimeZone(String timezoneId) {
        try {
            ZoneId zoneId = ZoneId.of(timezoneId);
            return TimeZone.getTimeZone(zoneId);
        } catch (DateTimeException e) {
            String message = String.format(
                "Invalid timezone ID: '%s'. Valid IDs include: %s",
                timezoneId,
                Arrays.toString(Arrays.stream(TimeZone.getAvailableIDs()).limit(10).toArray())
            );
            throw new IllegalArgumentException(message, e);
        }
    }

}

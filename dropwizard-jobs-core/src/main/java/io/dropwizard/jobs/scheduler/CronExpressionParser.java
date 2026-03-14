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
        if (configuration != null && configuration.getQuartzConfiguration().containsKey("de.spinscale.dropwizard.jobs.timezone")) {
            String timezoneId = configuration.getQuartzConfiguration().get("de.spinscale.dropwizard.jobs.timezone");
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
     */
    public CronScheduleBuilder parse(String cronExpression) {
        int i = cronExpression.indexOf("[");
        int j = cronExpression.indexOf("]");
        TimeZone timezone = defaultTimezone;
        if (i > -1 && j > -1) {
            String timezoneId = cronExpression.substring(i + 1, j);
            timezone = validateAndGetTimeZone(timezoneId);
            cronExpression = cronExpression.substring(0, i).trim();
        }
        return CronScheduleBuilder.cronSchedule(cronExpression).inTimeZone(timezone);
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

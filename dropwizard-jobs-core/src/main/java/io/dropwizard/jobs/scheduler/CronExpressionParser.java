package io.dropwizard.jobs.scheduler;

import io.dropwizard.jobs.JobConfiguration;
import org.quartz.CronScheduleBuilder;

import java.util.TimeZone;

public class CronExpressionParser {
    private final TimeZone defaultTimezone;

    public CronExpressionParser(JobConfiguration configuration) {
        this.defaultTimezone = readDefaultTimezoneFromConfiguration(configuration);
    }

    private TimeZone readDefaultTimezoneFromConfiguration(JobConfiguration configuration) {
        if (configuration != null && configuration.getQuartzConfiguration().containsKey("de.spinscale.dropwizard.jobs.timezone")) {
            return TimeZone.getTimeZone(configuration.getQuartzConfiguration().get("de.spinscale.dropwizard.jobs.timezone"));
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
            timezone = TimeZone.getTimeZone(cronExpression.substring(i + 1, j));
            cronExpression = cronExpression.substring(0, i).trim();
        }
        return CronScheduleBuilder.cronSchedule(cronExpression).inTimeZone(timezone);
    }

}

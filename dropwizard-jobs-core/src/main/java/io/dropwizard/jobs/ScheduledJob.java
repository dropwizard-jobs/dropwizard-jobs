package io.dropwizard.jobs;

import org.quartz.Job;
import org.quartz.JobKey;
import org.quartz.Trigger;

public class ScheduledJob {
    
    private JobKey jobKey;
    private Class<? extends Job> clazz;
    private Trigger trigger;
    private boolean requestsRecovery;
    private boolean storeDurably;
    private String message;

    public ScheduledJob(JobKey jobKey, Class<? extends Job> clazz, Trigger trigger, boolean requestsRecovery, boolean storeDurably, String message) {
        this.jobKey = jobKey;
        this.clazz = clazz;
        this.trigger = trigger;
        this.requestsRecovery = requestsRecovery;
        this.storeDurably = storeDurably;
        this.message = message;
    }

    public JobKey getJobKey() {
        return jobKey;
    }

    public Class<? extends Job> getClazz() {
        return clazz;
    }

    public Trigger getTrigger() {
        return trigger;
    }

    public boolean isRequestsRecovery() {
        return requestsRecovery;
    }

    public boolean isStoreDurably() {
        return storeDurably;
    }

    public String getMessage() {
        return message;
    }

}
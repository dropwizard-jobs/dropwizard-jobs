package io.dropwizard.jobs;

/**
 * Metadata for a job, containing the job class and group name.
 * Used to avoid instantiating job instances for discovery purposes.
 * Optionally holds a pre-instantiated job instance for cases where
 * the job was already created (e.g., in JobsBundle).
 */
public class JobMetadata {
    private final Class<? extends Job> jobClass;
    private final String groupName;
    private final Job jobInstance;

    public JobMetadata(Class<? extends Job> jobClass, String groupName) {
        this.jobClass = jobClass;
        this.groupName = groupName;
        this.jobInstance = null;
    }

    public JobMetadata(Job jobInstance) {
        this.jobClass = jobInstance.getClass();
        this.groupName = jobInstance.getGroupName();
        this.jobInstance = jobInstance;
    }

    public Class<? extends Job> getJobClass() {
        return jobClass;
    }

    public String getGroupName() {
        return groupName;
    }

    /**
     * Returns the pre-instantiated job instance, if available.
     * This is used by DropwizardJobFactory to return existing instances
     * rather than creating new ones via reflection.
     *
     * @return the job instance, or null if not available
     */
    public Job getJobInstance() {
        return jobInstance;
    }
}
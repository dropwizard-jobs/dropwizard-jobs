package io.dropwizard.jobs;

import java.util.Map;

public class DefaultQuartzConfiguration {
    private static final Map<String, String> defaultQuartzProperties = Map.of(
        "org.quartz.scheduler.instanceName", "scheduler",
        "org.quartz.scheduler.instanceId", "AUTO",
        "org.quartz.scheduler.skipUpdateCheck", "true",
        "org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool",
        "org.quartz.threadPool.threadCount", "10",
        "org.quartz.threadPool.threadPriority", "5",
        "org.quartz.jobStore.misfireThreshold", "60000");

    public static Map<String, String> get() {
        return defaultQuartzProperties;
    }
}

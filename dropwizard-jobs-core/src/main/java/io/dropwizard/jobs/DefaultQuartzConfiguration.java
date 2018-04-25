package io.dropwizard.jobs;

import java.util.HashMap;
import java.util.Map;

public class DefaultQuartzConfiguration {
    private static Map<String, String> persistenceConfiguration;
    static {
        persistenceConfiguration = new HashMap<String, String>();
        persistenceConfiguration.put("org.quartz.scheduler.instanceName", "scheduler");
        persistenceConfiguration.put("org.quartz.scheduler.instanceId", "AUTO");
        persistenceConfiguration.put("org.quartz.scheduler.skipUpdateCheck", "true");
        persistenceConfiguration.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        persistenceConfiguration.put("org.quartz.threadPool.threadCount", "10");
        persistenceConfiguration.put("org.quartz.threadPool.threadPriority", "5");
        persistenceConfiguration.put("org.quartz.jobStore.misfireThreshold", "60000");
    }
    public static Map<String, String> get() {
        return persistenceConfiguration;
    }
}

package io.dropwizard.jobs;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.dropwizard.Configuration;

public class JobConfiguration extends Configuration {
    public Map<String, String> getJobs() {
        return Collections.emptyMap();
    }

    @JsonProperty("quartz")
    public Map<String, String> getQuartzConfiguration() {
        return Collections.emptyMap();
    }
}

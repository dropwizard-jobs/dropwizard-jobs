package io.dropwizard.jobs;

import java.util.Collections;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public interface JobConfiguration {
    default Map<String, String> getJobs() {
        return Collections.emptyMap();
    }

    @JsonProperty("quartz")
    default Map<String, String> getQuartzConfiguration() {
        return Collections.emptyMap();
    }
}

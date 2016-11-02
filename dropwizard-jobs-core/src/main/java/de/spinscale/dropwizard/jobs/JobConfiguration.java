package de.spinscale.dropwizard.jobs;

import java.util.Collections;
import java.util.Map;

public interface JobConfiguration {
    default Map<String, String> getJobs() {
        return Collections.emptyMap();
    }
}

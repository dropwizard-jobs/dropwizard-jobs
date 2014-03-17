package de.spinscale.dropwizard.jobs;

import com.codahale.metrics.SharedMetricRegistries;
import io.dropwizard.Bundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;


public class JobsBundle implements Bundle {

	protected String scanURL = null;
	
    @Override
    public void initialize(Bootstrap<?> bootstrap) {
        // add shared metrics registry to be used by Jobs, since defaultRegistry has been removed
        SharedMetricRegistries.add("dropwizard-jobs", bootstrap.getMetricRegistry());
    }
    
    public JobsBundle() {
    }
    
    public JobsBundle(String scanURL) {
    	this.scanURL = scanURL;
    }

    @Override
    public void run(Environment environment) {
    	environment.lifecycle().manage(new JobManager(scanURL));
    }

}

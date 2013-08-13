package de.spinscale.dropwizard.jobs;

import com.yammer.dropwizard.Bundle;
import com.yammer.dropwizard.config.Bootstrap;
import com.yammer.dropwizard.config.Environment;

public class JobsBundle implements Bundle {

	private String scanURL = null;
	
    @Override
    public void initialize(Bootstrap<?> bootstrap) {
    }
    
    public JobsBundle() {
    }
    
    public JobsBundle(String scanURL) {
    	this.scanURL = scanURL;
    }

    @Override
    public void run(Environment environment) {
    	environment.manage(new JobManager(scanURL));
    }

}

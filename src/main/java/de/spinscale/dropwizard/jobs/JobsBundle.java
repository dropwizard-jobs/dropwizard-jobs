package de.spinscale.dropwizard.jobs;

import com.codahale.dropwizard.Bundle;
import com.codahale.dropwizard.setup.Bootstrap;
import com.codahale.dropwizard.setup.Environment;


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
    	environment.getApplicationContext().manage(new JobManager(scanURL));
    }

}

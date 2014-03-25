package de.spinscale.dropwizard.jobs;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import de.spinscale.dropwizard.jobs.annotations.Every;

import java.util.Date;
import java.util.List;

@Every("1s")
public class DependencyTestJob extends Job {

	@Inject
	private Dependency dependency;
	
    public static List<String> results = Lists.newArrayList();
    
	@Override
    public void doJob() {
		if (dependency == null) throw new IllegalStateException("dependency is null");
        results.add(new Date().toString());
    }
}

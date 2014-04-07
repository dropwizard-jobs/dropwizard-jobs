package de.spinscale.dropwizard.jobs;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import com.google.common.collect.Lists;

import de.spinscale.dropwizard.jobs.annotations.Every;

@Every("1s")
public class DependencyTestJob extends Job {

    public static List<String> results = Lists.newArrayList();
    @Inject
    private Dependency dependency;

    @Override
    public void doJob() {
        if (dependency == null) {
            throw new IllegalStateException("dependency is null");
        }
        results.add(new Date().toString());
    }
}

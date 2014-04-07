package de.spinscale.dropwizard.jobs;

import com.google.common.collect.Lists;

import de.spinscale.dropwizard.jobs.annotations.Every;

import java.util.Date;
import java.util.List;

import javax.inject.Inject;

@Every("1s")
public class DependencyTestJob extends Job {

    @Inject
    private Dependency dependency;

    public static List<String> results = Lists.newArrayList();

    @Override
    public void doJob() {
        if (dependency == null)
            throw new IllegalStateException("dependency is null");
        results.add(new Date().toString());
    }
}

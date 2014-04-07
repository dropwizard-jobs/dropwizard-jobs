package de.spinscale.dropwizard.jobs;

import java.util.Date;
import java.util.List;

import com.google.common.collect.Lists;

import de.spinscale.dropwizard.jobs.annotations.OnApplicationStart;

@OnApplicationStart
public class ApplicationStartTestJob extends Job {

    public static final List<String> results = Lists.newArrayList();

    @Override
    public void doJob() {
        results.add(getClass().getName() + " " + new Date());
    }
}

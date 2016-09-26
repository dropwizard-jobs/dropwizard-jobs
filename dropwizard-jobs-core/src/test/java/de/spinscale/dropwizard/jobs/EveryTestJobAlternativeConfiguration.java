package de.spinscale.dropwizard.jobs;

import com.google.common.collect.Lists;
import de.spinscale.dropwizard.jobs.annotations.Every;

import java.util.Date;
import java.util.List;

@Every("${testJob}")
public class EveryTestJobAlternativeConfiguration extends Job {

    public static List<String> results = Lists.newArrayList();

    @Override
    public void doJob() {
        results.add(new Date().toString());
    }
}

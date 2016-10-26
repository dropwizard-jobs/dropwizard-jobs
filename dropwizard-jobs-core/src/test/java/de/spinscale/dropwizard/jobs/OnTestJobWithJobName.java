package de.spinscale.dropwizard.jobs;

import java.util.Date;
import java.util.List;

import com.google.common.collect.Lists;

import de.spinscale.dropwizard.jobs.annotations.On;

@On(value = "0/1 * * * * ?", jobName = "BarJob")
public class OnTestJobWithJobName extends Job {

    public static List<String> results = Lists.newArrayList();

    @Override
    public void doJob() {
        results.add(new Date().toString());
    }
}

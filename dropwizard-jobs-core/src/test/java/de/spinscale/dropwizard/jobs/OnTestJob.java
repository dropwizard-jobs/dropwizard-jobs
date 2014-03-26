package de.spinscale.dropwizard.jobs;

import com.google.common.collect.Lists;
import de.spinscale.dropwizard.jobs.annotations.On;

import java.util.Date;
import java.util.List;

@On("0/1 * * * * ?")
public class OnTestJob extends Job {

    public static List<String> results = Lists.newArrayList();

    @Override
    public void doJob() {
        results.add(new Date().toString());
    }
}

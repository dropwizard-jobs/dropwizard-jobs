package de.spinscale.dropwizard.jobs;

import com.google.common.collect.Lists;
import de.spinscale.dropwizard.jobs.annotations.OnApplicationStop;

import java.util.Date;
import java.util.List;

@OnApplicationStop
public class ApplicationStopTestJob extends Job {

    public static final List<String> results = Lists.newArrayList();

    @Override
    public void doJob() {
        results.add(getClass().getName() + " " + new Date());
    }

}

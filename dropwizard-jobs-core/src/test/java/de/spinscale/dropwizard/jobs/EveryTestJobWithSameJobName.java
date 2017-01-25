package de.spinscale.dropwizard.jobs;

import java.util.Date;
import java.util.List;

import com.google.common.collect.Lists;

import de.spinscale.dropwizard.jobs.annotations.Every;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

// this job has the same job name as EveryTestJobWithJobName. Only one will be created
@Every(value = "1s", jobName = "FooJob")
public class EveryTestJobWithSameJobName extends Job {

    public static List<String> results = Lists.newArrayList();

    @Override
    public void doJob(JobExecutionContext context) throws JobExecutionException {
        results.add(new Date().toString());
    }
}

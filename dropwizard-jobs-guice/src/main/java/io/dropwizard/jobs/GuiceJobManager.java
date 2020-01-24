package io.dropwizard.jobs;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import org.quartz.spi.JobFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GuiceJobManager extends JobManager {

    private final GuiceJobFactory jobFactory;

    public GuiceJobManager(JobConfiguration config, Injector injector) {
        super(config, getJobs(injector));
        jobFactory = new GuiceJobFactory(injector);
    }

    static Job[] getJobs(Injector injector) {
        List<Job> jobs = new ArrayList<>();
        Map<Key<?>, Binding<?>> bindings = injector.getBindings();
        for (Key<?> key : bindings.keySet()) {
            TypeLiteral<?> typeLiteral = key.getTypeLiteral();
            Class<?> clazz = typeLiteral.getRawType();
            if (Job.class.isAssignableFrom(clazz)) {
                jobs.add((Job) injector.getInstance(clazz));
            }
        }
        return jobs.toArray(new Job[]{});
    }


    @Override
    protected JobFactory getJobFactory() {
        return jobFactory;
    }
}

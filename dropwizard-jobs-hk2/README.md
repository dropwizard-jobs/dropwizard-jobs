# dropwizard-jobs-hk2 Integration

This is an extension for [dropwizard-jobs](https://github.com/dropwizard-jobs/dropwizard-jobs) to use
[HK2](https://javaee.github.io/hk2/) for dependency injection. HK2 is built into Dropwizard's core
through Jersey, so no extra dependency is needed beyond this module. This is especially handy when
you need to inject dependencies into Jobs.

This module uses [`Hk2JobManager`](src/main/java/io/dropwizard/jobs/Hk2JobManager.java) which leverages
[`JobMetadata`](../dropwizard-jobs-core/src/main/java/io/dropwizard/jobs/JobMetadata.java) to avoid
useless job instantiation during discovery. Jobs are discovered using `ServiceHandle` without creating
instances, and only instantiated when needed for scheduling.

## Using maven central repository

dropwizard-jobs-hk2 can be used with Maven. It is located in Central Repository: <https://search.maven.org/>

Add to your pom:

```xml
<dependency>
  <groupId>io.github.dropwizard-jobs</groupId>
  <artifactId>dropwizard-jobs-hk2</artifactId>
  <version>7.0.0</version> <!-- Replace with the latest version -->
</dependency>
```

## Installing from source

```bash
git clone https://github.com/dropwizard-jobs/dropwizard-jobs
cd dropwizard-jobs
./mvnw install
```

After installing locally, include the following dependency:

```xml
<dependency>
  <groupId>io.github.dropwizard-jobs</groupId>
  <artifactId>dropwizard-jobs-hk2</artifactId>
  <version>7.0.0</version> <!-- Replace with the latest version -->
</dependency>
```

## Activating the HK2 bundle

The [`Hk2JobsBundle`](src/main/java/io/dropwizard/jobs/Hk2JobsBundle.java) uses HK2's `Filter` to discover
jobs. The most common approach is to use `BuilderHelper.createContractFilter(Job.class.getName())` to find
all jobs bound to the `Job` contract.

```java
import org.glassfish.hk2.utilities.BuilderHelper;
import io.dropwizard.jobs.Hk2JobsBundle;
import io.dropwizard.jobs.Job;

@Override
public void initialize(Bootstrap<MyConfiguration> bootstrap) {
    bootstrap.addBundle(new Hk2JobsBundle(
        BuilderHelper.createContractFilter(Job.class.getName())
    ));
}

@Override
public void run(MyConfiguration configuration, Environment environment) {
    environment.jersey().register(new AbstractBinder() {
        @Override
        protected void configure() {
            // Register your jobs here
            bind(MyJob.class).to(Job.class).in(Singleton.class);
        }
    });
}
```

## Job registration via AbstractBinder

Jobs are registered with HK2 using an `AbstractBinder`. You can control the scope of your jobs:

```java
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import jakarta.inject.Singleton;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.PerThread;

environment.jersey().register(new AbstractBinder() {
    @Override
    protected void configure() {
        // Per-lookup scope (new instance each time)
        bind(MyPerLookupJob.class).to(Job.class);

        // Singleton scope (single instance shared)
        bind(MySingletonJob.class).to(Job.class).in(Singleton.class);

        // Per-thread scope
        bind(MyPerThreadJob.class).to(Job.class).in(PerThread.class);
    }
});
```

## Job Injection

Once the [`Hk2JobsBundle`](src/main/java/io/dropwizard/jobs/Hk2JobsBundle.java) has been added and your
jobs are registered, you can inject dependencies into your Jobs. A common usage is to inject constructor
arguments:

```java
import io.dropwizard.jobs.Job;
import io.dropwizard.jobs.annotations.Every;
import jakarta.inject.Inject;

@Every("1s")
public class InjectedJob extends Job {

    private final MyDependency dependency;

    @Inject
    public InjectedJob(MyDependency dependency) {
        this.dependency = dependency;
    }

    @Override
    public void doJob(JobExecutionContext context) {
        // use dependency
    }
}
```

## Job Listener support

The [`Hk2JobsBundle`](src/main/java/io/dropwizard/jobs/Hk2JobsBundle.java) supports optional
`JobListener` discovery via a second filter parameter:

```java
import org.glassfish.hk2.utilities.BuilderHelper;
import org.quartz.JobListener;

bootstrap.addBundle(new Hk2JobsBundle(
    BuilderHelper.createContractFilter(Job.class.getName()),
    BuilderHelper.createContractFilter(JobListener.class.getName())
));
```

Register your listeners in the same `AbstractBinder`:

```java
environment.jersey().register(new AbstractBinder() {
    @Override
    protected void configure() {
        bind(MyJob.class).to(Job.class).in(Singleton.class);
        bind(MyJobListener.class).to(JobListener.class).in(Singleton.class);
    }
});
```

## Key behavior notes

Jobs are discovered at **Jersey container startup**, not at bootstrap time. This is a deliberate
design choice that solves the dependency timing problem: the HK2 `ServiceLocator` is only available
after the Jersey container starts, so job discovery is deferred until `onStartup()` is called.

This means:

- `getScheduler()` returns `null` before the container has started
- Jobs are not scheduled until the application is fully initialized
- Dependencies injected into jobs will be fully available at job instantiation time

The bundle registers an `AbstractContainerLifecycleListener` internally to handle this lifecycle:

- On container startup: discovers jobs via `locator.getAllServiceHandles(searchCriteria)` and
    starts the [`JobManager`](../dropwizard-jobs-core/src/main/java/io/dropwizard/jobs/JobManager.java)
- On container shutdown: stops the [`JobManager`](../dropwizard-jobs-core/src/main/java/io/dropwizard/jobs/JobManager.java)

### Efficient Job Discovery

This module uses [`Hk2JobManager`](src/main/java/io/dropwizard/jobs/Hk2JobManager.java), which leverages
[`JobMetadata`](../dropwizard-jobs-core/src/main/java/io/dropwizard/jobs/JobMetadata.java) to avoid
instantiating all job classes during discovery:

1. Jobs are discovered using HK2's `ServiceHandle` without creating instances
2. Only one instance per unique job class is created (to get the group name)
3. [`JobMetadata`](../dropwizard-jobs-core/src/main/java/io/dropwizard/jobs/JobMetadata.java) is used
    for scheduling decisions
4. Actual job instances are created by HK2 when Quartz executes them

This approach minimizes unnecessary object creation and respects your configured HK2 scopes
(Singleton, PerLookup, PerThread).

## Architecture

This module follows the Factory/Manager/Bundle triple pattern used by all DI extensions:

- [`Hk2JobFactory`](src/main/java/io/dropwizard/jobs/Hk2JobFactory.java) - Quartz `JobFactory`
    that creates job instances via HK2
- [`Hk2JobManager`](src/main/java/io/dropwizard/jobs/Hk2JobManager.java) - Extends
    [`JobManager`](../dropwizard-jobs-core/src/main/java/io/dropwizard/jobs/JobManager.java) with
    HK2-specific job discovery
- [`Hk2JobsBundle`](src/main/java/io/dropwizard/jobs/Hk2JobsBundle.java) - Dropwizard bundle
    that wires everything together

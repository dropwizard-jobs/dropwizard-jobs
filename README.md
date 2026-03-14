# dropwizard-jobs

[![CodeFactor](https://www.codefactor.io/repository/github/dropwizard-jobs/dropwizard-jobs/badge)](https://www.codefactor.io/repository/github/dropwizard-jobs/dropwizard-jobs)
[![Maintainability](https://api.codeclimate.com/v1/badges/71ea62844095d88b2264/maintainability)](https://codeclimate.com/github/dropwizard-jobs/dropwizard-jobs/maintainability)

This plugin integrates the [quartz scheduler](https://quartz-scheduler.org/) with dropwizard and allows you to easily create background jobs, which are not bound to the HTTP request-response cycle.
Quartz creates a threadpool on application startup and uses it for background jobs.

There are four different types of jobs:

- Jobs run on application start for initial setup (could also be done via a managed instance in dropwizard)
- Jobs run at application stop before the application is closed (could also be done via managed instance in dropwizard)
- Jobs which are repeated after a certain time interval
- Jobs which need to run at a specific time, via a cron-like expression

## Using maven central repository

dropwizard jobs can be used with maven.
It is located in Central Repository. <https://search.maven.org/>

Add to your pom:

```xml
<dependency>
  <groupId>io.github.dropwizard-jobs</groupId>
  <artifactId>dropwizard-jobs-core</artifactId>
  <version>7.0.0</version> <!-- Replace with the latest version -->
</dependency>
```

## Installing the bundle from source code

```bash
git clone https://github.com/dropwizard-jobs/dropwizard-jobs
cd dropwizard-jobs
./mvn install
```

After installing the plugin locally you can include it in your pom.xml

```xml
<dependency>
  <groupId>io.github.dropwizard-jobs</groupId>
  <artifactId>dropwizard-jobs-core</artifactId>
  <version>$VERSION</version>
</dependency>
```

## Activating the bundle: Configuration

Your Dropwizard application configuration class must extend `JobConfiguration`:

```java
public class ApplicationConfiguration extends JobConfiguration {

...

}

```

## Activating the bundle: Initialization

In your application's `initialize` method, call `bootstrap.addBundle(new JobsBundle(<List of jobs>))`:

```java
@Override
public void initialize(Bootstrap<MyConfiguration> bootstrap) {
  SomeDependency dependency = new Dependency();
  Job startJob = new StartupJob();
  Job stopJob = new StopJob();
  Job everyJob = new EveryTestJob(dependency);
  bootstrap.addBundle(new JobsBundle(new ArrayList<>(startJob, stopJob, everyJob)));
}
```

## Available job types

The `@OnApplicationStart` annotation triggers a job after the quartz scheduler is started

```java
@OnApplicationStart
public class StartupJob extends Job {
  @Override
  public void doJob(JobExecutionContext context) throws JobExecutionException {
    // logic run once on application start
  }
}
```

The `@OnApplicationStop` annotation triggers a job when the application is stopped. Be aware that it is not guaranteed that this job is executed, in case the application is killed.

```java
@OnApplicationStop
public class StopJob extends Job {
  @Override
  public void doJob(JobExecutionContext context) throws JobExecutionException {
    // logic run once on application stop
  }
}
```

The `@Every` annotation first triggers a job after the quartz scheduler is started and then every n times, as it is configured. You can use a number and a time unit, which can be one of "s" for seconds, "m" or "mn" or "min" for minutes, "h" for hours, "d" for days and "ms" for milliseconds.
Use in conjunction with `@DelayStart` to delay the first invocation of this job.

```java
@Every("1s")
public class EveryTestJob extends Job {
  @Override
  public void doJob(JobExecutionContext context) throws JobExecutionException {
    // logic run every time and time again
  }
}
```

The `@DelayStart` annotation can be used in conjunction with @Every to delay the start of the job. Without this, all the @Every jobs start up at the same time when the scheduler starts.

```java
@DelayStart("5s")
@Every("1s")
public class EveryTestJobWithDelayedStart extends Job {
  @Override
  public void doJob(JobExecutionContext context) throws JobExecutionException {
    // logic run every time and time again
  }
}
```

The `@On` annotation allows one to use cron-like expressions for complex time settings. You can read more about possible cron expressions at <https://www.quartz-scheduler.org/documentation/quartz-2.3.0/tutorials/tutorial-lesson-06.html>

This expression would run on Mondays at 1pm, Los Angeles time. If the optional parameter `timeZone` is not set system default will be used.

```java
@On(value = "0 0 13 ? * MON", timeZone = "America/Los_Angeles")
public class OnTestJob extends Job {
  @Override
  public void doJob(JobExecutionContext context) throws JobExecutionException {
    // logic run via cron expression
  }
}
```

By default a class can only be scheduled once in the jobs bundle, this can be overridden by setting a unique `groupName` to the instance.

```java
@Every("15m")
public class GroupNameJob extends Job {
  public GroupNameJob(String groupName) {
      super(groupName);
  }
}
```

## Job Listeners

You can register Quartz [`JobListener`](https://www.quartz-scheduler.org/api/2.3.0/org/quartz/JobListener.html) implementations to react to job lifecycle events. Job listeners are notified when a job is about to execute, when execution is vetoed, and after a job has executed. This is useful for:

- Clearing thread-local values after job execution
- Adding reporting or metrics for job failures
- Implementing cross-cutting concerns for job execution

### Using the `@ListeningFor` Annotation

Create a class that implements `JobListener` and annotate it with `@ListeningFor`:

```java
@ListeningFor
public class MyJobListener implements JobListener {
    @Override
    public String getName() {
        return "MyJobListener";
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        // Called before job executes
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        // Called if job was vetoed by a trigger listener
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        // Called after job executes — check jobException for failures
        if (jobException != null) {
            // Handle failed job
        }
    }
}
```

### Matcher Types

The `@ListeningFor` annotation supports a `matcher` attribute and a `value` attribute to control which jobs the listener observes:

| Matcher Type | Description | Example |
|---|---|---|
| `ALL_JOBS` (default) | Listens to all jobs | `@ListeningFor` |
| `JOB_NAME_EQUALS` | Match specific job by name | `@ListeningFor(matcher = MatcherType.JOB_NAME_EQUALS, value = "myJob")` |
| `JOB_GROUP_EQUALS` | Match jobs in a specific group | `@ListeningFor(matcher = MatcherType.JOB_GROUP_EQUALS, value = "myGroup")` |
| `JOB_NAME_STARTS_WITH` | Match jobs whose name starts with | `@ListeningFor(matcher = MatcherType.JOB_NAME_STARTS_WITH, value = "report")` |
| `JOB_NAME_ENDS_WITH` | Match jobs whose name ends with | `@ListeningFor(matcher = MatcherType.JOB_NAME_ENDS_WITH, value = "Cleanup")` |
| `JOB_NAME_CONTAINS` | Match jobs whose name contains | `@ListeningFor(matcher = MatcherType.JOB_NAME_CONTAINS, value = "import")` |
| `JOB_GROUP_STARTS_WITH` | Match jobs whose group starts with | `@ListeningFor(matcher = MatcherType.JOB_GROUP_STARTS_WITH, value = "batch")` |
| `JOB_GROUP_ENDS_WITH` | Match jobs whose group ends with | `@ListeningFor(matcher = MatcherType.JOB_GROUP_ENDS_WITH, value = "Jobs")` |
| `JOB_GROUP_CONTAINS` | Match jobs whose group contains | `@ListeningFor(matcher = MatcherType.JOB_GROUP_CONTAINS, value = "critical")` |

### Registering Job Listeners

**Plain Dropwizard:**

Pass listeners to the [`JobsBundle`](dropwizard-jobs-core/src/main/java/io/dropwizard/jobs/JobsBundle.java) constructor:

```java
@Override
public void initialize(Bootstrap<MyConfiguration> bootstrap) {
    Job myJob = new MyJob();
    JobListener myListener = new MyJobListener();
    bootstrap.addBundle(new JobsBundle(List.of(myJob), List.of(myListener)));
}
```

**Guice:**

Register listeners as beans in your Guice module. The [`GuiceJobsBundle`](dropwizard-jobs-guice/src/main/java/io/dropwizard/jobs/GuiceJobsBundle.java) will auto-discover all `JobListener` implementations annotated with `@ListeningFor`:

```java
public class MyModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(MyJobListener.class).asEagerSingleton();
    }
}
```

**Note:** When using [`GuiceJobsBundle`](dropwizard-jobs-guice/src/main/java/io/dropwizard/jobs/GuiceJobsBundle.java) with frameworks like [dropwizard-guicey](https://github.com/xvik/dropwizard-guicey), you may need to use the deferred constructor that accepts a `Supplier<Injector>`. See the [Guice module documentation](dropwizard-jobs-guice/README.md) for details.

**Spring:**

Register listeners as beans in your Spring configuration. The [`SpringJobsBundle`](dropwizard-jobs-spring/src/main/java/io/dropwizard/jobs/SpringJobsBundle.java) will auto-discover all `JobListener` implementations annotated with `@ListeningFor`:

```java
@Configuration
public class MyConfiguration {
    @Bean
    public MyJobListener myJobListener() {
        return new MyJobListener();
    }
}
```

## Using dropwizard-jobs in a Clustered Environment

By default, dropwizard-jobs is designed to be used with an in-memory Quartz scheduler. If you wish to deploy it in a clustered environment that consists of more than one node, you'll need to use a scheduler that has some sort of persistence. You can either add a file called `quartz.properties` to your classpath or you can provide the quartz configuration in your Dropwizard configuration file. The content of the `quartz` element is passed to the Quartz scheduler directly (so you can take the properties from the official docs). If you'd like to add the config to your Dropwizard configuration file, you need to override the `getQuartzConfiguration()` method in your application's configuration. You can set the map to `DefaultQuartzConfiguration.get()`.

See the full Quartz configuration reference at <https://www.quartz-scheduler.org/documentation/quartz-2.x/configuration/>

```yaml
[...]
quartz:
  org.quartz.scheduler.instanceName: "scheduler"
  org.quartz.scheduler.instanceId: "AUTO"
  org.quartz.scheduler.skipUpdateCheck: "true"
  org.quartz.threadPool.class: "org.quartz.simpl.SimpleThreadPool"
  org.quartz.threadPool.threadCount: "10"
  org.quartz.threadPool.threadPriority: "5"
  org.quartz.jobStore.misfireThreshold: "60000"
  org.quartz.jobStore.class: "org.quartz.impl.jdbcjobstore.JobStoreTX"
  org.quartz.jobStore.driverDelegateClass: "org.quartz.impl.jdbcjobstore.StdJDBCDelegate"
  org.quartz.jobStore.useProperties: "false"
  org.quartz.jobStore.dataSource: "myDS"
  org.quartz.jobStore.tablePrefix: "QRTZ_"
  org.quartz.jobStore.isClustered: "true"
  org.quartz.dataSource.myDS.driver: "com.mysql.cj.jdbc.Driver"
  org.quartz.dataSource.myDS.URL: "jdbc:mysql://localhost:3306/quartz"
  org.quartz.dataSource.myDS.user: "fami"
  # NOTE: Use environment variables or a secrets manager for sensitive values
  org.quartz.dataSource.myDS.password: "<YOUR_PASSWORD>"
  org.quartz.dataSource.myDS.maxConnections: "5"
  org.quartz.dataSource.myDS.validationQuery: "select 1"
```

When you do this, dropwizard-jobs will ensure that only one instance of each job is scheduled, regardless of the number of nodes in your cluster by using the fully-qualified class name of your job implementation as the name of your job. For example, if your job implementation resides in a class called `MyJob`, which in turn is located in the package `com.my.awesome.web.app`, then the name of your job (so far as Quartz is concerned) will be `com.my.awesome.web.app.MyJob`.

If you wish to override the default name that dropwizard-jobs assigns to your job, you can do so by setting the `jobName` property in the `@Every` or `@On` annotation like so:

```java
package com.my.awesome.web.app

/**
 * This job will be given the name "MyJob" instead of the name "com.my.awesome.web.app.MyJob"
 */
@Every(value="5s", jobName="MyJob")
public class MyJob extends Job {
  @Override
  public void doJob(JobExecutionContext context) throws JobExecutionException {
    // do some work here
  }
}
```

This property is not supported in the `@OnApplicationStart` or `@ApplicationStop` annotations, as they
are designed for jobs that will fire reliably when Dropwizard starts or stops your web application.
As such, jobs annotated with `@OnApplicationStart` or `@OnApplicationStop` will be given unique names,
and will be fired according to schedule on every node in your cluster.

## Programmatic Quartz Configuration

In some cases, such as when writing integration tests with dynamic infrastructure (e.g. Testcontainers),
you may need to configure Quartz properties programmatically using values that are only known at runtime.
However, Dropwizard's `ConfigOverride.config()` method interprets periods as hierarchical separators,
which breaks Quartz property keys like `org.quartz.dataSource.myDS.URL` that must remain as flat string keys.

The [`MutableJobConfiguration`](dropwizard-jobs-core/src/main/java/io/dropwizard/jobs/MutableJobConfiguration.java)
class solves this problem by providing a fluent API for programmatic configuration. It extends
[`JobConfiguration`](dropwizard-jobs-core/src/main/java/io/dropwizard/jobs/JobConfiguration.java) and allows
you to build the configuration map directly without going through Dropwizard's config override mechanism.

### The Problem

This doesn't work because Dropwizard treats the periods as nested properties:

```java
// ❌ This DOESN'T WORK - Dropwizard interprets periods as hierarchy
ConfigOverride.config("org.quartz.dataSource.myDS.URL", postgresContainer.getJdbcUrl())
```

### The Solution

Use `MutableJobConfiguration` to build the configuration programmatically:

```java
PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
postgres.start();

MutableJobConfiguration config = new MutableJobConfiguration()
    .withQuartzProperty("org.quartz.jobStore.class", "org.quartz.impl.jdbcjobstore.JobStoreTX")
    .withQuartzProperty("org.quartz.jobStore.driverDelegateClass", "org.quartz.impl.jdbcjobstore.PostgreSQLDelegate")
    .withQuartzProperty("org.quartz.jobStore.dataSource", "myDS")
    .withQuartzProperty("org.quartz.dataSource.myDS.driver", postgres.getDriverClassName())
    .withQuartzProperty("org.quartz.dataSource.myDS.URL", postgres.getJdbcUrl())
    .withQuartzProperty("org.quartz.dataSource.myDS.user", postgres.getUsername())
    .withQuartzProperty("org.quartz.dataSource.myDS.password", postgres.getPassword())
    .withQuartzProperty("org.quartz.dataSource.myDS.maxConnections", "5");

JobManager jobManager = new JobManager(config, jobs);
jobManager.start();
```

### Copying from Existing Configuration

You can also create a `MutableJobConfiguration` from an existing configuration and overlay
additional properties:

```java
// Start with configuration loaded from YAML
JobConfiguration yamlConfig = applicationConfiguration;

// Create a mutable copy and add dynamic properties
MutableJobConfiguration config = new MutableJobConfiguration(yamlConfig)
    .withQuartzProperty("org.quartz.dataSource.myDS.URL", dynamicJdbcUrl)
    .withJob("MyCustomJob", "30s");  // Override job schedule
```

### Available Methods

- **`withQuartzProperty(String key, String value)`** - Add a single Quartz property
- **`withQuartzProperties(Map<String, String> properties)`** - Add multiple Quartz properties
- **`withJob(String jobName, String schedule)`** - Add a job schedule override
- **`setQuartzConfiguration(Map<String, String> config)`** - Replace the entire Quartz configuration
- **`setJobs(Map<String, String> jobs)`** - Replace the entire jobs map
- **`clearQuartzConfiguration()`** - Clear all Quartz properties
- **`clearJobs()`** - Clear all job overrides

All methods (except `clear*`) return `this` for fluent method chaining.

## Configuring jobs in the Dropwizard Config File

The period for `@Every` and `@On` jobs can be read from the dropwizard config file instead of being hard-coded. The YAML looks like this:

```yaml
jobs:
  myJob: 10s
  myOtherJob: 20s
  cronJob: "0 0/3 0 ? * * * [Europe/London]"
```

For `@On` jobs, the cron expression can have an optional timezone specified in square brackets.
If no timezone is given, the `de.spinscale.dropwizard.jobs.timezone` system property will be used,
otherwise your server's default timezone will apply.

Where MyJob and MyOtherJob are the names of Job classes in the application. In the `Configuration` class add the corresponding property:

```java
@JsonProperty("jobs")
private Map<String , String> jobs;

public Map<String, String> getJobs() {
    return jobs;
}

public void setJobs(Map<String, String> jobs) {
    this.jobs = jobs;
}
```

Then the `@Every` annotation can be used without a value, its value is set from the configuration:

```java
@Every
public class MyJob extends Job {
    ...
}
```

An alternative label to the class name can also be specified:

```java
@Every("${foobar}")
public class MyJob extends Job {
    ...
}
```

The same can be done with the `@On` annotation as well, as second option to cron-base jobs configuration
An alternative label to the class name can also be specified:

```java
@On("${cronJob}")
public class MyJob extends Job {
    ...
}
```

So long as there is a matching property in the YAML:

```yaml
jobs:
  foobar: 10s
```

## Thanks

- The playframework 1.x for the idea of simple annotations at Job classes

## Contributors

- [Alexander Reelsen](https://github.com/spinscale)
- [Hakan Dilek](https://github.com/hakandilek)
- [Yun Zhi Lin](https://github.com/yunspace)
- [Eyal Golan](https://github.com/eyalgo)
- [Jonathan Fritz](https://github.com/MusikPolice)
- [Ahsan Rabbani](https://github.com/xargsgrep)

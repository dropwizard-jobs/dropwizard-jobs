[![Build Status](https://travis-ci.org/dropwizard-jobs/dropwizard-jobs.svg?branch=master)](https://travis-ci.org/dropwizard-jobs/dropwizard-jobs)
[![DepShield Badge](https://depshield.sonatype.org/badges/dropwizard-jobs/dropwizard-jobs/depshield.svg)](https://depshield.github.io)
[![CodeFactor](https://www.codefactor.io/repository/github/dropwizard-jobs/dropwizard-jobs/badge)](https://www.codefactor.io/repository/github/dropwizard-jobs/dropwizard-jobs)
[![Maintainability](https://api.codeclimate.com/v1/badges/71ea62844095d88b2264/maintainability)](https://codeclimate.com/github/dropwizard-jobs/dropwizard-jobs/maintainability)
[![Test Coverage](https://api.codeclimate.com/v1/badges/71ea62844095d88b2264/test_coverage)](https://codeclimate.com/github/dropwizard-jobs/dropwizard-jobs/test_coverage)
# Dropwizard quartz integration

This plugin integrates the [quartz scheduler](http://quartz-scheduler.org/) with dropwizard and allows you to easily create background jobs, which are not bound to the HTTP request-response cycle.
Quartz creates a threadpool on application startup and uses it for background jobs.

There are four different types of jobs:

* Jobs run on application start for initial setup (could also be done via a managed instance in dropwizard)
* Jobs run at application stop before the application is closed (could also be done via managed instance in dropwizard)
* Jobs which are repeated after a certain time interval
* Jobs which need to run at a specific time, via a cron-like expression

## Using maven central repository
dropwizard jobs can be used with maven.
It is located in Central Repository. http://search.maven.org/

Add to your pom:
```xml
<dependency>
  <groupId>io.github.dropwizard-jobs</groupId>
  <artifactId>dropwizard-jobs</artifactId>
  <version>4.0.0-RELEASE</version>
</dependency>
```

## API changes from 3.x
The 3.x release has breaking API changes that would need to be addressed if upgrading from an older version. The
signature of the <code>doJob()</code> method has changed and now takes a <code>JobExecutionContext</code> as an
argument and also throws a <code>JobExecutionException</code>.

##### 3.x
```java
  @Override
  public void doJob(JobExecutionContext context) throws JobExecutionException {
    ...
  }
```

##### < 3.x
```java
  @Override
  public void doJob() {
    ...
  }
```


## Installing the bundle from source code

```
git clone https://github.com/dropwizard-jobs/dropwizard-jobs
cd dropwizard-jobs
./mvn install
```

After installing the plugin locally you can include it in your pom.xml

```xml
<dependency>
  <groupId>io.github.dropwizard-jobs</groupId>
  <artifactId>dropwizard-jobs</artifactId>
  <version>$VERSION</version>
</dependency>
```

## Activating the bundle: Configuration

Your Dropwizard application configuration class must implement `JobConfiguration`:

```java
public class ApplicationConfiguration extends Configuration implements JobConfiguration {

...

}

```

By default, `JobConfiguration` will return an empty configuration. If you want to allow configuring Quartz from your Dropwizard YML config file (recommended), then implement the `getQuartzConfiguration` method:

```java
public class ApplicationConfiguration extends Configuration implements JobConfiguration {

...
    @JsonProperty("quartz")
    public Map<String,String> quartz;

    @Override
    public Map<String,String> getQuartzConfiguration() {
        return quartz;
    }
}
```

## Activating the bundle: Initialization

In your application's `initialize` method, call `bootstrap.addBundle(new JobsBundle(<list of jobs>))`:

```java
@Override
public void initialize(Bootstrap<MyConfiguration> bootstrap) {
  SomeDependency dependency = new Dependency();
  Job startJob = new StartupJob();
  Job stopJob = new StopJob();
  Job everyJob = new EveryTestJob(dependency);
  bootstrap.addBundle(new JobsBundle(startJob, stopJob, everyJob));
}
```

## Available job types

The <code>@OnApplicationStart</code> annotation triggers a job after the quartz scheduler is started

```java
@OnApplicationStart
public class StartupJob extends Job {
  @Override
  public void doJob(JobExecutionContext context) throws JobExecutionException {
    // logic run once on application start
  }
}
```

The <code>@OnApplicationStop</code> annotation triggers a job when the application is stopped. Be aware that it is not guaranteed that this job is executed, in case the application is killed.

```java
@OnApplicationStop
public class StopJob extends Job {
  @Override
  public void doJob(JobExecutionContext context) throws JobExecutionException {
    // logic run once on application stop
  }
}
```

The <code>@Every</code> annotation first triggers a job after the quartz scheduler is started and then every n times, as it is configured. You can use a number and a time unit, which can be one of "s" for seconds, "m" or "mn" or "min" for minutes, "h" for hours, "d" for days and "ms" for milliseconds.
Use in conjunction with <code>@DelayStart</code> to delay the first invocation of this job.

```java
@Every("1s")
public class EveryTestJob extends Job {
  @Override
  public void doJob(JobExecutionContext context) throws JobExecutionException {
    // logic run every time and time again
  }
}
```

The <code>@DelayStart</code> annotation can be used in conjunction with @Every to delay the start of the job. Without this, all the @Every jobs start up at the same time when the scheduler starts.

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

The <code>@On</code> annotation allows one to use cron-like expressions for complex time settings. You can read more about possible cron expressions at http://quartz-scheduler.org/documentation/quartz-2.1.x/tutorials/tutorial-lesson-06

This expression would run on Mondays at 1pm, Los Angeles time. If the optional parameter `timeZone` is not set system default will be used. 

```java
@On("0 0 13 ? * MON", timeZone = "America/Los_Angeles")
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

## Using dropwizard-jobs in a Clustered Environment

By default, dropwizard-jobs is designed to be used with an in-memory Quartz scheduler. If you wish to deploy it in a clustered environment that consists of more than one node, you'll need to use a scheduler that has some sort of persistence. You can either add a file called `quartz.properties` to your classpath or you can provide the quartz configuration in your Dropwizard configuration file. The content of the `quartz` element is passed to the Quartz scheduler directly (so you can take the properties from the official docs). If you'd like to add the config to your Dropwizard configuration file, you need to override the `getQuartzConfiguration()` method in your application's configuration. You can set the map to `DefaultQuartzConfiguration.get()`.

See the full Quartz configuration reference at http://www.quartz-scheduler.org/documentation/quartz-2.x/configuration/
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
  org.quartz.dataSource.myDS.password: "ageClXl5mrSg"
  org.quartz.dataSource.myDS.maxConnections: "5"
  org.quartz.dataSource.myDS.validationQuery: "select 1"
```

When you do this, dropwizard-jobs will ensure that only one instance of each job is scheduled, regardless of the number of nodes in your cluster by using the fully-qualified class name of your job implementation as the name of your job. For example, if your job implementation resides in a class called `MyJob`, which in turn is located in the package `com.my.awesome.web.app`, then the name of your job (so far as Quartz is concerned) will be `com.my.awesome.web.app.MyJob`.

If you wish to override the default name that dropwizard-jobs assigns to your job, you can do so by setting the `jobName` property in the `@Every` or `@On` annotation like so:

```Java
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
This property is not supported in the `@OnApplicationStart` or `@ApplicationStop` annotations, as they are designed for jobs that will fire reliably when Dropwizard starts or stops your web application. As such, jobs annotated with `@OnApplicationStart` or `@OnApplicationStop` will be given unique names, and will be fired according to schedule on every node in your cluster.

## Configuring jobs in the Dropwizard Config File

The period for `@Every` and `@On` jobs can be read from the dropwizard config file instead of being hard-coded. The YAML looks like this:

```
jobs:
  myJob: 10s
  myOtherJob: 20s
  cronJob: "0 0/3 0 ? * * * [Europe/London]"
```

For `@On` jobs, the cron expression can have an optional timezone specified in square brackets.
If no timezone is given, the `de.spinscale.dropwizard.jobs.timezone` system property will be used,
otherwise your server's default timezone will apply.

Where MyJob and MyOtherJob are the names of Job classes in the application. In the <code>Configuration</code> class add the corresponding property:

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

Then the <code>@Every</code> annotation can be used without a value, its value is set from the configuration:

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
The same can be done with the <code>@On</code> annotation as well, as second option to cron-base jobs configuration
An alternative label to the class name can also be specified:

```java
@On("${cronJob}")
public class MyJob extends Job {
    ...
}
```

So long as there is a matching property in the YAML:

```
jobs:
  foobar: 10s
```

# Limitations
* Configuration mechanism is still in early stages. Might be enhanced in the future.

# Thanks

* The playframework 1.x for the idea of simple annotations at Job classes

# Contributors
 * [Alexander Reelsen](https://github.com/spinscale)
 * [Hakan Dilek](https://github.com/hakandilek)
 * [Yun Zhi Lin](https://github.com/yunspace)
 * [Eyal Golan](https://github.com/eyalgo)
 * [Jonathan Fritz](https://github.com/MusikPolice)
 * [Ahsan Rabbani](https://github.com/xargsgrep)

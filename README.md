[![Build Status](https://travis-ci.org/spinscale/dropwizard-jobs.svg?branch=master)](https://travis-ci.org/spinscale/dropwizard-jobs)

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
  <groupId>de.spinscale.dropwizard</groupId>
  <artifactId>dropwizard-jobs-core</artifactId>
  <version>2.0.0</version>
</dependency>
```

## Installing the bundle from source code

```
git clone https://github.com/spinscale/dropwizard-jobs
cd dropwizard-jobs
mvn install
```

After installing the plugin locally you can include it in your pom.xml

```xml
<dependency>
  <groupId>de.spinscale.dropwizard</groupId>
  <artifactId>dropwizard-jobs</artifactId>
  <version>$VERSION</version>
</dependency>
```

## Activating the bundle

Similar to the AssetsBundle or the ViewBundle you need to activate the JobsBundle class.

### Usage

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
  public void doJob() {
    // logic run once on application start
  }
}
```

The <code>@OnApplicationStop</code> annotation triggers a job when the application is stopped. Be aware that it is not guaranteed that this job is executed, in case the application is killed.

```java
@OnApplicationStop
public class StopJob extends Job {
  @Override
  public void doJob() {
    // logic run once on application stop
  }
}
```

The <code>@Every</code> annotation first triggers a job after the quartz scheduler is started and then every n times, as it is configured. You can use a number and a time unit, which can be one of "s" for seconds, "mn" or "min" for minutes, "h" for hours and "d" for days.
Use in conjunction with <code>@DelayStart</code> to delay the first invocation of this job.

```java
@Every("1s")
public class EveryTestJob extends Job {
  @Override
  public void doJob() {
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
  public void doJob() {
    // logic run every time and time again
  }
}
```

The <code>@On</code> annotation allows one to use cron-like expressions for complex time settings. You can read more about possible cron expressions at http://quartz-scheduler.org/documentation/quartz-2.1.x/tutorials/tutorial-lesson-06

This expression allows you to run a job every 15 seconds and could possibly also be run via a @Every annotation.

```java
@On("0/15 * * * * ?")
public class OnTestJob extends Job {

  @Override
  public void doJob() {
    // logic run via cron expression
  }
}
```

## Using dropwizard-jobs in a Clustered Environment

By default, dropwizard-jobs is designed to be used with an in-memory Quartz scheduler. If you wish to deploy it in a clustered environment that consists of more than one node, you'll need to use a scheduler that has some sort of persistence. Adding a file called `quartz.properties` to your classpath that looks something like this will do the trick:

```
# See the full Quartz configuration reference at http://www.quartz-scheduler.org/documentation/quartz-2.x/configuration/

# define the scheduler and how many threads it ought to use
org.quartz.scheduler.instanceName = MyScheduler
org.quartz.scheduler.instanceId = AUTO
org.quartz.threadPool.threadCount = 1

# instances will synchronize their schedulers by communicating via a postgresql database
# this ensures that scheduled jobs won't be run by two or more nodes at the same time
org.quartz.jobStore.class = org.quartz.impl.jdbcjobstore.JobStoreTX
org.quartz.jobStore.driverDelegateClass = org.quartz.impl.jdbcjobstore.PostgreSQLDelegate
org.quartz.jobStore.dataSource = my-datasource
org.quartz.jobStore.isClustered = true
org.quartz.jobStore.tablePrefix = quartz.qrtz_

# specifies how to connect to the database
# the database tables that quartz requires must be created in advance, and must reside in the quartz schema
org.quartz.dataSource.aeryon-live-datasource.driver = org.postgresql.Driver
org.quartz.dataSource.aeryon-live-datasource.URL = # DATABASE CONNECTION STRING
org.quartz.dataSource.aeryon-live-datasource.user = # USERNAME
org.quartz.dataSource.aeryon-live-datasource.password = # PASSWORD
org.quartz.dataSource.aeryon-live-datasource.maxConnections = 1
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
  public void doJob() {
    // do some work here
  } 
}
```
This property is not supported in the `@OnApplicationStart` or `@ApplicationStop` annotations, as they are designed for jobs that will fire reliably when Dropwizard starts or stops your web application. As such, jobs annotated with `@OnApplicationStart` or `@OnApplicationStop` will be given unique names, and will be fired according to schedule on every node in your cluster.

## Configuring jobs in the Dropwizard Config File

As of 1.0.2, the period for @Every jobs can be read from the dropwizard config file instead of being hard-coded. The YAML looks like this:

```
jobs:
  myJob: 10s
  myOtherJob: 20s
```
  
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

So long as there is a matching property in the YAML:

```
jobs:
  foobar: 10s
```

# Usage with Dropwizard

```java
public class ExampleApplication extends Application<ApplicationConfiguration> {

    @Override
    public void initialize(final Bootstrap<ApplicationConfiguration> bootstrap) {
        Logger jobLogger = LoggerFactory.getLogger("jobs");
        bootstrap.addBundle(new JobsBundle(new StartJob(jobLogger), new StopJob(jobLogger)));
    }

}
```

This also means, that your `ApplicationConfiguration` needs to implement `JobConfiguration`

```java
public class ApplicationConfiguration extends Configuration implements JobConfiguration {

...

}

```

# Limitations

* The jobs are not persisted, but purely in memory (though quartz can do different), so shutting down your dropwizard service at a certain time might lead to not run the job.
* The scheduler is not configurable at the moment, for example the threadpool size is fixed to ten.

# Thanks

* The playframework 1.x for the idea of simple annotations at Job classes

# Contributors
 * [Alexander Reelsen](https://github.com/spinscale)
 * [Hakan Dilek](https://github.com/hakandilek)
 * [Yun Zhi Lin](https://github.com/yunspace)
 * [Eyal Golan](https://github.com/eyalgo)
 * [Jonathan Fritz](https://github.com/MusikPolice)


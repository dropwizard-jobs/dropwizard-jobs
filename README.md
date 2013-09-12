# Dropwizard quartz integration

This plugin integrates the [quartz scheduler](http://quartz-scheduler.org/) with dropwizard and allows you to easily create background jobs, which are not bound to the HTTP request-response cycle.
Quartz creates a threadpool on application startup and uses it for background jobs.

There are four different types of jobs:

* Jobs run on application start for initial setup (could also be done via a managed instance in dropwizard)
* Jobs run at application stop before the application is closed (could also be done via managed instance in dropwizard)
* Jobs which are repeated after a certain time interval
* Jobs which need to run at a specific time, via a cron-like expression

## Installing the bundle

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
  <version>1.0-SNAPSHOT</version>
</dependency>
```

## Activating the bundle

Similar to the AssetsBundle or the ViewBundle you need to activate the JobsBundle class.

### Dropwizard 0.5.x

Integration with dropwizard 0.5.x is done in the constructor of the service class, like in the [ViewBundle](http://dropwizard.codahale.com/manual/views/)

```java
public MyService() {
    super("my-service");
    addBundle(new JobsBundle());
}
```

### Dropwizard 0.6.0-SNAPSHOT

Because I am using dropwizard 0.6.0-SNAPSHOT in my current project, this is how it is integrated there

```java
@Override
public void initialize(Bootstrap<DelaSearchConfiguration> bootstrap) {
  bootstrap.setName("myService");
  bootstrap.addBundle(new JobsBundle());
}
```

Be aware that Jobs are searched by reflection only in the current package. 
You can define jobs location by passing package url to the JobsBundle constructor like this:

```java
  bootstrap.addBundle(new JobsBundle('com.youpackage.url'));
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

The <code>@Every</code> annotation triggers a job every n times, as it is configured. You can use a number and a time unit, which can be one of "s" for seconds, "mn" or "min" for minutes, "h" for hours and "d" for days.
This job has the severe limitation, that if you schedule a job for each day, but restart the service twice a day, your job will never run. You better use this type for short running repeated tasks.

```java
@Every("1s")
public class EveryTestJob extends Job {
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

# Limitations

* Your jobs have to have a no-args constructor
* The jobs are not persisted, but purely in memory (though quartz can do different), so shutting down your dropwizard service at a certain time might lead to not run the job.
* The scheduler is not configurable at the moment, for example the threadpool size is fixed to ten.
* If you run the same dropwizard service on multiple instances, you also run the same jobs twice. This might not be what you want

# TODO

* I hacked this in a few hours in the evening, so rather see it as a prototype.
* Ask the community whether this is useful. It seems, it makes more sense that you use a DI container like Guice in order to inject daos or other persistence layers into the jobs, as you really want to do store stuff.

# Thanks

* The playframework 1.x for the idea of simple annotations at Job classes

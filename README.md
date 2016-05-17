# Fork #

This is a fork used simply to publish dropwizard-jobs to a public maven repository.

The repository is available at https://nexus.vanntett.net/content/repositories/dropwizard-jobs/

I will keep this repository updated as needed. I have also reached out to the original dev to grant him access to publish to the repository if wanted.

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
  <groupId>de.spinscale</groupId>
  <artifactId>dropwizard-jobs-core</artifactId>
  <version>1.0.1</version>
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
  <version><current version></version>
</dependency>
```

## Activating the bundle

Similar to the AssetsBundle or the ViewBundle you need to activate the JobsBundle class.

### Dropwizard 0.7.0

```java
@Override
public void initialize(Bootstrap<DelaSearchConfiguration> bootstrap) {
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

To configure the jobs, you must call the bundle's <code>configure()</code> method in the application's <code>run()</code> method:

```java
private GuiceJobsBundle guiceJobsBundle;

public void run(MyConfiguration config, Environment env) throws Exception {
    guiceJobsBundle.configure(config);
    ...
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


# Limitations

* Your jobs have to have a no-args constructor, unless you use Dependency Injection.
* The jobs are not persisted, but purely in memory (though quartz can do different), so shutting down your dropwizard service at a certain time might lead to not run the job.
* The scheduler is not configurable at the moment, for example the threadpool size is fixed to ten.
* If you run the same dropwizard service on multiple instances, you also run the same jobs twice. This might not be what you want

# TODO

* I hacked this in a few hours in the evening, so rather see it as a prototype.
* Ask the community whether this is useful. It seems, it makes more sense that you use a DI container like Guice in order to inject daos or other persistence layers into the jobs, as you really want to do store stuff.

# Thanks

* The playframework 1.x for the idea of simple annotations at Job classes

# Contributors
 * [Alexander Reelsen](https://github.com/spinscale)
 * [Hakan Dilek](https://github.com/hakandilek)
 * [Yun Zhi Lin](https://github.com/yunspace)
 * [Eyal Golan](https://github.com/eyalgo)


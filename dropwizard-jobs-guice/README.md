# Dropwizard quartz integration with Guice

This is a extension for [dropwizard-jobs](https://github.com/dropwizard-jobs/dropwizard-jobs) to use
[Google Guice](https://code.google.com/p/google-guice/) to provide Dependency Injection. This is especially handy when you need to inject
arguments into Jobs.

## Using maven central repository

dropwizard jobs can be used with maven.
It is located in Central Repository [https://search.maven.org/](https://search.maven.org/)

Add to your pom:

```xml
<dependency>
  <groupId>io.github.dropwizard-jobs</groupId>
  <artifactId>dropwizard-jobs-guice</artifactId>
  <version>7.0.0</version> <!-- Replace with the latest version -->
</dependency>
```

## Installing the bundle from source code

## Installing the bundle from source

```bash
git clone https://github.com/dropwizard-jobs/dropwizard-jobs
cd dropwizard-jobs
mvn install
```

After installing the plugin locally, include the following dependencies only:

```xml
<dependency>
  <groupId>io.github.dropwizard-jobs</groupId>
  <artifactId>dropwizard-jobs-guice</artifactId>
  <version>7.0.0</version> <!-- Replace with the latest version -->
</dependency>
<dependency>
  <groupId>com.hubspot.dropwizard</groupId>
  <artifactId>dropwizard-guice</artifactId>
  <version>0.7.1</version>
</dependency>
```

dropwizard-jobs-core and other dependencies will be resolved automaticlaly.

## Activating the Guice and GuiceJobs bundle

[`GuiceJobsBundle`](src/main/java/io/dropwizard/jobs/GuiceJobsBundle.java) supports two modes of operation depending on when the Guice Injector becomes available.

### Eager Mode (Traditional)

Use this when you create the Guice Injector yourself during the initialization phase:

```java
@Override
public void initialize(Bootstrap<YourConfiguration> bootstrap) {
  Injector injector = Guice.createInjector(new YourModule());
  bootstrap.addBundle(new GuiceJobsBundle(injector));
}
```

### Deferred Mode (For dropwizard-guicey Compatibility)

Use this when working with [dropwizard-guicey](https://github.com/xvik/dropwizard-guicey) or other frameworks where the Injector is not available until the run phase. Pass a `Supplier<Injector>` that will be called during the managed lifecycle start:

```java
@Override
public void initialize(Bootstrap<YourConfiguration> bootstrap) {
  GuiceBundle guiceBundle = GuiceBundle.<YourConfiguration>newBuilder()
    .addModule(new YourModule())
    .enableAutoConfig(getClass().getPackage().getName())
    .setConfigClass(YourConfiguration.class)
    .build();
  bootstrap.addBundle(guiceBundle);

  // Use a Supplier to defer injector resolution until Managed.start()
  bootstrap.addBundle(new GuiceJobsBundle(() -> guiceBundle.getInjector()));
}
```

**Why use deferred mode?**

Some Guice integration libraries (like dropwizard-guicey) don't create the Injector until the `run()` phase, after all bundles have been initialized. The deferred constructor allows [`GuiceJobsBundle`](src/main/java/io/dropwizard/jobs/GuiceJobsBundle.java) to work with these frameworks by deferring injector resolution until `Managed.start()`, when the injector is guaranteed to be available.

**Note:** In deferred mode, `getScheduler()` returns `null` until the bundle has started (i.e., until the Dropwizard lifecycle has completed startup).

## Job Injection

Once the GuiceBundle and GuiceJobsBundle have been added, you can now inject dependencies into your Jobs.
A common usage is to inject constructor arguments:

```java
@Every("1s")
public class InjectedJob extends Job {
  @Inject
  public InjectedJob(Object dependency) {
    // do some logic with dependency
  }

}
```

See [Dropwizard-Guice](https://github.com/HubSpot/dropwizard-guice) for more information on how to use
Guice and Modules with Dropwizard.

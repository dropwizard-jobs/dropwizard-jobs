# Dropwizard quartz integration with Guice

This is a extension for [dropwizard-jobs](https://github.com/dropwizard-jobs/dropwizard-jobs) to use
[Google Guice] (https://code.google.com/p/google-guice/) to provide Dependency Injection. This is especially handy when you need to inject
arguments into Jobs.

## Using maven central repository
dropwizard jobs can be used with maven.
It is located in Central Repository. http://search.maven.org/

Add to your pom:
```xml
<dependency>
  <groupId>io.github.dropwizard-jobs</groupId>
  <artifactId>dropwizard-jobs-guice</artifactId>
  <version>4.0.0</version>
</dependency>
```

## Installing the bundle from source code

## Installing the bundle from source

```
git clone https://github.com/dropwizard-jobs/dropwizard-jobs
cd dropwizard-jobs
mvn install
```

After installing the plugin locally, include the following dependencies only:

```xml
<dependency>
  <groupId>io.github.dropwizard-jobs</groupId>
  <artifactId>dropwizard-jobs-guice</artifactId>
  <version><current version></version>
</dependency>
<dependency>
  <groupId>com.hubspot.dropwizard</groupId>
  <artifactId>dropwizard-guice</artifactId>
  <version>0.7.1</version>
</dependency>
```

dropwizard-jobs-core and other dependencies will be resolved automaticlaly.

## Activating the Guice and GuiceJobs bundle

IMPORTANT: You must add the GuiceBundle first before you instantiate the GuiceJobsBundle.
Otherwise you will get a NULL injector.

```java
@Override
public void initialize( Bootstrap<SomeConfig> bootstrap )
{
  GuiceBundle guiceBundle = GuiceBundle.<YourConfiguration>newBuilder()
    .addModule( new YourModule() )
    .enableAutoConfig( getClass().getPackage().getName() )
    .setConfigClass( YourConfiguration.class )
    .build();
  bootstrap.addBundle( guiceBundle );

  GuiceJobsBundle guiceJobsBundle = new GuiceJobsBundle(
    'com.youpackage.url',
    guiceBundle.getInjector() );
  bootstrap.addBundle( guiceJobsBundle );
}
```

## Job Injection

Once the GuiceBundle and GuiceJobsBundle have been added, you can now inject dependencies into your Jobs.
A common usage is to inject constructor arguments:

```java
@Every("1s")
public class InjectedJob extends Job {
  @Injected
  public InjectedJob ( Object depdency ) {
    // do some logic with dependency
  }

}
```

See [Dropwizard-Guice](https://github.com/HubSpot/dropwizard-guice) for more information on how to use
Guice and Modules with Dropwizard.

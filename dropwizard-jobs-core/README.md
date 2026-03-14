# Dropwizard Jobs Core

This is the core scheduling library for Dropwizard applications, providing seamless integration
with the [Quartz Scheduler](https://quartz-scheduler.org/).

It includes:

- Base [`Job`](src/main/java/io/dropwizard/jobs/Job.java) class for background job implementation
- Scheduling annotations:
    - [`@Every`](src/main/java/io/dropwizard/jobs/annotations/Every.java),
    - [`@On`](src/main/java/io/dropwizard/jobs/annotations/On.java),
    - [`@OnApplicationStart`](src/main/java/io/dropwizard/jobs/annotations/OnApplicationStart.java),
    - [`@OnApplicationStop`](src/main/java/io/dropwizard/jobs/annotations/OnApplicationStop.java),
    - [`@DelayStart`](src/main/java/io/dropwizard/jobs/annotations/DelayStart.java)
- [`JobManager`](src/main/java/io/dropwizard/jobs/JobManager.java) for lifecycle management
- [`JobsBundle`](src/main/java/io/dropwizard/jobs/JobsBundle.java) for Dropwizard integration
- [`JobMetadata`](src/main/java/io/dropwizard/jobs/JobMetadata.java) for separating job discovery
    from instantiation (used by DI modules)
- Quartz scheduler configuration and management

## Using maven central repository

dropwizard-jobs-core can be used with Maven. It is located in Central Repository: <https://search.maven.org/>

Add to your pom:

```xml
<dependency>
  <groupId>io.github.dropwizard-jobs</groupId>
  <artifactId>dropwizard-jobs-core</artifactId>
  <version>7.0.0</version> <!-- Replace with the latest version -->
</dependency>
```

## Installing from source

```bash
git clone https://github.com/dropwizard-jobs/dropwizard-jobs
cd dropwizard-jobs
./mvnw install
```

After installing locally, include the dependency:

```xml
<dependency>
  <groupId>io.github.dropwizard-jobs</groupId>
  <artifactId>dropwizard-jobs-core</artifactId>
  <version>7.0.0</version> <!-- Replace with the latest version -->
</dependency>
```

## Dependency Injection Extensions

The core module provides a foundation for dependency injection integrations. For applications requiring
DI, use one of these extension modules:

- **[HK2 Integration](../dropwizard-jobs-hk2/README.md)** (`dropwizard-jobs-hk2`) - Uses Dropwizard's
    built-in HK2 container via Jersey. Leverages `JobMetadata` to avoid useless job instantiation
    during discovery.
- **[Guice Integration](../dropwizard-jobs-guice/README.md)** (`dropwizard-jobs-guice`) - Integrates
    with Google Guice for dependency injection.
- **Spring Integration** (`dropwizard-jobs-spring`) - Integrates with Spring Framework for dependency
    injection.

These modules follow the Factory/Manager/Bundle triple pattern, extending
[`JobManager`](src/main/java/io/dropwizard/jobs/JobManager.java) to support runtime dependency injection.

## JobMetadata

The [`JobMetadata`](src/main/java/io/dropwizard/jobs/JobMetadata.java) class separates job discovery
from instantiation, allowing DI modules to:

1. Discover job classes without instantiating them
2. Use DI containers to create job instances only when needed
3. Avoid unnecessary object creation during application startup

This is particularly useful for HK2, Guice, and Spring integrations where jobs may have complex dependency graphs.

## Usage Documentation

For comprehensive usage documentation, including:

- Job types and annotations
- Configuration options
- Quartz integration
- Clustered environments
- Job listeners
- Programmatic configuration

Please refer to the [root README](../README.md).

# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Build & Test Commands

- **Build all**: `./mvnw -B package` (Maven wrapper, no install needed)
- **Single module**: `./mvnw -B package -pl dropwizard-jobs-core`
- **Single test class**: `./mvnw -B test -pl dropwizard-jobs-core -Dtest=JobManagerTest`
- **Single test method**: `./mvnw -B test -pl dropwizard-jobs-core -Dtest=JobManagerTest#testMethod`
- **Skip tests**: `./mvnw -B package -DskipTests`
- **Check dep updates**: `./mvnw versions:display-dependency-updates`
- Surefire runs tests **parallel by classes with unlimited threads**; tests must be thread-safe

## Non-Obvious Project Details

- Java 17 source/target; CI tests against 17, 21, 25
- `dropwizard-jobs-spring` uses `javax.inject:javax.inject:1` while the rest of the project uses
    `jakarta.inject` — do not mix
- `@Every` annotation value and `@On` cron can be overridden via `JobConfiguration` map at runtime
    (key = value string from annotation)
- `JobManager.stop()` uses a hardcoded 100ms sleep before shutdown — this is intentional sequencing,
    not a bug
- `SchedulerException` in `scheduleOrRescheduleJob()` is caught and logged (swallowed) — by design
    for cluster resilience
- Job key defaults to canonical class name; `jobName` annotation attribute overrides it. Duplicate
    jobNames collapse to one trigger
- Formatter on/off tags: `@formatter:on` / `@formatter:off` (Eclipse JDT profile in `code-format.xml`)

## Code Style (from code-format.xml)

- 4-space indent (spaces, not tabs), line width 150, comment width 120
- Braces: end-of-line (K&R) for all constructs
- Wrap before binary operators; do NOT join wrapped lines
- Test classes: `*Test` suffix; test job fixtures: `*TestJob` suffix
- All code in single package `io.dropwizard.jobs` (flat, no sub-packages per module)

## Coding Rules

- Extend `Job` (abstract) and implement `doJob(JobExecutionContext)` — never override `execute()`
    directly (it manages metrics timing)
- Schedule type is determined solely by class-level annotation: `@Every`, `@On`,
    `@OnApplicationStart`, `@OnApplicationStop`
- `@DelayStart` only works with `@Every` jobs — combining it with `@On` has no effect
- DI modules (Guice/Spring) override `JobFactory` to enable injection; when adding a new DI module,
    follow the triple-class pattern: `*JobFactory`, `*JobManager`, `*JobsBundle`
- The `scheduler/` sub-package contains strategy classes (`EveryScheduler`, `OnCronScheduler`, etc.)
    composed by `JobManager` — add new schedule types here
- `TimeParserUtil` parses human-readable durations (e.g., "5min", "1h") — use it instead of manual
    parsing
- Quartz config: if `JobConfiguration.getQuartzConfiguration()` returns empty,
    `DefaultQuartzConfiguration` provides RAMJobStore defaults
- Tests use both JUnit 5 Jupiter assertions AND Hamcrest matchers — follow existing test style
- Formatter profile: "MongoJack Format" in `code-format.xml` — import into IDE for consistent
    formatting

## Debugging

- Test logging: `logback-test.xml` in each module's `src/test/resources/` — `io.dropwizard.jobs` at
    INFO, root at WARN
- `SchedulerException` is silently caught in `JobManager.scheduleOrRescheduleJob()` — check logs for
    "Error scheduling" if jobs don't fire
- `JobManager.stop()` sleeps 100ms before `scheduler.shutdown(true)` — timing-dependent stop issues
    trace here
- Surefire parallel=classes + unlimitedThreads: flaky tests may be caused by shared static state in
    test job fixtures (e.g., `AbstractJob` latch patterns)
- Quartz scheduler instance is created fresh in `start()` via `StdSchedulerFactory` — no singleton;
    each test can get its own scheduler
- `CronExpressionParser` supports inline timezone override with `[timezone]` suffix in cron
    expressions — check timezone parsing if cron jobs fire at wrong times

## Architecture

- Architecture: Dropwizard `Managed` lifecycle → `JobManager` → composes `JobScheduler` strategies →
    Quartz `Scheduler`
- DI extension pattern: each module provides Factory/Manager/Bundle triple inheriting from core;
    `JobManager` is designed for this extension
- `JobManager` enforces single-trigger-per-job: multi-trigger jobs get deleted and recreated — this
    is cluster-safety design, not a limitation
- `DefaultQuartzConfiguration` hardcodes RAMJobStore — persistent job store requires user-provided
    `quartzConfiguration` map in YAML
- `@Every` value resolution: annotation value → `JobConfiguration` map lookup → `TimeParserUtil`
    parse — this indirection enables runtime config override
- `@On` cron resolution: annotation value → `JobConfiguration` map lookup → `CronExpressionParser`
    (handles `[TZ]` suffix) → Quartz CronTrigger
- Spring module has javax.inject/jakarta.inject mismatch with parent — architectural debt, must be
    resolved if upgrading to full Jakarta

## Project Context

- All three modules share the same package `io.dropwizard.jobs` — classes are distinguished by
    module, not package
- `improvements.md` contains 34 categorized findings (6 high, 18 medium, 10 low severity) about code
    quality — reference it for known issues
- The `scheduler/` sub-package implements a Strategy pattern: each scheduler type handles one
    annotation type
- `JobMediator` is the internal coordination point between `JobManager` and scheduler strategies —
    not part of public API
- `code-format.xml` is an Eclipse JDT formatter profile named "MongoJack Format" (historical name,
    unrelated to MongoDB)
- Maven wrapper (`mvnw`) pins Maven 3.9.2 — do not advise using system Maven

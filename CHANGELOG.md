# Changelog


## 4.0.0

* The `groupId` has been renamed to `io.github.dropwizard-jobs`
* Repository moved to `https://github.com/dropwizard-jobs/dropwizard-jobs`
* Java packages are renamed to `io.dropwizard.jobs.*` 
* See [release notes](https://github.com/dropwizard-jobs/dropwizard-jobs/releases/tag/v4.0.0-RELEASE) for a full list of changes 

## 2.0.0

* The `groupId` has been renamed to `de.spinscale.dropwizard`
* Removed any reflection based code in the code base, along with the reflections library
* The above also means, every job has to be added manually to the JobsBundle - no more autodetection
* The above also means, that wiring for guice/spring has to happen - constructor injection is encouraged now
* Allowed jobs to have arbitrary constructors


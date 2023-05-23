# Changelog

## 5.1.0

* Update maven and maven wrapper #163
* Guice 7 update #164
* update mockito to v5.3.1 #165
* update jaxb-runtime to v4.0.2 #166


## 5.0.1

* Fix gpg signing and source jars for publishing


## 5.0.0

* Dependency updates due to vulnerabilities - #161 #160 #159 
* Updated to use Dropwizard 3 - #151 - Thanks to @eddymouthaan
* Artifact publishing refactored to publish snapshots on [GH package repository](https://github.com/orgs/dropwizard-jobs/packages?repo_name=dropwizard-jobs) - #162


## 4.1.0

* Mainly dependency updates due to vulnerabilities
* JUnit 5 support #134
* Fix: Cleanup exisint jobs on @OnApplicationStart #131
* Fix: duplicate job initiation #123


## 4.0.0

* The `groupId` has been renamed to `io.github.dropwizard-jobs`
* Repository moved to `https://github.com/dropwizard-jobs/dropwizard-jobs`
* Java packages are renamed to `io.dropwizard.jobs.*` 
* See [release notes](https://github.com/dropwizard-jobs/dropwizard-jobs/releases/tag/v4.1.0-RELEASE) for a full list of changes 

## 2.0.0

* The `groupId` has been renamed to `de.spinscale.dropwizard`
* Removed any reflection based code in the code base, along with the reflections library
* The above also means, every job has to be added manually to the JobsBundle - no more autodetection
* The above also means, that wiring for guice/spring has to happen - constructor injection is encouraged now
* Allowed jobs to have arbitrary constructors


# Changelog

**Only BREAKING changes are listed here. Please check [release logs](https://github.com/dropwizard-jobs/dropwizard-jobs/releases) for a list of full changes**

## 6.0.0

* Major refactoring and clean up by @hakandilek in #121
  BREAKING: `JobsBundle()` constructor receives a `List` instead of `Varargs`
* BREAKING: Drop Java 11 Support by @ElardoR in #226
* BREAKING: Bump Dropwizard to v4.0.4 by @ElardoR in #222
* BREAKING: Bump spring.version from 5.3.27 to 6.1.1 #220

# 5.1.4

* Bump guava from 32.0.1-jre to 32.1.1-jre by @dependabot in https://github.com/dropwizard-jobs/dropwizard-jobs/pull/190
* Bump org.junit.vintage:junit-vintage-engine from 5.10.0-RC1 to 5.10.0 by @dependabot in https://github.com/dropwizard-jobs/dropwizard-jobs/pull/197
* Update Docu link in README.md by @aceArt-GmbH in https://github.com/dropwizard-jobs/dropwizard-jobs/pull/198
* Bump com.google.guava:guava from 32.1.1-jre to 32.1.2-jre by @dependabot in https://github.com/dropwizard-jobs/dropwizard-jobs/pull/199
* Bump org.mockito:mockito-core from 5.4.0 to 5.5.0 by @dependabot in https://github.com/dropwizard-jobs/dropwizard-jobs/pull/200
* Bump org.apache.maven.plugins:maven-javadoc-plugin from 3.5.0 to 3.6.0 by @dependabot in https://github.com/dropwizard-jobs/dropwizard-jobs/pull/202
* Bump actions/checkout from 3 to 4 by @dependabot in https://github.com/dropwizard-jobs/dropwizard-jobs/pull/204
* Bump org.mockito:mockito-core from 5.5.0 to 5.6.0 by @dependabot in https://github.com/dropwizard-jobs/dropwizard-jobs/pull/205
* Bump jackson.version from 2.15.2 to 2.15.3 by @dependabot in https://github.com/dropwizard-jobs/dropwizard-jobs/pull/208
* Bump com.google.guava:guava from 32.1.2-jre to 32.1.3-jre by @dependabot in https://github.com/dropwizard-jobs/dropwizard-jobs/pull/206
* update dropwizard to v3.0.2 by @hakandilek in https://github.com/dropwizard-jobs/dropwizard-jobs/pull/209

# 5.1.3

* Bump junit-vintage-engine from 5.10.0-M1 to 5.10.0-RC1 by @dependabot in https://github.com/dropwizard-jobs/dropwizard-jobs/pull/191
* Update instructions about JobConfiguration in README by @hakandilek in https://github.com/dropwizard-jobs/dropwizard-jobs/pull/193

## 5.1.2

* Bump jackson-core from 2.15.1 to 2.15.2 by @dependabot in https://github.com/dropwizard-jobs/dropwizard-jobs/pull/184
* Bump maven-surefire-plugin.version from 3.1.0 to 3.1.2 by @dependabot in https://github.com/dropwizard-jobs/dropwizard-jobs/pull/185
* update mockito to v5.4.0 by @hakandilek in https://github.com/dropwizard-jobs/dropwizard-jobs/pull/187
* update guava to v32.0.1-jre by @hakandilek in https://github.com/dropwizard-jobs/dropwizard-jobs/pull/189

## 5.1.1

* Bump jackson-core from 2.15.1 to 2.15.2 by @dependabot in https://github.com/dropwizard-jobs/dropwizard-jobs/pull/184
* Bump maven-surefire-plugin.version from 3.1.0 to 3.1.2 by @dependabot in https://github.com/dropwizard-jobs/dropwizard-jobs/pull/185

## 5.1.0

* Update maven and maven wrapper #163
* Guice 7 update #164
* Update mockito to v5.3.1 #165
* Update jaxb-runtime to v4.0.2 #166
* Update README to prevent confusion #169
* Dependency cleanup #170
* Codeql GH action #171
* dependency-review GH Action #172
* Update snakeyaml to v2.0+ #174
* Update jackson to v2.15.1 #175
* Intoduced dependabot.yml config #176
* Bump actions/dependency-review-action from 2 to 3 #177
* Bump maven-source-plugin from 3.2.1 to 3.3.0 #178
* Bump nexus-staging-maven-plugin from 1.6.8 to 1.6.13 #181
* Test and build on multiple java versions #182
* Use https urls instead of http #183

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


---
title: Configure the describe goal
author: 
  - John Casey
  - Maria Odea Ching
  - Vincent Siveton
date: 2026-11-17
---

<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->


# Configure the describe goal

The [`help:describe`](../describe-mojo.html) goal answers two kinds of questions: what a plugin or goal does and which
parameters it takes, and what Maven runs for a lifecycle phase. This page shows its options.

## Choose a plugin with `plugin`

The `plugin` parameter accepts a plugin prefix, or coordinates in the form `groupId:artifactId[:version]`:

```bash
mvn help:describe -Dplugin=help
mvn help:describe -Dplugin=org.apache.maven.plugins:maven-help-plugin
mvn help:describe -Dplugin=org.apache.maven.plugins:maven-help-plugin:3.6.0
```

Without a version, the goal describes the latest release. The separate `groupId`, `artifactId` and `version`
parameters do the same as the coordinates form:

```bash
mvn help:describe -DgroupId=org.apache.maven.plugins -DartifactId=maven-help-plugin
```

The output starts with the plugin's coordinates and prefix, then lists its goals:

```text
[INFO] org.apache.maven.plugins:maven-help-plugin:3.6.0

Name: Apache Maven Help Plugin
Description: The Maven Help plugin provides goals aimed at helping to make
  sense out of the build environment. ...
Group Id: org.apache.maven.plugins
Artifact Id: maven-help-plugin
Version: 3.6.0
Goal Prefix: help

This plugin has 11 goals:

help:active-profiles
  Description: Displays a list of the profiles which are currently active for
    this build.

help:all-profiles
  Description: Displays a list of available profiles under the current
    project. ...
...
```

## Describe one goal with `goal`

Add the `goal` parameter to describe a single goal of the plugin:

```bash
mvn help:describe -Dplugin=help -Dgoal=effective-pom
```

## Show more or less with `detail` and `minimal`

Add `-Ddetail` to list each goal's parameters, with their descriptions, default values and the user property
you can set them with on the command line. This is usually what you want when you configure a goal:

```bash
mvn help:describe -Dplugin=compiler -Dgoal=compile -Ddetail
```

Add `-Dminimal` to print only the plugin's coordinates and description, without its goals:

```bash
mvn help:describe -Dplugin=help -Dminimal
```

```text
[INFO] org.apache.maven.plugins:maven-help-plugin:3.6.0

Name: Apache Maven Help Plugin
Description: The Maven Help plugin provides goals aimed at helping to make
  sense out of the build environment. ...
Group Id: org.apache.maven.plugins
Artifact Id: maven-help-plugin
Version: 3.6.0
Goal Prefix: help

For more information, run 'mvn help:describe [...] -Ddetail'
```

## Describe a phase or goal with `cmd`

The `cmd` parameter takes a lifecycle phase or a plugin goal in the form `prefix:goal`, as you would pass them to `mvn`. The answer reflects your project, so for a phase it shows the plugin goals bound by the project's
packaging, with their versions.

For a phase, the goal shows the plugin goal it runs and the whole lifecycle it belongs to:

```bash
mvn help:describe -Dcmd=compile
```

```text
[INFO] 'compile' is a phase corresponding to this plugin:
org.apache.maven.plugins:maven-compiler-plugin:3.15.0:compile

It is a part of the lifecycle for the POM packaging 'jar'. This lifecycle includes the following phases:
* validate: Not defined
* initialize: Not defined
* generate-sources: Not defined
* process-sources: Not defined
* generate-resources: Not defined
* process-resources: org.apache.maven.plugins:maven-resources-plugin:3.4.0:resources
* compile: org.apache.maven.plugins:maven-compiler-plugin:3.15.0:compile
* process-classes: Not defined
* generate-test-sources: Not defined
* process-test-sources: Not defined
* generate-test-resources: Not defined
* process-test-resources: org.apache.maven.plugins:maven-resources-plugin:3.4.0:testResources
* test-compile: org.apache.maven.plugins:maven-compiler-plugin:3.15.0:testCompile
* process-test-classes: Not defined
* test: org.apache.maven.plugins:maven-surefire-plugin:3.5.4:test
* prepare-package: Not defined
* package: org.apache.maven.plugins:maven-jar-plugin:3.5.0:jar
* pre-integration-test: Not defined
* integration-test: Not defined
* post-integration-test: Not defined
* verify: Not defined
* install: org.apache.maven.plugins:maven-install-plugin:3.1.4:install
* deploy: org.apache.maven.plugins:maven-deploy-plugin:3.1.4:deploy
```

`Not defined` means no plugin goal is bound to that phase by default; your POM can still bind one there.

The `clean` phase belongs to its own lifecycle:

```bash
mvn help:describe -Dcmd=clean
```

```text
[INFO] 'clean' is a phase within the 'clean' lifecycle, which has the following phases:
* pre-clean: Not defined
* clean: org.apache.maven.plugins:maven-clean-plugin:3.2.0:clean
* post-clean: Not defined
```

For a plugin goal, the output matches `-Dplugin=... -Dgoal=...`:

```bash
mvn help:describe -Dcmd=compiler:compile
```

```text
[INFO] 'compiler:compile' is a plugin goal (aka mojo).
Mojo: 'compiler:compile'
compiler:compile
  Description: Compiles application sources. By default uses the javac
    compiler of the JDK used to execute Maven. This can be overwritten through
    Toolchains or parameter compilerId. See also: javac Command

For more information, run 'mvn help:describe [...] -Ddetail'
```

The versions in these samples depend on your Maven version and your POM.

## Write to a file with `output`

The `output` parameter writes the description to a file instead of the console:

```bash
mvn help:describe -Dplugin=compiler -Ddetail -Doutput=compiler-plugin.txt
```

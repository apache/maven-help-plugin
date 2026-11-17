---
title: Usage
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


# Usage

You can run every goal from the command line without configuring the plugin in your POM. Goals that inspect a project
run against the POM in the current directory; add `-f path/to/pom.xml` to point at another one. Every goal except
`help:help` accepts `-Doutput=<file>` to write its result to a file instead of the console.

## See the effective POM

Your POM is only part of the model Maven builds from: it inherits from parent POMs and the super POM, imports BOMs,
applies active profiles and interpolates properties. The [`effective-pom`](./effective-pom-mojo.html) goal shows the
result for each project in the build:

```bash
mvn help:effective-pom
```

To find out where a value comes from, add `-Dverbose`. Each element then carries a comment with the POM and line
that set it:

```xml
<project xmlns="http://maven.apache.org/POM/4.0.0" ...>
  <modelVersion>4.0.0</modelVersion>  <!-- com.example:demo:1.0.0, line 1 -->
  <groupId>com.example</groupId>  <!-- com.example:demo:1.0.0, line 1 -->
  <artifactId>demo</artifactId>  <!-- com.example:demo:1.0.0, line 1 -->
  <version>1.0.0</version>  <!-- com.example:demo:1.0.0, line 1 -->
  <repositories>
    <repository>
      <snapshots>
        <enabled>false</enabled>  <!-- org.apache.maven:maven-model-builder:3.9.16:super-pom, line 33 -->
      </snapshots>
```

To inspect the effective POM of a published artifact instead, add `-Dartifact`. The goal still has to run inside a
project directory:

```bash
mvn help:effective-pom -Dartifact=org.apache.commons:commons-lang3:3.17.0
```

Leave out the version to use the latest release.

## Check which profiles apply

The [`active-profiles`](./active-profiles-mojo.html) goal lists the profiles that are active for each project, and
where each one is defined. Use it with the same `-P` options and properties you build with:

```bash
mvn help:active-profiles -Pci
```

```text
Active Profiles for Project 'com.example:demo:jar:1.0.0':

The following profiles are active:

 - ci (source: com.example:demo:1.0.0)
```

The [`all-profiles`](./all-profiles-mojo.html) goal lists every profile available to the project, from the POM and
from `settings.xml`, and whether it is active:

```bash
mvn help:all-profiles
```

```text
[INFO] Listing Profiles for Project: com.example:demo:jar:1.0.0
  Profile Id: dev (Active: true, Source: pom)
  Profile Id: ci (Active: false, Source: pom)
```

A profile reported as inactive may need an activation property or a `-P` option. See the
[introduction to build profiles](https://maven.apache.org/guides/introduction/introduction-to-profiles.html).

In a multi-module build, `effective-pom`, `active-profiles` and `all-profiles` report on each module. Use `-pl` to
narrow them to one module, for example `mvn help:active-profiles -pl core`.

## Read a property or expression

The [`evaluate`](./evaluate-mojo.html) goal prints the value of a Maven expression. In a script, combine it with
`-q` and `-DforceStdout` so that only the value is printed:

```bash
VERSION=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
echo "$VERSION"
```

Write the expression without the surrounding `${...}`. Any property works, as do model paths such as
`project.build.directory` or `project.parent.version`. An expression that resolves to nothing prints
`null object or invalid expression`, and a malformed one prints nothing. In both cases the command exits with status 0,
so check the value in your script.

Add `-Dartifact=groupId:artifactId[:version]` to evaluate the expression against a published artifact instead of the
current project.

Without `-Dexpression`, the goal prompts for expressions one at a time until you enter `0`:

```text
[INFO] Enter the Maven expression i.e. ${project.groupId} or 0 to exit?:
${project.artifactId}
[INFO]
demo
```

The interactive mode needs Maven to run in interactive mode, so it does not work with `-B` (batch mode). It always
prints to the console and ignores `-Doutput`.

The expressions available to plugin parameters are listed in the Javadoc of
[`PluginParameterExpressionEvaluator`](/ref/current/maven-core/apidocs/org/apache/maven/plugin/PluginParameterExpressionEvaluator.html).

## Check your settings

The [`effective-settings`](./effective-settings-mojo.html) goal shows the settings Maven actually uses: the global
`settings.xml` merged with your user `settings.xml`, where the user file wins. This is the place to check mirrors,
servers, proxies and the local repository path:

```bash
mvn help:effective-settings
```

Passwords are masked. Add `-DshowPasswords` to print them, and keep that output out of build logs.

## Learn about a plugin

The [`describe`](./describe-mojo.html) goal shows the goals of a plugin and, with `-Ddetail`, their parameters. Name the
plugin by its prefix or by its coordinates:

```bash
mvn help:describe -Dplugin=compiler
mvn help:describe -Dplugin=org.apache.maven.plugins:maven-compiler-plugin -Dgoal=compile -Ddetail
```

It also tells you what runs in a lifecycle phase:

```bash
mvn help:describe -Dcmd=package
```

[Configure the describe goal](./examples/describe-configuration.html) covers all of its options with sample output.

## List lifecycles, packagings and dependency types

Three goals list what your Maven installation knows about. They don't need a project.

- [`list-lifecycle-phases`](./list-lifecycle-phases-mojo.html) lists the phases of each lifecycle.
- [`list-packaging`](./list-packaging-mojo.html) lists each packaging type with the plugin goals bound to its phases.
- [`list-dependency-types`](./list-dependency-types-mojo.html) lists each dependency `type` with its file extension and
  classifier, and whether it is added to the classpath.

For example:

```bash
mvn help:list-dependency-types
```

```text
Maven Dependency Types defined:

ejb-client
 - Extension: *.jar
 - Classifier: client
 - Added to classpath

jar
 - Extension: *.jar
 - Added to classpath

pom
 - Extension: *.pom

test-jar
 - Extension: *.jar
 - Classifier: tests
 - Added to classpath
...
```

See the [introduction to the build lifecycle](https://maven.apache.org/guides/introduction/introduction-to-the-lifecycle.html)
for what the phases mean.

## Inspect the environment

The [`system`](./system-mojo.html) goal lists the system properties and environment variables that Maven sees. It
helps when a build behaves differently on another machine or in CI:

```bash
mvn help:system
```

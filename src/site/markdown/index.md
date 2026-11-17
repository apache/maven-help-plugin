---
title: Introduction
author: 
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

# Apache Maven Help Plugin

When a build does something unexpected, the Help Plugin shows you what Maven actually sees:
the final POM after inheritance and profiles, the merged settings, the active profiles, the
value of any property, and what a plugin goal does and which parameters it takes.

You don't need to add the plugin to your POM. Run a goal from your project directory:

```bash
mvn help:effective-pom
```

## What do you want to know?

| Question | Command |
|---|---|
| What does my POM look like after inheritance, profiles and interpolation? | `mvn help:effective-pom` |
| Where does that value in the effective POM come from? | `mvn help:effective-pom -Dverbose` |
| Which profiles are active, and which ones exist? | `mvn help:active-profiles`, `mvn help:all-profiles` |
| What is the value of a property or expression? | `mvn help:evaluate -Dexpression=project.version -q -DforceStdout` |
| Which settings, mirrors and servers is Maven using? | `mvn help:effective-settings` |
| What goals and parameters does a plugin have? | `mvn help:describe -Dplugin=compiler -Ddetail` |
| Which plugin runs in a given phase? | `mvn help:describe -Dcmd=package` |
| Which lifecycle phases, packagings and dependency types exist? | `mvn help:list-lifecycle-phases`, `mvn help:list-packaging`, `mvn help:list-dependency-types` |
| Which JVM, system properties and environment variables does Maven see? | `mvn help:system` |

The [usage page](./usage.html) walks through each of these with sample output.

## Goals

The plugin has 11 goals.

Inspect the build:

- [`help:effective-pom`](./effective-pom-mojo.html) displays the effective POM of each project in the build, or of any
  artifact with `-Dartifact=groupId:artifactId[:version]`.
- [`help:effective-settings`](./effective-settings-mojo.html) displays the settings after the global and user
  `settings.xml` files are merged. Passwords are hidden unless you add `-DshowPasswords`.
- [`help:active-profiles`](./active-profiles-mojo.html) lists the profiles that are active for each project, and where
  each one is defined.
- [`help:all-profiles`](./all-profiles-mojo.html) lists every profile available to the project, active or not.
- [`help:evaluate`](./evaluate-mojo.html) prints the value of a Maven expression, either one given on the command line or
  interactively.
- [`help:system`](./system-mojo.html) lists the system properties and environment variables.

Explore plugins and lifecycles:

- [`help:describe`](./describe-mojo.html) describes a plugin, one of its goals, or a lifecycle phase.
- [`help:list-lifecycle-phases`](./list-lifecycle-phases-mojo.html) lists the phases of the `clean`, `default` and `site`
  lifecycles.
- [`help:list-packaging`](./list-packaging-mojo.html) lists the known packaging types and the plugin goals bound to each
  phase.
- [`help:list-dependency-types`](./list-dependency-types-mojo.html) lists the known dependency types with their file
  extension and classifier.
- [`help:help`](./help-mojo.html) displays help for this plugin itself.

Every goal except `help:help` accepts `-Doutput=<file>` to write its result to a file instead of the console.

## Requirements

The plugin needs Maven 3.9.0 or later and Java 8 or later. You don't need to declare it in your POM: Maven resolves
the `help` prefix to the latest release, unless your POM pins a version. To choose the version yourself,
call the goal by its full coordinates, for example `mvn org.apache.maven.plugins:maven-help-plugin:3.6.0:effective-pom`.

## Getting help

- Ask questions on the [user mailing list](./mailing-lists.html); its archive may already hold the answer.
- Report bugs and request features in the [issue tracker](./issue-management.html). A small project that reproduces
  the problem gets it fixed fastest.
- Contributions are welcome: see the [source repository](./scm.html) and the
  [guide to helping with Maven](https://maven.apache.org/guides/development/guide-helping.html).

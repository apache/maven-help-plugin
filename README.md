<!---
 Licensed to the Apache Software Foundation (ASF) under one or more
 contributor license agreements.  See the NOTICE file distributed with
 this work for additional information regarding copyright ownership.
 The ASF licenses this file to You under the Apache License, Version 2.0
 (the "License"); you may not use this file except in compliance with
 the License.  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

# Apache Maven Help Plugin

[![Apache License, Version 2.0, January 2004](https://img.shields.io/github/license/apache/maven.svg?label=License)](https://www.apache.org/licenses/LICENSE-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.apache.maven.plugins/maven-help-plugin.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/org.apache.maven.plugins/maven-help-plugin)
[![Reproducible Builds](https://img.shields.io/endpoint?url=https://raw.githubusercontent.com/jvm-repo-rebuild/reproducible-central/master/content/org/apache/maven/plugins/maven-help-plugin/badge.json)](https://github.com/jvm-repo-rebuild/reproducible-central/blob/master/content/org/apache/maven/plugins/maven-help-plugin/README.md)
[![Verify](https://github.com/apache/maven-help-plugin/actions/workflows/maven-verify.yml/badge.svg)](https://github.com/apache/maven-help-plugin/actions/workflows/maven-verify.yml)

The Help Plugin shows what Maven actually sees when it builds a project: the effective POM after inheritance and
profiles, the merged settings, the active profiles, the value of any expression, and the goals and parameters of any
plugin. It needs Maven 3.9.0 or later and Java 8 or later, and you don't have to add it to your POM:

```bash
mvn help:effective-pom -Dverbose
mvn help:evaluate -Dexpression=project.version -q -DforceStdout
mvn help:describe -Dplugin=compiler -Ddetail
```

The [plugin site](https://maven.apache.org/plugins/maven-help-plugin/) lists every goal, with
[usage examples](https://maven.apache.org/plugins/maven-help-plugin/usage.html).

## Building

Build and run the unit tests:

```bash
mvn verify
```

The integration tests under `src/it/projects` run with the `run-its` profile. To run a subset, name the projects:

```bash
mvn -P run-its verify -Dinvoker.test=evaluate-forceStdout*
```

## Contributing

Report bugs and request features in [GitHub issues](https://github.com/apache/maven-help-plugin/issues), and send
changes as pull requests against `master`. Discuss larger features on the
[developer mailing list](https://maven.apache.org/mailing-lists.html) first. Code follows the
[Maven code style](https://maven.apache.org/developers/conventions/code.html), which Spotless enforces during the
build; run `mvn spotless:apply` before you push. Pull request titles become entries in the release notes.

See [helping with Maven](https://maven.apache.org/guides/development/guide-helping.html) for the wider project, and
the [contributor license agreements](https://www.apache.org/licenses/#clas) if you plan to contribute regularly.

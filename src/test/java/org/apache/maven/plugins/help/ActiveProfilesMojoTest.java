/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.help;

import javax.inject.Inject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Test class for the active-profiles mojo of the Help Plugin.
 */
@MojoTest
class ActiveProfilesMojoTest {

    @Inject
    private MavenProject project;

    @Inject
    private MavenSession mavenSession;

    @TempDir
    private Path tempDir;

    private Path outputPath;

    @BeforeEach
    void setup() throws IOException {
        when(mavenSession.getProjects()).thenReturn(Collections.singletonList(project));

        outputPath = Files.createTempFile(tempDir, "maven-help-plugin-test-", ".txt");
        mavenSession.getUserProperties().setProperty("outputPath", outputPath.toString());
    }
    /**
     * Tests that profiles activated in the settings are resolved.
     *
     * @throws Exception in case of errors.
     */
    @Test
    @InjectMojo(goal = "active-profiles")
    @MojoParameter(name = "output", value = "${outputPath}")
    void testActiveProfilesFromSettings(ActiveProfilesMojo mojo) throws Exception {
        when(project.getInjectedProfileIds())
                .thenReturn(getProfiles(Collections.singletonList("from-settings"), Collections.emptyList()));

        mojo.execute();

        String file = readOutput();
        assertTrue(file.contains("from-settings (source: external)"));
    }

    /**
     * Tests that profiles activated in the POM are resolved.
     *
     * @throws Exception in case of errors.
     */
    @Test
    @InjectMojo(goal = "active-profiles")
    @MojoParameter(name = "output", value = "${outputPath}")
    void testActiveProfilesFromPom(ActiveProfilesMojo mojo) throws Exception {
        when(project.getInjectedProfileIds())
                .thenReturn(getProfiles(Collections.emptyList(), Collections.singletonList("from-pom")));

        mojo.execute();

        String file = readOutput();
        assertTrue(file.contains("from-pom (source: org.apache.maven.test:test:1.0)"));
    }

    private Map<String, List<String>> getProfiles(List<String> externals, List<String> pom) {
        Map<String, List<String>> profiles = new HashMap<>();
        profiles.put("external", externals); // from settings
        profiles.put("org.apache.maven.test:test:1.0", pom); // from POM
        profiles.put("", Collections.emptyList()); // from super POM
        return profiles;
    }

    private String readOutput() throws IOException {
        return new String(Files.readAllBytes(outputPath));
    }
}

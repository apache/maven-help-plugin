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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.maven.api.Project;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.InputSource;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.junit.jupiter.api.Test;

import static org.apache.maven.api.plugin.testing.MojoExtension.getBasedir;
import static org.apache.maven.api.plugin.testing.MojoExtension.setVariableValueToObject;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test class for the active-profiles mojo of the Help Plugin.
 */
@MojoTest
public class ActiveProfilesMojoTest {

    static final String CONFIG_XML = "classpath:/unit/active-profiles/plugin-config.xml";

    static final String OUTPUT = "output.txt";

    /**
     * Tests that profiles activated in the settings are resolved.
     *
     * @throws Exception in case of errors.
     */
    @Test
    @InjectMojo(goal = "active-profiles", pom = CONFIG_XML)
    @MojoParameter(name = "output", value = OUTPUT)
    @Basedir
    public void testActiveProfilesFromSettings(ActiveProfilesMojo mojo) throws Exception {
        Project project = mock(Project.class);
        Profile profile = Profile.newBuilder()
                .id("from-settings")
                .location("", new InputLocation(1, 1, new InputSource(null, "~/.m2/settings.xml")))
                .build();
        when(project.getEffectiveActiveProfiles()).thenReturn(List.of(profile));

        setVariableValueToObject(mojo, "projects", List.of(project));

        mojo.execute();

        String file = Files.readString(Path.of(getBasedir(), OUTPUT));
        assertThat(file, containsString("from-settings (source: ~/.m2/settings.xml)"));
    }

    /**
     * Tests that profiles activated in the POM are resolved.
     *
     * @throws Exception in case of errors.
     */
    @Test
    @InjectMojo(goal = "active-profiles", pom = CONFIG_XML)
    @MojoParameter(name = "output", value = OUTPUT)
    @Basedir
    public void testActiveProfilesFromPom(ActiveProfilesMojo mojo) throws Exception {
        Project project = mock(Project.class);
        Profile profile = Profile.newBuilder()
                .id("from-pom")
                .location("", new InputLocation(1, 1, new InputSource("org.apache.maven.test:test:1.0", null)))
                .build();
        when(project.getEffectiveActiveProfiles()).thenReturn(List.of(profile));

        setVariableValueToObject(mojo, "projects", List.of(project));

        mojo.execute();

        String file = Files.readString(Path.of(getBasedir(), OUTPUT));
        assertThat(file, containsString("from-pom (source: org.apache.maven.test:test:1.0)"));
    }
}

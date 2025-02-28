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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.api.Project;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.InputSource;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.plugin.Log;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.api.plugin.testing.stubs.ProjectStub;
import org.apache.maven.api.plugin.testing.stubs.SessionMock;
import org.apache.maven.api.services.Prompter;
import org.apache.maven.api.settings.Server;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.impl.InternalSession;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.apache.maven.api.plugin.testing.MojoExtension.getBasedir;
import static org.apache.maven.api.plugin.testing.MojoExtension.setVariableValueToObject;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for the all-profiles mojo of the Help Plugin.
 */
@MojoTest
@MockitoSettings(strictness = Strictness.WARN)
class AllProfilesMojoTest {

    static final String CONFIG_XML = "classpath:/unit/all-profiles/plugin-config.xml";

    static final String OUTPUT = "output.txt";

    @Mock
    private Log log;

    @Mock
    private Prompter prompter;

    /**
     * Tests the case when no profiles are present for the projects.
     *
     * @throws Exception in case of errors.
     */
    @Test
    @InjectMojo(goal = "all-profiles", pom = CONFIG_XML)
    @MojoParameter(name = "output", value = OUTPUT)
    @Basedir
    public void testNoProfiles(AllProfilesMojo mojo) throws Exception {
        setVariableValueToObject(mojo, "projects", List.of(new ProjectStub()));
        setVariableValueToObject(mojo, "settingsProfiles", List.of());

        mojo.execute();

        verify(log).warn("No profiles detected!");
    }

    /**
     * Tests the case when profiles are present in the POM and in a parent POM.
     *
     * @throws Exception in case of errors.
     */
    @Test
    @InjectMojo(goal = "all-profiles", pom = CONFIG_XML)
    @MojoParameter(name = "output", value = OUTPUT)
    @Basedir
    public void testProfileFromPom(AllProfilesMojo mojo) throws Exception {
        Project project = mock(Project.class);
        when(project.getEffectiveProfiles())
                .thenReturn(List.of(newPomProfile("pro-1", "pom"), newPomProfile("pro-2", "pom")));
        when(project.getEffectiveActiveProfiles()).thenReturn(List.of(newPomProfile("pro-1", "pom")));
        setVariableValueToObject(mojo, "projects", List.of(project));

        setVariableValueToObject(mojo, "settingsProfiles", List.of());

        mojo.execute();

        String file = readFile(OUTPUT);
        assertThat(file, containsString("Profile Id: pro-1 (Active: true, Source: pom)"));
        assertThat(file, containsString("Profile Id: pro-2 (Active: false, Source: pom)"));
    }

    /**
     * Tests the case when profiles are present in the settings.
     *
     * @throws Exception in case of errors.
     */
    @Test
    @InjectMojo(goal = "all-profiles", pom = CONFIG_XML)
    @MojoParameter(name = "output", value = OUTPUT)
    @Basedir
    public void testProfileFromSettings(AllProfilesMojo mojo) throws Exception {
        Project project = mock(Project.class);
        when(project.getEffectiveActiveProfiles()).thenReturn(List.of(newPomProfile("settings-1", "settings.xml")));
        setVariableValueToObject(mojo, "projects", List.of(project));

        List<org.apache.maven.api.settings.Profile> settingsProfiles = new ArrayList<>();
        settingsProfiles.add(newSettingsProfile("settings-1"));
        settingsProfiles.add(newSettingsProfile("settings-2"));
        setVariableValueToObject(mojo, "settingsProfiles", settingsProfiles);

        mojo.execute();

        String file = readFile(OUTPUT);
        assertThat(file, containsString("Profile Id: settings-1 (Active: true, Source: settings.xml)"));
        assertThat(file, containsString("Profile Id: settings-2 (Active: false, Source: settings.xml)"));
    }

    @Provides
    InternalSession createSession(Prompter prompter) {
        InternalSession session = SessionMock.getMockSession("target/local-repo");

        when(session.getSettings())
                .thenReturn(Settings.newBuilder()
                        .servers(List.of(Server.newBuilder()
                                .id("central")
                                .username("foo")
                                .build()))
                        .build());
        when(session.getService(Prompter.class)).thenReturn(prompter);

        return session;
    }

    @Provides
    @Singleton
    Log createlog() {
        return log;
    }

    private Profile newPomProfile(String id, String source) {
        return Profile.newBuilder()
                .id(id)
                .location("", new InputLocation(1, 1, new InputSource(null, source)))
                .build();
    }

    private org.apache.maven.api.settings.Profile newSettingsProfile(String id) {
        return org.apache.maven.api.settings.Profile.newBuilder().id(id).build();
    }

    private String readFile(String path) throws IOException {
        return Files.readString(Path.of(getBasedir(), path));
    }
}

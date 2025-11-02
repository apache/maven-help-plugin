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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for the all-profiles mojo of the Help Plugin.
 */
@ExtendWith(MockitoExtension.class)
@MojoTest
class AllProfilesMojoTest {

    @Inject
    private MavenProject project;

    @Inject
    private MavenSession mavenSession;

    @Mock
    private Settings settings;

    @Mock
    private Model projectModel;

    @Mock
    private Log log;

    @TempDir
    private Path tempDir;

    private Path outputPath;

    @Provides
    private Log provideLogger() {
        return log;
    }

    private final List<Profile> projectProfiles = new ArrayList<>();
    private final List<Profile> projectActiveProfiles = new ArrayList<>();
    private final List<org.apache.maven.settings.Profile> settingsProfiles = new ArrayList<>();

    @BeforeEach
    void setup() throws IOException {
        when(mavenSession.getProjects()).thenReturn(Collections.singletonList(project));
        when(mavenSession.getSettings()).thenReturn(settings);
        when(settings.getProfiles()).thenReturn(settingsProfiles);

        when(project.getActiveProfiles()).thenReturn(projectActiveProfiles);
        when(project.getModel()).thenReturn(projectModel);
        when(projectModel.getProfiles()).thenReturn(projectProfiles);

        outputPath = Files.createTempFile(tempDir, "maven-help-plugin-test-", ".txt");
        mavenSession.getUserProperties().setProperty("outputPath", outputPath.toString());
    }

    /**
     * Tests the case when no profiles are present for the projects.
     *
     * @throws Exception in case of errors.
     */
    @Test
    @InjectMojo(goal = "all-profiles")
    void noProfiles(AllProfilesMojo mojo) throws Exception {
        mojo.execute();

        verify(log).warn("No profiles detected!");
    }

    /**
     * Tests the case when profiles are present in the POM and in a parent POM.
     *
     * @throws Exception in case of errors.
     */
    @Test
    @InjectMojo(goal = "all-profiles")
    @MojoParameter(name = "output", value = "${outputPath}")
    void profileFromPom(AllProfilesMojo mojo) throws Exception {
        projectProfiles.add(newPomProfile("pro-1", "pom"));
        projectProfiles.add(newPomProfile("pro-2", "pom"));

        Model parentModel = mock(Model.class);
        when(parentModel.getProfiles()).thenReturn(Collections.singletonList(newPomProfile("pro-3", "pom")));
        MavenProject parentProject = mock(MavenProject.class);
        when(parentProject.getModel()).thenReturn(parentModel);
        when(project.getParent()).thenReturn(parentProject);

        projectActiveProfiles.add(newPomProfile("pro-1", "pom"));

        mojo.execute();

        String file = readOutput();
        assertTrue(file.contains("Profile Id: pro-1 (Active: true, Source: pom)"));
        assertTrue(file.contains("Profile Id: pro-2 (Active: false, Source: pom)"));
        assertTrue(file.contains("Profile Id: pro-3 (Active: false, Source: pom)"));
    }

    /**
     * Tests the case when active profiles are present in the parent POM.
     *
     * @throws Exception in case of errors.
     */
    @Test
    @InjectMojo(goal = "all-profiles")
    @MojoParameter(name = "output", value = "${outputPath}")
    void profileFromParentPom(AllProfilesMojo mojo) throws Exception {
        Model parentModel = mock(Model.class);
        when(parentModel.getProfiles()).thenReturn(Collections.singletonList(newPomProfile("pro-1", "pom")));
        MavenProject parentProject = mock(MavenProject.class);
        when(parentProject.getModel()).thenReturn(parentModel);
        when(parentProject.getActiveProfiles()).thenReturn(Collections.singletonList(newPomProfile("pro-1", "pom")));
        when(project.getParent()).thenReturn(parentProject);

        mojo.execute();

        String file = readOutput();
        assertTrue(file.contains("Profile Id: pro-1 (Active: true, Source: pom)"));
    }

    /**
     * Tests the case when profiles are present in the settings.
     *
     * @throws Exception in case of errors.
     */
    @Test
    @InjectMojo(goal = "all-profiles")
    @MojoParameter(name = "output", value = "${outputPath}")
    void profileFromSettings(AllProfilesMojo mojo) throws Exception {
        projectActiveProfiles.add(newPomProfile("settings-1", "settings.xml"));

        settingsProfiles.add(newSettingsProfile("settings-1"));
        settingsProfiles.add(newSettingsProfile("settings-2"));

        mojo.execute();

        String file = readOutput();
        assertTrue(file.contains("Profile Id: settings-1 (Active: true, Source: settings.xml)"));
        assertTrue(file.contains("Profile Id: settings-2 (Active: false, Source: settings.xml)"));
    }

    private Profile newPomProfile(String id, String source) {
        Profile profile = new Profile();
        profile.setId(id);
        profile.setSource(source);
        return profile;
    }

    private org.apache.maven.settings.Profile newSettingsProfile(String id) {
        org.apache.maven.settings.Profile profile = new org.apache.maven.settings.Profile();
        profile.setId(id);
        return profile;
    }

    private String readOutput() throws IOException {
        return new String(Files.readAllBytes(outputPath));
    }
}

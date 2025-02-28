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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.maven.api.Project;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.InputSource;
import org.apache.maven.api.model.Profile;
import org.apache.maven.api.plugin.MojoException;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.api.plugin.annotations.Parameter;
import org.apache.maven.impl.SettingsUtilsV4;

/**
 * Displays a list of available profiles under the current project.
 * <br>
 * <b>Note</b>: it will list <b>all</b> profiles for a project. If a
 * profile comes up with a status <b>inactive</b> then there might be a need to
 * set profile activation switches/property.
 *
 * @author <a href="mailto:rahul.thakur.xdev@gmail.com">Rahul Thakur</a>
 * @since 2.1
 */
@Mojo(name = "all-profiles", projectRequired = false)
public class AllProfilesMojo extends AbstractHelpMojo {
    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * This is the list of projects currently slated to be built by Maven.
     */
    @Parameter(defaultValue = "${session.projects}", required = true, readonly = true)
    private List<Project> projects;

    /**
     * The list of profiles defined in the current Maven settings.
     */
    @Parameter(defaultValue = "${settings.profiles}", readonly = true, required = true)
    private List<org.apache.maven.api.settings.Profile> settingsProfiles;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void execute() throws MojoException {
        StringBuilder descriptionBuffer = new StringBuilder();

        for (Project project : projects) {
            descriptionBuffer
                    .append("Listing Profiles for Project: ")
                    .append(project.getId())
                    .append(LS);

            Map<String, Profile> allProfilesByIds = new HashMap<>();
            Map<String, Profile> activeProfilesByIds = new HashMap<>();
            addSettingsProfiles(allProfilesByIds);
            addProjectPomProfiles(project, allProfilesByIds, activeProfilesByIds);

            // now display
            if (allProfilesByIds.isEmpty()) {
                getLog().warn("No profiles detected!");
            } else {
                // active Profiles will be a subset of *all* profiles
                allProfilesByIds.keySet().removeAll(activeProfilesByIds.keySet());

                for (Profile profile : activeProfilesByIds.values()) {
                    writeProfileDescription(descriptionBuffer, profile, true);
                }
                for (Profile profile : allProfilesByIds.values()) {
                    writeProfileDescription(descriptionBuffer, profile, false);
                }
            }
        }

        if (output != null) {
            try {
                writeFile(output, descriptionBuffer);
            } catch (IOException e) {
                throw new MojoException("Cannot write profiles description to output: " + output, e);
            }

            getLog().info("Wrote descriptions to: " + output);
        } else {
            getLog().info(descriptionBuffer.toString());
        }
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    private void writeProfileDescription(StringBuilder sb, Profile profile, boolean active) {
        String source = getProfileSource(profile);
        sb.append("  Profile Id: ").append(profile.getId());
        sb.append(" (Active: ")
                .append(active)
                .append(", Source: ")
                .append(source)
                .append(")");
        sb.append(LS);
    }

    private static String getProfileSource(Profile profile) {
        InputLocation location = profile.getLocation("");
        InputSource src = location != null ? location.getSource() : null;
        String loc = src != null ? src.getLocation() != null ? src.getLocation() : src.getModelId() : null;
        String source = loc != null ? loc : profile.getSource();
        return source;
    }

    /**
     * Adds the profiles from <code>pom.xml</code> and all of its parents.
     *
     * @param project could be null
     * @param allProfiles Map to add the profiles to.
     * @param activeProfiles Map to add the active profiles to.
     */
    private void addProjectPomProfiles(
            Project project, Map<String, Profile> allProfiles, Map<String, Profile> activeProfiles) {
        for (Profile profile : project.getEffectiveProfiles()) {
            allProfiles.put(profile.getId(), profile);
        }
        for (Profile profile : project.getEffectiveActiveProfiles()) {
            activeProfiles.put(profile.getId(), profile);
        }
    }

    /**
     * Adds the profiles from <code>settings.xml</code>.
     *
     * @param allProfiles Map to add the profiles to.
     */
    private void addSettingsProfiles(Map<String, Profile> allProfiles) {
        getLog().debug("Attempting to read profiles from settings.xml...");
        for (org.apache.maven.api.settings.Profile settingsProfile : settingsProfiles) {
            Profile profile = SettingsUtilsV4.convertFromSettingsProfile(settingsProfile);
            allProfiles.put(profile.getId(), profile);
        }
    }
}

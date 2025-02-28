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
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.api.ArtifactCoordinates;
import org.apache.maven.api.DownloadedArtifact;
import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.plugin.Log;
import org.apache.maven.api.plugin.Mojo;
import org.apache.maven.api.plugin.MojoException;
import org.apache.maven.api.plugin.annotations.Parameter;
import org.apache.maven.api.services.ArtifactResolverException;
import org.apache.maven.api.services.ProjectBuilder;
import org.apache.maven.api.services.ProjectBuilderRequest;
import org.apache.maven.api.services.ProjectManager;
import org.codehaus.plexus.util.io.CachingWriter;

/**
 * Base class with some Help Mojo functionalities.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @since 2.1
 */
public abstract class AbstractHelpMojo implements Mojo {
    /** The maximum length of a display line. */
    protected static final int LINE_LENGTH = 79;

    /** The line separator for the current OS. */
    protected static final String LS = System.lineSeparator();

    /**
     * Current Maven project.
     */
    @Inject
    protected Project project;

    /**
     * The current build session instance.
     */
    @Inject
    protected Session session;

    @Inject
    protected Log log;

    public Log getLog() {
        return log;
    }

    public void setLog(Log log) {
        this.log = log;
    }

    /**
     * Optional parameter to write the output of this help in a given file, instead of writing to the console.
     * <br>
     * <b>Note</b>: Could be a relative path.
     */
    @Parameter(property = "output")
    protected Path output;

    /**
     * Utility method to write a content in a given file.
     *
     * @param output is the wanted output file.
     * @param content contains the content to be written to the file.
     * @throws IOException if any
     * @see #writeFile(Path, String)
     */
    protected static void writeFile(Path output, StringBuilder content) throws IOException {
        writeFile(output, content.toString());
    }

    /**
     * Utility method to write a content in a given file.
     *
     * @param output is the wanted output file.
     * @param content contains the content to be written to the file.
     * @throws IOException if any
     */
    protected static void writeFile(Path output, String content) throws IOException {
        if (output == null) {
            return;
        }
        Files.createDirectories(output.getParent());
        try (Writer out = new CachingWriter(output, StandardCharsets.UTF_8)) {
            out.write(content);
        }
    }

    /**
     * Parses the given String into GAV artifact coordinate information, adding the given type.
     *
     * @param artifactString should respect the format <code>groupId:artifactId[:version]</code>
     * @param type The extension for the artifact, must not be <code>null</code>.
     * @return the <code>Artifact</code> object for the <code>artifactString</code> parameter.
     * @throws MojoException if the <code>artifactString</code> doesn't respect the format.
     */
    protected ArtifactCoordinates getArtifactCoordinates(String artifactString, String type) throws MojoException {
        if (artifactString == null || artifactString.isEmpty()) {
            throw new IllegalArgumentException("artifact parameter could not be empty");
        }

        String groupId; // required
        String artifactId; // required
        String version; // optional

        String[] artifactParts = artifactString.split(":");
        switch (artifactParts.length) {
            case 2:
                groupId = artifactParts[0];
                artifactId = artifactParts[1];
                version = "LATEST";
                break;
            case 3:
                groupId = artifactParts[0];
                artifactId = artifactParts[1];
                version = artifactParts[2];
                break;
            default:
                throw new MojoException("The artifact parameter '" + artifactString + "' should be conform to: "
                        + "'groupId:artifactId[:version]'.");
        }

        return session.createArtifactCoordinates(groupId, artifactId, version, type);
    }

    /**
     * Retrieves the Maven Project associated with the given artifact String, in the form of
     * <code>groupId:artifactId[:version]</code>. This resolves the POM artifact at those coordinates and then builds
     * the Maven project from it.
     *
     * @param artifactString Coordinates of the Maven project to get.
     * @return New Maven project.
     * @throws MojoException If there was an error while getting the Maven project.
     */
    protected Project getMavenProject(String artifactString) throws MojoException {
        try {
            DownloadedArtifact artifact = resolveArtifact(getArtifactCoordinates(artifactString, "pom"));

            ProjectBuilder projectBuilder = session.getService(ProjectBuilder.class);
            ProjectBuilderRequest request = ProjectBuilderRequest.builder()
                    .session(session.withRemoteRepositories(
                            session.getService(ProjectManager.class).getRemoteProjectRepositories(project)))
                    .path(artifact.getPath())
                    .processPlugins(false)
                    .build();
            return projectBuilder.build(request).getProject().orElseThrow();
        } catch (Exception e) {
            throw new MojoException(
                    "Unable to get the POM for the artifact '" + artifactString + "'. Verify the artifact parameter.",
                    e);
        }
    }

    protected DownloadedArtifact resolveArtifact(ArtifactCoordinates artifact) throws ArtifactResolverException {

        // TODO: do we need an additional indirection to support relocation ?
        Session s = session.withRemoteRepositories(
                session.getService(ProjectManager.class).getRemoteProjectRepositories(project));
        return s.resolveArtifact(artifact);
    }
}

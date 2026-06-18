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
import java.util.Map;
import java.util.TreeMap;

import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.ProjectBuilder;
import org.eclipse.aether.RepositorySystem;

/**
 * Displays a list of artifact handlers that are defined in Maven.
 *
 * @since 3.5.2
 */
@Mojo(name = "list-dependency-types", requiresProject = false, aggregator = true)
public class ListDependencyTypesMojo extends AbstractHelpMojo {
    /**
     * The Maven default built-in lifecycles.
     */
    private final Map<String, ArtifactHandler> artifactHandlers;

    @Inject
    public ListDependencyTypesMojo(
            ProjectBuilder projectBuilder,
            RepositorySystem repositorySystem,
            Map<String, ArtifactHandler> artifactHandlers) {
        super(projectBuilder, repositorySystem);
        this.artifactHandlers = artifactHandlers;
    }

    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            StringBuilder descriptionBuffer = new StringBuilder();
            for (Map.Entry<String, ArtifactHandler> handlerEntry : new TreeMap<>(artifactHandlers).entrySet()) {
                if ("default".equals(handlerEntry.getKey())) {
                    continue;
                }
                descriptionBuffer.append(handlerEntry.getKey()).append(LS);
                ArtifactHandler handler = handlerEntry.getValue();
                descriptionBuffer
                        .append(" - Extension: ")
                        .append("*.")
                        .append(handler.getExtension())
                        .append(LS);
                if (handler.getClassifier() != null && !handler.getClassifier().isEmpty()) {
                    descriptionBuffer
                            .append(" - Classifier: ")
                            .append(handler.getClassifier())
                            .append(LS);
                }
                if (handler.isAddedToClasspath()) {
                    descriptionBuffer.append(" - Added to Classpath").append(LS);
                }
                if (handler.isIncludesDependencies()) {
                    descriptionBuffer.append(" - Includes dependencies").append(LS);
                }
                descriptionBuffer.append(LS);
            }
            getLog().info(LS + "Maven Dependency Types defined:" + LS + LS + descriptionBuffer);
            writeFile(output, descriptionBuffer);
        } catch (IOException e) {
            throw new MojoFailureException(e);
        }
    }
}

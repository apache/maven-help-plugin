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

import org.apache.maven.lifecycle.mapping.Lifecycle;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.lifecycle.mapping.LifecycleMojo;
import org.apache.maven.lifecycle.mapping.LifecyclePhase;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.ProjectBuilder;
import org.eclipse.aether.RepositorySystem;

/**
 * Displays a list of packaging that are defined in Maven.
 *
 * @since 3.5.2
 */
@Mojo(name = "describe-packaging", requiresProject = false, aggregator = true)
public class DescribePackagingMojo extends AbstractHelpMojo {
    /**
     * The Maven default built-in lifecycles.
     */
    private final Map<String, LifecycleMapping> lifecycleMapping;

    @Inject
    public DescribePackagingMojo(
            ProjectBuilder projectBuilder,
            RepositorySystem repositorySystem,
            Map<String, LifecycleMapping> lifecycleMapping) {
        super(projectBuilder, repositorySystem);
        this.lifecycleMapping = lifecycleMapping;
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
            for (Map.Entry<String, LifecycleMapping> mappingEntry : new TreeMap<>(lifecycleMapping).entrySet()) {
                LifecycleMapping mapping = mappingEntry.getValue();
                for (Map.Entry<String, Lifecycle> phaseEntry :
                        mapping.getLifecycles().entrySet()) {
                    Lifecycle lifecycle = phaseEntry.getValue();
                    descriptionBuffer
                            .append(mappingEntry.getKey())
                            .append(" (lifecycle: ")
                            .append(lifecycle.getId())
                            .append(")")
                            .append(LS);
                    for (Map.Entry<String, LifecyclePhase> phaseE :
                            lifecycle.getLifecyclePhases().entrySet()) {
                        descriptionBuffer.append("  - ").append(phaseE.getKey()).append(LS);
                        for (LifecycleMojo mojo : phaseE.getValue().getMojos()) {
                            descriptionBuffer
                                    .append("    - ")
                                    .append(mojo.getGoal())
                                    .append(LS);
                        }
                    }
                }
                descriptionBuffer.append(LS);
            }
            getLog().info(LS + "Maven packaging defined:" + LS + LS + descriptionBuffer);
            writeFile(output, descriptionBuffer);
        } catch (IOException e) {
            throw new MojoFailureException(e);
        }
    }
}

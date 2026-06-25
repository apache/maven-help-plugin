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
import java.util.List;

import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.ProjectBuilder;
import org.eclipse.aether.RepositorySystem;

/**
 * Displays the list of all lifecycle phases that are defined in Maven.
 *
 * @since 3.5.2
 */
@Mojo(name = "list-lifecycle-phases", requiresProject = false, aggregator = true)
public class ListLifecyclePhasesMojo extends AbstractHelpMojo {
    /**
     * The Maven default built-in lifecycles.
     */
    private final DefaultLifecycles defaultLifecycles;

    @Inject
    public ListLifecyclePhasesMojo(
            ProjectBuilder projectBuilder, RepositorySystem repositorySystem, DefaultLifecycles defaultLifecycles) {
        super(projectBuilder, repositorySystem);
        this.defaultLifecycles = defaultLifecycles;
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
            List<Lifecycle> lifecycles = defaultLifecycles.getLifeCycles();
            for (Lifecycle lifecycle : lifecycles) {
                descriptionBuffer.append(lifecycle.getId()).append(LS);
                lifecycle
                        .getPhases()
                        .forEach(p -> descriptionBuffer.append(" * ").append(p).append(LS));
                descriptionBuffer.append(LS);
            }
            getLog().info(LS + "Maven lifecycles defined:" + LS + LS + descriptionBuffer);
            writeFile(output, descriptionBuffer);
        } catch (IOException e) {
            throw new MojoFailureException(e);
        }
    }
}

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

import java.util.Collections;
import java.util.List;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactDescriptorRequest;
import org.eclipse.aether.resolution.ArtifactDescriptorResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the coordinate parsing in {@link AbstractHelpMojo#getAetherArtifact(String, String)}.
 */
class AbstractHelpMojoTest {

    private final AbstractHelpMojo mojo = new AbstractHelpMojo(null, null) {
        @Override
        public void execute() {}
    };

    @Test
    void coordinatesWithoutAVersionGetTheLatestMetaversion() throws Exception {
        Artifact artifact = mojo.getAetherArtifact("org.apache.maven:maven-core", "pom");

        assertEquals("org.apache.maven", artifact.getGroupId());
        assertEquals("maven-core", artifact.getArtifactId());
        assertEquals("pom", artifact.getExtension());
        // The literal, not the constant: leaving the version out has to keep producing a
        // metaversion the resolver recognises. A null version would arrive here as "" instead.
        assertEquals("LATEST", artifact.getVersion());
    }

    @Test
    void coordinatesWithAVersionKeepIt() throws Exception {
        Artifact artifact = mojo.getAetherArtifact("org.apache.maven:maven-core:3.9.16", "pom");

        assertEquals("3.9.16", artifact.getVersion());
    }

    @Test
    void coordinatesWithTooManyPartsAreRejected() {
        assertThrows(
                MojoExecutionException.class,
                () -> mojo.getAetherArtifact("org.apache.maven:maven-core:3.9.16:extra", "pom"));
    }

    @Test
    void emptyCoordinatesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> mojo.getAetherArtifact("", "pom"));
    }

    @Test
    void resolveArtifactWithExplicitRepositoriesUsesThoseRepositories() throws Exception {
        RepositorySystem repositorySystem = mock(RepositorySystem.class);
        AbstractHelpMojo mojoWithExplicitRepos = new AbstractHelpMojo(null, repositorySystem) {
            @Override
            public void execute() {}
        };

        MavenSession session = mock(MavenSession.class);
        when(session.getRepositorySession()).thenReturn(mock(RepositorySystemSession.class));
        mojoWithExplicitRepos.session = session;

        Artifact artifact = new DefaultArtifact("org.test:test-plugin:1.0");
        ArtifactDescriptorResult descriptorResult =
                new ArtifactDescriptorResult(new ArtifactDescriptorRequest()).setArtifact(artifact);
        when(repositorySystem.readArtifactDescriptor(any(), any())).thenReturn(descriptorResult);
        ArtifactResult artifactResult = new ArtifactResult(new ArtifactRequest());
        when(repositorySystem.resolveArtifact(any(), any())).thenReturn(artifactResult);

        List<RemoteRepository> pluginRepositories = Collections.singletonList(
                new RemoteRepository.Builder("plugin-repo", "default", "https://plugin.example/").build());

        ArtifactResult result = mojoWithExplicitRepos.resolveArtifact(artifact, pluginRepositories);

        assertEquals(artifactResult, result);

        ArgumentCaptor<ArtifactDescriptorRequest> descriptorCaptor =
                ArgumentCaptor.forClass(ArtifactDescriptorRequest.class);
        verify(repositorySystem).readArtifactDescriptor(any(), descriptorCaptor.capture());
        assertEquals(pluginRepositories, descriptorCaptor.getValue().getRepositories());

        ArgumentCaptor<ArtifactRequest> requestCaptor = ArgumentCaptor.forClass(ArtifactRequest.class);
        verify(repositorySystem).resolveArtifact(any(), requestCaptor.capture());
        assertEquals(pluginRepositories, requestCaptor.getValue().getRepositories());
    }
}

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

import org.apache.maven.plugin.MojoExecutionException;
import org.eclipse.aether.artifact.Artifact;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
}

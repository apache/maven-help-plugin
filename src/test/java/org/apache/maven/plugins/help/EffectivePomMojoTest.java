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
import java.util.Collections;
import java.util.Properties;

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.apache.maven.api.plugin.testing.MojoExtension.setVariableValueToObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

/**
 * Test class for the effective-pom mojo of the Help Plugin.
 */
@ExtendWith(MockitoExtension.class)
@MojoTest
class EffectivePomMojoTest {

    @Inject
    private MavenProject project;

    @Inject
    private MavenSession mavenSession;

    @Mock
    private MojoExecution mojoExecution;

    @TempDir
    private Path tempDir;

    private final Model model = new Model();

    private final Properties originalProperties = new Properties();

    @BeforeEach
    void setup() throws IOException {
        originalProperties.setProperty("b.property", "b-value");
        originalProperties.setProperty("a.property", "a-value");

        model.setProperties(originalProperties);

        when(project.getModel()).thenReturn(model);

        Path outputPath = Files.createTempFile(tempDir, "maven-help-plugin-test-", ".xml");
        mavenSession.getUserProperties().setProperty("outputPath", outputPath.toString());
    }

    @InjectMojo(goal = "effective-pom")
    @MojoParameter(name = "output", value = "${outputPath}")
    void testExecuteDoesNotModifyProjectModel(EffectivePomMojo mojo) throws Exception {
        // snapshot of the contents before the mojo runs, to detect in-place modification
        Properties expectedProperties = new Properties();
        expectedProperties.putAll(originalProperties);

        setVariableValueToObject(mojo, "projects", Collections.singletonList(project));
        setVariableValueToObject(mojo, "mojoExecution", mojoExecution);

        mojo.execute();

        assertSame(
                originalProperties,
                model.getProperties(),
                "effective-pom must not replace the properties of the project model");

        assertEquals(
                expectedProperties,
                model.getProperties(),
                "effective-pom must not modify the properties of the project model");
    }
}

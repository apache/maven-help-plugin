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

import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.apache.maven.api.plugin.testing.MojoExtension.setVariableValueToObject;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/** Tests that list goals route output to either the file or the log. */
@ExtendWith(MockitoExtension.class)
@MojoTest
class ListMojoOutputTest {

    @Mock
    private Log log;

    @TempDir
    private Path tempDir;

    @Provides
    private Log provideLogger() {
        return log;
    }

    @Test
    @InjectMojo(goal = "list-dependency-types")
    void dependencyTypesWriteOnlyToConfiguredFile(ListDependencyTypesMojo mojo) throws Exception {
        Path output = tempDir.resolve("dependency-types.txt");
        setVariableValueToObject(mojo, "output", output.toFile());

        mojo.execute();

        assertFalse(new String(Files.readAllBytes(output)).isEmpty());
        verify(log, never()).info(anyString());
    }

    @Test
    @InjectMojo(goal = "list-lifecycle-phases")
    void lifecyclePhasesWriteOnlyToConfiguredFile(ListLifecyclePhasesMojo mojo) throws Exception {
        Path output = tempDir.resolve("lifecycle-phases.txt");
        setVariableValueToObject(mojo, "output", output.toFile());

        mojo.execute();

        assertFalse(new String(Files.readAllBytes(output)).isEmpty());
        verify(log, never()).info(anyString());
    }

    @Test
    @InjectMojo(goal = "list-packaging")
    void packagingWritesOnlyToConfiguredFile(ListPackagingMojo mojo) throws Exception {
        Path output = tempDir.resolve("packaging.txt");
        setVariableValueToObject(mojo, "output", output.toFile());

        mojo.execute();

        assertFalse(new String(Files.readAllBytes(output)).isEmpty());
        verify(log, never()).info(anyString());
    }
}

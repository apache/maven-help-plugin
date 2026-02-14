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

import org.apache.maven.model.InputLocation;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.*;

public class InputLocationFormatterFactoryTest {
    @Test
    public void whenNoSuchMethodThrownReturnsDefaultInputLocationFormatter() {
        // Arrange
        final Log log = mock(Log.class);
        final MavenProject project = mock(MavenProject.class);

        // Act
        final InputLocation.StringFormatter result = InputLocationFormatterFactory.produce(log, project);

        // Assert
        assertThat(result, instanceOf(DefaultInputLocationFormatter.class));
    }

    @Test
    public void whenMethodExistsReturnsMaven4InputLocationFormatter() {
        // Arrange
        InputLocationFormatterFactory.inputLocationClass = InputLocationStub.class;

        final Log log = mock(Log.class);
        final MavenProject project = mock(MavenProject.class);

        // Act
        final InputLocation.StringFormatter result = InputLocationFormatterFactory.produce(log, project);

        // Assert
        assertThat(result, instanceOf(ImportedFromLocationFormatter.class));
    }

    private static class InputLocationStub {
        public String getImportedFrom() {
            return "";
        }
    }
}

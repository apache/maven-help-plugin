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

import java.util.Stack;

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.project.MavenProject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ImportedFromLocationFormatterTest {

    @Test
    public void testImportedFromSingleLocation() {
        // Arrange
        final MavenProject project = new MavenProject();
        final ImportedFromLocationFormatter formatter = new ImportedFromLocationFormatterMock(project);

        final InputSource source = new InputSource();
        source.setModelId("org.example:MPG-183-project:1-SNAPSHOT");
        final InputLocation location = new InputLocation(7, 5, source);

        // Act
        final String result = formatter.toString(location);

        // Assert
        assertEquals("}org.example:MPG-183-project:1-SNAPSHOT, line 7", result);
    }

    @Test
    public void testImportedFromDifferentLocation() {
        // Arrange
        final InputSource importedFromSource = new InputSource();
        importedFromSource.setModelId("org.example:MPG-183-bom2:1-SNAPSHOT");
        final InputLocation importedFrom = new InputLocation(7, 5, importedFromSource);

        final MavenProject project = new MavenProject();
        final ImportedFromLocationFormatter formatter = new ImportedFromLocationFormatterMock(project, importedFrom);

        final InputSource source = new InputSource();
        source.setModelId("org.example:MPG-183-bom1:1-SNAPSHOT");
        final InputLocation location = new InputLocation(7, 5, source);

        // Act
        final String result = formatter.toString(location);

        // Assert
        assertEquals("}org.example:MPG-183-bom1:1-SNAPSHOT, line 7 from org.example:MPG-183-bom2:1-SNAPSHOT", result);
    }

    @Test
    public void testImportedFromDoNotPrintSameLocationTwice() {
        // Arrange
        final InputSource importedFromSource = new InputSource();
        importedFromSource.setModelId("org.example:MPG-183-bom:1-SNAPSHOT");
        final InputLocation importedFrom = new InputLocation(7, 5, importedFromSource);

        final MavenProject project = new MavenProject();
        final ImportedFromLocationFormatter formatter = new ImportedFromLocationFormatterMock(project, importedFrom);

        final InputSource source = new InputSource();
        source.setModelId("org.example:MPG-183-bom:1-SNAPSHOT");
        final InputLocation location = new InputLocation(7, 5, source);

        // Act
        final String result = formatter.toString(location);

        // Assert
        assertEquals("}org.example:MPG-183-bom:1-SNAPSHOT, line 7", result);
    }

    @Test
    public void testImportedFromMultiLevelPrintsWithFrom() {
        // Arrange
        final ImportedFromLocationFormatter formatter = createMultiImportedFromFormatter();

        final InputSource source = new InputSource();
        source.setModelId("org.example:MPG-183-project:1-SNAPSHOT");
        final InputLocation location = new InputLocation(7, 5, source);

        // Act
        final String result = formatter.toString(location);

        // Assert
        String expected =
                "}org.example:MPG-183-project:1-SNAPSHOT, line 7 from org.example:MPG-183-bom-2:1-SNAPSHOT from org.example:MPG-183-bom-1:1-SNAPSHOT";
        assertEquals(expected, result);
    }

    private static ImportedFromLocationFormatter createMultiImportedFromFormatter() {
        final InputSource importedFromSource1 = new InputSource();
        importedFromSource1.setModelId("org.example:MPG-183-bom-1:1-SNAPSHOT");
        final InputLocation importedFrom1 = new InputLocation(7, 5, importedFromSource1);

        final InputSource importedFromSource2 = new InputSource();
        importedFromSource2.setModelId("org.example:MPG-183-bom-2:1-SNAPSHOT");
        final InputLocation importedFrom2 = new InputLocation(7, 5, importedFromSource2);

        return new ImportedFromLocationFormatterMock(new MavenProject(), importedFrom1, importedFrom2);
    }

    private static class ImportedFromLocationFormatterMock extends ImportedFromLocationFormatter {
        private final Stack<InputLocation> mockedImportedFrom;

        public ImportedFromLocationFormatterMock(MavenProject project) {
            this(project, new Stack<>());
        }

        public ImportedFromLocationFormatterMock(MavenProject project, Stack<InputLocation> mockedImportedFrom) {
            super(null, project);
            this.mockedImportedFrom = mockedImportedFrom;
        }

        public ImportedFromLocationFormatterMock(MavenProject project, InputLocation... mockedImportedFrom) {
            super(null, project);

            this.mockedImportedFrom = new Stack<>();
            for (InputLocation location : mockedImportedFrom) {
                this.mockedImportedFrom.push(location);
            }
        }

        @Override
        protected InputLocation getImportedFrom(InputLocation location) {
            return !mockedImportedFrom.isEmpty() ? mockedImportedFrom.pop() : null;
        }
    }
}

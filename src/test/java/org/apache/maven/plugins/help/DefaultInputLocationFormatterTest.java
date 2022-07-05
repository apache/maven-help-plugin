package org.apache.maven.plugins.help;

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

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.StringContains.containsString;

public class DefaultInputLocationFormatterTest
{
    private final InputLocation.StringFormatter formatter = new DefaultInputLocationFormatter();

    @Test
    public void withLineNumberShouldIncludeLineNumber() {
        // Arrange
        final InputSource source = new InputSource();
        source.setModelId( "foo:bar:1.0-SNAPSHOT" );
        source.setLocation( "/tmp/project/pom.xml" );
        final InputLocation location = new InputLocation( 3, 5, source );

        // Act
        final String result = formatter.toString( location );

        // Assert
        assertThat( result, containsString( "line 3" ) );
    }

    @Test
    public void withoutLineNumberShouldNotIncludeLineNumber() {
        // Arrange
        final InputSource source = new InputSource();
        source.setModelId( "foo:bar:1.0-SNAPSHOT" );
        source.setLocation( "/tmp/project/pom.xml" );
        final InputLocation location = new InputLocation( -1, -1, source );

        // Act
        final String result = formatter.toString( location );

        // Assert
        assertThat( result, not( containsString( "line" ) ) );
    }

    @Test
    public void withModelIdShouldIncludeModelId() {
        // Arrange
        final InputSource source = new InputSource();
        source.setModelId( "foo:bar:1.0-SNAPSHOT" );
        source.setLocation( "/tmp/project/pom.xml" );
        final InputLocation location = new InputLocation( 3, 5, source );

        // Act
        final String result = formatter.toString( location );

        // Assert
        assertThat( result, containsString( "foo:bar:1.0-SNAPSHOT" ) );
    }

    @Test
    public void withoutModelIdShouldIncludeUnknownVersion() {
        // Arrange
        final InputSource source = new InputSource();
        source.setLocation( "/tmp/project/pom.xml" );
        final InputLocation location = new InputLocation( 3, 5, source );

        // Act
        final String result = formatter.toString( location );

        // Assert
        assertThat( result, not( containsString( "foo:bar:1.0-SNAPSHOT" ) ) );
    }
}
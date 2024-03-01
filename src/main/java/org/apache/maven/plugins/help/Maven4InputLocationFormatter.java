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

import org.apache.maven.model.Dependency;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.InputSource;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Maven 4.x-based implementation of {@link InputLocation.StringFormatter}. Enhances the default implementation
 * with support for following "references" (caused by e.g. dependency management imports).
 */
public class Maven4InputLocationFormatter extends InputLocation.StringFormatter
{
    private final Method getImportedFromMethod;
    private final MavenProject project;

    public Maven4InputLocationFormatter( final Method getImportedFromMethod, final MavenProject project )
    {
        this.getImportedFromMethod = getImportedFromMethod;
        this.project = project;
    }

    @Override
    public String toString( InputLocation location )
    {
        InputSource source = location.getSource();

        String s = source.getModelId(); // by default, display modelId

        if ( StringUtils.isBlank( s ) || s.contains( "[unknown-version]" ) )
        {
            // unless it is blank or does not provide version information
            s = source.toString();
        }

        InputLocation importedFrom = getImportedFrom( location );

        StringBuilder p = new StringBuilder();

        while ( importedFrom != null )
        {
            p.append( " from " ).append( importedFrom.getSource().getModelId() );
            importedFrom = getImportedFrom( importedFrom );
        }

        return '}' + s + ( ( location.getLineNumber() >= 0 ) ? ", line " + location.getLineNumber() : "" ) + p;
    }

    private InputLocation getImportedFrom( final InputLocation location )
    {
        try
        {
            InputLocation result = (InputLocation) getImportedFromMethod.invoke( location );

            // TODO: Replace black magic
            if ( result == null && project != null )
            {
                for ( Dependency dependency : project.getDependencyManagement().getDependencies() )
                {
                    InputLocation groupIdLocation = dependency.getLocation( "groupId" );
                    InputLocation artifactIdLocation = dependency.getLocation( "artifactId" );
                    InputLocation versionLocation = dependency.getLocation( "version" );

                    if ( groupIdLocation != null && groupIdLocation.toString().equals( location.toString() ) )
                    {
                        result = (InputLocation) Dependency.class.getMethod( "getImportedFrom" )
                                .invoke( dependency );
                        break;
                    }
                    if ( artifactIdLocation != null && artifactIdLocation.toString().equals( location.toString() ) )
                    {
                        result = (InputLocation) Dependency.class.getMethod( "getImportedFrom" )
                                .invoke( dependency );
                        break;
                    }
                    if ( versionLocation != null && versionLocation.toString().equals( location.toString() ) )
                    {
                        result = (InputLocation) Dependency.class.getMethod( "getImportedFrom" )
                                .invoke( dependency );
                        break;
                    }
                }
            }

            return result;
        }
        catch ( IllegalAccessException | InvocationTargetException | NoSuchMethodException e )
        {
            throw new RuntimeException( e );
        }
    }
}

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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

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

    protected InputLocation getImportedFrom( final InputLocation location )
    {
        try
        {
            InputLocation result = (InputLocation) getImportedFromMethod.invoke( location );

            if ( result == null && project != null )
            {
                for ( Dependency dependency : project.getDependencyManagement().getDependencies() )
                {
                    Set<?> locationKeys = getLocationKeys( dependency );
                    for ( Object key : locationKeys )
                    {
                        if ( !( key instanceof String ) )
                        {
                            throw new RuntimeException( "Expected a String, got " + key.getClass().getName() );
                        }

                        InputLocation dependencyLocation = dependency.getLocation( key );
                        if ( dependencyLocation != null && dependencyLocation.toString().equals( location.toString() ) )
                        {
                            result = ( InputLocation ) Dependency.class.getMethod( "getImportedFrom" )
                                    .invoke( dependency );
                            break;
                        }
                    }
                }
            }

            return result;
        }
        catch ( IllegalAccessException | InvocationTargetException | NoSuchMethodException | NoSuchFieldException
                | ClassNotFoundException e )
        {
            throw new RuntimeException( e );
        }
    }

    private Set<?> getLocationKeys( Dependency dependency ) throws NoSuchFieldException, IllegalAccessException
            , ClassNotFoundException
    {
        Field delegateField = Class.forName( "org.apache.maven.model.BaseObject" ).getDeclaredField( "delegate" );
        delegateField.setAccessible( true );
        Object delegate = delegateField.get( dependency );
        delegateField.setAccessible( false );

        Field locationsField = delegate.getClass().getDeclaredField( "locations" );
        locationsField.setAccessible( true );
        Object locations = locationsField.get( delegate );
        locationsField.setAccessible( false );

        if ( !( locations instanceof Map ) )
        {
            throw new RuntimeException( "Expected a Map, got " + locations.getClass().getName() );
        }

        return ( (Map<?, ?>) locations ).keySet();
    }
}

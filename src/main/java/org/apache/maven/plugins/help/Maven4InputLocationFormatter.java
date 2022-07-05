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
import org.codehaus.plexus.util.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Maven 4.x-based implementation of {@link InputLocation.StringFormatter}. Enhances the default implementation
 * with support for following "references" (caused by e.g. dependency management imports).
 */
public class Maven4InputLocationFormatter extends InputLocation.StringFormatter
{
    private final Method getReferencedByMethod;

    public Maven4InputLocationFormatter( final Method getReferencedByMethod )
    {
        this.getReferencedByMethod = getReferencedByMethod;
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

        InputLocation referencedBy = getReferencedBy( location );

        StringBuilder p = new StringBuilder();

        while ( referencedBy != null )
        {
            p.append( " from " ).append( referencedBy.getSource().getModelId() );
            referencedBy = getReferencedBy( referencedBy );
        }

        return '}' + s + ( ( location.getLineNumber() >= 0 ) ? ", line " + location.getLineNumber() : "" ) + p;
    }

    private InputLocation getReferencedBy( final InputLocation location )
    {
        try
        {
            return (InputLocation) getReferencedByMethod.invoke( location );
        }
        catch ( IllegalAccessException | InvocationTargetException e )
        {
            throw new RuntimeException( e );
        }
    }
}

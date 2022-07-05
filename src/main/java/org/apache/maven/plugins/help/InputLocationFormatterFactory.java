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
import org.apache.maven.plugin.logging.Log;

import java.lang.reflect.Method;

/**
 * Selects the most suitable implementation for {@link InputLocation.StringFormatter}.
 */
public class InputLocationFormatterFactory
{
    public static InputLocation.StringFormatter produce( final Log log )
    {
        try
        {
            // This method was introduced in Maven 4.
            Method getReferencedByMethod = InputLocation.class.getDeclaredMethod( "getReferencedBy", InputLocation.class );
            return new Maven4InputLocationFormatter( getReferencedByMethod );
        }
        catch ( NoSuchMethodException nsme )
        {
            // Fallback to pre-Maven 4 implementation.
             log.info( "Unable to print chain of POM imports, falling back to printing the source POM "
                     + "without import information. This feature is available in Maven 4.0.0+." );
            return new DefaultInputLocationFormatter();
        }
    }
}

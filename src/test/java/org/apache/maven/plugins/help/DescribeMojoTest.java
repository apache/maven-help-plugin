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

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.internal.MojoDescriptorCreator;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.version.PluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionResolver;
import org.apache.maven.plugin.version.PluginVersionResult;
import org.apache.maven.plugins.help.DescribeMojo.PluginInfo;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.exec.MavenPluginManagerHelper;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.InvocationTargetException;

import static org.apache.commons.lang3.reflect.FieldUtils.writeDeclaredField;
import static org.apache.commons.lang3.reflect.FieldUtils.writeField;
import static org.apache.commons.lang3.reflect.MethodUtils.invokeMethod;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
public class DescribeMojoTest
{
    /**
     * Test method for {@link org.apache.maven.plugins.help.DescribeMojo#toLines(java.lang.String, int, int, int)}.
     *
     */
    @Test
    public void testGetExpressionsRoot()
    {
        try
        {
            DescribeMojo describeMojo = new DescribeMojo();
            invokeMethod( describeMojo, true, "toLines", "", 2, 2, 80 );
        }
        catch ( Throwable e )
        {
            fail( "The API changes" );
        }
    }
    
    @Test
    public void testValidExpression()
        throws Exception
    {
        StringBuilder sb = new StringBuilder();
        MojoDescriptor md = new MojoDescriptor();
        Parameter parameter = new Parameter();
        parameter.setName( "name" );
        parameter.setExpression( "${valid.expression}" );
        md.addParameter( parameter );
        
        String ls = System.getProperty( "line.separator" );
        
        try
        {
            invokeMethod( new DescribeMojo(), true, "describeMojoParameters", md, sb );

            assertEquals(
                    "  Available parameters:" + ls + ls + "    name" + ls + "      User property: valid.expression" + ls + "      (no description available)" + ls,
                    sb.toString() );
        }
        catch ( Throwable e )
        {
            fail( e.getMessage() );
        }
    }
    
    @Test
    public void testInvalidExpression()
        throws Exception
    {
        StringBuilder sb = new StringBuilder();
        MojoDescriptor md = new MojoDescriptor();
        Parameter parameter = new Parameter();
        parameter.setName( "name" );
        parameter.setExpression( "${project.build.directory}/generated-sources/foobar" ); //this is a defaultValue
        md.addParameter( parameter );
        
        String ls = System.getProperty( "line.separator" );
        
        try
        {
            invokeMethod( new DescribeMojo(), true, "describeMojoParameters", md, sb );

            assertEquals( "  Available parameters:" + ls +
                          ls +
                          "    name" + ls +
                          "      Expression: ${project.build.directory}/generated-sources/foobar" + ls +
                          "      (no description available)" + ls, sb.toString() );
        }
        catch ( Throwable e )
        {
            fail( e.getMessage() );
        }
        
    }
    
    @Test
    public void testParsePluginInfoGAV()
        throws Throwable
    {
        DescribeMojo mojo = new DescribeMojo();
        writeDeclaredField( mojo, "groupId", "org.test", true );
        writeDeclaredField( mojo, "artifactId", "test", true );
        writeDeclaredField( mojo, "version", "1.0", true );

        PluginInfo pi = (PluginInfo) invokeMethod( mojo, true, "parsePluginLookupInfo" );

        assertEquals( "org.test", pi.getGroupId() );
        assertEquals( "test", pi.getArtifactId() );
        assertEquals( "1.0", pi.getVersion() );
        assertNull( pi.getPrefix() );
    }
    
    @Test
    public void testParsePluginInfoPluginPrefix()
        throws Throwable
    {
        DescribeMojo mojo = new DescribeMojo();
        writeDeclaredField( mojo, "plugin", "help", true );

        PluginInfo pi = (PluginInfo) invokeMethod( mojo, true, "parsePluginLookupInfo" );

        assertNull( pi.getGroupId() );
        assertNull( pi.getArtifactId() );
        assertNull( pi.getVersion() );
        assertEquals( "help", pi.getPrefix() );

        writeDeclaredField( mojo, "plugin", "help2:::", true );

        pi = (PluginInfo) invokeMethod( mojo, true, "parsePluginLookupInfo" );

        assertEquals( "help2", pi.getPrefix() );
    }
    
    @Test
    public void testParsePluginInfoPluginGA()
        throws Throwable
    {
        DescribeMojo mojo = new DescribeMojo();
        writeDeclaredField( mojo, "plugin", "org.test:test", true );

        PluginInfo pi = (PluginInfo) invokeMethod( mojo, true, "parsePluginLookupInfo" );

        assertEquals( "org.test", pi.getGroupId() );
        assertEquals( "test", pi.getArtifactId() );
        assertNull( pi.getVersion() );
        assertNull( pi.getPrefix() );
    }
    
    @Test
    public void testParsePluginInfoPluginGAV()
        throws Throwable
    {
        DescribeMojo mojo = new DescribeMojo();
        writeDeclaredField( mojo, "plugin", "org.test:test:1.0", true );

        PluginInfo pi = (PluginInfo) invokeMethod( mojo, true, "parsePluginLookupInfo" );

        assertEquals( "org.test", pi.getGroupId() );
        assertEquals( "test", pi.getArtifactId() );
        assertEquals( "1.0", pi.getVersion() );
        assertNull( pi.getPrefix() );
    }
    
    @Test
    public void testParsePluginInfoPluginIncorrect()
        throws Throwable
    {
        DescribeMojo mojo = new DescribeMojo();
        writeDeclaredField( mojo, "plugin", "org.test:test:1.0:invalid", true );
        try
        {
            invokeMethod( mojo, "parsePluginLookupInfo" );
            fail();
        }
        catch ( Exception e )
        {
            // expected
        }
    }
    
    @Test
    public void testLookupPluginDescriptorPrefixWithVersion()
        throws Throwable
    {
        DescribeMojo mojo = new DescribeMojo();

        PluginInfo pi = new PluginInfo();
        pi.setPrefix( "help" );
        pi.setVersion( "1.0" );

        Plugin plugin = new Plugin();
        plugin.setGroupId( "org.test" );
        plugin.setArtifactId( "test" );
        
        PluginDescriptor pd = new PluginDescriptor();

        MojoDescriptorCreator mojoDescriptorCreator = mock( MojoDescriptorCreator.class );
        PluginVersionResolver pluginVersionResolver = mock( PluginVersionResolver.class );
        MavenPluginManagerHelper pluginManager = mock( MavenPluginManagerHelper.class );
        MavenSession session = mock( MavenSession.class );
        writeDeclaredField( mojo, "mojoDescriptorCreator", mojoDescriptorCreator, true );
        writeDeclaredField( mojo, "pluginVersionResolver", pluginVersionResolver, true );
        writeDeclaredField( mojo, "pluginManager", pluginManager, true );
        writeField( mojo, "session", session, true );
        when( mojoDescriptorCreator.findPluginForPrefix( "help", session ) ).thenReturn( plugin );
        when( pluginManager.getPluginDescriptor( any( Plugin.class ), eq( session ) ) ).thenReturn( pd );

        PluginDescriptor returned = (PluginDescriptor) invokeMethod( mojo, true, "lookupPluginDescriptor", pi );

        assertEquals( pd, returned );

        verify( mojoDescriptorCreator ).findPluginForPrefix( "help", session );
        verify( pluginVersionResolver, never() ).resolve( any( PluginVersionRequest.class ) );
        ArgumentCaptor<Plugin> argument = ArgumentCaptor.forClass( Plugin.class );
        verify( pluginManager ).getPluginDescriptor( argument.capture(), eq( session ) );
        Plugin capturedPlugin = argument.getValue();
        assertEquals( "org.test", capturedPlugin.getGroupId() );
        assertEquals( "test", capturedPlugin.getArtifactId() );
        assertEquals( "1.0", capturedPlugin.getVersion() );
    }
    
    @Test
    public void testLookupPluginDescriptorPrefixWithoutVersion()
        throws Throwable
    {
        DescribeMojo mojo = new DescribeMojo();

        PluginInfo pi = new PluginInfo();
        pi.setPrefix( "help" );

        Plugin plugin = new Plugin();
        plugin.setGroupId( "org.test" );
        plugin.setArtifactId( "test" );
        
        PluginDescriptor pd = new PluginDescriptor();

        MojoDescriptorCreator mojoDescriptorCreator = mock( MojoDescriptorCreator.class );
        PluginVersionResolver pluginVersionResolver = mock( PluginVersionResolver.class );
        MavenPluginManagerHelper pluginManager = mock( MavenPluginManagerHelper.class );
        PluginVersionResult versionResult = mock( PluginVersionResult.class );
        MavenSession session = mock( MavenSession.class );
        writeDeclaredField( mojo, "mojoDescriptorCreator", mojoDescriptorCreator, true );
        writeDeclaredField( mojo, "pluginVersionResolver", pluginVersionResolver, true );
        writeDeclaredField( mojo, "pluginManager", pluginManager, true );
        writeField( mojo, "session", session, true );
        writeDeclaredField( mojo, "project", new MavenProject(), true );
        when( mojoDescriptorCreator.findPluginForPrefix( "help", session ) ).thenReturn( plugin );
        when( pluginVersionResolver.resolve( any( PluginVersionRequest.class ) ) ).thenReturn( versionResult );
        when( versionResult.getVersion() ).thenReturn( "1.0" );
        when( pluginManager.getPluginDescriptor( any( Plugin.class ), eq( session ) ) ).thenReturn( pd );

        PluginDescriptor returned = (PluginDescriptor) invokeMethod( mojo, true, "lookupPluginDescriptor", pi );
        assertEquals( pd, returned );


        verify( mojoDescriptorCreator ).findPluginForPrefix( "help", session );
        ArgumentCaptor<PluginVersionRequest> versionArgument = ArgumentCaptor.forClass( PluginVersionRequest.class );
        verify( pluginVersionResolver ).resolve( versionArgument.capture() );
        assertEquals( "org.test", versionArgument.getValue().getGroupId() );
        assertEquals( "test", versionArgument.getValue().getArtifactId() );
        ArgumentCaptor<Plugin> argument = ArgumentCaptor.forClass( Plugin.class );
        verify( pluginManager ).getPluginDescriptor( argument.capture(), eq( session ) );
        Plugin capturedPlugin = argument.getValue();
        assertEquals( "org.test", capturedPlugin.getGroupId() );
        assertEquals( "test", capturedPlugin.getArtifactId() );
        assertEquals( "1.0", capturedPlugin.getVersion() );
    }
    
    @Test
    public void testLookupPluginDescriptorGAV()
        throws Throwable
    {
        DescribeMojo mojo = new DescribeMojo();

        PluginInfo pi = new PluginInfo();
        pi.setGroupId( "org.test" );
        pi.setArtifactId( "test" );
        pi.setVersion( "1.0" );

        PluginDescriptor pd = new PluginDescriptor();

        MojoDescriptorCreator mojoDescriptorCreator = mock( MojoDescriptorCreator.class );
        PluginVersionResolver pluginVersionResolver = mock( PluginVersionResolver.class );
        MavenPluginManagerHelper pluginManager = mock( MavenPluginManagerHelper.class );
        MavenSession session = mock( MavenSession.class );
        writeDeclaredField( mojo, "mojoDescriptorCreator", mojoDescriptorCreator, true );
        writeDeclaredField( mojo, "pluginVersionResolver", pluginVersionResolver, true );
        writeDeclaredField( mojo, "pluginManager", pluginManager, true );
        writeField( mojo, "session", session, true );
        when( pluginManager.getPluginDescriptor( any( Plugin.class ), eq( session ) ) ).thenReturn( pd );

        PluginDescriptor returned = (PluginDescriptor) invokeMethod( mojo, true, "lookupPluginDescriptor", pi );

        assertEquals( pd, returned );

        verify( mojoDescriptorCreator, never() ).findPluginForPrefix( any( String.class ), any( MavenSession.class ) );
        verify( pluginVersionResolver, never() ).resolve( any( PluginVersionRequest.class ) );
        ArgumentCaptor<Plugin> argument = ArgumentCaptor.forClass( Plugin.class );
        verify( pluginManager ).getPluginDescriptor( argument.capture(), eq( session ) );
        Plugin capturedPlugin = argument.getValue();
        assertEquals( "org.test", capturedPlugin.getGroupId() );
        assertEquals( "test", capturedPlugin.getArtifactId() );
        assertEquals( "1.0", capturedPlugin.getVersion() );
    }

    @Test
    public void testLookupPluginDescriptorGMissingA()
    {
        DescribeMojo mojo = new DescribeMojo();
        PluginInfo pi = new PluginInfo();
        pi.setGroupId( "org.test" );
        try
        {
            invokeMethod( mojo, true, "lookupPluginDescriptor", pi );
            fail();
        }
        catch ( InvocationTargetException e )
        {
            assertTrue( e.getTargetException()
                    .getMessage().startsWith( "You must specify either" ) );
        }
        catch ( NoSuchMethodException | IllegalAccessException e )
        {
            fail();
        }
    }
    
    @Test
    public void testLookupPluginDescriptorAMissingG()
    {
        DescribeMojo mojo = new DescribeMojo();
        PluginInfo pi = new PluginInfo();
        pi.setArtifactId( "test" );
        try
        {
            invokeMethod( mojo, true, "lookupPluginDescriptor", pi );
            fail();
        }
        catch ( InvocationTargetException e )
        {
            assertTrue( e.getTargetException()
                    .getMessage().startsWith( "You must specify either" ) );
        }
        catch ( Exception e)
        {
            fail( e.getMessage() );
        }
    }

}

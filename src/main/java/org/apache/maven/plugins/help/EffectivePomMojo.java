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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.model.InputSource;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.model.io.xpp3.MavenXpp3WriterExOldSupport;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecution.Source;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.logging.MessageUtils;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;
import org.codehaus.plexus.util.xml.XmlWriterUtil;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.pull.XmlSerializer;

/**
 * Displays the effective POM as an XML for this build, with the active profiles factored in, or a specified artifact.
 * If <code>verbose</code>, a comment is added to each XML element describing the origin of the line.
 *
 * @since 2.0
 */
@Mojo( name = "effective-pom", aggregator = true )
public class EffectivePomMojo
    extends AbstractEffectiveMojo
{
    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * The Maven project.
     *
     * @since 2.0.2
     */
    @Parameter( defaultValue = "${project}", readonly = true, required = true )
    private MavenProject project;

    /**
     * The projects in the current build. The effective-POM for
     * each of these projects will written.
     */
    @Parameter( defaultValue = "${reactorProjects}", required = true, readonly = true )
    private List<MavenProject> projects;

    /**
     * This mojo execution, used to determine if it was launched from the lifecycle or the command-line.
     */
    @Parameter( defaultValue = "${mojo}", required = true, readonly = true )
    private MojoExecution mojoExecution;

    /**
     * The artifact for which to display the effective POM.
     * <br>
     * <b>Note</b>: Should respect the Maven format, i.e. <code>groupId:artifactId[:version]</code>. The
     * latest version of the artifact will be used when no version is specified.
     *
     * @since 3.0.0
     */
    @Parameter( property = "artifact" )
    private String artifact;

    /**
     * Output POM input location as comments.
     * 
     * @since 3.2.0
     */
    @Parameter( property = "verbose", defaultValue = "false" )
    private boolean verbose = false;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void execute()
        throws MojoExecutionException
    {
        if ( StringUtils.isNotEmpty( artifact ) )
        {
            project = getMavenProject( artifact );
            projects = Collections.singletonList( project );
        }

        StringWriter w = new StringWriter();
        String encoding = output != null ? project.getModel().getModelEncoding()
                                : System.getProperty( "file.encoding" );
        XMLWriter writer =
            new PrettyPrintXMLWriter( w, StringUtils.repeat( " ", XmlWriterUtil.DEFAULT_INDENTATION_SIZE ),
                                      encoding, null );

        writeHeader( writer );

        if ( shouldWriteAllEffectivePOMsInReactor() )
        {
            // outer root element
            writer.startElement( "projects" );
            for ( MavenProject subProject : projects )
            {
                writeEffectivePom( subProject, writer );
            }
            writer.endElement();
        }
        else
        {
            writeEffectivePom( project, writer );
        }

        String effectivePom = prettyFormat( w.toString(), encoding, false );
        if ( verbose )
        {
            // tweak location tracking comment, that are put on a separate line by pretty print
            effectivePom = effectivePom.replaceAll( "(?m)>\\s+<!--}", ">  <!-- " );
        }

        if ( output != null )
        {
            try
            {
                writeXmlFile( output, effectivePom );
            }
            catch ( IOException e )
            {
                throw new MojoExecutionException( "Cannot write effective-POM to output: " + output, e );
            }

            getLog().info( "Effective-POM written to: " + output );
        }
        else
        {
            if ( MessageUtils.isColorEnabled() )
            {
                // add color to comments
                String comment = MessageUtils.buffer().project( "<!--.-->" ).toString();
                int dotIndex = comment.indexOf( "." );
                String commentStart = comment.substring( 0, dotIndex );
                String commentEnd = comment.substring( dotIndex + 1 );
                effectivePom = effectivePom.replaceAll( "<!--", commentStart ).replaceAll( "-->", commentEnd );
            }

            StringBuilder message = new StringBuilder();

            message.append( LS );
            message.append( "Effective POMs, after inheritance, interpolation, and profiles are applied:" );
            message.append( LS ).append( LS );
            message.append( effectivePom );
            message.append( LS );

            getLog().info( message.toString() );
        }
    }

    /**
     * Determines if all effective POMs of all the projects in the reactor should be written. When this goal is started
     * on the command-line, it is always the case. However, when it is bound to a phase in the lifecycle, it is only the
     * case when the current project being built is the head project in the reactor.
     *
     * @return <code>true</code> if all effective POMs should be written, <code>false</code> otherwise.
     */
    private boolean shouldWriteAllEffectivePOMsInReactor()
    {
        Source source = mojoExecution.getSource();
        // [MNG-5550] For Maven < 3.2.1, the source is null, instead of LIFECYCLE: only rely on comparisons with CLI
        return projects.size() > 1
            && ( source == Source.CLI || source != Source.CLI && projects.get( 0 ).equals( project ) );
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * Method for writing the effective pom informations of the current build.
     *
     * @param project the project of the current build, not null.
     * @param writer the XML writer , not null, not null.
     * @throws MojoExecutionException if any
     */
    private void writeEffectivePom( MavenProject project, XMLWriter writer )
        throws MojoExecutionException
    {
        Model pom = project.getModel();
        cleanModel( pom );

        StringWriter sWriter = new StringWriter();
        try
        {
            if ( verbose )
            {
                // try to use Maven core-provided xpp3 extended writer (available since Maven 3.6.1)
                if ( ! writeMavenXpp3WriterEx( sWriter, pom ) )
                {
                    // xpp3 extended writer not provided by Maven core, use local code
                    new EffectiveWriterExOldSupport().write( sWriter, pom );
                }
            }
            else
            {
                new MavenXpp3Writer().write( sWriter, pom );
            }
        }
        catch ( IOException e )
        {
            throw new MojoExecutionException( "Cannot serialize POM to XML.", e );
        }

        // This removes the XML declaration written by MavenXpp3Writer
        String effectivePom = prettyFormat( sWriter.toString(), null, true );

        writeComment( writer, "Effective POM for project \'" + project.getId() + "\'" );

        writer.writeMarkup( effectivePom );
    }

    /**
     * Apply some logic to clean the model before writing it.
     *
     * @param pom not null
     */
    private static void cleanModel( Model pom )
    {
        Properties properties = new SortedProperties();
        properties.putAll( pom.getProperties() );
        pom.setProperties( properties );
    }

    private void warnWriteMavenXpp3WriterEx( Throwable t )
    {
        getLog().warn( "Unexpected exception while running Maven Model Extended Writer, "
            + "falling back to old internal implementation.", t );
    }

    private boolean writeMavenXpp3WriterEx( Writer writer, Model model )
        throws IOException
    {
        try
        {
            Class<?> mavenXpp3WriterExClass = Class.forName( "org.apache.maven.model.io.xpp3.MavenXpp3WriterEx" );
            Object mavenXpp3WriterEx = mavenXpp3WriterExClass.newInstance();

            Method setStringFormatter =
                mavenXpp3WriterExClass.getMethod( "setStringFormatter", InputLocation.StringFormatter.class );
            setStringFormatter.invoke( mavenXpp3WriterEx, new InputLocationStringFormatter() );

            Method write = mavenXpp3WriterExClass.getMethod( "write", Writer.class, Model.class );
            write.invoke( mavenXpp3WriterEx, writer, model );

            return true;
        }
        catch ( ClassNotFoundException e )
        {
            // MavenXpp3WriterEx not available in running Maven version
        }
        catch ( NoSuchMethodException e )
        {
            warnWriteMavenXpp3WriterEx( e );
        }
        catch ( SecurityException e )
        {
            warnWriteMavenXpp3WriterEx( e );
        }
        catch ( InstantiationException e )
        {
            warnWriteMavenXpp3WriterEx( e );
        }
        catch ( IllegalAccessException e )
        {
            warnWriteMavenXpp3WriterEx( e );
        }
        catch ( IllegalArgumentException e )
        {
            warnWriteMavenXpp3WriterEx( e );
        }
        catch ( InvocationTargetException e )
        {
            if ( e.getTargetException() instanceof IOException )
            {
                throw (IOException) e.getTargetException();
            }
            else if ( e.getTargetException() instanceof RuntimeException )
            {
                throw (RuntimeException) e.getTargetException();
            }
            warnWriteMavenXpp3WriterEx( e );
        }
        return false;
    }

    private static String toString( InputLocation location )
    {
        InputSource source = location.getSource();

        String s = source.getModelId(); // by default, display modelId

        if ( StringUtils.isBlank( s ) || s.contains( "[unknown-version]" ) )
        {
            // unless it is blank or does not provide version information
            s = source.toString();
        }

        return '}' + s + ( ( location.getLineNumber() >= 0 ) ? ", line " + location.getLineNumber() : "" ) + ' ';
    }

    private static class InputLocationStringFormatter
        extends InputLocation.StringFormatter
    {

        public String toString( InputLocation location )
        {
            return EffectivePomMojo.toString( location );
        }

    }

    /**
     * Xpp3 extended writer extension to improve default InputSource display
     */
    private static class EffectiveWriterExOldSupport
        extends MavenXpp3WriterExOldSupport
    {

        @Override
        public String toString( InputLocation location )
        {
            return EffectivePomMojo.toString( location );
        }

        @Override
        protected void writeXpp3DomToSerializer( Xpp3Dom dom, XmlSerializer serializer )
            throws java.io.IOException
        {
            // default method uses Xpp3Dom input location tracking, not available in older Maven versions
            // use old Xpp3Dom serialization, without input location tracking
            dom.writeToSerializer( null, serializer );
        }
    }
}

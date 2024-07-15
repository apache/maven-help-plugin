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

import javax.xml.stream.XMLStreamException;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.List;
import java.util.TreeMap;

import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Project;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.model.InputLocation;
import org.apache.maven.api.model.InputSource;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.plugin.MojoException;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.api.plugin.annotations.Parameter;
import org.apache.maven.api.services.MessageBuilderFactory;
import org.apache.maven.model.v4.MavenStaxWriter;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;
import org.codehaus.plexus.util.xml.XmlWriterUtil;

/**
 * Displays the effective POM as an XML for this build, with the active profiles factored in, or a specified artifact.
 * If <code>verbose</code>, a comment is added to each XML element describing the origin of the line.
 *
 * @since 2.0
 */
@Mojo(name = "effective-pom", aggregator = true)
public class EffectivePomMojo extends AbstractEffectiveMojo {
    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * The projects in the current build. The effective-POM for
     * each of these projects will written.
     */
    @Parameter(defaultValue = "${session.projects}", required = true, readonly = true)
    private List<Project> projects;

    /**
     * This mojo execution, used to determine if it was launched from the lifecycle or the command-line.
     */
    @Inject
    private MojoExecution mojoExecution;

    /**
     * The artifact for which to display the effective POM.
     * <br>
     * <b>Note</b>: Should respect the Maven format, i.e. <code>groupId:artifactId[:version]</code>. The
     * latest version of the artifact will be used when no version is specified.
     *
     * @since 3.0.0
     */
    @Parameter(property = "artifact")
    private String artifact;

    /**
     * Output POM input location as comments.
     *
     * @since 3.2.0
     */
    @Parameter(property = "verbose", defaultValue = "false")
    private boolean verbose = false;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void execute() throws MojoException {
        if (artifact != null && !artifact.isEmpty()) {
            project = getMavenProject(artifact);
            projects = Collections.singletonList(project);
        }

        StringWriter w = new StringWriter();
        String encoding = output != null
                ? project.getModel().getModelEncoding()
                : Charset.defaultCharset().displayName();
        XMLWriter writer = new PrettyPrintXMLWriter(
                w, StringUtils.repeat(" ", XmlWriterUtil.DEFAULT_INDENTATION_SIZE), encoding, null);

        writeHeader(writer);

        if (shouldWriteAllEffectivePOMsInReactor()) {
            // outer root element
            writer.startElement("projects");
            for (Project subProject : projects) {
                writeEffectivePom(subProject, writer);
            }
            writer.endElement();
        } else {
            writeEffectivePom(project, writer);
        }

        String effectivePom = prettyFormat(w.toString(), encoding, false);
        if (verbose) {
            // tweak location tracking comment, that are put on a separate line by pretty print
            effectivePom = effectivePom.replaceAll("(?m)>\\s+<!--}", ">  <!-- ");
        }

        if (output != null) {
            try {
                writeFile(output, effectivePom);
            } catch (IOException e) {
                throw new MojoException("Cannot write effective-POM to output: " + output, e);
            }

            getLog().info("Effective-POM written to: " + output);
        } else {
            if (verbose) {
                MessageBuilderFactory mbf = session.getService(MessageBuilderFactory.class);
                if (mbf.isColorEnabled()) {
                    // add color to comments
                    String comment = mbf.builder().project("<!--.-->").toString();
                    int dotIndex = comment.indexOf(".");
                    String commentStart = comment.substring(0, dotIndex);
                    String commentEnd = comment.substring(dotIndex + 1);
                    effectivePom = effectivePom.replace("<!--", commentStart).replace("-->", commentEnd);
                }
            }

            if (getLog().isInfoEnabled()) {
                getLog().info(LS + "Effective POMs, after inheritance, interpolation, and profiles are applied:" + LS
                        + LS + effectivePom + LS);
            } else if (forceStdout) {
                System.out.println(effectivePom);
            }
        }
    }

    /**
     * Determines if all effective POMs of all the projects in the reactor should be written. When this goal is started
     * on the command-line, it is always the case. However, when it is bound to a phase in the lifecycle, it is only the
     * case when the current project being built is the head project in the reactor.
     *
     * @return <code>true</code> if all effective POMs should be written, <code>false</code> otherwise.
     */
    protected boolean shouldWriteAllEffectivePOMsInReactor() {
        return projects.size() > 1 && ("default-cli".equals(mojoExecution.getExecutionId()) || project.isRootProject());
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * Method for writing the effective pom informations of the current build.
     *
     * @param project the project of the current build, not null.
     * @param writer the XML writer , not null, not null.
     * @throws MojoException if any
     */
    private void writeEffectivePom(Project project, XMLWriter writer) throws MojoException {
        Model pom = cleanModel(project.getModel());

        StringWriter sWriter = new StringWriter();
        try {
            MavenStaxWriter w = new MavenStaxWriter();
            w.setAddLocationInformation(verbose);
            w.setStringFormatter(EffectivePomMojo::toString);
            w.write(sWriter, pom);
        } catch (XMLStreamException | IOException e) {
            throw new MojoException("Cannot serialize POM to XML.", e);
        }

        // This removes the XML declaration written by MavenXpp3Writer
        String effectivePom = prettyFormat(sWriter.toString(), null, true);

        writeComment(writer, "Effective POM for project '" + project.getId() + "'");

        writer.writeMarkup(effectivePom);
    }

    /**
     * Apply some logic to clean the model before writing it.
     *
     * @param pom not null
     */
    private static Model cleanModel(Model pom) {
        return pom.withProperties(new TreeMap<>(pom.getProperties()));
    }

    public static String toString(InputLocation location) {
        InputSource source = location.getSource();

        String s = source.getModelId(); // by default, display modelId

        if (StringUtils.isBlank(s) || s.contains("[unknown-version]")) {
            // unless it is blank or does not provide version information
            s = source.toString();
        }

        return '}' + s + ((location.getLineNumber() >= 0) ? ", line " + location.getLineNumber() : "") + ' ';
    }
}

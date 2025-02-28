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

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.zip.ZipFile;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.collections.PropertiesConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import org.apache.commons.lang3.ClassUtils;
import org.apache.maven.api.MojoExecution;
import org.apache.maven.api.Project;
import org.apache.maven.api.di.Inject;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.plugin.MojoException;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.api.plugin.annotations.Parameter;
import org.apache.maven.api.services.MavenException;
import org.apache.maven.api.services.Prompter;
import org.apache.maven.api.services.PrompterException;
import org.apache.maven.api.services.xml.ModelXmlFactory;
import org.apache.maven.api.services.xml.SettingsXmlFactory;
import org.apache.maven.api.services.xml.XmlWriterException;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.plugin.PluginParameterExpressionEvaluatorV4;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluationException;

/**
 * Evaluates Maven expressions given by the user in an interactive mode.
 *
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 * @since 2.1
 */
@Mojo(name = "evaluate", projectRequired = false)
public class EvaluateMojo extends AbstractHelpMojo {

    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    // we need to hide the 'output' defined in AbstractHelpMojo to have a correct "since".
    /**
     * Optional parameter to write the output of this help in a given file, instead of writing to the console.
     * This parameter will be ignored if no <code>expression</code> is specified.
     * <br/>
     * <b>Note</b>: Could be a relative path.
     *
     * @since 3.0.0
     */
    @Parameter(property = "output")
    private Path output;

    /**
     * This options gives the option to output information in cases where the output has been suppressed by using
     * <code>-q</code> (quiet option) in Maven. This is useful if you like to use
     * <code>maven-help-plugin:evaluate</code> in a script call (for example in bash) like this:
     *
     * <pre>
     * RESULT=$(mvn help:evaluate -Dexpression=project.version -q -DforceStdout)
     * echo $RESULT
     * </pre>
     *
     * This will only printout the information which has been requested by <code>expression</code> to
     * <code>stdout</code>.
     *
     * @since 3.1.0
     */
    @Parameter(property = "forceStdout", defaultValue = "false")
    private boolean forceStdout;

    /**
     * An artifact for evaluating Maven expressions. <br/>
     * <b>Note</b>: Should respect the Maven format, i.e. <code>groupId:artifactId[:version]</code>. The latest version
     * of the artifact will be used when no version is specified.
     */
    @Parameter(property = "artifact")
    private String artifact;

    /**
     * An expression to evaluate instead of prompting. Note that this <i>must not</i> include the surrounding ${...}.
     */
    @Parameter(property = "expression")
    private String expression;

    @Inject
    private MojoExecution mojoExecution;

    /**
     * The system settings for Maven.
     */
    @Parameter(property = "session.settings")
    private Settings settings;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void execute() throws MojoException {
        if (expression == null && !settings.isInteractiveMode()) {
            getLog().error("Maven is configured to NOT interact with the user for input. "
                    + "This Mojo requires that 'interactiveMode' in your settings file is flag to 'true'.");
            return;
        }

        validateParameters();

        if (artifact != null && !artifact.isEmpty()) {
            project = getMavenProject(artifact);
        }

        if (expression == null) {
            if (output != null) {
                getLog().warn("When prompting for input, the result will be written to the console, "
                        + "ignoring 'output'.");
            }
            while (true) {
                getLog().info("Enter the Maven expression i.e. ${project.groupId} or 0 to exit?:");

                try {
                    Prompter prompter = session.getService(Prompter.class);
                    String userExpression =
                            prompter.prompt("Enter the Maven expression i.e. ${project.groupId} or 0 to exit?");
                    if (userExpression == null || userExpression.equals("0")) {
                        break;
                    }

                    handleResponse(userExpression, null);
                } catch (PrompterException e) {
                    throw new MojoException("Unable to read from standard input.", e);
                }
            }
        } else {
            handleResponse("${" + expression + "}", output);
        }
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * Validate Mojo parameters.
     */
    private void validateParameters() {
        if (artifact == null) {
            // using project if found or super-pom
            getLog().info("No artifact parameter specified, using '" + project.getId() + "' as project.");
        }
    }

    /**
     * @return a lazy loading evaluator object.
     * @throws MojoException if any reflection exceptions occur or missing components.
     */
    private PluginParameterExpressionEvaluatorV4 getEvaluator() throws MojoException {
        return new PluginParameterExpressionEvaluatorV4(session, project, mojoExecution);
    }

    /**
     * @param expr the user expression asked.
     * @param output the file where to write the result, or <code>null</code> to print in standard output.
     * @throws MojoException if any reflection exceptions occur or missing components.
     */
    private void handleResponse(String expr, Path output) throws MojoException {
        StringBuilder response = new StringBuilder();

        Object obj;
        try {
            obj = getEvaluator().evaluate(expr);
        } catch (ExpressionEvaluationException e) {
            throw new MojoException("Error when evaluating the Maven expression", e);
        }

        if (obj != null && expr.equals(obj.toString())) {
            getLog().warn("The Maven expression was invalid. Please use a valid expression.");
            return;
        }

        // handle null
        if (obj == null) {
            response.append("null object or invalid expression");
        }
        // handle primitives objects
        else if (obj instanceof String) {
            response.append(obj.toString());
        } else if (obj instanceof Boolean) {
            response.append(obj.toString());
        } else if (obj instanceof Byte) {
            response.append(obj.toString());
        } else if (obj instanceof Character) {
            response.append(obj.toString());
        } else if (obj instanceof Double) {
            response.append(obj.toString());
        } else if (obj instanceof Float) {
            response.append(obj.toString());
        } else if (obj instanceof Integer) {
            response.append(obj.toString());
        } else if (obj instanceof Long) {
            response.append(obj.toString());
        } else if (obj instanceof Short) {
            response.append(obj.toString());
        }
        // handle specific objects
        else if (obj instanceof File f) {
            response.append(f.getAbsolutePath());
        } else if (obj instanceof Path p) {
            response.append(p.toAbsolutePath().toString());
        }
        // handle Maven pom object
        else if (obj instanceof Project projectAsked) {
            try {
                StringWriter sWriter = new StringWriter();
                session.getService(ModelXmlFactory.class).write(projectAsked.getModel(), sWriter);
                response.append(sWriter);
            } catch (XmlWriterException e) {
                throw new MojoException("Error when writing pom", e);
            }
        }
        // handle Maven Settings object
        else if (obj instanceof Settings settingsAsked) {
            try {
                StringWriter sWriter = new StringWriter();
                session.getService(SettingsXmlFactory.class).write(settingsAsked, sWriter);
                response.append(sWriter.toString());
            } catch (XmlWriterException e) {
                throw new MojoException("Error when writing settings", e);
            }
        } else {
            // others Maven objects
            response.append(toXML(expr, obj));
        }

        if (output != null) {
            try {
                writeFile(output, response);
            } catch (IOException e) {
                throw new MojoException("Cannot write evaluation of expression to output: " + output, e);
            }
            getLog().info("Result of evaluation written to: " + output);
        } else {
            if (getLog().isInfoEnabled()) {
                getLog().info(LS + response.toString());
            } else {
                if (forceStdout) {
                    System.out.print(response.toString());
                    System.out.flush();
                }
            }
        }
    }

    /**
     * @param expr the user expression.
     * @param obj a not null.
     * @return the XML for the given object.
     */
    private String toXML(String expr, Object obj) {
        XStream currentXStream = getXStream();

        // beautify list
        if (obj instanceof List<?> list) {
            if (!list.isEmpty()) {
                Object elt = list.iterator().next();

                String name = lowerCase(elt.getClass().getName());
                currentXStream.alias(pluralize(name), List.class);
            } else {
                // try to detect the alias from question
                if (expr.indexOf('.') != -1) {
                    String name = expr.substring(expr.indexOf('.') + 1, expr.indexOf('}'));
                    currentXStream.alias(name, List.class);
                }
            }
        }

        return currentXStream.toXML(obj);
    }

    /**
     * @return lazy loading xstream object.
     */
    private XStream getXStream() {
        XStream xstream = new XStream();
        addAlias(xstream);

        // handle Properties a la Maven
        xstream.registerConverter(new PropertiesConverter() {
            /** {@inheritDoc} */
            @Override
            public boolean canConvert(Class type) {
                return Map.class.isAssignableFrom(type);
            }

            /** {@inheritDoc} */
            @Override
            public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
                Map<?, ?> map = new TreeMap<>((Map) source); // sort
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    writer.startNode(entry.getKey().toString());
                    writer.setValue(entry.getValue() != null ? entry.getValue().toString() : null);
                    writer.endNode();
                }
            }
        });

        return xstream;
    }

    /**
     * @param xstreamObject not null
     */
    private void addAlias(XStream xstreamObject) {
        try {
            addAlias(xstreamObject, Model.class);
            addAlias(xstreamObject, Settings.class);
        } catch (MavenException e) {
            if (getLog().isDebugEnabled()) {
                getLog().debug(e.getMessage(), e);
            }
        }

        // TODO need to handle specific Maven objects like DefaultArtifact?
    }

    private void addAlias(XStream xstreamObject, Class<?> clazz) {
        String url = Optional.ofNullable(
                        clazz.getClassLoader().getResource(clazz.getName().replace('.', '/') + ".class"))
                .map(URL::toExternalForm)
                .orElse(null);
        if (url != null && url.startsWith("jar:file:")) {
            int exId = url.indexOf('!');
            String path = url.substring("jar:file:".length(), exId);
            try (ZipFile zipFile = new ZipFile(path)) {
                String prefix = clazz.getPackageName().replace('.', '/') + "/";
                List<String> classes = zipFile.stream()
                        .map(e -> e.getName())
                        .filter(e -> e.startsWith(prefix)
                                && e.endsWith(".class")
                                && !e.contains("$")
                                && !e.contains("package-info"))
                        .map(e -> e.substring(0, e.length() - ".class".length()).replace('/', '.'))
                        .toList();
                for (String c : classes) {
                    Class<?> cl = ClassUtils.getClass(c);
                    String alias = lowerCase(cl.getSimpleName());
                    xstreamObject.alias(alias, cl);
                    if ("TrackableBase".equals(cl.getSimpleName())) {
                        xstreamObject.omitField(cl, "locations");
                    }
                }
            } catch (Exception e) {
                throw new MavenException(e);
            }
        }
    }

    /**
     * @param name not null
     * @return the plural of the name
     */
    private static String pluralize(String name) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name is required");
        }

        if (name.endsWith("y")) {
            return name.substring(0, name.length() - 1) + "ies";
        } else if (name.endsWith("s")) {
            return name;
        } else {
            return name + "s";
        }
    }

    private static String lowerCase(String name) {
        return name.substring(0, 1).toLowerCase() + name.substring(1);
    }
}

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

import java.io.IOException;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.apache.maven.api.plugin.MojoException;
import org.apache.maven.api.plugin.annotations.Mojo;
import org.apache.maven.api.plugin.annotations.Parameter;
import org.apache.maven.api.services.xml.SettingsXmlFactory;
import org.apache.maven.api.settings.Profile;
import org.apache.maven.api.settings.Proxy;
import org.apache.maven.api.settings.Server;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.settings.v4.SettingsTransformer;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;

/**
 * Displays the calculated settings as XML for this project, given any profile enhancement and the inheritance
 * of the global settings into the user-level settings.
 *
 * @since 2.0
 */
@Mojo(name = "effective-settings", projectRequired = false)
public class EffectiveSettingsMojo extends AbstractEffectiveMojo {
    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * The system settings for Maven. This is the instance resulting from
     * merging global and user-level settings files.
     */
    @Parameter(property = "session.settings")
    private Settings settings;

    /**
     * For security reasons, all passwords are hidden by default. Set this to <code>true</code> to show all passwords.
     *
     * @since 2.1
     */
    @Parameter(property = "showPasswords", defaultValue = "false")
    private boolean showPasswords;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void execute() throws MojoException {
        Settings copySettings = copySettings(settings, showPasswords);

        StringWriter w = new StringWriter();
        String encoding = output != null && copySettings != null
                ? copySettings.getModelEncoding()
                : Charset.defaultCharset().displayName();
        XMLWriter writer = new PrettyPrintXMLWriter(w, "  ", encoding, null);

        writeHeader(writer);

        writeEffectiveSettings(copySettings, writer);

        String effectiveSettings = prettyFormat(w.toString(), encoding, false);

        if (output != null) {
            try {
                writeFile(output, effectiveSettings);
            } catch (IOException e) {
                throw new MojoException("Cannot write effective-settings to output: " + output, e);
            }

            getLog().info("Effective-settings written to: " + output);
        } else {
            if (getLog().isInfoEnabled()) {
                getLog().info(LS + "Effective user-specific configuration settings:" + LS + LS + effectiveSettings
                        + LS);
            } else if (forceStdout) {
                System.out.println(effectiveSettings);
            }
        }
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * @param settings could be {@code null}
     * @return a new instance of settings or {@code null} if settings was {@code null}.
     */
    private static Settings copySettings(Settings settings, boolean showPasswords) {
        if (settings == null) {
            return null;
        }

        SettingsTransformer transformer = new SettingsTransformer(s -> s) {
            @Override
            protected Server.Builder transformServer_Password(
                    Supplier<? extends Server.Builder> creator, Server.Builder builder, Server target) {
                String oldVal = target.getPassword();
                String newVal = !showPasswords && oldVal != null && !oldVal.isEmpty() ? "***" : oldVal;
                return newVal != oldVal ? (builder != null ? builder : creator.get()).password(newVal) : builder;
            }

            @Override
            protected Server.Builder transformServer_Passphrase(
                    Supplier<? extends Server.Builder> creator, Server.Builder builder, Server target) {
                String oldVal = target.getPassphrase();
                String newVal = !showPasswords && oldVal != null && !oldVal.isEmpty() ? "***" : oldVal;
                return newVal != oldVal ? (builder != null ? builder : creator.get()).passphrase(newVal) : builder;
            }

            @Override
            protected Proxy.Builder transformProxy_Password(
                    Supplier<? extends Proxy.Builder> creator, Proxy.Builder builder, Proxy target) {
                String oldVal = target.getPassword();
                String newVal = !showPasswords && oldVal != null && !oldVal.isEmpty() ? "***" : oldVal;
                return newVal != oldVal ? (builder != null ? builder : creator.get()).password(newVal) : builder;
            }

            @Override
            protected Profile.Builder transformProfile_Properties(
                    Supplier<? extends Profile.Builder> creator, Profile.Builder builder, Profile target) {
                Map<String, String> props = target.getProperties();
                Map<String, String> newProps = new TreeMap<>(props);
                for (Map.Entry<String, String> entry : props.entrySet()) {
                    String newVal = transform(entry.getValue());
                    if (newVal != null) {
                        newProps.put(entry.getKey(), newVal);
                    }
                }
                builder = builder != null ? builder : creator.get();
                builder.properties(newProps);
                return builder;
            }
        };

        return transformer.visit(settings);
    }

    /**
     * Method for writing the effective settings informations.
     *
     * @param settings the settings, not null.
     * @param writer the XML writer used, not null.
     * @throws MojoException if any
     */
    private void writeEffectiveSettings(Settings settings, XMLWriter writer) throws MojoException {
        StringWriter sWriter = new StringWriter();
        try {
            session.getService(SettingsXmlFactory.class).write(settings, sWriter);
        } catch (Exception e) {
            throw new MojoException("Cannot serialize Settings to XML.", e);
        }

        // This removes the XML declaration written by MavenXpp3Writer
        String effectiveSettings = prettyFormat(sWriter.toString(), null, true);

        writeComment(writer, "Effective Settings for '" + getUserName() + "' on '" + getHostName() + "'");

        writer.writeMarkup(effectiveSettings);
    }

    /**
     * @return the current host name or <code>unknown</code> if error
     * @see InetAddress#getLocalHost()
     */
    private static String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }

    /**
     * @return the user name or <code>unknown</code> if <code>user.name</code> is not a system property.
     */
    private static String getUserName() {
        String userName = System.getProperty("user.name");
        if (userName == null || userName.isEmpty()) {
            return "unknown";
        }

        return userName;
    }
}

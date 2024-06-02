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
import java.util.List;
import java.util.Properties;

import org.apache.maven.api.settings.InputLocation;
import org.apache.maven.api.settings.InputSource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Proxy;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.io.xpp3.SettingsXpp3Writer;
import org.apache.maven.settings.v4.SettingsXpp3WriterEx;
import org.codehaus.plexus.util.StringUtils;
import org.codehaus.plexus.util.xml.PrettyPrintXMLWriter;
import org.codehaus.plexus.util.xml.XMLWriter;
import org.codehaus.plexus.util.xml.XmlWriterUtil;

/**
 * Displays the calculated settings as XML for this project, given any profile enhancement and the inheritance
 * of the global settings into the user-level settings.
 *
 * @since 2.0
 */
@Mojo(name = "effective-settings", requiresProject = false)
public class EffectiveSettingsMojo extends AbstractEffectiveMojo {
    // ----------------------------------------------------------------------
    // Mojo parameters
    // ----------------------------------------------------------------------

    /**
     * The system settings for Maven. This is the instance resulting from
     * merging global and user-level settings files.
     */
    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    private Settings settings;

    /**
     * For security reasons, all passwords are hidden by default. Set this to <code>true</code> to show all passwords.
     *
     * @since 2.1
     */
    @Parameter(property = "showPasswords", defaultValue = "false")
    private boolean showPasswords;

    /**
     * Output POM input location as comments.
     *
     * @since 3.5.0
     */
    @Parameter(property = "verbose", defaultValue = "false")
    private boolean verbose = false;

    // ----------------------------------------------------------------------
    // Public methods
    // ----------------------------------------------------------------------

    /** {@inheritDoc} */
    public void execute() throws MojoExecutionException {
        Settings copySettings;
        if (showPasswords) {
            copySettings = settings;
        } else {
            copySettings = copySettings(settings);
            if (copySettings != null) {
                hidePasswords(copySettings);
            }
        }

        StringWriter w = new StringWriter();
        String encoding = output != null && copySettings != null
                ? copySettings.getModelEncoding()
                : System.getProperty("file.encoding");
        XMLWriter writer = new PrettyPrintXMLWriter(
                w, StringUtils.repeat(" ", XmlWriterUtil.DEFAULT_INDENTATION_SIZE), encoding, null);

        writeHeader(writer);

        writeEffectiveSettings(copySettings, writer);

        String effectiveSettings = prettyFormat(w.toString(), encoding, false);
        if (verbose) {
            // tweak location tracking comment, that are put on a separate line by pretty print
            effectiveSettings = effectiveSettings.replaceAll("(?m)>\\s+<!--}", ">  <!-- ");
        }

        if (output != null) {
            try {
                writeXmlFile(output, effectiveSettings);
            } catch (IOException e) {
                throw new MojoExecutionException("Cannot write effective-settings to output: " + output, e);
            }

            getLog().info("Effective-settings written to: " + output);
        } else {
            getLog().info(LS + "Effective user-specific configuration settings:" + LS + LS + effectiveSettings + LS);
        }
    }

    // ----------------------------------------------------------------------
    // Private methods
    // ----------------------------------------------------------------------

    /**
     * Hide proxy and server passwords.
     *
     * @param aSettings not null
     */
    private static void hidePasswords(Settings aSettings) {
        List<Proxy> proxies = aSettings.getProxies();
        for (Proxy proxy : proxies) {
            if (StringUtils.isNotEmpty(proxy.getPassword())) {
                proxy.setPassword("***");
            }
        }

        List<Server> servers = aSettings.getServers();
        for (Server server : servers) {
            // Password
            if (StringUtils.isNotEmpty(server.getPassword())) {
                server.setPassword("***");
            }
            // Passphrase
            if (StringUtils.isNotEmpty(server.getPassphrase())) {
                server.setPassphrase("***");
            }
        }
    }

    /**
     * @param settings could be {@code null}
     * @return a new instance of settings or {@code null} if settings was {@code null}.
     */
    private static Settings copySettings(Settings settings) {
        if (settings == null) {
            return null;
        }
        return new Settings(settings.getDelegate());
    }

    /**
     * Method for writing the effective settings informations.
     *
     * @param settings the settings, not null.
     * @param writer the XML writer used, not null.
     * @throws MojoExecutionException if any
     */
    private void writeEffectiveSettings(Settings settings, XMLWriter writer) throws MojoExecutionException {
        cleanSettings(settings);

        StringWriter sWriter = new StringWriter();
        try {
            if (verbose) {
                SettingsXpp3WriterEx settingsWriter = new SettingsXpp3WriterEx();
                settingsWriter.setStringFormatter(new InputLocationStringFormatter());
                settingsWriter.write(sWriter, settings.getDelegate());
            } else {
                new SettingsXpp3Writer().write(sWriter, settings);
            }
        } catch (IOException e) {
            throw new MojoExecutionException("Cannot serialize Settings to XML.", e);
        }

        // This removes the XML declaration written by MavenXpp3Writer
        String effectiveSettings = prettyFormat(sWriter.toString(), null, true);

        writeComment(writer, "Effective Settings for '" + getUserName() + "' on '" + getHostName() + "'");

        writer.writeMarkup(effectiveSettings);
    }

    /**
     * Apply some logic to clean the model before writing it.
     *
     * @param settings not null
     */
    private static void cleanSettings(Settings settings) {
        List<Profile> profiles = settings.getProfiles();
        for (Profile profile : profiles) {
            Properties properties = new SortedProperties();
            properties.putAll(profile.getProperties());
            profile.setProperties(properties);
        }
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

    private static class InputLocationStringFormatter implements InputLocation.StringFormatter {
        @Override
        public String toString(InputLocation location) {
            InputSource source = location.getSource();

            String s = source.toString();

            return '}' + s + ((location.getLineNumber() >= 0) ? ", line " + location.getLineNumber() : "") + ' ';
        }
    }
}

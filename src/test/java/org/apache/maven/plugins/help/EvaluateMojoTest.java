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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.plugin.Log;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.api.plugin.testing.stubs.SessionMock;
import org.apache.maven.api.services.Prompter;
import org.apache.maven.api.settings.Server;
import org.apache.maven.api.settings.Settings;
import org.apache.maven.impl.InternalSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test class for the evaluate mojo of the Help Plugin.
 */
@MojoTest
@MockitoSettings(strictness = Strictness.WARN)
class EvaluateMojoTest {

    static final String CONFIG_XML = "classpath:/unit/evaluate/plugin-config.xml";

    @Mock
    Prompter prompter;

    @Mock
    Log log;

    final ByteArrayOutputStream stdout = new ByteArrayOutputStream();

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(stdout));
    }

    /**
     * Tests evaluation of an expression in interactive mode with a mock input handler.
     * @throws Exception in case of errors.
     */
    @Test
    @InjectMojo(goal = "evaluate", pom = CONFIG_XML)
    @Basedir
    public void testEvaluateWithoutExpression(EvaluateMojo mojo) throws Exception {
        when(prompter.prompt(anyString())).thenReturn("${project.groupId}", "0");
        when(log.isInfoEnabled()).thenReturn(true);

        mojo.execute();

        verify(log, atLeastOnce()).isInfoEnabled();
        verify(log)
                .info(
                        "No artifact parameter specified, using 'org.apache.maven.its.help:evaluate:jar:1.0-SNAPSHOT' as project.");
        verify(log, times(2)).info("Enter the Maven expression i.e. ${project.groupId} or 0 to exit?:");
        verify(log).info(System.lineSeparator() + "org.apache.maven.its.help");
        verify(log, never()).warn(any(CharSequence.class));
        verify(prompter, times(2)).prompt("Enter the Maven expression i.e. ${project.groupId} or 0 to exit?");
        verifyNoMoreInteractions(log, prompter);
    }

    /**
     * Tests evaluation of an expression in interactive mode with a mock input handler, when "output" is set.
     * @throws Exception in case of errors.
     */
    @Test
    @InjectMojo(goal = "evaluate", pom = CONFIG_XML)
    @MojoParameter(name = "output", value = "result.txt")
    @Basedir
    public void testEvaluateWithoutExpressionWithOutput(EvaluateMojo mojo) throws Exception {
        when(prompter.prompt(any())).thenReturn("${project.groupId}", "0");
        when(log.isInfoEnabled()).thenReturn(true);

        mojo.execute();

        verify(log, atLeastOnce()).isInfoEnabled();
        verify(log)
                .info(
                        "No artifact parameter specified, using 'org.apache.maven.its.help:evaluate:jar:1.0-SNAPSHOT' as project.");
        verify(log).warn("When prompting for input, the result will be written to the console, ignoring 'output'.");
        verify(log, times(2)).info("Enter the Maven expression i.e. ${project.groupId} or 0 to exit?:");
        verify(log).info(System.lineSeparator() + "org.apache.maven.its.help");
        verify(prompter, times(2)).prompt("Enter the Maven expression i.e. ${project.groupId} or 0 to exit?");
        verifyNoMoreInteractions(log, prompter);
    }

    /**
     * This test will check that only the <code>project.groupId</code> is printed to
     * stdout nothing else.
     *
     * @throws Exception in case of errors.
     * @see <a href="https://issues.apache.org/jira/browse/MPH-144">MPH-144</a>
     */
    @Test
    @InjectMojo(goal = "evaluate", pom = CONFIG_XML)
    @MojoParameter(name = "forceStdout", value = "true")
    @MojoParameter(name = "expression", value = "project.groupId")
    @Basedir
    public void testEvaluateQuietModeWithOutputOnStdout(EvaluateMojo mojo) throws Exception {
        // Quiet mode given on command line.(simulation)
        when(log.isInfoEnabled()).thenReturn(false);

        mojo.execute();

        verify(log, atLeastOnce()).isInfoEnabled();
        verify(log)
                .info(
                        "No artifact parameter specified, using 'org.apache.maven.its.help:evaluate:jar:1.0-SNAPSHOT' as project.");
        assertEquals("org.apache.maven.its.help", stdout.toString());
        verifyNoMoreInteractions(log, prompter);
    }

    /**
     * This test will check that only the <code>project.groupId</code> is printed to
     * stdout nothing else.
     *
     * @throws Exception in case of errors.
     * @see <a href="https://issues.apache.org/jira/browse/MPH-144">MPH-144</a>
     */
    @Test
    @InjectMojo(goal = "evaluate", pom = CONFIG_XML)
    @MojoParameter(name = "forceStdout", value = "true")
    @MojoParameter(name = "expression", value = "settings.servers[0]")
    @Basedir
    public void testEvaluateSettings(EvaluateMojo mojo) throws Exception {
        // Quiet mode given on command line.(simulation)
        when(log.isInfoEnabled()).thenReturn(false);

        mojo.execute();

        verify(log, atLeastOnce()).isInfoEnabled();
        verify(log)
                .info(
                        "No artifact parameter specified, using 'org.apache.maven.its.help:evaluate:jar:1.0-SNAPSHOT' as project.");
        assertEquals(
                "<server>\n" + "  <id>central</id>\n" + "  <username>foo</username>\n" + "</server>",
                stdout.toString());
        verifyNoMoreInteractions(log, prompter);
    }

    @Provides
    @Singleton
    Prompter prompter() {
        return prompter;
    }

    @Provides
    @Singleton
    Log createlog() {
        return log;
    }

    @Provides
    InternalSession createSession() {
        InternalSession session = SessionMock.getMockSession("target/local-repo");

        when(session.getSettings())
                .thenReturn(Settings.newBuilder()
                        .servers(List.of(Server.newBuilder()
                                .id("central")
                                .username("foo")
                                .build()))
                        .build());
        when(session.getService(Prompter.class)).thenReturn(prompter);

        return session;
    }
}

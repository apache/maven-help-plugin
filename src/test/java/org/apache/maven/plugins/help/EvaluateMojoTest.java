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

import org.apache.maven.api.di.Provides;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.plugin.PluginParameterExpressionEvaluator;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.components.interactivity.InputHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.apache.maven.api.plugin.testing.MojoExtension.setVariableValueToObject;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for the evaluate mojo of the Help Plugin.
 */
@ExtendWith(MockitoExtension.class)
@MojoTest
class EvaluateMojoTest {

    @Mock
    private Log log;

    @Mock
    private InputHandler inputHandler;

    @Mock
    private PluginParameterExpressionEvaluator expressionEvaluator;

    @Provides
    private Log provideLogger() {
        return log;
    }

    @Provides
    private InputHandler provideInputHandler() {
        return inputHandler;
    }

    /**
     * Tests evaluation of an expression in interactive mode with a mock input handler.
     * @throws Exception in case of errors.
     */
    @Test
    @InjectMojo(goal = "evaluate")
    void testEvaluateWithoutExpression(EvaluateMojo mojo) throws Exception {
        when(inputHandler.readLine()).thenReturn("${project.groupId}", "0");
        when(expressionEvaluator.evaluate(anyString())).thenReturn("My result");
        when(log.isInfoEnabled()).thenReturn(true);

        setVariableValueToObject(mojo, "evaluator", expressionEvaluator);

        mojo.execute();

        String ls = System.lineSeparator();

        verify(log).info(ls + "My result");
        verify(log, never()).warn(anyString());
        verify(expressionEvaluator).evaluate("${project.groupId}");
        verify(inputHandler, times(2)).readLine();
    }

    /**
     * Tests evaluation of an expression in interactive mode with a mock input handler, when "output" is set.
     * @throws Exception in case of errors.
     */
    @Test
    @InjectMojo(goal = "evaluate")
    @MojoParameter(name = "output", value = "result.txt")
    void testEvaluateWithoutExpressionWithOutput(EvaluateMojo mojo) throws Exception {
        when(inputHandler.readLine()).thenReturn("${project.artifactId}", "0");
        when(expressionEvaluator.evaluate(anyString())).thenReturn("My result");
        when(log.isInfoEnabled()).thenReturn(true);

        setVariableValueToObject(mojo, "evaluator", expressionEvaluator);

        mojo.execute();

        String ls = System.lineSeparator();
        verify(log).info(ls + "My result");
        verify(log).warn(anyString());
        verify(expressionEvaluator).evaluate("${project.artifactId}");
        verify(inputHandler, times(2)).readLine();
    }

    /**
     * Tests evaluation of a complex expression.
     *
     * @throws Exception in case of errors.
     */
    @Test
    @ResourceLock(Resources.SYSTEM_OUT)
    @InjectMojo(goal = "evaluate")
    @MojoParameter(name = "forceStdout", value = "true")
    @MojoParameter(name = "expression", value = "project_groupId=${project.groupId}")
    void testEvaluateForComplexExpression(EvaluateMojo mojo) throws Exception {
        when(expressionEvaluator.evaluate(anyString())).thenReturn("project_groupId=org.apache.maven.its.help");

        // Quiet mode given on command line.(simulation)
        when(log.isInfoEnabled()).thenReturn(false);

        setVariableValueToObject(mojo, "evaluator", expressionEvaluator);

        PrintStream saveOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));

        try {
            mojo.execute();
        } finally {
            System.setOut(saveOut);
            baos.close();
        }

        String stdResult = baos.toString();
        assertEquals("project_groupId=org.apache.maven.its.help", stdResult);
        verify(log, never()).warn(anyString());
    }

    /**
     * This test will check that only the <code>project.groupId</code> is printed to
     * stdout nothing else.
     *
     * @throws Exception in case of errors.
     * @see <a href="https://issues.apache.org/jira/browse/MPH-144">MPH-144</a>
     */
    @Test
    @ResourceLock(Resources.SYSTEM_OUT)
    @InjectMojo(goal = "evaluate")
    @MojoParameter(name = "forceStdout", value = "true")
    @MojoParameter(name = "expression", value = "project.groupId")
    void testEvaluateQuiteModeWithOutputOnStdout(EvaluateMojo mojo) throws Exception {
        when(expressionEvaluator.evaluate(anyString())).thenReturn("org.apache.maven.its.help");

        // Quiet mode given on command line.(simulation)
        when(log.isInfoEnabled()).thenReturn(false);

        setVariableValueToObject(mojo, "evaluator", expressionEvaluator);

        PrintStream saveOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));

        try {
            mojo.execute();
        } finally {
            System.setOut(saveOut);
            baos.close();
        }

        String stdResult = baos.toString();
        assertEquals("org.apache.maven.its.help", stdResult);
        verify(log, never()).warn(anyString());
    }
}

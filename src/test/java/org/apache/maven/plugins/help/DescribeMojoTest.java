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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.lifecycle.DefaultLifecycles;
import org.apache.maven.lifecycle.Lifecycle;
import org.apache.maven.lifecycle.internal.MojoDescriptorCreator;
import org.apache.maven.lifecycle.mapping.LifecycleMapping;
import org.apache.maven.lifecycle.mapping.LifecyclePhase;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MavenPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.version.PluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionResolver;
import org.apache.maven.plugin.version.PluginVersionResult;
import org.apache.maven.plugins.help.DescribeMojo.PluginInfo;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
class DescribeMojoTest {

    @Test
    void testGetExpressionsRoot()
            throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException,
                    InvocationTargetException {
        DescribeMojo describeMojo = new DescribeMojo(null, null, null, null, null, null, null);
        Method toLines =
                describeMojo.getClass().getDeclaredMethod("toLines", String.class, int.class, int.class, int.class);
        toLines.setAccessible(true);
        toLines.invoke(null, "", 2, 2, 80);
    }

    @Test
    void testValidExpression() throws Exception {
        StringBuilder sb = new StringBuilder();
        MojoDescriptor md = new MojoDescriptor();
        Parameter parameter = new Parameter();
        parameter.setName("name");
        parameter.setExpression("${valid.expression}");
        md.addParameter(parameter);

        String ls = System.getProperty("line.separator");

        Method describeMojoParameters = DescribeMojo.class.getDeclaredMethod(
                "describeMojoParameters", MojoDescriptor.class, StringBuilder.class);
        describeMojoParameters.setAccessible(true);
        describeMojoParameters.invoke(new DescribeMojo(null, null, null, null, null, null, null), md, sb);

        assertEquals(
                "  Available parameters:" + ls + ls + "    name" + ls + "      User property: valid.expression" + ls
                        + "      (no description available)" + ls,
                sb.toString());
    }

    @Test
    void testInvalidExpression() throws Exception {
        StringBuilder sb = new StringBuilder();
        MojoDescriptor md = new MojoDescriptor();
        Parameter parameter = new Parameter();
        parameter.setName("name");
        parameter.setExpression("${project.build.directory}/generated-sources/foobar"); // this is a defaultValue
        md.addParameter(parameter);

        String ls = System.lineSeparator();

        Method describeMojoParameters = DescribeMojo.class.getDeclaredMethod(
                "describeMojoParameters", MojoDescriptor.class, StringBuilder.class);
        describeMojoParameters.setAccessible(true);
        describeMojoParameters.invoke(new DescribeMojo(null, null, null, null, null, null, null), md, sb);

        assertEquals(
                "  Available parameters:" + ls + ls
                        + "    name"
                        + ls + "      Expression: ${project.build.directory}/generated-sources/foobar"
                        + ls + "      (no description available)"
                        + ls,
                sb.toString());
    }

    @Test
    void testParsePluginInfoGAV() throws Throwable {
        DescribeMojo mojo = new DescribeMojo(null, null, null, null, null, null, null);
        setFieldWithReflection(mojo, "groupId", "org.test");
        setFieldWithReflection(mojo, "artifactId", "test");
        setFieldWithReflection(mojo, "version", "1.0");

        Method parsePluginLookupInfo = setParsePluginLookupInfoAccessibility();
        PluginInfo pi = (PluginInfo) parsePluginLookupInfo.invoke(mojo);

        assertEquals("org.test", pi.getGroupId());
        assertEquals("test", pi.getArtifactId());
        assertEquals("1.0", pi.getVersion());
        assertNull(pi.getPrefix());
    }

    @Test
    void testParsePluginInfoPluginPrefix() throws Throwable {
        DescribeMojo mojo = new DescribeMojo(null, null, null, null, null, null, null);
        setFieldWithReflection(mojo, "plugin", "help");

        Method parsePluginLookupInfo = setParsePluginLookupInfoAccessibility();
        PluginInfo pi = (PluginInfo) parsePluginLookupInfo.invoke(mojo);

        assertNull(pi.getGroupId());
        assertNull(pi.getArtifactId());
        assertNull(pi.getVersion());
        assertEquals("help", pi.getPrefix());

        setFieldWithReflection(mojo, "plugin", "help2:::");

        pi = (PluginInfo) parsePluginLookupInfo.invoke(mojo);

        assertEquals("help2", pi.getPrefix());
    }

    @Test
    void testParsePluginInfoPluginGA() throws Throwable {
        DescribeMojo mojo = new DescribeMojo(null, null, null, null, null, null, null);
        setFieldWithReflection(mojo, "plugin", "org.test:test");

        Method parsePluginLookupInfo = setParsePluginLookupInfoAccessibility();
        PluginInfo pi = (PluginInfo) parsePluginLookupInfo.invoke(mojo);

        assertEquals("org.test", pi.getGroupId());
        assertEquals("test", pi.getArtifactId());
        assertNull(pi.getVersion());
        assertNull(pi.getPrefix());
    }

    @Test
    void testParsePluginInfoPluginGAV() throws Throwable {
        DescribeMojo mojo = new DescribeMojo(null, null, null, null, null, null, null);
        setFieldWithReflection(mojo, "plugin", "org.test:test:1.0");

        Method parsePluginLookupInfo = setParsePluginLookupInfoAccessibility();
        PluginInfo pi = (PluginInfo) parsePluginLookupInfo.invoke(mojo);

        assertEquals("org.test", pi.getGroupId());
        assertEquals("test", pi.getArtifactId());
        assertEquals("1.0", pi.getVersion());
        assertNull(pi.getPrefix());
    }

    @Test
    void testParsePluginInfoPluginIncorrect() throws Throwable {
        DescribeMojo mojo = new DescribeMojo(null, null, null, null, null, null, null);
        setFieldWithReflection(mojo, "plugin", "org.test:test:1.0:invalid");
        try {
            Method parsePluginLookupInfo = setParsePluginLookupInfoAccessibility();
            parsePluginLookupInfo.invoke(mojo);
            fail();
        } catch (Exception e) {
            // expected
        }
    }

    @Test
    void testLookupPluginDescriptorPrefixWithVersion() throws Throwable {
        DescribeMojo mojo = new DescribeMojo(null, null, null, null, null, null, null);

        PluginInfo pi = new PluginInfo();
        pi.setPrefix("help");
        pi.setVersion("1.0");

        Plugin plugin = new Plugin();
        plugin.setGroupId("org.test");
        plugin.setArtifactId("test");

        PluginDescriptor pd = new PluginDescriptor();

        MojoDescriptorCreator mojoDescriptorCreator = mock(MojoDescriptorCreator.class);
        PluginVersionResolver pluginVersionResolver = mock(PluginVersionResolver.class);
        MavenPluginManager pluginManager = mock(MavenPluginManager.class);
        MavenSession session = mock(MavenSession.class);
        when(session.getRepositorySession()).thenReturn(mock(RepositorySystemSession.class));
        setFieldsOnMojo(mojo, mojoDescriptorCreator, pluginVersionResolver, pluginManager, session);
        MavenProject mavenProject = new MavenProject();
        mavenProject.setPluginArtifactRepositories(Collections.emptyList());
        setParentFieldWithReflection(mojo, "project", mavenProject);
        when(mojoDescriptorCreator.findPluginForPrefix("help", session)).thenReturn(plugin);
        when(pluginManager.getPluginDescriptor(any(Plugin.class), anyList(), any()))
                .thenReturn(pd);

        Method lookupPluginDescriptor = setLookupPluginDescriptorAccessibility();
        PluginDescriptor returned = (PluginDescriptor) lookupPluginDescriptor.invoke(mojo, pi);

        assertEquals(pd, returned);

        verify(mojoDescriptorCreator).findPluginForPrefix("help", session);
        verify(pluginVersionResolver, never()).resolve(any(PluginVersionRequest.class));
        ArgumentCaptor<Plugin> argument = ArgumentCaptor.forClass(Plugin.class);
        verify(pluginManager).getPluginDescriptor(argument.capture(), anyList(), any());
        Plugin capturedPlugin = argument.getValue();
        assertEquals("org.test", capturedPlugin.getGroupId());
        assertEquals("test", capturedPlugin.getArtifactId());
        assertEquals("1.0", capturedPlugin.getVersion());
    }

    @Test
    void testLookupPluginDescriptorPrefixWithoutVersion() throws Throwable {
        DescribeMojo mojo = new DescribeMojo(null, null, null, null, null, null, null);

        PluginInfo pi = new PluginInfo();
        pi.setPrefix("help");

        Plugin plugin = new Plugin();
        plugin.setGroupId("org.test");
        plugin.setArtifactId("test");

        PluginDescriptor pd = new PluginDescriptor();

        MojoDescriptorCreator mojoDescriptorCreator = mock(MojoDescriptorCreator.class);
        PluginVersionResolver pluginVersionResolver = mock(PluginVersionResolver.class);
        MavenPluginManager pluginManager = mock(MavenPluginManager.class);
        PluginVersionResult versionResult = mock(PluginVersionResult.class);
        MavenSession session = mock(MavenSession.class);
        when(session.getRepositorySession()).thenReturn(mock(RepositorySystemSession.class));
        setFieldsOnMojo(mojo, mojoDescriptorCreator, pluginVersionResolver, pluginManager, session);
        MavenProject mavenProject = new MavenProject();
        mavenProject.setPluginArtifactRepositories(Collections.emptyList());
        setParentFieldWithReflection(mojo, "project", mavenProject);
        when(mojoDescriptorCreator.findPluginForPrefix("help", session)).thenReturn(plugin);
        when(pluginVersionResolver.resolve(any(PluginVersionRequest.class))).thenReturn(versionResult);
        when(versionResult.getVersion()).thenReturn("1.0");
        when(pluginManager.getPluginDescriptor(any(Plugin.class), anyList(), any()))
                .thenReturn(pd);

        Method lookupPluginDescriptor = setLookupPluginDescriptorAccessibility();
        PluginDescriptor returned = (PluginDescriptor) lookupPluginDescriptor.invoke(mojo, pi);
        assertEquals(pd, returned);

        verify(mojoDescriptorCreator).findPluginForPrefix("help", session);
        ArgumentCaptor<PluginVersionRequest> versionArgument = ArgumentCaptor.forClass(PluginVersionRequest.class);
        verify(pluginVersionResolver).resolve(versionArgument.capture());
        assertEquals("org.test", versionArgument.getValue().getGroupId());
        assertEquals("test", versionArgument.getValue().getArtifactId());
        ArgumentCaptor<Plugin> argument = ArgumentCaptor.forClass(Plugin.class);
        verify(pluginManager).getPluginDescriptor(argument.capture(), anyList(), any());
        Plugin capturedPlugin = argument.getValue();
        assertEquals("org.test", capturedPlugin.getGroupId());
        assertEquals("test", capturedPlugin.getArtifactId());
        assertEquals("1.0", capturedPlugin.getVersion());
    }

    @Test
    void testLookupPluginDescriptorGAV() throws Throwable {
        DescribeMojo mojo = new DescribeMojo(null, null, null, null, null, null, null);

        PluginInfo pi = new PluginInfo();
        pi.setGroupId("org.test");
        pi.setArtifactId("test");
        pi.setVersion("1.0");

        PluginDescriptor pd = new PluginDescriptor();

        MojoDescriptorCreator mojoDescriptorCreator = mock(MojoDescriptorCreator.class);
        PluginVersionResolver pluginVersionResolver = mock(PluginVersionResolver.class);
        MavenPluginManager pluginManager = mock(MavenPluginManager.class);
        MavenSession session = mock(MavenSession.class);
        when(session.getRepositorySession()).thenReturn(mock(RepositorySystemSession.class));
        setFieldsOnMojo(mojo, mojoDescriptorCreator, pluginVersionResolver, pluginManager, session);
        MavenProject mavenProject = new MavenProject();
        mavenProject.setPluginArtifactRepositories(Collections.emptyList());
        setParentFieldWithReflection(mojo, "project", mavenProject);
        when(pluginManager.getPluginDescriptor(any(Plugin.class), anyList(), any()))
                .thenReturn(pd);

        Method lookupPluginDescriptor = setLookupPluginDescriptorAccessibility();
        PluginDescriptor returned = (PluginDescriptor) lookupPluginDescriptor.invoke(mojo, pi);

        assertEquals(pd, returned);

        verify(mojoDescriptorCreator, never()).findPluginForPrefix(any(String.class), any(MavenSession.class));
        verify(pluginVersionResolver, never()).resolve(any(PluginVersionRequest.class));
        ArgumentCaptor<Plugin> argument = ArgumentCaptor.forClass(Plugin.class);
        verify(pluginManager).getPluginDescriptor(argument.capture(), anyList(), any());
        Plugin capturedPlugin = argument.getValue();
        assertEquals("org.test", capturedPlugin.getGroupId());
        assertEquals("test", capturedPlugin.getArtifactId());
        assertEquals("1.0", capturedPlugin.getVersion());
    }

    @Test
    void testLookupPluginDescriptorGMissingA() {
        DescribeMojo mojo = new DescribeMojo(null, null, null, null, null, null, null);
        PluginInfo pi = new PluginInfo();
        pi.setGroupId("org.test");
        try {
            Method lookupPluginDescriptor = setLookupPluginDescriptorAccessibility();
            lookupPluginDescriptor.invoke(mojo, pi);
            fail();
        } catch (InvocationTargetException e) {
            assertTrue(e.getTargetException().getMessage().startsWith("You must specify either"));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            fail();
        }
    }

    @Test
    void testLookupPluginDescriptorAMissingG() {
        DescribeMojo mojo = new DescribeMojo(null, null, null, null, null, null, null);
        PluginInfo pi = new PluginInfo();
        pi.setArtifactId("test");
        try {
            Method lookupPluginDescriptor = setLookupPluginDescriptorAccessibility();
            lookupPluginDescriptor.invoke(mojo, pi);
            fail();
        } catch (InvocationTargetException e) {
            assertTrue(e.getTargetException().getMessage().startsWith("You must specify either"));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    /**
     * Regression test for Maven 3.10.0 behavior where {@code Lifecycle.getDefaultLifecyclePhases()}
     * returns an empty map (not null) for the "default" lifecycle.
     * The mojo must fall through to the packaging-specific lifecycle mapping in that case.
     */
    @Test
    void testDescribeCommandPackagingSpecificPhaseShowsBindings() throws Exception {
        // default lifecycle: built-in phases are empty (3.10.0 behavior)
        Lifecycle lifecycle = mock(Lifecycle.class);
        when(lifecycle.getId()).thenReturn("default");
        when(lifecycle.getPhases()).thenReturn(Arrays.asList("validate", "compile", "test", "package"));
        when(lifecycle.getDefaultLifecyclePhases()).thenReturn(Collections.emptyMap());

        Map<String, Lifecycle> phaseMap = new HashMap<>();
        for (String p : Arrays.asList("validate", "compile", "test", "package")) {
            phaseMap.put(p, lifecycle);
        }
        DefaultLifecycles defaultLifecycles = mock(DefaultLifecycles.class);
        when(defaultLifecycles.getPhaseToLifecycleMap()).thenReturn(phaseMap);

        // packaging-specific bindings: only "compile" phase is bound
        Map<String, LifecyclePhase> lifecyclePhases = new LinkedHashMap<>();
        lifecyclePhases.put(
                "compile", new LifecyclePhase("org.apache.maven.plugins:maven-compiler-plugin:3.11.0:compile"));
        org.apache.maven.lifecycle.mapping.Lifecycle mappingLifecycle =
                new org.apache.maven.lifecycle.mapping.Lifecycle();
        mappingLifecycle.setLifecyclePhases(lifecyclePhases);

        LifecycleMapping lifecycleMapping = mock(LifecycleMapping.class);
        when(lifecycleMapping.getLifecycles()).thenReturn(Collections.singletonMap("default", mappingLifecycle));

        MavenProject project = mock(MavenProject.class);
        when(project.getPackaging()).thenReturn("jar");

        DescribeMojo mojo = new DescribeMojo(
                null, null, null, null, null, defaultLifecycles, Collections.singletonMap("jar", lifecycleMapping));
        setFieldWithReflection(mojo, "cmd", "compile");
        setParentFieldWithReflection(mojo, "project", project);

        StringBuilder sb = new StringBuilder();
        Method describeCommand = DescribeMojo.class.getDeclaredMethod("describeCommand", StringBuilder.class);
        describeCommand.setAccessible(true);
        boolean result = (boolean) describeCommand.invoke(mojo, sb);

        String output = sb.toString();
        assertFalse(result);
        assertTrue(output.contains("maven-compiler-plugin"), "Should show compiler plugin: " + output);
        assertFalse(output.contains("* compile: Not defined"), "compile should not be 'Not defined': " + output);
        assertTrue(output.contains("* validate: Not defined"), "validate has no binding: " + output);
        assertTrue(output.contains("It is a part of the lifecycle for the POM packaging 'jar'"), output);
    }

    /**
     * Built-in lifecycle (e.g. "clean") has non-empty {@code getDefaultLifecyclePhases()} —
     * the mojo should use those directly without consulting lifecycle mappings.
     */
    @Test
    void testDescribeCommandBuiltinLifecyclePhaseShowsBindings() throws Exception {
        Map<String, LifecyclePhase> builtinPhases = new LinkedHashMap<>();
        builtinPhases.put("pre-clean", null);
        builtinPhases.put("clean", new LifecyclePhase("org.apache.maven.plugins:maven-clean-plugin:3.2.0:clean"));
        builtinPhases.put("post-clean", null);

        Lifecycle lifecycle = mock(Lifecycle.class);
        when(lifecycle.getId()).thenReturn("clean");
        when(lifecycle.getPhases()).thenReturn(Arrays.asList("pre-clean", "clean", "post-clean"));
        when(lifecycle.getDefaultLifecyclePhases()).thenReturn(builtinPhases);

        Map<String, Lifecycle> phaseMap = new HashMap<>();
        for (String p : Arrays.asList("pre-clean", "clean", "post-clean")) {
            phaseMap.put(p, lifecycle);
        }
        DefaultLifecycles defaultLifecycles = mock(DefaultLifecycles.class);
        when(defaultLifecycles.getPhaseToLifecycleMap()).thenReturn(phaseMap);

        MavenProject project = mock(MavenProject.class);
        when(project.getPackaging()).thenReturn("jar");

        DescribeMojo mojo = new DescribeMojo(null, null, null, null, null, defaultLifecycles, Collections.emptyMap());
        setFieldWithReflection(mojo, "cmd", "clean");
        setFieldWithReflection(mojo, "lifecycleMappings", Collections.emptyMap());
        setParentFieldWithReflection(mojo, "project", project);

        StringBuilder sb = new StringBuilder();
        Method describeCommand = DescribeMojo.class.getDeclaredMethod("describeCommand", StringBuilder.class);
        describeCommand.setAccessible(true);
        boolean result = (boolean) describeCommand.invoke(mojo, sb);

        String output = sb.toString();
        assertFalse(result);
        assertTrue(output.contains("'clean' is a phase within the 'clean' lifecycle"), output);
        assertTrue(output.contains("maven-clean-plugin"), output);
        assertTrue(output.contains("* pre-clean: Not defined"), output);
        assertTrue(output.contains("* post-clean: Not defined"), output);
    }

    @Test
    void testDescribeCommandUnknownPhaseThrows() throws Exception {
        DefaultLifecycles defaultLifecycles = mock(DefaultLifecycles.class);
        when(defaultLifecycles.getPhaseToLifecycleMap()).thenReturn(Collections.emptyMap());

        DescribeMojo mojo = new DescribeMojo(null, null, null, null, null, defaultLifecycles, Collections.emptyMap());
        setFieldWithReflection(mojo, "cmd", "nonexistent-phase");
        setParentFieldWithReflection(mojo, "project", mock(MavenProject.class));

        Method describeCommand = DescribeMojo.class.getDeclaredMethod("describeCommand", StringBuilder.class);
        describeCommand.setAccessible(true);
        try {
            describeCommand.invoke(mojo, new StringBuilder());
            fail("Expected MojoExecutionException");
        } catch (InvocationTargetException e) {
            assertTrue(
                    e.getTargetException() instanceof MojoExecutionException,
                    "Expected MojoExecutionException, got: " + e.getTargetException());
            assertTrue(e.getTargetException().getMessage().contains("nonexistent-phase"));
        }
    }

    private static void setParentFieldWithReflection(
            final DescribeMojo mojo, final String fieldName, final Object value)
            throws NoSuchFieldException, IllegalAccessException {
        final Field field = mojo.getClass().getSuperclass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(mojo, value);
        field.setAccessible(false);
    }

    private static void setFieldWithReflection(final Object mojo, final String fieldName, final Object value)
            throws NoSuchFieldException, IllegalAccessException {
        final Field field = mojo.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(mojo, value);
        field.setAccessible(false);
    }

    private static void setFieldsOnMojo(
            final DescribeMojo mojo,
            final MojoDescriptorCreator mojoDescriptorCreator,
            final PluginVersionResolver pluginVersionResolver,
            final MavenPluginManager pluginManager,
            final MavenSession session)
            throws NoSuchFieldException, IllegalAccessException {
        setFieldWithReflection(mojo, "mojoDescriptorCreator", mojoDescriptorCreator);
        setFieldWithReflection(mojo, "pluginVersionResolver", pluginVersionResolver);
        setFieldWithReflection(mojo, "pluginManager", pluginManager);
        setParentFieldWithReflection(mojo, "session", session);
    }

    private static Method setLookupPluginDescriptorAccessibility() throws NoSuchMethodException {
        Method lookupPluginDescriptor =
                DescribeMojo.class.getDeclaredMethod("lookupPluginDescriptor", PluginInfo.class);
        lookupPluginDescriptor.setAccessible(true);
        return lookupPluginDescriptor;
    }

    private static Method setParsePluginLookupInfoAccessibility() throws NoSuchMethodException {
        Method parsePluginLookupInfo = DescribeMojo.class.getDeclaredMethod("parsePluginLookupInfo");
        parsePluginLookupInfo.setAccessible(true);
        return parsePluginLookupInfo;
    }
}

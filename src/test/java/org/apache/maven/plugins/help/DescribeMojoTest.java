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
import org.apache.maven.plugin.descriptor.MojoDescriptor;
import org.apache.maven.plugin.descriptor.Parameter;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.version.PluginVersionRequest;
import org.apache.maven.plugin.version.PluginVersionResolver;
import org.apache.maven.plugin.version.PluginVersionResult;
import org.apache.maven.plugins.help.DescribeMojo.PluginInfo;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystemSession;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author <a href="mailto:vincent.siveton@gmail.com">Vincent Siveton</a>
 */
public class DescribeMojoTest {
    /**
     * Test method for {@link org.apache.maven.plugins.help.DescribeMojo#toLines(java.lang.String, int, int, int)}.
     *
     */
    @Test
    public void testGetExpressionsRoot() {
        try {
            DescribeMojo describeMojo = new DescribeMojo();
            Method toLines =
                    describeMojo.getClass().getDeclaredMethod("toLines", String.class, int.class, int.class, int.class);
            toLines.setAccessible(true);
            toLines.invoke(null, "", 2, 2, 80);
        } catch (Throwable e) {
            fail("The API changes");
        }
    }

    @Test
    public void testValidExpression() throws Exception {
        StringBuilder sb = new StringBuilder();
        MojoDescriptor md = new MojoDescriptor();
        Parameter parameter = new Parameter();
        parameter.setName("name");
        parameter.setExpression("${valid.expression}");
        md.addParameter(parameter);

        String ls = System.getProperty("line.separator");

        try {
            Method describeMojoParameters = DescribeMojo.class.getDeclaredMethod(
                    "describeMojoParameters", MojoDescriptor.class, StringBuilder.class);
            describeMojoParameters.setAccessible(true);
            describeMojoParameters.invoke(new DescribeMojo(), md, sb);

            assertEquals(
                    "  Available parameters:" + ls + ls + "    name" + ls + "      User property: valid.expression" + ls
                            + "      (no description available)" + ls,
                    sb.toString());
        } catch (Throwable e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testInvalidExpression() throws Exception {
        StringBuilder sb = new StringBuilder();
        MojoDescriptor md = new MojoDescriptor();
        Parameter parameter = new Parameter();
        parameter.setName("name");
        parameter.setExpression("${project.build.directory}/generated-sources/foobar"); // this is a defaultValue
        md.addParameter(parameter);

        String ls = System.getProperty("line.separator");

        try {
            Method describeMojoParameters = DescribeMojo.class.getDeclaredMethod(
                    "describeMojoParameters", MojoDescriptor.class, StringBuilder.class);
            describeMojoParameters.setAccessible(true);
            describeMojoParameters.invoke(new DescribeMojo(), md, sb);

            assertEquals(
                    "  Available parameters:" + ls + ls
                            + "    name"
                            + ls + "      Expression: ${project.build.directory}/generated-sources/foobar"
                            + ls + "      (no description available)"
                            + ls,
                    sb.toString());
        } catch (Throwable e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testParsePluginInfoGAV() throws Throwable {
        DescribeMojo mojo = new DescribeMojo();
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
    public void testParsePluginInfoPluginPrefix() throws Throwable {
        DescribeMojo mojo = new DescribeMojo();
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
    public void testParsePluginInfoPluginGA() throws Throwable {
        DescribeMojo mojo = new DescribeMojo();
        setFieldWithReflection(mojo, "plugin", "org.test:test");

        Method parsePluginLookupInfo = setParsePluginLookupInfoAccessibility();
        PluginInfo pi = (PluginInfo) parsePluginLookupInfo.invoke(mojo);

        assertEquals("org.test", pi.getGroupId());
        assertEquals("test", pi.getArtifactId());
        assertNull(pi.getVersion());
        assertNull(pi.getPrefix());
    }

    @Test
    public void testParsePluginInfoPluginGAV() throws Throwable {
        DescribeMojo mojo = new DescribeMojo();
        setFieldWithReflection(mojo, "plugin", "org.test:test:1.0");

        Method parsePluginLookupInfo = setParsePluginLookupInfoAccessibility();
        PluginInfo pi = (PluginInfo) parsePluginLookupInfo.invoke(mojo);

        assertEquals("org.test", pi.getGroupId());
        assertEquals("test", pi.getArtifactId());
        assertEquals("1.0", pi.getVersion());
        assertNull(pi.getPrefix());
    }

    @Test
    public void testParsePluginInfoPluginIncorrect() throws Throwable {
        DescribeMojo mojo = new DescribeMojo();
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
    public void testLookupPluginDescriptorPrefixWithVersion() throws Throwable {
        DescribeMojo mojo = new DescribeMojo();

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
    public void testLookupPluginDescriptorPrefixWithoutVersion() throws Throwable {
        DescribeMojo mojo = new DescribeMojo();

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
    public void testLookupPluginDescriptorGAV() throws Throwable {
        DescribeMojo mojo = new DescribeMojo();

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
    public void testLookupPluginDescriptorGMissingA() {
        DescribeMojo mojo = new DescribeMojo();
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
    public void testLookupPluginDescriptorAMissingG() {
        DescribeMojo mojo = new DescribeMojo();
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
     * Regression test for Maven 3.10, where the default lifecycle has an empty map of built-in bindings. The mojo must
     * use the packaging-specific lifecycle mapping in that case.
     */
    @Test
    public void testDescribeCommandPackagingSpecificPhaseShowsBindings() throws Exception {
        Lifecycle lifecycle = mock(Lifecycle.class);
        when(lifecycle.getId()).thenReturn("default");
        when(lifecycle.getPhases()).thenReturn(Arrays.asList("validate", "compile", "test", "package"));
        when(lifecycle.getDefaultPhases()).thenReturn(Collections.emptyMap());
        when(lifecycle.getDefaultLifecyclePhases()).thenReturn(Collections.emptyMap());

        Map<String, Lifecycle> phaseMap = new HashMap<>();
        for (String phase : Arrays.asList("validate", "compile", "test", "package")) {
            phaseMap.put(phase, lifecycle);
        }
        DefaultLifecycles defaultLifecycles = mock(DefaultLifecycles.class);
        when(defaultLifecycles.getPhaseToLifecycleMap()).thenReturn(phaseMap);

        Map<String, String> legacyPhases = new LinkedHashMap<>();
        legacyPhases.put("compile", "org.apache.maven.plugins:maven-compiler-plugin:3.11.0:compile");
        Map<String, LifecyclePhase> lifecyclePhases = new LinkedHashMap<>();
        lifecyclePhases.put(
                "compile", new LifecyclePhase("org.apache.maven.plugins:maven-compiler-plugin:3.11.0:compile"));

        org.apache.maven.lifecycle.mapping.Lifecycle mappingLifecycle =
                mock(org.apache.maven.lifecycle.mapping.Lifecycle.class);
        when(mappingLifecycle.getPhases()).thenReturn(legacyPhases);
        when(mappingLifecycle.getLifecyclePhases()).thenReturn(lifecyclePhases);
        LifecycleMapping lifecycleMapping = mock(LifecycleMapping.class);
        when(lifecycleMapping.getLifecycles()).thenReturn(Collections.singletonMap("default", mappingLifecycle));

        MavenProject project = mock(MavenProject.class);
        when(project.getPackaging()).thenReturn("jar");

        DescribeMojo mojo = new DescribeMojo();
        setFieldWithReflection(mojo, "cmd", "compile");
        setFieldWithReflection(mojo, "defaultLifecycles", defaultLifecycles);
        setFieldWithReflection(mojo, "lifecycleMappings", Collections.singletonMap("jar", lifecycleMapping));
        setParentFieldWithReflection(mojo, "project", project);

        StringBuilder buffer = new StringBuilder();
        Method describeCommand = DescribeMojo.class.getDeclaredMethod("describeCommand", StringBuilder.class);
        describeCommand.setAccessible(true);
        boolean result = (boolean) describeCommand.invoke(mojo, buffer);

        String output = buffer.toString();
        assertFalse(result);
        assertTrue("Should show the compiler plugin: " + output, output.contains("maven-compiler-plugin"));
        assertFalse("compile should not be 'Not defined': " + output, output.contains("* compile: Not defined"));
        assertTrue("validate has no binding: " + output, output.contains("* validate: Not defined"));
        assertTrue(output, output.contains("It is a part of the lifecycle for the POM packaging 'jar'"));
    }

    @Test
    public void testDescribeCommandBuiltinLifecyclePhaseShowsBindings() throws Exception {
        Map<String, String> legacyPhases = new LinkedHashMap<>();
        legacyPhases.put("clean", "org.apache.maven.plugins:maven-clean-plugin:3.2.0:clean");
        Map<String, LifecyclePhase> lifecyclePhases = new LinkedHashMap<>();
        lifecyclePhases.put("pre-clean", null);
        lifecyclePhases.put("clean", new LifecyclePhase("org.apache.maven.plugins:maven-clean-plugin:3.2.0:clean"));
        lifecyclePhases.put("post-clean", null);

        Lifecycle lifecycle = mock(Lifecycle.class);
        when(lifecycle.getId()).thenReturn("clean");
        when(lifecycle.getPhases()).thenReturn(Arrays.asList("pre-clean", "clean", "post-clean"));
        when(lifecycle.getDefaultPhases()).thenReturn(legacyPhases);
        when(lifecycle.getDefaultLifecyclePhases()).thenReturn(lifecyclePhases);

        Map<String, Lifecycle> phaseMap = new HashMap<>();
        for (String phase : Arrays.asList("pre-clean", "clean", "post-clean")) {
            phaseMap.put(phase, lifecycle);
        }
        DefaultLifecycles defaultLifecycles = mock(DefaultLifecycles.class);
        when(defaultLifecycles.getPhaseToLifecycleMap()).thenReturn(phaseMap);

        MavenProject project = mock(MavenProject.class);
        when(project.getPackaging()).thenReturn("jar");

        DescribeMojo mojo = new DescribeMojo();
        setFieldWithReflection(mojo, "cmd", "clean");
        setFieldWithReflection(mojo, "defaultLifecycles", defaultLifecycles);
        setFieldWithReflection(mojo, "lifecycleMappings", Collections.emptyMap());
        setParentFieldWithReflection(mojo, "project", project);

        StringBuilder buffer = new StringBuilder();
        Method describeCommand = DescribeMojo.class.getDeclaredMethod("describeCommand", StringBuilder.class);
        describeCommand.setAccessible(true);
        boolean result = (boolean) describeCommand.invoke(mojo, buffer);

        String output = buffer.toString();
        assertFalse(result);
        assertTrue(output, output.contains("'clean' is a phase within the 'clean' lifecycle"));
        assertTrue(output, output.contains("maven-clean-plugin"));
        assertTrue(output, output.contains("* pre-clean: Not defined"));
        assertTrue(output, output.contains("* post-clean: Not defined"));
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

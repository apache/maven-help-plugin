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
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.maven.api.Project;
import org.apache.maven.api.Session;
import org.apache.maven.api.di.Provides;
import org.apache.maven.api.di.Singleton;
import org.apache.maven.api.model.InputSource;
import org.apache.maven.api.model.Model;
import org.apache.maven.api.plugin.Log;
import org.apache.maven.api.plugin.testing.Basedir;
import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.api.plugin.testing.stubs.ProducedArtifactStub;
import org.apache.maven.api.plugin.testing.stubs.ProjectStub;
import org.apache.maven.api.services.ArtifactManager;
import org.apache.maven.model.v4.MavenStaxReader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@MojoTest
class EffectivePomMojoTest {

    static final String CONFIG_XML = "classpath:/unit/evaluate/plugin-config.xml";

    final ByteArrayOutputStream stdout = new ByteArrayOutputStream();

    final Log log = mock(Log.class);

    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(stdout));
    }

    @Test
    @InjectMojo(goal = "effective-pom", pom = CONFIG_XML)
    @MojoParameter(name = "verbose", value = "false")
    @MojoParameter(name = "forceStdout", value = "true")
    @Basedir
    void testEffectivePomForceStdout(EffectivePomMojo mojo) {
        when(log.isInfoEnabled()).thenReturn(false);
        mojo.execute();

        String output = stdout.toString();
    }

    //    @Provides
    //    @Singleton
    //    Session createSession() {
    //        InternalSession session = SessionMock.getMockSession("target/local-repo");
    //
    //        when(session.getSettings()).thenReturn(Settings.newInstance());
    //
    //        MessageBuilderFactory mbf = new DefaultMessageBuilderFactory
    //        when(session.getService(MessageBuilderFactory.class)).thenReturn(mbf);
    //
    //        return session;
    //    }

    @Provides
    @Singleton
    Log createlog() {
        return log;
    }

    @Provides
    @Singleton
    Project createProject(Session s) throws Exception {
        Path path = Paths.get(getClass()
                .getResource(CONFIG_XML.substring("classpath:".length()))
                .getFile());
        Model model;
        try (InputStream is = Files.newInputStream(path)) {
            InputSource source = new InputSource(null, path.toUri().toString());
            model = new MavenStaxReader().read(is, true, source);
        }

        ProjectStub stub = new ProjectStub();
        if (!"pom".equals(model.getPackaging())) {
            ProducedArtifactStub artifact = new ProducedArtifactStub(
                    model.getGroupId(), model.getArtifactId(), "", model.getVersion(), model.getPackaging());
            stub.setMainArtifact(artifact);
        }
        stub.setModel(model);
        stub.setBasedir(path.getParent());
        stub.setPomPath(path);
        s.getService(ArtifactManager.class).setPath(stub.getPomArtifact(), path);
        return stub;
    }
}

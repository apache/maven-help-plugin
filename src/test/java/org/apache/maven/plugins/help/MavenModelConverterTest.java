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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import com.thoughtworks.xstream.mapper.MapperWrapper;
import org.apache.maven.model.InputLocation;
import org.apache.maven.model.Model;
import org.apache.maven.model.Resource;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MavenModelConverterTest {
    private XStream newXStream() {
        XStream xstream = new XStream(new DomDriver()) {
            @Override
            protected MapperWrapper wrapMapper(MapperWrapper next) {
                return MavenModelConverter.createMapper(next);
            }
        };
        MavenModelConverter.register(xstream, Model.class);
        MavenModelConverter.register(xstream, Settings.class);
        return xstream;
    }

    @Test
    void preservesNullCollectionElements() {
        String xml = newXStream().toXML(new ArrayList<>(Arrays.asList(null, "value")));
        assertTrue(xml.contains("<null/>"), xml);
        assertTrue(xml.contains("<string>value</string>"), xml);
    }

    @Test
    void preservesOrdinaryMapEntries() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("delegate", "delegate value");
        values.put("locations", "locations value");
        values.put("importedFrom", "importedFrom value");
        String xml = newXStream().toXML(values);
        values.forEach((key, value) -> {
            assertTrue(xml.contains("<string>" + key + "</string>"), xml);
            assertTrue(xml.contains("<string>" + value + "</string>"), xml);
        });
    }

    @Test
    void preservesUnrelatedObjectsWithDelegateGetters() {
        XStream xstream = newXStream();
        xstream.alias("unrelated", Unrelated.class);
        String xml = xstream.toXML(new Unrelated());
        assertTrue(xml.contains("<delegate>delegate value</delegate>"), xml);
        assertTrue(xml.contains("<locations>locations value</locations>"), xml);
        assertTrue(xml.contains("<importedFrom>importedFrom value</importedFrom>"), xml);
    }

    @Test
    void preservesMaven3ModelFieldsAndLocations() {
        Resource resource = new Resource();
        resource.setDirectory("src/main/resources");
        resource.setLocation("directory", new InputLocation(12, 34));
        XStream xstream = newXStream();
        xstream.alias("resource", Resource.class);
        String xml = xstream.toXML(resource);
        assertTrue(xml.contains("<directory>src/main/resources</directory>"), xml);
        assertTrue(xml.contains("<lineNumber>12</lineNumber>"), xml);
        assertTrue(xml.contains("<columnNumber>34</columnNumber>"), xml);
    }

    private static class Unrelated {
        private final String delegate = "delegate value";
        private final String locations = "locations value";
        private final String importedFrom = "importedFrom value";

        public String getDelegate() {
            throw new AssertionError("Unrelated delegate getter must not be invoked");
        }
    }
}

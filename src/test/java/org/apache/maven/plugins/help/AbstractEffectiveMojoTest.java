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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AbstractEffectiveMojoTest {
    @Test
    void prettyFormatAcceptsPropertyNamesWithMultipleColons() {
        String model = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<project><properties><org.mockito:mockito-core:jar>4.11.0</org.mockito:mockito-core:jar>"
                + "</properties></project>";

        String formatted = AbstractEffectiveMojo.prettyFormat(model, "UTF-8", false);

        assertTrue(formatted.contains("<org.mockito:mockito-core:jar>4.11.0</org.mockito:mockito-core:jar>"));
        assertEquals(1, formatted.split("<\\?xml", -1).length - 1);
    }

    @Test
    void prettyFormatAcceptsPrefixedElementsWithAttributes() {
        String model = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<project><target><ac:if xmlns:ac=\"antlib:net.sf.antcontrib\">"
                + "<equals arg1=\"a\" arg2=\"a\"/><ac:then><echo message=\"hello\"/></ac:then>"
                + "<ac:else attr='x'/></ac:if></target></project>";

        String formatted = AbstractEffectiveMojo.prettyFormat(model, "UTF-8", false);

        assertTrue(formatted.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"), formatted);
        assertEquals(1, formatted.split("<\\?xml", -1).length - 1);
        assertTrue(formatted.contains("<ac:if xmlns:ac=\"antlib:net.sf.antcontrib\">"), formatted);
        assertTrue(formatted.contains("<ac:then>"), formatted);
        assertTrue(formatted.contains("<ac:else attr=\"x\" />"), formatted);
        assertTrue(formatted.contains("</ac:if>"), formatted);
        assertFalse(formatted.contains("__MAVEN_HELP_PLUGIN_COLON__"), formatted);
    }
}

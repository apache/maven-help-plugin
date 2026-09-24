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

import java.lang.reflect.Method;
import java.util.Properties;

import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class EffectiveSettingsMojoTest {

    @Test
    void copySettingsDoesNotShareProfileProperties() throws Exception {
        Settings settings = new Settings();
        Profile profile = new Profile();
        Properties properties = new Properties();
        properties.setProperty("password", "secret");
        profile.setProperties(properties);
        settings.addProfile(profile);

        Method copySettings = EffectiveSettingsMojo.class.getDeclaredMethod("copySettings", Settings.class);
        copySettings.setAccessible(true);
        Settings copy = (Settings) copySettings.invoke(null, settings);

        assertNotSame(settings.getProfiles().get(0), copy.getProfiles().get(0));
        assertNotSame(settings.getProfiles().get(0).getProperties(), copy.getProfiles().get(0).getProperties());

        copy.getProfiles().get(0).getProperties().setProperty("password", "***");

        assertEquals("secret", settings.getProfiles().get(0).getProperties().getProperty("password"));
    }
}

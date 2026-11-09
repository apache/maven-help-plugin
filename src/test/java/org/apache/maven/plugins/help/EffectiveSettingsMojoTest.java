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
import java.util.Properties;

import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Settings;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class EffectiveSettingsMojoTest {

    @Test
    void showPasswordsDoesNotMutateSettingsProfiles() throws Exception {
        EffectiveSettingsMojo mojo = new EffectiveSettingsMojo(null, null);
        Settings settings = new Settings();
        Profile profile = new Profile();
        Properties properties = new Properties();
        properties.setProperty("key", "value");
        profile.setProperties(properties);
        settings.addProfile(profile);

        setField(mojo, "settings", settings);
        setField(mojo, "showPasswords", true);

        mojo.execute();

        assertSame(properties, profile.getProperties());
    }

    private static void setField(Object target, String name, Object value) throws ReflectiveOperationException {
        Class<?> type = target.getClass();
        Field field = null;
        while (type != null && field == null) {
            try {
                field = type.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            }
        }
        if (field == null) {
            throw new NoSuchFieldException(name);
        }
        field.setAccessible(true);
        field.set(target, value);
    }
}

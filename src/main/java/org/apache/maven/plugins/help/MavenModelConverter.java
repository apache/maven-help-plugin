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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.MapperWrapper;

/** Serializes Maven 4 compatibility models without their wrapper or source locations. */
final class MavenModelConverter extends ReflectionConverter {
    private final Class<?> wrapperType;
    private final Method getDelegate;

    private MavenModelConverter(XStream xstream, Class<?> wrapperType) throws NoSuchMethodException {
        super(xstream.getMapper(), xstream.getReflectionProvider());
        this.wrapperType = wrapperType;
        this.getDelegate = wrapperType.getMethod("getDelegate");
    }

    static void register(XStream xstream, Class<?> modelType) {
        try {
            Class<?> wrapperType =
                    Class.forName(modelType.getPackage().getName() + ".BaseObject", false, modelType.getClassLoader());
            xstream.registerConverter(new MavenModelConverter(xstream, wrapperType));
        } catch (ClassNotFoundException e) {
            // Maven 3 models do not wrap API model delegates.
        } catch (NoSuchMethodException e) {
            throw new ConversionException("Cannot access Maven model delegate", e);
        }
    }

    static MapperWrapper createMapper(MapperWrapper next) {
        return new MapperWrapper(next) {
            @Override
            public String serializedClass(Class type) {
                if (isApiModel(type)) {
                    try {
                        Class<?> wrapper = Class.forName(
                                type.getName().replace("maven.api.", "maven."),
                                false,
                                MavenModelConverter.class.getClassLoader());
                        if (wrapper.getMethod("getDelegate").getReturnType() == type) {
                            // Preserve the compatibility class's existing name or alias for nested delegates.
                            return super.serializedClass(wrapper);
                        }
                    } catch (ClassNotFoundException | NoSuchMethodException e) {
                        // API-only types have no compatibility wrapper.
                    }
                }
                return super.serializedClass(type);
            }

            @Override
            public boolean shouldSerializeMember(Class definedIn, String fieldName) {
                if (("locations".equals(fieldName) || "importedFrom".equals(fieldName)) && isApiModel(definedIn)) {
                    return false;
                }
                return super.shouldSerializeMember(definedIn, fieldName);
            }
        };
    }

    private static boolean isApiModel(Class<?> type) {
        Package modelPackage = type != null ? type.getPackage() : null;
        return modelPackage != null
                && ("org.apache.maven.api.model".equals(modelPackage.getName())
                        || "org.apache.maven.api.settings".equals(modelPackage.getName()));
    }

    @Override
    public boolean canConvert(Class type) {
        return wrapperType.isAssignableFrom(type) && wrapperType.getPackage().equals(type.getPackage());
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
        Object delegate;
        try {
            delegate = getDelegate.invoke(source);
        } catch (IllegalAccessException e) {
            throw new ConversionException("Cannot access Maven model delegate", e);
        } catch (InvocationTargetException e) {
            throw new ConversionException("Cannot read Maven model delegate", e.getCause());
        }
        if (delegate == null) {
            throw new ConversionException("Maven model delegate is null");
        }
        // Write the delegate's fields into the existing wrapper element, also for nested models.
        super.marshal(delegate, writer, context);
    }
}

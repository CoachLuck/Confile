/*
 *   Project: Confile
 *   File: ConfigurationSerialization.java
 *   Last Modified: 1/22/21, 2:43 PM
 *
 *    Copyright 2021 AJ Romaniello
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package io.coachluck.confile.serialization;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ConfigurationSerialization {
    public static final String SERIALIZED_TYPE_KEY = "==";
    private final Class<? extends ConfigurationSerializable> clazz;
    private static Map<String, Class<? extends ConfigurationSerializable>> aliases = new HashMap();

    protected ConfigurationSerialization(@NotNull Class<? extends ConfigurationSerializable> clazz) {
        this.clazz = clazz;
    }

    @Nullable
    protected Method getMethod(@NotNull String name, boolean isStatic) {
        try {
            Method method = this.clazz.getDeclaredMethod(name, Map.class);
            if (!ConfigurationSerializable.class.isAssignableFrom(method.getReturnType())) {
                return null;
            }

            return Modifier.isStatic(method.getModifiers()) != isStatic ? null : method;
        } catch (NoSuchMethodException | SecurityException e) {
            return null;
        }
    }

    @Nullable
    protected Constructor<? extends ConfigurationSerializable> getConstructor() {
        try {
            return this.clazz.getConstructor(Map.class);
        } catch (NoSuchMethodException | SecurityException var2) {
            return null;
        }
    }

    @Nullable
    protected ConfigurationSerializable deserializeViaMethod(@NotNull Method method, @NotNull Map<String, ?> args) {
        try {
            ConfigurationSerializable result = (ConfigurationSerializable) method.invoke(null, args);
            if (result != null) {
                return result;
            }

            Logger.getLogger(ConfigurationSerialization.class.getName()).log(Level.SEVERE,
                    "Could not call method '" + method.toString() + "' of " + this.clazz
                            + " for deserialization: method returned null");
        } catch (Throwable e) {
            Logger.getLogger(ConfigurationSerialization.class.getName()).log(Level.SEVERE,
                    "Could not call method '" + method.toString() + "' of " + this.clazz + " for deserialization",
                    e instanceof InvocationTargetException ? e.getCause() : e);
        }

        return null;
    }

    @Nullable
    protected ConfigurationSerializable deserializeViaCtor(@NotNull Constructor<? extends ConfigurationSerializable> struct, @NotNull Map<String, ?> args) {
        try {
            return struct.newInstance(args);
        } catch (Throwable e) {
            Logger.getLogger(ConfigurationSerialization.class.getName()).log(Level.SEVERE,
                    "Could not call constructor '" + struct.toString() + "' of " + this.clazz + " for deserialization",
                    e instanceof InvocationTargetException ? e.getCause() : e);
            return null;
        }
    }

    @Nullable
    public ConfigurationSerializable deserialize(@NotNull Map<String, ?> args) {
        ConfigurationSerializable result = null;
        Method method = this.getMethod("deserialize", true);
        if (method != null) {
            result = this.deserializeViaMethod(method, args);
        }

        if (result == null) {
            method = this.getMethod("valueOf", true);
            if (method != null) {
                result = this.deserializeViaMethod(method, args);
            }
        }

        if (result == null) {
            Constructor<? extends ConfigurationSerializable> constructor = this.getConstructor();
            if (constructor != null) {
                result = this.deserializeViaCtor(constructor, args);
            }
        }

        return result;
    }

    @Nullable
    public static ConfigurationSerializable deserializeObject(@NotNull Map<String, ?> args, @NotNull Class<? extends ConfigurationSerializable> clazz) {
        return (new ConfigurationSerialization(clazz)).deserialize(args);
    }

    @Nullable
    public static ConfigurationSerializable deserializeObject(@NotNull Map<String, ?> args) {
        Class<? extends ConfigurationSerializable> clazz;
        if (args.containsKey(SERIALIZED_TYPE_KEY)) {
            try {
                String alias = (String)args.get(SERIALIZED_TYPE_KEY);
                if (alias == null) {
                    throw new IllegalArgumentException("Cannot have null alias");
                }

                clazz = getClassByAlias(alias);
                if (clazz == null) {
                    throw new IllegalArgumentException("Specified class does not exist ('" + alias + "')");
                }
            } catch (ClassCastException e) {
                e.fillInStackTrace();
                throw e;
            }

            return (new ConfigurationSerialization(clazz)).deserialize(args);
        }

        throw new IllegalArgumentException("Args doesn't contain type key ('" + SERIALIZED_TYPE_KEY + "')");
    }

    public static void registerClass(@NotNull Class<? extends ConfigurationSerializable> clazz) {
        DelegateDeserialization delegate = clazz.getAnnotation(DelegateDeserialization.class);
        if (delegate == null) {
            registerClass(clazz, getAlias(clazz));
            registerClass(clazz, clazz.getName());
        }
    }

    public static void registerClass(@NotNull Class<? extends ConfigurationSerializable> clazz, @NotNull String alias) {
        aliases.put(alias, clazz);
    }

    public static void unregisterClass(@NotNull String alias) {
        aliases.remove(alias);
    }

    public static void unregisterClass(@NotNull Class<? extends ConfigurationSerializable> clazz) {
        while(aliases.values().remove(clazz)) { }
    }

    @Nullable
    public static Class<? extends ConfigurationSerializable> getClassByAlias(@NotNull String alias) {
        return aliases.get(alias);
    }

    @NotNull
    public static String getAlias(@NotNull Class<? extends ConfigurationSerializable> clazz) {
        DelegateDeserialization delegate = clazz.getAnnotation(DelegateDeserialization.class);
        if (delegate != null) {
            if (delegate.value() != clazz) {
                return getAlias(delegate.value());
            }

            delegate = null;
        }

        SerializableAs alias = clazz.getAnnotation(SerializableAs.class);
        return alias != null ? alias.value() : clazz.getName();
    }
}
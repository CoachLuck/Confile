/*
 *   Project: Confile
 *   File: MemorySection.java
 *   Last Modified: 1/22/21, 2:17 PM
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

package io.coachluck.confile;

import io.coachluck.confile.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.Map.Entry;

public class MemorySection implements ConfigurationSection {
    protected final Map<String, Object> map = new LinkedHashMap<>();
    private final Configuration root;
    private final ConfigurationSection parent;
    private final String path;
    private final String fullPath;

    protected MemorySection() {
        if (!(this instanceof Configuration)) {
            throw new IllegalStateException("Cannot construct a root core.MemorySection when not a Configuration");
        }

        this.path = "";
        this.fullPath = "";
        this.parent = null;
        this.root = (Configuration) this;
    }

    protected MemorySection(@NotNull ConfigurationSection parent, @NotNull String path) {
        this.path = path;
        this.parent = parent;
        this.root = parent.getRoot();
        this.fullPath = createPath(parent, path);
    }

    @NotNull
    public Set<String> getKeys(boolean deep) {
        Set<String> result = new LinkedHashSet<>();
        Configuration root = this.getRoot();
        if (root != null && root.options().copyDefaults()) {
            ConfigurationSection defaults = this.getDefaultSection();
            if (defaults != null) {
                result.addAll(defaults.getKeys(deep));
            }
        }

        this.mapChildrenKeys(result, this, deep);
        return result;
    }

    @NotNull
    public Map<String, Object> getValues(boolean deep) {
        Map<String, Object> result = new LinkedHashMap<>();
        Configuration root = this.getRoot();
        if (root != null && root.options().copyDefaults()) {
            ConfigurationSection defaults = this.getDefaultSection();
            if (defaults != null) {
                result.putAll(defaults.getValues(deep));
            }
        }

        this.mapChildrenValues(result, this, deep);
        return result;
    }

    public boolean contains(@NotNull String path) {
        return this.contains(path, false);
    }

    public boolean contains(@NotNull String path, boolean ignoreDefault) {
        return (ignoreDefault ? this.get(path, null) : this.get(path)) != null;
    }

    public boolean isSet(@NotNull String path) {
        Configuration root = this.getRoot();
        if (root == null) {
            return false;
        } else if (root.options().copyDefaults()) {
            return this.contains(path);
        } else {
            return this.get(path, null) != null;
        }
    }

    @NotNull
    public String getCurrentPath() {
        return this.fullPath;
    }

    @NotNull
    public String getName() {
        return this.path;
    }

    @Nullable
    public Configuration getRoot() {
        return this.root;
    }

    @Nullable
    public ConfigurationSection getParent() {
        return this.parent;
    }

    public void addDefault(@NotNull String path, @Nullable Object value) {
        Configuration root = this.getRoot();
        if (root == null) {
            throw new IllegalStateException("Cannot add default without root");
        } else if (root == this) {
            throw new UnsupportedOperationException("Unsupported addDefault(String, Object) implementation");
        } else {
            root.addDefault(createPath(this, path), value);
        }
    }

    @Nullable
    public ConfigurationSection getDefaultSection() {
        Configuration root = this.getRoot();
        Configuration defaults = root == null ? null : root.getDefaults();
        return defaults != null && defaults.isConfigurationSection(this.getCurrentPath()) ? defaults.getConfigurationSection(this.getCurrentPath()) : null;
    }

    public void set(@NotNull String path, @Nullable Object value) {
        Configuration root = this.getRoot();
        if (root == null) {
            throw new IllegalStateException("Cannot use section without a root");
        } else {
            char separator = root.options().pathSeparator();
            int i1 = -1;
            ConfigurationSection section = this;

            int i2;
            String key;
            while((i1 = path.indexOf(separator, i2 = i1 + 1)) != -1) {
                key = path.substring(i2, i1);
                ConfigurationSection subSection = section.getConfigurationSection(key);
                if (subSection == null) {
                    if (value == null) {
                        return;
                    }

                    section = section.createSection(key);
                } else {
                    section = subSection;
                }
            }

            key = path.substring(i2);
            if (section == this) {
                if (value == null) {
                    this.map.remove(key);
                } else {
                    this.map.put(key, value);
                }
            } else {
                section.set(key, value);
            }
        }
    }

    @Nullable
    public Object get(@NotNull String path) {
        return this.get(path, this.getDefault(path));
    }

    @Nullable
    public Object get(@NotNull String path, @Nullable Object def) {
        if (path.length() == 0) {
            return this;
        }

        Configuration root = this.getRoot();
        if (root == null) {
            throw new IllegalStateException("Cannot access section without a root");
        } else {
            char separator = root.options().pathSeparator();
            int i1 = -1;
            ConfigurationSection section = this;

            int i2;
            while((i1 = path.indexOf(separator, i2 = i1 + 1)) != -1) {
                section = section.getConfigurationSection(path.substring(i2, i1));
                if (section == null) {
                    return def;
                }
            }

            String key = path.substring(i2);
            if (section == this) {
                Object result = this.map.get(key);
                return result == null ? def : result;
            } else {
                return section.get(key, def);
            }
        }
    }

    @NotNull
    public ConfigurationSection createSection(@NotNull String path) {
        Configuration root = this.getRoot();
        if (root == null) {
            throw new IllegalStateException("Cannot create section without a root");
        }

        char separator = root.options().pathSeparator();
        int i1 = -1;
        ConfigurationSection section = this;

        int i2;
        String key;
        while((i1 = path.indexOf(separator, i2 = i1 + 1)) != -1) {
            key = path.substring(i2, i1);
            ConfigurationSection subSection = section.getConfigurationSection(key);
            if (subSection == null) {
                section = section.createSection(key);
            } else {
                section = subSection;
            }
        }

        key = path.substring(i2);
        if (section == this) {
            ConfigurationSection result = new MemorySection(this, key);
            this.map.put(key, result);
            return result;
        } else {
            return section.createSection(key);
        }
    }

    @NotNull
    public ConfigurationSection createSection(@NotNull String path, @NotNull Map<?, ?> map) {
        ConfigurationSection section = this.createSection(path);

        for (Entry<?, ?> value : map.entrySet()) {
            if (value.getValue() instanceof Map) {
                section.createSection(value.getKey().toString(), (Map) value.getValue());
            } else {
                section.set(value.getKey().toString(), value.getValue());
            }
        }

        return section;
    }

    @Nullable
    public String getString(@NotNull String path) {
        Object def = this.getDefault(path);
        return this.getString(path, def != null ? def.toString() : null);
    }

    @Nullable
    public String getString(@NotNull String path, @Nullable String def) {
        Object val = this.get(path, def);
        return val != null ? val.toString() : def;
    }

    public boolean isString(@NotNull String path) {
        Object val = this.get(path);
        return val instanceof String;
    }

    @Nullable
    public List<?> getList(@NotNull String path) {
        Object def = this.getDefault(path);
        return this.getList(path, def instanceof List ? (List) def : null);
    }

    @Nullable
    public List<?> getList(@NotNull String path, @Nullable List<?> def) {
        Object val = this.get(path, def);
        return (List) (val instanceof List ? val : def);
    }

    public boolean isList(@NotNull String path) {
        Object val = this.get(path);
        return val instanceof List;
    }

    @NotNull
    public ArrayList<String> getStringArrayList(@NotNull String path) {
        return (ArrayList<String>) getStringList(path);
    }

    @NotNull
    public List<String> getStringList(@NotNull String path) {
        List<?> list = this.getList(path);
        if (list == null) {
            return new ArrayList<>(0);
        }

        List<String> result = new ArrayList<>();
        Iterator<?> it = list.iterator();
        while(true) {
            Object object;
            do {
                if (!it.hasNext()) {
                    return result;
                }

                object = it.next();
            } while (!(object instanceof String) && !this.isPrimitiveWrapper(object));

            result.add(String.valueOf(object));
        }
    }

    @NotNull
    public List<Character> getCharacterList(@NotNull String path) {
        List<?> list = this.getList(path);
        if (list == null) {
            return new ArrayList<>(0);
        }
        List<Character> result = new ArrayList<>();

        for (Object object : list) {
            if (object instanceof Character) {
                result.add((Character) object);
            } else if (object instanceof String) {
                String str = (String) object;
                if (str.length() == 1) {
                    result.add(str.charAt(0));
                }
            } else if (object instanceof Number) {
                result.add((char) ((Number) object).intValue());
            }
        }

        return result;
    }

    @NotNull
    public List<Map<?, ?>> getMapList(@NotNull String path) {
        List<?> list = this.getList(path);
        List<Map<?, ?>> result = new ArrayList<>();
        if (list == null) {
            return result;
        }

        for (Object object : list) {
            if (object instanceof Map) {
                result.add((Map) object);
            }
        }

        return result;
    }

    @Nullable
    public <T> T getObject(@NotNull String path, @NotNull Class<T> clazz) {
        Object def = this.getDefault(path);
        return this.getObject(path, clazz, clazz.isInstance(def) ? clazz.cast(def) : null);
    }

    @Nullable
    public <T> T getObject(@NotNull String path, @NotNull Class<T> clazz, @Nullable T def) {
        Object val = this.get(path, def);
        return clazz.isInstance(val) ? clazz.cast(val) : def;
    }

    @Nullable
    public <T extends ConfigurationSerializable> T getSerializable(@NotNull String path, @NotNull Class<T> clazz) {
        return this.getObject(path, clazz);
    }

    @Nullable
    public <T extends ConfigurationSerializable> T getSerializable(@NotNull String path, @NotNull Class<T> clazz, @Nullable T def) {
        return this.getObject(path, clazz, def);
    }

    /**
     * Gets the configuration section at the provided path
     * @param path the path to the configuration section
     * @return the ConfigurationSection, null if not present
     */
    @Nullable
    public ConfigurationSection getConfigurationSection(@NotNull String path) {
        Object val = this.get(path, null);
        if (val != null) {
            return val instanceof ConfigurationSection ? (ConfigurationSection)val : null;
        } else {
            val = this.get(path, this.getDefault(path));
            return val instanceof ConfigurationSection ? this.createSection(path) : null;
        }
    }

    /**
     * Checks whether or not a configuration section exists at the path
     * @param path the path to check
     * @return true if ConfigurationSection, false if not
     */
    public boolean isConfigurationSection(@NotNull String path) {
        Object val = this.get(path);
        return val instanceof ConfigurationSection;
    }

    protected boolean isPrimitiveWrapper(@Nullable Object input) {
        return input instanceof Integer || input instanceof Boolean || input instanceof Character || input instanceof Byte || input instanceof Short || input instanceof Double || input instanceof Long || input instanceof Float;
    }

    @Nullable
    protected Object getDefault(@NotNull String path) {
        Configuration root = this.getRoot();
        Configuration defaults = root == null ? null : root.getDefaults();
        return defaults == null ? null : defaults.get(createPath(this, path));
    }

    protected void mapChildrenKeys(@NotNull Set<String> output, @NotNull ConfigurationSection section, boolean deep) {
        Iterator<?> it;
        if (section instanceof MemorySection) {
            MemorySection sec = (MemorySection)section;
            it = sec.map.entrySet().iterator();

            while(it.hasNext()) {
                Entry entry = (Entry) it.next();
                output.add(createPath(section, (String) entry.getKey(), this));
                if (deep && entry.getValue() instanceof ConfigurationSection) {
                    ConfigurationSection subsection = (ConfigurationSection)entry.getValue();
                    this.mapChildrenKeys(output, subsection, deep);
                }
            }
        } else {
            Set<String> keys = section.getKeys(deep);
            it = keys.iterator();

            while(it.hasNext()) {
                String key = (String) it.next();
                output.add(createPath(section, key, this));
            }
        }
    }

    protected void mapChildrenValues(@NotNull Map<String, Object> output,
                                     @NotNull ConfigurationSection section, boolean deep) {
        Entry<String, Object> entry;
        Iterator<Entry<String, Object>> it;
        if (section instanceof MemorySection) {
            MemorySection sec = (MemorySection)section;
            it = sec.map.entrySet().iterator();

            while (it.hasNext()) {
                entry = it.next();
                String childPath = createPath(section, entry.getKey(), this);
                output.remove(childPath);
                output.put(childPath, entry.getValue());
                if (entry.getValue() instanceof ConfigurationSection && deep) {
                    this.mapChildrenValues(output, (ConfigurationSection) entry.getValue(), deep);
                }
            }
        } else {
            Map<String, Object> values = section.getValues(deep);
            it = values.entrySet().iterator();

            while (it.hasNext()) {
                entry = it.next();
                output.put(createPath(section, entry.getKey(), this), entry.getValue());
            }
        }

    }

    /**
     * Creates a path under the given key within the configuration section
     * @param section the section you are creating the path in
     * @param key the key to access this path from
     * @return the full string of your configuration section
     */
    @NotNull
    public static String createPath(@NotNull ConfigurationSection section, @Nullable String key) {
        return createPath(section, key, section.getRoot());
    }

    @NotNull
    public static String createPath(@NotNull ConfigurationSection section, @Nullable String key, @Nullable ConfigurationSection relativeTo) {
        Configuration root = section.getRoot();
        if (root == null) {
            throw new IllegalStateException("Can't create path without a root");
        }

        char separator = root.options().pathSeparator();
        StringBuilder builder = new StringBuilder();
        for (ConfigurationSection parent = section; parent != null && parent != relativeTo; parent = parent.getParent()) {
            if (builder.length() > 0) {
                builder.insert(0, separator);
            }

            builder.insert(0, parent.getName());
        }

        if (key != null && key.length() > 0) {
            if (builder.length() > 0) {
                builder.append(separator);
            }

            builder.append(key);
        }

        return builder.toString();
    }

    public String toString() {
        Configuration root = this.getRoot();
        return this.getClass().getSimpleName()
                + "[path='" + this.getCurrentPath()
                + "', root='" + (root == null ? null : root.getClass().getSimpleName()) + "']";
    }
}

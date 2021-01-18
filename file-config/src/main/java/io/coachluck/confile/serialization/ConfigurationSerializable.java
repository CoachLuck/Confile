package io.coachluck.confile.serialization;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface ConfigurationSerializable {
    @NotNull
    Map<String, Object> serialize();
}

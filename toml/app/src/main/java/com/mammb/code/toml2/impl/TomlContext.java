package com.mammb.code.toml2.impl;

import java.util.Collections;
import java.util.Map;

final class TomlContext {

    static final String PROPERTY_BUFFER_POOL = BufferPool.class.getName();

    /** Default maximum level of nesting. */
    private static final int DEFAULT_MAX_DEPTH = 1000;

    private final Map<String, ?> config;
    private final BufferPool bufferPool;

    // Maximum depth to parse
    private final int depthLimit;


    TomlContext(Map<String, ?> config, BufferPool defaultPool) {
        this.bufferPool = getBufferPool(config, defaultPool);
        this.config = config != null ? Collections.unmodifiableMap(config) : null;
        this.depthLimit = getIntConfig(TomlConfig.MAX_DEPTH, config, DEFAULT_MAX_DEPTH);
    }

    Map<String, ?> config() {
        return config;
    }

    Object config(String propertyName) {
        return config != null ? config.get(propertyName) : null;
    }

    BufferPool bufferPool() {
        return bufferPool;
    }

    int depthLimit() {
        return depthLimit;
    }

    private static BufferPool getBufferPool(Map<String, ?> config, BufferPool defaultPool) {
        BufferPool pool = (config != null) ? (BufferPool) config.get(PROPERTY_BUFFER_POOL) : null;
        return (pool != null) ? pool : defaultPool;
    }

    private static int getIntConfig(String propertyName, Map<String, ?> config, int defaultValue) {
        // try config Map first
        Integer intConfig = (config != null) ? getIntProperty(propertyName, config) : null;
        if (intConfig != null) {
            return intConfig;
        }
        // try system properties as fallback.
        intConfig = getIntSystemProperty(propertyName);
        return (intConfig != null) ? intConfig : defaultValue;
    }

    private static Integer getIntProperty(String propertyName, Map<String, ?> config) {
        Object property = config.get(propertyName);
        return switch (property) {
            case null -> null;
            case Number number -> number.intValue();
            case String s -> propertyStringToInt(propertyName, s);
            default -> throw new RuntimeException(
                String.format("Could not convert %s property of type %s to Integer",
                    propertyName, property.getClass().getName()));
        };
    }

    private static int propertyStringToInt(String propertyName, String propertyValue) {
        try {
            return Integer.parseInt(propertyValue);
        } catch (NumberFormatException e) {
            throw new RuntimeException(
                String.format("Value of %s property is not a number", propertyName), e);
        }
    }

    private static Integer getIntSystemProperty(String propertyName) {
        String systemProperty = System.getProperty(propertyName);
        if (systemProperty == null) {
            return null;
        }
        return propertyStringToInt(propertyName, systemProperty);
    }

}

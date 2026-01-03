package com.mammb.code.toml.impl;

import com.mammb.code.toml.api.TomlValue;

public final class MapUtil {

    private MapUtil() {
        super();
    }

    static TomlValue handle(Object value) {
        if (value instanceof TomlValue) {
            return (TomlValue) value;
        }
        throw new IllegalArgumentException(String.format("Type %s is not supported.", value.getClass()));
    }
}

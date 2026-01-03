package com.mammb.code.toml2.impl;

import com.mammb.code.toml2.api.TomlValue;

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

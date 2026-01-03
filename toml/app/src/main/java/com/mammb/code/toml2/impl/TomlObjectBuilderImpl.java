package com.mammb.code.toml2.impl;

import com.mammb.code.toml2.api.TomlArrayBuilder;
import com.mammb.code.toml2.api.TomlObjectBuilder;
import com.mammb.code.toml2.api.TomlValue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.Map;

class TomlObjectBuilderImpl implements TomlObjectBuilder {
    protected Map<String, TomlValue> valueMap;

    TomlObjectBuilderImpl(TomlContext jsonContext) {
    }

    TomlObjectBuilderImpl(TomlValue.TomlObject object, TomlContext jsonContext) {
        this(jsonContext);
        this.valueMap = new LinkedHashMap<>();
        this.valueMap.putAll(object);
    }

    TomlObjectBuilderImpl(Map<String, ?> map, TomlContext jsonContext) {
        this(jsonContext);
        this.valueMap = new LinkedHashMap<>();
    }
    @Override
    public TomlObjectBuilder add(String name, TomlValue value) {
        return null;
    }

    @Override
    public TomlObjectBuilder add(String name, String value) {
        return null;
    }

    @Override
    public TomlObjectBuilder add(String name, BigInteger value) {
        return null;
    }

    @Override
    public TomlObjectBuilder add(String name, BigDecimal value) {
        return null;
    }

    @Override
    public TomlObjectBuilder add(String name, int value) {
        return null;
    }

    @Override
    public TomlObjectBuilder add(String name, long value) {
        return null;
    }

    @Override
    public TomlObjectBuilder add(String name, double value) {
        return null;
    }

    @Override
    public TomlObjectBuilder add(String name, boolean value) {
        return null;
    }

    @Override
    public TomlObjectBuilder add(String name, TomlObjectBuilder builder) {
        return null;
    }

    @Override
    public TomlObjectBuilder add(String name, TomlArrayBuilder builder) {
        return null;
    }

    @Override
    public TomlObjectBuilder addAll(TomlObjectBuilder builder) {
        return null;
    }

    @Override
    public TomlObjectBuilder remove(String name) {
        return null;
    }

    @Override
    public TomlValue.TomlObject build() {
        return null;
    }
}

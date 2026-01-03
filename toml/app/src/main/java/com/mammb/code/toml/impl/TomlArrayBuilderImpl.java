package com.mammb.code.toml.impl;

import com.mammb.code.toml.api.TomlArrayBuilder;
import com.mammb.code.toml.api.TomlObjectBuilder;
import com.mammb.code.toml.api.TomlValue;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

public class TomlArrayBuilderImpl implements TomlArrayBuilder {

    private ArrayList<TomlValue> valueList;

    TomlArrayBuilderImpl() {
    }

    TomlArrayBuilderImpl(TomlValue.TomlArray array) {
        this();
        valueList = new ArrayList<>();
        valueList.addAll(array);
    }

    TomlArrayBuilderImpl(Collection<?> collection) {
        this();
        valueList = new ArrayList<>();
        populate(collection);
    }

    @Override
    public TomlArrayBuilder add(TomlValue value) {
        validateValue(value);
        addValueList(value);
        return this;
    }

    @Override
    public TomlArrayBuilder add(String value) {
        return null;
    }

    @Override
    public TomlArrayBuilder add(BigDecimal value) {
        return null;
    }

    @Override
    public TomlArrayBuilder add(BigInteger value) {
        return null;
    }

    @Override
    public TomlArrayBuilder add(int value) {
        return null;
    }

    @Override
    public TomlArrayBuilder add(long value) {
        return null;
    }

    @Override
    public TomlArrayBuilder add(double value) {
        return null;
    }

    @Override
    public TomlArrayBuilder add(boolean value) {
        return null;
    }

    @Override
    public TomlArrayBuilder add(TomlObjectBuilder builder) {
        return null;
    }

    @Override
    public TomlArrayBuilder add(TomlArrayBuilder builder) {
        return null;
    }

    @Override
    public TomlValue.TomlArray build() {
        return null;
    }

    private void validateValue(Object value) {
        if (value == null) {
            throw new NullPointerException("Cannot invoke add(null) while building TomlArray.");
        }
    }

    private void addValueList(TomlValue value) {
        if (valueList == null) {
            valueList = new ArrayList<>();
        }
        valueList.add(value);
    }

    private void populate(Collection<?> collection) {
        for (Object value : collection) {
            if (value instanceof Optional) {
                ((Optional<?>) value).ifPresent(v ->
                    this.valueList.add(MapUtil.handle(v)));
            } else {
                this.valueList.add(MapUtil.handle(value));
            }
        }
    }

}

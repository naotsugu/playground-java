package com.mammb.code.toml.api;

import java.math.BigDecimal;
import java.math.BigInteger;

public interface TomlObjectBuilder {
    TomlObjectBuilder add(String name, TomlValue value);
    TomlObjectBuilder add(String name, String value);
    TomlObjectBuilder add(String name, BigInteger value);
    TomlObjectBuilder add(String name, BigDecimal value);
    TomlObjectBuilder add(String name, int value);
    TomlObjectBuilder add(String name, long value);
    TomlObjectBuilder add(String name, double value);
    TomlObjectBuilder add(String name, boolean value);
    TomlObjectBuilder add(String name, TomlObjectBuilder builder);
    TomlObjectBuilder add(String name, TomlArrayBuilder builder);
    TomlObjectBuilder addAll(TomlObjectBuilder builder);
    TomlObjectBuilder remove(String name);
    TomlValue.TomlObject build();
}

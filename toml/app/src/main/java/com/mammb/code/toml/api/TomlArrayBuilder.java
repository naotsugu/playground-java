package com.mammb.code.toml.api;

import java.math.BigDecimal;
import java.math.BigInteger;

public interface TomlArrayBuilder {

    TomlArrayBuilder add(TomlValue value);
    TomlArrayBuilder add(String value);
    TomlArrayBuilder add(BigDecimal value);
    TomlArrayBuilder add(BigInteger value);
    TomlArrayBuilder add(int value);
    TomlArrayBuilder add(long value);
    TomlArrayBuilder add(double value);
    TomlArrayBuilder add(boolean value);
    TomlArrayBuilder add(TomlObjectBuilder builder);
    TomlArrayBuilder add(TomlArrayBuilder builder);
    default TomlArrayBuilder addAll(TomlArrayBuilder builder) {
        throw new UnsupportedOperationException();
    }
    default TomlArrayBuilder add(int index, TomlValue value) {
        throw new UnsupportedOperationException();
    }
    default TomlArrayBuilder add(int index, String value) {
        throw new UnsupportedOperationException();
    }
    default TomlArrayBuilder add(int index, BigDecimal value) {
        throw new UnsupportedOperationException();
    }
    default TomlArrayBuilder add(int index, BigInteger value) {
        throw new UnsupportedOperationException();
    }
    default TomlArrayBuilder add(int index, int value) {
        throw new UnsupportedOperationException();
    }
    default TomlArrayBuilder add(int index, long value) {
        throw new UnsupportedOperationException();
    }
    default TomlArrayBuilder add(int index, double value) {
        throw new UnsupportedOperationException();
    }
    default TomlArrayBuilder add(int index, boolean value) {
        throw new UnsupportedOperationException();
    }
    default TomlArrayBuilder add(int index, TomlObjectBuilder builder) {
        throw new UnsupportedOperationException();
    }
    default TomlArrayBuilder add(int index, TomlArrayBuilder builder) {
        throw new UnsupportedOperationException();
    }
    default TomlArrayBuilder set(int index, TomlValue value) {
        throw new UnsupportedOperationException();
    }
    default TomlArrayBuilder set(int index, String value) {
        throw new UnsupportedOperationException();
    }
    default TomlArrayBuilder set(int index, BigDecimal value) {
        throw new UnsupportedOperationException();
    }
    default TomlArrayBuilder set(int index, BigInteger value) {
        throw new UnsupportedOperationException();
    }
    default TomlArrayBuilder set(int index, int value) {
        throw new UnsupportedOperationException();
    }
    default TomlArrayBuilder set(int index, long value) {
        throw new UnsupportedOperationException();
    }
    default TomlArrayBuilder set(int index, double value) {
        throw new UnsupportedOperationException();
    }
    default TomlArrayBuilder set(int index, boolean value) {
        throw new UnsupportedOperationException();
    }
    default TomlArrayBuilder set(int index, TomlObjectBuilder builder) {
        throw new UnsupportedOperationException();
    }
    default TomlArrayBuilder set(int index, TomlArrayBuilder builder) {
        throw new UnsupportedOperationException();
    }
    default TomlArrayBuilder remove(int index) {
        throw new UnsupportedOperationException();
    }
    TomlValue.TomlArray build();

}

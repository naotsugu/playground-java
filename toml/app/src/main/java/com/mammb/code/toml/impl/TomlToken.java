package com.mammb.code.toml.impl;

public enum TomlToken {
    STRING(true), INTEGER(true), FLOAT(true), TRUE(true), FALSE(true),
    INF(false), N_INF(false), NAN(false),
    DATETIME(true), DATE(true), TIME(true),
    EQUALS(false), COMMA(false), DOT(false),
    CURLYOPEN(false), CURLYCLOSE(false),
    SQUAREOPEN(false), SQUARECLOSE(false),
    EOF(false),
    ;

    TomlToken(boolean value) {
        this.value = value;
    }

    private final boolean value;

    boolean isValue() {
        return value;
    }

}

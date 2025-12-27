package com.mammb.code.toml.impl;

public enum TomlToken {
    KEY(true),
    STRING(true), INTEGER(true), FLOAT(true), TRUE(true), FALSE(true), //DATETIME(true),
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

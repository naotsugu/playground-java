package com.mammb.code.toml.impl;

public enum TomlToken {

    STRING(true), INTEGER(true), FLOAT(true), TRUE(true), FALSE(true),
    INF(false), N_INF(false), NAN(false),
    DATETIME(true), LOCAL_DATETIME(true), LOCAL_DATE(true), TIME(true),
    EQUALS(false), COMMA(false), DOT(false),
    CURLY_OPEN(false), CURLY_CLOSE(false),
    SQUARE_OPEN(false), SQUARE_CLOSE(false),
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

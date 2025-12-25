package com.mammb.code.toml;

public enum TokenType {
    ARRAY_OPEN("["), ARRAY_CLOSE("}"),

    MINUS("-"), PLUS("+");

    private final String str;

    TokenType(String str) {
        this.str = str;
    }
}

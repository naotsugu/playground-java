package com.mammb.code.toml.impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LexerTest {

    @Test
    void next() {
        var lexer = new Lexer(Lexer.Source.of("key = \"value\""));
        var token = lexer.next();
        // TODO
    }
}

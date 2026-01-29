package com.mammb.code.toml.impl;

import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.*;

class TomlParserImplTest {

    @Test
    void test() {
        var parser = new TomlParserImpl(new StringReader(""), BufferPool.defaultPool());
        if (parser.hasNext()) {

        }
    }
}

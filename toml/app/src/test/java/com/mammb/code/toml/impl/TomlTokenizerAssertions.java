package com.mammb.code.toml.impl;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TomlTokenizerAssertions {

    public static void assertNext(TomlTokenizer tokenizer, TomlToken expectedToken, String expected) {
        assertEquals(expectedToken, tokenizer.nextToken());
        assertEquals(expected, tokenizer.getCharSequence().toString());
    }

    public static void assertNext(TomlTokenizer tokenizer, TomlToken expectedToken, int expected) {
        assertEquals(expectedToken, tokenizer.nextToken());
        assertEquals(expected, tokenizer.getInt());
    }

    public static void assertNext(TomlTokenizer tokenizer, TomlToken expectedToken, long expected) {
        assertEquals(expectedToken, tokenizer.nextToken());
        assertEquals(expected, tokenizer.getLong());
    }

    public static void assertNext(TomlTokenizer tokenizer, TomlToken expectedToken, LocalTime expected) {
        assertEquals(expectedToken, tokenizer.nextToken());
        assertEquals(expected, tokenizer.getLocalTime());
    }

    public static void assertNext(TomlTokenizer tokenizer, TomlToken expectedToken) {
        assertEquals(expectedToken, tokenizer.nextToken());
    }

}

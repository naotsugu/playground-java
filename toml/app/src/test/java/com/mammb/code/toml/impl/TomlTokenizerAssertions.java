package com.mammb.code.toml.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;

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

    public static void assertNext(TomlTokenizer tokenizer, TomlToken expectedToken, LocalDate expected) {
        assertEquals(expectedToken, tokenizer.nextToken());
        assertEquals(expected, tokenizer.getLocalDate());
    }

    public static void assertNext(TomlTokenizer tokenizer, TomlToken expectedToken, LocalDateTime expected) {
        assertEquals(expectedToken, tokenizer.nextToken());
        assertEquals(expected, tokenizer.getLocalDateTime());
    }

    public static void assertNext(TomlTokenizer tokenizer, TomlToken expectedToken, OffsetDateTime expected) {
        assertEquals(expectedToken, tokenizer.nextToken());
        assertEquals(expected, tokenizer.getOffsetDateTime());
    }

    public static void assertNext(TomlTokenizer tokenizer, TomlToken expectedToken) {
        assertEquals(expectedToken, tokenizer.nextToken());
    }

}

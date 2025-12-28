package com.mammb.code.toml.impl;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TomlTokenizerTest {

    @Test void bareKeyValue() {
        var tokenizer = new TomlTokenizer(new StringReader("a=\"b\""));
        assertToken(tokenizer, List.of(
            of(TomlToken.STRING, "a"),
            of(TomlToken.EQUALS, ""),
            of(TomlToken.STRING, "b")
        ));
    }

    @Test void basicKeyValue() {
        var tokenizer = new TomlTokenizer(new StringReader("\"a\"=\"b\""));
        assertToken(tokenizer, List.of(
            of(TomlToken.STRING, "a"),
            of(TomlToken.EQUALS, ""),
            of(TomlToken.STRING, "b")
        ));
    }

    @Test void literalKeyValue() {
        var tokenizer = new TomlTokenizer(new StringReader("'a'=\"b\""));
        assertToken(tokenizer, List.of(
            of(TomlToken.STRING, "a"),
            of(TomlToken.EQUALS, ""),
            of(TomlToken.STRING, "b")
        ));
    }

    @Test void infValue() {
        var tokenizer = new TomlTokenizer(new StringReader("a=inf"));
        assertToken(tokenizer, List.of(
            of(TomlToken.STRING, "a"),
            of(TomlToken.EQUALS, ""),
            of(TomlToken.PINF, "")
        ));
    }

    @Test void negativeInfValue() {
        var tokenizer = new TomlTokenizer(new StringReader("a=-inf"));
        assertToken(tokenizer, List.of(
            of(TomlToken.STRING, "a"),
            of(TomlToken.EQUALS, ""),
            of(TomlToken.NINF, "")
        ));
    }

    @Test void nanValue() {
        var tokenizer = new TomlTokenizer(new StringReader("a=nan"));
        assertToken(tokenizer, List.of(
            of(TomlToken.STRING, "a"),
            of(TomlToken.EQUALS, ""),
            of(TomlToken.NAN, "")
        ));
    }

    // ---

    private void assertToken(TomlTokenizer tokenizer, List<Pair<String>> pairs) {
        for (Pair<String> pair : pairs) {
            assertEquals(pair.type, tokenizer.nextToken());
            assertEquals(pair.value, tokenizer.getCharSequence().toString());
        }
    }

    private static <V> Pair<V> of(TomlToken type, V value) {
        return new Pair<>(type, value);
    }

    record Pair<V>(TomlToken type, V value) { }

}

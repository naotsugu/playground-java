package com.mammb.code.toml.impl;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TomlTokenizerTest {

    @Test void keyValue() {
        var tokenizer = new TomlTokenizer(new StringReader("key = \"value\""));
        assertToken(tokenizer, List.of(
            of(TomlToken.STRING, "key"),
            of(TomlToken.EQUALS, ""),
            of(TomlToken.STRING, "value")
        ));
    }

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

    @Test void numKeyValue() {
        var tokenizer = new TomlTokenizer(new StringReader("1234 = \"value\""));
        assertToken(tokenizer, List.of(
            of(TomlToken.STRING, "1234"),
            of(TomlToken.EQUALS, ""),
            of(TomlToken.STRING, "value")
        ));
    }

    @Test void quotedValue() {
        var tokenizer = new TomlTokenizer(new StringReader("'quoted \"value\"' = \"value\""));
        assertToken(tokenizer, List.of(
            of(TomlToken.STRING, "quoted \"value\""),
            of(TomlToken.EQUALS, ""),
            of(TomlToken.STRING, "value")
        ));
    }

    @Test void dottedKey() {
        var tokenizer = new TomlTokenizer(new StringReader("physical.color = \"orange\""));
        assertToken(tokenizer, List.of(
            of(TomlToken.STRING, "physical"),
            of(TomlToken.DOT, ""),
            of(TomlToken.STRING, "color"),
            of(TomlToken.EQUALS, ""),
            of(TomlToken.STRING, "orange")
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

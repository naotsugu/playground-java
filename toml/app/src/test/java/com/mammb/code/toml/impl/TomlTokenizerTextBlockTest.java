package com.mammb.code.toml.impl;

import org.junit.jupiter.api.Test;
import java.io.StringReader;

import static com.mammb.code.toml.impl.TomlTokenizerAssertions.assertNext;

public class TomlTokenizerTextBlockTest {

    @Test
    void textBlock() {
        var tokenizer = new TomlTokenizer(new StringReader("text = \"\"\"Here are two quotation marks: \"\". Simple enough.\"\"\""));
        assertNext(tokenizer, TomlToken.STRING, "text");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.STRING, "Here are two quotation marks: \"\". Simple enough.");
    }

    @Test
    void textBlockMl() {
        var tokenizer = new TomlTokenizer(new StringReader("str1 = \"\"\"\nRoses are red\nViolets are blue\"\"\""));
        assertNext(tokenizer, TomlToken.STRING, "str1");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.STRING, "Roses are red\nViolets are blue");
    }

    @Test
    void textBlockWithLineEscape1() {
        var tokenizer = new TomlTokenizer(new StringReader("str2 = \"\"\"\nThe quick brown \\\n\n fox jumps over \\\n   the lazy dog.\"\"\""));
        assertNext(tokenizer, TomlToken.STRING, "str2");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.STRING, "The quick brown fox jumps over the lazy dog.");
    }

    @Test
    void textBlockWithLineEscape2() {
        var tokenizer = new TomlTokenizer(new StringReader("str3 = \"\"\"\\\n       The quick brown \\\n       fox jumps over \\\n       the lazy dog.\\\n       \"\"\""));
        assertNext(tokenizer, TomlToken.STRING, "str3");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.STRING, "The quick brown fox jumps over the lazy dog.");
    }

    @Test void literalTextBlock() {
        var tokenizer = new TomlTokenizer(new StringReader("regex = '''I [dw]on't need \\d{2} apples'''"));
        assertNext(tokenizer, TomlToken.STRING, "regex");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.STRING, "I [dw]on't need \\d{2} apples");
    }

    @Test void literalTextBlockMl() {
        var tokenizer = new TomlTokenizer(new StringReader("lines = '''\nThe first newline is\ntrimmed in literal strings.\n'''"));
        assertNext(tokenizer, TomlToken.STRING, "lines");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.STRING, "The first newline is\ntrimmed in literal strings.\n");
    }


}

package com.mammb.code.toml.impl;

import org.junit.jupiter.api.Test;
import java.io.StringReader;

import static com.mammb.code.toml.impl.TomlTokenizerAssertions.assertNext;

public class TomlTokenizerInlineTableTest {

    @Test void basicKeyValue() {
        var tokenizer = new TomlTokenizer(new StringReader("name = { first = \"Tom\", last = \"Preston-Werner\" }"));

        assertNext(tokenizer, TomlToken.STRING, "name");
        assertNext(tokenizer, TomlToken.EQUALS);

        assertNext(tokenizer, TomlToken.CURLY_OPEN);
        assertNext(tokenizer, TomlToken.STRING, "first");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.STRING, "Tom");
        assertNext(tokenizer, TomlToken.COMMA);
        assertNext(tokenizer, TomlToken.STRING, "last");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.STRING, "Preston-Werner");
        assertNext(tokenizer, TomlToken.CURLY_CLOSE);
    }

    @Test void intValue() {
        var tokenizer = new TomlTokenizer(new StringReader("point = { x = 1, y = 2 }"));

        assertNext(tokenizer, TomlToken.STRING, "point");
        assertNext(tokenizer, TomlToken.EQUALS);

        assertNext(tokenizer, TomlToken.CURLY_OPEN);
        assertNext(tokenizer, TomlToken.STRING, "x");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.INTEGER, 1);
        assertNext(tokenizer, TomlToken.COMMA);
        assertNext(tokenizer, TomlToken.STRING, "y");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.INTEGER, 2);
        assertNext(tokenizer, TomlToken.CURLY_CLOSE);
    }

    @Test void dottedKey() {
        var tokenizer = new TomlTokenizer(new StringReader("animal = { type.name = \"pug\" }"));

        assertNext(tokenizer, TomlToken.STRING, "animal");
        assertNext(tokenizer, TomlToken.EQUALS);

        assertNext(tokenizer, TomlToken.CURLY_OPEN);
        assertNext(tokenizer, TomlToken.STRING, "type");
        assertNext(tokenizer, TomlToken.DOT);
        assertNext(tokenizer, TomlToken.STRING, "name");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.STRING, "pug");
        assertNext(tokenizer, TomlToken.CURLY_CLOSE);
    }

}

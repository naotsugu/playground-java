package com.mammb.code.toml.impl;

import org.junit.jupiter.api.Test;
import java.io.StringReader;

import static com.mammb.code.toml.impl.TomlTokenizerAssertions.assertNext;

public class TomlTokenizerArrayTest {

    @Test
    void intArray() {
        var tokenizer = new TomlTokenizer(new StringReader("integers = [ 1, 2, 3 ]"));
        assertNext(tokenizer, TomlToken.STRING, "integers");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.SQUAREOPEN);
        assertNext(tokenizer, TomlToken.INTEGER, 1);
        assertNext(tokenizer, TomlToken.COMMA);
        assertNext(tokenizer, TomlToken.INTEGER, 2);
        assertNext(tokenizer, TomlToken.COMMA);
        assertNext(tokenizer, TomlToken.INTEGER, 3);
        assertNext(tokenizer, TomlToken.SQUARECLOSE);
    }

    @Test
    void stringArray() {
        var tokenizer = new TomlTokenizer(new StringReader("colors = [ \"red\", \"yellow\", \"green\" ]"));
        assertNext(tokenizer, TomlToken.STRING, "colors");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.SQUAREOPEN);
        assertNext(tokenizer, TomlToken.STRING, "red");
        assertNext(tokenizer, TomlToken.COMMA);
        assertNext(tokenizer, TomlToken.STRING, "yellow");
        assertNext(tokenizer, TomlToken.COMMA);
        assertNext(tokenizer, TomlToken.STRING, "green");
        assertNext(tokenizer, TomlToken.SQUARECLOSE);
    }

    @Test
    void nestedIntsArray() {
        var tokenizer = new TomlTokenizer(new StringReader("nested_arrays_of_ints = [ [ 1, 2 ], [3, 4, 5] ]"));
        assertNext(tokenizer, TomlToken.STRING, "nested_arrays_of_ints");
        assertNext(tokenizer, TomlToken.EQUALS);
        assertNext(tokenizer, TomlToken.SQUAREOPEN);
        assertNext(tokenizer, TomlToken.SQUAREOPEN);
        assertNext(tokenizer, TomlToken.INTEGER, 1);
        assertNext(tokenizer, TomlToken.COMMA);
        assertNext(tokenizer, TomlToken.INTEGER, 2);
        assertNext(tokenizer, TomlToken.SQUARECLOSE);
        assertNext(tokenizer, TomlToken.COMMA);
        assertNext(tokenizer, TomlToken.SQUAREOPEN);
        assertNext(tokenizer, TomlToken.INTEGER, 3);
        assertNext(tokenizer, TomlToken.COMMA);
        assertNext(tokenizer, TomlToken.INTEGER, 4);
        assertNext(tokenizer, TomlToken.COMMA);
        assertNext(tokenizer, TomlToken.INTEGER, 5);
        assertNext(tokenizer, TomlToken.SQUARECLOSE);
        assertNext(tokenizer, TomlToken.SQUARECLOSE);
    }

}
